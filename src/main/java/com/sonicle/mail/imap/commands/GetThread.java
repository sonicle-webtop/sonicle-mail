/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sonicle.mail.imap.commands;

import com.sun.mail.iap.Argument;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.imap.protocol.SearchSequence;
import javax.mail.search.SearchException;
import javax.mail.search.SearchTerm;
/**
 *
 * @author gbulfon
 */
public class GetThread implements IMAPFolder.ProtocolCommand {

	private String method="REFERENCES";
	private SearchTerm term=null;
	private String charset=null;
	
	public GetThread(String method, SearchTerm term, String charset) {
		this.method=method;
		this.term=term;
		this.charset=charset;
	}

	public Object doCommand(IMAPProtocol imapp) throws ProtocolException {
		Argument args=new Argument();
		
		args.writeAtom(method);
        if (charset==null)  charset="US-ASCII";
        args.writeAtom(charset);
        Argument searchArg=null;
        if (term!=null) {
			try {
				searchArg=(new SearchSequence()).generateSequence(term, charset);
			} catch(Exception exc) {
				throw new ProtocolException(exc.getMessage());
			}
		}
        else { searchArg=new Argument(); searchArg.writeAtom("ALL"); }
        args.append(searchArg);
		
		return imapp.command("THREAD", args);
	}
	
}
