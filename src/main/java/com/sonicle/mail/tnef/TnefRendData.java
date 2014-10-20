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
 * Contains TNEF rendering information. This class is based on 
 * the <code>_renddata</code> structure as defined in the <code>tnef.h</code> header
 * file. 
 *
 * @author Priyantha Jayanetti
 * @author Mirror Worlds Technologies
 * @version    Feb 20, 2000 Lifestreams 1.5
 *
 */
public class TnefRendData {    
    
    /**
     * Tags a Attachment of type NULL (unknown?)
     */
    public static final int ATYP_NULL   = 0;
    
    /**
     * Tags a Attachment as a file.
     */    
    public static final int ATYP_FILE   = 1;
    
    /**
     * Tags a Attachment as an OLE object.
     */         
    public static final int ATYP_OLE    = 2;
    
    /**
     * Tags a Attachment as a bitmap (details unknown?)
     */    
    public static final int ATYP_PIX    = 3;
    
    /**
     * Tags a Attachment as ??(unknown?)
     */        
    public static final int ATYP_MAX    = 4;
    
    /**
     * Type of data (eg: ATYP_FILE)
     */
    public int aType; // type of attachment.
    
    /**
     * Rendering position. (details unknown. X,Y struct?)
     */
    public int ulPosition;
    
    /**
     * Rendering Width. (details unknown)
     */    
    public int dxWidth;

    /**
     * Rendering Height. (details unknown)
     */
    public int dyHeight;
    
    /**
     * Rendering Flags. (details unknown)
     * if <code>dwFlag == MAC_BINARY</code> then attachment data bytes are in 
     * Mac Binary format. (mac bin hex?) 
     */    
    public int dwFlags;
        
    /**
     * Creates TNEF RendData object from the data bytes obtained from the 
     * TNEF stream.
     */
    public TnefRendData(byte b[]) {
        if (b != null && b.length == 14) {
            parse(b);
        } else {
            aType = 0;
            ulPosition = -1;
            dxWidth = -1;
            dyHeight = -1;
            dwFlags = -1;
        }
        //System.out.println(this);
    }
        
    private void parse(byte b[]) {
        //todo: handle sign bit when converting byte to unsigned int
        aType = ((int)b[1] << 8) + (int)b[0]; 
        ulPosition = (((int)b[5] << 24) + ((int)b[4] << 16) + ((int)b[3] << 8) + (int)b[2]);
        dxWidth = ((int)b[7] << 8) + (int)b[6];
        dyHeight = ((int)b[9] << 8) + (int)b[8];
        dwFlags = ((int)b[13] << 24) + ((int)b[12] << 16) + ((int)b[11] << 8) + (int)b[10];
        //System.out.println("renddata " + atp + " " + ulp + " " + dxw + " " + dyh + " " + dwf);
    }
    
    public String toString() {
        return "tnef_rendata[atyp=" + aType + " ulPos=" + ulPosition +
                    " dxw=" + dxWidth + " dyh=" + dyHeight + " dwflags=" + dwFlags + "]";
    }
}

