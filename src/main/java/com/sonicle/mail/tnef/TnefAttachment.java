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
 * Tnef Message Attachment
 *
 * @author Priyantha Jayanetti
 * @author Mirror Worlds Technologies
 * @version    Feb 20, 2000 Lifestreams 1.5
 *
 */
 
 public class TnefAttachment {
        
    private TnefMessage     parent;
    private Hashtable       attributes;
    private Date            date;
    private byte            data[];
    private TnefRendData    rendData;
    
    /**
     * Constructs a TnefAttachment. The content-type is initialized to application/octet-stream,
     * the date is set to current time and the filename contains a random string with a
     * '.bin' extension. These parameters are generally reset to the attachment's actual
     * values by the Message Builder.
     *
     * @param parent message parent.
     */
    public TnefAttachment(TnefMessage parent) {
        this.parent = parent;
        attributes = new Hashtable();
        setAttribute("filename",Long.toHexString(System.currentTimeMillis()) + ".bin"); 
        setAttribute("content-type","application/octet-stream"); 
        date = new Date();
        setAttribute("date",date.toString());         
    }
    
    /**
     * Returns the <code>TnefMessage</code> parent.
     */
    public TnefMessage getParent() {
        return parent;
    }        
    
    /**
     * @return MIME content-type of this attachment.
     */    
    public String getContentType() {
        return (String)attributes.get("content-type");
    }
    
    /**
     * @return filename of this attachment.
     */
    public String getFilename() {
        return (String)attributes.get("filename");
    }
    
    /**
     * @return content-length in bytes if known. Same as attrubute 'content-length'.
     */
    public int getContentLength() {
        int len = 0;
        if (data != null) {
            len = data.length;
        }
        setAttribute("content-length","" + len); 
        return len;
    }
    
    /**
     * @return attachment contents as a byte array.
     */
    public byte[] getContentBytes() {
        return data;
    }
    
    /**
     * @return the attribute. eg: content-type
     */    
    public String getAttribute(String name) {
        if (name != null) {
            return (String)attributes.get(name.trim().toLowerCase());
        }
        return null;
    }
    
    /**
     * @return Enumeration of all attribute names.
     */    
    public Enumeration attributes() {
        return attributes.keys();
    }
    
    /**
     * @return RendData (rendering information) for this attachment.
     */
    public TnefRendData getRendData() {
        return rendData;
    }
    
    /**
     * Sets (by the  builder) the associated RendData object.
     * @param rendData rend data associated with this attachment.
     */
    public void setRendData(TnefRendData rendData) {
        this.rendData = rendData;
        //todo: check if rendData.dwFlags == TnefConstants.MAC_BINARY 
        //(encoded in mac binary format)
    }
    
    /**
     * Sets the attachment attributes such as content-type etc.
     * @param name attribute name
     * @param value value of attribute.
     */    
    public void setAttribute(String name, String value) {
        attributes.put(name,value);
    }
    
    /**
     * Sets the date associate with this attachment. Same as 'date-modified'
     * and 'date' attributes.
     */
    public void setDate(Date d) {
        if (d != null) {
            this.date = d;
            setAttribute("date-modified", d.toString());
            setAttribute("date", d.toString());
        }
    }
    
    /**
     * Returns the date (modified) date of this attachment.
     */
    public Date getDate() {
        return date;
    }
    
    /**
     * Sets (by the builder) the attachment data.
     */
    public void setData(byte data[]) {
        this.data = data;
    }
    
    /**
     * @return input stream to the attachment content.
     */
    public InputStream getInputStream() {
        if (data != null) {
            return new ByteArrayInputStream(data);
        }
        return null;
    }
    
    /**
     * Writes the attachment content (binary) to an output stream (useful for
     * saving attachments.
     */
    public void writeTo(OutputStream out) throws IOException {
        // write contents to file. (binary)
        OutputStream o;
        if (!(out instanceof BufferedOutputStream)) {
            o = new BufferedOutputStream(out, 1024);
        } else {
            o = out;
        }
        o.write(data);
        o.flush();        
    }
    
    /**
     * Prints debug information.
     */
    public void printInfo() {
        System.out.println("--");
        Enumeration e = attributes.keys();
        while(e.hasMoreElements()) {
            String n = (String)e.nextElement();
            String v = (String)attributes.get(n);
            System.out.println("   ATTACH " + n + "=" + v);
        }        
    }
    
 }
 