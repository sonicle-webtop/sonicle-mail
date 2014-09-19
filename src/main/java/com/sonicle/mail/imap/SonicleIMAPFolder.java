/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sonicle.mail.imap;

import com.sonicle.mail.imap.commands.GetAnnotation;
import com.sonicle.mail.imap.commands.SetAnnotation;
import com.sun.mail.iap.*;
import com.sun.mail.imap.*;
import com.sun.mail.imap.protocol.*;
import java.io.IOException;
import java.util.Comparator;
import java.util.Vector;
import javax.mail.*;
import javax.mail.internet.MimeUtility;
import javax.mail.search.*;

/**
 *
 * @author gbulfon
 */
public class SonicleIMAPFolder extends IMAPFolder {

    private static String[] searchCharsets; 	// array of search charsets

    static {
        searchCharsets = new String[2]; // 2, for now.
        searchCharsets[0] = "UTF-8";
        searchCharsets[1] = MimeUtility.mimeCharset(
                                MimeUtility.getDefaultJavaCharset()
                            );
    }
	
    public SonicleIMAPFolder(String fullName, char separator, IMAPStore store,
				Boolean isNamespace) {
        super(fullName,separator,store,isNamespace);
    }

    public SonicleIMAPFolder(String fullName, Character separator, IMAPStore store,
				Boolean isNamespace) {
        super(fullName,separator.charValue(),store,isNamespace);
    }

    public SonicleIMAPFolder(ListInfo li, IMAPStore store) {
        super(li,store);
    }
	
	/*
	 * Cyrus ANNOTATION impl
	 */

	public String getAnnotation(String key, boolean shared) throws MessagingException {
		return (String)doCommand(new GetAnnotation(this.fullName, key, shared));
	}
    
	public void setAnnotation(String key, boolean shared, String value) throws MessagingException {
		doCommand(new SetAnnotation(this.fullName, key, shared,value));
	}
    
    /**
     * Sort and Search whole folder for messages matching the given terms.
     */
    public synchronized Message[] sort(SonicleSortTerm sort, SearchTerm term)
				throws MessagingException {
	checkOpened();

	try {
	    Message[] matchMsgs = null;

	    synchronized(messageCacheLock) {
		int[] matches = _sort(sort,term);
		if (matches != null) {
		    matchMsgs = new IMAPMessage[matches.length];
		    // Map seq-numbers into actual Messages.
		    for (int i = 0; i < matches.length; i++)
			matchMsgs[i] = getMessageBySeqNumber(matches[i]);
		}
	    }
	    return matchMsgs;

	} catch (SonicleSortException sex) {
	    // too complex for IMAP
	    return _internalSort(sort,term);
	}
    }

    /**
     * Issue the given sort criterion on all messages in this folder.
     * Returns array of matching sorted sequence numbers. An empty array
     * is returned if no matches are found.
     *
     * @param	sort	SortTerm
     * @return	array of matching sequence numbers.
     */
    private int[] _sort(SonicleSortTerm sort)
			throws MessagingException, SonicleSortException, SearchException {
	return _sort(sort, null);
    }

    /**
     * Issue the given sort and search criterion on all messages in this folder.
     * Returns array of matching sorted sequence numbers. An empty array
     * is returned if no matches are found.
     *
     * @param	msgsets	array of MessageSets
     * @param   sort   SortTerm
     * @param	term	SearchTerm
     * @return	array of matching sequence numbers.
     */
    private int[] _sort(SonicleSortTerm sort, SearchTerm term)
			throws MessagingException, SonicleSortException, SearchException {
	// Check if the search "text" terms is null or contains only ASCII chars
	if (term==null || SearchSequence.isAscii(term)) {
	    try {
		return _issueSort(sort, term, null);
	    } catch (IOException ioex) { /* will not happen */ }
	}

	/* The search "text" terms do contain non-ASCII chars. We need to
	 * use SEARCH CHARSET <charset> ...
	 *	The charsets we try to use are UTF-8 and the locale's
	 * default charset. If the server supports UTF-8, great,
	 * always use it. Else we try to use the default charset.
	 */

	// Cycle thru the list of charsets
	for (int i = 0; i < searchCharsets.length; i++) {
	    if (searchCharsets[i] == null)
		continue;

	    try {
		return _issueSort(sort, term, searchCharsets[i]);
	    } catch (IOException ioex) {
		/* Charset conversion failed. Try the next one */
		continue;
	    } catch (SonicleSortException sex) {
		throw sex;
	    }
	}

	// No luck.
	throw new SonicleSortException("Sort failed");
    }

