/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sonicle.mail.imap.test;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.mail.*;
import javax.swing.JOptionPane;
/**
 *
 * @author gabriele.bulfon
 */
public class Dovecot {
	
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

		Folder dfolder=store.getDefaultFolder();
		for(Folder folder: dfolder.list()) {
			System.out.println(folder.getFullName());
			for(Folder child: folder.list()) {
				System.out.println("  child: "+child.getFullName());
			}
		}
		Folder shares[]=store.getUserNamespaces("");
		for(Folder share: shares) {
			System.out.println("Share NS: "+share.getFullName());
			if ((share.getType()&IMAPFolder.HOLDS_MESSAGES)==0) {
				System.out.println(" [ NO MESSAGES HERE ] ");
			}
			for(Folder child: share.list()) {
				System.out.println("  Folder: "+child.getFullName());
				for(Folder subchild: child.list()) {
					System.out.println("    Subfolder: "+subchild.getFullName());
				}
			}
		}
		
		//folder.close(true);
		store.close();
	}
	
}
