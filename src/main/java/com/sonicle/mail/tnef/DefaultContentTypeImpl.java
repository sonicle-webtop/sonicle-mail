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

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Date;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * Default implementation for the tnef content-type map. Note that the methods in this
 * class are not synchronized i.e. not thread safe. <P>
 * To save the 72 preconfigured entries to a file from the command line:
 * <pre>
 *  java com.sonicle.mail.tnef.DefaultContentTypeImpl mimetypes.txt 
 * </pre>
 * @author Priyantha Jayanetti
 * @author Mirror Worlds Technologies
 * @version     Feb 20, 2000 Lifestreams 1.5
 *
 */
 
 public class DefaultContentTypeImpl implements TnefContentTypes {
    
    private Hashtable mimeTypes;
    private String defaultContentType = "application/octet-stream";

    /**
     * Creates and initialize a  list of file extensions and associated
     * mime content-types. This implementation is configured with 72 entries.
     */
    public DefaultContentTypeImpl() {
        mimeTypes = new Hashtable();
        createTypes();        
    }
    
    /**
     * @return number of extension/mime-type entries in this registry.
     */
     public int getSize() {
        return mimeTypes.size();
     }
     
    /**
     * Returns an Enumeration to the current file extensions. Useful for subclasses to
     * see the current extentsions.
     */
    public Enumeration extensions() {
        return mimeTypes.keys();
    }
    
    /**
     * Returns an Enumeration to the current mime types. Useful for subclasses to
     * see the current content-types.
     */
    public Enumeration contentTypes() {
        return mimeTypes.elements();
    }
    
    /**
     * Lets subclasses add extension and content-types.
     *
     * @param ext file extension
     * @param contentType MIME type associated with the file extension.
     */
    public void addType(String ext, String contentType) {
        if (ext != null && contentType != null ) {
            mimeTypes.put(ext.toLowerCase(),contentType.toLowerCase());
        }
    }

    /**
     * Lets subclasses remove extension and its associated content-type.
     *
     * @param ext file extension
     */
    public void removeExtension(String ext) {
        if (ext != null && mimeTypes.containsKey(ext)) {
            mimeTypes.remove(ext);
        }
    }

    /**
     * Lets subclasses remove content-type and its associated file extensions.
     *
     * @param contentType MIME content-type.
     */
    public void removeContentType(String contentType) {
        if (contentType != null) {            
            Enumeration e = mimeTypes.keys();
            String value;
            String key;
            while(e.hasMoreElements()) {
                key = (String) e.nextElement();
                value = (String) mimeTypes.get(key);
                if (contentType.equalsIgnoreCase(value)) {
                    mimeTypes.remove(key);
                }//if
            }//while
        }//if
    }
    
    /**
     * Writes the current list of mime type to the output stream using the format <BR>
     * <pre>extension [space] mime-type</pre> <br>
     * For example:
     * <pre>
     *       try {
     *           DefaultContentTypeImpl ct = new DefaultContentTypeImpl();
     *           FileOutputStream fos = new FileOutputStream("types.txt");
     *           ct.writeTo(fos);
     *           fos.close();
     *       }catch(Exception e) {
     *       }      
     * </pre>
     * @param o outputstream to where the contents are written to.
     */
    public void writeTo(OutputStream o)throws IOException {
        PrintWriter out = new PrintWriter(o);
        Date date = new Date();
        out.println("# Mime-type contents in registry  " + this.getClass().getName());
        out.println("# " + date.toString());
        out.println("# " + mimeTypes.size() + " entries found.");        
        out.println("");
        Enumeration e = extensions();
        while(e.hasMoreElements()) {
            String n = (String)e.nextElement();
            String v = (String)mimeTypes.get(n);
            out.println(n + " " + v);
            out.flush();
        }
    }
    
    /**
     * Loads the extension|content-type from an (text)inputstream. This method expects the
     * inputstream to contain entries in the following (one entry per line) format: <br>
     * <pre>extension [space] mime-type</pre> <br>
     * If the <code>clear</code> flag is set to <code>true</code> then the current
     * contents of the registry are cleared prior to loading. Otherwise, the content
     * are replaced (over written).
     * For example:
     * <pre>
     *       try {
     *           DefaultContentTypeImpl ct = new DefaultContentTypeImpl();
     *           FileInputStream fis = new FileInputStream("my-types.txt");
     *           ct.load(fos,false);
     *           fis.close();
     *       }catch(Exception e) {
     *       }      
     * </pre>
     * Any line begining with the pound (<code>#</code>) sign is assumed to be a 
     * comment and is ignored.
     *
     * @param i inputstream of ext/mime-type contents
     * @param clear if <code>true</code>,clears the current contents prior to loading.
     */
    public void load(InputStream i, boolean clear)throws IOException {
        if (clear) {
            mimeTypes.clear();
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(i));
        String line = null;
        String ext;
        String ct;
        while ( (line = reader.readLine()) != null ) {
            line = line.trim();
            if (line.equals("") || line.startsWith("#")) {
                continue;
            }
            // format: extension <space> content-type
            int idx = line.indexOf(" ");
            if (idx > 0) {
                ext = line.substring(0,idx).toLowerCase();
                ct = line.substring(idx +1).trim().toLowerCase();
                mimeTypes.put(ext, ct);
            }
        }
    }    
    
    // ------ TnefContentTypes Interface Methods --------
    
    /**
     * Returns the MIME content type given a file extension. Eg: .txt or .gif
     * The default value is application/octet-stream
     *
     * @param ext file extentsion.
     */
    public String getContentTypeFromExtension(String ext) { 
        //System.out.println("GET CT ext=" + ext + " SIZE=" + mimeTypes.size());
        if (ext == null || ext.trim().length() == 0) {
            return defaultContentType;
        }
        ext = ext.trim().toLowerCase();
        String mime = (String)mimeTypes.get(ext);        
        //System.out.println("GET CT ext=" + ext + " MIME=" + mime);
        return (mime != null)? mime : defaultContentType;
    }        

    /**
     * Returns the MIME content type given a file name. Eg: hello.txt or image.gif
     * The default value is application/octet-stream
     *
     * @param name file name with extentsion.
     */
    public String getContentTypeFromFilename(String filename){
        //System.out.println("GET CT filename=" + filename);
        if (filename == null) {
            return defaultContentType;
        }
        int idx = filename.lastIndexOf(".");
        if (idx > 0) {
            String ext = filename.substring(idx + 1);
            return getContentTypeFromExtension(ext);
        }
        return getContentTypeFromExtension(filename);
    }
    
        
    public static void main(String args[]) {
        DefaultContentTypeImpl ct = new DefaultContentTypeImpl();
        if (args != null && args.length == 1) {
            FileInputStream fis = null;
            FileOutputStream fos = null;
            try {
                //fis = new FileInputStream("mytypes.txt");
                //ct.load(fis,false);
                //fis.close();
                fos = new FileOutputStream(args[0]);
                ct.writeTo(fos);
                fos.close();
            }catch(Exception e) {
                System.out.println(e);
                e.printStackTrace();
            } finally {
            }
        } else {            
            try {
                ct.writeTo(System.out);
            }catch(IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println();
        System.out.println("" + ct.getSize() + " entries found.");
        
    }
        
    private void createTypes() {
        mimeTypes.put("txt",    "text/plain");
        mimeTypes.put("html",   "text/html");
        mimeTypes.put("shtml",  "text/html");
        mimeTypes.put("htm",    "text/html");        
        mimeTypes.put("tex",    "text/vnd.latex-z");
        mimeTypes.put("rtf",     "text/richtext");
        mimeTypes.put("css",    "text/css");
        mimeTypes.put("htt",    "text/webviewhtml");
        mimeTypes.put("rt",     "text/vnd.rn-realtext");
        mimeTypes.put("vcf",    "text/x-vcard");
        
        mimeTypes.put("msg",    "message/rfc822");
        mimeTypes.put("msg",    "message/rfc822");
        mimeTypes.put("mhtml",  "message/rfc822");
        mimeTypes.put("mht",    "message/rfc822");
        mimeTypes.put("eml",    "message/rfc822");
        
        mimeTypes.put("ps",     "application/postscript");
        mimeTypes.put("eps",    "application/postscript");
        mimeTypes.put("pdf",    "application/pdf");
        mimeTypes.put("zip",    "application/zip");
        mimeTypes.put("tar",    "application/x-tar");
        mimeTypes.put("gz",     "application/x-tar");
        mimeTypes.put("doc",    "application/msword");
        mimeTypes.put("wri",    "application/msword");
        mimeTypes.put("mat",    "application/mathematica");
        mimeTypes.put("fm",     "application/vnd.framemaker");
        mimeTypes.put("xls",    "application/vnd.ms-excel");
        mimeTypes.put("xlb",    "application/vnd.ms-excel");
        mimeTypes.put("xlm",    "application/vnd.ms-excel");
        mimeTypes.put("xla",    "application/vnd.ms-excel");
        mimeTypes.put("xlc",    "application/vnd.ms-excel");
        mimeTypes.put("csv",    "application/vnd.ms-excel");
        mimeTypes.put("mdb",    "application/vnd.ms-access");
        mimeTypes.put("ppt",    "application/vnd.ms-powerpoint");
        mimeTypes.put("wcm",    "application/vnd.ms-works");
        mimeTypes.put("wks",    "application/vnd.ms-works");
        mimeTypes.put("123",    "application/vnd.lotus-1-2-3");
        mimeTypes.put("wpd",    "application/wordperfect5.1");
        mimeTypes.put("cdf",    "application/x-cdf");
        mimeTypes.put("js",     "application/x-javascript");
        mimeTypes.put("dir",    "application/x-director");
        mimeTypes.put("dcr",    "application/x-director");
        mimeTypes.put("dxr",    "application/x-director");
        mimeTypes.put("rm",     "application/vnd.rn-realmedia");
        mimeTypes.put("prx",    "application/vnd.rn-realplayer");
        mimeTypes.put("vsd",    "application/visio");
        
        mimeTypes.put("gif",    "image/gif");
        mimeTypes.put("jpg",    "image/jpeg");
        mimeTypes.put("jpeg",   "image/jpeg");
        mimeTypes.put("tif",    "image/tiff");
        mimeTypes.put("tiff",   "image/tiff");        
        mimeTypes.put("png",    "image/png");
        mimeTypes.put("bmp",    "image/bmp");
        mimeTypes.put("rf",     "image/vnd.rn-realflash");
        mimeTypes.put("rp",     "image/vnd.rn-realpix");
        mimeTypes.put("dwg",    "image/vnd.dwg");
        mimeTypes.put("dxf",    "image/vnd.dxf");
        
        mimeTypes.put("au",     "audio/au");
        mimeTypes.put("wav",    "audio/wav");        
        mimeTypes.put("aif",    "audio/x-aiff");
        mimeTypes.put("rmi",    "audio/midi");
        mimeTypes.put("mid",    "audio/midi");
        mimeTypes.put("ra",     "audio/vnd.rn-realaudio");
        mimeTypes.put("ram",    "audio/x-pn-realaudio");
        
        mimeTypes.put("mpg",    "video/mpeg");
        mimeTypes.put("mpeg",   "video/mpeg");
        mimeTypes.put("qt",     "video/quicktime");
        mimeTypes.put("qtm",    "video/quicktime");
        mimeTypes.put("mov",    "video/quicktime");
        mimeTypes.put("asf",    "video/x-ms-asf");
        mimeTypes.put("avi",    "video/x-msvideo");
        mimeTypes.put("rv",     "video/vnd.rn-realvideo");
        
        mimeTypes.put("wrl",    "model/vrml");
        mimeTypes.put("vrml",   "model/vrml");
        
    }  
 }    