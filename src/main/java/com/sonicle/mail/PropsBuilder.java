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

import java.util.Properties;
import net.sf.qualitycheck.Check;

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
	
	public PropsBuilder withDebug() {
		props.setProperty("mail.debug", "true");
		return this;
	}
	
	public PropsBuilder withSocketDebug() {
		props.setProperty("mail.socket.debug", "true");
		return this;
	}
	
	public PropsBuilder withParseDebug() {
		props.setProperty("mail.imap.parse.debug", "true");
		props.setProperty("mail.imaps.parse.debug", "true");
		return this;
	}
	
	public PropsBuilder withSonicleIMAPFolder() {
		props.setProperty("mail.imap.folder.class", "com.sonicle.mail.imap.SonicleIMAPFolder");
		props.setProperty("mail.imaps.folder.class", "com.sonicle.mail.imap.SonicleIMAPFolder");
		return this;
	}
	
	public PropsBuilder withEnableIMAPEvents() {
		props.setProperty("mail.imap.enableimapevents", "true");
		props.setProperty("mail.imaps.enableimapevents", "true");
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
}
