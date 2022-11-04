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

import jakarta.activation.DataSource;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 * @author malbinola
 */
public interface EmailPopulatingBuilder {
	
	EmailMessage build();
	EmailPopulatingBuilder from(final String fromAddress);
	EmailPopulatingBuilder from(final String name, final String fromAddress);
	EmailPopulatingBuilder from(final String fixedName, final InternetAddress fromAddress);
	EmailPopulatingBuilder from(final InternetAddress fromAddress);
	EmailPopulatingBuilder from(final Recipient recipient);
	EmailPopulatingBuilder withReplyTo(final String replyToAddress);
	EmailPopulatingBuilder withReplyTo(final String fixedName, final String replyToAddress);
	EmailPopulatingBuilder withReplyTo(final InternetAddress replyToAddress);
	EmailPopulatingBuilder withReplyTo(final String fixedName, InternetAddress replyToAddress);
	EmailPopulatingBuilder withReplyTo(final Recipient recipient);
	EmailPopulatingBuilder withBounceTo(final String bounceToAddress);
	EmailPopulatingBuilder withBounceTo(final String name, final String bounceToAddress);
	EmailPopulatingBuilder withBounceTo(final InternetAddress bounceToAddress);
	EmailPopulatingBuilder withBounceTo(final String name, final InternetAddress bounceToAddress);
	EmailPopulatingBuilder withBounceTo(final Recipient recipient);
	EmailPopulatingBuilder to(final Recipient... recipients);
	EmailPopulatingBuilder to(final Collection<Recipient> recipients);
	EmailPopulatingBuilder to(final String name, String oneOrMoreAddresses);
	EmailPopulatingBuilder to(final String oneOrMoreAddresses);
	EmailPopulatingBuilder to(final String name, final String... oneOrMoreAddressesEach);
	EmailPopulatingBuilder to(final String name, final Collection<String> oneOrMoreAddressesEach);
	EmailPopulatingBuilder toMultiple(final String... oneOrMoreAddressesEach);
	EmailPopulatingBuilder toMultiple(final Collection<String> oneOrMoreAddressesEach);
	EmailPopulatingBuilder toWithFixedName(final String name, final String... oneOrMoreAddressesEach);
	EmailPopulatingBuilder toWithDefaultName(final String name, final String... oneOrMoreAddressesEach);
	EmailPopulatingBuilder toWithFixedName(final String name, final Collection<String> oneOrMoreAddressesEach);
	EmailPopulatingBuilder toWithDefaultName(final String name, final Collection<String> oneOrMoreAddressesEach);
	EmailPopulatingBuilder to(final String name, InternetAddress address);
	EmailPopulatingBuilder to(final InternetAddress address);
	EmailPopulatingBuilder to(final String name, final InternetAddress... adresses);
	EmailPopulatingBuilder toAddresses(final String name, final Collection<InternetAddress> adresses);
	EmailPopulatingBuilder toMultiple(final InternetAddress... adresses);
	EmailPopulatingBuilder toMultipleAddresses(final Collection<InternetAddress> adresses);
	EmailPopulatingBuilder toAddressesWithFixedName(final String name, final InternetAddress... adresses);
	EmailPopulatingBuilder toAddressesWithDefaultName(final String name, final InternetAddress... adresses);
	EmailPopulatingBuilder toAddressesWithFixedName(final String name, final Collection<InternetAddress> adresses);
	EmailPopulatingBuilder toAddressesWithDefaultName(final String name, final Collection<InternetAddress> adresses);
	EmailPopulatingBuilder cc(final Recipient... recipients);
	EmailPopulatingBuilder cc(final Collection<Recipient> recipients);
	EmailPopulatingBuilder cc(final String name, String oneOrMoreAddresses);
	EmailPopulatingBuilder cc(final String oneOrMoreAddresses);
	EmailPopulatingBuilder cc(final String name, final String... oneOrMoreAddressesEach);
	EmailPopulatingBuilder cc(final String name, final Collection<String> oneOrMoreAddressesEach);
	EmailPopulatingBuilder ccMultiple(final String... oneOrMoreAddressesEach);
	EmailPopulatingBuilder ccAddresses(final Collection<String> oneOrMoreAddressesEach);
	EmailPopulatingBuilder ccWithFixedName(final String name, final String... oneOrMoreAddressesEach);
	EmailPopulatingBuilder ccWithDefaultName(final String name, final String... oneOrMoreAddressesEach);
	EmailPopulatingBuilder ccWithFixedName(final String name, final Collection<String> oneOrMoreAddressesEach);
	EmailPopulatingBuilder ccWithDefaultName(final String name, final Collection<String> oneOrMoreAddressesEach);
	EmailPopulatingBuilder cc(final String name, InternetAddress address);
	EmailPopulatingBuilder cc(final InternetAddress address);
	EmailPopulatingBuilder cc(final String name, final InternetAddress... adresses);
	EmailPopulatingBuilder ccAddresses(final String name, final Collection<InternetAddress> adresses);
	EmailPopulatingBuilder ccMultiple(final InternetAddress... adresses);
	EmailPopulatingBuilder ccMultipleAddresses(final Collection<InternetAddress> adresses);
	EmailPopulatingBuilder ccAddressesWithFixedName(final String name, final InternetAddress... adresses);
	EmailPopulatingBuilder ccAddressesWithDefaultName(final String name, final InternetAddress... adresses);
	EmailPopulatingBuilder ccAddressesWithFixedName(final String name, final Collection<InternetAddress> adresses);
	EmailPopulatingBuilder ccAddressesWithDefaultName(final String name, final Collection<InternetAddress> adresses);
	EmailPopulatingBuilder bcc(final Recipient... recipients);
	EmailPopulatingBuilder bcc(final Collection<Recipient> recipients);
	EmailPopulatingBuilder bcc(final String name, String oneOrMoreAddresses);
	EmailPopulatingBuilder bcc(final String oneOrMoreAddresses);
	EmailPopulatingBuilder bcc(final String name, final String... oneOrMoreAddressesEach);
	EmailPopulatingBuilder bcc(final String name, final Collection<String> oneOrMoreAddressesEach);
	EmailPopulatingBuilder bccMultiple(final String... oneOrMoreAddressesEach);
	EmailPopulatingBuilder bccAddresses(final Collection<String> oneOrMoreAddressesEach);
	EmailPopulatingBuilder bccWithFixedName(final String name, final String... oneOrMoreAddressesEach);
	EmailPopulatingBuilder bccWithDefaultName(final String name, final String... oneOrMoreAddressesEach);
	EmailPopulatingBuilder bccWithFixedName(final String name, final Collection<String> oneOrMoreAddressesEach);
	EmailPopulatingBuilder bccWithDefaultName(final String name, final Collection<String> oneOrMoreAddressesEach);
	EmailPopulatingBuilder bcc(final String name, final InternetAddress address);
	EmailPopulatingBuilder bcc(final InternetAddress address);
	EmailPopulatingBuilder bcc(final String name, final InternetAddress... adresses);
	EmailPopulatingBuilder bccAddresses(final String name, final Collection<InternetAddress> adresses);
	EmailPopulatingBuilder bccMultiple(final InternetAddress... adresse);
	EmailPopulatingBuilder bccMultipleAddresses(final Collection<InternetAddress> adresses);
	EmailPopulatingBuilder bccAddressesWithFixedName(final String name, final InternetAddress... adresses);
	EmailPopulatingBuilder bccAddressesWithDefaultName(final String name, final InternetAddress... adresses);
	EmailPopulatingBuilder bccAddressesWithFixedName(final String name, final Collection<InternetAddress> adresses);
	EmailPopulatingBuilder bccAddressesWithDefaultName(final String name, final Collection<InternetAddress> adresses);
	EmailPopulatingBuilder withRecipientsWithDefaultName(final String defaultName, final Collection<String> oneOrMoreAddressesEach, final RecipientType recipientType);
	EmailPopulatingBuilder withRecipientsWithFixedName(final String fixedName, final Collection<String> oneOrMoreAddressesEach, final RecipientType recipientType);
	EmailPopulatingBuilder withRecipientsWithDefaultName(final String personal, final RecipientType recipientType, final String... oneOrMoreAddressesEach);
	EmailPopulatingBuilder withRecipientsWithFixedName(final String personal, final RecipientType recipientType, final String... oneOrMoreAddressesEach);
	EmailPopulatingBuilder withAddressesWithDefaultName(final String defaultPersonal, final Collection<InternetAddress> addresses, final RecipientType recipientType);
	EmailPopulatingBuilder withAddressesWithFixedName(final String fixedPersonal, final Collection<InternetAddress> addresses, final RecipientType recipientType);
	EmailPopulatingBuilder withAddresses(final String name, boolean fixedPersonal, final Collection<InternetAddress> addresses, final RecipientType recipientType);
	EmailPopulatingBuilder withRecipients(final String name, final boolean fixedPersonal, final RecipientType recipientType, final String... oneOrMoreAddressesEach);
	EmailPopulatingBuilder withRecipients(final String name, final boolean fixedPersonal, final Collection<String> oneOrMoreAddressesEach, final RecipientType recipientType);
	EmailPopulatingBuilder withRecipients(final Collection<Recipient> recipients);
	EmailPopulatingBuilder withRecipients(final Recipient... recipients);
	EmailPopulatingBuilder withRecipients(final Collection<Recipient> recipients, final RecipientType fixedRecipientType);
	EmailPopulatingBuilder withRecipient(final String singleAddress, final RecipientType recipientType);
	EmailPopulatingBuilder withRecipient(final String name, final String singleAddress, final RecipientType recipientType);
	EmailPopulatingBuilder withRecipient(final String name, boolean fixedName, final String singleAddress, final RecipientType recipientType);
	EmailPopulatingBuilder withRecipient(final Recipient recipient);
	EmailPopulatingBuilder withSubject(final String subject);
	//EmailPopulatingBuilder withPlainText(final File textPlainFile);
	EmailPopulatingBuilder withPlainText(final String textPlain);
	//EmailPopulatingBuilder prependTextPlain(final File textPlainFile);
	EmailPopulatingBuilder prependTextPlain(final String textPlain);
	//EmailPopulatingBuilder appendTextPlain(final File textPlainFile);
	EmailPopulatingBuilder appendTextPlain(final String textPlain);
	//EmailPopulatingBuilder withHTMLText(final File textHTMLFile);
	EmailPopulatingBuilder withHTMLText(final String textHTML);
	//EmailPopulatingBuilder prependTextHTML(final File textHTMLFile);
	EmailPopulatingBuilder prependTextHTML(final String textHTML);
	//EmailPopulatingBuilder appendTextHTML(final File textHTMLFile);
	EmailPopulatingBuilder appendTextHTML(final String textHTML);
	EmailPopulatingBuilder withCalendarText(final CalendarMethod calendarMethod, String textCalendar);
	EmailPopulatingBuilder withAttachment(final byte[] data, final String mimetype, final String name);
	EmailPopulatingBuilder withAttachment(final byte[] data, final String mimetype, final String name, final ContentTransferEncoding contentTransferEncoding);
	EmailPopulatingBuilder withAttachment(final DataSource filedata, final String name);
	EmailPopulatingBuilder withAttachment(final DataSource filedata, final String name, final ContentTransferEncoding contentTransferEncoding);
	EmailPopulatingBuilder withAttachments(final Collection<AttachmentResource> attachments);
	<T> EmailPopulatingBuilder withHeaders(final Map<String, Collection<T>> headers);
	EmailPopulatingBuilder withHeader(final String name, final Object value);
	EmailPopulatingBuilder withContentTransferEncoding(final ContentTransferEncoding contentTransferEncoding);
	EmailPopulatingBuilder withDispositionNotificationTo();
	EmailPopulatingBuilder withDispositionNotificationTo(final String address);
	EmailPopulatingBuilder withDispositionNotificationTo(final InternetAddress address);
	EmailPopulatingBuilder withDispositionNotificationTo(final String fixedName, final InternetAddress address);
	EmailPopulatingBuilder withDispositionNotificationTo(final Recipient recipient);
	EmailPopulatingBuilder withReturnReceiptTo();
	EmailPopulatingBuilder withReturnReceiptTo(final String address);
	EmailPopulatingBuilder withReturnReceiptTo(final InternetAddress address);
	EmailPopulatingBuilder withReturnReceiptTo(final String fixedName, final InternetAddress address);
	EmailPopulatingBuilder withReturnReceiptTo(final Recipient recipient);
	
