/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sonicle.mail.imap;

/**
 *
 * @author gbulfon
 */
public abstract class SonicleSortTerm {

    SonicleSortTerm next=null;
    boolean reversed=false;

    public SonicleSortTerm(boolean rev) {
        this.reversed=rev;
    }

    public void append(SonicleSortTerm nextTerm) {
        next=nextTerm;
    }

    public SonicleSortTerm next() {
        return next;
    }

    public boolean isReversed() {
        return reversed;
    }

}
