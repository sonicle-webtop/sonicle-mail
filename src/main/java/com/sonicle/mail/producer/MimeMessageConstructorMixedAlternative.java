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
package com.sonicle.mail.producer;

import com.sonicle.mail.email.EmailMessage;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

/**
 * Produces a MimeMessage with the following structure:
 * - mixed (root)
 *     - plain text || html text || iCalendar text
 *     - forwarded message
 *     - attachments
 * 
 * Heavily inspired by https://github.com/bbottema/simple-java-mail
 * @author malbinola
 */
public class MimeMessageConstructorMixedAlternative extends MimeMessageConstructor {

	@Override
	boolean isCompatibleWithEmail(EmailMessage email) {
		return emailContainsMixedContent(email) && !emailContainsRelatedContent(email) && emailContainsAlternativeContent(email);
	}

	@Override
	void populateMimeMessageMultipartStructure(MimeMessage message, EmailMessage email) throws MessagingException {
		MultipartStructureWrapper multipartStructureWrapper = new MultipartStructureWrapper();
		setTexts(email, multipartStructureWrapper.multipartAlternativeMessages);
		configureForwarding(email, multipartStructureWrapper.multipartRootMixed);
		setAttachments(email, multipartStructureWrapper.multipartRootMixed);
		message.setContent(multipartStructureWrapper.multipartRootMixed);
	}
	
	private static class MultipartStructureWrapper {
		private final MimeMultipart multipartRootMixed;
		private final MimeMultipart multipartAlternativeMessages;
		
		private MultipartStructureWrapper() {
			multipartRootMixed = new MimeMultipart("mixed");
			final MimeBodyPart contentAlternativeMessages = new MimeBodyPart();
			multipartAlternativeMessages = new MimeMultipart("alternative");
			try {
				// Construct mail structure
				multipartRootMixed.addBodyPart(contentAlternativeMessages);
				contentAlternativeMessages.setContent(multipartAlternativeMessages);
			} catch (final MessagingException ex) {
				throw new MimeMessageConstructException(ex.getMessage(), ex);
			}
		}
	}
}
