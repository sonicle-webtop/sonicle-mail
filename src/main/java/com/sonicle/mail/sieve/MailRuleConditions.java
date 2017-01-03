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
public class MailRuleConditions extends ArrayList<MailRuleCondition> {
	
	public static final String OPERATOR_OR="or";
	public static final String OPERATOR_AND="and";
	
	long rule_id;
	boolean enabled;
	String action;
	String actionvalue;
	String operator;

	public MailRuleConditions(long rule_id, boolean enabled, String action, String actionvalue, String operator) {
        this.rule_id=rule_id;
		this.enabled=enabled;
		this.action=action;
		this.actionvalue=actionvalue;
		this.operator=operator;
	}
	
	public long getRuleId() {
		return rule_id;
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
