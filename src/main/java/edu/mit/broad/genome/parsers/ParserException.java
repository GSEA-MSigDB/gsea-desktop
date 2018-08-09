/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

/**
 * An exception related to parsing data.
 *
 * @author Aravind Subramanian
 * @author David Eby - Updated to use modern Java built-in RuntimeException
 * @version %I%, %G%
 */
public class ParserException extends Exception {

    /**
     * Create an exception with a detail message.
     *
     * @param msg the message
     */
    public ParserException(final String msg) {
        super(msg);
    }

    /**
     * Create a chained exception.
     *
     * @param t the nested esception.
     */
    public ParserException(final Throwable t) {
        super(t);
    }

    /**
     * Create a chained exception along with a message
     *
     * @param msg the message
     * @param t   the nested esception.
     */
    public ParserException(final String msg, final Throwable t) {
        super(msg, t);
    }

}    // End ParserException
