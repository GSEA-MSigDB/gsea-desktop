/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api;

public class CanceledException extends Exception {
    public CanceledException() { }

    public CanceledException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public CanceledException(String message, Throwable cause) {
        super(message, cause);
    }

    public CanceledException(String message) {
        super(message);
    }

    public CanceledException(Throwable cause) {
        super(cause);
    }
}
