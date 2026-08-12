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
package com.sonicle.mail.cyrus;

import com.sonicle.commons.LangUtils;
import com.sonicle.mail.PropsBuilder;
import com.sonicle.mail.StoreHostParams;
import com.sonicle.mail.StoreProtocol;
import com.sonicle.mail.StoreUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import java.io.Console;
import java.security.GeneralSecurityException;
import java.util.Properties;
import java.util.Set;

/**
 *
 * @author malbinola
 */
public class Main {
	
	public static void main(String[] args) {
		if (args.length < 4 || args.length > 7) {
			System.err.println("Usage: CyrusManager <host> <port> <protocol> <adminUser> [adminPassword] <userToCreate>");
			System.err.println("If adminPassword is omitted, it will be requested interactively.");
			System.exit(1);
		}

		String host = args[0];
		int port = Integer.parseInt(args[1]);
		String protocol = args[2];
		String adminUser = args[3];

		String adminPassword;
		String userToCreate;
		Set<String> extraAcls = null;
		
		// Determine how many positional args we have, accounting for the optional
		// password and the optional trailing comma-separated parameter.
		// Base required args: host, port, protocol, adminUser -> already consumed (indices 0-3)
		// Remaining args (from index 4 onward) can be:
		//   [adminPassword] userToCreate [extraParam]
		// We figure out presence of password by checking total remaining count.
		int remaining = args.length - 4;
		if (remaining == 1) {
			// only userToCreate, password omitted, no extra param
			adminPassword = readPasswordFromConsole();
			userToCreate = args[4];
		} else if (remaining == 2) {
			// could be: password + userToCreate  OR  userToCreate + extraParam
			// Heuristic: if password was omitted, extra param is present instead.
			// Since we can't reliably distinguish, we require explicit password when extraParam is used.
			// Assume: adminPassword userToCreate (most common case)
			adminPassword = args[4];
			userToCreate = args[5];
		} else if (remaining == 3) {
			// adminPassword userToCreate extraParam
			adminPassword = args[4];
			userToCreate = args[5];
			extraAcls = LangUtils.parseStringAsSet(args[6]);
		} else {
			System.err.println("Not enough arguments.");
			System.exit(1);
			return;
		}
				
/*
		if (args.length == 6) {
			// password provided explicitly
			adminPassword = args[4];
			userToCreate = args[5];
		} else if (args.length == 5) {
			// password omitted, last argument is the user to create
			adminPassword = readPasswordFromConsole();
			userToCreate = args[4];
		} else {
			System.err.println("Not enough arguments.");
			System.exit(1);
			return; // for the compiler
		}
		*/
		
		Properties props = new PropsBuilder(System.getProperties())
			.withCustomizedIMAPFolderClasses()
			.build();
		
		try {
			final StoreHostParams hostParams = new StoreHostParams(host, port, StoreProtocol.parse(protocol, false))
				.withUsername(adminUser)
				.withPassword(adminPassword);
			
			Set<String> acls = LangUtils.asSet(
				// Full-rights for Admin: here the username used to connection is the admin (use it as is)!
				adminUser + ":" + StoreUtils.FOLDER_FULL_RIGHTS,
				// Full-rights for the owning user
				userToCreate + ":" + StoreUtils.FOLDER_FULL_RIGHTS
			);
			if (extraAcls != null && !extraAcls.isEmpty()) {
				acls.addAll(extraAcls);
			}
			StoreUtils.createMailbox(StoreUtils.createSession(hostParams, 1, props), hostParams.getProtocol(), "user", userToCreate, acls);
			
		} catch (GeneralSecurityException | MessagingException e) {
			System.err.println("Error while creating the mailbox: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		
		Store store = null;
		try {
			final String prefix = "user";
			final StoreHostParams hostParams = new StoreHostParams(host, port, StoreProtocol.parse(protocol, false))
				.withUsername(userToCreate)
				.withSASLImpersonate(adminUser, adminPassword);
			
			store = StoreUtils.open(StoreUtils.createSession(hostParams, 1, props), hostParams.getProtocol());
			// https://github.com/cyrusimap/cyrus-imapd/issues/2592
			StoreUtils.createFolderIfNecessary(store, StoreUtils.toBaseFolderName("Sent", prefix), LangUtils.asSet("/specialuse:\\Sent"));
			StoreUtils.createFolderIfNecessary(store, StoreUtils.toBaseFolderName("Drafts", prefix), LangUtils.asSet("/specialuse:\\Drafts"));
			StoreUtils.createFolderIfNecessary(store, StoreUtils.toBaseFolderName("Trash", prefix), LangUtils.asSet("/specialuse:\\Trash"));
			StoreUtils.createFolderIfNecessary(store, StoreUtils.toBaseFolderName("Spam", prefix), LangUtils.asSet("/specialuse:\\Junk"));
			StoreUtils.createFolderIfNecessary(store, StoreUtils.toBaseFolderName("Archive", prefix), LangUtils.asSet("/specialuse:\\Archive"));
			
		} catch (GeneralSecurityException | MessagingException e) {
			System.err.println("Error while creating folders: " + e.getMessage());
			e.printStackTrace();
			System.exit(2);
		} finally {
			StoreUtils.closeQuietly(store);
		}
	}

	private static String readPasswordFromConsole() {
		Console console = System.console();
		if (console == null) {
			// Fallback in case no console is available (e.g. running from some IDEs)
			System.err.println("Unable to access the console for secure password input.");
			System.err.println("Run the program from an actual terminal, or pass the password as an argument.");
			System.exit(1);
		}
		char[] passwordChars = console.readPassword("Admin password: ");
		return new String(passwordChars);
	}
}
