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
package com.sonicle.mail.imap;

import com.sun.mail.util.MailSSLSocketFactory;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Builds SSLSocketFactory delegates carrying a specific trust policy,
 * to be wrapped by {@link SonicleIMAPSSLSocketFactory}.
 */
public final class TrustFactories {

	private TrustFactories() {}

	/**
	 * Accepts a server whose leaf certificate matches one of the given
	 * SHA-256 fingerprints; otherwise falls back to the JVM default trust.
	 * Fingerprints may contain ':' separators and any case.
	 */
	public static SSLSocketFactory pinned(final Collection<String> sha256Fingerprints) throws GeneralSecurityException {
		final Set<String> pins = new HashSet<>();
		for (String fp : sha256Fingerprints) pins.add(normalize(fp));

		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init((KeyStore)null);
		final X509TrustManager defaultTm = firstX509(tmf.getTrustManagers());

		X509TrustManager tm = new X509TrustManager() {
			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				if (chain != null && chain.length > 0 && pins.contains(fingerprint(chain[0]))) return;
				defaultTm.checkServerTrusted(chain, authType);
			}

			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				defaultTm.checkClientTrusted(chain, authType);
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return defaultTm.getAcceptedIssuers();
			}
		};

		SSLContext ctx = SSLContext.getInstance("TLS");
		ctx.init(null, new TrustManager[]{tm}, null);
		return ctx.getSocketFactory();
	}

	/**
	 * Trusts any certificate. Use only for hosts explicitly flagged as trusted.
	 */
	public static SSLSocketFactory trustAll() {
		SSLSocketFactory sf = null;
		try {
			MailSSLSocketFactory msf = new MailSSLSocketFactory();
			msf.setTrustAllHosts(true);
			sf = (SSLSocketFactory) msf;
		} catch(GeneralSecurityException exc) {
			sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
		}
		return sf;
	}

	private static String fingerprint(X509Certificate cert) throws CertificateException {
		try {
			return toHex(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));
		} catch (NoSuchAlgorithmException ex) {
			throw new CertificateException(ex);
		}
	}

	private static String toHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) sb.append(String.format("%02x", b));
		return sb.toString();
	}

	private static String normalize(String fp) {
		return fp.replace(":", "").replace(" ", "").toLowerCase(Locale.ROOT);
	}

	private static X509TrustManager firstX509(TrustManager[] tms) {
		for (TrustManager t : tms) {
			if (t instanceof X509TrustManager) return (X509TrustManager)t;
		}
		throw new IllegalStateException("No X509TrustManager available");
	}
}