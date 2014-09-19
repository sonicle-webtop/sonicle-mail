/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sonicle.mail.imap;

import java.util.Properties;
import java.util.Vector;
import javax.mail.Session;
import javax.mail.Store;

/**
 *
 * @author gbulfon
 */
public class Test {

    public static void main(String args[]) throws Exception {
        String host=args[0];
        String username=args[1];
        String password=args[2];
        Properties props=System.getProperties();
        props.setProperty("mail.imaps.ssl.trust", "*");
        props.setProperty("mail.imap.folder.class","com.sonicle.mail.imap.SonicleIMAPFolder");
        props.setProperty("mail.imaps.folder.class","com.sonicle.mail.imap.SonicleIMAPFolder");
        Session session=Session.getDefaultInstance(props, null);
        session.setDebug(false);
        Store store=session.getStore("imap");
        store.connect(host,username,password);
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