    /* Apply the given SearchTerm on the specified sequence, using the
     * given charset. <p>
     * Returns array of matching sequence numbers. Note that an empty
     * array is returned for no matches.
     */
    private int[] _issueSort(SonicleSortTerm sort, SearchTerm term,
      			      String charset)
	     throws MessagingException, SonicleSortException, SearchException, IOException {

	// Generate a search-sequence with the given charset
	Argument args = SortSequence.generateSequence(sort,term,
			  charset == null ? null :
					    MimeUtility.javaCharset(charset)
			);

        Response[] r=(Response[])doCommand(new SortCommand(args));

	Response response = r[r.length-1];
	int[] matches = null;

	// Grab all SEARCH responses
	if (response.isOK()) { // command succesful
	    Vector v = new Vector();
	    int num;
	    for (int i = 0, len = r.length; i < len; i++) {
		if (!(r[i] instanceof IMAPResponse))
		    continue;

		IMAPResponse ir = (IMAPResponse)r[i];
		// There *will* be one SEARCH response.
		if (ir.keyEquals("SORT")) {
		    while ((num = ir.readNumber()) != -1)
			v.addElement(new Integer(num));
		    r[i] = null;
		}
	    }

	    // Copy the vector into 'matches'
	    int vsize = v.size();
	    matches = new int[vsize];
	    for (int i = 0; i < vsize; i++)
		matches[i] = ((Integer)v.elementAt(i)).intValue();
	} else {
            throw new SonicleSortException("No server sort capability");
        }

	// dispatch remaining untagged responses
//	notifyResponseHandlers(r);
//	handleResult(response);
	return matches;
    }

    public Message[] _internalSort(SonicleSortTerm sort, SearchTerm term)
				throws MessagingException {
        Message msgs[]=null;
        if (term!=null) msgs=search(term);
        else msgs=getMessages();
        FetchProfile fp=new FetchProfile();
        SonicleSortTerm xterm=sort;
        while(xterm!=null) {
            if (xterm instanceof EnvelopeSortTerm) {
                if (!fp.contains(FetchProfile.Item.ENVELOPE))
                    fp.add(FetchProfile.Item.ENVELOPE);
            }
            else if (xterm instanceof FlagSortTerm || xterm instanceof StatusSortTerm) {
                if (!fp.contains(FetchProfile.Item.FLAGS))
                    fp.add(FetchProfile.Item.FLAGS);
            }
            else if (xterm instanceof MessageIDSortTerm) {
                if (!fp.contains("Message-ID"))
                    fp.add("Message-ID");
            }
            else if (xterm instanceof PrioritySortTerm) {
                if (!fp.contains("X-Priority"))
                    fp.add("X-Priority");
            }
            xterm=xterm.next();
        }
        FetchProfile.Item items[]=fp.getItems();
        String headers[]=fp.getHeaderNames();
        if ((items!=null && items.length>0)||(headers!=null && headers.length>0)) {
            fetch(msgs,fp);
        }
        MessageComparator mcomp=new MessageComparator(sort);
        java.util.Arrays.sort(msgs, mcomp);
	return msgs;
    }

    class MessageComparator implements Comparator<Message> {

      private SonicleSortTerm sort;

      MessageComparator(SonicleSortTerm sort) {
          this.sort=sort;
      }

      public int compare(Message m1, Message m2) {
        int result=0;
        SonicleSortTerm sort=this.sort;
        while(result==0 && sort!=null) {
            result=compare(m1,m2,sort);
            sort=sort.next();
        }
        return result;
      }

