/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sonicle.mail.imap;

/**
 * To easily identify a SonicleSortTerm that needs an ENVELOPE fetch profile.
 * @author gbulfon
 */
public abstract class EnvelopeSortTerm extends SonicleSortTerm {

    public EnvelopeSortTerm(boolean rev) {
        super(rev);
    }
}
