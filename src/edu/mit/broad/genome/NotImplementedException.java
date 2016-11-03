/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome;

/**
 * An unchecked exception.
 * Inevitably, there are classes with skeleton methods or methods for some
 * situations that could legally arise but have just not been implemented yet
 * as they are low priority. This exception can be used to flag such cases.
 * <p/>
 * If you see this exception, first check to make sure that you are calling
 * the method with the correct parameters.
 * If that checks out then look to ask about implementing the offending
 * method.
 * <p/>
 * Should be thrown when a feature is not implemented.
 * Usage of this exception should allow us to distingush between
 * errors and unimplemented features.
 * Also a regex search for this NotImplementedException wil reveal
 * source code that needs work.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class NotImplementedException extends RuntimeException {

    private static final String MSG = "Method not implemented yet";

    public NotImplementedException() {
        super(MSG);
    }

    public NotImplementedException(String msg) {
        super(MSG + ": " + msg);
    }

}    // End of class NotImplementedException
