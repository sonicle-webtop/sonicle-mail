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
package com.sonicle.mail.email.internal;

import com.sonicle.commons.InternetAddressUtils;
import com.sonicle.commons.LangUtils;
import com.sonicle.mail.email.AttachmentResource;
import com.sonicle.mail.email.CalendarMethod;
import com.sonicle.mail.email.ContentTransferEncoding;
import com.sonicle.mail.email.EmailMessage;
import com.sonicle.mail.email.EmailPopulatingBuilder;
import com.sonicle.mail.email.Recipient;
import jakarta.activation.DataSource;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.qualitycheck.Check;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author malbinola
 * https://github.com/bbottema/simple-java-mail/blob/master/modules/simple-java-mail/src/main/java/org/simplejavamail/email/internal/EmailPopulatingBuilderImpl.java
 */
public class EmailPopulatingBuilderImpl implements InternalEmailPopulatingBuilder {
	protected String id;
	protected Recipient fromRecipient;
	protected Recipient replyToRecipient;
	protected Recipient bounceToRecipient;
	protected List<Recipient> recipients;
	protected String subject;
	private MimeMessage emailToForward;
	protected String textPlain;
	protected String textHTML;
	protected CalendarMethod calendarMethod;
	protected String textCalendar;
	protected final List<AttachmentResource> embeddedImages;
	protected final List<AttachmentResource> attachments;
	protected Map<String, Collection<String>> headers;
	protected ContentTransferEncoding contentTransferEncoding = ContentTransferEncoding.QUOTED_PRINTABLE;
	private boolean useDispositionNotificationTo;
	private Recipient dispositionNotificationTo;
	private boolean useReturnReceiptTo;
	private Recipient returnReceiptTo;
	protected Date sentDate;
	
	EmailPopulatingBuilderImpl(final boolean applyDefaults) {
		recipients = new ArrayList<>();
		embeddedImages = new ArrayList<>();
		attachments = new ArrayList<>();
		headers = new HashMap<>();
		
		if (applyDefaults) {
			
		}
	}
	
	@Override
	public EmailMessage build() {
		return new EmailMessage(this);
	}
	
	@Override
	public EmailPopulatingBuilder from(final String fromAddress) {
		Check.notNull(fromAddress, "fromAddress");
		return from(null, fromAddress);
	}
	
	@Override
	public EmailPopulatingBuilder from(String name, final String fromAddress) {
		Check.notNull(fromAddress, "fromAddress");
		return from(new Recipient(fromAddress, name, null));
	}
	
	@Override
	public EmailPopulatingBuilder from(final String fixedName, final InternetAddress fromAddress) {
		Check.notNull(fromAddress, "fromAddress");
		return from(new Recipient(fromAddress.getAddress(), fixedName, null));
	}
	
	@Override
	public EmailPopulatingBuilder from(final InternetAddress fromAddress) {
		Check.notNull(fromAddress, "fromAddress");
		return from(new Recipient(fromAddress, null));
	}
	
