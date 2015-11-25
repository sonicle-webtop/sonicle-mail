/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sonicle.mail.imap;

import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.util.ASCIIUtility;


/**
 *
 * @author gabriele.bulfon
 */
public class SonicleIMAPThreadResponse extends IMAPResponse {
	
	public SonicleIMAPThreadResponse(IMAPResponse r) {
		super(r);
	}
	
	public ThreadList readThreadList() {
		ThreadList tl=readThreadList(0,index,buffer.length);
		if (tl!=null) index=tl.getEndIndex()+1;
		return tl;
	}
	
	private ThreadList readThreadList(int level, int rstart, int rend) {
		ThreadList tl=null;
		
		int index=rstart;
		
		index=skipSpaces(index,rend);
		
		if (index>=rend) {// not what we expected
			return null;
		}
		if (level==0) {
			if (buffer[index] != '(') // not what we expected
				return null;
				
			++index;
			if (buffer[index]=='(') {
				//get first list
				ThreadList subtl=readThreadList(level+1,index+1,rend);
				if (subtl==null) return null;
				index=skipSpaces(subtl.getEndIndex(),rend)+1;
				while(index<rend && buffer[index]!=')') {
					ThreadList bsubtl=readThreadList(level+1,index+1,rend);
					if (bsubtl==null) return null;
					subtl.addBrotherList(bsubtl);
					index=skipSpaces(bsubtl.getEndIndex(),rend)+1;
				}
				return subtl;
			}
		}
			
		//not level 0
		
		int indent=1;
		int end=rend;
		int start=index;
		while(index<end && indent>0) {
			byte b=buffer[index++];
			switch(b) {
				case '(':
					++indent;
					break;
					
				case ')':
					--indent;
					break;
			}
		}
		end=index-1;
		
		//something was wrong
		if (indent>0 || start==end) return null;
		
		int ix=start;
		tl=new ThreadList(start,end);
		while(ix<end) {
			boolean isBrother=false;
			boolean isChild=false;
			char c;
			while(ix<end && !Character.isDigit(c=(char)buffer[ix])) {
				++ix;
				switch(c) {
					case '(': isBrother=true; isChild=false; break;
					case ' ': isChild=true; isBrother=false; break;
				}
			}
			int ix2=ix;
			while(ix2<end && Character.isDigit((char)buffer[ix2])) ++ix2;
			if (ix2>ix) {
				int n=ASCIIUtility.parseInt(buffer, ix, ix2);
				tl.insertId(n);
				ix=ix2;
			}
		}
		
		return tl;
	}
	
    public int skipSpaces(int start, int end) {
		int index=start;
		while (index < end && buffer[index] == ' ')
			index++;
		return index;
    }
	
}
