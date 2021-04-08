/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sonicle.mail.imap.test;

import com.sonicle.mail.imap.SonicleIMAPFolder;
import com.sonicle.mail.imap.SonicleIMAPMessage;
import java.util.Properties;
import jakarta.mail.*;
import javax.swing.JOptionPane;
/**
 *
 * @author gabriele.bulfon
 */
public class Thread {
	
	public static void main(String args[]) throws Exception {
		String host=JOptionPane.showInputDialog("Host");
		String user=JOptionPane.showInputDialog("User");
		String password=JOptionPane.showInputDialog("Password");
		Properties props=new Properties();
		props.setProperty("mail.imap.folder.class","com.sonicle.mail.imap.SonicleIMAPFolder");
		props.setProperty("mail.imaps.folder.class","com.sonicle.mail.imap.SonicleIMAPFolder");
		Session session=Session.getInstance(props);
		//session.setDebug(true);
		Store store=session.getStore("imap");
		store.connect(host, 143, user, password);
		SonicleIMAPFolder folder=(SonicleIMAPFolder)store.getFolder("INBOX");
		folder.open(Folder.READ_ONLY);
		jakarta.mail.search.FromStringTerm fst=new jakarta.mail.search.FromStringTerm("fullone");
		FetchProfile FP = new FetchProfile();
		FP.add(FetchProfile.Item.ENVELOPE);
		FP.add(FetchProfile.Item.FLAGS);
		FP.add(FetchProfile.Item.CONTENT_INFO);
		FP.add(UIDFolder.FetchProfileItem.UID);
		FP.add("Message-ID");
		FP.add("X-Priority");
		SonicleIMAPMessage sims[]=folder.thread("REFERENCES",fst,FP);
		
		for(SonicleIMAPMessage sim: sims) {
			String line="";
			if (sim.getThreadSize()>1) {
				if (!sim.isMostRecentInThread()) line+=" -- ";
			}
			line+=sim.getReceivedDate()+" - "+sim.getSubject();
			System.out.println(line);
		}
		
		folder.close(true);
		store.close();
	}
	
}
