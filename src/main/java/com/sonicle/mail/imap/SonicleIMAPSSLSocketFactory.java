/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sonicle.mail.imap;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.net.ssl.SSLSocketFactory;

/**
 * SSLSocketFactory used for direct-SSL IMAP (imaps) and for the SSL
 * upgrade leg of STARTTLS. Records every socket it creates into a
 * {@link SonicleIMAPSocketTracker} so they can be hard-closed at logout.
 *
 * @author gabriele.bulfon
 */
public class SonicleIMAPSSLSocketFactory extends SSLSocketFactory {

	private final SonicleIMAPSocketTracker tracker;
	private final SSLSocketFactory delegate;

	public SonicleIMAPSSLSocketFactory(SonicleIMAPSocketTracker tracker) {
		this.tracker = tracker;
		this.delegate = (SSLSocketFactory) SSLSocketFactory.getDefault();
	}

	private Socket track(Socket s) {
		tracker.track(s);
		return s;
	}

	@Override
	public String[] getDefaultCipherSuites() {
		return delegate.getDefaultCipherSuites();
	}

	@Override
	public String[] getSupportedCipherSuites() {
		return delegate.getSupportedCipherSuites();
	}

	@Override
	public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
		return track(delegate.createSocket(s, host, port, autoClose));
	}

	@Override
	public Socket createSocket() throws IOException {
		return track(delegate.createSocket());
	}

	@Override
	public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
		return track(delegate.createSocket(host, port));
	}

	@Override
	public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
		return track(delegate.createSocket(host, port, localHost, localPort));
	}

	@Override
	public Socket createSocket(InetAddress host, int port) throws IOException {
		return track(delegate.createSocket(host, port));
	}

	@Override
	public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
		return track(delegate.createSocket(address, port, localAddress, localPort));
	}
}
