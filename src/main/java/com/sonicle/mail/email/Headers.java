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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import net.sf.qualitycheck.Check;

/**
 *
 * @author malbinola
 */
public class Headers {
	
	public static class Builder {
		// According to RFC 5322, a header field-name must consist of
		// printable US-ASCII characters excluding colon ":" and space.
		// This regex accepts: 0x21-0x39, 0x3B-0x7E (i.e. !-9 and ;-~)
		private static final Pattern HEADER_NAME = Pattern.compile("^[!-9;-~]+$");
		private final Map<String, Collection<Object>> headers = new LinkedHashMap<>();
		
		public Builder addHeader(final String name, final Object value) {
			validateHeaderName(name);
			validateHeaderValue(value);
			
			headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
			return this;
		}
		
		public Builder addHeaders(final String name, final Collection<?> values) {
			validateHeaderName(name);
			Check.notNull(values, "values");
			for (Object value : values) validateHeaderValue(value);
			
			headers.computeIfAbsent(name, k -> new ArrayList<>()).addAll(values);
			return this;
		}
		
		public Builder setHeader(final String name, final Object value) {
			validateHeaderName(name);
			validateHeaderValue(value);
			
			Collection<Object> list = new ArrayList<>();
			list.add(value);
			headers.put(name, list);
			return this;
		}
		
		public Builder removeHeader(final String name) {
			validateHeaderName(name);
			
			headers.remove(name);
			return this;
		}
		
		public Map<String, Collection<Object>> build() {
			Map<String, Collection<Object>> result = new LinkedHashMap<>();
			for (Map.Entry<String, Collection<Object>> entry : headers.entrySet()) {
				result.put(
					entry.getKey(),
					Collections.unmodifiableCollection(new ArrayList<>(entry.getValue()))
				);
			}
			return Collections.unmodifiableMap(result);
		}
		
		/**
		 * Validates header name according to RFC 5322:
		 * - must not be null or blank
		 * - must contain only allowed printable ASCII characters
		 * - must not contain colon ":" or whitespace
		 * @param name 
		 */
		private static void validateHeaderName(final String name) {
			Check.notEmpty(name, "name");
			
			if (!HEADER_NAME.matcher(name).matches()) {
				throw new IllegalArgumentException("Invalid header name: " + name);
			}
		}
		
		/**
		 * Protects against header injection attacks:
		 * - Disallows CR (\r) and LF (\n)
		 * - Disallows other control characters (0x00-0x1F and 0x7F)
		 * 
		 * Even if the value is not a String, it will be serialized
		 * to text when used as an email header, so we validate
		 * its String representation.
		 * 
		 * Jakarta Mail handles proper encoding and line folding,
		 * but this validation prevents malicious input from injecting
		 * additional headers.
		 * @param value 
		 */
		private static void validateHeaderValue(final Object value) {
			Check.notNull(value, "value");
			String s = String.valueOf(value);
			
			// Prevent CRLF injection
			if (s.indexOf('\r') >= 0 || s.indexOf('\n') >= 0) {
				throw new IllegalArgumentException("Invalid header value: contains CR/LF");
			}
			// Prevent other control characters
			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				if ((c <= 0x1F) || (c == 0x7F)) {
					throw new IllegalArgumentException("Invalid header value: contains control character");
				}
			}
		}
	}
}
