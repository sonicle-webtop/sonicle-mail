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
import com.sonicle.mail.imap.SonicleIMAPSSLSocketFactory;
import com.sonicle.mail.imap.SonicleIMAPSocketFactory;
import com.sonicle.mail.imap.SonicleIMAPSocketTracker;
import java.util.Properties;

/**
 *
 * @author malbinola
 */
public class PropsBuilder {
	private final Properties props;
	
	public PropsBuilder() {
		this(System.getProperties());
	}
	
	public PropsBuilder(Properties defaults) {
		this.props = new Properties(defaults);
	}
	
	public PropsBuilder debug(boolean debug) {
		enableDebug(props, debug);
		return this;
	}
	
	public PropsBuilder withDebug() {
		return debug(true);
	}
	
	public PropsBuilder socketDebug(boolean socketDebug) {
		enableSocketDebug(props, socketDebug);
		return this;
	}
	
	public PropsBuilder withSocketDebug() {
		return socketDebug(true);
	}
	
	public PropsBuilder withParseDebug() {
		enableIMAPParseDebug(props, true);
		return this;
	}
	
	public PropsBuilder withCustomizedIMAPFolderClasses() {
		useCustomizedIMAPFolderClasses(props);
		return this;
	}
	
	public PropsBuilder withAvoidSlownessOnGettingLocalHostName() {
		avoidSlownessOnGettingLocalHostName(props);
		return this;
	}
	
	public PropsBuilder withEnableIMAPEvents() {
		enableIMAPEvents(props, true);
		return this;
	}
	
	public PropsBuilder withProperty(final StoreProtocol protocol, String nameAfterMailProto, String value) {
		Check.notNull(protocol, "protocol");
		props.setProperty(protocol.getPropertyName(nameAfterMailProto), value);
		return this;
	}
	
	public Properties build() {
		return props;
	}
	
	public static Properties enableDebug(final Properties properties, final boolean enabled) {
		properties.setProperty("mail.debug", String.valueOf(enabled));
		return properties;
	}
	
	public static Properties enableSocketDebug(final Properties properties, final boolean enabled) {
		properties.setProperty("mail.socket.debug", String.valueOf(enabled));
		return properties;
	}
	
	public static Properties enableIMAPParseDebug(final Properties properties, final boolean enabled) {
		properties.setProperty("mail.imap.parse.debug", String.valueOf(enabled));
		properties.setProperty("mail.imaps.parse.debug", String.valueOf(enabled));
		return properties;
	}
	
	public static Properties enableIMAPEvents(final Properties properties, final boolean enabled) {
		properties.setProperty("mail.imap.enableimapevents", String.valueOf(enabled));
		properties.setProperty("mail.imaps.enableimapevents", String.valueOf(enabled));
		return properties;
	}
	
	public static Properties useCustomizedIMAPFolderClasses(final Properties properties) {
		properties.setProperty("mail.imap.folder.class", "com.sonicle.mail.imap.SonicleIMAPFolder");
		properties.setProperty("mail.imaps.folder.class", "com.sonicle.mail.imap.SonicleIMAPFolder");
		return properties;
	}
	
	public static Properties useCustomizedIMAPSocketFactory(final Properties properties, final SonicleIMAPSocketTracker socketTracker) {
		SonicleIMAPSocketFactory plainFactory = new SonicleIMAPSocketFactory(socketTracker);
		SonicleIMAPSSLSocketFactory sslFactory = new SonicleIMAPSSLSocketFactory(socketTracker);
		properties.put("mail.imap.socketFactory", plainFactory);
		properties.put("mail.imap.ssl.socketFactory", sslFactory);
		properties.put("mail.imaps.socketFactory", plainFactory);
		properties.put("mail.imaps.ssl.socketFactory", sslFactory);
		return properties;
	}
	
	public static Properties avoidSlownessOnGettingLocalHostName(final Properties properties) {
		// Calls to InternetAddress.getLocalHostName can be slow, especially 
		// in dev/local environments, when the internal flow tries to call 
		// the inner method getCanonicalHostName. This tries to reverse the 
		// discovered InetAddress for getting a FQDN, this can be slow on 
		// environments without a DNS properly configured.
		// Also MimeMessage.reply() method may be affected.
		// https://github.com/jakartaee/mail-api/blob/master/api/src/main/java/jakarta/mail/internet/MimeMessage.java#L1738C13-L1738C75
		// https://github.com/jakartaee/mail-api/blob/master/api/src/main/java/jakarta/mail/internet/InternetAddress.java#L664C27-L664C47
		
		properties.setProperty("mail.mime.address.usecanonicalhostname", "false");
		return properties;
	}
}
