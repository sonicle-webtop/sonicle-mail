/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sonicle.mail.sieve;

/**
 *
 * @author gabriele.bulfon
 */
public class SieveResponse {
	String lines[];
	boolean status;
	
	public SieveResponse(boolean status, String lines[]) {
		this.status=status;
		this.lines=lines;
	}
	
	public String getMessage() {
		String msg;
		if (lines!=null && lines.length>0) {
			StringBuffer sb=new StringBuffer();
			for(String line: lines) { sb.append(line); sb.append('\n'); }
			msg=sb.toString();
		}
		else msg="";
		return msg;
	}
	
	
}
