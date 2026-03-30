/*
 * sonicle-mail is is a helper library developed by Sonicle S.r.l.
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
 * Sonicle logo and Sonicle copyright notice. If the display of the logo is not
 * reasonably feasible for technical reasons, the Appropriate Legal Notices must
 * display the words "Copyright (C) 2014 Sonicle S.r.l.".
 */
package com.sonicle.mail.parser;

import com.sonicle.commons.EnumUtils;
import com.sonicle.commons.MailUtils;
import com.sonicle.mail.MimeUtils;
import com.sonicle.mail.email.CalendarMethod;
import com.sonicle.mail.imap.SonicleIMAPMessage;
import com.sonicle.mail.tnef.internet.TnefMultipart;
import com.sonicle.mail.tnef.internet.TnefMultipartDataSource;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimePart;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.qualitycheck.Check;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 *
 * @author malbinola
 */
public class MimeMessageParser {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(MimeMessageParser.class);
	
	private boolean processDisplayParts = false;
	private boolean pdpBalanceTags = false;
	private DisplayPartEvaluator dpe = null;
	
	public MimeMessageParser withProcessDisplayParts(boolean balanceTags, DisplayPartEvaluator dpe) {
		this.processDisplayParts = true;
		this.pdpBalanceTags = balanceTags;
		this.dpe = dpe;
		return this;
	}
	
	public ParsedMimeMessageComponents parse(final MimeMessage message, final boolean isPECAccount) {
		ParsedMimeMessageComponents parsed = new ParsedMimeMessageComponents(message, isPECAccount);
		
		parseMimePartTree(parsed, message, 0);
		if (parsed.displayParts.isEmpty() && !parsed.attachmentParts.isEmpty()) {
			Part part0 = parsed.attachmentParts.get(0);
			if (isMimeType(part0, "text/plain") || isMimeType(part0, "text/html") || isMimeType(part0, "message/delivery-status")) {
				parsed.appendDisplayPart(part0, 0);
			}
		}
		
		if (processDisplayParts) {
			try {
				processDisplayParts(parsed, message);
			} catch(MessagingException|IOException exc) {
				LOGGER.error("Error processing display parts", exc);
			}
		}
		
		return parsed;
	}
	
