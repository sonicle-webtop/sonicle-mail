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
package com.sonicle.mail.imap.commands;

import com.sun.mail.iap.Argument;
import com.sun.mail.iap.BadCommandException;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.protocol.BASE64MailboxEncoder;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.imap.protocol.IMAPResponse;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author malbinola
 */
public class GetMetadata implements IMAPFolder.ProtocolCommand {
	private final String mbox;
	private final boolean shared;
	private final String key;
	
	public GetMetadata(String mbox, boolean shared, String key) {
		this.mbox = mbox;
		this.shared = shared;
		this.key = key;
	}
	
	@Override
	public Object doCommand(IMAPProtocol imapp) throws ProtocolException {
		if (!imapp.hasCapability("METADATA")) throw new BadCommandException("GETMETADATA not supported");
		
		// https://datatracker.ietf.org/doc/html/rfc5464#section-4.2
		// C: a GETMETADATA "INBOX" /private/comment
		// S: * METADATA "INBOX" (/private/comment "My own comment")
		// S: a OK GETMETADATA complete
		
		// encode the mbox as per RFC2060
		String embox = BASE64MailboxEncoder.encode(mbox);
		
		Argument args = new Argument();
		args.writeString(embox);
		
		Argument itemArgs = new Argument();
		
		itemArgs.writeString((shared ? "/shared" : "/private") + "/" + StringUtils.removeStart(key, "/"));
		args.writeArgument(itemArgs);
		
		Response[] responses = imapp.command("GETMETADATA", args);
		Response status = responses[responses.length-1];
		if (!status.isOK()) throw new ProtocolException("Error on GETMETADATA: " + status.toString());
		
		String key, value = null;
		if (responses[0] instanceof IMAPResponse) {
			IMAPResponse ir = (IMAPResponse)responses[0];
			if (ir.keyEquals("METADATA")) {
				String rmbox = ir.readAtomString();
				if (rmbox != null && rmbox.equalsIgnoreCase(mbox)) {
					ir.skipSpaces();
					if (ir.peekByte() == '(') {
						// Skip '(' char
						ir.readByte();
						// Read until ')' char
						while (ir.peekByte() != ')') {
							key = ir.readAtomString();
							ir.skipSpaces();
							value = ir.readString();
							ir.skipSpaces();
						}
						// Skip ')' char
						ir.readByte();
					}
				}
			}
		}
		
		imapp.notifyResponseHandlers(responses);
		imapp.handleResult(status);
		return value;
	}
}
