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
 * A TnefStreamParser parses a TNEF input stream (eg: winmail.dat) and invokes
 * a TnefBuilder methods to build a TNEF message object. 
 * For example:
 * <pre>
 *       TnefStreamParser parser = new TnefStreamParserImpl();
 *       TnefBuilder      builder = new TnefMessageBuilder();
 *       TnefContentTypes contentTypes = new DefaultContentTypeImpl();
 *       TnefMessage      message = null;
 *       try {
 *          builder.setContentTypes(contentTypes);
 *          parser.setBuilder(builder);
 *          parser.parse(tnefStream);        
 *          message = builder.getMessage();
 *      } catch (IOException e) {
 *      }
 * </pre> 
 *
 * @author Priyantha Jayanetti
 * @author Mirror Worlds Technologies
 * @version    Feb 20, 2000 Lifestreams 1.5
 *
 */
 
 public interface TnefStreamParser {
    
    /**
     * Associates a builder. The builder will be responsible for composing the final
     * TNEF message object.
     *
     * @param builder TnefBuilder implementation.
     */
    public void setBuilder(TnefBuilder builder);
    
    /**
     * Starts the parsing process. The Parser expects that the TNefBuilder has already
     * been set.
     *
     * @param inputstream TNEF message stream. eg: <code>new FileInputStream("winmail.dat")</code>
     * @exception IOException due to parse errors or builder errors.
     */
    public void parse(InputStream inputstream) throws IOException;
 }