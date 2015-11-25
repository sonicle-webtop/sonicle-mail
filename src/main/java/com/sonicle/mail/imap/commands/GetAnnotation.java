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
/**
 *
 * @author gbulfon
 */
public class GetAnnotation implements IMAPFolder.ProtocolCommand {
	
	String foldername;
	String key;
	boolean shared;
	
	public GetAnnotation(String foldername, String key, boolean shared) {
		this.foldername=foldername;
		this.key=key;
		this.shared=shared;
	}

	public Object doCommand(IMAPProtocol imapp) throws ProtocolException {
//		Argument args=new Argument();
//		args.writeString(foldername);
//		args.writeString(key);
//		args.writeString(shared?"value.shared":"value.priv");
		
		//String cmd="GETANNOTATION";
		String cmd="GETANNOTATION \""+foldername+"\" \""+key+"\" \""+(shared?"value.shared":"value.priv")+"\"";
		
		Response[] r=imapp.command(cmd, null);
		Response status = r[1];
		
		if (status.isOK()) {
			IMAPResponse ir=(IMAPResponse) r[0];
			if (ir.keyEquals("ANNOTATION")) {
				String rfoldername=ir.readString();
				String rkey=ir.readString();
				ir.skipSpaces();
				if (ir.readByte()=='(') {
					String rshared=ir.readString();
					String rvalue=ir.readString();
					return rvalue;
				} else {
					throw new ProtocolException("Open parenthesis not found in value for ANNOTATION");
				}
			}
		}
		
		throw new ProtocolException("Status error on ANNOTATION: "+status.toString());
	}
	
}
