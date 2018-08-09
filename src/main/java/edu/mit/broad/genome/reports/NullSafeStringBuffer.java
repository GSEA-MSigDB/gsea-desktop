/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.reports;

import edu.mit.broad.genome.Constants;

/**
 * @author Aravind Subramanian
 */
public class NullSafeStringBuffer {

    private StringBuffer fBuf;

    private boolean fReplaceNullWithNULL;

    /**
     * Class constructor
     *
     * @param replaceNullWithNULL
     */
    public NullSafeStringBuffer(boolean replaceNullWithNULL) {
        fBuf = new StringBuffer();
        this.fReplaceNullWithNULL = replaceNullWithNULL;
    }

    public NullSafeStringBuffer append(String s) {
        if (s != null) {
            fBuf.append(s);
        } else {
            if (fReplaceNullWithNULL) {
                fBuf.append(Constants.NULL);
            }
        }

        return this;
    }

    public NullSafeStringBuffer append(char s) {
        fBuf.append(s);
        return this;
    }

    public String toString() {
        return fBuf.toString();
    }

    public int hashCode() {
        return fBuf.hashCode();
    }

    public boolean equals(Object obj) {
        return fBuf.equals(obj);
    }

} // End class NullSafeStringBuffer
