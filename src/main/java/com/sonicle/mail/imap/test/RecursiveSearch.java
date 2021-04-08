/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sonicle.mail.imap.test;

import com.sonicle.mail.imap.SonicleIMAPFolder;
import java.util.Properties;
import java.util.Vector;
import jakarta.mail.Session;
import jakarta.mail.Store;
import javax.swing.JOptionPane;

/**
 *
 * @author gbulfon
 */
public class RecursiveSearch {

    public static void main(String args[]) throws Exception {
		String host=JOptionPane.showInputDialog("Host");
		String user=JOptionPane.showInputDialog("User");
		String password=JOptionPane.showInputDialog("Password");
        Properties props=System.getProperties();
        props.setProperty("mail.imaps.ssl.trust", "*");
        props.setProperty("mail.imap.folder.class","com.sonicle.mail.imap.SonicleIMAPFolder");
        props.setProperty("mail.imaps.folder.class","com.sonicle.mail.imap.SonicleIMAPFolder");
        Session session=Session.getDefaultInstance(props, null);
        session.setDebug(false);
        Store store=session.getStore("imap");
        store.connect(host,user,password);
        SonicleIMAPFolder folder=(SonicleIMAPFolder)store.getFolder("");
        String skipfolders[]=new String[] {
            "Drafts","Sent","Trash","Spam"
        };
        SonicleIMAPFolder.RecursiveSearchResult rsr=folder.recursiveSearchByMessageID("<62F8143B24EE488380DD63C32054FC90@DDM001.com>",skipfolders);
        System.out.println("Result: "+rsr);
        store.close();
        System.exit(0);
    }

}
