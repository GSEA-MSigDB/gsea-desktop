/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.utils;

/**
 * For use by classes that wish to make an object immutable.
 * (in cases where the 2 class object immutableObject pattern is hard
 * (or unintuitive) to implement
 * <p/>
 * Immuted -> made immutable
 *
 * @author Aravind Subramanian
 * @author David Eby - Updated to use modern Java built-in RuntimeException
 * @version %I%, %G%
 */
public class ImmutedException extends RuntimeException {

    public ImmutedException() {
        super("This object is immutable or has been immuted and hence this method cannot be called");
    }

}    // End ImmutedException
