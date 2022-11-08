/*
 * sonicle-mail is is a helper library developed by Sonicle S.r.l.
 * Copyright (C) 2022 Sonicle S.r.l.
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
 * display the words "Copyright (C) 2022 Sonicle S.r.l.".
 */
package com.sonicle.mail;

import com.sun.mail.imap.ACL;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPFolder.FetchProfileItem;
import com.sun.mail.imap.Rights;
import jakarta.mail.FetchProfile;
import jakarta.mail.FetchProfile.Item;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.search.HeaderTerm;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Properties;
import net.sf.qualitycheck.Check;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author malbinola
 */
public class StoreUtils {
	public static final FetchProfile FETCH_PROFILE_FLAGS_UID = new FetchProfile();
	public static final FetchProfile FETCH_PROFILE_HEAD = new FetchProfile();
	public static final FetchProfile FETCH_PROFILE_UID = new FetchProfile();
	public static final String FOLDER_FULL_RIGHTS = "lrswipcda";
	
	static {
		FETCH_PROFILE_HEAD.add(Item.ENVELOPE);
		FETCH_PROFILE_HEAD.add(Item.CONTENT_INFO);
		FETCH_PROFILE_HEAD.add(FetchProfileItem.HEADERS);
		FETCH_PROFILE_FLAGS_UID.add(Item.FLAGS);
		FETCH_PROFILE_FLAGS_UID.add(UIDFolder.FetchProfileItem.UID);
		FETCH_PROFILE_UID.add(UIDFolder.FetchProfileItem.UID);
	}
	
	public static Session createSession(final StoreHostParams params, final int poolSize) throws GeneralSecurityException {
		return createSession(params, poolSize, System.getProperties());
	}
	
	public static Session createSession(final StoreHostParams params, final int poolSize, final Properties defaultProperties) throws GeneralSecurityException {
		Properties props = new Properties(defaultProperties);
		
		final StoreProtocol proto = params.getProtocol();
		proto.applyProperties(props);
		props.setProperty(proto.getPropertyName("host"), params.getHost());
		props.setProperty(proto.getPropertyName("port"), String.valueOf(params.getPort()));
		if (params.getTrustHost()) proto.sslTrustHosts(props, Arrays.asList(params.getHost()));
		props.setProperty(proto.getPropertyName("connectionpoolsize"), String.valueOf(poolSize));
		
		String username = null;
		if (!StringUtils.isBlank(params.getUsername())) {
			if (ImpersonateMode.SASL.equals(params.getImpersonateMode()) && !StringUtils.isBlank(params.getAdminUsername())) {
				username = params.getAdminUsername();
				props.setProperty(proto.getPropertyName("sasl.authorizationid"), params.getUsername());
			} else if (ImpersonateMode.VMAIL.equals(params.getImpersonateMode())) {
				username = params.getUsername() + "*vmail";
			} else {
				username = params.getUsername();
			}
			props.setProperty(proto.getPropertyName("username"), username);
		}
		if (username != null && params.getPassword() != null) {
			props.setProperty(proto.getPropertyName("authenticate"), "true");
			return Session.getInstance(props, new PasswordAuthenticator(username, params.getPassword()));
			
		} else {
			return Session.getInstance(props);
		}
	}
	
	public static Store open(final Session session, final StoreProtocol protocol) throws MessagingException {
		Check.notNull(session, "session");
		Check.notNull(protocol, "protocol");
		return ensureConnected(session.getStore(protocol.getProtocol()));
	}
	
	public static Store ensureConnected(final Store store) throws MessagingException {
		Check.notNull(store, "store");
		if (!store.isConnected()) store.connect();
		return store;
	}
	
	public static void closeQuietly(final Store store) {
		try {
			StoreUtils.close(store);
		} catch (MessagingException ex) { /* Do nothing... */ }
	}
	
	public static void close(final Store store) throws MessagingException {
		if (store != null && store.isConnected()) store.close();
	}
	
