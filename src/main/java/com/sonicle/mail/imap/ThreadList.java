/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sonicle.mail.imap;

import java.util.ArrayList;

/**
 *
 * @author gabriele.bulfon
 */
public class ThreadList {
	
	private int threadId=0;
	private final ArrayList<Integer> ids=new ArrayList<Integer>();

	private final int startIndex;
	private int endIndex;

	private ArrayList<ThreadList> brothers=null;

	public ThreadList(int start, int end) {
		startIndex=start;
		endIndex=end;
	}

	//first id is threadId
	public void addId(int id) {
		if (threadId==0) threadId=id;
		this.ids.add(id);
	}

	//first id is threadId
	public void insertId(int id) {
		if (threadId==0) threadId=id;
		this.ids.add(0,id);
	}

	public void addBrotherList(ThreadList btl) {
		if (brothers==null) brothers=new ArrayList<ThreadList>();
		brothers.add(btl);
		int bend=btl.getEndIndex()+1;
		if (bend>endIndex) endIndex=bend;
	}

	public ArrayList<ThreadList> getBrothers() {
		return brothers;
	}

	public boolean hasBrothers() {
		return brothers!=null;
	}

	public int getThreadId() {
		return threadId;
	}

	public ArrayList<Integer> getIds() {
		return ids;
	}

	public int size() {
		return ids.size();
	}

	public int getStartIndex() {
		return startIndex;
	}

	public int getEndIndex() {
		return endIndex;
	}

}
