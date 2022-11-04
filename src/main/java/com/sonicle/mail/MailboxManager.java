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

import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Properties;
import net.sf.qualitycheck.Check;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author malbinola
 */
public class MailboxManager {
	private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(MailboxManager.class);
	private final StoreHostParams hostParams;
	private final MailboxConfig mailboxConfig;
	private final Properties defaultProperties;
	
	public MailboxManager(final StoreHostParams hostParams, final MailboxConfig mailboxConfig, final Properties defaultProperties) {
		this.hostParams = Check.notNull(hostParams, "hostParams");
		this.mailboxConfig = Check.notNull(mailboxConfig, "mailboxConfig");
		this.defaultProperties = Check.notNull(defaultProperties, "defaultProperties");
	}
	
	public void deleteMailbox(final String user) throws MessagingException, GeneralSecurityException {
		Check.notEmpty(user, "user");
		
		Store store = null;
		try {
			store = StoreUtils.open(StoreUtils.createSession(hostParams, 1, defaultProperties), hostParams.getProtocol());
			
			final String plainUser = StoreUtils.toPlainUser(user);
			final String trailingSuffix = StoreUtils.extractTrailingInternetNameSuffix(user);
			final Folder otherUsersFolder = StoreUtils.getFirstNamespaceFolder(store);
			if (otherUsersFolder == null) throw new MessagingException("Target UserNamespace folder is missing");
			
			final Folder userParentFolder = otherUsersFolder.getFolder(plainUser);
			if (userParentFolder.exists()) {
				for (Folder folder : userParentFolder.list()) {
					System.out.println("Deleting folder: " + folder.getName());
					//folder.delete(true);
				}
			}
			
		} finally {
			StoreUtils.closeQuietly(store);
		}
	}
	
	public void createMailbox(final String user) throws MessagingException, GeneralSecurityException {
		createMailbox(user, null);
	}
	
	public void createMailbox(final String user, final Collection<String> folders) throws MessagingException, GeneralSecurityException {
		Check.notEmpty(user, "user");
		
		Store store = null;
		try {
			store = StoreUtils.open(StoreUtils.createSession(hostParams, 1, defaultProperties), hostParams.getProtocol());
			createMailboxRootFolder(store, user);
			
			if (folders != null) {
				final String plainUser = StoreUtils.toPlainUser(user);
				final String trailingSuffix = StoreUtils.extractTrailingInternetNameSuffix(user);
				final Folder otherUsersFolder = StoreUtils.getFirstNamespaceFolder(store);
				if (otherUsersFolder == null) throw new MessagingException("Target UserNamespace folder is missing");
				final char sep = otherUsersFolder.getSeparator();
				
				for (String folder : folders) {
					final String name = plainUser + sep + StoreUtils.toBaseFolderName(folder, mailboxConfig.getUserFolderPrefix()) + trailingSuffix;
					StoreUtils.createFolderIfNecessary(otherUsersFolder, name);
				}
			}
			
		} finally {
			StoreUtils.closeQuietly(store);
		}
	}
	
	private Folder createMailboxRootFolder(final Store store, final String user) throws MessagingException {
		char sep = store.getDefaultFolder().getSeparator();
		String folderName = "user" + sep + user;
		
		LOGGER.debug("Getting folder '{}'", folderName);
		Folder mailboxFolder = store.getFolder(folderName);
		if (!mailboxFolder.exists()) {
			LOGGER.debug("Folder '{}' does not exist, creating it...", folderName);
			mailboxFolder.create(Folder.HOLDS_FOLDERS);
		}
		
		LOGGER.debug("Applying ACL to folder '{}'", folderName, user, StoreUtils.FOLDER_FULL_RIGHTS);
		StoreUtils.applyAcl(mailboxFolder, user, StoreUtils.FOLDER_FULL_RIGHTS);
		LOGGER.debug("Applying ACL to folder '{}'", folderName, hostParams.getUsername(), StoreUtils.FOLDER_FULL_RIGHTS);
		StoreUtils.applyAcl(mailboxFolder, hostParams.getUsername(), StoreUtils.FOLDER_FULL_RIGHTS);
		return mailboxFolder;
	}
	
	public static class MailboxConfig {
		private final String userFolderPrefix;
		
		public MailboxConfig(String userFolderPrefix) {
			this.userFolderPrefix = userFolderPrefix;
		}
		
		public String getUserFolderPrefix() {
			return userFolderPrefix;
		}
	}
}
