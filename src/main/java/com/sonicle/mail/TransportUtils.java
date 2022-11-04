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

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Properties;
import net.sf.qualitycheck.Check;
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
}
