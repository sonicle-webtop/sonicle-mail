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
import java.io.InputStream;
import java.io.IOException;
import java.util.Vector;

import jakarta.mail.*;
import jakarta.mail.internet.*;


/**
 * This DataSource encapsulates a TNEF winmail data stream.
 * <P>
 * Note: This package is still at a very early stage. Requires the Java Mail API
 * and the Java Activation Framework API.
 *
 *
 * @author Priyantha Jayanetti
 * @author Mirror Worlds Technologies
 * @version    Feb 22, 2000 Lifestreams 1.5
 */

public class TnefMultipartDataSource extends MimePartDataSource
				     implements MultipartDataSource {

    private Vector parts;

    public TnefMultipartDataSource(MimePart part)throws MessagingException {
    	super(part);
    	String ct =  getContentType().toLowerCase();
    	if (ct == null || ct.indexOf("application/ms-tnef") == -1) {
    	    throw new MessagingException("TnefMultipartDataSource expects a MimePart of application/ms-tnef");
    	}
	    parts = new Vector();
	    try {
	        InputStream in = getInputStream();
	        TnefMessage tnefMsg = JTnef.createTnefMessage(in);
	        int count = tnefMsg.getCount();	            
	        for(int j = 0; j < count; j++) {
	            TnefAttachment attach = (TnefAttachment)tnefMsg.getAttachmentAt(j);
	            TnefBodyPart tbp = new TnefBodyPart(attach);
	            parts.addElement(tbp);
	        }
	        in.close();
	    }catch(Exception ex2) {
	        //ex2.printStackTrace();
	        throw new MessagingException("TnefMultipartDataSource - " + ex2.getMessage());
	    }
    }
        
    public String getName() {
        return "ms-tnef winmail stream";
    }
    
    public int getCount() {
    	return parts.size();
    }

    public BodyPart getBodyPart(int index) throws MessagingException {
	    return (BodyPart)parts.elementAt(index);
    }    
    
}
