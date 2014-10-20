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
 * MS-TNEF Message Constants based on the <code>tnef.h</code> from the Microsoft
 * MAPI SDK. See http://www.microsoft.com for more information.
 *
 * @author Priyantha Jayanetti
 * @author Mirror Worlds Technologies
 * @version    Feb 19, 2000 Lifestreams 1.5
 *
 */
 

public class TnefConstants {    
    
    /**
     * Creates the TNEF Stream Version.
     */
    public static int generateTnefVersion(int mj,int mn) {
        return ((0x0000FFFF & mj) << 16) | (0x0000FFFF & mn);    
    }    
       
    /**
     * Returns the 16 bit TNEF message attribute id given the 32 bit complete id.
     */
    public static int getAttributeID(int att) {
        return (att & 0x0000FFFF);
    }

    /**
     * Returns the 16 bit TNEF message attribute type given the 32 bit complete id.
     * The type are short, word, date, string etc.
     */
    public static int getAttributeType(int att) {
        return ((att >> 16) & 0x0000FFFF);
    }

    public static int create32BitAttribute(int atp, int id) {
        return (((atp) << 16) | id);
    }    

    // --------------------------
    // These properties are based on the tnef.h header file (found on MSDN).
    // --------------------------
        
    //AddProps, ExtractProps
    //ulong
    public static final int     TNEF_PROP_INCLUDE           = 0x00000001;
    public static final int     TNEF_PROP_EXCLUDE           = 0x00000002;
    public static final int     TNEF_PROP_CONTAINED         = 0x00000004;
    public static final int     TNEF_PROP_MESSAGE_ONLY      = 0x00000008;
    public static final int     TNEF_PROP_ATTACHMENTS_ONLY  = 0x00000010;
    public static final int     TNEF_PROP_CONTAINED_TNEF    = 0x00000040;

    //FinishComponent
    //ulong
    public static final int     TNEF_COMPONENT_MESSAGE      = 0x00001000;
    public static final int     TNEF_COMPONENT_ATTACHMENT   = 0x00002000;
    public static final int     TNEF_SIGNATURE              = 0x223E9F78;
    public static final int     TNEF_VERSION                = generateTnefVersion(1,0);     
    public static final int     MAC_BINARY                  = 0x00000001;

    //byte
    public static final int     fmsNull                     = 0x00;
    public static final int     fmsModified                 = 0x01;
    public static final int     fmsLocal                    = 0x02;
    public static final int     fmsSubmitted                = 0x04;
    public static final int     fmsRead                     = 0x20;
    public static final int     fmsHasAttach                = 0x80;


    //TNEF MESSAGE ATTRIBUTE TYPES
    //word|short
    public static final int     atpTriples                  = 0x0000;
    public static final int     atpString                   = 0x0001;
    public static final int     atpText                     = 0x0002;
    public static final int     atpDate                     = 0x0003;
    public static final int     atpShort                    = 0x0004;
    public static final int     atpLong                     = 0x0005;
    public static final int     atpByte                     = 0x0006;
    public static final int     atpWord                     = 0x0007;
    public static final int     atpDword                    = 0x0008;
    public static final int     atpMax                      = 0x0009;

    //These two constants identify the message or attribute sequence.
    //byte
    public static final int     LVL_MESSAGE                 = 0x01;
    public static final int     LVL_ATTACHMENT              = 0x02;

    // MAPI message priorities
    //int
    public static final int     prioLow                     = 3;
    public static final int     prioNorm                    = 2;
    public static final int     prioHigh                    = 1;

    //TNEF MESSAGE ATTRIBUTES
    //dword|int
    public static final int    attNull                      = create32BitAttribute(0,0x0000);
    public static final int    attFrom                      = create32BitAttribute(atpTriples, 0x8000); // PR_ORIGINATOR_RETURN_ADDRESS
    public static final int    attSubject                   = create32BitAttribute(atpString,  0x8004); // PR_SUBJECT 
    public static final int    attDateSent                  = create32BitAttribute(atpDate,    0x8005); // PR_CLIENT_SUBMIT_TIME
    public static final int    attDateRecd                  = create32BitAttribute(atpDate,    0x8006); // PR_MESSAGE_DELIVERY_TIME
    public static final int    attMessageStatus             = create32BitAttribute(atpByte,    0x8007); // PR_MESSAGE_FLAGS
    public static final int    attMessageClass              = create32BitAttribute(atpWord,    0x8008); // PR_MESSAGE_CLASS
    public static final int    attMessageID                 = create32BitAttribute(atpString,  0x8009); // PR_MESSAGE_ID
    public static final int    attParentID                  = create32BitAttribute(atpString,  0x800A); // PR_PARENT_ID
    public static final int    attConversationID            = create32BitAttribute(atpString,  0x800B); // PR_CONVERSATION_ID
    public static final int    attBody                      = create32BitAttribute(atpText,    0x800C); // PR_BODY 
    public static final int    attPriority                  = create32BitAttribute(atpShort,   0x800D); // PR_IMPORTANCE
    public static final int    attAttachData                = create32BitAttribute(atpByte,    0x800F); // PR_ATTACH_DATA_xxx
    public static final int    attAttachTitle               = create32BitAttribute(atpString,  0x8010); // PR_ATTACH_FILENAME
    public static final int    attAttachMetaFile            = create32BitAttribute(atpByte,    0x8011); // PR_ATTACH_RENDERING
    public static final int    attAttachCreateDate          = create32BitAttribute(atpDate,    0x8012); // PR_CREATION_TIME
    public static final int    attAttachModifyDate          = create32BitAttribute(atpDate,    0x8013); // PR_LAST_MODIFICATION_TIME
    public static final int    attDateModified              = create32BitAttribute(atpDate,    0x8020); // PR_LAST_MODIFICATION_TIME
    public static final int    attAttachTransportFilename   = create32BitAttribute(atpByte,    0x9001); // PR_ATTACH_TRANSPORT_NAME
    public static final int    attAttachRenddata            = create32BitAttribute(atpByte,    0x9002);
    public static final int    attMAPIProps                 = create32BitAttribute(atpByte,    0x9003);
    public static final int    attRecipTable                = create32BitAttribute(atpByte,    0x9004); // PR_MESSAGE_RECIPIENTS
    public static final int    attAttachment                = create32BitAttribute(atpByte,    0x9005);
    public static final int    attTnefVersion               = create32BitAttribute(atpDword,   0x9006);
    public static final int    attOemCodepage               = create32BitAttribute(atpByte,    0x9007);
    public static final int    attOriginalMessageClass      = create32BitAttribute(atpWord,    0x0006); // PR_ORIG_MESSAGE_CLASS 
    public static final int    attOwner                     = create32BitAttribute(atpByte,    0x0000); // PR_RCVD_REPRESENTING_xxx  or
                                                                                           // PR_SENT_REPRESENTING_xxx 
                                                                                            
    public static final int    attSentFor                   = create32BitAttribute(atpByte,    0x0001); // PR_SENT_REPRESENTING_xxx
    public static final int    attDateStart                 = create32BitAttribute(atpDate,    0x0006); // PR_DATE_START
    public static final int    attDateEnd                   = create32BitAttribute(atpDate,    0x0007); // PR_DATE_END
    public static final int    attAidOwner                  = create32BitAttribute(atpLong,    0x0008); // PR_OWNER_APPT_ID
    public static final int    attRequestRes                = create32BitAttribute(atpShort,   0x0009); // PR_RESPONSE_REQUESTED    
}