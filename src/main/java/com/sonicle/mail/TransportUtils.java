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

import com.sonicle.commons.Check;
import com.sonicle.mail.email.EmailMessage;
import com.sonicle.mail.email.Recipient;
import com.sonicle.mail.producer.MimeMessageProducer;
import jakarta.mail.Address;
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
import org.apache.commons.io.output.QueueOutputStream;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author malbinola
 */
public class TransportUtils {
	
	/**
	 * Creates a new JavaMail/Jakarta Mail Session from the given transport
	 * parameters, using the system properties (System.getProperties()) as
	 * the base on which protocol/host-specific properties are applied.
	 *
	 * @param params the transport host parameters (protocol, host, port, credentials, etc.)
	 * @return the Session ready to be used for sending messages
	 * @throws GeneralSecurityException if an error occurs while setting security options (e.g. SSL/TLS)
	 */
	public static Session createSession(final TransportHostParams params) throws GeneralSecurityException {
		return createSession(params, System.getProperties());
	}
	
	/**
	 * Creates a new JavaMail/Jakarta Mail Session from the given transport
	 * parameters and an explicitly passed set of default properties
	 * (instead of using System.getProperties()).
	 *
	 * The method applies protocol-specific properties (e.g. smtp, smtps,
	 * etc.), sets host and port, and, if requested (trustHost=true),
	 * disables SSL host identity checking and marks the host as trusted.
	 * If a username and password are present, it enables authentication
	 * and returns a Session with an Authenticator based on
	 * PasswordAuthenticator; otherwise it returns a Session without
	 * authentication.
	 *
	 * @param params the transport host parameters
	 * @param defaultProperties base properties used to build the final ones
	 * @return the configured Session
	 * @throws GeneralSecurityException if an error occurs while setting security options (e.g. SSL/TLS)
	 */
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
	
	/**
	 * Opens (retrieves and connects) a Transport for the given Session and
	 * protocol.
	 *
	 * @param session the Session to get the Transport from
	 * @param protocol the transport protocol to use
	 * @return the connected Transport, ready for sending
	 * @throws MessagingException if the Transport cannot be obtained or connected
	 */
	public static Transport open(final Session session, final TransportProtocol protocol) throws MessagingException {
		Check.notNull(session, "session");
		Check.notNull(protocol, "protocol");
		return ensureConnected(session.getTransport(protocol.getProtocol()));
	}
	
	/**
	 * Ensures the given Transport is connected; connects it if it isn't.
	 * Useful for reusing an already-existing Transport without manually
	 * checking its state every time.
	 *
	 * @param transport the Transport to check/connect
	 * @return the same Transport, guaranteed to be connected
	 * @throws MessagingException if the connection fails
	 */
	public static Transport ensureConnected(final Transport transport) throws MessagingException {
		Check.notNull(transport, "transport");
		if (!transport.isConnected()) transport.connect();
		return transport;
	}
	
	/**
	 * Sends the message using the recipients already present in the
	 * message itself (message.getAllRecipients()).
	 *
	 * @param transport the (already connected) Transport used to send the message
	 * @param message the MIME message to send
	 * @throws MessagingException if sending fails
	 */
	public static void send(final Transport transport, final MimeMessage message) throws MessagingException {
		send(transport, message, null);
	}
	
	/**
	 * Sends the message using an explicit list of transport-level
	 * recipients (transportRecipients), useful for example to handle
	 * "envelope" recipients different from the message's To/Cc/Bcc
	 * headers. If transportRecipients is null, the recipients present in
	 * the message (message.getAllRecipients()) are used instead.
	 *
	 * @param transport the (already connected) Transport used to send the message
	 * @param message the MIME message to send
	 * @param transportRecipients explicit recipients for the transport-level send, or null to use the message's own recipients
	 * @throws MessagingException if sending fails
	 */
	public static void send(final Transport transport, final MimeMessage message, final Address[] transportRecipients) throws MessagingException {
		Check.notNull(transport, "transport");
		transport.sendMessage(message, (transportRecipients != null) ? transportRecipients : message.getAllRecipients());
	}
	
	/**
	 * Closes the Transport, silently swallowing any MessagingException
	 * that may occur during closing. Handy to use in finally blocks where
	 * a closing exception should not be propagated over more relevant
	 * exceptions.
	 *
	 * @param transport the Transport to close (may be null)
	 */
	public static void closeQuietly(final Transport transport) {
		try {
			close(transport);
		} catch (MessagingException ex) { /* Do nothing... */ }
	}
	
	/**
	 * Closes the Transport if it is not null and is currently connected.
	 *
	 * @param transport the Transport to close (may be null)
	 * @throws MessagingException if closing fails
	 */
	public static void close(final Transport transport) throws MessagingException {
		if (transport != null && transport.isConnected()) transport.close();
	}
	
	/**
	 * Extracts the sender ("from") email address from a message,
	 * supporting two object types: jakarta.mail.internet.MimeMessage,
	 * where the address is retrieved via MimeUtils.getFromAddress,
	 * ignoring any MessagingException (in which case null is returned);
	 * and com.sonicle.mail.email.EmailMessage, where the address is
	 * retrieved from the Recipient returned by getFromRecipient().
	 *
	 * If the message is not one of the two supported types, an
	 * IllegalArgumentException is thrown.
	 *
	 * @param message the message (MimeMessage or EmailMessage) to extract the sender address from
	 * @return the sender's email address, or null if it cannot be determined
	 * @throws IllegalArgumentException if the type of message is not supported
	 */
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
	
	/**
	 * Prepares a transport Session ready for sending, setting the
	 * "mail.from" property with the sender address (if not blank) before
	 * actually creating the Session. This avoids, at send time, slowness
	 * caused by DNS lookups triggered by the call to
	 * message.saveChanges() (see the links in the code comments).
	 *
	 * @param transportParams the transport host parameters
	 * @param sendingAddress the sender email address to set as "mail.from" (may be null or blank)
	 * @param copyOfPoperties a Properties object (typically a copy, not to be shared across concurrent calls) on which "mail.from" is set and which is used as the base for the Session
	 * @return the Session ready for sending
	 * @throws Exception if creating the Session fails (in particular, a GeneralSecurityException is rethrown as an Exception with a dedicated message)
	 */
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
	
	/**
	 * Prepares a MimeMessage ready to be sent through the given transport
	 * Session, starting from one of the two supported message types:
	 *
	 * jakarta.mail.internet.MimeMessage is "rebuilt" by reading its bytes
	 * through a QueueOutputStream/QueueInputStream and creating a new
	 * MimeMessage bound to the transportSession. updateMessageID() is
	 * overridden so that an already-present Message-ID from the original
	 * message is not overwritten. Finally, saveChanges() is called to
	 * finalize the headers.
	 *
	 * com.sonicle.mail.email.EmailMessage delegates the production of the
	 * MimeMessage to MimeMessageProducer.produceMimeMessage, after which
	 * saveChanges() is called.
	 *
	 * If the message is not one of the two supported types, null is
	 * returned (no exception is thrown in this case).
	 *
	 * @param message the source message (MimeMessage or EmailMessage)
	 * @param transportSession the transport Session to bind the new MimeMessage to
	 * @return the MimeMessage ready for sending, or null if the type of message is not supported
	 * @throws Exception if an I/O, messaging, or encoding error occurs while creating the MimeMessage
	 */
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