	public static Folder openFolder(final Store store, final String name, final boolean readWrite) throws MessagingException {
		Check.notNull(store, "store");
		return openFolder(store.getFolder(name), readWrite);
	}
	
	public static Folder openFolder(final Folder folder, final boolean readWrite) throws MessagingException {
		Check.notNull(folder, "folder");
		if (folder.exists() && !folder.isOpen() && (folder.getType() & Folder.HOLDS_MESSAGES) != 0) {
			folder.open(readWrite ? Folder.READ_WRITE : Folder.READ_ONLY);
		}
		return folder;
	}
	

	/*
	public static Folder createFolder(final Store store, final String pathName, final boolean createHierarchy) throws MessagingException {
		final Folder root = store.getDefaultFolder();
		return createFolder(root, pathName, root.getSeparator(), createHierarchy);
	}
	
	public static Folder createFolder(final Folder parent, final String pathName, final char separator, final boolean createHierarchy) throws MessagingException {
		Check.notNull(parent, "parent");
		Folder folder = parent.getFolder(pathName);
		if (folder.exists()) {
			return folder;
		} else {
			return createFolderRecursive(parent, pathName, separator, createHierarchy);
		}
	}
	*/
	
	public static Folder getFolder(final Store store, final String relativeName, final boolean createIfNecessary) throws MessagingException {
		final Folder root = store.getDefaultFolder();
		return getFolder(root, relativeName, root.getSeparator(), createIfNecessary);
	}
	
	public static Folder getFolder(final Folder parent, final String relativeName, final char separator, final boolean createIfNecessary) throws MessagingException {
		Check.notNull(parent, "parent");
		Check.notEmpty(relativeName, "relativeName");
		Check.notNull(separator, "separator");
		Folder folder = parent.getFolder(relativeName);
		if (folder.exists()) {
			return folder;
		} else {
			return internalCreateFolderRecursive(parent, relativeName, separator, createIfNecessary);
		}
	}
	
	private static Folder internalCreateFolderRecursive(final Folder parent, final String relativeName, final char separator, final boolean createHierarchy) throws MessagingException {
		Check.notNull(parent, "parent");
		Check.notEmpty(relativeName, "relativeName");
		Check.notNull(separator, "separator");
		final String name = StringUtils.substringBefore(relativeName, separator);
		final String newPathName = StringUtils.substringAfter(relativeName, separator);
		
		Folder folder = parent.getFolder(name);
		if (!folder.exists()) {
			if (createHierarchy) {
				folder.create(Folder.HOLDS_MESSAGES | Folder.HOLDS_FOLDERS);
			} else {
				throw new MessagingException("Folder '" + folder.getFullName() + "' not exists");
			}
		}
		return StringUtils.isBlank(newPathName) ? folder : internalCreateFolderRecursive(folder, newPathName, separator, createHierarchy);
	}
	
	public static Folder createFolderIfNecessary(final Store store, final String name) throws MessagingException {
		final Folder root = store.getDefaultFolder();
		return createFolderIfNecessary(root, name);
	}
	
	public static Folder createFolderIfNecessary(final Folder parent, final String name) throws MessagingException {
		Check.notNull(parent, "parent");
		Check.notEmpty(name, "name");
		Folder folder = parent.getFolder(name);
		if (!folder.exists()) {
			folder.create(Folder.HOLDS_MESSAGES | Folder.HOLDS_FOLDERS);
		}
		return folder;
	}
	
	public static void applyAcl(final Folder folder, final String user, final String rights) throws MessagingException {
		Check.notNull(folder, "folder");
		Check.notEmpty(user, "user");
		Check.notEmpty(rights, "rights");
		if (!(folder instanceof IMAPFolder)) throw new MessagingException("Folder '" + folder.getFullName() + "' must be of IMAP type");
		((IMAPFolder)folder).addACL(new ACL(user, new Rights(rights)));
	}
	
	public static Folder getFirstNamespaceFolder(final Store store) throws MessagingException {
		Check.notNull(store, "store");
		Folder uns[] = store.getUserNamespaces("");
		return uns.length > 0 ? uns[0] : null;
	}
	
