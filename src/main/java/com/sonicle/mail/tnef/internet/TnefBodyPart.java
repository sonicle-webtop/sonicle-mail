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

package com.sonicle.mail.tnef.internet;

import com.sonicle.mail.tnef.*;
import java.io.*;
import java.util.Enumeration;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.activation.*;


/**
 * A bridge to Tnef Mime BodyPart for the Java Mail API.
 * <P>
 * Note: This package is still at a very early stage. Requires the Java Mail API
 * and the Java Activation Framework API.
 * 
 *
 * @author Priyantha Jayanetti
 * @author Mirror Worlds Technologies
 * @version Feb 22, 2000 Lifestreams 1.5
 */

public class TnefBodyPart extends MimeBodyPart {
    private TnefAttachment tnef;
    private String sectionId;
    // processed values ..
    private String type;
    private String description;
    private String disposition;
    private String filename;
    private boolean headersLoaded = false;
    private String encoding;
    private String contentId;

    protected TnefBodyPart(TnefAttachment tnef) {
	    super();
	    this.tnef = tnef;
	    description = "TNEF body part " + tnef.getFilename();
	    try {
	        ContentType ct = new ContentType(tnef.getContentType());
	        type = ct.toString();
	    }catch(ParseException pex) {
	        type = tnef.getContentType();
	    }
	    
	    content = tnef.getContentBytes();
	    Enumeration e = tnef.attributes();
	    String name;
	    String value;
	    while(e.hasMoreElements()) {
	        name = (String)e.nextElement();
	        value = tnef.getAttribute(name);
	        if (value != null) {
	            headers.setHeader(name,value);
	        }
        }// while
        contentId = Long.toHexString(System.currentTimeMillis());
        encoding = "base64";
        filename = tnef.getFilename();
        // disposition eg = attachment; filename=genome.jpeg; modification-date="Wed, 12 Feb 1997 16:29:51 -0500";
        disposition = "attachment; filename=" + filename;        
        // what about DataHandler dh?
        //debug("created " + filename);
    }

    private void debug(String s) {
        System.out.println("TnefBodyPart>"  + s);
    }
    
    /* Override this method to make it a no-op, rather than throw
     * an IllegalWriteException.
     */
    protected void updateHeaders() {
        //debug("updateHeaders called.");
    }

    public int getSize() throws MessagingException {
	    return tnef.getContentLength();
    }

    public int getLineCount() throws MessagingException {
	    return -1;
    }

    public String getContentType() throws MessagingException {
	    return type;
    }

    public String getDisposition() throws MessagingException {
	    return disposition;
    }

    public void setDisposition(String disposition) throws MessagingException {
	    throw new IllegalWriteException("TnefBodyPart is read-only");
    }

    public String getEncoding() throws MessagingException {
	    return encoding;
    }

    public String getContentID() throws MessagingException {
	    return  contentId;
    }

    public String getContentMD5() throws MessagingException {
	    return null;
    }

    public void setContentMD5(String md5) throws MessagingException {
	    throw new IllegalWriteException("TnefBodyPart is read-only");
    }

    public String getDescription() throws MessagingException {
	    return description;
    }

    public void setDescription(String description, String charset)
			throws MessagingException {
    	throw new IllegalWriteException("TnefBodyPart is read-only");
    }

    public String getFileName() throws MessagingException {
        return filename;
    }

    public void setFileName(String filename) throws MessagingException {
    	throw new IllegalWriteException("TnefBodyPart is read-only");
    }

    protected InputStream getContentStream() throws MessagingException {
        return tnef.getInputStream();
    }
    
    public InputStream getInputStream() throws IOException, MessagingException {
	    return tnef.getInputStream();
    }
    
	    
    public synchronized DataHandler getDataHandler() 
		throws MessagingException {
    	if (dh == null) {
		 //   dh = new DataHandler(
		 //    );
        }
    	return super.getDataHandler();
    }

    public void setDataHandler(DataHandler content) throws MessagingException {
	    throw new IllegalWriteException("TnefBodyPart is read-only");
    }

    public void setContent(Object o, String type) throws MessagingException {
	    throw new IllegalWriteException("TnefBodyPart is read-only");
    }

    public void setContent(Multipart mp) throws MessagingException {
    	throw new IllegalWriteException("TnefBodyPart is read-only");
    }

    public String[] getHeader(String name) throws MessagingException {
	    return super.getHeader(name);
    }
    
    public void setHeader(String name, String value) throws MessagingException {
	    throw new IllegalWriteException("TnefBodyPart is read-only");
    }

    public void addHeader(String name, String value) throws MessagingException {
	    throw new IllegalWriteException("TnefBodyPart is read-only");
    }

    public void removeHeader(String name) throws MessagingException {
	    throw new IllegalWriteException("TnefBodyPart is read-only");
    }

    public Enumeration getAllHeaders() throws MessagingException {
	    return super.getAllHeaders();
    }

    public Enumeration getMatchingHeaders(String[] names) throws MessagingException {
	    return super.getMatchingHeaders(names);
    }

    public Enumeration getNonMatchingHeaders(String[] names) throws MessagingException {
	    return super.getNonMatchingHeaders(names);
    }

    public void addHeaderLine(String line) throws MessagingException {
	    throw new IllegalWriteException("TnefBodyPart is read-only");
    }

    public Enumeration getAllHeaderLines() throws MessagingException {
	    return super.getAllHeaderLines();
    }

    public Enumeration getMatchingHeaderLines(String[] names) throws MessagingException {
	    return super.getMatchingHeaderLines(names);
    }

    public Enumeration getNonMatchingHeaderLines(String[] names) throws MessagingException {
	    return super.getNonMatchingHeaderLines(names);
    }

}