	private void parseMimePartTree(final ParsedMimeMessageComponents parsed, final Part currentPart, final int depth) {
		final String currentDisposition = parseDisposition(currentPart);
		
		if (isMimeType(currentPart, "text/plain") 
				|| isMimeType(currentPart, "text/html")) {
			if (!Part.ATTACHMENT.equalsIgnoreCase(currentDisposition)) {
				parsed.appendDisplayPart(currentPart, depth);
			} else {
				parsed.appendAttachmentPart(currentPart, depth);
			}
			
		} else if (isMimeType(currentPart, "message/delivery-status") 
				|| isMimeType(currentPart, "message/disposition-notification")) {
			if (!Part.ATTACHMENT.equalsIgnoreCase(currentDisposition)) {
				parsed.appendDisplayPart(currentPart, depth);
			} else {
				parsed.appendAttachmentPart(currentPart, depth);
			}
			
		} else if (isMimeType(currentPart, "text/calendar") || isMimeType(currentPart, "application/ics")) {
			parsed.appendDisplayPart(currentPart, depth);
			if (isMimeType(currentPart, "text/calendar") && !parsed.hasCalendar() && !Part.ATTACHMENT.equalsIgnoreCase(currentDisposition)) {
				try {
					parsed.addCalendar(parseCalendarMethod(currentPart), parseCalendarContent(currentPart));
				} catch(MimeMessageParseException exc) { }
			}
			
		} else if (isMimeType(currentPart, "message/rfc822")) {
			parsed.appendDisplayPart(currentPart, depth);
			parseMimePartTree(parsed, (Message)parseContent(currentPart), depth+1);
			
		} else if (isMimeType(currentPart, "application/ms-tnef")) {
			try {
				TnefMultipart tnetMultipart = new TnefMultipart(new TnefMultipartDataSource((MimePart)currentPart));
				for (int j = 0; j < countBodyParts(tnetMultipart); ++j) {
					Part bodyPart = getBodyPartAtIndex(tnetMultipart, j);
					parseMimePartTree(parsed, bodyPart, depth);
				}
			} catch (Throwable ex) {
				//Service.logger.error("Exception", exc);
				parsed.appendUnknownPart(currentPart, depth);
				parsed.appendAttachmentPart(currentPart, depth);
			}

		} else if (isMimeType(currentPart, "application/pkcs7-signature") 
				|| isMimeType(currentPart, "application/x-pkcs7-signature")) {
			parsed.appendUnknownPart(currentPart, depth);
			parsed.appendAttachmentPart(currentPart, depth);
			
		} else if (isMimeType(currentPart, "multipart/alternative")) {
			final Part altPart = parseAndFindAlternativeDisplayPart(parsed, parseContent(currentPart), depth);
			if (altPart != null) {
				parsed.appendDisplayPart(altPart, depth);
			}
			
		} else if (isMimeType(currentPart, "multipart/*")) {
			final Multipart mp = parseContent(currentPart);
			for (int i = 0; i < countBodyParts(mp); ++i) {
				final Part mpPart = getBodyPartAtIndex(mp, i);
				final String mpDisposition = parseDisposition(mpPart);
				
				if (isMimeType(mpPart, "text/plain") 
						|| isMimeType(mpPart, "text/html")) {
					if (!Part.ATTACHMENT.equalsIgnoreCase(mpDisposition)) {// uguale a sopra
						parsed.appendDisplayPart(mpPart, depth);
					} else {
						parsed.appendAttachmentPart(mpPart, depth);
					}

				} else if (isMimeType(mpPart, "message/delivery-status") 
						|| isMimeType(mpPart, "message/disposition-notification")) {// uguale a sopra
					if (!Part.ATTACHMENT.equalsIgnoreCase(mpDisposition)) {
						parsed.appendDisplayPart(mpPart, depth);
					} else {
						parsed.appendAttachmentPart(mpPart, depth);
					}
				
				} else if (isMimeType(mpPart, "text/calendar") || isMimeType(mpPart, "application/ics")) {// uguale a sopra
					parsed.appendDisplayPart(mpPart, depth);
					if (isMimeType(mpPart, "text/calendar") && !parsed.hasCalendar() && !Part.ATTACHMENT.equalsIgnoreCase(mpDisposition)) {
						try {
							parsed.addCalendar(parseCalendarMethod(mpPart), parseCalendarContent(mpPart));
						} catch(MimeMessageParseException exc) { }
					}
					
				} else if (isMimeType(mpPart, "message/rfc822")) {// rfc822 diverso !!!
					int newDepth = depth;
					if (!Part.ATTACHMENT.equalsIgnoreCase(mpDisposition) && !parsed.isPECAccount) {
						parsed.appendDisplayPart(mpPart, depth);
					} else {
						parsed.appendAttachmentPart(mpPart, depth);
						++newDepth;
					}
					parseMimePartTree(parsed, (Message)parseContent(mpPart), newDepth);
					
				} else if (isMimeType(mpPart, "application/ms-tnef")) {// uguale a sopra
					try {
						TnefMultipart tnetMultipart = new TnefMultipart(new TnefMultipartDataSource((MimePart)mpPart));
						for (int j = 0; j < countBodyParts(tnetMultipart); ++j) {
							final Part bodyPart = getBodyPartAtIndex(tnetMultipart, j);
							parseMimePartTree(parsed, bodyPart, depth);
						}
					} catch (Throwable ex) {
						//Service.logger.error("Exception", exc);
						parsed.appendUnknownPart(mpPart, depth);
						parsed.appendAttachmentPart(mpPart, depth);
					}
					
				}  else if (isMimeType(mpPart, "application/pkcs7-signature") 
						|| isMimeType(mpPart, "application/x-pkcs7-signature")) {// uguale a sopra
					parsed.appendUnknownPart(mpPart, depth);
					parsed.appendAttachmentPart(mpPart, depth);
				
				} else if (isMimeType(mpPart, "multipart/*")) {// multipart diverso !!!
					parseMimePartTree(parsed, mpPart, depth);
					
				} else if (isMimeType(mpPart, "multipart/alternative")) {
					final Part altPart = parseAndFindAlternativeDisplayPart(parsed, parseContent(currentPart), depth);
					final String altDisposition = parseDisposition(altPart);
					
					if (altPart != null) {
						if (isMimeType(altPart, "text/calendar") || isMimeType(altPart, "application/ics")) {
							if (!Part.ATTACHMENT.equalsIgnoreCase(altDisposition)) {
								parsed.appendDisplayPart(altPart, depth);
							} else {
								// This replicate logic inside orignal addAttachmentPart 
								// whether the part is added only if flag is false!
								if (!parsed.hasICalAttachment) {
									parsed.hasICalAttachment = true;
									parsed.appendAttachmentPart(altPart, depth);
								}
							}
							if (isMimeType(altPart, "text/calendar") && !parsed.hasCalendar() && !Part.ATTACHMENT.equalsIgnoreCase(altDisposition)) {
								try {
									parsed.addCalendar(parseCalendarMethod(altPart), parseCalendarContent(altPart));
								} catch(MimeMessageParseException exc) { }
							}
						}
					}
					
				} else {// else diverso !!!
					parsed.appendUnknownPart(mpPart, depth);
					parsed.appendAttachmentPart(mpPart, depth);
					evaluateCidPart(parsed, mpPart, depth);
				}
			}
			
		} else {
			parsed.appendUnknownPart(currentPart, depth);
			parsed.appendAttachmentPart(currentPart, depth);
		}
	}
	
