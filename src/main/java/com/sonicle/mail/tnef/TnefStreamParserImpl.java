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
 * A simple implementation of the TnefStreamParser interface. This class parses
 * the TNEF attributes and invokes the methods on the
 * <code>TnefBuilder</code> to build the parsed data.
 * <P>
 * For more information (good luck), search for "TNEF Stream Syntax" and
 * "TNEF Stream Structure" at http://www.microsoft.com
 *
 * @author Priyantha Jayanetti
 * @author Mirror Worlds Technologies
 * @version    Feb 20, 2000 Lifestreams 1.5
 *
 */

 public class TnefStreamParserImpl  implements TnefStreamParser {

    /**
     * Tnef builder.
     */
    protected TnefBuilder builder;

    /**
     * Associates a builder. The builder will be responsible for composing the final
     * TNEF message object.
     *
     * @param builder TnefBuilder implementation.
     */
    public void setBuilder(TnefBuilder builder) {
        this.builder = builder;
    }

    /**
     * Starts the parsing process. The Parser expects that the TNefBuilder has already
     * been set.
     * <pre>
     * TnefStream = TnefSignature TnefKey TnefObject
     * </pre>
     *
     * @param inputstream TNEF message stream. eg: <code>new FileInputStream("winmail.dat")</code>
     * @exception IOException due to parse errors or builder errors.
     */
    public void parse(InputStream inputstream) throws IOException {
        if (builder == null) {
            throw new IOException("Cannot parse TNEF stream - TnefBuilder has not been set.");
        }
        PushbackInputStream in = null;
        in = new PushbackInputStream(inputstream, 4096);
        // no need to buffer since pushback stream contains a buffer as well.
        /*
        if (!(inputstream instanceof BufferedInputStream)) {
            in = new PushbackInputStream(new BufferedInputStream(inputstream, 8192), 4048);
        } else {
            in = new PushbackInputStream(inputstream,1024);
        }*/

        int signature = readLEInt(in); // 32 bit msg sequence.
        builder.setSignature(signature);

        int key = (int)readLEUnsignedShort(in); // 16 bit key
        builder.setKey(key);
        //debug("sig = " + hex(signature));
        //debug("key = " + hex(key));
        readObject(in);
    }

    /**
     * Parses a Tnef Object.
     * <pre>
     *  TnefObject = MessageSequence | MessageSequence AttachSequence | AttachSequence
     * </pre>
     */
    protected void readObject(PushbackInputStream in) throws IOException {
        //debug("read object");
        int type = in.read();
        //debug(" type=" + type);
        in.unread(type & 0x000000ff);
        if (type == TnefConstants.LVL_MESSAGE) {
            //debug("Type LVL_MESSAGE ");
        } else if (type == TnefConstants.LVL_ATTACHMENT) {
            //debug("Type LVL_ATTACHMENT ");
        } else {
            throw new IOException("Unexpected TNEF token type. Expected LVL_MESSAGE or LVL_ATTACHMENT.");
        }
        readMessageSeq(in);
        readAttachSeq(in);
    }

    /**
     * Parses a Tnef MessageSequence.
     * <pre>
     *  MessageSequence =
     * </pre>
     */
    protected void readMessageSeq(PushbackInputStream in) throws IOException {
        //debug("Message_Seq");
        int type = in.read(); // lvl_message
        //debug(" type=" + type);
        int b = readLEInt(in); // const = attTneversion | attMessageClass
        //debug(" attver=" + b + " expect " + TnefConstants.attTnefVersion);
        unreadInt(in,b);
        in.unread(type);
        if (b == TnefConstants.attTnefVersion) {
            //debug("case1 tnefvesion");
        } else if (b == TnefConstants.attMessageClass) {
            //debug("case2 msgclass");
        } else {
            //debug("case3 msgattrseq");
        }
        readAttTnefVersion(in);
        readMsgAttributeSeq(in);
        readAttMessageClass(in);
        readMsgAttributeSeq(in);

    }

    /**
     * Parses a Tnef Version. Once the version is parsed, the Builder's
     * <code>setTnefVersion(version)</code> method is invoked.
     * <pre>
     *  attTnefVersion = LVL_MESSAGE attTnefVerion sizeof(ulong) 0x00010000 checksum
     * </pre>
     */
    protected void readAttTnefVersion(PushbackInputStream in) throws IOException {
        // LVL_MESSAGE attTnefVersion_32 sizeof(long) 0x00010000 checksum_16
        int type = in.read(); // lvl_message
        if (type != TnefConstants.LVL_MESSAGE) {
            in.unread(type);
            return;
        }
        int ver = readLEInt(in);
        if (ver != TnefConstants.attTnefVersion) {
            unreadInt(in,ver);
            in.unread(type);
            return;
        }
        //debug("  attTnefVersion = " + ver);
        readBytes(in,4,null);//"     sizeof");
        readBytes(in,4,null);//"     const");
        int checksum = readLEUnsignedShort(in);
        //debug("     checksum=" + checksum);
        builder.setTnefVersion(ver);
    }

    /**
     * Parses Tnef MessageClass token. Once the MessageClass is parsed, the Builder's
     * <code>setMessageAttribute(id,bytes)</code> method is invoked.
     * <pre>
     *  attMessageClass = LVL_MESSAGE attMessageClass length_int msg_class_data checksum
     * </pre>
     */
    protected void readAttMessageClass(PushbackInputStream in) throws IOException {
        //LVL_MESSAGE attMessageClass_32 msg_cls_length msg_cls checksum_16
        //debug("peek attMessageClass");
        int type = in.read(); // lvl_message
        if (type != TnefConstants.LVL_MESSAGE) {
            in.unread(type);
            return;
        }
        int id = readLEInt(in);
        if (id != TnefConstants.attMessageClass) {
            unreadInt(in,id);
            in.unread(type);
            return;
        }
        int len = readLEInt(in);
        int tot = 0;
        int num;
        byte buf[] = new byte[len];
        num = in.read(buf,0,len);
        tot = 0;
        for (int i = 0; i < len; i++) {
            tot += buf[i] & 0xff ;
        }
        int ev = tot % 65536;
        int checksum = readLEUnsignedShort(in);

        if (checksum != ev) {
            //debug("     error checksum=" + checksum + " ev=" + ev);
            throw new IOException("Parser checksum error reading attMessageClass token.");
        }
        builder.setMessageAttribute(id,buf);
    }

    /**
     * Parses Tnef Message Attribute Sequence token
     * <pre>
     *  MessageAttribSequence = MessageAttribute | MessageAttribute MessageAttributeSequence
     * </pre>
     */
    protected void readMsgAttributeSeq(PushbackInputStream in) throws IOException {
        //debug("  Msg_Attribute_Seq");
        boolean hasmore = readMsgAttribute(in);
        if (hasmore) {
            readMsgAttributeSeq(in);
        }
    }

    /**
     * Parses Tnef MessageAttribute token.  Once the attribute is parsed, the Builder's
     * <code>setMessageAttribute(id,bytes)</code> method is invoked.
     * <pre>
     *  MessageAttribute = LVL_MESSAGE attribute_id attribute_length_int attrib_data checksum
     * </pre>
     */
    protected boolean readMsgAttribute(PushbackInputStream in) throws IOException {
        //LVL_MESSAGE attrib_id_32 attrib_length attrib_data checksum_16
        int type = in.read(); // lvl_message
        if (type != TnefConstants.LVL_MESSAGE) {
            in.unread(type);
            return false;
        }
        int id = readLEInt(in);
        int len = readLEInt(in);
        int tot = 0;
        int num;
        byte buf[] = new byte[len];
        num = in.read(buf,0,len);
        tot = 0;
        for (int i = 0; i < len; i++) {
            tot += buf[i] & 0xff ;
        }
        int ev = tot % 65536;
        int checksum = readLEUnsignedShort(in);

        if (checksum != ev) {
            throw new IOException("Parser checksum error reading Msg_Attribute"
                            + " token (id=" + TnefConstants.getAttributeID(id) + ")");
        }
        builder.setMessageAttribute(id,buf);
        return true;
    }

    /**
     * Reads a AttachSeq.
     * <pre>
     *  Attach_Seq = attRendData | attRendData Att_Attribute_Seq
     * </pre>
     */
    protected void readAttachSeq(PushbackInputStream in) throws IOException {
        //debug("Attach_Seq");
        readAttAttributeSeq(in);
    }

    /**
     * Reads a Attach_Attribute_Sequence
     * <pre>
     *  Attach_Attribute_Seq = Att_Attribute | Att_Attribute Att_Attribute_Seq
     * </pre>
     */
    protected void readAttAttributeSeq(PushbackInputStream in) throws IOException {
        //debug("attAttributeSeq");
        boolean hasmore = readAttAttribute(in);
        if (hasmore) {
            readAttAttributeSeq(in);
        }
    }

    /**
     * Reads a Attach_Attribute.  Once the Attachment attribute is parsed, the Builder's
     * <code>setAttachmentAttribute(id,bytes)</code> method is invoked.
     * <pre>
     *  Att_Attribute = LVL_ATTACHMENT attrib-id attrib-length attrib-data checksum
     * </pre>
     */
    private boolean readAttAttribute(PushbackInputStream in) throws IOException {
        int type = in.read(); // lvl_attachment
        if (type != TnefConstants.LVL_ATTACHMENT) {
            in.unread(type);
            return false;
        }
        //debug("attAttribute");
        int id = readLEInt(in);

        if (id == TnefConstants.attAttachRenddata) {
            builder.addAttachment();
        }

        int len = readLEInt(in);
        //debug("attAttribute   id=" + id + " " + hex(TnefConstants.getAttID(id)) + " len=" + len);
        //debug("   len=" + len);
        int tot = 0;
        int num = 0;
        int pos = 0;
        int size = 128;
        if (len < size) {
            size = len;
        }
        byte buf[] = new byte[len];
        byte b[] = new byte[size];
        long t1 = System.currentTimeMillis();
        while (pos < len && (num = in.read(b,0,size)) != -1) {
            System.arraycopy(b,0,buf,pos,num);
            pos += num;
            if ((len-pos)< size) {
                size = len-pos;
            }
        }
        long t2 = System.currentTimeMillis();
        for (int i = 0; i < len; i++) {
            tot += buf[i] & 0xff ;
        }
        long t3 = System.currentTimeMillis();
        //debug("time=" + (t2-t1) + "," + (t3-t2) + " bytes=" + len + " read=" + num);
        int ev = tot % 65536;
        int checksum = readLEUnsignedShort(in);
        //debug("     checksum=" + checksum + " ev=" + ev);
        if (checksum != ev) {
            throw new IOException("Parser checksum error reading Attach_Attribute"
                            + " token (id=" + TnefConstants.getAttributeID(id) + ")");
        }
        builder.setAttachmentAttribute(id, buf);
        return true;
    }

    private void readBytes(PushbackInputStream in, int n, String s) throws IOException {
        String temp = "";
        int d = 0;
        for (int i = 0; i < n;i++) {
            d = in.read();
            temp = " " + hex(d) + temp;
        }
        if (s != null) {
            debug(s + temp);
        }
    }

    /**
     * @return a signed <code>short</code> (16 bit) value read in Little Endian format.
     */
    protected short readLEShort(PushbackInputStream in) throws IOException {
        int b1 = in.read();
        int b2 = in.read();
        if (b1 == -1 || b2 == -1) {
            throw new EOFException();
        }
        return (short)((b2 << 8) + b1);
    }

    /**
     * @return an unsigned <code>short</code> (16 bit) value read in Little Endian format.
     */
    protected int readLEUnsignedShort(PushbackInputStream in) throws IOException {
        int b1 = in.read();
        int b2 = in.read();
        if (b1 == -1 || b2 == -1) {
            throw new EOFException();
        }
        return ((b2 << 8) + b1);
    }

    /**
     * @return a signed <code>int</code> (32 bit) value read in Little Endian format.
     */
    protected int readLEInt(PushbackInputStream in) throws IOException {
        int b1 = in.read();
        int b2 = in.read();
        int b3 = in.read();
        int b4 = in.read();
        if (b4 == -1 || b3 == -1 || b2 == -1 || b1 == -1) {
            throw new EOFException();
        }
        return ((b4 << 24) + (b3 << 16) + (b2 << 8) + b1);
    }

    /**
     * Unreads (pushes back)a given <code>int</code> (32 bit) into the pushback
     * stream.
     */
    protected void unreadInt(PushbackInputStream in, int b) throws IOException{
        in.unread((b >> 24));
        in.unread((b >> 16));
        in.unread((b >> 8));
        in.unread(b);
    }

    private String hex(int i) {
        if (i > 9) {
            return (Integer.toHexString(i)).toUpperCase();
        } else {
            return "0" + (Integer.toHexString(i)).toUpperCase();
        }
    }

    private int integer(String hex) {
        try {
            return Integer.parseInt(hex,16);
        }catch(Exception e) {
            return -1;
        }
    }


    public void debug(String s) {
        System.out.println("TnefStreamParser>" + s);
    }



 }
