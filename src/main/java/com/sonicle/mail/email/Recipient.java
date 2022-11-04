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

import com.sonicle.commons.InternetAddressUtils;
import com.sonicle.commons.LangUtils;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.internet.InternetAddress;
import java.io.Serializable;
import net.sf.qualitycheck.Check;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 *
 * @author malbinola
 */
public class Recipient implements Serializable {
	private static final long serialVersionUID = 1234567L;
	private final String address;
	private final String name;
	private final RecipientType type;
	
	public Recipient(final String address, final String name, final RecipientType type) {
		this.address = Check.notEmpty(address, "address");
		this.name = name;
		this.type = type;
	}
	
	public Recipient(final InternetAddress internetAddress, final RecipientType type) {
		this(Check.notNull(internetAddress, "internetAddress").getAddress(), internetAddress.getPersonal(), type);
	}

	public String getAddress() {
		return address;
	}

	public String getName() {
		return name;
	}

	public RecipientType getType() {
		return type;
	}
	
	public InternetAddress asInternetAddress() {
		return InternetAddressUtils.toInternetAddress(address, name);
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder()
			.append(address)
			.append(name)
			.append(type)
			.toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Recipient == false) return false;
		if (this == obj) return true;
		final Recipient otherObject = (Recipient)obj;
		return new EqualsBuilder()
			.append(address, otherObject.address)
			.append(name, otherObject.name)
			.append(type, otherObject.type)
			.isEquals();
	}
	
	public static Recipient from(final String name, boolean fixedName, final String emailAddress, final RecipientType type) {
		InternetAddress ia = InternetAddressUtils.toInternetAddress(emailAddress);
		if (ia == null) throw new IllegalArgumentException();
		final String relevantName = (fixedName || ia.getPersonal() == null)
			? LangUtils.coalesce(name, ia.getPersonal())
			: LangUtils.coalesce(ia.getPersonal(),name);
		return new Recipient(ia.getAddress(), relevantName, type);
	}
}