	private Part parseAndFindAlternativeDisplayPart(final ParsedMimeMessageComponents parsed, final Multipart currentPart, final int depth) {
		Part displayPart = null;
		boolean htmlFound = false;
		for (int j = 0; j < countBodyParts(currentPart); ++j) {
			final Part altPart = getBodyPartAtIndex(currentPart, j);
			final String altDisposition = parseDisposition(altPart);
			
			if (isMimeType(altPart, "multipart/*")) {
				if (isMimeType(altPart, "multipart/related")) {
					displayPart = parseAndFindAlternativeDisplayPart(parsed, parseContent(altPart), depth); 
					if (isMimeType(altPart, "text/html")) {
						htmlFound = true;
					}
				} else {
					parseMimePartTree(parsed, altPart, depth);
				}
			} else if (isMimeType(altPart, "text/html")) {
				displayPart = altPart;
				htmlFound = true;
			} else if (isMimeType(altPart, "text/plain")) {
				if (!htmlFound) {
					displayPart = altPart;
				}
			} else if (isMimeType(altPart, "text/calendar")) {
				// Usually calendar parts are defined into alternative set
				if (/*isMimeType(bodyPart, "text/calendar") &&*/ !parsed.hasCalendar() && !Part.ATTACHMENT.equalsIgnoreCase(altDisposition)) {
					try {
						parsed.addCalendar(parseCalendarMethod(altPart), parseCalendarContent(altPart));
					} catch(MimeMessageParseException exc) { }
				}
				
				// This replicate logic inside orignal addAttachmentPart 
				// whether the part is added only if flag is false!
				if (!parsed.hasICalAttachment) {
					parsed.hasICalAttachment = true;
					parsed.appendAttachmentPart(altPart, depth);
				}
				parsed.appendDisplayPart(altPart, depth);
				if (!htmlFound) {
					displayPart = altPart;	
				}
			} else {
				parsed.appendUnknownPart(altPart, depth);
				parsed.appendAttachmentPart(altPart, depth);
				evaluateCidPart(parsed, altPart, depth);
			}
		}
		return displayPart;
	}
	
	private void evaluateCidPart(final ParsedMimeMessageComponents parsed, final Part part, final int depth) {
		// Look for a possible CID
		String cid = parseContentID(part);
		String filename = (cid != null) ? cid : parseFileName(part, false);		
		if (filename != null) {
			if (!parsed.hasCidPart(filename)) parsed.appendCidPart(filename, part, depth);
		}
		// Look for a possible URL copy
		String url = parseContentLocation(part);
		if (url != null) {
			if (!parsed.hasUrlPart(filename)) parsed.appendUrlPart(url, part, depth);
		}
	}
	
