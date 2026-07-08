/*
 * sonicle-mail is is a helper library developed by Sonicle S.r.l.
 * Copyright (C) 2014 Sonicle S.r.l.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation with the addition of the following permission
 * added to Section 15 as permitted in Section 7(a): FOR ANY PART OF THE COVERED
 * WORK IN WHICH THE COPYRIGHT IS OWNED BY SONICLE, SONICLE DISCLAIMS THE
 * WARRANTY OF NON INFRINGEMENT OF THIRD PARTY RIGHTS.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301 USA.
 *
 * You can contact Sonicle S.r.l. at email address sonicle@sonicle.com
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License version 3.
 *
 * In accordance with Section 7(b) of the GNU Affero General Public License
 * version 3, these Appropriate Legal Notices must retain the display of the
 * Sonicle logo and Sonicle copyright notice. If the display of the logo is not
 * reasonably feasible for technical reasons, the Appropriate Legal Notices must
 * display the words "Copyright (C) 2014 Sonicle S.r.l.".
 */
package com.sonicle.mail;

import com.sonicle.commons.Check;
import com.sonicle.commons.flags.BitFlags;
import com.sonicle.mail.imap.SonicleIMAPSocketTracker;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Quota;
import jakarta.mail.Session;
import jakarta.mail.Store;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sonicle.commons.flags.BitFlagsEnum;
import com.sonicle.mail.cyrus.CyrusManager;
import static com.sonicle.mail.cyrus.CyrusManager.CYRUS_KEY_SHAREDSEEN;
import jakarta.mail.event.ConnectionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/**
 *
 * @author malbinola
 */
public class Mailbox {
	private static final Logger LOGGER = (Logger)LoggerFactory.getLogger(Mailbox.class);
	public static final String DEFAULT_INBOX_FOLDERNAME = "INBOX";
	protected final StoreHostParams hostParams;
	protected final MailboxConfig config;
	protected final Session session;
	protected final Properties defaultProperties;
	protected final StampedLock lock = new StampedLock();
	protected Store store = null;
	protected BitFlags<StoreOption> storeOptions = null;
	//volatile + build-then-publish in doConnect: read lock-free by helpers like
	//getSharedFolderName/isUnderSharedFolder on request threads while a reconnect
	//under the write lock rebuilds them
	protected volatile HashSet<String> sharedPrefixes = null;
	protected volatile char folderSeparator = Character.MIN_VALUE;
	// Live dedicated idle connections (each on its own store/session/tracker,
	// independent of the interactive pooled store). Closed on disconnect().
	// Guarded by its own monitor: registration in openDedicatedFolder must not
	// interleave with closeAllDedicatedFolders, or an in-flight open survives
	// the teardown as an untracked live IDLE connection.
	protected final Set<DedicatedFolder> dedicatedFolders = ConcurrentHashMap.newKeySet();
	protected boolean dedicatedTeardown = false; //guarded by synchronized(dedicatedFolders)
	// Optional tracker for the POOLED store's sockets: when present, disconnect()
	// force-closes them before store.close(), so teardown cannot hang on a dead
	// socket (e.g. dropped VPN). Dedicated idle folders have their own trackers.
	protected final SonicleIMAPSocketTracker storeSocketTracker;

	public Mailbox(final StoreHostParams mailboxParams, final MailboxConfig mailboxConfig, final Properties defaultProperties) throws GeneralSecurityException {
		this(mailboxParams, mailboxConfig, defaultProperties, null);
	}

	public Mailbox(final StoreHostParams mailboxParams, final MailboxConfig mailboxConfig, final Properties defaultProperties, final SonicleIMAPSocketTracker storeSocketTracker) throws GeneralSecurityException {
		this.hostParams = mailboxParams;
		this.config = mailboxConfig;
		this.defaultProperties = defaultProperties;
		this.storeSocketTracker = storeSocketTracker;
		this.session = (storeSocketTracker != null)
			? StoreUtils.createSession(hostParams, mailboxConfig.getPoolSize(), defaultProperties, storeSocketTracker)
			: StoreUtils.createSession(hostParams, mailboxConfig.getPoolSize(), defaultProperties);
	}
	
