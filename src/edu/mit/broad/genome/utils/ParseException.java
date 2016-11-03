/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.utils;

/**
 * Tags a checked exception as Parsing related.
 * Designed for use with ParseUtils.java.
 * <p/>
 * Note: This is for use with simple (i.e single-method parsing operations
 * do not confuse with the ParserException class).
 *
 * @author Aravind Subramanian
 * @author David Eby - Updated to use modern Java built-in RuntimeException
 * @version %I%, %G%
 * @see edu.mit.broad.genome.parsers.ParseUtils.java
 * @see ParserException.java
 */

// Imp that this be a checked exception as Parse errors tend to be show-stoppers.
public class ParseException extends Exception {

    /**
     * Construct a new <code>ParseException</code> instance.
     *
     * @param message The detail message for this exception.
     */
    public ParseException(final String message) {
        super(message);
    }

    /**
     * Construct a new <code>ParseException</code> instance.
     *
     * @param message   The detail message for this exception.
     * @param throwable the root cause of the exception
     */
    public ParseException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Construct a new <code>ParseException</code> instance.
     *
     * @param throwable the root cause of the exception
     */
    public ParseException(final Throwable throwable) {
        super(throwable);
    }
    
    
}    // End ParseException
