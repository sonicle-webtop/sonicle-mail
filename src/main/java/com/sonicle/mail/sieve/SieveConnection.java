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
import java.net.*;
import java.nio.CharBuffer;
import java.util.ArrayList;

public class SieveConnection {

  public static boolean debug=false;

  BufferedReader reader=null;
  PrintWriter writer=null;
  Socket socket=null;

  public SieveConnection(String hostname, int port) throws UnknownHostException, IOException {
    socket=new Socket(hostname,port);
    reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
    writer=new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8"));
    getResponse();
  }

  public SieveResponse send(String s) throws IOException {
    if (debug) System.out.println("<< "+s);
    writer.println(s);
    writer.flush();
    return getResponse();
  }

  private SieveResponse getResponse() throws IOException {
    String line=null;
    boolean b=false;
    ArrayList<String> lines=new ArrayList<String>();
    while((line=reader.readLine())!=null) {
      if (debug) System.out.println(">> "+line);
      if (line.startsWith("OK") || line.trim().length()==0) {
        b=true;
        break;
      }
      else if (line.startsWith("NO")) {
		StringBuffer sb=new StringBuffer();
		while(reader.ready()) lines.add(reader.readLine());
        b=false;
        break;
      }
      lines.add(line);
    }
	SieveResponse sr=new SieveResponse(b,lines.toArray(new String[lines.size()]));
    return sr;
  }

  public void close() throws IOException {
    if (socket!=null) socket.close();
  }
}
