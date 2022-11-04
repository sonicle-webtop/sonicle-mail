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

import com.sonicle.mail.MimeUtils;
import com.sonicle.mail.NamedDataSource;
import com.sonicle.mail.email.AttachmentResource;
import com.sonicle.mail.email.EmailMessage;
import com.sonicle.mail.email.Recipient;
import jakarta.activation.DataHandler;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimePart;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.internet.ParameterList;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import net.sf.qualitycheck.Check;
import org.apache.commons.lang3.StringUtils;

/**
 * Heavily inspired by https://github.com/bbottema/simple-java-mail
 * @author malbinola
 */
public abstract class MimeMessageConstructor {
	private static final String CHARACTER_ENCODING = StandardCharsets.UTF_8.name();
	
	abstract boolean isCompatibleWithEmail(EmailMessage email);
	abstract void populateMimeMessageMultipartStructure(MimeMessage  message, EmailMessage email) throws MessagingException;
	
	final MimeMessage populateMimeMessage(final EmailMessage email, Session session) throws MessagingException, UnsupportedEncodingException {
		Check.notNull(email, "email");
		Check.notNull(session, "session");
		
		MimeMessage message = new MimeMessage(session) {
			@Override
			protected void updateMessageID() throws MessagingException {
				if (StringUtils.isBlank(email.getId())) {
					super.updateMessageID();
				} else {
					setHeader(MimeUtils.HEADER_MESSAGE_ID, email.getId());
				}
			}
			@Override
			public String toString() {
				try {
					return "MimeMessage<id:" + super.getMessageID() + ", subject:" + super.getSubject() + ">";
				} catch (MessagingException ex) {
					throw new IllegalStateException("Should never happen!");
				}
			}
		};
		
		setSubject(email, message);
		setFrom(email, message);
		setReplyTo(email, message);
		setRecipients(email, message);
		
		populateMimeMessageMultipartStructure(message, email);
		
		setHeaders(email, message);
		message.setSentDate(email.getSentDate() != null ? email.getSentDate() : new Date());
		
		if (email.getBounceToRecipient() != null) {
			// display name not applicable: https://tools.ietf.org/html/rfc5321#section-4.1.2
			//message = new ImmutableDelegatingSMTPMessage(message, email.getBounceToRecipient().getAddress());
		}
		
		return message;
	}
	
	static void setSubject(final EmailMessage email, final MimeMessage message) throws MessagingException {
		Check.notNull(email, "email");
		Check.notNull(message, "message");
		message.setSubject(email.getSubject(), CHARACTER_ENCODING);
	}
	
	static void setFrom(final EmailMessage email, final MimeMessage message) throws MessagingException {
		Check.notNull(email, "email");
		Check.notNull(message, "message");
		if (email.getFromRecipient() != null) {
			final InternetAddress ia = email.getFromRecipient().asInternetAddress();
			if (ia == null) throw new MessagingException("Invalid from address '" + email.getFromRecipient().getAddress() + "'");
			message.setFrom(ia);
		}
	}
	
	static void setRecipients(final EmailMessage email, final Message message) throws MessagingException {
		Check.notNull(email, "email");
		Check.notNull(message, "message");
		for (final Recipient recipient : email.getRecipients()) {
			final Address address = recipient.asInternetAddress();
			if (address == null) throw new MessagingException("Invalid recipient address '" + recipient.getAddress() + "'");
			message.addRecipient(recipient.getType(), address);
		}
	}
	
	static void setReplyTo(final EmailMessage email, final Message message) throws MessagingException {
		Check.notNull(email, "email");
		Check.notNull(message, "message");
		if (email.getReplyToRecipient() != null) {
			final InternetAddress ia = email.getReplyToRecipient().asInternetAddress();
			if (ia == null) throw new MessagingException("Invalid replyTo address '" + email.getReplyToRecipient().getAddress() + "'");
			message.setReplyTo(new Address[]{ia});
		}
	}
	