	public static void closeQuietly(final Folder folder, final boolean expunge) {
		try {
			close(folder, expunge);
		} catch (MessagingException ex) { /* Do nothing... */ }
	}
	
	public static void close(final Folder folder, final boolean expunge) throws MessagingException {
		if (folder != null && folder.isOpen()) folder.close(expunge);
	}
	
	public static void deleteMessage(final Message message) throws MessagingException {
		Check.notNull(message, "message");
		message.setFlag(Flags.Flag.DELETED, true);
	}
	
	public static void setMessageSeen(final Message message, final boolean seen) throws MessagingException {
		Check.notNull(message, "message");
		message.setFlag(Flags.Flag.SEEN, seen);
	}
	
	public static void copyMessage(final Message message, final Folder from, final Folder to) throws MessagingException {
		Check.notNull(message, "message");
		Check.notNull(to, "to");
		copyMessages(new Message[]{message}, from, to);
	}
	
	public static void copyMessages(final Message[] messages, final Folder from, final Folder to) throws MessagingException {
		Check.notNull(messages, "messages");
		Check.notNull(to, "to");
		if (from != null) {
			from.copyMessages(messages, to);
		} else {
			to.appendMessages(messages);
		}
	}
	
	public static void moveMessage(final Message message, final Folder from, final Folder to, final boolean expunge) throws MessagingException {
		Check.notNull(message, "message");
		Check.notNull(to, "to");
		moveMessages(new Message[]{message}, from, to, expunge);
	}
	
	public static void moveMessages(final Message[] messages, final Folder from, final Folder to, final boolean expunge) throws MessagingException {
		Check.notNull(messages, "messages");
		Check.notNull(to, "to");
		copyMessages(messages, from, to);
		if (from != null) {
			from.setFlags(messages, new Flags(Flags.Flag.DELETED), true);
			if (expunge) from.expunge();
		}
	}
	
	public static void issueNoop(final Folder folder) throws MessagingException {
		Check.notNull(folder, "folder");
		if (folder instanceof IMAPFolder) {
			IMAPFolder ifolder = (IMAPFolder)folder;
			ifolder.doCommand((p) -> {
				p.simpleCommand("NOOP", null);
				return null;
			});
		}
	}
	
	public static Message[] getMessagesByMessageID(final Folder folder, final String messageId) throws MessagingException {
		Check.notNull(folder, "folder");
		Check.notNull(messageId, "messageId");
		boolean wasOpen = folder.isOpen();
		openFolder(folder, false);
		Message[] result = folder.search(new HeaderTerm(MimeUtils.HEADER_MESSAGE_ID, messageId));
		if (!wasOpen) closeQuietly(folder, false);
		return result;
	}
	
	public static Message getMessageByMessageID(final Folder folder, final String messageId) throws MessagingException {
		final Message[] result = getMessagesByMessageID(folder, messageId);
		return (result.length > 0) ? result[0] : null;
	}
	
	/**
	 * Strips, if matches, specified prefix from passed folder name.
	 * @param folderName The folder name to be evaluated.
	 * @param folderPrefix The configured prefix. Can be null or empty.
	 * @return Stripped folder name
	 */
	public static String toBaseFolderName(final String folderName, final String folderPrefix) {
		return StringUtils.isBlank(folderPrefix) ? folderName : StringUtils.removeStart(folderName, folderPrefix);
	}
	
	/**
	 * 
	 * @param user The target username.
	 * @return 
	 */
	public static String toPlainUser(final String user) {
		Check.notNull(user, "user");
		return StringUtils.substringBeforeLast(user, "@");
	}
	
	/**
	 * Extracts the trailing internet name suffix (@domain), if present.
	 * @param user The target username.
	 * @return 
	 */
	public static String extractTrailingInternetNameSuffix(final String user) {
		Check.notNull(user, "user");
		String internetName = StringUtils.substringAfterLast(user, "@");
		return StringUtils.isBlank(internetName) ? "" : ("@" + internetName);
	}
}