	public void ensureConnected() throws MessagingException {
		long stamp = lock.readLock();
		try {
			if (!doIsReady()) {
				stamp = upgradeToWriteLock(stamp);
				doConnect(false);
			}
		} finally {
			lock.unlock(stamp);
		}
	}
	
	public boolean connect() {
		return connect(false);
	}
	
	public boolean connect(boolean reset) {
		if (reset) {
			long stamp = lock.writeLock();
			try {
				doDisconnect();
				doConnect(true);
				return true;
				
			} catch (MessagingException ex) {
				LOGGER.debug("Error reset connecting", ex);
				doQuietlyDisconnect();
				return false;
			} finally {
				lock.unlockWrite(stamp);
			}
		} else {
			long stamp = lock.readLock();
			try {
				if (doIsReady()) return true;
				stamp = upgradeToWriteLock(stamp);
				doConnect(true);
				return true;
				
			} catch (MessagingException ex) {
				LOGGER.debug("Error connecting", ex);
				doQuietlyDisconnect();
				return false;
			} finally {
				lock.unlock(stamp);
			}
		}
	}
	
	public boolean disconnect() {
		// Force-close any dedicated idle connections first (independent of the
		// interactive store state and lock): tracker.closeAll() unblocks parked
		// IDLE reads so their store.close() cannot hang.
		closeAllDedicatedFolders();
		long stamp = lock.readLock();
		try {
			if (!doIsReady()) return true;
			stamp = upgradeToWriteLock(stamp);
			doDisconnect();
			return true;

		} catch (MessagingException ex) {
			LOGGER.debug("Error disconnecting", ex);
			return false;
		} finally {
			lock.unlock(stamp);
		}
	}
	
	public boolean isConnected() {
		long stamp = lock.readLock();
		try {
			return doIsReady();
		} finally {
			lock.unlock(stamp);
		}
	}
	
	public void addConnectionListener(final ConnectionListener connectionListener) throws MessagingException {
		//mutates the store's listener registry: write lock + readiness (not a bare
		//store!=null, which would register on a closed store)
		long stamp = lock.readLock();
		try {
			if (!doIsReady()) {
				stamp = upgradeToWriteLock(stamp);
				doConnect(false);
				doManageConnectionListener(false, connectionListener);
			} else {
				stamp = upgradeToWriteLock(stamp);
				doManageConnectionListener(false, connectionListener);
			}
		} finally {
			lock.unlock(stamp);
		}
	}

	public void removeConnectionListener(final ConnectionListener connectionListener) throws MessagingException {
		long stamp = lock.readLock();
		try {
			if (!doIsReady()) {
				stamp = upgradeToWriteLock(stamp);
				doConnect(false);
				doManageConnectionListener(true, connectionListener);
			} else {
				stamp = upgradeToWriteLock(stamp);
				doManageConnectionListener(true, connectionListener);
			}
		} finally {
			lock.unlock(stamp);
		}
	}
	
	public char getFolderSeparator() {
		return folderSeparator;
	}
	
	public Folder getInboxFolder() throws MessagingException {
		long stamp = lock.readLock();
		try {
			if (doIsReady()) {
				return doGetSpecialFolder(doGetInboxFolderFullName(), false);
			} else {
				stamp = upgradeToWriteLock(stamp);
				doConnect(false);
				return doGetSpecialFolder(doGetInboxFolderFullName(), false);
			}
		} finally {
			lock.unlock(stamp);
		}
	}
	
	public Folder getRootFolder() throws MessagingException {
		long stamp = lock.readLock();
		try {
			if (doIsReady()) {
				return doGetRootFolder();
			} else {
				stamp = upgradeToWriteLock(stamp);
				doConnect(false);
				return doGetRootFolder();
			}
		} finally {
			lock.unlock(stamp);
		}
	}
	
