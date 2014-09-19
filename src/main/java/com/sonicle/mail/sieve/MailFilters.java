/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sonicle.mail.sieve;

import java.util.ArrayList;

/**
 *
 * @author gbulfon
 */
public class MailFilters extends ArrayList<MailFilterConditions> {
	
	private String vacationMessage=null;
	private String vacationAddresses=null;
	
	public boolean isVacationEnabled() {
		return vacationAddresses!=null;
	}
	
	public void setVacation(String message, String addresses) {
		this.vacationAddresses=addresses;
		this.vacationMessage=message;
	}
	
	public String getVacationMessage() {
		return vacationMessage;
	}
	
	public String getVacationAddresses() {
		return vacationAddresses;
	}
}
