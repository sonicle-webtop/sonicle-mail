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
package com.sonicle.mail;

import com.sonicle.mail.email.EmailMessage;
import com.sonicle.mail.email.Recipient;
import com.sonicle.mail.producer.MimeMessageProducer;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Properties;
import net.sf.qualitycheck.Check;
import org.apache.commons.io.output.QueueOutputStream;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author malbinola
 */
public class TransportUtils {
	
	public static Session createSession(final TransportHostParams params) throws GeneralSecurityException {
		return createSession(params, System.getProperties());
	}
	
	public static Session createSession(final TransportHostParams params, final Properties defaultProperties) throws GeneralSecurityException {
		Properties props = new Properties(defaultProperties);
		
		final TransportProtocol proto = params.getProtocol();
		proto.applyProperties(props);
		props.setProperty(proto.getPropertyName("host"), params.getHost());
		props.setProperty(proto.getPropertyName("port"), String.valueOf(params.getPort()));
		if (params.getTrustHost()) {
			proto.sslCheckHostIdentity(props, false);
			proto.sslTrustHosts(props, Arrays.asList(params.getHost()));
		}
		
		if (!StringUtils.isBlank(params.getUsername()) && params.getPassword() != null) {
			props.setProperty(proto.getPropertyName("authenticate"), "true");
			return Session.getInstance(props, new PasswordAuthenticator(params.getUsername(), params.getPassword()));
			
		} else {
			return Session.getInstance(props);
		}
	}
	
	public static Transport open(final Session session, final TransportProtocol protocol) throws MessagingException {
		Check.notNull(session, "session");
		Check.notNull(protocol, "protocol");
		return ensureConnected(session.getTransport(protocol.getProtocol()));
	}
	
	public static Transport ensureConnected(final Transport transport) throws MessagingException {
		Check.notNull(transport, "transport");
		if (!transport.isConnected()) transport.connect();
		return transport;
	}
	
	public static void send(final Transport transport, final MimeMessage message) throws MessagingException {
		Check.notNull(transport, "transport");
		transport.sendMessage(message, message.getAllRecipients());
	}
	
	public static void closeQuietly(final Transport transport) {
		try {
			close(transport);
		} catch (MessagingException ex) { /* Do nothing... */ }
	}
	
	public static void close(final Transport transport) throws MessagingException {
		if (transport != null && transport.isConnected()) transport.close();
	}
	
	public static String extractSendingAddress(final Object message) {
		String sendingAddress = null;
		if (message instanceof MimeMessage) {
			InternetAddress ia = null;
			try {
				ia = MimeUtils.getFromAddress((MimeMessage)message);
			} catch (MessagingException ex) { /* Do nothing... */ }
			if (ia != null) sendingAddress = ia.getAddress();
			
		} else if (message instanceof EmailMessage) {
			Recipient rcpt = ((EmailMessage)message).getFromRecipient();
			if (rcpt != null) sendingAddress = rcpt.getAddress();
		} else {
			throw new IllegalArgumentException("Unsupported message Type: only 'jakarta.mail.internet.MimeMessage' and 'com.sonicle.mail.email.EmailMessage' objects are supported.");
		}
		return sendingAddress;
	}
	
	public static Session prepareTransportSession(final TransportHostParams transportParams, final String sendingAddress, final Properties copyOfPoperties) throws Exception {
		try {
			// Avoid slowness of call to message.saveChanges() due to DNS lookups
			// https://stackoverflow.com/questions/44435457/mimemessage-savechanges-is-really-slow
			// https://javaee.github.io/javamail/docs/api/
			// https://javaee.github.io/javamail/FAQ#commonmistakes
			if (!StringUtils.isBlank(sendingAddress)) copyOfPoperties.setProperty("mail.from", sendingAddress);
			return TransportUtils.createSession(transportParams, copyOfPoperties);
			
		} catch (GeneralSecurityException ex) {
			throw new Exception("Unable to create Transport session", ex);
		}
	}
	
	public static MimeMessage prepareMimeMessage(final Object message, final Session transportSession) throws Exception {
		MimeMessage mimeMessage = null;
		if (message instanceof MimeMessage) {
			try {
				QueueOutputStream os = new QueueOutputStream();
				try (InputStream is = os.newQueueInputStream()) {
					mimeMessage = new MimeMessage(transportSession, is) {
						@Override
						protected void updateMessageID() throws MessagingException {
							if (getHeader(MimeUtils.HEADER_MESSAGE_ID) == null) {
								super.updateMessageID();
							}
						}
					};
					mimeMessage.saveChanges();
				}
			} catch (IOException | MessagingException ex) {
				throw new Exception("Unable to create mimeMessage", ex);
			}
			
		} else if (message instanceof EmailMessage) {
			try {
				mimeMessage = MimeMessageProducer.produceMimeMessage((EmailMessage)message, transportSession);
				mimeMessage.saveChanges();

			} catch (UnsupportedEncodingException | MessagingException ex) {
				throw new Exception("Unable to create mimeMessage", ex);
			}
		}
		return mimeMessage;
	}
}
