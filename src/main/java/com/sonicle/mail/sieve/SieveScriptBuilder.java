/*
 * webtop-mail is a WebTop Service developed by Sonicle S.r.l.
 * Copyright (C) 2014 Sonicle S.r.l.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation with the addition of the following permission
 * added to Section 15 as permitted in Section 7(a): FOR ANY PART OF THE COVERED
 * WORK IN WHICH THE COPYRIGHT IS OWNED BY SONICLE, SONICLE DISCLAIMS THE
 * WARRANTY OF NON INFRINGEMENT OF THIRD PARTY RIGHTS.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301 USA.
 *
 * You can contact Sonicle S.r.l. at email address sonicle@sonicle.com
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License version 3.
 *
 * In accordance with Section 7(b) of the GNU Affero General Public License
 * version 3, these Appropriate Legal Notices must retain the display of the
 * "Powered by Sonicle WebTop" logo. If the display of the logo is not reasonably
 * feasible for technical reasons, the Appropriate Legal Notices must display
 * the words "Powered by Sonicle WebTop".
 */
package com.sonicle.mail.sieve;

import com.sonicle.commons.EnumUtils;
import com.sonicle.commons.RegexUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author malbinola
 */
public class SieveScriptBuilder {
	private static final String REQUIRE_BODY = "body";
	private static final String REQUIRE_COPY = "copy";
	private static final String REQUIRE_FILEINTO = "fileinto";
	private static final String REQUIRE_IMAP4FLAGS = "imap4flags";
	private static final String REQUIRE_REJECT = "reject";
	private static final String REQUIRE_VACATION = "vacation";
	
	private final HashSet<String> requires = new HashSet<>();
	private String spamFolder = null;
	private SieveVacation vacation = null;
	private final ArrayList<Filter> filters = new ArrayList<>();
	
	public SieveScriptBuilder() {}
	
	public void setSpamFilter(String spamFolder) {
		this.spamFolder = spamFolder;
	}
	
	public void setVacation(SieveVacation vacation) {
		this.vacation = vacation;
	}
	
	public void addFilter(String name, SieveMatch match, ArrayList<SieveRule> rules, ArrayList<SieveAction> actions) {
		filters.add(new Filter(name, match, rules, actions));
	}
	
	public String build() {
		StringBuilder sb = new StringBuilder();
		
		if (!StringUtils.isBlank(spamFolder)) {
			sb.append(spamFilter(spamFolder));
		}
		if (vacation != null) {
			sb.append(vacation(vacation));
		}
		for(Filter filter : filters) {
			sb.append(rule(filter));
		}
		
		return requires() + sb.toString();
	}
	
	private String requires() {
		StringBuilder sb = new StringBuilder();
		
		if (!requires.isEmpty()) {
			sb.append("require");
			sb.append(" ");
			sb.append("[");

			int i = 0;
			for(String require : requires) {
				i++;
				sb.append("\"");
				sb.append(require);
				sb.append("\"");
				if (i != requires.size()) {
					sb.append(", ");
				}
			}

			sb.append("]");
			sb.append(";\n");
		}
		
		return sb.toString();
	}
	
	private String spamFilter(String spamFolder) {
		StringBuilder sb = new StringBuilder();
		requires.add(REQUIRE_FILEINTO);
		
		sb.append("if exists \"X-Spam-Flag\" {");
		sb.append("\n");
		sb.append(" if header :contains \"X-Spam-Flag\" \"YES\" {");
		sb.append("\n");
		sb.append("  fileinto ").append(asValueArgument(spamFolder)).append(";");
		sb.append("\n");
		sb.append("  stop;");
		sb.append("\n");
		sb.append("}");
		sb.append("\n");
		sb.append("}");
		sb.append("\n");
		
		return sb.toString();
	}
	
	private String vacation(SieveVacation vacation) {
		StringBuilder sb = new StringBuilder();
		requires.add(REQUIRE_VACATION);
		
		sb.append("vacation");
		sb.append(" ");
		
		Short days = (vacation.getDaysInterval() != null) ? vacation.getDaysInterval() : 1;
		sb.append(":days");
		sb.append(" ");
		sb.append(printValue(days));
		sb.append(" ");
		
		if (!StringUtils.isBlank(vacation.getAddresses())) {
			sb.append(":addresses");
			sb.append(" ");
			sb.append(printQuotedArray(extractAddresses(vacation.getAddresses())));
			sb.append(" ");
		}
		
		if (!StringUtils.isBlank(vacation.getSubject())) {
			sb.append(":subject");
			sb.append(" ");
			sb.append(printQuotedValue(vacation.getSubject()));
			sb.append(" ");
		}
		
		if (!StringUtils.isBlank(vacation.getMessage())) {
			sb.append(printTextValue(vacation.getMessage()));
		}
		
		sb.append(";\n");
		
		return sb.toString();
	}
	
