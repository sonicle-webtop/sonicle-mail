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
 * A simple implementation of the <code>TnefBuilder</code> interface.
 * This implementation builds a <code>TnefMessage</code> based on the
 * attribute data bytes passed by the parser. This class ignores some
 * attributes such as <code>attConversationID, attMAPIProps</code> etc., however
 * it tries to build the attachments (eg: files ) sent in a TNEF stream.
 * See the description on <code>setMessageAttribute()</code> and
 * <code>setAttachmentAttribute()</code> methods to see the actual
 * attributes used to build a message. The methods in this class are usually
 * invoked by the parser.
 *
 * <P>
 * For more information (good luck), search for
 * "Mapping of TNEF Attributes to MAPI Properties",
 * "Mapping of Internet Mail Attributes to MAPI Properties", "TNEF Stream Syntax" and
 * "TNEF Stream Structure" at http://www.microsoft.com
 *
 * @author Priyantha Jayanetti
 * @author Mirror Worlds Technologies
 * @version    Feb 20, 2000 Lifestreams 1.5
 *
 */

 public class TnefMessageBuilder  implements  TnefBuilder {

    /**
     * Message object being built.
     */
    protected TnefMessage message;

    /**
     * The current tnef attachment.
     */
    protected TnefAttachment attachment;

    /**
     * MIME content-type manager that translates file extensions to a content-type.
     */
    protected TnefContentTypes contentTypes;

    /**
     * Constructs a basic builder and an empty TnefMessage object.
     */
    public TnefMessageBuilder() {
        message = new TnefMessage();
    }

    /**
     * Sets the content-type handler. This handler is used to determine the
     * MIME content-type of the attachments (based on the filename extension).
     *
     */
    public void setContentTypes(TnefContentTypes contentTypes) {
        this.contentTypes = contentTypes;
        //check for null ptr when this obj. is used.
    }

    /**
     * Sets the TNEF stream signature (32 bits).
     */
    public void setSignature(int signature) throws IOException {
        if (signature != TnefConstants.TNEF_SIGNATURE) {
            throw new IOException("Invalid TNEF signature.");
        }
        message.setSignature(signature);
    }

    /**
     * Sets the TNEF stream message key (16 bits).
     */
    public void setKey(int key) {
        message.setKey(key);
    }

    /**
     * Sets the TNEF stream version (32 bits).
     */
    public void setTnefVersion(int version) {
        message.setTnefVersion(version);
    }

    /**
     * Sets the TNEF stream message attributes. Note that all of the attributes
     * described in the <code>TnefConstants</code> are not guaranteed to be in the
     * TNEF stream. This implemention sets the following message properties
     * in the <code>TnefMessage</code> object. (other properties are ignored).
     * <pre>
     * MAPI attDateRecd      = TnefMessage attribute : date-received
     * MAPI attDateSent      = TnefMessage attribute : date-sent  (same as msg.getDate())
     * MAPI attDateModified  = TnefMessage attribute : date-modified
     * MAPI attSubject       = TnefMesasge attribute : subject
     * MAPI attMessageID     = TnefMessage attribute : message-id
     * MAPI attPriority      = TnefMessage attribute : message-priority (same asmsg.getPriority())
     * MAPI attFrom          = TBD/ignored.
     * MAPI attConversationID= TBD/ignored.
     * MAPI attBody          = TBD/ignored.
     * MAPI attMAPIProps     = TBD/ignored.
     * MAPI attMessageClass  = TnefMessage attribute : message-class
     * </pre>
     * The attributes can be accessed from the TnefMessage's <code>getAttribute(name)</code>
     * method.(for example <code>getAttribute("date-received")</code>.
     * <P>
     * Note that there is no guarantee that the above properties are in the TNEF message
     * stream.
     *
     * @param id message attribute id
     * @param data message attribute data bytes
     */
    public void setMessageAttribute(int id, byte data[]) {
        parseMessageAttribute(id, data);
    }

    /**
     * Sets the TNEF stream attachment attributes. Note that all of the attributes
     * described in the <code>TnefConstants</code> are not guaranteed to be in the
     * TNEF stream. This implemention sets the following attachment properties
     * in the <code>TnefAttachment</code> object. (other properties are ignored).
     * <pre>
     * MAPI attAttachment        = TBD/ignored.
     * MAPI attAttachData        = TnefAttachment : getInputStream() or getContentBytes()
     * MAPI attAttachMetaFile    = TBD/ignored.
     * MAPI attAttachTitle       = TnefAttachment attribute : title (same as getFilename())
     * MAPI attAttachCreateDate  = TnefAttachment attribute : date-created (same as getDate())
     * MAPI attAttachRenddata    = TnefAttachment : getRendData()
     * </pre>
     * The attributes can be accessed from the TnefAttachment's <code>getAttribute(name)</code>
     * method.(for example <code>getAttribute("filename")</code>. Generally <code>attAttachTitle</code>
     * attribute contains the filename. This implementation of the builder determines the content-type
     * of the attachment based on this filename attribute. (assuming that the TnefContentType is set
     * via <code>setContentTypes()</code> method).
     * <P>
     * Note that there is no guarantee that the above properties are in the TNEF message
     * stream.
     *
     * @param id attachment attribute id
     * @param data attachment attribute data bytes
     */
    public void setAttachmentAttribute(int id, byte data[]) {
        parseAttachmentAttribute(id, data);
    }


    public void addAttachment() {
        //debug("  -- new attachment --");
        attachment = new TnefAttachment(message);
        message.addAttachment(attachment);
    }

    public TnefMessage getMessage() {
        return message;
    }

    /*
    * Parsed Properties:
    * MAPI attDateRecd      = TnefMessage attribute : date-received
    * MAPI attDateSent      = TnefMessage attribute : date-sent  (same as msg.getDate())
    * MAPI attDateModified  = TnefMessage attribute : date-modified
    * MAPI attSubject       = TnefMesasge attribute : subject
    * MAPI attMessageID     = TnefMessage attribute : message-id
    * MAPI attMessageClass  = TnefMessage attribute : message-class
    * MAPI attMAPIProps     = TBD/ignored.
    * MAPI attPriority      = TnefMessage attribute : message-priority (same asmsg.getPriority())
    */
    private void parseMessageAttribute(int id, byte buf[]) { // throw something?
        TnefDate tdate;
        if (id == TnefConstants.attDateRecd) {
            //debug(s + " attDateRecd");
            tdate = new TnefDate(buf);
            message.setAttribute("date-received", tdate.getDate().toString());
        } else if (id == TnefConstants.attDateSent) {
            //debug(s + " attDateSent");
            tdate = new TnefDate(buf);
            //message.setAttribute("date-sent", tdate.getDate().toString());
            message.setDate(tdate.getDate());
        } else if (id == TnefConstants.attFrom) {
            //debug(" Msg_Attrib attFrom");
        } else if (id == TnefConstants.attSubject) {
            //debug(s + " attSubject");
            String sub = getString(buf);
            message.setAttribute("subject", sub);
        } else if (id == TnefConstants.attConversationID) {
            //debug(s + " attConversationID");
            //String cid = getString(buf);
        } else if (id == TnefConstants.attMessageID) {
            //debug(s + " attMessageID");
            String mid = getString(buf);
            message.setAttribute("message-id", mid);
        } else if (id == TnefConstants.attMessageStatus) {
            //debug(" Msg_Attrib attMessageStaus");
        } else if (id == TnefConstants.attBody) {
            //debug(" Msg_Attrib attBody");
        } else if (id == TnefConstants.attDateModified) {
            //debug(" Msg_Attrib attDateModified");
            tdate = new TnefDate(buf);
            message.setAttribute("date-modified", tdate.getDate().toString());
        } else if (id == TnefConstants.attPriority) {
            //debug(s + " attPriority");
            int prio = getShort(buf);
            message.setPriority(prio);
            message.setAttribute("message-priority", "" + prio);
        } else if (id == TnefConstants.attMessageClass) {
            //debug(s + " attMessageClass");
            String mc = getString(buf);
            message.setAttribute("message-class", mc);
        } else if (id == TnefConstants.attMAPIProps) {
            //debug(" attMAPIProps");
            //String mc = getString(buf);
            //message.setAttribute("message-class", mc);
        } else {
            //debug(" Msg_Attrib id=" + hex(TnefConstants.getAttID(id)) + " len=" + buf.length);
        }
    }

    /*
     * Parsed Properties:
     * MAPI attAttachment        = TBD/ignored.
     * MAPI attAttachData        = TnefAttachment : getInputStream() or getContentBytes()
     * MAPI attAttachMetaFile    = TBD/ignored.
     * MAPI attAttachTitle       = TnefAttachment attribute : title (same as getFilename())
     * MAPI attAttachCreateDate  = TnefAttachment attribute : date-created (same as getDate())
     * MAPI attAttachRenddata    = TnefAttachment : getRendData()
    */
    private void parseAttachmentAttribute(int id, byte buf[]) { // throw something?
        TnefDate tdate;
        if (id == TnefConstants.attAttachment) {
            //debug(" Attach_Attrib attAttachment");
        } else if (id == TnefConstants.attAttachData) {
            //debug(" Attach_Attrib attAttachData");
            attachment.setData(buf);
            attachment.setAttribute("content-length", "" + buf.length);
        } else if (id == TnefConstants.attAttachMetaFile) {
            //debug(" Attach_Attrib attAttachMetaFile");
        } else if (id == TnefConstants.attAttachTitle) {
            //debug(" Attach_Attrib attAttachTitle");
            String title = getString(buf);
            attachment.setAttribute("filename", title);
            if (contentTypes != null) {
                attachment.setAttribute("content-type", contentTypes.getContentTypeFromFilename(title));
            }
            //set content type here.
        } else if (id == TnefConstants.attAttachCreateDate) {
            //debug(" Attach_Attrib attAttachCreateDate");
            tdate = new TnefDate(buf);
            attachment.setAttribute("date-created", tdate.getDate().toString());
        } else if (id == TnefConstants.attAttachModifyDate) {
            //debug(" Attach_Attrib  attAttachModifyDate");
            tdate = new TnefDate(buf);
            //attachment.setAttribute("date-modified", tdate.getDate().toString());
            //attachment.setAttribute("date", tdate.getDate().toString());
            attachment.setDate(tdate.getDate());
        } else if (id == TnefConstants.attAttachRenddata) {
            //debug(" Attach_Attrib attAttachRenddata");
            TnefRendData rdata = new TnefRendData(buf);
            attachment.setRendData(rdata);
        } else {
            //debug(" Attach_Attrib  id=" + hex(TnefConstants.getAttID(id)) + " len=" + buf.length);
        }
    }

    /**
     * @return a String given an array of bytes.
     */
    protected String getString(byte d[]) {
        //encoding?
        return new String(d);
    }

    /**
     * @return a short (16 bit int) given an array of (2) bytes.
     */
    protected int getShort(byte d[]) {
        return ((d[1] << 8) + d[0]);
    }

    /**
     * @return a int (32 bit) given an array of (4) bytes.
     */
    protected int getInt(byte d[]) {
        return ((d[3] << 24) +(d[2] << 16) +(d[1] << 8) + d[0]);
    }

    private String hex(int i) {
        if (i > 9) {
            return (Integer.toHexString(i)).toUpperCase();
        } else {
            return "0" + (Integer.toHexString(i)).toUpperCase();
        }
    }

    public void debug(String s) {
        System.out.println("TnefMessageBuilder>" + s);
    }

}
