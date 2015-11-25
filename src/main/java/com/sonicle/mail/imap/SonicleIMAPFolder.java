/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sonicle.mail.imap;

import com.sonicle.mail.imap.commands.GetAnnotation;
import com.sonicle.mail.imap.commands.GetThread;
import com.sonicle.mail.imap.commands.SetAnnotation;
import com.sun.mail.iap.*;
import com.sun.mail.imap.*;
import com.sun.mail.imap.protocol.*;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
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
	
    protected IMAPMessage newIMAPMessage(int msgnum) {
		return new SonicleIMAPMessage(this, msgnum);
    }
	
/*	public Map getCapabilities() {
		Map map=null;
		try {
			map=getProtocol().getCapabilities();
		} catch(ProtocolException exc) {
		}
		return map;
	}*/
	
	
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
	 * Thread messages
	 */

	
    public synchronized SonicleIMAPMessage[] thread(String method)
				throws MessagingException {
		return thread(method,null);
	}
	
    /**
     * Thread and Search whole folder for messages matching the given terms.
     */
    public synchronized SonicleIMAPMessage[] thread(String method, SearchTerm term)
				throws MessagingException {
		checkOpened();
		
		boolean simplified=!method.equals("REFS");

		try {
			SonicleIMAPMessage[] matchMsgs = null;

			synchronized(messageCacheLock) {
				ThreadList[] matches = _thread(method,term,simplified);
				if (matches != null) {
					ArrayList<SonicleIMAPMessage> vmatchMsgs = new ArrayList<SonicleIMAPMessage>();
					// Map seq-numbers into actual Messages.
					if (simplified) {
						//no REFS method, tries the simplified threading
						for (int i=matches.length-1;i>=0;--i) {
							ThreadList tl=matches[i];
							int tid=tl.getThreadId();
							ArrayList<Integer> ids=tl.getIds();
							int tsize=ids.size();
							if (tsize==1) {
								SonicleIMAPMessage sim=(SonicleIMAPMessage)getMessageBySeqNumber(ids.get(0));
								sim.setThreadId(tid);
								vmatchMsgs.add(sim);
							} else {
								//create first (last in date) and search insert point before i-th
								SonicleIMAPMessage sim0=(SonicleIMAPMessage)getMessageBySeqNumber(ids.get(0));
								sim0.setThreadId(tid);
								sim0.setThreadSize(tsize);
								sim0.setMostRecentInThread(true);
								Date rdate0=sim0._getDate();
								sim0.setMostRecentThreadDate(rdate0);
								int pos=vmatchMsgs.size();
								if (pos>0) {
									while(pos>0) {
										SonicleIMAPMessage tmx=vmatchMsgs.get(pos-1);
										if (!tmx.hasThreads() || tmx.isMostRecentInThread()) {
											Date rdatex=tmx._getDate();
											if (rdatex.after(rdate0)) {
												//skip after current threads
												if (tmx.hasThreads()) {
													while(!vmatchMsgs.get(pos).isOldestInThread()) ++pos;
													++pos;
												}
												break;
											}
										}
										--pos;
									}
								}

								vmatchMsgs.add(pos++,sim0);

								SonicleIMAPMessage lastsim=null;
								for(int j=1;j<ids.size();++j) {
									lastsim=(SonicleIMAPMessage)getMessageBySeqNumber(ids.get(j));
									lastsim.setThreadId(tid);
									lastsim.setThreadSize(tsize);
									lastsim.setMostRecentThreadDate(rdate0);
									vmatchMsgs.add(pos++,lastsim);
								}
								lastsim.setOldestInThread(true);
							}
						}
						matchMsgs=new SonicleIMAPMessage[vmatchMsgs.size()];
						vmatchMsgs.toArray(matchMsgs);
						
					} else {
						
						for (int i=matches.length-1;i>=0;--i) {
							ThreadList tl=matches[i];
							int tid=tl.getThreadId();
							ArrayList<Integer> ids=tl.getIds();
							int tsize=ids.size();
							if (tsize==1) {
								SonicleIMAPMessage sim=(SonicleIMAPMessage)getMessageBySeqNumber(ids.get(0));
								sim.setThreadId(tid);
								vmatchMsgs.add(sim);
							} else {
								for(int j=tsize-1;j>=0;--j) {
									SonicleIMAPMessage sim=(SonicleIMAPMessage)getMessageBySeqNumber(ids.get(j));
									sim.setThreadId(tid);
									vmatchMsgs.add(sim);
								}
							}
						}
						matchMsgs=new SonicleIMAPMessage[vmatchMsgs.size()];
						vmatchMsgs.toArray(matchMsgs);
					}
										
				} 
			}
			return matchMsgs;

		} catch (Exception sex) {
			throw new MessagingException("Command too complex",sex);
		}
    }
	
    /**
     * Issue the given thread criterion on all messages in this folder.
     * Returns array of matching threaded sequence numbers. An empty array
     * is returned if no matches are found.
     *
     * @param	int	thread method
     * @return	array of matching sequence numbers.
     */
    private ThreadList[] _thread(String method, boolean simplified)
			throws MessagingException, SearchException {
		return _thread(method, null, simplified);
    }

    /**
     * Issue the given thread and search criterion on all messages in this folder.
     * Returns array of matching threaded sequence numbers. An empty array
     * is returned if no matches are found.
     *
     * @param   int   thread method
     * @param	term	SearchTerm
     * @return	array of matching sequence numbers.
     */
    private ThreadList[] _thread(String method, SearchTerm term, boolean simplified)
			throws MessagingException, SearchException {
		// Check if the search "text" terms is null or contains only ASCII chars
		if (term==null || SearchSequence.isAscii(term)) {
			try {
				return _issueThread(method, term, null, simplified);
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
				return _issueThread(method, term, searchCharsets[i], simplified);
			} catch (IOException ioex) {
				/* Charset conversion failed. Try the next one */
				continue;
			} catch (SearchException sex) {
				throw sex;
			}
		}

		// No luck.
		throw new MessagingException("Thread failed");
    }

    /* Apply the given threading method and SearchTerm on the specified sequence, using the
     * given charset. <p>
     * Returns array of matching sequence numbers. Note that an empty
     * array is returned for no matches.
     */
    private ThreadList[] _issueThread(String method, SearchTerm term, String charset, boolean simplified)
	     throws MessagingException, SearchException, IOException {

        Response[] r=(Response[])doCommand(new GetThread(method,term,charset));

		Response response = r[r.length-1];
		ThreadList[] matches = null;

		// Grab all THREAD responses
		if (response.isOK()) { // command succesful
			ArrayList<ThreadList> v = new ArrayList<ThreadList>();
			ArrayList<ThreadList> brothers=new ArrayList<ThreadList>();
			//int num;
			for (int i = 0, len = r.length; i < len; i++) {
				if (!(r[i] instanceof IMAPResponse))
					continue;

				IMAPResponse ir = (IMAPResponse)r[i];
				// There *will* be one THREAD response.
				if (ir.keyEquals("THREAD")) {
					SonicleIMAPThreadResponse sitr=new SonicleIMAPThreadResponse(ir);
					ThreadList tl;
					while((tl=sitr.readThreadList())!=null) {
						v.add(tl);
						if (simplified && tl.hasBrothers())
							brothers.addAll(tl.getBrothers());
					}
					if (simplified) {
						ThreadListComparator tlc=new ThreadListComparator();
						for(ThreadList brother: brothers) {
							int ix=Collections.binarySearch(v, brother,tlc);
							//Date d=((SonicleIMAPMessage)getMessageBySeqNumber(brother.getIds().get(0)))._getDate();
							if (ix>=0)
								v.add(ix, brother);
							else
								v.add((ix*-1)-1,brother);
						}
					}
					r[i] = null;
				}
			}

			// Copy the vector into 'matches'
			matches = new ThreadList[v.size()];
			v.toArray(matches);
		} else {
			throw new MessagingException("No server thread capability");
		}

		return matches;
    }

	
	/************************************************************************/
	
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
	
    public synchronized Message[] uid_sort(SonicleSortTerm sort, SearchTerm term)
				throws MessagingException {
		checkOpened();

		try {
			Message[] matchMsgs = null;

			synchronized(messageCacheLock) {
				long[] matches = _uid_sort(sort,term);
				//System.out.println("matches "+matches.length);
				if (matches != null) {
					matchMsgs=getMessagesByUID(matches);
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
	
    private long[] _uid_sort(SonicleSortTerm sort)
			throws MessagingException, SonicleSortException, SearchException {
		return _uid_sort(sort, null);
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
	
    private long[] _uid_sort(SonicleSortTerm sort, SearchTerm term)
			throws MessagingException, SonicleSortException, SearchException {
		// Check if the search "text" terms is null or contains only ASCII chars
		if (term==null || SearchSequence.isAscii(term)) {
			try {
			return _issueUIDSort(sort, term, null);
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
				return _issueUIDSort(sort, term, searchCharsets[i]);
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

    private long[] _issueUIDSort(SonicleSortTerm sort, SearchTerm term,
      			      String charset)
	     throws MessagingException, SonicleSortException, SearchException, IOException {

		// Generate a search-sequence with the given charset
		Argument args = SortSequence.generateSequence(sort,term,
				  charset == null ? null :
							MimeUtility.javaCharset(charset)
				);

			Response[] r=(Response[])doCommand(new UIDSortCommand(args));

		Response response = r[r.length-1];
		long[] matches = null;

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
				v.addElement(new Long(num));
				r[i] = null;
			}
			}

			// Copy the vector into 'matches'
			int vsize = v.size();
			matches = new long[vsize];
			for (int i = 0; i < vsize; i++)
			matches[i] = ((Long)v.elementAt(i)).longValue();
		} else {
			throw new SonicleSortException("No server uid sort capability");
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
	
    class UIDSortCommand implements ProtocolCommand {

        Argument args;

        UIDSortCommand(Argument args) {
            this.args=args;
        }

        public Object doCommand(IMAPProtocol imapp) throws ProtocolException {
            return imapp.command("UID SORT", args);
        }

    }

    class UIDFetchCommand implements ProtocolCommand {

        UIDSet[] uidset;
		String what;

        UIDFetchCommand(UIDSet[] uidset, String what) {
            this.uidset=uidset;
			this.what=what;
        }

        public Object doCommand(IMAPProtocol imapp) throws ProtocolException {
            return imapp.command("UID FETCH " + UIDSet.toString(uidset) +" (" + what + ")", null);
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
	
	class ThreadListComparator implements Comparator {
		DateFormat df=DateFormat.getDateInstance(DateFormat.SHORT);

		public int compare(Object o1, Object o2) {
			int result=0;
			try {
				ThreadList tl1=(ThreadList)o1;
				ThreadList tl2=(ThreadList)o2;
				int id1=tl1.getIds().get(0);
				int id2=tl2.getIds().get(0);
				SonicleIMAPMessage sim1=(SonicleIMAPMessage)getMessageBySeqNumber(id1);
				SonicleIMAPMessage sim2=(SonicleIMAPMessage)getMessageBySeqNumber(id2);
				Date d1=sim1._getDate();
				Date d2=sim2._getDate();
				result=d1.compareTo(d2);
			} catch(MessagingException exc) {
				exc.printStackTrace();
			}
			return result;
		}
		
	}
	
	
    /**
     * Get the Messages specified by the given array. <p>
     *
     * <code>uids.length()</code> elements are returned.
     * If any UID in the array is invalid, a <code>null</code> entry
     * is returned for that element.
	 * @param uids
	 * @return 
     */
	@Override
    public synchronized Message[] getMessagesByUID(long[] uids)
			throws MessagingException {
		checkOpened(); // insure that folder is open

		try {
			synchronized(messageCacheLock) {
				long[] unavailUids = uids;
				if (uidTable != null) {
					Vector v = new Vector(); // to collect unavailable UIDs
					Long l;
					for (int i = 0; i < uids.length; i++) {
						if (!uidTable.containsKey(l = Long.valueOf(uids[i])))
							// This UID has not been loaded yet.
							v.addElement(l);
					}

					int vsize = v.size();
					unavailUids = new long[vsize];
					for (int i = 0; i < vsize; i++)
						unavailUids[i] = ((Long)v.elementAt(i)).longValue();
				} else
					uidTable = new java.util.Hashtable();

				if (unavailUids.length > 0) {
					// Issue UID FETCH request for given uids
					int len=unavailUids.length;
					UID[] ua;
					/*
					//SONICLE: breaks it into banks of 10000 if larger
					int N=10000;
					if (len>N) {
						//System.out.println("getMessagesByUID: more than 10000");
						ua=new UID[len];
						for(int i=0;i<len;i+=N) {
							int remaining=len-i;
							int n=(remaining>=N?N:remaining);
							//System.out.println("getMessagesByUID: fetching "+n+" messages, from "+i);
							long[] unUids=new long[n];
							System.arraycopy(unavailUids, i, unUids, 0, n);
							UID[] uax = getProtocol().fetchSequenceNumbers(unUids);
							System.arraycopy(uax, 0, ua, i, n);
						}
					} else {
						//System.out.println("getMessagesByUID: less than 10000");
						ua = getProtocol().fetchSequenceNumbers(unavailUids);
					}*/
					ua = fetchSequenceNumbers(unavailUids);
					
					SonicleIMAPMessage m;
					for (int i = 0; i < ua.length; i++) {
						m = (SonicleIMAPMessage) getMessageBySeqNumber(ua[i].seqnum);
						if (m != null) {
							m.setUID(ua[i].uid);
							uidTable.put(Long.valueOf(ua[i].uid), m);
						}
					}
				}

				// Return array of size = uids.length
				Message[] msgs = new Message[uids.length];
				for (int i = 0; i < uids.length; i++)
					msgs[i] = (Message)uidTable.get(Long.valueOf(uids[i]));
				return msgs;
			}
		} catch(ConnectionException cex) {
			throw new FolderClosedException(this, cex.getMessage());
		} catch (ProtocolException pex) {
			throw new MessagingException(pex.getMessage(), pex);
		} catch (IOException ioex) {
			throw new MessagingException(ioex.getMessage(), ioex);
		}
    }
	
	int seqcounter=0;
	
	public UID[] fetchSequenceNumbers(long uids[]) throws ConnectionException, ProtocolException, IOException {
		UID[] ouids=new UID[uids.length];
		for(int i=0;i<uids.length;++i) {
			ouids[i]=new SonicleUID(++seqcounter,uids[i]);
		}
		return ouids;
	}
	
	class SonicleUID extends com.sun.mail.imap.protocol.UID {
		
		SonicleUID(int seqnum, long uid) throws ParsingException, ProtocolException, IOException {
			super(new FetchResponse(new IMAPResponse("* "+seqnum+" FETCH (UID "+uid+")")));
			this.seqnum=seqnum;
			this.uid=uid;
		}
	}
	
	
	
	
	
	/**
	 * Prefetch attributes, based on the given FetchProfile.
	 */
	public synchronized void uid_fetch(Message[] msgs, FetchProfile fp)
			throws MessagingException {
		checkOpened();

		StringBuffer command = new StringBuffer();
		boolean first = true;
		boolean allHeaders = false;

		if (fp.contains(FetchProfile.Item.ENVELOPE)) {
			command.append(getEnvelopeCommand());
			first = false;
		}
		if (fp.contains(FetchProfile.Item.FLAGS)) {
			command.append(first ? "FLAGS" : " FLAGS");
			first = false;
		}
		if (fp.contains(FetchProfile.Item.CONTENT_INFO)) {
			command.append(first ? "BODYSTRUCTURE" : " BODYSTRUCTURE");
			first = false;
		}
		if (fp.contains(UIDFolder.FetchProfileItem.UID)) {
			command.append(first ? "UID" : " UID");
			first = false;
		}
		if (fp.contains(IMAPFolder.FetchProfileItem.HEADERS)) {
			allHeaders = true;
			if (protocol.isREV1()) {
				command.append(first
						? "BODY.PEEK[HEADER]" : " BODY.PEEK[HEADER]");
			} else {
				command.append(first ? "RFC822.HEADER" : " RFC822.HEADER");
			}
			first = false;
		}
		if (fp.contains(IMAPFolder.FetchProfileItem.MESSAGE)) {
			allHeaders = true;
			if (protocol.isREV1()) {
				command.append(first ? "BODY.PEEK[]" : " BODY.PEEK[]");
			} else {
				command.append(first ? "RFC822" : " RFC822");
			}
			first = false;
		}
		if (fp.contains(FetchProfile.Item.SIZE)
				|| fp.contains(IMAPFolder.FetchProfileItem.SIZE)) {
			command.append(first ? "RFC822.SIZE" : " RFC822.SIZE");
			first = false;
		}

		// if we're not fetching all headers, fetch individual headers
		String[] hdrs = null;
		if (!allHeaders) {
			hdrs = fp.getHeaderNames();
			if (hdrs.length > 0) {
				if (!first) {
					command.append(" ");
				}
				command.append(createHeaderCommand(hdrs));
			}
		}

		/*
		 * Add any additional extension fetch items.
		 */
		FetchItem[] fitems = protocol.getFetchItems();
		for (int i = 0; i < fitems.length; i++) {
			if (fp.contains(fitems[i].getFetchProfileItem())) {
				if (command.length() != 0) {
					command.append(" ");
				}
				command.append(fitems[i].getName());
			}
		}

		Utility.Condition condition
				= new IMAPMessage.FetchProfileCondition(fp, fitems);

		// Acquire the Folder's MessageCacheLock.
		synchronized (messageCacheLock) {

	    // Apply the test, and get the sequence-number set for
			// the messages that need to be prefetched.
			UIDSet[] uidsets = toUIDSet(msgs, condition);

			if (uidsets == null) // We already have what we need.
			{
				return;
			}

			Response[] r = null;
			Vector v = new Vector(); // to collect non-FETCH responses &
			// unsolicited FETCH FLAG responses 
			r=(Response[])doCommand(new UIDFetchCommand(uidsets,command.toString()));

			if (r == null) {
				return;
			}

			for (int i = 0; i < r.length; i++) {
				if (r[i] == null) {
					continue;
				}
				if (!(r[i] instanceof FetchResponse)) {
					v.addElement(r[i]); // Unsolicited Non-FETCH response
					continue;
				}

				// Got a FetchResponse.
				FetchResponse f = (FetchResponse) r[i];
				// Get the corresponding message.
				SonicleIMAPMessage msg = (SonicleIMAPMessage)getMessageByUID(f.getItem(UID.class).uid);

				int count = f.getItemCount();
				boolean unsolicitedFlags = false;

				for (int j = 0; j < count; j++) {
					Item item = f.getItem(j);
					// Check for the FLAGS item
					if (item instanceof Flags
							&& (!fp.contains(FetchProfile.Item.FLAGS)
							|| msg == null)) {
						// Ok, Unsolicited FLAGS update.
						unsolicitedFlags = true;
					} else if (msg != null) {
						msg.handleFetchItem(item, hdrs, allHeaders);
					}
				}
				if (msg != null) {
					msg.handleExtensionFetchItems(f.getExtensionItems());
				}

		// If this response contains any unsolicited FLAGS
				// add it to the unsolicited response vector
				if (unsolicitedFlags) {
					v.addElement(f);
				}
			}

			// Dispatch any unsolicited responses
			int size = v.size();
			if (size != 0) {
				Response[] responses = new Response[size];
				v.copyInto(responses);
				handleResponses(responses);
			}

		} // Release messageCacheLock
	}
	

    /**
     * Handle the given array of Responses.
     *
     * ASSERT: This method must be called only when holding the
     * 	messageCacheLock
     */
	void handleResponses(Response[] r) {
		for (int i = 0; i < r.length; i++) {
			if (r[i] != null) {
				handleResponse(r[i]);
			}
		}
	}
	
	/**
	 * Create the appropriate IMAP FETCH command items to fetch the requested
	 * headers.
	 */
	private String createHeaderCommand(String[] hdrs) {
		StringBuffer sb;
		
		if (protocol.isREV1()) {
			sb = new StringBuffer("BODY.PEEK[HEADER.FIELDS (");
		} else {
			sb = new StringBuffer("RFC822.HEADER.LINES (");
		}

		for (int i = 0; i < hdrs.length; i++) {
			if (i > 0) {
				sb.append(" ");
			}
			sb.append(hdrs[i]);
		}

		if (protocol.isREV1()) {
			sb.append(")]");
		} else {
			sb.append(")");
		}

		return sb.toString();
	}
	
	private UIDSet[] toUIDSet(Message[] msgs, Utility.Condition cond) throws MessagingException {
		Vector v = new Vector(1);
		long current, next;
		
		SonicleIMAPMessage msg;
		for (int i = 0; i < msgs.length; i++) {
			msg = (SonicleIMAPMessage) msgs[i];
			if (msg.isExpunged()) // expunged message, skip it
			{
				continue;
			}

			//current = msg.getUID();
			current=getUID(msg);
			// Apply the condition. If it fails, skip it.
			if ((cond != null) && !cond.test(msg)) {
				continue;
			}

			UIDSet set = new UIDSet();
			set.start = current;

			// Look for contiguous sequence numbers
			for (++i; i < msgs.length; i++) {
				// get next message
				msg = (SonicleIMAPMessage) msgs[i];

				if (msg.isExpunged()) // expunged message, skip it
				{
					continue;
				}
				next = msg.getUID();

				// Does this message match our condition ?
				if ((cond != null) && !cond.test(msg)) {
					continue;
				}

				if (next == current + 1) {
					current = next;
				} else { // break in sequence
					// We need to reexamine this message at the top of
					// the outer loop, so decrement 'i' to cancel the
					// outer loop's autoincrement 
					i--;
					break;
				}
			}
			set.end = current;
			v.addElement(set);
		}

		if (v.isEmpty()) // No valid messages
		{
			return null;
		} else {
			UIDSet[] sets = new UIDSet[v.size()];
			v.copyInto(sets);
			return sets;
		}
	}
	
}
