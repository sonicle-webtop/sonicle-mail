/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sonicle.mail.imap.test;

import java.util.Properties;
import jakarta.mail.*;
import javax.swing.JOptionPane;
/**
 *
 * @author gabriele.bulfon
 */
public class TestLIstMessages {
	
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
		Folder folder=store.getFolder("Test/Testà");
		folder.open(Folder.READ_ONLY);
		FetchProfile FP = new FetchProfile();
		FP.add(FetchProfile.Item.ENVELOPE);
		FP.add(FetchProfile.Item.FLAGS);
		FP.add(FetchProfile.Item.CONTENT_INFO);
		FP.add(UIDFolder.FetchProfileItem.UID);
		FP.add("Message-ID");
		FP.add("X-Priority");
		Message sims[]=folder.getMessages();
		folder.fetch(sims, FP);
		
		for(Message sim: sims) {
			System.out.println(sim.getSubject());
		}
		
		folder.close(true);
		store.close();
	}
	
}
