/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sonicle.mail.imap;

import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks sockets opened by {@link SonicleIMAPSocketFactory} /
 * {@link SonicleIMAPSSLSocketFactory} so they can be force-closed at
 * disconnect, breaking out of any blocked read/write (e.g. IDLE over a
 * dead VPN socket) without going through JavaMail's synchronized APIs.
 *
 * @author gabriele.bulfon
 */
public class SonicleIMAPSocketTracker {

	private final Set<Socket> sockets = ConcurrentHashMap.newKeySet();

	public void track(Socket s) {
		if (s != null) sockets.add(s);
	}

	public void closeAll() {
		for (Socket s : sockets) {
			try { s.close(); } catch (Exception ignore) {}
		}
		sockets.clear();
	}
}
