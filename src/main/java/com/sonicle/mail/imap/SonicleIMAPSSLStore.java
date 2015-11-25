/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sonicle.mail.imap;

import com.sun.mail.imap.IMAPSSLStore;
import javax.mail.Session;
import javax.mail.URLName;

/**
 *
 * @author gabriele.bulfon
 */
public class SonicleIMAPSSLStore extends SonicleIMAPStore {

    /**
     * Constructor that takes a Session object and a URLName that
     * represents a specific IMAP server.
     *
     * @param	session	the Session
     * @param	url	the URLName of this store
     */
    public SonicleIMAPSSLStore(Session session, URLName url) {
		super(session, url, "imaps", true); // call super constructor
    }
}
