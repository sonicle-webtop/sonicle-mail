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
import java.util.*;

/**
 * Creates MS-TNEF Message Objects given a inputstream of TNEF data.
 * (eg: winmail.dat file). Usage example:
 * <pre>
 *       try {
 *          String filename = "winmail.dat";
 *          //create message object using default parser and builder.
 *          TnefMessage msg = JTnef.createTnefMessage(filename);          
 *          msg.printInfo(); // print debug info
 *          int count = msg.getCount();   // get number of attachments.   
 *           for (int i = 0; i < count; i++) { // loop thru attachments and print
 *              TnefAttachment a = msg.getAttachmentAt(i);
 *               System.out.println("   -- ");
 *               a.printInfo();
 *               // content-type of attachment is a.getContentType();
 *               // filename of attachment is a.getFilename();
 *               // attachment stream is a.getInputStream();
 *               // you can save attachment:
 *               //      File f = new File(a.getFilename);
 *               //      FileOutputStream fos = new FileOutputStream(f);      
 *               //      a.writeTo(f); // save attachment.
 *           }
 *       }catch(Exception e) {
 *           e.printStackTrace
 *       } 
 * </pre>
 * If you want to, you could specify a TNEF stream parser and builder.
 * <pre>
 *       //get parser - this class parses the tnef byte streams
 *       TnefStreamParser = new TnefStreamParserImpl();
 *       // get the builder. The parser will call methods in the builder when 
 *       // the parser encounters tnef tokens, attributes, data etc. The builder
 *       // will take this information to build a TnefMessage class.
 *       TnefBuilder builder = new TnefMessageBuilder();
 *       //associate the builder to the parser.
 *       parser.setBuilder(builder);
 *       
 *       //get content-type manager (maps file extensions to MIME content-types)
 *       TnefContentType contentTypes = new DefaultContentTypeImpl();        
 *       //associate the content-types with the builder.
 *       builder.setContentTypes(contentTypes);
 *       
 *       // begin parsing.
 *       parser.parse(tnefStream);        
 *       // and finaly grab the message.
 *       TnefMessage msg =  builder.getMessage();
 * </pre>
 *
 * @author Priyantha Jayanetti
 * @author Mirror Worlds Technologies
 * @version    Feb 19, 2000 Lifestreams 1.5
 *
 */

public class JTnef {

    /**
     * Parses and returns a <code>TnefMessage</code> message given a TNEF binary
     * input stream (i.e not a base64 encoded stream). 
     * The client code is responsible for closing the input stream once the parsing
     * is completed.
     *
     * @param tnefStream TNEF raw binary stream.
     * @exception IOException if the parsing failed.
     * @return TnefMessage containing Tnef attachments etc.
     * @see TnefMessage     
     */    
    public static TnefMessage createTnefMessage(InputStream tnefStream) 
                                                            throws IOException {
        //define vars                                                                
        TnefStreamParser parser;
        TnefBuilder      builder;
        TnefContentTypes contentTypes;
        TnefMessage      message;     
        
        //get parser - this class parses the tnef byte streams
        parser = createParser();
        // get the builder. The parser will call methods in the builder when 
        // the parser encounters tnef tokens, attributes, data etc. The builder
        // will take this information to build a TnefMessage class.
        builder = createBuilder();
        //associate the builder to the parser.
        parser.setBuilder(builder);
        
        //get content-type manager (maps file extensions to MIME content-types)
        contentTypes = createContentTypes();        
        //associate the content-types with the builder.
        builder.setContentTypes(contentTypes);
        
        // begin parsing.
        parser.parse(tnefStream);        
        // and finaly grab the message.
        return builder.getMessage();
    }

    /**
     * Parses and returns a <code>TnefMessage</code> message given a TNEF filename.
     * Generally, this is the 'winmail.dat' file. 
     *
     * @param winmailFilename name of file (winmail.dat)
     * @exception IOException if the parsing failed.
     * @return TnefMessage containing Tnef attachments etc.
     * @see TnefMessage     
     */    
    public static TnefMessage createTnefMessage(String winmailFilename) throws IOException {
        return createTnefMessage(new File(winmailFilename));
    }

