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

import java.io.*;

/**
 * Interface to build a Tnef Message from a Tnef Stream. The builder methods are invoked
 * by a TnefStreamParser.
 *
 * @author Priyantha Jayanetti
 * @author Mirror Worlds Technologies
 * @version    Feb 18, 2000 Lifestreams 1.5
 *
 */
 
 public interface TnefBuilder {
    
    /**
     * Sets the TnefContentTypes manager. The TnefContentTypes maps a filename
     * or file extension to its corresponding (MIME) content-type.
     *
     * @param contentTypes content-type manager.
     */
    public void setContentTypes(TnefContentTypes contentTypes);

    /**
     * Sets the TNEF stream signature. The expected signature is 223E9F78. (hex).
     *
     * @param signature TNEF stream signature.
     * @exception IO Exception if the signature is incorrect.
     */
    public void setSignature(int signature) throws IOException;
    
    /**
     * Sets the message key set defined in the TNEF stream. The key is a 16 bit
     * unsigned integer.
     *
     * @param key message key.
     */
    public void setKey(int key);
    
    /**
     * Sets the TNEF version defined in the message stream.
     *
     * @param version TNEF message stream version.
     */
    public void setTnefVersion(int version);
    
    
    /**
     * Sets Message Sequence attributes. See <code>TnefConstants</code> class
     * for list of possible attributes. The type of attribute (eg: String) can
     * be identified via <code>TnefConstants.getAttType(id)</code> method.
     *
     * @param id TNEF Message Attribute mapped to MAPI.
     * @param data data for the defined attributes.
     */
    public void setMessageAttribute(int id, byte data[]);

    /**
     * Sets Attachment Sequence attributes. See <code>TnefConstants</code> class
     * for list of possible attributes. The type of attribute (eg: String) can
     * be identified via <code>TnefConstants.getAttType(id)</code> method.
     *
     * @param id TNEF Attachment Attribute mapped to MAPI.
     * @param data data for the defined attributes.
     */    
    public void setAttachmentAttribute(int id, byte data[]);        
    
    /**
     * This method is called by the parser when it encounters a TNEF attachment
     * (start of attachment) sequence.
     */
    public void addAttachment();
    
    /**
     * Returns the message build by this builder.
     */
    public TnefMessage getMessage();
 }