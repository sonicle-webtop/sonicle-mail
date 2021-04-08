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
import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.ContentType;
import jakarta.activation.DataSource;
import java.util.*;
import java.io.*;

/**
 * The TnefMultipart class is an implementation of the abstract Multipart
 * class (in the Java MAIL api) that uses MIME conventions for the multipart data. <p>
 * <P>
 * Note: This package is still at a very early stage. Requires the Java Mail API
 * and the Java Activation Framework API.
 * <P>
 * Sample usage (hack) with Java Mail API:
 * <pre>
 *  // assume you have Java Mail API MultiPart object that has text/plain message
 *  // as well as an application/ms-tnef attachment (bodypart).
 *  // The solution (hack) described here basically removes this bodypart from the
 *  // multipart, then obtains the tnef attachments from this application/ms-tnef
 *  // bodypart and adds these attachments back into the original multipart object.
 *
 *  // (ideally, all of these should be transparent to the Java Mail API user,
 *  //  probably by associating a DataContentHandler etc.(JAF) for the tnef content-type).
 *
 *  // mp is of type Multipart that has the application/ms-tnef attachment.
 *  // loop thru and get the tnef part.
 *  int size = mp.getCount();
 *  boolean done = false;
 *  for (int i = 0; i < size; i++) {
 *      BodyPart bp = mp.getBodyPart(i);
 *      // ct is content-type of the body part.
 *      String ct = bp.getContentType();
 *      //if this is a tnef, then process it.
 *       if (ct.indexOf("application/ms-tnef") != -1) {
 *           // create tnef data source.
 *           TnefMultipartDataSource tnefDS = new TnefMultipartDataSource((MimePart)bp);
 *           // create Tnef multipart
 *           MimeMultipart tnefMP = new TnefMultipart(tnefDS);
 *           // get num of tnef attachments
 *           int partCount = tnefMP.getCount();
 *           // loop thru and add the attachment into the original multipart.
 *           for (int k = 0; k < partCount; k++) {
 *               BodyPart tnefBodyPart = tnefMP.getBodyPart(k);
 *               mp.addBodyPart(tnefBodyPart);                    
 *           }
 *           //remove the original application/ms-tnef part.
 *           mp.removeBodyPart(bp);
 *           bp = null;
 *           done = true;
 *           break;
 *       }// if
 *  }//for           
 * </pre> 
 *
 * @author Priyantha Jayanetti
 * @author Mirror Worlds Technologies
 * @version Feb 22, 2000 Lifestreams 1.5
 */

public class TnefMultipart extends MimeMultipart {


    /**
     * Default constructor. An empty TnefMultipart object
     * is created. Its contentType is set to "multipart/tnef".
     * A unique boundary string is generated and this string is
     * setup as the "boundary" parameter for the 
     * <code>contentType</code> field. <p>
     *
     * MimeBodyParts may be added later.
     */
    public TnefMultipart() {
	    super("tnef");
	    parsed = true;
    }


    /**
     * Constructs a TnefMultipart object and its bodyparts from the 
     * given DataSource. <p>
     *
     *
     * @param	ds	TnefMultipartDataSource.
     */
    public TnefMultipart(TnefMultipartDataSource ds) throws MessagingException {
    	super("tnef");
    	parsed = true;
	    if (ds instanceof MessageAware) {
	        MessageContext mc = ((MessageAware)ds).getMessageContext();
	        setParent(mc.getPart());
	    }
	    
        setMultipartDataSource((MultipartDataSource)ds);
	    ContentType cType = new ContentType("multipart", "tnef", null);
	    cType.setParameter("boundary", getUniqueValue());
	    contentType = cType.toString();        
    }
    
    private String getUniqueValue() {
	    StringBuffer s = new StringBuffer();
	    int rand = (int)(Math.pow(2,32) * Math.random());
	    // Unique string is <hashcode>.<currentTime>.JavaMail.<rand_int_hex>
    	s.append(s.hashCode()).append('.').
	    append(System.currentTimeMillis()).append('.').
	    append("TnefMultipart.").
	    append(Integer.toHexString(rand));
	    return s.toString();
    }
        

}
