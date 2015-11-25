/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sonicle.mail.imap;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.IMAPStore;
import java.io.IOException;
import javax.mail.Session;
import javax.mail.URLName;

/**
 *
 * @author gabriele.bulfon
 */
public class SonicleIMAPStore extends IMAPStore {

	public SonicleIMAPStore(Session session, URLName url) {
		super(session, url);
		//System.out.println("Creating Sonicle IMAP Store class");
	}
	
    protected SonicleIMAPStore(Session session, URLName url,
				String name, boolean isSSL) {
		super(session,url,name,isSSL);
		//System.out.println("Creating Sonicle IMAP Store class");
	}
	
    protected SonicleIMAPProtocol newIMAPProtocol(String host, int port)
				throws IOException, ProtocolException {
		//System.out.println("Creating Sonicle IMAP Protocol class");
		return new SonicleIMAPProtocol(name, host, port, 
							session.getProperties(),
							isSSL,
							logger
						   );
    }
	
	
}
