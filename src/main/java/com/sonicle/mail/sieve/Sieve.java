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
  String spamFolder=null;
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
  
  public void setSpamFolder(String spamFolder) {
	  this.spamFolder=spamFolder;
  }

  public SieveScript[] getScripts() throws java.net.UnknownHostException, IOException {
    SieveConnection sc=new SieveConnection(hostname,port);
    SieveScript ss[]=null;
    if (authenticate(sc)) {
      SieveResponse sr=sc.send("LISTSCRIPTS");
	  String resp[]=sr.lines;
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
	  logout(sc);
    }
    close(sc);
    return ss;
  }

  public boolean putScript(String name, StringBuffer script, boolean activate) throws SieveException, java.net.UnknownHostException, IOException {
    //SieveConnection.debug=true;
    SieveConnection sc=new SieveConnection(hostname,port);
    SieveScript ss[]=null;
    boolean retval=false;
    if (authenticate(sc)) {
      String command="PUTSCRIPT \""+name+"\" {"+(script.length())+"+}\r\n"+script;
      SieveResponse sr = sc.send(command);
      retval=sr.status;
      if (retval && activate) {
        sr=sc.send("SETACTIVE \""+name+"\"");
        retval=sr.status;
		logout(sc);
      }
	  else {
		  String msg=sr.getMessage();
		  sr=logout(sc);
		  close(sc);
		  throw new SieveException(msg);
	  }
    }
	close(sc);
    return retval;
  }

  private boolean authenticate(SieveConnection sc) throws IOException {
	String authcmd="AUTHENTICATE \"PLAIN\" \""+encoder.encode((((char)0)+username+((char)0)+password).getBytes())+"\"";
	SieveResponse resp= sc.send(authcmd);
	return resp.status;
  }

  private SieveResponse logout(SieveConnection sc) throws IOException {
    return sc.send("LOGOUT");
  }
  
  private void close(SieveConnection sc) {
	  try { sc.close(); } catch(Exception exc) {}
  }

  public void saveScript(MailRules filters, boolean activate) throws SieveException,java.net.UnknownHostException, IOException {
    SieveScriptGenerator ssg=new SieveScriptGenerator();
	StringBuffer sievescript=ssg.generate(filters,spamFolder);

    putScript("webtop",sievescript,activate);
  }
  
  public String getScript(String name) throws SieveException, java.net.UnknownHostException, IOException  {
	  StringBuffer sb=new StringBuffer();
	  SieveConnection sc=new SieveConnection(hostname,port);
	  if (authenticate(sc)) {
		  SieveResponse resp=sc.send("GETSCRIPT \""+name+"\"");
		  if (resp.status) {
			  for(String line: resp.lines) {
				  sb.append(line);
				  sb.append('\n');
			  }
		  }
		  logout(sc);
	  }
	  close(sc);
	  return sb.toString();
  }

}
