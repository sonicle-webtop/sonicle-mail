/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sonicle.mail.imap.test;

import com.sun.mail.imap.IMAPStore;
import java.util.Properties;
import jakarta.mail.*;
import javax.swing.JOptionPane;
/**
 *
 * @author gabriele.bulfon
 */
public class TestQuota {
	
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
		Quota[] quotas = ((IMAPStore)store).getQuota("INBOX");
		System.out.println("Got "+quotas.length+" quota entries");
        //Iterate through the Quotas
        for (Quota quota : quotas) {
            System.out.println(String.format("quotaRoot:'%s'", quota.quotaRoot));
            //Iterate through the Quota Resource
            for (Quota.Resource resource : quota.resources) {
				System.out.println(String.format(
					"name:'%s', limit:'%s', usage:'%s'", resource.name,
					resource.limit, resource.usage));
            }
        }	
		store.close();
	}
	
}