      public int compare(Message m1, Message m2, SonicleSortTerm sort) {
          int result=0;
          if (m1.isExpunged() || m2.isExpunged()) return 0;

          if (sort instanceof MessageIDSortTerm) {
              String mid1="";
              String mid2="";
              try {
                  String mids[]=m1.getHeader("Message-ID");
                  if (mids!=null) mid1=mids[0];
              } catch(Exception exc) {}
              try {
                  String mids[]=m2.getHeader("Message-ID");
                  if (mids!=null) mid2=mids[0];
              } catch(Exception exc) {}
              result=mid1.compareTo(mid2);
          }
          else if (sort instanceof FromSortTerm) {
              String sender1="";
              String sender2="";
              try {
                Address senders[]=m1.getFrom();
                if (senders!=null) sender1=senders[0].toString();
              } catch(Exception exc) {}
              try {
                Address senders[]=m2.getFrom();
                if (senders!=null) sender2=senders[0].toString();
              } catch(Exception exc) {}
              result=sender1.compareToIgnoreCase(sender2);
          }
          else if (sort instanceof ToSortTerm) {
              String rcpt1="";
              String rcpt2="";
              try {
                Address rcpts[]=m1.getRecipients(Message.RecipientType.TO);
                if (rcpts!=null) rcpt1=rcpts[0].toString();
              } catch(Exception exc) {}
              try {
                Address rcpts[]=m2.getRecipients(Message.RecipientType.TO);
                if (rcpts!=null) rcpt2=rcpts[0].toString();
              } catch(Exception exc) {}
              result=rcpt1.compareToIgnoreCase(rcpt2);
          }
          else if (sort instanceof SubjectSortTerm) {
              String subject1=null;
              String subject2=null;
              try { subject1=m1.getSubject(); } catch(Exception exc) {}
              try { subject2=m2.getSubject(); } catch(Exception exc) {}
              if (subject1==null) subject1="";
              if (subject2==null) subject2="";
              result=subject1.compareToIgnoreCase(subject2);
          }
          else if (sort instanceof ArrivalSortTerm) {
              java.util.Date date1=null;
              java.util.Date date2=null;
              try { date1=m1.getReceivedDate(); } catch(Exception exc) { exc.printStackTrace();}
              try { date2=m2.getReceivedDate(); } catch(Exception exc) {exc.printStackTrace();}
              if (date1==null) {
                if (date2==null) result=0;
                else result=-1;
              } else if (date2==null) {
                result=1;
              } else {
                result=date1.compareTo(date2);
              }
          }
          else if (sort instanceof DateSortTerm) {
              java.util.Date date1=null;
              java.util.Date date2=null;
              try { date1=m1.getSentDate(); if (date1==null) date1=m1.getReceivedDate(); } catch(Exception exc) { }
              try { date2=m2.getSentDate(); if (date2==null) date2=m2.getReceivedDate(); } catch(Exception exc) { }
              if (date1==null) {
                if (date2==null) result=0;
                else result=-1;
              } else if (date2==null) {
                result=1;
              } else {
                result=date1.compareTo(date2);
              }
          }
          else if (sort instanceof SizeSortTerm) {
              int size1=0;
              int size2=0;
              try { size1=m1.getSize(); } catch(Exception exc) {}
              try { size2=m2.getSize(); } catch(Exception exc) {}
              result=size1-size2;
          }
          else if (sort instanceof StatusSortTerm) {
              try {
                Flags fl1=m1.getFlags();
                Flags fl2=m2.getFlags();
                int f1=fl1.contains(Flags.Flag.ANSWERED)
                    ?(fl1.contains(Flags.Flag.SEEN)?3:1)
                    :fl1.contains(Flags.Flag.SEEN)?2:0;
                int f2=fl2.contains(Flags.Flag.ANSWERED)
                    ?(fl2.contains(Flags.Flag.SEEN)?3:1)
                    :fl2.contains(Flags.Flag.SEEN)?2:0;
                result=f1-f2;
              } catch(MessagingException exc) {
                result=0;
              }
          }
          else if (sort instanceof PrioritySortTerm) {
              try {
                  int p1=getPriority(m1);
                  int p2=getPriority(m2);
                  result=p1-p2;
              } catch(MessagingException exc) {
                  result=0;
              }
          }
          else if (sort instanceof FlagSortTerm) {
              try {
                String flagStrings[]=((FlagSortTerm)sort).getFlagStrings();
                Flags fl1=m1.getFlags();
                Flags fl2=m2.getFlags();
                int len=flagStrings.length;
                int f1=99;
                int f2=99;
                for(int i=0;i<len;++i) {
                    String fs=flagStrings[i];
                    if (fl1.contains(fs)) f1=i;
                    if (fl2.contains(fs)) f2=i;
                }
                result=f1-f2;
              } catch(MessagingException exc) {
                exc.printStackTrace();
                result=0;
              }
          }

          if (sort.isReversed()) result*=-1;

          return result;
      }

