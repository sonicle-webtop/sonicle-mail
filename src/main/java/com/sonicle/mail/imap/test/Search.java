/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sonicle.mail.imap.test;

import com.sun.mail.imap.IMAPMessage;
import java.util.Properties;
import jakarta.mail.*;
import jakarta.mail.search.BodyTerm;
import jakarta.mail.search.RecipientStringTerm;
import javax.swing.JOptionPane;
/**
 *
 * @author gabriele.bulfon
 */
public class Search {
	
	public static void main(String args[]) throws Exception {
		String host=null;
		String user=null;
		String password=null;
		if (args.length>=3) {
			host=args[0];
			user=args[1];
			password=args[2];
		} else {
			host=JOptionPane.showInputDialog("Host");
			user=JOptionPane.showInputDialog("User");
			password=JOptionPane.showInputDialog("Password");
		}
		Properties props=new Properties();
		Session session=Session.getInstance(props);
		//session.setDebug(true);
		Store store=session.getStore("imap");
		store.connect(host, 143, user, password);
		Folder folder=store.getFolder("user");
		Folder[] fusers=folder.list();
		for(Folder fuser: fusers) {
			Folder fsent=fuser.getFolder("Sent");
			try {
				fsent.open(Folder.READ_ONLY);
				Message msgs[]=fsent.search(new BodyTerm("pattern"));
				if (msgs.length>0) {
					System.out.println("found "+msgs.length+" in "+fsent.getFullName());
					/*for(Message msg: msgs) {
						System.out.println("=====================================");
						((IMAPMessage)msg).writeTo(System.out);
						System.out.println("=====================================");
					}*/
				}
				fsent.close(false);
			} catch(MessagingException exc) {
				
			}
		}
		store.close();
	}
	
}
