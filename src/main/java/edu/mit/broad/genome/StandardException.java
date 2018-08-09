/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome;

/**
 * @author Aravind Subramanian
 */
public class StandardException extends RuntimeException {

    private int fErrorCode;

    /**
     * Class constructor
     *
     * @param badParams
     */
    public StandardException(final String title, int errorCode) {
        super(title);
        this.fErrorCode = errorCode;
    }

    public StandardException(String title, Throwable cause, int errorCode) {
        super(title, cause);
        this.fErrorCode = errorCode;
    }

    public int getErrorCode() {
        return fErrorCode;
    }

} // End class StandardException
