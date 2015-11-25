/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sonicle.mail.imap.test;

import com.sun.mail.imap.IMAPStore;
import java.util.Properties;
import javax.mail.*;
import javax.mail.search.*;
import javax.swing.JOptionPane;
/**
 *
 * @author gabriele.bulfon
 */
public class Tagging {
	
	public static void main(String args[]) throws Exception {
		String host=JOptionPane.showInputDialog("Host");
		String user=JOptionPane.showInputDialog("User");
		String password=JOptionPane.showInputDialog("Password");
		Properties props=new Properties();
		props.setProperty("mail.imap.folder.class","com.sonicle.mail.imap.SonicleIMAPFolder");
		props.setProperty("mail.imaps.folder.class","com.sonicle.mail.imap.SonicleIMAPFolder");
		props.setProperty("mail.imaps.ssl.trust", "*");
		Session session=Session.getInstance(props);
		//session.setDebug(true);
		IMAPStore store=(IMAPStore)session.getStore("imaps");
		store.connect(host, 993, user, password);

		Folder folder=store.getFolder("Prese");
		folder.open(Folder.READ_WRITE);
		Flags flags=new Flags("$Archived");
		FlagTerm fterm=new FlagTerm(flags,true);
		Message msgs[]=folder.search(fterm);
		if (msgs==null || msgs.length==0) {
			msgs=folder.getMessages();
			Message m=msgs[0];
			System.out.println("setting flag in msg '"+m.getSubject()+"'");
			m.setFlags(flags,true);
		} else {
			for(Message m: msgs) {
				System.out.println("found flag in msg '"+m.getSubject()+"'");
			}
		}
		folder.close(true);
		store.close();
	}
	
}