	static void setTexts(final EmailMessage email, final MimeMultipart multipartAlternativeMessages) throws MessagingException {
		Check.notNull(email, "email");
		Check.notNull(multipartAlternativeMessages, "multipartAlternativeMessages");
		if (email.getPlainText() != null) {
			final MimeBodyPart messagePart = new MimeBodyPart();
			messagePart.setText(email.getPlainText(), CHARACTER_ENCODING);
			messagePart.addHeader(MimeUtils.HEADER_CONTENT_TRANSFER_ENCODING, email.getContentTransferEncoding().toString());
			multipartAlternativeMessages.addBodyPart(messagePart);
		}
		if (email.getHTMLText() != null) {
			final MimeBodyPart messagePartHTML = new MimeBodyPart();
			messagePartHTML.setContent(email.getHTMLText(), "text/html; charset=\"" + CHARACTER_ENCODING + "\"");
			messagePartHTML.addHeader(MimeUtils.HEADER_CONTENT_TRANSFER_ENCODING, email.getContentTransferEncoding().toString());
			multipartAlternativeMessages.addBodyPart(messagePartHTML);
		}
		if (email.getCalendarText() != null && email.getCalendarMethod() != null) {
			final MimeBodyPart messagePartCalendar = new MimeBodyPart();
			messagePartCalendar.setContent(email.getCalendarText(), "text/calendar; charset=\"" + CHARACTER_ENCODING + "\"; method=\"" + email.getCalendarMethod().toString() + "\"");
			messagePartCalendar.addHeader(MimeUtils.HEADER_CONTENT_TRANSFER_ENCODING, email.getContentTransferEncoding().toString());
			multipartAlternativeMessages.addBodyPart(messagePartCalendar);
		}
	}
	
	static void setTexts(final EmailMessage email, final MimePart messagePart) throws MessagingException {
		Check.notNull(email, "email");
		Check.notNull(messagePart, "messagePart");
		if (email.getPlainText() != null) {
			messagePart.setText(email.getPlainText(), CHARACTER_ENCODING);
		}
		if (email.getHTMLText() != null) {
			messagePart.setContent(email.getHTMLText(), "text/html; charset=\"" + CHARACTER_ENCODING + "\"");
		}
		if (email.getCalendarText() != null && email.getCalendarMethod() != null) {
			messagePart.setContent(email.getCalendarText(), "text/calendar; charset=\"" + CHARACTER_ENCODING + "\"; method=\"" + email.getCalendarMethod().toString() + "\"");
		}
		messagePart.addHeader(MimeUtils.HEADER_CONTENT_TRANSFER_ENCODING, email.getContentTransferEncoding().toString());
	}
	
	static void setEmbeddedImages(final EmailMessage email, final MimeMultipart multipartRelated) throws MessagingException {
		Check.notNull(email, "email");
		Check.notNull(multipartRelated, "multipartRelated");
		for (final AttachmentResource embeddedImage : email.getEmbeddedImages()) {
			multipartRelated.addBodyPart(getBodyPartFromDatasource(embeddedImage, Part.INLINE));
		}
	}
	
	static void setAttachments(final EmailMessage email, final MimeMultipart multipartRoot) throws MessagingException {
		Check.notNull(email, "email");
		Check.notNull(multipartRoot, "multipartRoot");
		for (final AttachmentResource resource : email.getAttachments()) {
			multipartRoot.addBodyPart(getBodyPartFromDatasource(resource, Part.ATTACHMENT));
		}
	}
	
	static void setHeaders(final EmailMessage email, final Message message) throws UnsupportedEncodingException, MessagingException {
		Check.notNull(email, "email");
		Check.notNull(message, "message");
		// add headers (for raw message headers we need to 'fold' them using MimeUtility
		for (final Map.Entry<String, Collection<String>> header : email.getHeaders().entrySet()) {
			for (final String headerValue : header.getValue()) {
				final String headerName = header.getKey();
				final String headerValueEncoded = MimeUtility.encodeText(headerValue, CHARACTER_ENCODING, null);
				final String foldedHeaderValue = MimeUtility.fold(headerName.length() + 2, headerValueEncoded);
				message.addHeader(header.getKey(), foldedHeaderValue);
			}
		}
		
		if (email.isUseDispositionNotificationTo()) {
			final Recipient dispositionTo = Check.notNull(email.getDispositionNotificationTo(), "dispositionNotificationTo");
			final Address address = dispositionTo.asInternetAddress();
			if (address == null) throw new MessagingException("Invalid dispositionTo address '" + dispositionTo.getAddress() + "'");
			message.setHeader(MimeUtils.HEADER_DISP_NOTIFICATION_TO, address.toString());
		}
		if (email.isUseReturnReceiptTo()) {
			final Recipient returnReceiptTo = Check.notNull(email.getReturnReceiptTo(), "returnReceiptTo");
			final Address address = returnReceiptTo.asInternetAddress();
			if (address == null) throw new MessagingException("Invalid returnReceiptTo address '" + returnReceiptTo.getAddress() + "'");
			message.setHeader(MimeUtils.HEADER_RETURN_RECEIPT_TO, address.toString());
		}
	}
	
