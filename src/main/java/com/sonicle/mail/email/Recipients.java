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

import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.sf.qualitycheck.Check;

/**
 *
 * @author malbinola
 */
public class Recipients {
	
	public static Builder to(InternetAddress internetAddress) {
		return new Builder().to(internetAddress);
	}
	
	public static Builder cc(InternetAddress internetAddress) {
		return new Builder().cc(internetAddress);
	}
	
	public static Builder bcc(InternetAddress internetAddress) {
		return new Builder().bcc(internetAddress);
	}
	
	public static Builder with(Recipient recipient) {
		return new Builder().with(recipient);
	}
	
	public static class Builder {
		private List<Recipient> rcpts = new ArrayList<>();
		
		public Builder to(InternetAddress internetAddress) {
			return with(new Recipient(internetAddress, Message.RecipientType.TO));
		}
		
		public Builder cc(InternetAddress internetAddress) {
			return with(new Recipient(internetAddress, Message.RecipientType.CC));
		}
		
		public Builder bcc(InternetAddress internetAddress) {
			return with(new Recipient(internetAddress, Message.RecipientType.BCC));
		}
		
		public Builder with(Recipient recipient) {
			rcpts.add(Check.notNull(recipient, "recipient"));
			return this;
		}
		
		public List<Recipient> asList() {
			return Collections.unmodifiableList(rcpts);
		}
	}
}
