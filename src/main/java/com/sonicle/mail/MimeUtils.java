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

import com.sun.mail.util.PropUtil;
import jakarta.mail.Address;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimePart;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.internet.ParseException;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.qualitycheck.Check;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author malbinola
 */
public class MimeUtils {
	private static final boolean encodeFileNameProp = PropUtil.getBooleanSystemProperty("mail.mime.encodefilename", false);
	private static final boolean decodeTextStrictProp = PropUtil.getBooleanSystemProperty("mail.mime.decodetext.strict", true);
	public static final String HEADER_FROM = "From";
	public static final String HEADER_TO = "To";
	public static final String HEADER_SUBJECT = "Subject";
	public static final String HEADER_RECEIVED = "Received";
	public static final String HEADER_MESSAGE_ID = "Message-ID";
	public static final String HEADER_CONTENT_LOCATION = "Content-Location";
	public static final String HEADER_CONTENT_ID = "Content-ID";
	public static final String CONTENT_ID_WRAPPER = "<>";
	public static final String HEADER_CONTENT_TYPE = "Content-Type";
	
	public static final String HEADER_DISP_NOTIFICATION_TO = "Disposition-Notification-To";
	public static final String HEADER_RETURN_RECEIPT_TO = "Return-Receipt-To";
	public static final String HEADER_CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
	
	public static final String CTYPE_TEXT_PLAIN = "text/plain";
	public static final String CTYPE_TEXT_HTML = "text/html";
	public static final String CTYPE_TEXT_CALENDAR = "text/calendar";
	public static final String CTYPE_APPLICATION_ICS = "application/ics";
	public static final String CTYPE_APPLICATION_MSTNEF = "application/ms-tnef";
	public static final String CTYPE_MESSAGE_RFC822 = "message/rfc822";
	
	// Regex for matching downlevel-revealed comments: see https://en.wikipedia.org/wiki/Conditional_comment
	// This kind of syntax often causes HTML validation errors due to presence 
	// of uncommented tags only valid for IE browsers.
	// (https://www.sitepoint.com/internet-explorer-conditional-comments/)
	public static final String MATCH_DOWNLEVEL_REVEALED = "<\\!(\\[if .+?\\])>|<\\!(\\[endif\\])>"; // Non-greedy match (please note ? char)
	public static final Pattern PATTERN_DOWNLEVEL_REVEALED = Pattern.compile(MATCH_DOWNLEVEL_REVEALED, Pattern.MULTILINE);
	
	/**
	 * Sanitizes a string, generally an HTML body, transforming downlevel-revealed
	 * comments in order to hide them to browsers different from IE.
	 * @param s The String to be sanitized
	 * @return The sanitized String
	 */
	public static String sanitizeDownlevelRevealedComments(String s) {
		Matcher matcher = PATTERN_DOWNLEVEL_REVEALED.matcher(s);
		StringBuffer sb = new StringBuffer(s.length());
		while (matcher.find()) {
			if (StringUtils.containsIgnoreCase(matcher.group(1), "endif")) {
				matcher.appendReplacement(sb, Matcher.quoteReplacement("<!--<!" + matcher.group(1) + "-->"));
			} else {
				matcher.appendReplacement(sb, Matcher.quoteReplacement("<!--" + matcher.group(1) + "><!-->"));
			}
		}
		matcher.appendTail(sb);
		return sb.toString();
	}
	
	/**
	 * Returns the decoded (as per RFC 822) text value, if an exception is 
	 * thrown during operation, the raw text is simply returned.
	 * @param text The possibly encoded value
	 * @return The decoded text
	 */
	public static String quietlyDecodeText(String text) {
		try {
			return decodeText(text);
		} catch (UnsupportedEncodingException ex) {
			return text;
		}
	}
	
	/**
	 * Returns the decoded (as per RFC 822) text value.
	 * @param text The text to be decoded.
	 * @return Decoded text
	 * @throws UnsupportedEncodingException if encoding is not supported
	 */
	public static String decodeText(final String text) throws UnsupportedEncodingException {
		return MimeUtility.decodeText(text);
	}
	
	/**
	 * Returns the Q-decoded text value.
	 * @param text The text to be decoded.
	 * @return Decoded text
	 */
	public static String decodeQText(final String text) {
		if (text == null) return text;
		if (!Normalizer.isNormalized(text, Normalizer.Form.NFC)) {
			return Normalizer.normalize(text, Normalizer.Form.NFC);
		}
		return text;
	}
	
