/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sonicle.mail.imap;

/**
 *
 * @author gbulfon
 */
public class FlagSortTerm extends SonicleSortTerm {

    String flagStrings[];

    public FlagSortTerm(String[] flagStrings, boolean rev) {
        super(rev);
        this.flagStrings=flagStrings;
    }

    public String[] getFlagStrings() {
        return flagStrings;
    }

}
