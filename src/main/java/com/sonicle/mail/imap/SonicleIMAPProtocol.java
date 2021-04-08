/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sonicle.mail.imap;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.imap.protocol.MessageSet;
import com.sun.mail.util.MailLogger;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author gabriele.bulfon
 */
public class SonicleIMAPProtocol extends IMAPProtocol {

	public SonicleIMAPProtocol(String name, String host, int port, Properties props, boolean isSSL, MailLogger logger) throws IOException, ProtocolException {
		super(name, host, port, props, isSSL, logger);
	}
	
	
	//Always tries to translate msgnum into uid, that's what we're working with
/*	@Override
    public Response[] fetch(int msg, String what) 
			throws ProtocolException {
		return fetch(String.valueOf(msg), what, false);
    }
	
	
	private Response[] fetch(String msgSequence, String what, boolean uid)
			throws ProtocolException {
		if (uid) {
			return command("UID FETCH " + msgSequence + " (" + what + ")", null);
		} else {
			return command("FETCH " + msgSequence + " (" + what + ")", null);
		}
	}*/
}
