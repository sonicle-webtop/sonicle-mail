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

import java.io.*;
import sun.misc.BASE64Encoder;



public class Sieve {

  String username=null;
  String password=null;
  String hostname=null;
  int port=2000;
  BASE64Encoder encoder=new BASE64Encoder();

  public Sieve(String hostname, String username, String password) {
    this(hostname,2000,username,password);
  }

  public Sieve(String hostname, int port, String username, String password) {
    this.hostname=hostname;
    this.port=port;
    this.username=username;
    this.password=password;
  }

  public void setUsername(String username) {
    this.username=username;
  }

  public String getUsername() {
    return username;
  }

  public void setPassword(String password) {
    this.password=password;
  }

  public String getPassword() {
    return password;
  }

  public void setHostname(String hostname) {
    this.hostname=hostname;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port=port;
  }

  public SieveScript[] getScripts() throws java.net.UnknownHostException, IOException {
    SieveConnection sc=new SieveConnection(hostname,port);
    SieveScript ss[]=null;
    if (authenticate(sc)) {
      String resp[]=sc.send("LISTSCRIPTS");
      ss=new SieveScript[resp.length];
      for(int i=0;i<resp.length;++i) {
        String name=null;
        boolean active=false;
        int x=resp[i].indexOf(" ");
        if (x>=0) {
          name=resp[i].substring(0,x).trim();
          if (resp[i].substring(x).trim().toUpperCase().equals("ACTIVE")) active=true;
        } else name=resp[i].trim();
        ss[i]=new SieveScript(name,active);
      }
    }
    logout(sc);
    return ss;
  }

  public boolean putScript(String name, StringBuffer script, boolean activate) throws java.net.UnknownHostException, IOException {
    //SieveConnection.debug=true;
    SieveConnection sc=new SieveConnection(hostname,port);
    SieveScript ss[]=null;
    boolean retval=false;
    if (authenticate(sc)) {
      String command="PUTSCRIPT \""+name+"\" {"+(script.length())+"+}\r\n"+script;
      String resp[] = sc.send(command);
      retval=(resp!=null);
      if (retval && activate) {
        resp=sc.send("SETACTIVE \""+name+"\"");
        retval=(resp!=null);
      }
      logout(sc);
    }
    return retval;
  }

  private boolean authenticate(SieveConnection sc) throws IOException {
    String[] resp= sc.send("AUTHENTICATE \"PLAIN\" \""+encoder.encode((((char)0)+username+((char)0)+password).getBytes())+"\"");
    return resp!=null;
  }

  private void logout(SieveConnection sc) throws IOException {
    sc.send("LOGOUT");
  }

  public void saveScript(MailFilters filters, boolean activate) throws Exception {
    SieveScriptGenerator ssg=new SieveScriptGenerator();
    StringBuffer sievescript=ssg.generate(filters);

    putScript("webtop",sievescript,activate);
  }

}
