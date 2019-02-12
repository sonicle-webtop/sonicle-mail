/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sonicle.mail.imap;

import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.util.ASCIIUtility;
import java.util.ArrayList;
import javax.mail.FetchProfile;
import javax.mail.Message;
import javax.mail.MessagingException;
import org.apache.commons.lang3.StringUtils;


/**
 *
 * @author gabriele.bulfon
 */
public class SonicleIMAPThreadResponse extends IMAPResponse {
    
    public static final char ITEM_SEPARATOR='^';
    public static final char LEVEL_SEPARATOR=':';
    public static final char ELEMENT_SEPARATOR=',';
    
    private static final byte[] ANY_PARENTHESIS_CHARBYTES={ '(', ')' };
    private static final byte[] OPEN_PARENTHESIS_CHARBYTES={ '(' };
    private static final byte[] CLOSED_PARENTHESIS_CHARBYTES={ ')' };
    
    private int nmessages=0;
	
	SonicleIMAPFolder folder;
	FetchProfile fetchProfile;
	
	public SonicleIMAPThreadResponse(IMAPResponse r, SonicleIMAPFolder folder, FetchProfile fetchProfile) {
		super(r);
		this.folder=folder;
		this.fetchProfile=fetchProfile;
		// Set the first uid as parent if parent doesn't exists (orphaned siblings)
		String data=new String(buffer);
		data=data.replaceAll("\\(\\(([0-9]+) ", "($1 (");
		buffer=data.getBytes();
	}
	  
    public String parse() throws MessagingException {
        nmessages=0;
        String parsed="";
        int begin=indexOf(OPEN_PARENTHESIS_CHARBYTES);
        if (begin>0) {
            StringBuffer sb=parse(begin,0,0);
            parsed=sb.toString();
        }
        return parsed;
    }
	
    private StringBuffer parse(int begin, int end, int depth) throws MessagingException {
        
        StringBuffer node=new StringBuffer();
        if (end==0) end=buffer.length;
		ArrayList<String> smsgs=new ArrayList<String>();
        
        // Let's try to store data in max. compacted stracture as a string,
        // arrays handling is much more expensive
        // For the following structure: THREAD (2)(3 6 (4 23)(44 7 96))
        // -- 2
        // -- 3
        //     \-- 6
        //         |-- 4
        //         |    \-- 23
        //         |
        //         \-- 44
        //               \-- 7
        //                    \-- 96
        //
        // The output will be: 2,3^1:6^2:4^3:23^2:44^3:7^4:96
        
        if (buffer[begin]!='(') {
            
            // find next bracket
            int stop=indexOf(ANY_PARENTHESIS_CHARBYTES,begin, end);
            String messages[]=StringUtils.split(new String(buffer,begin,stop-begin).trim()," ");
            if (messages==null || messages.length==0) {
                return node;
            }
            
            for(String msg: messages) {
                if (msg!=null && msg.length()>0) {
                    if (depth>0) node.append(ITEM_SEPARATOR).append(depth).append(LEVEL_SEPARATOR);
                    node.append(msg);
					smsgs.add(msg);
                    ++nmessages;
                    ++depth;
                }
            }
            
            if (stop<end) {
                node.append(parse(stop,end,depth));
            }
            
        } else {
            int off = begin;
            while(off<end) {
                int start=off;
                off++;
                int n=1;
                while(n>0) {
                    int p = indexOf(CLOSED_PARENTHESIS_CHARBYTES, off);
                    if (p<0) {
                        // error, wrong structure, mismatched brackets in IMAP THREAD response
                        // TODO: write error to the log?
                        return node;
                    }
                    int p1 = indexOf(OPEN_PARENTHESIS_CHARBYTES, off);
                    if (p1 >=0 && p1 < p) {
                        off = p1 + 1;
                        n++;
                    }
                    else {
                        off = p + 1;
                        n--;
                    }
                }
                
                StringBuffer thread = parse(start + 1, off - 1, depth);
                if (thread!=null && thread.length()>0) {
                    if (depth==0) {
                        if (node!=null && node.length()>0) {
                            node.append(ELEMENT_SEPARATOR);
                        }
                    }
                    node.append(thread);
                }                
                
            }
        }
		int imsgs[]=new int[smsgs.size()];
		for(int i=0;i<imsgs.length;++i) imsgs[i]=Integer.parseInt(smsgs.get(i));
		Message msgs[]=new Message[imsgs.length];
		msgs=folder.getMessages(imsgs);
		folder.fetch(msgs, fetchProfile);
        
        return node;
    }
    
    public int getMessageCount() {
        return nmessages;
    }
    
    private int indexOf(byte charbytes[]) {
        return indexOf(charbytes,0,buffer.length);
    }
    
    private int indexOf(byte charbytes[], int begin) {
        return indexOf(charbytes,begin,buffer.length);
    }
    
    private int indexOf(byte charbytes[], int begin, int end) {
        int i=begin;
        for(;i<end;++i) {
            byte b=buffer[i];
            for(byte cb: charbytes)
                if (b==cb) return i;
        }
        return i;
    }

    
}