	public static class DisplayPartEvaluator {
		
		public void evaluateCalendar(ParsedMimeMessageComponents parsed, Part part, InputStream istream, String charset) throws IOException, MessagingException { }
		
		public void evaluatePlainText(ParsedMimeMessageComponents parsed, Part part, InputStream istream, String charset) throws IOException, MessagingException { }
		
		public void evaluateMessage(ParsedMimeMessageComponents parsed, Part part, InputStream istream, String charset) throws IOException, MessagingException { }
	}
	
    private void processDisplayParts(ParsedMimeMessageComponents parsed, MimeMessage m) throws MessagingException, IOException {
		long msguid = 0;
		
		if (m instanceof SonicleIMAPMessage) msguid = ((SonicleIMAPMessage) m).getUID();
		
		//first cycle parts to get a possible default charset
		String defaultCharset=null;
		for(Part dispPart: parsed.getDisplayParts()) {
			//Use workaround for NethServer installation:
			defaultCharset=MailUtils.getCharsetOrNull(dispPart);
			if (defaultCharset != null) break;
		}
		
		for(Part dispPart: parsed.getDisplayParts()) {
		  java.io.InputStream istream=null;
		  
		  String charset=null;
		  if (defaultCharset==null)
			  //Use workaround for NethServer installation:
			  charset=MailUtils.getCharsetOrDefault(dispPart);
		  else {
			  //Use workaround for NethServer installation:
			  charset=MailUtils.getCharsetOrNull(dispPart);
			  if (charset==null) charset=defaultCharset;
		  }
		  
		  if (dispPart.isMimeType("text/plain")||dispPart.isMimeType("text/html")||dispPart.isMimeType("message/delivery-status")||dispPart.isMimeType("message/disposition-notification")||dispPart.isMimeType("text/calendar")||dispPart.isMimeType("application/ics")) {
			  try {
				if (dispPart instanceof jakarta.mail.internet.MimeMessage) {
				  jakarta.mail.internet.MimeMessage mm=(jakarta.mail.internet.MimeMessage)dispPart;
				  istream=mm.getInputStream();
				} else if (dispPart instanceof jakarta.mail.internet.MimeBodyPart) {
				  jakarta.mail.internet.MimeBodyPart mm=(jakarta.mail.internet.MimeBodyPart)dispPart;
				  istream=mm.getInputStream();
				}
			  } catch(Exception exc) { //unhandled format, get Raw data
				if (dispPart instanceof jakarta.mail.internet.MimeMessage) {
				  jakarta.mail.internet.MimeMessage mm=(jakarta.mail.internet.MimeMessage)dispPart;
				  istream=mm.getRawInputStream();
				} else if (dispPart instanceof jakarta.mail.internet.MimeBodyPart) {
				  jakarta.mail.internet.MimeBodyPart mm=(jakarta.mail.internet.MimeBodyPart)dispPart;
				  istream=mm.getRawInputStream();
				}
			  }


			  if (istream==null) throw new IOException("Unknown message class "+dispPart.getClass().getName());


			  StringBuffer xhtml=new StringBuffer();
			  if (dispPart.isMimeType("text/html")) {
				  Object tlock=new Object();
				  HTMLMailParserThread parserThread=null;
				  parserThread=new HTMLMailParserThread(tlock, istream, charset, pdpBalanceTags);
				  try {
					  java.io.BufferedReader breader=startHTMLMailParser(parserThread, new SaxHTMLMailEvaluator(parsed) {
						  @Override
						  public void evaluateCid(String cidName) {
							parsed.addReferencedCid(cidName);
						  }

						  @Override
						  public String evaluateUrl(String url, String baseUrl) {
							boolean islocal = parsed.containsUrlPart(url);
							if(!islocal) { //must use remote url: compose if necessary
							  if(baseUrl==null) {
								return url;
							  }
							  String str=null;
							  int len=url.length();
							  if (len>3) str=url.substring(0, 4).toLowerCase();
							  if(str!=null && (str.startsWith("http")||str.startsWith("ftp:"))) {
								return url;
							  }
							  if(len>0 && url.charAt(0)=='/') {
								url=url.substring(1);
							  }
							  return baseUrl+url;
							}
							parsed.removeUnknownPart(parsed.getUrlPart(url));
							return url;
						  }
						  
					  }, false);
					  char chars[]=new char[8192];
					  int n=0;
					  while((n=breader.read(chars))>=0) {
					   if (n>0) xhtml.append(chars,0,n);
					  }
				  } catch(Exception exc) {
					  LOGGER.error("Exception",exc);
					  parserThread.notifyParserEndOfRead();
				  }
				  parserThread.notifyParserEndOfRead();

				  parsed.appendProcessedHTMLPart(xhtml.toString(),parserThread.getHrefs());
				  
			  } else if (dispPart.isMimeType("text/calendar")||dispPart.isMimeType("application/ics")) {
				  if (dispPart.getContentType().contains("method=") && dpe != null) {
					  dpe.evaluateCalendar(parsed, dispPart, istream, charset);
				  } else {
					  parsed.appendAttachmentPart(dispPart,0);
				  }
			  } else {
				  if (dpe != null) dpe.evaluatePlainText(parsed, dispPart, istream, charset);
			  }
		  } else if (dispPart.isMimeType("message/*")) {
			  dpe.evaluateMessage(parsed, dispPart, istream, charset);
		  }

		}
	}
	
