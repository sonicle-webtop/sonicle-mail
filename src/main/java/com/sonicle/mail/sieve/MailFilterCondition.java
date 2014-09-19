/*
* WebTop Groupware is a bundle of WebTop Services developed by Sonicle S.r.l.
* Copyright (C) 2011 Sonicle S.r.l.
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

import java.util.*;

public class MailFilterCondition {
  public static final int ANY=1;
  public static final int ALL=2;

  public static final int EQUALTO=0;
  public static final int LESSTHAN=1;
  public static final int GREATERTHAN=2;
  public static final int CONTAINS=3;

  public static final String comparisons[]= {
      "equalto",
      "lessthan",
      "greaterthan",
      "contains"
  };

  int condition=ANY;
  String field=null;
  ArrayList<String> values=new ArrayList<String>();
  int comparison=CONTAINS;

  public MailFilterCondition() {
  }

  public int getCondition() {
    return condition;
  }

  public void setCondition(int c) {
    condition=c;
  }

  public String getStringComparison() {
    return comparisons[comparison];
  }

  public String getField() {
    return field;
  }

  public void setField(String f) {
    field=f;
  }

  public ArrayList<String> getValues() {
    return values;
  }

  public void addValue(String value) {
    values.add(value);
  }

  public void setValues(String v, boolean spaces) {
    String regex="[\\s,\\,\\;]";
    if (!spaces) regex="\\z";
    if (v.indexOf('"')>=0) regex="\"";
    String tokens[]=v.split(regex);
    for(int i=0;i<tokens.length;++i) {
      String ttoken=tokens[i].trim();
      if (ttoken.length()>0) {
        if (ttoken.charAt(0)==',' || ttoken.charAt(0)==';')
          ttoken=ttoken.substring(1);
        if (ttoken.length()>0) values.add(ttoken);
      }
    }
  }

  public int getComparison() {
    return comparison;
  }

  public void setComparison(int c) {
    comparison=c;
  }

}