	@Override
	public EmailPopulatingBuilder from(final Recipient recipient) {
		Check.notNull(recipient, "recipient");
		this.fromRecipient = new Recipient(recipient.getAddress(), recipient.getName(), null);
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder withReplyTo(final String replyToAddress) {
		return withReplyTo(replyToAddress != null ? new Recipient(replyToAddress, null, null) : null);
	}
	
	@Override
	public EmailPopulatingBuilder withReplyTo(final String fixedName, final String replyToAddress) {
		Check.notNull(replyToAddress, "replyToAddress");
		return withReplyTo(new Recipient(replyToAddress, fixedName, null));
	}
	
	@Override
	public EmailPopulatingBuilder withReplyTo(final InternetAddress replyToAddress) {
		Check.notNull(replyToAddress, "replyToAddress");
		return withReplyTo(new Recipient(replyToAddress, null));
	}
	
	@Override
	public EmailPopulatingBuilder withReplyTo(final String fixedName, final InternetAddress replyToAddress) {
		Check.notNull(replyToAddress, "replyToAddress");
		return withReplyTo(new Recipient(replyToAddress.getAddress(), fixedName, null));
	}
	
	@Override
	public EmailPopulatingBuilder withReplyTo(final Recipient recipient) {
		this.replyToRecipient = recipient != null ? new Recipient(recipient.getAddress(), recipient.getName(), null) : null;
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder withBounceTo(final String bounceToAddress) {
		return withBounceTo(bounceToAddress != null ? new Recipient(bounceToAddress, null, null) : null);
	}
	
	@Override
	public EmailPopulatingBuilder withBounceTo(final String name, final String bounceToAddress) {
		Check.notNull(bounceToAddress, "bounceToAddress");
		return withBounceTo(new Recipient(bounceToAddress, name, null));
	}
	
	@Override
	public EmailPopulatingBuilder withBounceTo(final InternetAddress bounceToAddress) {
		Check.notNull(bounceToAddress, "bounceToAddress");
		return withBounceTo(new Recipient(bounceToAddress, null));
	}
	
	@Override
	public EmailPopulatingBuilder withBounceTo(final String name, final InternetAddress bounceToAddress) {
		Check.notNull(bounceToAddress, "bounceToAddress");
		return withBounceTo(new Recipient(bounceToAddress.getAddress(), name, null));
	}
	
	@Override
	public EmailPopulatingBuilder withBounceTo(final Recipient recipient) {
		this.bounceToRecipient = recipient != null ? new Recipient(recipient.getAddress(), recipient.getName(), null) : null;
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder to(final Recipient... recipients) {
		Check.notNull(recipients, "recipients");
		return withRecipients(Arrays.asList(recipients), RecipientType.TO);
	}
	
	@Override
	public EmailPopulatingBuilder to(final Collection<Recipient> recipients) {
		Check.notNull(recipients, "recipients");
		return withRecipients(recipients, RecipientType.TO);
	}
	
	@Override
	public EmailPopulatingBuilder to(final String name, String oneOrMoreAddresses) {
		Check.notNull(oneOrMoreAddresses, "oneOrMoreAddresses");
		return withRecipients(name, true, Collections.singletonList(oneOrMoreAddresses), RecipientType.TO);
	}
	
	@Override
	public EmailPopulatingBuilder to(final String oneOrMoreAddresses) {
		Check.notNull(oneOrMoreAddresses, "oneOrMoreAddresses");
		return withRecipientsWithDefaultName(null, Collections.singletonList(oneOrMoreAddresses), RecipientType.TO);
	}
	
	@Override
	public EmailPopulatingBuilder to(final String name, final String... oneOrMoreAddressesEach) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return toWithFixedName(name, oneOrMoreAddressesEach);
	}
	
	@Override
	public EmailPopulatingBuilder to(final String name, final Collection<String> oneOrMoreAddressesEach) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return toWithFixedName(name, oneOrMoreAddressesEach);
	}
	
	@Override
	public EmailPopulatingBuilder toMultiple(final String... oneOrMoreAddressesEach) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return withRecipientsWithDefaultName(null, Arrays.asList(oneOrMoreAddressesEach), RecipientType.TO);
	}
	