	private String[] extractAddresses(String addresses) {
		String[] addrs = StringUtils.split(vacation.getAddresses(), ",");
		for(int i=0; i<addrs.length; i++) {
			addrs[i] = StringUtils.trim(addrs[i]);
		}
		return addrs;
	}
	
	private String printTextValue(Object obj) {
		StringBuilder sb = new StringBuilder();
		sb.append("text:");
		sb.append("\n");
		sb.append(StringUtils.replace(String.valueOf(obj), ".\n", ". \n"));
		sb.append("\n");
		sb.append(".\n");
		return sb.toString();
	}
	
	private String rule(Filter filter) {
		StringBuilder sb = new StringBuilder();
		
		sb.append(ruleComment(filter.name));
		sb.append("\n");
		sb.append(ruleBody(filter));
		sb.append("\n");
		
		return sb.toString();
	}
	
	private String ruleComment(String name) {
		return "# rule:[" + StringUtils.replace(name, "#", "_") + "]";
	}
	
	private String ruleBody(Filter rule) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("if");
		sb.append(" ");
		if (EnumUtils.equals(rule.match, SieveMatch.ALL_MESSAGES)) {
			sb.append("true");
		} else {
			if (EnumUtils.equals(rule.match, SieveMatch.ALL)) {
				sb.append("allof");
			} else if (EnumUtils.equals(rule.match, SieveMatch.ANY)) {
				sb.append("anyof");
			} else {
				throw new RuntimeException();
			}
			sb.append(" ");
			sb.append("(");
			sb.append(filterConditions(rule.rules));
			sb.append(")");
		}
		sb.append("\n");
		sb.append("{");
		sb.append("\n");
		sb.append(ruleActions(rule.actions));
		sb.append("}");
		sb.append("\n");
		
