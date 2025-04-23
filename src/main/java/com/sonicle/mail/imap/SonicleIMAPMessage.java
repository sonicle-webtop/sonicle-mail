/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sonicle.mail.imap;

import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.protocol.BODYSTRUCTURE;
import com.sun.mail.imap.protocol.Item;
import java.util.Date;
import java.util.Map;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.internet.MailDateFormat;
import java.io.IOException;
import java.text.ParseException;

/**
 *
 * @author gabriele.bulfon
 */
public class SonicleIMAPMessage extends IMAPMessage {
	
	private int threadId=0;
	
	private int threadSize;
	private Date threadMostRecentDate=null;
	
	private boolean threadMostRecent=false;
	private boolean threadOldest=false;
	
	private int threadIndent=0;
	private int threadChildren=0;
	
	private int peekCount=0;
	
	private static final MailDateFormat mailDateFormat = new MailDateFormat();
	
	public SonicleIMAPMessage(SonicleIMAPFolder folder, int msgnum) {
		super(folder,msgnum);
		threadSize=1;
	}
	
	public void setThreadId(int id) {
		this.threadId=id;
	}
	
	public void setThreadSize(int size) {
		this.threadSize=size;
	}
	
	public void setThreadIndent(int indent) {
		this.threadIndent=indent;
	}
	
	public int getThreadIndent() {
		return threadIndent;
	}
	
	public void setThreadChildren(int n) {
		threadChildren=n;
	}
	
	public void incrementThreadChildren() {
		threadChildren++;
	}
	
	public int getThreadChildren() {
		return threadChildren;
	}
	
	public boolean hasThreads() {
		return threadSize>1;
	}
	
	public void setMostRecentInThread(boolean b) {
		this.threadMostRecent=b;
	}
	
	public void setOldestInThread(boolean b) {
		this.threadOldest=b;
	}
	
	public void setMostRecentThreadDate(Date date) {
		this.threadMostRecentDate=date;
	}
	
	public int getThreadId() {
		return threadId;
	}
	
	public int getThreadSize() {
		return threadSize;
	}
	
	public boolean isMostRecentInThread() {
		return threadMostRecent;
	}

	public boolean isOldestInThread() {
		return threadOldest;
	}
	
	public Date getMostRecentThreadDate() {
		if (threadMostRecentDate==null) try {
			threadMostRecentDate=_getDate();
		} catch (MessagingException ex) {
			ex.printStackTrace();
		}
		return threadMostRecentDate;
	}
	
	public Date _getDate() throws MessagingException {
		Date d=getSentDate();
		if (d==null) d=getReceivedDate();
		if (d==null) d=new java.util.Date(0);
		return d;
	}	
	
	/**
	 * Returns the value of the RFC 5322 "Resent-Date" field.
	 * It indicates the date and time at which the resent message is dispatched 
	 * by the resender of the message. Like the "Date:" field, it is not the 
	 * date and time that the message was actually transported.
	 * @return The Resent-Date
	 * @throws MessagingException 
	 */
	public Date getResentDate() throws MessagingException {
		String s = getHeader("Resent-Date", null);
		if (s != null) {
			try {
				synchronized (mailDateFormat) {
					return mailDateFormat.parse(s);
				}
			} catch (ParseException pex) {
				return null;
			}
		}
		return null;
	}
	
	@Override
	protected void setUID(long uid) {
		super.setUID(uid);
	}
	
	@Override
    public long getUID() {
		return super.getUID();
    }
	
	@Override
    protected boolean handleFetchItem(Item item,
				String[] hdrs, boolean allHeaders)
				throws MessagingException {
		return super.handleFetchItem(item,hdrs,allHeaders);
    }
	
	@Override
    protected void handleExtensionFetchItems(Map extensionItems) {
		super.handleExtensionFetchItems(extensionItems);
	}	

	@Override
	public synchronized void setPeek(boolean peek) {
		//keep track of setPeek calls
		peekCount+=peek?1:-1;
		if ((peek && peekCount==1) || peekCount<=0) super.setPeek(peek);
		if (peekCount<0) peekCount=0;
	}
    
	public BODYSTRUCTURE getBodyStructure() {
		return bs;
	}

    public boolean hasAttachments() throws MessagingException, IOException {
		return hasAttachments(null);
	}
	
    public boolean hasAttachments(String lowerCaseNamePattern) throws MessagingException, IOException {
		return bs!=null && hasAttachments(getBodyStructure(), lowerCaseNamePattern);
	}
	
    private boolean hasAttachments(BODYSTRUCTURE bs, String lowerCaseNamePattern) throws MessagingException, IOException {
        boolean retval=false;
        
		if (isAttachment(bs)) {
			if (lowerCaseNamePattern!=null) {
				String filename = bs.cParams!=null ? bs.cParams.get("name") : null;
				if (filename==null) filename = bs.dParams!=null ? bs.dParams.get("filename") : null;
				if (filename!=null && filename.toLowerCase().contains(lowerCaseNamePattern))
					retval=true;
			}
			else retval=true;
		}
        else if(bs.bodies!=null && bs.bodies.length>0) {
            for(int i=0;i<bs.bodies.length;++i) {
                if (hasAttachments(bs.bodies[i], lowerCaseNamePattern)) {
                    retval=true;
                    break;
                }
            }
        }
        
        return retval;
    }

	private boolean isAttachment(BODYSTRUCTURE bs) {
		return bs.disposition!=null && bs.disposition.equals("ATTACHMENT");
	}
	
	// strict : check also request method
    public boolean hasInvitation(boolean strict) throws MessagingException, IOException {
		return hasInvitation(bs, strict);
	}
	
    private boolean hasInvitation(BODYSTRUCTURE bs, boolean strict) throws MessagingException, IOException {
        boolean retval=false;
        
		if (isInvitation(bs, strict)) {
			retval=true;
		}
        else if(bs.bodies!=null && bs.bodies.length>0) {
            for(int i=0;i<bs.bodies.length;++i) {
                if (hasInvitation(bs.bodies[i], strict)) {
                    retval=true;
                    break;
                }
            }
        }
        
        return retval;
    }

	protected boolean isInvitation(BODYSTRUCTURE bs, boolean strict) throws MessagingException {
		boolean retval =  false;
		boolean isTextCalendar = bs.type!=null && bs.type.equals("TEXT") && bs.subtype!=null && bs.subtype.equals("CALENDAR");
		if (isTextCalendar && strict) {
			String method = bs.cParams.get("method");
			if (method != null && method.equals("REQUEST")) retval = true;
		}
		return retval;
	}    
	
}