    /**
     * Parses and returns a <code>TnefMessage</code> message given a TNEF File object.
     * Generally, this is the File pointing to 'winmail.dat' file.
     *
     * @param winmailFile winmail.dat File object.
     * @exception IOException if the parsing failed.
     * @return TnefMessage containing Tnef attachments etc.
     * @see TnefMessage     
     */    
    public static TnefMessage createTnefMessage(File winmailFile)throws IOException {
        FileInputStream winmailStream = new FileInputStream(winmailFile);
        TnefMessage m = createTnefMessage(winmailStream);
        winmailStream.close();
        return m;
    }


    // ideally the following create methods should be based on a Factory
    // or a system property that defines the concrete class names. 
    
    /**
     * @return a concrete TNEF stream parser.
     */
    private static TnefStreamParser createParser() {
        return new TnefStreamParserImpl();
    }

    /**
     * @return a concrete TNEF Message builder.
     */    
    private static TnefBuilder createBuilder() {
        return new TnefMessageBuilder();
    }
    
    /**
     * @return a concrete TNEF content-type.
     */    
    private static TnefContentTypes createContentTypes() {
        return new DefaultContentTypeImpl();
        //return new TopSightContentTypeImpl();
    }

    public void debug(String s) {
        System.out.println("JTnef>" + s);
    }
    
    private static void printUsage() {
        System.out.println();
        System.out.println("LSTNEF: TNEF stream decoder API");
        System.out.println("(c)1999-2000 Mirror Worlds Technologies Inc.");
        System.out.println("http://www.mirrorworlds.com");
        System.out.println();
        System.out.println("usage: com.mirrorworlds.lifestreams.mail.tnef.JTnef [xv] winmail.dat [outputdir]");   
        System.out.println("     v generates verbose output to the stdout.");   
        System.out.println("     x extracts the attachments to the current directory.");
        System.out.println("     outputdir specifies a extract destination directory.");   
        System.out.println();
        System.out.println("example:");
        System.out.println("  JTnef v winmail.dat");
        System.out.println("    prints the contents of the winmail file.");
        System.out.println("  JTnef xv winmail.dat");
        System.out.println("    extracts(and prints) the contents of the winmail.dat to current directory.");
        System.out.println("  JTnef xv winmail.dat c:\\temp");
        System.out.println("    extracts(and prints) the contents of the winmail.dat to directory c:\\temp.");
        System.out.println();
    }
    
    private static void print(boolean verbose, String s) {
        if (verbose) {
            System.out.println(s);
        }
    }
    
    public static void main(String args[]) {
        if (args == null || args.length < 2) {
            printUsage();
            System.exit(0);
        }
        
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        
        String dest = ".";
        String filename = "winmail.dat";
        String flags = "";
        boolean verbose = false;
        boolean extract = false;
        
        flags = args[0];
        if (flags != null) {
            if (flags.indexOf("v")!= -1) {
                verbose = true;
            }
            if (flags.indexOf("x")!= -1) {
                extract = true;
            }            
        }
        filename = args[1];
        if (args.length > 2 && args[2] != null) {
            dest = args[2];
        }
        
        print(verbose,"Starting ... " + filename);
        File dir = new File(dest);
        if (extract) {
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        
        try {
            TnefMessage msg = JTnef.createTnefMessage(filename);
            if (verbose) {
                msg.printInfo();
            }
            int count = msg.getCount();            
            for (int i = 0; i < count; i++) {
                TnefAttachment a = msg.getAttachmentAt(i);
                if (verbose) {
                    a.printInfo();
                }                
                if (extract) {
                    File file = new File(dir, a.getFilename());
                    print(verbose,"   Saving to " + file);
                    FileOutputStream fos = new FileOutputStream(file);
                    a.writeTo(fos);
                    fos.close();
                }
            }
        }catch(Exception e) {
            System.out.println("Error ... " + e);
            e.printStackTrace();
        }
        print(verbose, "Done ... " + filename);
       
    }
    
}    
    