		return sb.toString();
	}
	
	private String filterConditions(ArrayList<SieveRule> rules) {
		StringBuilder sb = new StringBuilder();
		
		int i = 0;
		for(SieveRule rule : rules) {
			i++;
			sb.append(filterCondition(rule));
			if (i != rules.size()) {
				sb.append(", ");
			}
		}
		
		return sb.toString();
	}
	
	private String filterCondition(SieveRule rule) {
		final SieveRuleField field = rule.getField();
		final SieveRuleOperator operator = rule.getOperator();
		if (EnumUtils.equals(field, SieveRuleField.SUBJECT)) {
			return new StringBuilder()
				.append(isNotCondition(operator) ? "not " : "")
				.append("header")
				.append(" ")
				.append(conditionOperator(operator))
				.append(" ")
				.append("[\"subject\",\"Subject\",\"SUBJECT\"]")
				.append(" ")
				.append(asConditionValue(operator, rule.getValue()))
				.toString();
			
		} else if (EnumUtils.equals(field, SieveRuleField.FROM)) {
			return new StringBuilder()
				.append(isNotCondition(operator) ? "not " : "")
				.append("header")
				.append(" ")
				.append(conditionOperator(operator))
				.append(" ")
				.append("[\"from\",\"From\",\"FROM\"]")
				.append(" ")
				.append(asConditionValue(operator, rule.getValue()))
				.toString();
			
		} else if (EnumUtils.equals(field, SieveRuleField.TO)) {
			return new StringBuilder()
				.append(isNotCondition(operator) ? "not " : "")
				.append("header")
				.append(" ")
				.append(conditionOperator(operator))
				.append(" ")
				.append("[\"to\",\"To\",\"TO\"]")
				.append(" ")
				.append(asConditionValue(operator, rule.getValue()))
				.toString();
			
		} else if (EnumUtils.equals(field, SieveRuleField.CC)) {
			return new StringBuilder()
				.append(isNotCondition(operator) ? "not " : "")
				.append("header")
				.append(" ")
				.append(conditionOperator(operator))
				.append(" ")
				.append("[\"cc\",\"Cc\",\"CC\"]")
				.append(" ")
				.append(asConditionValue(operator, rule.getValue()))
				.toString();
			
		} else if (EnumUtils.equals(field, SieveRuleField.TO_OR_CC)) {
			return new StringBuilder()
				.append(isNotCondition(operator) ? "not " : "")
				.append("header")
				.append(" ")
				.append(conditionOperator(operator))
				.append(" ")
				.append("[\"to\",\"To\",\"TO\",\"cc\",\"Cc\",\"CC\"]")
				.append(" ")
				.append(asConditionValue(operator, rule.getValue()))
				.toString();
			
		} else if (EnumUtils.equals(field, SieveRuleField.BODY)) {
			requires.add(REQUIRE_BODY);
			return new StringBuilder()
				.append(isNotCondition(operator) ? "not " : "")
				.append("body")
				.append(" ")
				.append(":text")
				.append(" ")
				.append(conditionOperator(operator))
				.append(" ")
				.append(asConditionValue(operator, rule.getValue()))
				.toString();
			
		} else if (EnumUtils.equals(field, SieveRuleField.SIZE)) {
			return new StringBuilder()
				.append("size")
				.append(" ")
				.append(conditionOperator(operator))
				.append(" ")
				.append(rule.getValue())
				.toString();
			
		} else if (EnumUtils.equals(field, SieveRuleField.HEADER)) {
			return new StringBuilder()
				.append(isNotCondition(operator) ? "not " : "")
				.append("header")
				.append(" ")
				.append(conditionOperator(operator))
				.append(" ")
				.append(asConditionArgument(rule.getArgument()))
				.append(" ")
				.append(asConditionValue(operator, rule.getValue()))
				.toString();
			
		} else {
			throw new RuntimeException();
		}
	}
	
	private String asConditionArgument(String value) {
		return printQuotedValue(value);
	}
	
	private String asConditionValue(SieveRuleOperator operator, String value) {
		if (EnumUtils.equals(operator, SieveRuleOperator.EQUAL_MULTI) 
				|| EnumUtils.equals(operator, SieveRuleOperator.CONTAINS_MULTI)) {
			String[] tokens = StringUtils.split(value, ",");	
			for(int i=0; i<tokens.length; i++) {
				tokens[i] = StringUtils.trim(tokens[i]);
				tokens[i] = StringUtils.removeStart(tokens[i], "\"");
				tokens[i] = StringUtils.removeEnd(tokens[i], "\"");
			}
			return printQuotedArray(tokens);
		} else {
			return printQuotedValue(value);
		}
	}
	
	private boolean isNotCondition(SieveRuleOperator operator) {
		return EnumUtils.equals(operator, SieveRuleOperator.NOT_EQUAL) || EnumUtils.equals(operator, SieveRuleOperator.NOT_CONTAINS) || EnumUtils.equals(operator, SieveRuleOperator.NOT_MATCHES);
	}
	
	private String conditionOperator(SieveRuleOperator operator) {
		if (EnumUtils.equals(operator, SieveRuleOperator.EQUAL) || EnumUtils.equals(operator, SieveRuleOperator.NOT_EQUAL)) {
			return ":is";
		} else if (EnumUtils.equals(operator, SieveRuleOperator.EQUAL_MULTI)) {
			return ":is";
		} else if (EnumUtils.equals(operator, SieveRuleOperator.CONTAINS) || EnumUtils.equals(operator, SieveRuleOperator.NOT_CONTAINS)) {
			return ":comparator \"i;ascii-casemap\" :contains";
		} else if (EnumUtils.equals(operator, SieveRuleOperator.CONTAINS_MULTI)) {
			return ":comparator \"i;ascii-casemap\" :contains";
		} else if (EnumUtils.equals(operator, SieveRuleOperator.MATCHES) || EnumUtils.equals(operator, SieveRuleOperator.NOT_MATCHES)) {
			return ":comparator \"i;ascii-casemap\" :matches";
		} else if (EnumUtils.equals(operator, SieveRuleOperator.LOWER_THAN)) {
			return ":under";
		} else if (EnumUtils.equals(operator, SieveRuleOperator.GREATER_THAN)) {
			return ":over";
		} else {
			throw new RuntimeException();
		}
	}
	
	private String ruleActions(ArrayList<SieveAction> actions) {
		StringBuilder sb = new StringBuilder();
		
		for(SieveAction action : actions) {
			sb.append("        ");
			sb.append(ruleAction(action));
			sb.append(";\n");
		}
		
		return sb.toString();
	}
	
	private String ruleAction(SieveAction action) {
		final SieveActionMethod method = action.getMethod();
		if (EnumUtils.equals(method, SieveActionMethod.KEEP)) {
			return "keep";
			
		} else if (EnumUtils.equals(method, SieveActionMethod.DISCARD)) {
			return "discard";
			
		} else if (EnumUtils.equals(method, SieveActionMethod.REJECT)) {
			requires.add(REQUIRE_REJECT);
			return new StringBuilder()
				.append("reject")
				.append(" ")
				.append(asValueArgument(action.getArgument()))
				.toString();
			
		} else if (EnumUtils.equals(method, SieveActionMethod.REDIRECT)) {
			requires.add(REQUIRE_COPY);
			return new StringBuilder()
				.append("redirect")
				.append(" ")
				.append(":copy")
				.append(" ")
				.append(asValueArgument(action.getArgument()))
				.toString();
			
		} else if (EnumUtils.equals(method, SieveActionMethod.FILE_INTO)) {
			requires.add(REQUIRE_FILEINTO);
			return new StringBuilder()
				.append("fileinto")
				.append(" ")
				.append(asValueArgument(action.getArgument()))
				.toString();
			
		} else if (EnumUtils.equals(method, SieveActionMethod.STOP)) {
			return "stop";
			
		} else if (EnumUtils.equals(method, SieveActionMethod.ADD_FLAG)) {
			requires.add(REQUIRE_IMAP4FLAGS);
			return flagActionCommand(action.getArgument());
			
		} else {
			throw new RuntimeException();
		}
	}
	
	private String flagActionCommand(String flagArgument) {
		SieveActionArgFlag flag = EnumUtils.forSerializedName(flagArgument, SieveActionArgFlag.class);
		if (flag == null) throw new RuntimeException();
		
		if (EnumUtils.equals(flag, SieveActionArgFlag.JUNK)) {
			return "addflag " + asFlagValueArgument(flagArgument);
			
		} else if (EnumUtils.equals(flag, SieveActionArgFlag.NOT_JUNK)) {
			return "removeflag " + asFlagValueArgument(flagArgument);
			
		} else {
			throw new RuntimeException();
		}
	}
	
	private String asValueArgument(String argument) {
		return printQuotedValue(argument);
	}
	
	private String asFlagValueArgument(String argument) {
		SieveActionArgFlag flag = EnumUtils.forSerializedName(argument, SieveActionArgFlag.class);
		if (flag == null) throw new RuntimeException();
		
		if (EnumUtils.equals(flag, SieveActionArgFlag.SEEN)) {
			return asValueArgument("\\Seen");
			
		} else if (EnumUtils.equals(flag, SieveActionArgFlag.DELETED)) {
			return asValueArgument("\\Deleted");
			
		} else if (EnumUtils.equals(flag, SieveActionArgFlag.ANSWERED)) {
			return asValueArgument("\\Answered");
			
		} else if (EnumUtils.equals(flag, SieveActionArgFlag.FLAGGED)) {
			return asValueArgument("\\Flagged");
			
		} else if (EnumUtils.equals(flag, SieveActionArgFlag.JUNK) || EnumUtils.equals(flag, SieveActionArgFlag.NOT_JUNK)) {
			return printArray(asValueArgument("$Junk"), asValueArgument("Junk"), asValueArgument("JunkRecorded"));
			
		} else {
			throw new RuntimeException();
		}
	}
	
	private String printQuotedArray(String... elements) {
		String[] arr = new String[elements.length];
		for(int i=0; i<elements.length; i++) {
			arr[i] = printQuotedValue(elements[i]);
		}
		return printArray(arr);
	}
	
	private String printArray(String... elements) {
		return "[" + StringUtils.join(elements, ",") + "]";
	}
	
	private String printQuotedValue(Object obj) {
		// A quoted string starts and ends with a single double quote (the <">
		// character, ASCII 34).  A backslash ("\", ASCII 92) inside of a quoted
		// string is followed by either another backslash or a double quote.
		// This two-character sequence represents a single backslash or double-
		// quote within the string, respectively.
		String s = String.valueOf(obj);
		s = StringUtils.replace(s, "\\", "\\\\");
		s = StringUtils.replace(s, "\"", "\\\"");
		return "\"" + s + "\"";
	}
	
	private String printValue(Object obj) {
		return String.valueOf(obj);
	}
	
	private class Filter {
		public String name;
		public SieveMatch match;
		public ArrayList<SieveRule> rules;
		public ArrayList<SieveAction> actions;
		
		public Filter(String name, SieveMatch match, ArrayList<SieveRule> rules, ArrayList<SieveAction> actions) {
			this.name = name;
			this.match = match;
			this.rules = rules;
			this.actions = actions;
		}
	}
}
