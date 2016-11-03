/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Exception related utilities.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class TraceUtils {

    /**
     * @return The stack trace fo specified Throable as a String.
     */
    public static String getAsString(Throwable e) {

        if (e == null) {
            return "null exception";
        }

        if (e.getMessage() != null && e.getMessage().equalsIgnoreCase("No stack trace available")) {
            return e.getMessage();
        }

        StringWriter buf = new StringWriter();

        e.printStackTrace(new PrintWriter(buf));

        return buf.toString();
    }

    /**
     * just some fancy formatting of multiple exceptions
     */
    public static String getAsString(final Throwable[] errors) {

        if (errors == null || errors.length == 0) {
            return "There were no exception stack traces available";
        }

        StringBuffer buf = new StringBuffer("# of exceptions: ").append(errors.length).append('\n');

        for (int i = 0; i < errors.length; i++) {
            if (errors[i] == null) {
                buf.append("null exception at: ").append(i).append('\n');
            } else {
                buf.append("------").append(errors[i].getMessage()).append("------\n");
                buf.append(getAsString(errors[i])).append('\n').append('\n');

            }
        }

        return buf.toString();
    }

    public static void showTrace() {

        try {
            throw new Exception("");
        } catch (Exception t) {
            System.out.print("\n");
            t.printStackTrace();
        }
    }
}    // End TraceUtils