	/**
	 * Encode text to make sure email clients can interpret text properly, 
	 * we need to encode some values according to RFC-2047.
	 * @param text The text to be encoded.
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	public static String encodeText(final String text) throws UnsupportedEncodingException {
		if (text == null) return text;
		return MimeUtility.encodeText(text, StandardCharsets.UTF_8.name(), "B");
	}
	
	/**
	 * 
	 * @param mimeMessage
	 * @param recipientType
	 * @return
	 * @throws MessagingException 
	 */
	public static Address[] getRecipients(final MimeMessage mimeMessage, final RecipientType recipientType) throws MessagingException {
		Check.notNull(mimeMessage, "mimeMessage");
		Check.notNull(recipientType, "recipientType");
		// return mimeMessage.getRecipients(recipientType); // can fail in strict mode, see https://github.com/bbottema/simple-java-mail/issues/227
		// workaround following (copied and modified from JavaMail internal code):
		String s = mimeMessage.getHeader(getRecipientsHeaderName(recipientType), ",");
		return (s == null) ? null : InternetAddress.parseHeader(s, false);
	}
	
	private static String getRecipientsHeaderName(final RecipientType recipientType) {
		if (RecipientType.TO.equals(recipientType)) {
			return "To";
		} else if (RecipientType.CC.equals(recipientType)) {
			return "Cc";
		} else {
			return "Bcc";
		}
	}
	
	/**
	 * Converts a collection of Addresses into a list of InternetAddresses
	 * @param recipients Collection of Address
	 * @return 
	 */
	public static List<InternetAddress> toInternetAddresses(final Address[] recipients) {
		final List<Address> addresses = (recipients != null) ? Arrays.asList(recipients) : new ArrayList<>();
		final List<InternetAddress> list = new ArrayList<>();
		for (final Address address : addresses) {
			if (address instanceof InternetAddress) {
				list.add((InternetAddress) address);
			}
		}
		return list;
	}
	
	/**
	 * Returns the first header's value with the specified name.
	 * @param part The part to extract data from.
	 * @param name The header name.
	 * @return Header's value
	 * @throws MessagingException 
	 */
	public static String getFirstHeaderValue(final Part part, final String name) throws MessagingException {
		return getHeaderValue(part, name, null);
	}
	
	/**
	 * Returns the header values with the specified name.
	 * @param part The part to extract data from.
	 * @param name The header name.
	 * @param delimiter The delimiter to use for joining multiple values, set to null to return only the first value found.
	 * @return
	 * @throws MessagingException 
	 */
	public static String getHeaderValue(final Part part, final String name, final String delimiter) throws MessagingException {
		Check.notNull(part, "part");
		Check.notEmpty(name, "name");
		String[] headers = part.getHeader(name);
		if (headers == null || headers.length == 0) {
			return null;
		} else {
			return (delimiter != null) ? StringUtils.join(headers, delimiter) : headers[0];
		}
	}
	
	/**
	 * Strips whitespaces by reading passed string by lines, joining each line 
	 * after trimming it. Useful for headers whose values are splitted over more lines.
	 * @param value The header value.
	 * @return Stripped header value or null if read content is empty.
	 */
	public static String stripHeaderValueWhitespaces(String value) {
		if (value != null) {
			try {
				String s = "";
				String line;
				BufferedReader br = new BufferedReader(new StringReader(value));
				while ((line = br.readLine()) != null) s += line.trim();
				return StringUtils.defaultIfEmpty(s, null);
			} catch (IOException ex) { /* Do nothing... */ }
		}
		return null;
	}
	
	/**
	 * Helper which returns the fileName of the part, decoding any encoded 
	 * (for eg. Q-String) tokens of the String.
	 * @param part The part to extract data from.
	 * @param fallbackOnSubType Set to true to use contentType subType in case of missing filename in main header.
	 * @return Filename value (properly decoded) or null
	 * @throws MessagingException
	 * @throws UnsupportedEncodingException 
	 */
	public static String getFilename(final Part part, final boolean fallbackOnSubType) throws MessagingException, UnsupportedEncodingException {
		Check.notNull(part, "part");
		
		// Internally JavaMail already take into account any decoding only if
		// the property "mail.mime.decodefilename" is set to "true". In order
		// to make sure that any other encoding will be handled well (eg. QString)
		// system property "mail.mime.decodetext.strict" should be set to "false".
		
		// A valid filename is searched before in Content-Disposition header:
		//	"attachment; filename="Undelivered Mail Returned to Sender.eml""
		// ..and then in Content-Type header:
		//	"message/rfc822; name="Undelivered Mail Returned to Sender.eml""
		
		String filename = part.getFileName();
		if (fallbackOnSubType && (filename == null)) {
			String ctype = part.getContentType();
			if (ctype != null) filename = new ContentType(ctype).getSubType();
		}
		
		if (!encodeFileNameProp && (filename != null)) {
			filename = decodeText(filename);
		}
		if (decodeTextStrictProp && (filename != null)) filename = decodeQText(filename);
		return filename;
	}
	
