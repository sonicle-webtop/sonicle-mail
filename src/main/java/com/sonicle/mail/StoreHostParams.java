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

import net.sf.qualitycheck.Check;

/**
 *
 * @author malbinola
 */
public class StoreHostParams {
	protected String host;
	protected int port;
	protected StoreProtocol protocol;
	protected String username;
	protected String password;
	protected boolean trustHost = false;
	protected ImpersonateMode impersonateMode = ImpersonateMode.NONE;
	protected String adminUsername;
	
	public StoreHostParams() {}
	
	public StoreHostParams(String host, int port, StoreProtocol protocol) {
		this.host = host;
		this.port = port;
		this.protocol = Check.notNull(protocol, "protocol");
	}
	
	public StoreHostParams(String host, int port, StoreProtocol protocol, String username, String password) {
		this(host, port, protocol);
		this.username = username;
		this.password = password;
	}

	public String getHost() {
		return host;
	}
	
	public int getPort() {
		return port;
	}
	
	public StoreProtocol getProtocol() {
		return protocol;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public boolean getTrustHost() {
		return trustHost;
	}
	
	public ImpersonateMode getImpersonateMode() {
		return impersonateMode;
	}
	
	public String getAdminUsername() {
		return adminUsername;
	}
	
	public StoreHostParams withHost(String host) {
		this.host = host;
		return this;
	}
	
	public StoreHostParams withPort(int port) {
		this.port = port;
		return this;
	}
	
	public StoreHostParams withProtocol(StoreProtocol protocol) {
		this.protocol = Check.notNull(protocol, "protocol");
		return this;
	}
	
	public StoreHostParams withUsername(String username) {
		this.username = username;
		return this;
	}
	
	public StoreHostParams withPassword(String password) {
		this.password = password;
		return this;
	}
	
	public StoreHostParams withTrustHost(boolean trustHost) {
		this.trustHost = trustHost;
		return this;
	}
	
	public StoreHostParams withImpersonateMode(ImpersonateMode impersonateMode) {
		this.impersonateMode = Check.notNull(impersonateMode, "impersonateMode");
		return this;
	}
		
	public StoreHostParams withAdminUsername(String adminUsername) {
		this.adminUsername = adminUsername;
		return this;
	}
	
	public StoreHostParams withSASLImpersonate(String adminUsername, String adminPassword) {
		withAdminUsername(Check.notEmpty(adminUsername, "adminUsername"));
		withPassword(Check.notEmpty(adminPassword, "adminPassword"));
		return withImpersonateMode(ImpersonateMode.SASL);
	}
	
	public StoreHostParams withVMAILImpersonate(String vmailPassword) {
		withPassword(Check.notEmpty(vmailPassword, "vmailPassword"));
		return withImpersonateMode(ImpersonateMode.VMAIL);
	}
}
