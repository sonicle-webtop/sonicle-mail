/*
 * sonicle-mail is is a helper library developed by Sonicle S.r.l.
 * Copyright (C) 2022 Sonicle S.r.l.
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
 * display the words "Copyright (C) 2022 Sonicle S.r.l.".
 */
package com.sonicle.mail;

import com.sonicle.commons.EnumUtils;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Properties;
import net.sf.qualitycheck.Check;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author malbinola
 */
public enum StoreProtocol {
	// https://docs.oracle.com/javaee/6/api/javax/mail/package-summary.html
	// https://javaee.github.io/javamail/docs/api/com/sun/mail/imap/package-summary.html
	// https://github.com/simple-java-mail/core-module/blob/develop/src/main/java/org/simplejavamail/api/mailer/config/TransportStrategy.java
	// https://blog.eduonix.com/java-programming-2/learn-use-java-mail-api-send-receive-emails/
	
	IMAP {
		@Override
		public String getProtocol() {
			return "imap";
		}
		
		@Override
		public String getPropertyName(String remaining) {
			return "mail." + getProtocol() + "." + remaining;
		}
		
		@Override
		public void applyProperties(Properties props) {
			props.setProperty("mail.store.protocol", getProtocol());
			props.setProperty(getPropertyName("starttls.enable"), "true");
			props.setProperty(getPropertyName("starttls.required"), "false");
			props.setProperty(getPropertyName("ssl.trust"), "*");
			props.setProperty(getPropertyName("ssl.checkserveridentity"), "false");
		}
	},
	
	IMAPS {
		@Override
		public String getProtocol() {
			return "imaps";
		}
		
		@Override
		public String getPropertyName(String remaining) {
			return "mail." + getProtocol() + "." + remaining;
		}
		
		@Override
		public void applyProperties(Properties props) {
			props.setProperty("mail.store.protocol", getProtocol());
			props.setProperty(getPropertyName("ssl.checkserveridentity"), "false");
		}
	},
	
	IMAP_STARTTLS {
		@Override
		public String getProtocol() {
			return "imap";
		}
		
		@Override
		public String getPropertyName(String remaining) {
			return "mail." + getProtocol() + "." + remaining;
		}
		
		@Override
		public void applyProperties(Properties props) {
			props.setProperty("mail.store.protocol", getProtocol());
			props.setProperty(getPropertyName("starttls.enable"), "true");
			props.setProperty(getPropertyName("starttls.required"), "true");
			props.setProperty(getPropertyName("ssl.checkserveridentity"), "true");
		}
	},
	
	POP3 {
		@Override
		public String getProtocol() {
			return "pop3";
		}
		
		@Override
		public String getPropertyName(String remaining) {
			return "mail." + getProtocol() + "." + remaining;
		}
		
		@Override
		public void applyProperties(Properties props) {
			props.setProperty("mail.store.protocol", getProtocol());
			props.setProperty(getPropertyName("starttls.enable"), "true");
			props.setProperty(getPropertyName("starttls.required"), "false");
			props.setProperty(getPropertyName("ssl.trust"), "*");
			props.setProperty(getPropertyName("ssl.checkserveridentity"), "false");
		}
	},
	
	POP3S {
		@Override
		public String getProtocol() {
			return "pop3s";
		}
		
		@Override
		public String getPropertyName(String remaining) {
			return "mail." + getProtocol() + "." + remaining;
		}
		
		@Override
		public void applyProperties(Properties props) {
			props.setProperty("mail.store.protocol", getProtocol());
			props.setProperty(getPropertyName("ssl.checkserveridentity"), "false");
		}
	},
	
	POP3_STARTTLS {
		@Override
		public String getProtocol() {
			return "pop3";
		}
		
		@Override
		public String getPropertyName(String remaining) {
			return "mail." + getProtocol() + "." + remaining;
		}
		
		@Override
		public void applyProperties(Properties props) {
			props.setProperty("mail.store.protocol", getProtocol());
			props.setProperty(getPropertyName("starttls.enable"), "true");
			props.setProperty(getPropertyName("starttls.required"), "true");
			props.setProperty(getPropertyName("ssl.checkserveridentity"), "true");
		}
	};
	
	public abstract String getProtocol();
	public abstract String getPropertyName(String remaining);
	public abstract void applyProperties(Properties props);
	
	/*
	public void sslTrustAllHosts(Properties props) throws GeneralSecurityException {
		sslTrustHosts(props, null);
	}
	
	public void sslTrustHosts(Properties props, Collection<String> hosts) throws GeneralSecurityException {
		MailSSLSocketFactory ssf = new MailSSLSocketFactory();
		if (hosts == null) {
			ssf.setTrustAllHosts(true);
		} else {
			ssf.setTrustedHosts(hosts.toArray(new String[hosts.size()]));
		}
		props.put(getPropertyName("ssl.socketFactory"), ssf);
	}
	*/
	
	public void sslCheckHostIdentity(Properties props, boolean verifyServerIdentity) {
		Check.notNull(props, "props");
		props.setProperty(getPropertyName("ssl.checkserveridentity"), String.valueOf(verifyServerIdentity));
	}
	
	public void sslTrustAllHosts(Properties props) throws GeneralSecurityException {
		sslTrustHosts(props, null);
	}
	
	public void sslTrustHosts(Properties props, Collection<String> hosts) throws GeneralSecurityException {
		Check.notNull(props, "props");
		String value = "*";
		if (hosts != null) {
			value = StringUtils.join(hosts, " ");
		}
		props.put(getPropertyName("ssl.trust"), value);
	}
	
	public static StoreProtocol parse(String protocol, boolean starttls) {
		if (starttls) {
			return EnumUtils.forName(StringUtils.upperCase(protocol) + "_STARTTLS", StoreProtocol.class);
		} else {
			return EnumUtils.forName(StringUtils.upperCase(protocol), StoreProtocol.class);
		}
	}
}