	class HTMLMailParserThread implements Runnable {

	  InputStream istream=null;
	  String charset=null;
	  Object threadLock=null;
	  SaxHTMLMailParser saxHTMLMailParser=null;
	  boolean balanceTags=true;

	  HTMLMailParserThread(Object tlock, InputStream istream, String charset, boolean balanceTags) {
		  this.threadLock=tlock;
		  this.istream=istream;
		  this.charset=charset;
		  this.balanceTags=balanceTags;
		  this.saxHTMLMailParser=new SaxHTMLMailParser();
	  }

	  public void initialize(SaxHTMLMailEvaluator evaluator, boolean justBody) throws SAXException {
		  saxHTMLMailParser.initialize(evaluator, justBody, true);        
	  }

	  public void run() {
		try {
			doHTMLMailParse(saxHTMLMailParser,istream,charset,balanceTags);
			synchronized(threadLock) {
				threadLock.wait(60000); //give up after one minute
			}
		} catch(Exception exc) {
			LOGGER.error("Exception",exc);
		}
	  }

	  public BufferedReader getParsedHTML() {
		  return saxHTMLMailParser.getParsedHTML();        
	  }

	  private void notifyParserEndOfRead() {
		  synchronized(threadLock) {
			threadLock.notifyAll();
		  }
		  saxHTMLMailParser.release();
	  }

	  public ArrayList<String> getHrefs() {
		  return saxHTMLMailParser.getHrefs();
	  }

	}

	private void doHTMLMailParse(SaxHTMLMailParser saxHTMLMailParser, InputStream istream, String charset, boolean balanceTags) throws SAXException, IOException {
	  HTMLInputStream hstream=new HTMLInputStream(istream);
	  XMLReader xmlparser=XMLReaderFactory.createXMLReader("org.cyberneko.html.parsers.SAXParser");
	  //XMLReader xmlparser=XMLReaderFactory.createXMLReader("net.sourceforge.htmlunit.cyberneko.parsers.SAXParser");
	  xmlparser.setProperty("http://xml.org/sax/properties/lexical-handler", saxHTMLMailParser);
	  xmlparser.setFeature("http://apache.org/xml/features/scanner/notify-char-refs", true);
	  //xmlparser.setFeature("http://cyberneko.org/html/features/balance-tags", balanceTags);
	  xmlparser.setContentHandler(saxHTMLMailParser);
	  xmlparser.setErrorHandler(saxHTMLMailParser);
	  while(!hstream.isRealEof()) {
		hstream.newDocument();
		InputStreamReader isr=null;
		try {
			isr=charset!=null?new InputStreamReader(hstream,charset):new InputStreamReader(hstream);
		} catch(java.io.UnsupportedEncodingException exc) {
			isr=new InputStreamReader(hstream);	
		}
		xmlparser.parse(new InputSource(isr));
	  }
	  saxHTMLMailParser.endOfFile();
	}
	
