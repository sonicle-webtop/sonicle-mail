/*
 * Copyright (c) 1997 - 2000 Mirror Worlds Technologies, Inc. ("MWT") All Rights Reserved.
 *
 * This software is the confidential and proprietary information of Mirror
 * Worlds Technologies, Inc. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with MWT.
 *
 * MWT MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
 * SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. MWT SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * CopyrightVersion 1.5_Lifestreams_Office
 */

package com.sonicle.mail.tnef;

import java.util.*;
import java.io.*;

/**
 * This class contains a Tnef Message decoded from a TNEF byte stream.
 * Note that the TnefMessage class is not related to the Java Mail API's
 * <code>Message</code> class. A TnefMessage class can contain one or 
 * more attachments.
 * <P>
 * Example:
 * <pre>
 *       try {
 *          String filename = "winmail.dat";
 *          //create message object using default parser and builder.
 *          TnefMessage msg = JTnef.createTnefMessage(filename);          
 *          msg.printInfo(); // print debug info
 *          int count = msg.getCount();   // get number of attachments.   
 *           for (int i = 0; i < count; i++) { // loop thru attachments and print
 *              TnefAttachment a = msg.getAttachmentAt(i);
 *               System.out.println("   -- ");
 *               a.printInfo();
 *               // content-type of attachment is a.getContentType();
 *               // filename of attachment is a.getFilename();
 *               // attachment stream is a.getInputStream();
 *               // you can save attachment:
 *               //      File f = new File(a.getFilename);
 *               //      FileOutputStream fos = new FileOutputStream(f);      
 *               //      a.writeTo(f); // save attachment.
 *           }
 *       }catch(Exception e) {
 *           e.printStackTrace
 *       } 
 * </pre>
 *
 *
 * @author Priyantha Jayanetti
 * @author Mirror Worlds Technologies
 * @version    Feb 20, 2000 Lifestreams 1.5
 *
 */
 
 public class TnefMessage {
    
    private int         signature;
    private int         key;
    private int         version;
    private int         priority;
    private Vector      attachments;
    private Hashtable   attributes;
    private Date        date;
    private String      bodyText;
    
    //message seq
    //message key
    //rend data
    //message attribs subject,date(sent,received,modified), priority, [from, to, cc, bcc]
    //vect. attachments
    
    /** 
     * Creates an empty TNEF message. The content type is defaulted to "message/tnef"
     * (fictional content-type) and the date is set to the current time.
     */        
    public TnefMessage() {
        attachments = new Vector();
        attributes = new Hashtable();
        setAttribute("content-type","message/tnef");         
        setAttribute("from",""); 
        setAttribute("to",""); 
        setAttribute("cc",""); 
        setAttribute("subject",""); 
        date = new Date();
        setAttribute("date",date.toString());                 
        bodyText = new String("");
    }
    
    /**
     * @return the number of attachment in this message.
     */
    public int getCount() {
        return attachments.size();
    }
    
    /**
     * @return the MIME conent-type of this message.
     */
    public String getContentType() {
        return (String)attributes.get("content-type");
    }
    
    /**
     * @return the message attachment at the given index.
     */    
    public TnefAttachment getAttachmentAt(int index) throws ArrayIndexOutOfBoundsException {
        int count = getCount();
        if (index < 0 || index >= count) {
            throw new ArrayIndexOutOfBoundsException("attachment index is out of bounds.");
        }
        return (TnefAttachment) attachments.elementAt(index);
    }

    /**
     * @return the attribute. eg: subject
     */
    public String getAttribute(String name) {
        return (String)attributes.get(name);
    }    
    
    /**
     * Sets the message attributes such as subject, date-received etc.
     * @param name attribute name
     * @param value value of attribute.
     */
    public void setAttribute(String name, String value) {
        attributes.put(name,value);
    }    

    /**
     * Sets the message body text. (ignored in this implementation).
     * @param text message body
     */
    public void setText(String text) {
        bodyText = text;
    }
    
    /**
     * @return message body text. (ignored in this implementation).
     */
    public String getText() {
        return bodyText;
    }

    /**
     * Sets the TNEF stream 32 bit signature.
     */
    public void setSignature(int signature) {
        this.signature = signature;
    }
    
    /**
     * @return TNEF stream 32 bit signature.
     */
    public int getSignature() {
        return signature;
    }
    
    /**
     * Sets the TNEF message 16 bit key
     */          
    public void setKey(int key) {
        this.key = key;
    }
    
    /**
     * @return TNEF Message 16 bit key.
     */
    public int getKey() {
        return key;
    }

    /**
     * Sets the TNEF stream (32 bit) version.
     */    
    public void setTnefVersion(int version) {
        this.version = version;
    }
    
    /**
     * @return TNEF stream version.
     */
    public int getTnefVersion() {  
        return version;
    }
    
    /**
     * Sets the message priority. (1-3).
     */
    public void setPriority(int prio) {
        this.priority = prio;
    }    

    /**
     * @return the message priority. (1-3).
     */
    public int getPriority() {
        return priority;
    }    
    
    /**
     * Adds an attachment to this message.
     */
    public void addAttachment(TnefAttachment attachment) {
        if (!attachments.contains(attachment)) {
            attachments.addElement(attachment);
        }
    }
    
    /**
     * Sets the date. (same as 'date-sent').
     */
    public void setDate(Date d) {
        if (d != null) {
            this.date = d;
            setAttribute("date-sent", d.toString());
        }
    }
    
    /**
     * Writes this message (and its attachments) as a MIME multipart/mixed
     * message. (to convert tnef -> mime).
     * <P>
     * <B>method not implemented. TBD.
     */
    public void writeToMIME(OutputStream out) throws IOException {
        // write contents to file. (mime format?)
    }
    
    /**
     * Prints debugging info.
     */
    public void printInfo() {
        Enumeration e = attributes.keys();
        System.out.println("MSG TNEF Message"); 
        System.out.println("MSG number of attachments=" + attachments.size());
        while(e.hasMoreElements()) {
            String n = (String)e.nextElement();
            String v = (String)attributes.get(n);
            System.out.println("MSG " + n + "=" + v);
        }
        /*
        int size = attachments.size();
        for(int i = 0; i < size; i++) {
            System.out.println("MSG ATTACH#" + i);
            TnefAttachment a = (TnefAttachment)attachments.elementAt(i);
            a.printInfo();
        }//for            
        */
    }
    
    
 }