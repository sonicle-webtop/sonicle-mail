/*
 * sonicle-mail is is a helper library developed by Sonicle S.r.l.
 * Copyright (C) 2018 Sonicle S.r.l.
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
 * display the words "Copyright (C) 2018 Sonicle S.r.l.".
 */
package com.sonicle.mail.cyrus;

import com.sonicle.commons.InternetAddressUtils;
import com.sonicle.commons.LangUtils;
import com.sun.mail.imap.ACL;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.Rights;
import java.util.Properties;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author malbinola
 */
public class CyrusManager {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(CyrusManager.class);
	public static final String FOLDER_ALL_RIGHTS = "lrswipcda";
	
	private final String host;
	private final int port;
	private final String protocol;
	private final String adminUser;
	private final String adminPassword;
	private final Properties properties;
	
	public CyrusManager(String host, int port, String protocol, String adminUser, String adminPassword) {
		this(host, port, protocol, adminUser, adminPassword, null);
	}
	
	public CyrusManager(String host, int port, String protocol, String adminUser, String adminPassword, Properties properties) {
		this.host = host;
		this.port = port;
		this.protocol = protocol;
		this.adminUser = adminUser;
		this.adminPassword = adminPassword;
		this.properties = properties;
	}
	
	public void addMailbox(String user) throws MessagingException {
		Store store = null;
		try {
			store = createStore();
			store.connect(host, port, adminUser, adminPassword);
			addUserFolder(store, StringUtils.lowerCase(user));
		} finally {
			closeQuietly(store);
		}
	}
	
	private Store createStore() throws NoSuchProviderException {
		Properties props = new Properties((properties == null) ? System.getProperties() : properties);
		props.setProperty("mail.store.protocol", protocol);
		props.setProperty("mail.store.port", String.valueOf(port));
		return Session.getInstance(props, null).getStore(protocol);
	}
	
	private void addUserFolder(Store store, String username) throws MessagingException {
		char sep = store.getDefaultFolder().getSeparator();
		String folderName = "user" + sep + username;
		
		logger.debug("Getting folder '{}'", folderName);
		Folder folder = store.getFolder(folderName);
		if (!folder.exists()) {
			logger.debug("Folder '{}' does not exist, creating it...", folderName);
			folder.create(Folder.HOLDS_FOLDERS);
		}
		
		applyAcl(folder, username, FOLDER_ALL_RIGHTS);
		applyAcl(folder, adminUser, FOLDER_ALL_RIGHTS);
	}
	
	private void applyAcl(Folder folder, String targetUsername, String targetRights) throws MessagingException {
		if (folder instanceof IMAPFolder) {
			logger.debug("Applying ACL [{}, {}, {}]", folder.getName(), targetUsername, targetRights);
			((IMAPFolder)folder).addACL(new ACL(targetUsername, new Rights(targetRights)));
		}
	}
	
	private void closeQuietly(Store store) {
		if (store != null) try { store.close(); } catch (MessagingException ex) { /* Do nothing... */ }
	}
}
