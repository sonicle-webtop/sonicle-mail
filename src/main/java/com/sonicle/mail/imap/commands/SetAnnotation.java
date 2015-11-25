/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sonicle.mail.imap.commands;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 *
 * @author gbulfon
 */
public class SetAnnotation implements IMAPFolder.ProtocolCommand {
	
	String foldername;
	String key;
	boolean shared;
	String value;
	
	public SetAnnotation(String foldername, String key, boolean shared, String value) {
		this.foldername=foldername;
		this.key=key;
		this.shared=shared;
		this.value=value;
	}

	public Object doCommand(IMAPProtocol imapp) throws ProtocolException {
//		Argument args=new Argument();
//		args.writeString(foldername);
//		args.writeString(key);
//		args.writeString(shared?"value.shared":"value.priv");
		
		//String cmd="GETANNOTATION";
		String cmd="SETANNOTATION \""+foldername+"\" \""+key+"\" (\""+(shared?"value.shared":"value.priv")+"\" \""+value+"\")";
		
		Response[] r=imapp.command(cmd, null);
		Response status = r[0];
		
		if (!status.isOK()) {
			throw new ProtocolException("Status error on SETANNOTATION: "+status.toString());
		}
		
		return null;
	}
	

}