	static void configureForwarding(final EmailMessage email, final MimeMultipart multipartRootMixed) throws MessagingException {
		Check.notNull(email, "email");
		Check.notNull(multipartRootMixed, "multipartRootMixed");
		if (email.getEmailToForward() != null) {
			final BodyPart fordwardedMessage = new MimeBodyPart();
			fordwardedMessage.setContent(email.getEmailToForward(), "message/rfc822");
			multipartRootMixed.addBodyPart(fordwardedMessage);
		}
	}
	
	private static BodyPart getBodyPartFromDatasource(final AttachmentResource attachmentResource, final String dispositionType) throws MessagingException {
		Check.notNull(attachmentResource, "attachmentResource");
		Check.notNull(dispositionType, "dispositionType");
		final BodyPart attachmentPart = new MimeBodyPart();
		
		// Setting headers isn't working nicely using the javax mail API, so let's do that manually
		final String resourceName = determineResourceName(attachmentResource, true);
		final String fileName = determineResourceName(attachmentResource, false);
		
		attachmentPart.setDataHandler(new DataHandler(new NamedDataSource(fileName, attachmentResource.getDataSource())));
		attachmentPart.setFileName(fileName);
		final String contentType = attachmentResource.getDataSource().getContentType();
		ParameterList pl = new ParameterList();
		pl.set("filename", fileName);
		pl.set("name", fileName);
		
		attachmentPart.setHeader(MimeUtils.HEADER_CONTENT_TYPE, contentType + pl);
		attachmentPart.setHeader(MimeUtils.HEADER_CONTENT_ID, String.format("<%s>", resourceName));
		if (attachmentResource.getContentTransferEncoding() != null) {
			attachmentPart.setHeader("Content-Transfer-Encoding", attachmentResource.getContentTransferEncoding().toString());
		}
		attachmentPart.setDisposition(dispositionType);
		
		return attachmentPart;
	}
	
	static String determineResourceName(final AttachmentResource attachmentResource, final boolean encodeResourceName) {
		final String datasourceName = attachmentResource.getDataSource().getName();
		String resourceName;
		if (!StringUtils.isBlank(attachmentResource.getName())) {
			resourceName = attachmentResource.getName();
		} else if (!StringUtils.isBlank(datasourceName)) {
			resourceName = datasourceName;
		} else {
			resourceName = "resource" + UUID.randomUUID();
		}
		if (!StringUtils.isBlank(datasourceName)) {
			resourceName = possiblyAddExtension(datasourceName, resourceName);
		}
		try {
			return encodeResourceName ? MimeUtils.encodeText(resourceName) : resourceName;
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalArgumentException(ex.getMessage(), ex);
		}
	}
	
	private static String possiblyAddExtension(final String datasourceName, String resourceName) {
		final String possibleFilename = datasourceName;
		if (!resourceName.contains(".") && possibleFilename.contains(".")) {
			final String extension = possibleFilename.substring(possibleFilename.lastIndexOf("."));
			if (!resourceName.endsWith(extension)) {
				resourceName += extension;
			}
		}
		return resourceName;
	}
	
	static boolean emailContainsMixedContent(EmailMessage email) {
		Check.notNull(email, "email");
		return !email.getAttachments().isEmpty();// || email.getEmailToForward() != null;
	}
	
	static boolean emailContainsRelatedContent(EmailMessage email) {
		Check.notNull(email, "email");
		return !email.getEmbeddedImages().isEmpty();
	}
	
	static boolean emailContainsAlternativeContent(EmailMessage email) {
		Check.notNull(email, "email");
		return (email.getPlainText() != null ? 1 : 0) +
				(email.getHTMLText() != null ? 1 : 0) +
				(email.getCalendarText() != null ? 1 : 0) > 1;
	}
}