	public Folder getFolder(final String name) throws MessagingException {
		return getFolder(name, false);
	}
	
	public Folder getFolder(final String name, final boolean createIfNecessary) throws MessagingException {
		long stamp = lock.readLock();
		try {
			if (doIsReady()) {
				return doGetFolder(name, createIfNecessary);
			} else {
				stamp = upgradeToWriteLock(stamp);
				doConnect(false);
				return doGetFolder(name, createIfNecessary);
			}
		} finally {
			lock.unlock(stamp);
		}
	}
	
	/**
	 * Opens a folder on a DEDICATED connection, independent of the interactive
	 * pooled store, intended to be parked in {@code IMAPFolder.idle()}. The returned
	 * handle owns its own store, session and {@link SonicleIMAPSocketTracker}, so it
	 * can be force-closed ({@link DedicatedFolder#hardClose()}) to break a blocked
	 * IDLE read without disturbing the interactive pool or other idle folders. All
	 * live handles are also closed by {@link #disconnect()}.
	 *
	 * @param name Folder full name (resolved against the mailbox root).
	 * @param readWrite True to open READ_WRITE (required for IDLE), false READ_ONLY.
	 * @return The dedicated folder handle.
	 */
	public DedicatedFolder openDedicatedFolder(final String name, final boolean readWrite) throws MessagingException, GeneralSecurityException {
		// Ensure the interactive store is connected first so the folder separator
		// and root have been discovered (reused below for the dedicated store).
		ensureConnected();
		final SonicleIMAPSocketTracker tracker = new SonicleIMAPSocketTracker();
		final Session dsession = StoreUtils.createSession(hostParams, 1, defaultProperties, tracker);
		Store dstore = null;
		try {
			dstore = StoreUtils.open(dsession, hostParams.getProtocol());
			final Folder droot = config.hasAlternativeRootFolder() ? dstore.getFolder(config.getRootFolderName()) : dstore.getDefaultFolder();
			if (droot == null || !droot.exists()) throw new MessagingException("Root NOT found");
			final Folder f = StoreUtils.getFolder(droot, name, folderSeparator, false);
			if (readWrite && !f.isOpen()) f.open(Folder.READ_WRITE);
			final DedicatedFolder handle = new DedicatedFolder(this, dstore, tracker, (IMAPFolder)f);
			// Registration is gated: if a disconnect ran while this open was in
			// flight, adding now would leave a live IDLE connection nobody closes.
			synchronized (dedicatedFolders) {
				if (dedicatedTeardown) throw new MessagingException("Mailbox is disconnecting");
				dedicatedFolders.add(handle);
			}
			return handle;
		} catch (MessagingException | RuntimeException ex) {
			// Do not leak a half-opened dedicated connection
			if (dstore != null) StoreUtils.closeQuietly(dstore);
			tracker.closeAll();
			throw ex;
		}
	}

	private void closeAllDedicatedFolders() {
		final java.util.List<DedicatedFolder> toClose;
		synchronized (dedicatedFolders) {
			dedicatedTeardown = true; //re-armed by the next doConnect
			toClose = new ArrayList<>(dedicatedFolders);
			dedicatedFolders.clear();
		}
		for (DedicatedFolder handle : toClose) {
			handle.hardClose();
		}
	}

	public Folder getSpecialFolder(final SpecialFolder folder, final boolean createIfNecessary) throws MessagingException {
		Check.notNull(folder, "folder");
		long stamp = lock.readLock();
		try {
			final String name = toSpecialFolderName(folder);
			if (name == null) throw new MessagingException("Special folder '" + folder.name() + "' name not configured");
			if (doIsReady()) {
				return doGetSpecialFolder(name, createIfNecessary);
			} else {
				stamp = upgradeToWriteLock(stamp);
				doConnect(false);
				return doGetSpecialFolder(name, createIfNecessary);
			}
		} finally {
			lock.unlock(stamp);
		}
	}
	