	private BufferedReader startHTMLMailParser(HTMLMailParserThread parserThread, SaxHTMLMailEvaluator evaluator, boolean justBody) throws SAXException {
	  Thread engine=new Thread(parserThread);
	  parserThread.initialize(evaluator, justBody);
	  engine.start();
	  return parserThread.getParsedHTML();
	}
	
	
	public static class ParsedMimeMessageComponents {

		public class HTMLPart {

			public String html;
			public ArrayList<String> hrefs;

			HTMLPart(String html) {
				this(html, new ArrayList<String>());
			}
			
			HTMLPart(String html, ArrayList<String> hrefs) {
				this.html=html;
				this.hrefs=hrefs;
			}
		}
		
		private MimeMessage originalMessage = null;
	
		private final boolean isPECAccount;
		private final HashMap<Part, Integer> partDepthMap = new HashMap<>();
		private final ArrayList<Part> displayParts = new ArrayList<>();
		private final ArrayList<Part> attachmentParts = new ArrayList<>();
		private final ArrayList<Part> unknownParts = new ArrayList<>();
		private final ArrayList<HTMLPart> processedHtmlParts=new ArrayList<>();
		private final HashMap<String, Part> cidParts = new LinkedHashMap<>();
		private final HashMap<String, Part> urlParts = new LinkedHashMap<>();
		private final ArrayList<String> referencedCids=new ArrayList<>();
		public boolean hasICalAttachment = false;
		private CalendarMethod calendarMethod = null;
		private String calendarContent = null;
		
		ParsedMimeMessageComponents(final boolean isPECAccount) {
			this.isPECAccount = isPECAccount;
		}
				
		ParsedMimeMessageComponents(final MimeMessage originalMessage, final boolean isPECAccount) {
			this.originalMessage = originalMessage;
			this.isPECAccount = isPECAccount;
		}
		
		public MimeMessage getOriginalMessage() {
			return originalMessage;
		}
				
		public HashMap<Part, Integer> getPartsDepthMap() {
			return partDepthMap;
		}
		
		public ArrayList<Part> getDisplayParts() {
			return displayParts;
		}
		
		public ArrayList<Part> getAttachmentParts() {
			return attachmentParts;
		}
		
		public String getContentType(int index) {
			String contentType = "";
			try {
				Part part = attachmentParts.get(index);
				if (part != null) contentType = part.getContentType();
			} catch(MessagingException exc) {
				
			}
			return contentType;
		}
		
		public ArrayList<Part> getUnknownParts() {
			return unknownParts;
		}
		
		public ArrayList<HTMLPart> getProcessedHTMLParts() {
			return processedHtmlParts;
		}
		
		public HashMap<String, Part> getCidParts() {
			return cidParts;
		}
		
		public String getContentType(String cidName) {
			String contentType = "";
			try {
				Part part = cidParts.get(cidName);
				if (part != null) contentType = part.getContentType();
			} catch(MessagingException exc) {
				
			}
			return contentType;
		}
		
		public HashMap<String, Part> getUrlParts() {
			return urlParts;
		}
		
		public CalendarMethod getCalendarMethod() {
			return calendarMethod;
		}
		
		public String getCalendarContent() {
			return calendarContent;
		}
		
		private void appendDisplayPart(Part part, int depth) {
			displayParts.add(part);
			partDepthMap.put(part, depth);
		}
		
		public void appendAttachmentPart(Part part, int depth) {
			attachmentParts.add(part);
			partDepthMap.put(part, depth);
		}
		
		public void appendUnknownPart(Part part, int depth) {
			unknownParts.add(part);
			partDepthMap.put(part, depth);
		}
		
		public void appendProcessedHTMLPart(String html, ArrayList<String> hrefs) {
			processedHtmlParts.add(new HTMLPart(html, hrefs));
		}
		
