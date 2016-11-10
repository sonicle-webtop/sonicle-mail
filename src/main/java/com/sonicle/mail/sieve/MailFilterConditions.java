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
public class MailFilterConditions extends ArrayList<MailFilterCondition> {
	
	public static final String OPERATOR_OR="or";
	public static final String OPERATOR_AND="and";
	
	long idfilter;
	boolean enabled;
	String action;
	String actionvalue;
	String operator;

	public MailFilterConditions(long idfilter, boolean enabled, String action, String actionvalue, String operator) {
        this.idfilter=idfilter;
		this.enabled=enabled;
		this.action=action;
		this.actionvalue=actionvalue;
		this.operator=operator;
	}
	
	public long getIDFilter() {
		return idfilter;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public String getAction() {
		return action;
	}
	
	public String getActionValue() {
		return actionvalue;
	}
	
	public String getOperator() {
		return operator;
	}
	
}