	EmailPopulatingBuilder clearId();
	EmailPopulatingBuilder clearFromRecipient();
	EmailPopulatingBuilder clearReplyTo();
	EmailPopulatingBuilder clearBounceTo();
	EmailPopulatingBuilder clearPlainText();
	EmailPopulatingBuilder clearHTMLText();
	EmailPopulatingBuilder clearCalendarText();
	EmailPopulatingBuilder clearContentTransferEncoding();
	EmailPopulatingBuilder clearSubject();
	EmailPopulatingBuilder clearRecipients();
	
	public String getId();
	public Recipient getFromRecipient();
	public Recipient getReplyToRecipient();
	public Recipient getBounceToRecipient();
	public List<Recipient> getRecipients();
	public String getSubject();
	public MimeMessage getEmailToForward();
	public String getTextPlain();
	public String getTextHTML();
	public CalendarMethod getCalendarMethod();
	public String getTextCalendar();
	public List<AttachmentResource> getEmbeddedImages();
	public List<AttachmentResource> getAttachments();
	public Map<String, Collection<String>> getHeaders();
	public ContentTransferEncoding getContentTransferEncoding();
	public boolean isUseDispositionNotificationTo();
	public Recipient getDispositionNotificationTo();
	public boolean isUseReturnReceiptTo();
	public Recipient getReturnReceiptTo();
	public Date getSentDate();
}
