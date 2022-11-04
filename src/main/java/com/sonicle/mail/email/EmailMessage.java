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
package com.sonicle.mail.email;

import jakarta.mail.internet.MimeMessage;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import net.sf.qualitycheck.Check;

/**
 * Heavily inspired by https://github.com/bbottema/simple-java-mail
 * @author malbinola
 */
public class EmailMessage {
	private final String id;
	private final Recipient fromRecipient;
	private final Recipient replyToRecipient;
	private final Recipient bounceToRecipient;
	private final List<Recipient> recipients;
	private final String subject;
	private transient final MimeMessage emailToForward;
	private final String textPlain;
	private final String textHTML;
	private final CalendarMethod calendarMethod;
	private final String textCalendar;
	private final List<AttachmentResource> embeddedImages;
	private final List<AttachmentResource> attachments;
	private final Map<String, Collection<String>> headers;
	private final boolean useDispositionNotificationTo;
	private final Recipient dispositionNotificationTo;
	private final boolean useReturnReceiptTo;
	private final Recipient returnReceiptTo;
	private final Date sentDate;
	private final ContentTransferEncoding contentTransferEncoding;
	
	public EmailMessage(final EmailPopulatingBuilder builder) {
		Check.notNull(builder, "builder");
		
		id = builder.getId();
		fromRecipient = builder.getFromRecipient();
		replyToRecipient = builder.getReplyToRecipient();
		bounceToRecipient = builder.getBounceToRecipient();
		recipients = Collections.unmodifiableList(builder.getRecipients());
		emailToForward = builder.getEmailToForward();
		subject = builder.getSubject();
		textPlain = /*smimeMerge ? smimeSignedEmail.getTextPlain() :*/ builder.getTextPlain();
		textHTML = /*smimeMerge ? smimeSignedEmail.getTextHTML() :*/ builder.getTextHTML();
		calendarMethod = builder.getCalendarMethod();
		textCalendar = builder.getTextCalendar();
		embeddedImages = Collections.unmodifiableList(builder.getEmbeddedImages());
		attachments = Collections.unmodifiableList(builder.getAttachments());
		headers = Collections.unmodifiableMap(builder.getHeaders());
		contentTransferEncoding = builder.getContentTransferEncoding();
		useDispositionNotificationTo = builder.isUseDispositionNotificationTo();
		useReturnReceiptTo = builder.isUseReturnReceiptTo();
		sentDate = builder.getSentDate();
		
		if (useDispositionNotificationTo && (builder.getDispositionNotificationTo() == null)) {
			if (builder.getReplyToRecipient() != null) {
				dispositionNotificationTo = builder.getReplyToRecipient();
			} else {
				dispositionNotificationTo = builder.getFromRecipient();
			}
		} else {
			dispositionNotificationTo = builder.getDispositionNotificationTo();
		}
		
		if (useReturnReceiptTo && (builder.getReturnReceiptTo() == null)) {
			if (builder.getReplyToRecipient() != null) {
				returnReceiptTo = builder.getReplyToRecipient();
			} else {
				returnReceiptTo = builder.getFromRecipient();
			}
		} else {
			returnReceiptTo = builder.getReturnReceiptTo();
		}
	}

	public String getId() {
		return id;
	}

	public Recipient getFromRecipient() {
		return fromRecipient;
	}

	public Recipient getReplyToRecipient() {
		return replyToRecipient;
	}

	public Recipient getBounceToRecipient() {
		return bounceToRecipient;
	}
	
	public List<Recipient> getRecipients() {
		return recipients;
	}
	
	public String getSubject() {
		return subject;
	}
	
	public MimeMessage getEmailToForward() {
		return emailToForward;
	}
	
	public String getPlainText() {
		return textPlain;
	}

	public String getHTMLText() {
		return textHTML;
	}
	
	public CalendarMethod getCalendarMethod() {
		return calendarMethod;
	}
	
	public String getCalendarText() {
		return textCalendar;
	}
	
	public List<AttachmentResource> getEmbeddedImages() {
		return embeddedImages;
	}
	
	public List<AttachmentResource> getAttachments() {
		return attachments;
	}
	
	/*
	public List<AttachmentResource> getDecryptedAttachments() {
		return decryptedAttachments;
	}
	*/
	
	public Map<String, Collection<String>> getHeaders() {
		return headers;
	}
	
	public ContentTransferEncoding getContentTransferEncoding() {
		return contentTransferEncoding;
	}
	
	public boolean isUseDispositionNotificationTo() {
		return useDispositionNotificationTo;
	}
	
	public Recipient getDispositionNotificationTo() {
		return dispositionNotificationTo;
	}
	
	public boolean isUseReturnReceiptTo() {
		return useReturnReceiptTo;
	}
	
	public Recipient getReturnReceiptTo() {
		return returnReceiptTo;
	}
	
	public Date getSentDate() {
		return sentDate != null ? new Date(sentDate.getTime()) : null;
	}
}