	public Folder getSpecialFolder(final String name) throws MessagingException {
		long stamp = lock.readLock();
		try {
			if (doIsReady()) {
				return doGetSpecialFolder(name, true);
			} else {
				stamp = upgradeToWriteLock(stamp);
				doConnect(false);
				return doGetSpecialFolder(name, true);
			}
		} finally {
			lock.unlock(stamp);
		}
	}
	
	public String getSharedFolderName(String mailUser) throws MessagingException {
		String sharedFolderName = null;
		String folderName = null;
		
		// Clear mailUser removing any domain info (ldap auth contains 
		// domain suffix), we don't want it!
		String user = StringUtils.split(mailUser, "@")[0];
		
		for(String sharedPrefix: sharedPrefixes) {
			Folder sharedFolder = getFolder(sharedPrefix);
			for(Folder fo : sharedFolder.list()) {
				folderName = fo.getFullName();
				char sep=fo.getSeparator();
				//if is a shared mailbox, and it contains an @, match it with mail user (NS7)
				//or just user instead (XStream and NS6)
				String name = fo.getName().indexOf('@')>0?mailUser:user;
				if(folderName.equals(sharedPrefix + sep + name)) return folderName;
			}
		}
		return null;
	}

	/**
	 * Bulk variant of {@link #getSharedFolderName(String)}: lists each shared
	 * prefix ONCE and resolves every given mail user against that listing,
	 * instead of paying one LIST round-trip per user.
	 * @param mailUsers The mail users to resolve.
	 * @return mailUser to shared folder full name; users with no matching
	 * shared folder are absent from the map.
	 */
	public Map<String, String> getSharedFolderNames(Collection<String> mailUsers) throws MessagingException {
		Map<String, String> results = new LinkedHashMap<>();
		if (mailUsers == null || mailUsers.isEmpty()) return results;
		for(String sharedPrefix: sharedPrefixes) {
			if (results.size() == mailUsers.size()) break;
			Folder sharedFolder = getFolder(sharedPrefix);
			for(Folder fo : sharedFolder.list()) {
				String folderName = fo.getFullName();
				char sep = fo.getSeparator();
				//same rule as getSharedFolderName: folders named with an @ match the
				//full mail user (NS7), otherwise the bare user (XStream and NS6)
				boolean matchFullAddress = fo.getName().indexOf('@')>0;
				for(String mailUser: mailUsers) {
					if (results.containsKey(mailUser)) continue;
					String name = matchFullAddress ? mailUser : StringUtils.split(mailUser, "@")[0];
					if (folderName.equals(sharedPrefix + sep + name)) results.put(mailUser, folderName);
				}
			}
		}
		return results;
	}
	
	public boolean isUnderSharedFolder(String foldername) {
		boolean b = false;
		String str = null;
		for (String fn : sharedPrefixes) {
			str = fn + folderSeparator;
			if (foldername.startsWith(str)) {
				b = true;
				break;
			}
		}
		return b;
	}
	
	public String getMainSharedFolder(String foldername) {
		if (!foldername.endsWith(""+folderSeparator)) foldername+=folderSeparator;
		for (String fn : sharedPrefixes) {
			String str = fn + folderSeparator;
			if (foldername.startsWith(str)) {
				int ix=foldername.indexOf(folderSeparator,str.length());
				if (ix>=0) return foldername.substring(0,ix);
			}
		}
		return null;
	}

	public void setSharedSeen(final boolean value) throws MessagingException {
		long stamp = lock.readLock();
		try {
			if (doIsReady()) {
				doSetSharedSeen(value);
			} else {
				stamp = upgradeToWriteLock(stamp);
				doConnect(false);
				doSetSharedSeen(value);
			}
		} finally {
			lock.unlock(stamp);
		}
	}
	
