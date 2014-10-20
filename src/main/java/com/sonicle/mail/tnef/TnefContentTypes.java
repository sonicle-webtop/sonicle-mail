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

/**
 * Mime Content-Type map interface. This interface defines methods to return
 * a MIME content-type given a file extension.
 *
 * @author Priyantha Jayanetti
 * @author Mirror Worlds Technologies
 * @version    Feb 20, 2000 Lifestreams 1.5
 *
 */
 
 public interface TnefContentTypes {
    
    /**
     * Returns the content type given a file extension. Eg: .txt or .gif
     * The default value is application/octet-stream
     *
     * @param ext file extentsion.
     */
    public String getContentTypeFromExtension(String ext);

    /**
     * Returns the content type given a file name. Eg: hello.txt or image.gif
     * The default value is application/octet-stream
     *
     * @param name file name with extentsion.
     */
    public String getContentTypeFromFilename(String filename);
    
 }    