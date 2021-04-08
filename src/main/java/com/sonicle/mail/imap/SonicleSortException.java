package com.sonicle.mail.imap;

import jakarta.mail.MessagingException;


/**
 * The exception thrown when a Sort expression could not be handled.
 *
 * @author Gabriele Bulfon
 */

public class SonicleSortException extends MessagingException {

    /**
     * Constructs a SonicleSortException with no detail message.
     */
    public SonicleSortException() {
	super();
    }

    /**
     * Constructs a SonicleSortException with the specified detail message.
     * @param s		the detail message
     */
    public SonicleSortException(String s) {
	super(s);
    }
}