	/**
	 * Helper which returns the Contect-ID of the part, or null in case of header is NOT there.
	 * Heavily inspired by https://www.tabnine.com/web/assistant/code/rs/5c6587461095a500015c5582#L806
	 * @param part The part to extract the data from.
	 * @return Content-ID header value (without the wrapping "< and >" character) or null in case of missin header
	 * @throws MessagingException 
	 */
	public static String getContentID(final Part part) throws MessagingException {
		Check.notNull(part, "part");
		String[] headers = part.getHeader(HEADER_CONTENT_ID);
		if (headers != null && headers.length > 0) {
			for (String value : headers) {
				if (!StringUtils.isBlank(value)) {
					return StringUtils.strip(value, CONTENT_ID_WRAPPER);
				}
			}
		}
		return null;
	}
	
	public static boolean isMimeType(final Part part, final String mimeType) throws MessagingException {
		// Do NOT use part.isMimeType() due it's broken for MimeBodyPart
		try {
			final ContentType contentType = new ContentType(part.getDataHandler().getContentType());
			return contentType.match(mimeType);
		} catch (ParseException ex) {
			return getContentTypeStrict(part).equalsIgnoreCase(mimeType);
		}
	}
	
	/**
	 * Returns ContentType object of related header, or null if missing.
	 * @param part The part to extract data from.
	 * @return The ContentType object
	 * @throws MessagingException 
	 */
	public static ContentType getContentType(final Part part) throws MessagingException {
		Check.notNull(part, "part");
		final String ctype = part.getContentType();
		return (ctype != null) ? new ContentType(ctype) : null;
	}
	
	/**
	 * Helper which returns the pure mime/subMime content type without any other 
	 * extra parameters which may accompany the header value.
	 * @param part The part to extract data from.
	 * @return The pure mime/subMime type
	 * @throws MessagingException if retrieving the part's Content-Type header fails
	 */
	public static String getContentTypeStrict(final Part part) throws MessagingException {
		Check.notNull(part, "part");
		return getContentTypeStrict(part.getContentType());
	}
	
	/**
	 * Helper which extracts the content type from a header value removing 
	 * parameters and so on.
	 * @param headerValue The header value.
	 * @return The actual content type
	 */
	public static String getContentTypeStrict(final String headerValue) {
		return StringUtils.trim(StringUtils.substringBefore(headerValue, ";"));
		/*
		Check.notEmpty(headerValue, "headerValue");
		String ret = headerValue;
		final int semiColon = headerValue.indexOf(';');
		if (-1 != semiColon) {
			ret = headerValue.substring(0, semiColon);
		}
		return ret.trim();
		*/
	}
	
	/**
	 * Uses standard JDK java to read an inputstream to String using the given encoding.
	 * @param inputStream
	 * @param charset
	 * @return
	 * @throws IOException 
	 */
	public static String readInputStreamToString(final InputStream inputStream, final Charset charset) throws IOException {
		Check.notNull(inputStream, "inputStream");
		Check.notNull(charset, "charset");
		final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		int result = bufferedInputStream.read();
		while (result != -1) {
			byteArrayOutputStream.write((byte) result);
			result = bufferedInputStream.read();
		}
		return byteArrayOutputStream.toString(charset.name());
	}
	
	/**
	 * Uses standard JDK java to read an inputstream to byte[].
	 * @param inputStream
	 * @return
	 * @throws IOException 
	 */
	public static byte[] readInputStreamToBytes(final InputStream inputStream) throws IOException {
		Check.notNull(inputStream, "inputStream");
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] data = new byte[1024];
		int read;
		while ((read = inputStream.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, read);
		}
		buffer.flush();
		return buffer.toByteArray();
	}
}
