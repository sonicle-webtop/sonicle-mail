/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sonicle.mail.imap;

import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.protocol.Item;
import java.util.Date;
import java.util.Map;
import jakarta.mail.MessagingException;

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
    
    
	
}