      public boolean equals(Message m) {
        /**@todo Implement this java.util.Comparator method*/
        throw new java.lang.UnsupportedOperationException("Method equals() not yet implemented.");
      }

      private int getPriority(Message m) throws MessagingException {
        String xprio=null;
        String h[]=m.getHeader("X-Priority");
        if (h!=null && h.length>0) xprio=h[0];
        int priority=3;
        if (xprio!=null) {
            int ixp=xprio.indexOf(' ');
            if (ixp>0) xprio=xprio.substring(0,ixp);
            try {
                priority=Integer.parseInt(xprio);
            } catch(RuntimeException exc) {}
        }
        return priority;
      }

    }

    class SortCommand implements ProtocolCommand {

        Argument args;

        SortCommand(Argument args) {
            this.args=args;
        }

        public Object doCommand(IMAPProtocol imapp) throws ProtocolException {
            return imapp.command("SORT", args);
        }

    }

    public RecursiveSearchResult recursiveSearchByMessageID(String id, String skipfolders[]) throws MessagingException {
        getSeparator();
        RecursiveSearchResult rsr=null;
        rsr=(RecursiveSearchResult)doCommand(new RecursiveSearchByMessageIDCommand(id,skipfolders));
        return rsr;
    }

    class RecursiveSearchByMessageIDCommand implements ProtocolCommand {

        Argument args;
        String skipfolders[];
        String skipfoldersbase[];

        RecursiveSearchByMessageIDCommand(String id, String skipfolders[]) {
            this.args=new Argument();
            this.skipfolders=skipfolders;
            
            if (skipfolders!=null) {
                skipfoldersbase=new String[skipfolders.length];
                for(int i=0;i<skipfolders.length;++i) {
                    skipfoldersbase[i]=skipfolders[i]+separator;
                }
            }
            args.writeString(id);
        }

        public Object doCommand(IMAPProtocol imapp) throws ProtocolException {
            RecursiveSearchResult rsr=null;
            Vector v=null;
            String curfoldername=null;
            if (fullName.length()>0) {
                v=listFolder(imapp,fullName);
                curfoldername=fullName;
            }
            if (v==null) {
                ListInfo[] linfos=imapp.list(fullName, "*");
                for(ListInfo li: linfos) {
                    if (li.name.equals(fullName) || isSkipFolder(li.name)) continue;
                    curfoldername=li.name;
                    v=listFolder(imapp,curfoldername);
                    if (v!=null) break;
                }
            }
            if (v!=null) {
                rsr=new RecursiveSearchResult();
                rsr.uid=((Integer)v.elementAt(0)).longValue();
                rsr.foldername=curfoldername;
            }
            return rsr;
        }

        private boolean isSkipFolder(String name) {
            if (skipfolders!=null) {
                for(int i=0;i<skipfolders.length;++i) {
                    if (name.equals(skipfolders[i]) || name.startsWith(skipfoldersbase[i])) return true;
                }
            }
            return false;
        }

        private Vector listFolder(IMAPProtocol imapp, String foldername) throws ProtocolException {
            Vector v = null;
            Argument xargs=new Argument();
            xargs.writeString(BASE64MailboxEncoder.encode(foldername));
            Response[] r=imapp.command("SELECT",xargs);
            Response response = r[r.length-1];
            if (response.isOK()) {
                r=imapp.command("UID SEARCH HEADER \"Message-ID\"", args);
                response = r[r.length-1];
                if (response.isOK()) {
                    int num;
                    for (int i = 0, len = r.length; i < len; i++) {
                        if (!(r[i] instanceof IMAPResponse))
                            continue;

                        IMAPResponse ir = (IMAPResponse)r[i];
                        // There *will* be one SEARCH response.
                        if (ir.keyEquals("SEARCH")) {
                            while ((num = ir.readNumber()) != -1) {
                                if (v==null) v=new Vector();
                                v.addElement(new Integer(num));
                            }
                            r[i] = null;
                        }
                    }
                }
            }
            return v;
        }
    }

    public class RecursiveSearchResult {
        public long uid;
        public String foldername;

        public String toString() {
            return "[ UID="+uid+", FODLERNAME="+foldername+" ]";
        }
    }

}