	@Override
	public EmailPopulatingBuilder toMultiple(final Collection<String> oneOrMoreAddressesEach) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return withRecipientsWithDefaultName(null, oneOrMoreAddressesEach, RecipientType.TO);
	}
	
	@Override
	public EmailPopulatingBuilder toWithFixedName(final String name, final String... oneOrMoreAddressesEach) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return withRecipientsWithFixedName(name, Arrays.asList(oneOrMoreAddressesEach), RecipientType.TO);
	}
	
	@Override
	public EmailPopulatingBuilder toWithDefaultName(final String name, final String... oneOrMoreAddressesEach) {
		Check.notNull(name, "name");
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return withRecipientsWithDefaultName(name, Arrays.asList(oneOrMoreAddressesEach), RecipientType.TO);
	}
	
	@Override
	public EmailPopulatingBuilder toWithFixedName(final String name, final Collection<String> oneOrMoreAddressesEach) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return withRecipientsWithFixedName(name, oneOrMoreAddressesEach, RecipientType.TO);
	}
	
	@Override
	public EmailPopulatingBuilder toWithDefaultName(final String name, final Collection<String> oneOrMoreAddressesEach) {
		Check.notNull(name, "name");
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return withRecipientsWithDefaultName(name, oneOrMoreAddressesEach, RecipientType.TO);
	}
	
	@Override
	public EmailPopulatingBuilder to(final String name, InternetAddress address) {
		Check.notNull(address, "address");
		return toAddressesWithFixedName(name, address);
	}
	
	@Override
	public EmailPopulatingBuilder to(final InternetAddress address) {
		Check.notNull(address, "address");
		return withAddressesWithDefaultName(null, Collections.singletonList(address), RecipientType.TO);
	}
	
	@Override
	public EmailPopulatingBuilder to(final String name, final InternetAddress... adresses) {
		Check.notNull(adresses, "adresses");
		return toAddressesWithFixedName(name, adresses);
	}
	
	@Override
	public EmailPopulatingBuilder toAddresses(final String name, final Collection<InternetAddress> adresses) {
		Check.notNull(adresses, "adresses");
		return toAddressesWithFixedName(name, adresses);
	}
	
	@Override
	public EmailPopulatingBuilder toMultiple(final InternetAddress... adresses) {
		Check.notNull(adresses, "adresses");
		return withAddressesWithDefaultName(null, Arrays.asList(adresses), RecipientType.TO);
	}
	
	@Override
	public EmailPopulatingBuilder toMultipleAddresses(final Collection<InternetAddress> adresses) {
		Check.notNull(adresses, "adresses");
		return withAddressesWithDefaultName(null, adresses, RecipientType.TO);
	}
	
	@Override
	public EmailPopulatingBuilder toAddressesWithFixedName(final String name, final InternetAddress... adresses) {
		Check.notNull(adresses, "adresses");
		return withAddressesWithFixedName(name, Arrays.asList(adresses), RecipientType.TO);
	}
	
	@Override
	public EmailPopulatingBuilder toAddressesWithDefaultName(final String name, final InternetAddress... adresses) {
		Check.notNull(name, "name");
		Check.notNull(adresses, "adresses");
		return withAddressesWithDefaultName(name, Arrays.asList(adresses), RecipientType.TO);
	}
	
	@Override
	public EmailPopulatingBuilder toAddressesWithFixedName(final String name, final Collection<InternetAddress> adresses) {
		Check.notNull(adresses, "adresses");
		return withAddressesWithFixedName(name, adresses, RecipientType.TO);
	}
	
	@Override
	public EmailPopulatingBuilder toAddressesWithDefaultName(final String name, final Collection<InternetAddress> adresses) {
		Check.notNull(name, "name");
		Check.notNull(adresses, "adresses");
		return withAddressesWithDefaultName(name, adresses, RecipientType.TO);
	}
	
	@Override
	public EmailPopulatingBuilder cc(final Recipient... recipients) {
		Check.notNull(recipients, "recipients");
		return withRecipients(Arrays.asList(recipients), RecipientType.CC);
	}
	
	@Override
	public EmailPopulatingBuilder cc(final Collection<Recipient> recipients) {
		Check.notNull(recipients, "recipients");
		return withRecipients(recipients, RecipientType.CC);
	}
	
	@Override
	public EmailPopulatingBuilder cc(final String name, String oneOrMoreAddresses) {
		Check.notNull(oneOrMoreAddresses, "oneOrMoreAddresses");
		return withRecipients(name, true, Collections.singletonList(oneOrMoreAddresses), RecipientType.CC);
	}
	
	@Override
	public EmailPopulatingBuilder cc(final String oneOrMoreAddresses) {
		Check.notNull(oneOrMoreAddresses, "oneOrMoreAddresses");
		return withRecipientsWithDefaultName(null, Collections.singletonList(oneOrMoreAddresses), RecipientType.CC);
	}
	
	@Override
	public EmailPopulatingBuilder cc(final String name, final String... oneOrMoreAddressesEach) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return ccWithFixedName(name, oneOrMoreAddressesEach);
	}
	
	@Override
	public EmailPopulatingBuilder cc(final String name, final Collection<String> oneOrMoreAddressesEach) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return ccWithFixedName(name, oneOrMoreAddressesEach);
	}
	
	@Override
	public EmailPopulatingBuilder ccMultiple(final String... oneOrMoreAddressesEach) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return withRecipientsWithDefaultName(null, Arrays.asList(oneOrMoreAddressesEach), RecipientType.CC);
	}
	
	@Override
	public EmailPopulatingBuilder ccAddresses(final Collection<String> oneOrMoreAddressesEach) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return withRecipientsWithDefaultName(null, oneOrMoreAddressesEach, RecipientType.CC);
	}
	
	@Override
	public EmailPopulatingBuilder ccWithFixedName(final String name, final String... oneOrMoreAddressesEach) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return withRecipientsWithFixedName(name, Arrays.asList(oneOrMoreAddressesEach), RecipientType.CC);
	}
	
	@Override
	public EmailPopulatingBuilder ccWithDefaultName(final String name, final String... oneOrMoreAddressesEach) {
		Check.notNull(name, "name");
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return withRecipientsWithDefaultName(name, Arrays.asList(oneOrMoreAddressesEach), RecipientType.CC);
	}
	
	@Override
	public EmailPopulatingBuilder ccWithFixedName(final String name, final Collection<String> oneOrMoreAddressesEach) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return withRecipientsWithFixedName(name, oneOrMoreAddressesEach, RecipientType.CC);
	}
	
	@Override
	public EmailPopulatingBuilder ccWithDefaultName(final String name, final Collection<String> oneOrMoreAddressesEach) {
		Check.notNull(name, "name");
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return withRecipientsWithDefaultName(name, oneOrMoreAddressesEach, RecipientType.CC);
	}
	
	@Override
	public EmailPopulatingBuilder cc(final String name, InternetAddress address) {
		return ccAddressesWithFixedName(name, address);
	}
	
	@Override
	public EmailPopulatingBuilder cc(final InternetAddress address) {
		Check.notNull(address, "address");
		return withAddressesWithDefaultName(null, Collections.singletonList(address), RecipientType.CC);
	}
	
	@Override
	public EmailPopulatingBuilder cc(final String name, final InternetAddress... adresses) {
		Check.notNull(adresses, "adresses");
		return ccAddressesWithFixedName(name, adresses);
	}
	
	@Override
	public EmailPopulatingBuilder ccAddresses(final String name, final Collection<InternetAddress> adresses) {
		Check.notNull(adresses, "adresses");
		return ccAddressesWithFixedName(name, adresses);
	}
	
	@Override
	public EmailPopulatingBuilder ccMultiple(final InternetAddress... adresses) {
		Check.notNull(adresses, "adresses");
		return withAddressesWithDefaultName(null, Arrays.asList(adresses), RecipientType.CC);
	}
	
	@Override
	public EmailPopulatingBuilder ccMultipleAddresses(final Collection<InternetAddress> adresses) {
		Check.notNull(adresses, "adresses");
		return withAddressesWithDefaultName(null, adresses, RecipientType.CC);
	}
	
	@Override
	public EmailPopulatingBuilder ccAddressesWithFixedName(final String name, final InternetAddress... adresses) {
		Check.notNull(adresses, "adresses");
		return withAddressesWithFixedName(name, Arrays.asList(adresses), RecipientType.CC);
	}
	
	@Override
	public EmailPopulatingBuilder ccAddressesWithDefaultName(final String name, final InternetAddress... adresses) {
		Check.notNull(name, "name");
		Check.notNull(adresses, "adresses");
		return withAddressesWithDefaultName(name, Arrays.asList(adresses), RecipientType.CC);
	}
	
	@Override
	public EmailPopulatingBuilder ccAddressesWithFixedName(final String name, final Collection<InternetAddress> adresses) {
		Check.notNull(adresses, "adresses");
		return withAddressesWithFixedName(name, adresses, RecipientType.CC);
	}
	
	@Override
	public EmailPopulatingBuilder ccAddressesWithDefaultName(final String name, final Collection<InternetAddress> adresses) {
		Check.notNull(name, "name");
		Check.notNull(adresses, "adresses");
		return withAddressesWithDefaultName(name, adresses, RecipientType.CC);
	}
	
	
	
	@Override
	public EmailPopulatingBuilder bcc(final Recipient... recipients) {
		Check.notNull(recipients, "recipients");
		return withRecipients(Arrays.asList(recipients), RecipientType.BCC);
	}
	
	@Override
	public EmailPopulatingBuilder bcc(final Collection<Recipient> recipients) {
		Check.notNull(recipients, "recipients");
		return withRecipients(recipients, RecipientType.BCC);
	}
	
	@Override
	public EmailPopulatingBuilder bcc(final String name, String oneOrMoreAddresses) {
		Check.notNull(oneOrMoreAddresses, "oneOrMoreAddresses");
		return withRecipients(name, true, Collections.singletonList(oneOrMoreAddresses), RecipientType.BCC);
	}
	
	@Override
	public EmailPopulatingBuilder bcc(final String oneOrMoreAddresses) {
		Check.notNull(oneOrMoreAddresses, "oneOrMoreAddresses");
		return withRecipientsWithDefaultName(null, Collections.singletonList(oneOrMoreAddresses), RecipientType.BCC);
	}
	
	@Override
	public EmailPopulatingBuilder bcc(final String name, final String... oneOrMoreAddressesEach) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return bccWithFixedName(name, oneOrMoreAddressesEach);
	}
	
	@Override
	public EmailPopulatingBuilder bcc(final String name, final Collection<String> oneOrMoreAddressesEach) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return bccWithFixedName(name, oneOrMoreAddressesEach);
	}
	
	@Override
	public EmailPopulatingBuilder bccMultiple(final String... oneOrMoreAddressesEach) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return withRecipientsWithDefaultName(null, Arrays.asList(oneOrMoreAddressesEach), RecipientType.BCC);
	}
	
	@Override
	public EmailPopulatingBuilder bccAddresses(final Collection<String> oneOrMoreAddressesEach) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return withRecipientsWithDefaultName(null, oneOrMoreAddressesEach, RecipientType.BCC);
	}
	
	@Override
	public EmailPopulatingBuilder bccWithFixedName(final String name, final String... oneOrMoreAddressesEach) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return withRecipientsWithFixedName(name, Arrays.asList(oneOrMoreAddressesEach), RecipientType.BCC);
	}
	
	@Override
	public EmailPopulatingBuilder bccWithDefaultName(final String name, final String... oneOrMoreAddressesEach) {
		Check.notNull(name, "name");
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return withRecipientsWithDefaultName(name, Arrays.asList(oneOrMoreAddressesEach), RecipientType.BCC);
	}
	
	@Override
	public EmailPopulatingBuilder bccWithFixedName(final String name, final Collection<String> oneOrMoreAddressesEach) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return withRecipientsWithFixedName(name, oneOrMoreAddressesEach, RecipientType.BCC);
	}
	
	@Override
	public EmailPopulatingBuilder bccWithDefaultName(final String name, final Collection<String> oneOrMoreAddressesEach) {
		Check.notNull(name, "name");
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return withRecipientsWithDefaultName(name, oneOrMoreAddressesEach, RecipientType.BCC);
	}
	
	@Override
	public EmailPopulatingBuilder bcc(final String name, final InternetAddress address) {
		Check.notNull(address, "address");
		return bccAddressesWithFixedName(name, address);
	}
	
	@Override
	public EmailPopulatingBuilder bcc(final InternetAddress address) {
		Check.notNull(address, "address");
		return withAddressesWithDefaultName(null, Collections.singletonList(address), RecipientType.BCC);
	}
	
	@Override
	public EmailPopulatingBuilder bcc(final String name, final InternetAddress... adresses) {
		Check.notNull(adresses, "adresses");
		return bccAddressesWithFixedName(name, adresses);
	}
	
	@Override
	public EmailPopulatingBuilder bccAddresses(final String name, final Collection<InternetAddress> adresses) {
		Check.notNull(adresses, "adresses");
		return bccAddressesWithFixedName(name, adresses);
	}
	
	@Override
	public EmailPopulatingBuilder bccMultiple(final InternetAddress... adresses) {
		Check.notNull(adresses, "adresses");
		return withAddressesWithDefaultName(null, Arrays.asList(adresses), RecipientType.BCC);
	}
	
	@Override
	public EmailPopulatingBuilder bccMultipleAddresses(final Collection<InternetAddress> adresses) {
		Check.notNull(adresses, "adresses");	
		return withAddressesWithDefaultName(null, adresses, RecipientType.BCC);
	}
	
	@Override
	public EmailPopulatingBuilder bccAddressesWithFixedName(final String name, final InternetAddress... adresses) {
		Check.notNull(adresses, "adresses");
		return withAddressesWithFixedName(name, Arrays.asList(adresses), RecipientType.BCC);
	}
	
	@Override
	public EmailPopulatingBuilder bccAddressesWithDefaultName(final String name, final InternetAddress... adresses) {
		Check.notNull(name, "name");
		Check.notNull(adresses, "adresses");
		return withAddressesWithDefaultName(name, Arrays.asList(adresses), RecipientType.BCC);
	}
	
	@Override
	public EmailPopulatingBuilder bccAddressesWithFixedName(final String name, final Collection<InternetAddress> adresses) {
		Check.notNull(adresses, "adresses");
		return withAddressesWithFixedName(name, adresses, RecipientType.BCC);
	}
	
	@Override
	public EmailPopulatingBuilder bccAddressesWithDefaultName(final String name, final Collection<InternetAddress> adresses) {
		Check.notNull(name, "name");
		Check.notNull(adresses, "adresses");
		return withAddressesWithDefaultName(name, adresses, RecipientType.BCC);
	}
	
	@Override
	public EmailPopulatingBuilder withRecipientsWithDefaultName(final String defaultName, final Collection<String> oneOrMoreAddressesEach, final RecipientType recipientType) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return withRecipients(defaultName, false, oneOrMoreAddressesEach, recipientType);
	}
	
	@Override
	public EmailPopulatingBuilder withRecipientsWithFixedName(final String fixedName, final Collection<String> oneOrMoreAddressesEach, final RecipientType recipientType) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return withRecipients(fixedName, true, oneOrMoreAddressesEach, recipientType);
	}
	
	@Override
	public EmailPopulatingBuilder withRecipientsWithDefaultName(final String name, final RecipientType recipientType, final String... oneOrMoreAddressesEach) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return withRecipients(name, false, Arrays.asList(oneOrMoreAddressesEach), recipientType);
	}
	
	@Override
	public EmailPopulatingBuilder withRecipientsWithFixedName(final String name, final RecipientType recipientType, final String... oneOrMoreAddressesEach) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return withRecipients(name, true, Arrays.asList(oneOrMoreAddressesEach), recipientType);
	}
	
	@Override
	public EmailPopulatingBuilder withAddressesWithDefaultName(final String defaultName, final Collection<InternetAddress> addresses, final RecipientType recipientType) {
		Check.notNull(addresses, "addresses");
		return withAddresses(defaultName, false, addresses, recipientType);
	}
	
	@Override
	public EmailPopulatingBuilder withAddressesWithFixedName(final String fixedName, final Collection<InternetAddress> addresses, final RecipientType recipientType) {
		Check.notNull(addresses, "addresses");
		return withAddresses(fixedName, true, addresses, recipientType);
	}
	
	@Override
	public EmailPopulatingBuilder withAddresses(final String name, boolean fixedName, final Collection<InternetAddress> addresses, final RecipientType recipientType) {
		Check.notNull(addresses, "addresses");
		for (InternetAddress address : addresses) {
			if (address == null) continue;
			String effectiveName = (fixedName || StringUtils.isEmpty(address.getPersonal())) ? name : address.getPersonal();
			withRecipient(effectiveName, address.getAddress(), recipientType);
		}
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder withRecipients(final String name, final boolean fixedName, final RecipientType recipientType, final String... oneOrMoreAddressesEach) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		return withRecipients(name, fixedName, Arrays.asList(oneOrMoreAddressesEach), recipientType);
	}
	
	@Override
	public EmailPopulatingBuilder withRecipients(final String name, final boolean fixedName, final Collection<String> oneOrMoreAddressesEach, final RecipientType recipientType) {
		Check.notNull(oneOrMoreAddressesEach, "oneOrMoreAddressesEach");
		for (String oneOrMoreAddresses : oneOrMoreAddressesEach) {
			if (oneOrMoreAddresses == null) continue;
			for (String emailAddress : InternetAddressUtils.extractEmailAddresses(oneOrMoreAddresses)) {
				withRecipient(name, fixedName, emailAddress, recipientType);
			}
		}
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder withRecipients(final Collection<Recipient> recipients) {
		Check.notNull(recipients, "recipients");
		return withRecipients(recipients, null);
	}
	
	@Override
	public EmailPopulatingBuilder withRecipients(final Recipient... recipients) {
		Check.notNull(recipients, "recipients");
		return withRecipients(Arrays.asList(recipients), null);
	}
	
	@Override
	public EmailPopulatingBuilder withRecipients(final Collection<Recipient> recipients, final RecipientType fixedRecipientType) {
		Check.notNull(recipients, "recipients");
		for (Recipient recipient : recipients) {
			withRecipient(recipient.getName(), recipient.getAddress(), LangUtils.coalesce(fixedRecipientType, recipient.getType()));
		}
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder withRecipient(final String singleAddress, final RecipientType recipientType) {
		Check.notNull(singleAddress, "singleAddress");
		return withRecipient(null, singleAddress, recipientType);
	}
	
	@Override
	public EmailPopulatingBuilder withRecipient(final String name, final String singleAddress, final RecipientType recipientType) {
		Check.notNull(singleAddress, "singleAddress");
		return withRecipient(name, true, singleAddress, recipientType);
	}
	
	@Override
	public EmailPopulatingBuilder withRecipient(final String name, boolean fixedName, final String singleAddress, final RecipientType recipientType) {
		Check.notNull(singleAddress, "singleAddress");
		try {
			recipients.add(Recipient.from(name, fixedName, singleAddress, recipientType));
		} catch (Exception e){
			// assume recipient was malformed and simply ignore it
		}
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder withRecipient(final Recipient recipient) {
		Check.notNull(recipient, "recipient");
		recipients.add(new Recipient(recipient.getAddress(), recipient.getName(), recipient.getType()));
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder withSubject(final String subject) {
		this.subject = subject;
		return this;
	}
	
	@Override
	public InternalEmailPopulatingBuilder withForward(final MimeMessage emailMessageToForward) {
		this.emailToForward = emailMessageToForward;
		return this;
	}
	
	/*
	@Override
	public EmailPopulatingBuilder withPlainText(final File textPlainFile) {
		Check.notNull(textPlainFile, "textPlainFile");
		try {
			return withPlainText(FileUtil.readFileContent(textPlainFile));
		} catch (IOException ex) {
			throw new EmailException(format(ERROR_READING_FROM_FILE, textPlainFile), e);
		}
	}
	*/
	
	@Override
	public EmailPopulatingBuilder withPlainText(final String textPlain) {
		this.textPlain = textPlain;
		return this;
	}
	
	/*
	@Override
	public EmailPopulatingBuilder prependTextPlain(final File textPlainFile) {
		Check.notNull(textPlainFile, "textPlainFile");
		try {
			return prependTextPlain(FileUtil.readFileContent(textPlainFile));
		} catch (IOException ex) {
			throw new EmailException(format(ERROR_READING_FROM_FILE, textPlainFile), e);
		}
	}
	*/
	
	@Override
	public EmailPopulatingBuilder prependTextPlain(final String textPlain) {
		Check.notNull(textPlain, "textPlain");
		this.textPlain = textPlain + StringUtils.defaultString(this.textPlain);
		return this;
	}
	
	/*
	@Override
	public EmailPopulatingBuilder appendTextPlain(final File textPlainFile) {
		Check.notNull(textPlainFile, "textPlainFile");
		try {
			return appendTextPlain(FileUtil.readFileContent(textPlainFile));
		} catch (IOException ex) {
			throw new EmailException(format(ERROR_READING_FROM_FILE, textPlainFile), e);
		}
	}
	*/
	
	@Override
	public EmailPopulatingBuilder appendTextPlain(final String textPlain) {
		Check.notNull(textPlain, "textPlain");
		this.textPlain = StringUtils.defaultString(this.textPlain) + textPlain;
		return this;
	}
	
	/*
	@Override
	public EmailPopulatingBuilder withHTMLText(final File textHTMLFile) {
		Check.notNull(textHTMLFile, "textHTMLFile");
		try {
			return withHTMLText(FileUtil.readFileContent(textHTMLFile));
		} catch (IOException ex) {
			throw new EmailException(format(ERROR_READING_FROM_FILE, textHTMLFile), e);
		}
	}
	*/
	
	@Override
	public EmailPopulatingBuilder withHTMLText(final String textHTML) {
		this.textHTML = textHTML;
		return this;
	}
	
	/*
	@Override
	public EmailPopulatingBuilder prependTextHTML(final File textHTMLFile) {
		Check.notNull(textHTMLFile, "textHTMLFile");
		try {
			return prependTextHTML(FileUtil.readFileContent(textHTMLFile));
		} catch (IOException ex) {
			throw new EmailException(format(ERROR_READING_FROM_FILE, textHTMLFile), e);
		}
	}
	*/
	
	@Override
	public EmailPopulatingBuilder prependTextHTML(final String textHTML) {
		Check.notNull(textHTML, "textHTML");
		this.textHTML = textHTML + StringUtils.defaultString(this.textHTML);
		return this;
	}
	
	/*
	@Override
	public EmailPopulatingBuilder appendTextHTML(final File textHTMLFile) {
		Check.notNull(textHTMLFile, "textHTMLFile");
		try {
			return appendTextHTML(FileUtil.readFileContent(textHTMLFile));
		} catch (IOException ex) {
			throw new EmailException(format(ERROR_READING_FROM_FILE, textHTMLFile), e);
		}
	}
	*/
	
	@Override
	public EmailPopulatingBuilder appendTextHTML(final String textHTML) {
		Check.notNull(textHTML, "textHTML");
		this.textHTML = StringUtils.defaultString(this.textHTML) + textHTML;
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder withCalendarText(final CalendarMethod calendarMethod, final String textCalendar) {
		this.calendarMethod = Check.notNull(calendarMethod, "calendarMethod");
		this.textCalendar = Check.notNull(textCalendar, "textCalendar");
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder withAttachment(final byte[] data, final String mimetype, final String name) {
		return withAttachment(data, mimetype, name, null);
	}
	
	@Override
	public EmailPopulatingBuilder withAttachment(final byte[] data, final String mimetype, final String name, final ContentTransferEncoding contentTransferEncoding) {
		Check.notNull(data, "data");
		Check.notNull(mimetype, "mimetype");
		final ByteArrayDataSource dataSource = new ByteArrayDataSource(data, mimetype);
		dataSource.setName(name);
		withAttachment(dataSource, name, contentTransferEncoding);
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder withAttachment(final DataSource filedata, final String name) {
		return withAttachment(filedata, name, null);
	}
	
	@Override
	public EmailPopulatingBuilder withAttachment(final DataSource filedata, final String name, final ContentTransferEncoding contentTransferEncoding) {
		Check.notNull(filedata, "filedata");
		attachments.add(new AttachmentResource(filedata, name, contentTransferEncoding));
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder withAttachments(final Collection<AttachmentResource> attachments) {
		Check.notNull(attachments, "attachments");
		for (final AttachmentResource attachment : attachments) {
			if (attachment == null) continue;
			withAttachment(attachment.getDataSource(), attachment.getName(), attachment.getContentTransferEncoding());
		}
		return this;
	}
	
	@Override
	public <T> EmailPopulatingBuilder withHeaders(final Map<String, Collection<T>> headers) {
		Check.notNull(headers, "headers");
		return withHeaders(headers, false);
	}
	
	public <T> InternalEmailPopulatingBuilder withHeaders(final Map<String, Collection<T>> headers, final boolean ignoreSmimeMessageId) {
		for (Map.Entry<String, Collection<T>> headerEntry : headers.entrySet()) {
			for (final T headerValue : headerEntry.getValue()) {
				//if (!ignoreSmimeMessageId || !isGeneratedSmimeMessageId(headerEntry.getKey(), headerValue)) {
					withHeader(headerEntry.getKey(), headerValue);
				//}
			}
		}
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder withHeader(final String name, final Object value) {
		Check.notNull(name, "name");
		if (!headers.containsKey(name)) {
			headers.put(name, new ArrayList<>());
		}
		headers.get(name).add(value != null ? String.valueOf(value) : null);
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder withContentTransferEncoding(final ContentTransferEncoding contentTransferEncoding) {
		Check.notNull(contentTransferEncoding, "contentTransferEncoding");
		this.contentTransferEncoding = contentTransferEncoding;
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder withDispositionNotificationTo() {
		this.useDispositionNotificationTo = true;
		this.dispositionNotificationTo = null;
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder withDispositionNotificationTo(final String address) {
		Check.notNull(address, "address");
		return withDispositionNotificationTo(new Recipient(address, null, null));
	}
	
	@Override
	public EmailPopulatingBuilder withDispositionNotificationTo(final InternetAddress address) {
		Check.notNull(address, "address");
		return withDispositionNotificationTo(new Recipient(address, null));
	}
	
	@Override
	public EmailPopulatingBuilder withDispositionNotificationTo(final String fixedName, final InternetAddress address) {
		Check.notNull(address, "address");
		return withDispositionNotificationTo(new Recipient(address.getAddress(), fixedName, null));
	}
	
	@Override
	public EmailPopulatingBuilder withDispositionNotificationTo(final Recipient recipient) {
		Check.notNull(recipient, "recipient");
		Check.notNull(recipient.getAddress(), "address");
		this.useDispositionNotificationTo = true;
		this.dispositionNotificationTo = new Recipient(recipient.getAddress(), recipient.getName(), null);
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder withReturnReceiptTo() {
		this.useReturnReceiptTo = true;
		this.returnReceiptTo = null;
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder withReturnReceiptTo(final String address) {
		Check.notNull(address, "address");
		return withDispositionNotificationTo(new Recipient(address, null, null));
	}
	
	@Override
	public EmailPopulatingBuilder withReturnReceiptTo(final InternetAddress address) {
		Check.notNull(address, "address");
		return withDispositionNotificationTo(new Recipient(address, null));
	}
	
	@Override
	public EmailPopulatingBuilder withReturnReceiptTo(final String fixedName, final InternetAddress address) {
		Check.notNull(address, "address");
		return withDispositionNotificationTo(new Recipient(address.getAddress(), fixedName, null));
	}
	
	@Override
	public EmailPopulatingBuilder withReturnReceiptTo(final Recipient recipient) {
		Check.notNull(recipient, "recipient");
		Check.notNull(recipient.getAddress(), "address");
		this.useReturnReceiptTo = true;
		this.returnReceiptTo = new Recipient(recipient.getAddress(), recipient.getName(), null);
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder clearId() {
		this.id = null;
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder clearFromRecipient() {
		this.fromRecipient = null;
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder clearReplyTo() {
		this.replyToRecipient = null;
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder clearBounceTo() {
		this.bounceToRecipient = null;
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder clearRecipients() {
		this.recipients.clear();
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder clearSubject() {
		this.subject = null;
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder clearPlainText() {
		this.textPlain = null;
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder clearHTMLText() {
		this.textHTML = null;
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder clearCalendarText() {
		this.calendarMethod = null;
		this.textCalendar = null;
		return this;
	}
	
	@Override
	public EmailPopulatingBuilder clearContentTransferEncoding() {
		this.contentTransferEncoding = ContentTransferEncoding.QUOTED_PRINTABLE;
		return this;
	}
	
	@Override
	public String getId() {
		return id;
	}
	
	@Override
	public Recipient getFromRecipient() {
		return fromRecipient;
	}
	
	@Override
	public Recipient getReplyToRecipient() {
		return replyToRecipient;
	}
	
	@Override
	public Recipient getBounceToRecipient() {
		return bounceToRecipient;
	}
	
	@Override
	public List<Recipient> getRecipients() {
		return new ArrayList<>(recipients);
	}
	
	@Override
	public String getSubject() {
		return subject;
	}
	
	@Override
	public MimeMessage getEmailToForward() {
		return emailToForward;
	}
	
	@Override
	public String getTextPlain() {
		return textPlain;
	}
	
	@Override
	public String getTextHTML() {
		return textHTML;
	}
	
	@Override
	public CalendarMethod getCalendarMethod() {
		return calendarMethod;
	}
	
	@Override
	public String getTextCalendar() {
		return textCalendar;
	}
	
	@Override
	public List<AttachmentResource> getEmbeddedImages() {
		return new ArrayList<>(embeddedImages);
	}
	
	@Override
	public List<AttachmentResource> getAttachments() {
		return new ArrayList<>(attachments);
	}
	
	@Override
	public Map<String, Collection<String>> getHeaders() {
		return new HashMap<>(headers);
	}
	
	@Override
	public ContentTransferEncoding getContentTransferEncoding() {
		return contentTransferEncoding;
	}
	
	@Override
	public boolean isUseDispositionNotificationTo() {
		return useDispositionNotificationTo;
	}
	
	@Override
	public Recipient getDispositionNotificationTo() {
		return dispositionNotificationTo;
	}
	
	@Override
	public boolean isUseReturnReceiptTo() {
		return useReturnReceiptTo;
	}
	
	@Override
	public Recipient getReturnReceiptTo() {
		return returnReceiptTo;
	}
	
	@Override
	public Date getSentDate() {
		return sentDate != null ? new Date(sentDate.getTime()) : null;
	}
}
