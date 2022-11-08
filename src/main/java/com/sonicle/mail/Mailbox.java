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

import com.sonicle.commons.flags.BitFlags;
import com.sun.mail.imap.IMAPStore;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.StampedLock;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sf.qualitycheck.Check;
import com.sonicle.commons.flags.BitFlagsEnum;
import jakarta.mail.event.ConnectionListener;

/**
 *
 * @author malbinola
 */
public class Mailbox {
	private static final Logger LOGGER = (Logger)LoggerFactory.getLogger(Mailbox.class);
	protected final StoreHostParams hostParams;
	protected final MailboxConfig config;
	protected final Session session;
	protected final StampedLock lock = new StampedLock();
	protected Store store = null;
	protected BitFlags<StoreOption> storeOptions = null;
	protected String[] sharedPrefixes = null;
	protected char folderSeparator = Character.MIN_VALUE;
	protected AtomicBoolean connecting = new AtomicBoolean(false);
	protected AtomicBoolean disconnecting = new AtomicBoolean(false);
	
	public Mailbox(final StoreHostParams mailboxParams, final MailboxConfig mailboxConfig, final Properties defaultProperties) throws GeneralSecurityException {
		this.hostParams = mailboxParams;
		this.config = mailboxConfig;
		this.session = StoreUtils.createSession(hostParams, mailboxConfig.getPoolSize(), defaultProperties);
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
	
	public void addConnectionListener(final ConnectionListener connectionListener) throws MessagingException {
		long stamp = lock.readLock();
		try {
			if (store != null) {
				doManageConnectionListener(false, connectionListener);
			} else {
				stamp = upgradeToWriteLock(stamp);
				doConnect(false);
				doManageConnectionListener(false, connectionListener);
			}
		} finally {
			lock.unlock(stamp);
		}
	}
	
	public void removeConnectionListener(final ConnectionListener connectionListener) throws MessagingException {
		long stamp = lock.readLock();
		try {
			if (store != null) {
				doManageConnectionListener(true, connectionListener);
			} else {
				stamp = upgradeToWriteLock(stamp);
				doConnect(false);
				doManageConnectionListener(true, connectionListener);
			}
		} finally {
			lock.unlock(stamp);
		}
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
			
			// Discover shared prefixes
			Folder uns[] = store.getUserNamespaces("");
			String[] prefixes = new String[uns.length];
			int i = 0;
			for (Folder folder : uns) {
				prefixes[i] = folder.getFullName();
				i++;
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
		if (store != null && store.isConnected()) store.close();
	}
	
	private void doManageConnectionListener(final boolean remove, final ConnectionListener connectionListener) throws MessagingException {
		if (remove) {
			store.removeConnectionListener(connectionListener);
		} else {
			store.addConnectionListener(connectionListener);
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
		String name = "INBOX";
		if (config.hasAlternativeRootFolder()) {
			name = doGetRootFolder().getFullName();
			if (storeOptions.has(StoreOption.EXPLICIT_INBOX)) name += folderSeparator + "INBOX";
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
