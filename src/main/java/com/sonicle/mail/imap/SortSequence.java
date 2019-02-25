/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sonicle.mail.imap;

import com.sun.mail.iap.*;
import com.sun.mail.imap.protocol.SearchSequence;
import java.io.*;
import javax.mail.search.*;

/**
 *
 * @author Gabriele Bulfon
 */
public class SortSequence {

    static Argument generateSequence(SonicleSortTerm sort, SearchTerm term, String charset)
		throws SonicleSortException, SearchException, IOException {
        Argument sortArg=new Argument();
        StringBuffer sortSB=new StringBuffer();
        int sortArgs=0;
        while(sort!=null) {

            if (sortSB.length()>0) sortSB.append(' ');
            
            if (sort.isReversed()) sortSB.append("REVERSE ");

            if (sort instanceof ArrivalSortTerm) sortSB.append("ARRIVAL");
            else if (sort instanceof CcSortTerm) sortSB.append("CC");
            else if (sort instanceof DateSortTerm) sortSB.append("DATE");
            else if (sort instanceof FromSortTerm) sortSB.append("FROM");
            else if (sort instanceof SizeSortTerm) sortSB.append("SIZE");
            else if (sort instanceof SubjectSortTerm) sortSB.append("SUBJECT");
            else if (sort instanceof ToSortTerm) sortSB.append("TO");
            else {
                throw new SonicleSortException("Sort too complex");
            }
            ++sortArgs;
            sort=sort.next();
        }
        sortArg.writeAtom("("+sortSB.toString()+")");
        Argument searchArg=null;
        if (term!=null) searchArg=(new com.sonicle.mail.imap.SearchSequence()).generateSequence(term, charset);
        else { searchArg=new Argument(); searchArg.writeAtom("ALL"); }

        Argument args=new Argument();
        args.append(sortArg);
        if (charset==null)  charset="US-ASCII";
        args.writeAtom(charset);
        args.append(searchArg);
        return args;
    }

}