	/**
	 * Shared-namespace prefixes discovered at connect (IMAP NAMESPACE command).
	 * @return a defensive copy, never null (empty when none / not discovered yet)
	 */
	public Set<String> getSharedPrefixes() throws MessagingException {
		long stamp = lock.readLock();
		try {
			if (!doIsReady()) {
				stamp = upgradeToWriteLock(stamp);
				doConnect(false);
			}
			HashSet<String> prefixes = sharedPrefixes;
			return (prefixes != null) ? new LinkedHashSet<>(prefixes) : new LinkedHashSet<String>();
		} finally {
			lock.unlock(stamp);
		}
	}

	/**
	 * Quota info for the passed folder root (IMAP QUOTA extension, RFC 2087).
	 * @return the store's quota array, or null when the store is not IMAP
	 */
	public Quota[] getQuota(final String folderName) throws MessagingException {
		long stamp = lock.readLock();
		try {
			if (!doIsReady()) {
				stamp = upgradeToWriteLock(stamp);
				doConnect(false);
			}
			if (!(store instanceof IMAPStore)) return null;
			return ((IMAPStore)store).getQuota(folderName);
		} finally {
			lock.unlock(stamp);
		}
	}

	public boolean hasOption(final StoreOption option) throws MessagingException {
		Check.notNull(option, "option");
		long stamp = lock.readLock();
		try {
			if (doIsReady()) {
				return doCheckStoreOption(option);
			} else {
				stamp = upgradeToWriteLock(stamp);
				doConnect(false);
				return doCheckStoreOption(option);
			}
		} finally {
			lock.unlock(stamp);
		}
	}
	
	public boolean isDovecot() throws MessagingException {
		return hasOption(StoreOption.IS_DOVECOT);
	}
	
	public boolean isCyrus() throws MessagingException {
		return hasOption(StoreOption.IS_CYRUS);
	}
	
	private boolean doIsReady() {
		return (store != null) && (store.isConnected());
	}
	
	private void doConnect(boolean initial) throws MessagingException {
		// Creates & connect store
		if (store == null) {
			initial = true;
			store = StoreUtils.open(session, hostParams.getProtocol());
		} else {
			StoreUtils.ensureConnected(store);
		}
		// A deliberate reconnect re-opens the gate closed by disconnect()'s
		// dedicated-folder teardown (we hold the write lock here).
		synchronized (dedicatedFolders) {
			dedicatedTeardown = false;
		}

		if (initial) {
			// Discover store's options
			storeOptions = new BitFlags<>(StoreOption.class);
			if (store instanceof IMAPStore) {
				if (((IMAPStore)store).hasCapability("ANNOTATEMORE")) {
					storeOptions.set(StoreOption.ANNOTATIONS);
				}
				if (((IMAPStore)store).hasCapability("ID")) {
					final Map<String, String> map = ((IMAPStore)store).id(null);
					if (map != null) {
						if (map.containsKey("name")) {
							final String name = StringUtils.lowerCase(map.get("name"));
							if ("dovecot".equals(name)) {
								storeOptions.set(StoreOption.IS_DOVECOT);
								storeOptions.set(StoreOption.EXPLICIT_INBOX);
							} else if (StringUtils.startsWith(name, "cyrus")) {
								storeOptions.set(StoreOption.IS_CYRUS);
							}
						}
					}
				}
			}
			
			// Discover shared prefixes: build fully, then publish (the volatile
			// field is read lock-free by request threads during a reconnect)
			Folder uns[] = store.getUserNamespaces("");
			LinkedHashSet<String> prefixes = new LinkedHashSet<>(uns.length);
			for (Folder folder : uns) {
				prefixes.add(folder.getFullName());
			}
			sharedPrefixes = prefixes;

			// Discover folder separator
			Folder rootFolder = doGetRootFolder();
			folderSeparator = rootFolder.getSeparator();
		}	
	}
	