		public void appendProcessedHTMLPart(String html) {
			processedHtmlParts.add(new HTMLPart(html));
		}
		
		public void appendProcessedHTMLPart(int index, String html) {
			processedHtmlParts.add(index, new HTMLPart(html));
		}
		
		public void addReferencedCid(String name) {
			Part cidPart = getCidPart(name);
			if (cidPart != null) removeUnknownPart(cidPart);
			referencedCids.add(name);
		}
  
		private Part getCidPart(String name) {
			return cidParts.get(name);
		}
		
		private void removeUnknownPart(Part part) {
			unknownParts.remove(part);
			partDepthMap.remove(part);
		}
		
		private boolean containsUrlPart(String url) {
			return urlParts.containsKey(url);
		}
		
		private Part getUrlPart(String url) {
			return urlParts.get(url);
		}
		
		private void appendCidPart(String name, Part part, int depth) {
			cidParts.put(name, part);
			partDepthMap.put(part, depth);
		}
		
		private void appendUrlPart(String url, Part part, int depth) {
			urlParts.put(url, part);
			partDepthMap.put(part, depth);
		}
		
		private void addCalendar(String calendarMethod, String calendarContent) {
			this.calendarMethod = EnumUtils.forName(StringUtils.upperCase(calendarMethod), CalendarMethod.class);
			this.calendarContent = calendarContent;
		}
		
		private boolean hasCidPart(String name) {
			return cidParts.containsKey(name);
		}
		
		private boolean hasUrlPart(String url) {
			return urlParts.containsKey(url);
		}
		
		private boolean hasCalendar() {
			return calendarMethod != null && calendarContent != null;
		}
	}
	
	public static InternetAddress parseFrom(final MimeMessage mimeMessage, final boolean quietly) {
		Check.notNull(mimeMessage, "mimeMessage");
		try {
			final Address[] addresses = mimeMessage.getFrom();
			return (addresses == null || addresses.length == 0) ? null : (InternetAddress)addresses[0];
		} catch (MessagingException ex) {
			if (quietly) return null;
			throw new MimeMessageParseException("Error parsing from-address", ex);
		}
	}
	
	public static InternetAddress parseReplyTo(final MimeMessage mimeMessage, final boolean quietly) {
		Check.notNull(mimeMessage, "mimeMessage");
		try {
			final Address[] addresses = mimeMessage.getReplyTo();
			return (addresses == null || addresses.length == 0) ? null : (InternetAddress)addresses[0];
		} catch (MessagingException ex) {
			if (quietly) return null;
			throw new MimeMessageParseException("Error parsing replyTo-address", ex);
		}
	}
	
	public static String parseMessageId(final MimeMessage mimeMessage, final boolean quietly) {
		Check.notNull(mimeMessage, "mimeMessage");
		try {
			return mimeMessage.getMessageID();
		} catch (final MessagingException ex) {
			if (quietly) return null;
			throw new MimeMessageParseException("Error getting message ID", ex);
		}
	}
	
	public static List<InternetAddress> parseToAddresses(final MimeMessage mimeMessage, final boolean quietly) {
		Check.notNull(mimeMessage, "mimeMessage");
		try {
			return MimeUtils.toInternetAddresses(MimeUtils.getRecipients(mimeMessage, Message.RecipientType.TO));
		} catch (final MessagingException ex) {
			if (quietly) return Collections.emptyList();
			throw new MimeMessageParseException("Error getting [TO] recipients", ex);
		}
	}
	
	public static List<InternetAddress> parseCcAddresses(final MimeMessage mimeMessage, final boolean quietly) {
		Check.notNull(mimeMessage, "mimeMessage");
		try {
			return MimeUtils.toInternetAddresses(MimeUtils.getRecipients(mimeMessage, Message.RecipientType.CC));
		} catch (final MessagingException ex) {
			if (quietly) return Collections.emptyList();
			throw new MimeMessageParseException("Error getting [CC] recipients", ex);
		}
	}
	
