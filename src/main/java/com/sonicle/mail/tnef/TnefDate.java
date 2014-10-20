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

import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Encapsulates TNEF date information.
 *
 * @author Priyantha Jayanetti
 * @author Mirror Worlds Technologies
 * @version    Feb 20, 2000 Lifestreams 1.5
 *
 */
public class TnefDate {
    
    private Date date;

    /**
     * Creates a TNEF date class given the TNEF byte stream (14 bytes). This class
     * is based on the <code>_dtr</code> structure as defined in the tnef.h header
     * file.
     */

    public TnefDate(byte b[]) {
        if (b != null && b.length == 14) {
            parseDate(b);
        } else {
            date = Calendar.getInstance().getTime();
        }
        //System.out.println(this);
    }
        
    /**
     * @return Date object representing the TnefDate.
     */
    public Date getDate() {
        return date;
    }
    
    private void parseDate(byte b[]) {
        //System.out.println("b1=" + b[3] + " b0=" + ((int)b[2])); 
        int d1 = (((int)b[1]) << 8) + (b[0] & 0xff ); // year (+256 or &FF for sign correction)
        int d2 = (((int)b[3]) << 8) + (int)b[2];// month (1-12)
        int d3 = (((int)b[5]) << 8) + (int)b[4];//day (1-31)
        int d4 = (((int)b[7]) << 8) + (int)b[6];//hour (0-23)
        int d5 = (((int)b[9]) << 8) + (int)b[8];// min (0-59)
        int d6 = (((int)b[11]) << 8) + (int)b[10];// sec (0-59) 
        int d7 = (((int)b[13]) << 8) + (int)b[12]; // day-of-week (0=sun ... 6=sat)
        //System.out.println("" + d1 + " " + d2 + " " + d3 + " " + d4 + " " + d5 + " " + d6 + " " + d7);
        Calendar cal = Calendar.getInstance();
        cal.set(d1, d2-1, d3, d4, d5,d6);
        date = cal.getTime();
        //System.out.println("date=" + cal.getTime().toString());
    }
    
    public String toString() {
        return "tnef_date[" + date.toString() + "]";
    }
}