	private boolean doQuietlyDisconnect() {
		try {
			doDisconnect();
			return true;
		} catch (MessagingException ex) {
			LOGGER.debug("Error closing store", ex);
			return false;
		}
	}
	
	private void doDisconnect() throws MessagingException {
		//force-close pooled sockets first (when tracked): a parked read on a dead
		//socket would otherwise make store.close() hang
		if (storeSocketTracker != null) {
			try { storeSocketTracker.closeAll(); } catch (Exception ignore) {}
		}
		if (store != null && store.isConnected()) store.close();
	}
	
	private void doManageConnectionListener(final boolean remove, final ConnectionListener connectionListener) throws MessagingException {
		if (remove) {
			store.removeConnectionListener(connectionListener);
		} else {
			store.addConnectionListener(connectionListener);
		}
	}
	
	private void doSetSharedSeen(final boolean value) throws MessagingException {
		if (doCheckStoreOption(StoreOption.IS_CYRUS)) {
			Folder rootFolder = doGetRootFolder();
			if (rootFolder == null || !rootFolder.exists()) throw new MessagingException("Root NOT found");

			final CyrusManager.SharedSeenMethod method = CyrusManager.guessSharedSeenMethod((IMAPStore)store);
			doCyrusSetSharedSeenRecursive(method, rootFolder, value);
			
		} else {
			throw new MessagingException("SharedSeen is available only for CyrusIMAP");
		}
	}
	
	private void doCyrusSetSharedSeenRecursive(final CyrusManager.SharedSeenMethod method, final Folder parentFolder, final boolean value) throws MessagingException {
		final Folder[] children = parentFolder.list();
		if (children == null || children.length == 0) return;
		
		for (Folder child : children) {
			if (sharedPrefixes.contains(child.getFullName())) continue;
			CyrusManager.setSharedSeen(method, child, value);
			doCyrusSetSharedSeenRecursive(method, child, value);
		}
	}
	
	private Folder doGetFolder(final String name, final boolean createIfNecessary) throws MessagingException {
		Folder rootFolder = doGetRootFolder();
		if (rootFolder == null || !rootFolder.exists()) throw new MessagingException("Root NOT found");
		return StoreUtils.getFolder(rootFolder, name, folderSeparator, createIfNecessary);
	}
	
	private Folder doGetSpecialFolder(final String name, final boolean createIfNecessary) throws MessagingException {
		Folder rootFolder = doGetRootFolder();
		if (rootFolder == null || !rootFolder.exists()) throw new MessagingException("Root NOT found");
		return StoreUtils.getFolder(rootFolder, name, folderSeparator, createIfNecessary);
	}
	
	private String doGetInboxFolderFullName() throws MessagingException {
		String name = DEFAULT_INBOX_FOLDERNAME;
		if (config.hasAlternativeRootFolder()) {
			name = doGetRootFolder().getFullName();
			if (storeOptions.has(StoreOption.EXPLICIT_INBOX)) name += folderSeparator + DEFAULT_INBOX_FOLDERNAME;
		}
		return name;
	}
	
	private Folder doGetRootFolder() throws MessagingException {
		return config.hasAlternativeRootFolder() ? store.getFolder(config.getRootFolderName()) : store.getDefaultFolder();
	}
	
	private boolean doCheckStoreOption(StoreOption option) {
		return storeOptions.has(option);
	}
	
	private long upgradeToWriteLock(long rstamp) {
		long wstamp = lock.tryConvertToWriteLock(rstamp);
		if (wstamp == 0L) {
			lock.unlockRead(rstamp);
			return lock.writeLock();
		} else {
			return wstamp;
		}
	}
	