	public static List<InternetAddress> parseBccAddresses(final MimeMessage mimeMessage, final boolean quietly) {
		Check.notNull(mimeMessage, "mimeMessage");
		try {
			return MimeUtils.toInternetAddresses(MimeUtils.getRecipients(mimeMessage, Message.RecipientType.BCC));
		} catch (final MessagingException ex) {
			if (quietly) return Collections.emptyList();
			throw new MimeMessageParseException("Error getting [BCC] recipients", ex);
		}
	}
	
	public static MimeBodyPart getBodyPartAtIndex(final Multipart parentMultiPart, final int index) {
		try {
			return (MimeBodyPart)parentMultiPart.getBodyPart(index);
		} catch (MessagingException ex) {
			throw new MimeMessageParseException("Error getting bodypart at index " + index, ex);
		}
	}
	
	public static int countBodyParts(final Multipart multipart) {
		try {
			return multipart.getCount();
		} catch (MessagingException ex) {
			throw new MimeMessageParseException("Error parsing MimeMessage multipart count", ex);
		}
	}
	
	public static <T> T parseContent(final Part part) {
		try {
			return (T)part.getContent();
		} catch (IOException | MessagingException ex) {
			throw new MimeMessageParseException("Error parsing content", ex);
		}
	}
	
	public static String parseContentLocation(final Part part) {
		try {
			String contentLocation = MimeUtils.getFirstHeaderValue(part, MimeUtils.HEADER_CONTENT_LOCATION);
			return MimeUtils.stripHeaderValueWhitespaces(contentLocation);
		} catch (MessagingException ex) {
			throw new MimeMessageParseException("Error parsing Contect-Location header", ex);
		}
	}
	
	public static String parseFileName(final Part part, final boolean fallbackOnSubType) {
		try {
			return MimeUtils.getFilename(part, fallbackOnSubType);
		} catch (MessagingException | UnsupportedEncodingException ex) {
			throw new MimeMessageParseException("Error parsing fileName", ex);
		}
	}
	
	public static String parseContentID(final Part part) {
		try {
			return MimeUtils.getContentID(part);
		} catch (MessagingException ex) {
			throw new MimeMessageParseException("Error parsing content-ID", ex);
		}
	}
	
	public static String parseDisposition(final Part part) {
		try {
			return part.getDisposition();
		} catch (MessagingException ex) {
			throw new MimeMessageParseException("Error parsing disposition", ex);
		}
	}
	
	private static boolean isMimeType(final Part part, final String mimeType) {
		try {
			return MimeUtils.isMimeType(part, mimeType);
		} catch (MessagingException ex) {
			throw new MimeMessageParseException("Error getting content type", ex);
		}
	}
	
	/**
	 * Returns the "method" part from the Calendar content type (such as "{@code text/calendar; charset="UTF-8"; method="REQUEST"}").
	 * @param part
	 * @return 
	 */
	public static String parseCalendarMethod(final Part part) {
		Pattern compile = Pattern.compile("method=\"?(\\w+)");
		final String contentType;
		try {
			contentType = part.getDataHandler().getContentType();
		} catch (MessagingException ex) {
			throw new MimeMessageParseException("Error getting content type from Calendar bodypart. Unable to determine Calendar METHOD", ex);
		}
		Matcher matcher = compile.matcher(contentType);
		if (!matcher.find()) throw new MimeMessageParseException("Calendar METHOD not found in bodypart content type");
		return matcher.group(1);
	}
	
	/**
	 * Returns the "content" part as String from the Calendar content type
	 * @param part
	 * @return 
	 */
	public static String parseCalendarContent(final Part part) {
		Object content = parseContent(part);
		if (content instanceof InputStream) {
			final InputStream calendarContent = (InputStream)content;
			try {
				return MimeUtils.readInputStreamToString(calendarContent, StandardCharsets.UTF_8);
			} catch (IOException ex) {
				throw new MimeMessageParseException("Error parsing MimeMessage Calendar content", ex);
			}
		}
		return String.valueOf(content);
	}
	
	/*
	public static boolean isPEC(final Part part) {
		try {
			return PECUtils.isPECEnvelope(part);
		} catch (MessagingException ex) {
			throw new MimeMessageParseException("Error checking PEC type", ex);
		}
	}
	*/
}