	private String toSpecialFolderName(final SpecialFolder folder) {
		if (SpecialFolder.SENT.equals(folder)) {
			return StringUtils.defaultIfBlank(config.getSentFolderName(), null);
		} else if (SpecialFolder.DRAFTS.equals(folder)) {
			return StringUtils.defaultIfBlank(config.getDraftsFolderName(), null);
		} else if (SpecialFolder.TRASH.equals(folder)) {
			return StringUtils.defaultIfBlank(config.getTrashFolderName(), null);
		} else if (SpecialFolder.SPAM.equals(folder)) {
			return StringUtils.defaultIfBlank(config.getSpamFolderName(), null);
		} else if (SpecialFolder.ARCHIVE.equals(folder)) {
			return StringUtils.defaultIfBlank(config.getArchiveFolderName(), null);
		}
		return null;
	}
	
	/**
	 * A folder opened on a dedicated IMAP connection (own store/session/tracker),
	 * meant to be parked in {@code IMAPFolder.idle()}. Not returned to any pool.
	 */
	public static final class DedicatedFolder {
		private final Mailbox mailbox;
		private final Store store;
		private final SonicleIMAPSocketTracker tracker;
		private final IMAPFolder folder;
		private volatile boolean closed = false;

		DedicatedFolder(final Mailbox mailbox, final Store store, final SonicleIMAPSocketTracker tracker, final IMAPFolder folder) {
			this.mailbox = mailbox;
			this.store = store;
			this.tracker = tracker;
			this.folder = folder;
		}

		public IMAPFolder getFolder() {
			return folder;
		}

		public boolean isClosed() {
			return closed;
		}

		/**
		 * Ensures the dedicated store is connected and the folder open READ_WRITE,
		 * reconnecting after a server-side IDLE timeout or a dropped connection.
		 * Called by the idle loop before each {@code idle()}.
		 *
		 * <p>Synchronized against {@link #hardClose()}'s close phase, with `closed`
		 * re-checked after the reconnect: without this, a hardClose landing between
		 * the entry check and store.connect() would tear down the OLD socket while
		 * we open a NEW one that escapes both the tracker sweep and the registry —
		 * a leaked live IDLE connection.</p>
		 */
		public synchronized void ensureOpen() throws MessagingException {
			if (closed) throw new MessagingException("Dedicated folder is closed");
			if (!store.isConnected()) store.connect();
			if (closed) {
				StoreUtils.closeQuietly(store);
				throw new MessagingException("Dedicated folder closed during reconnect");
			}
			if (!folder.isOpen()) folder.open(Folder.READ_WRITE);
			if (closed) {
				try { folder.close(false); } catch (Exception ignore) {}
				StoreUtils.closeQuietly(store);
				throw new MessagingException("Dedicated folder closed during reopen");
			}
		}

		/**
		 * Force-closes the dedicated connection: hard-closes its socket (unblocking
		 * any parked IDLE read) then quietly closes the folder and store. Idempotent.
		 *
		 * <p>Ordering matters: the closed flag and tracker.closeAll() run BEFORE
		 * taking the monitor — the tracker force-close also aborts a store.connect()
		 * in-flight inside ensureOpen (tracked sockets), so ensureOpen releases the
		 * monitor promptly and this method can never deadlock behind a blocked
		 * network call.</p>
		 */
		public void hardClose() {
			closed = true;
			try { tracker.closeAll(); } catch (Exception ignore) {}
			synchronized (this) {
				try { if (folder.isOpen()) folder.close(false); } catch (Exception ignore) {}
				StoreUtils.closeQuietly(store);
			}
			mailbox.dedicatedFolders.remove(this);
		}
	}

	public static enum SpecialFolder {
		SENT, DRAFTS, TRASH, SPAM, ARCHIVE
	}
	
	public static enum StoreOption implements BitFlagsEnum<StoreOption> {
		ANNOTATIONS(1 << 1), EXPLICIT_INBOX(1 << 2), IS_CYRUS(1 << 3), IS_DOVECOT(1 << 4);
		
		private long mask = 0;
		private StoreOption(long mask) { this.mask = mask; }
		@Override
		public long mask() { return this.mask; }
	}
}
