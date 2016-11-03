/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Aravind Subramanian
 */
public class Errors {

    private List fErrors_as_strings;
    private List fErrors_as_throwables;
    private String fErrorName;

    /**
     * Class constructor
     */
    public Errors() {
        this("ERROR(S)");
    }

    /**
     * Class constructor
     *
     * @param errorName
     */
    public Errors(final String errorName) {
        this.fErrorName = errorName;
        this.fErrors_as_throwables = new ArrayList();
        this.fErrors_as_strings = new ArrayList();
    }

    public String getName() {
        return fErrorName;
    }

    public void add(final String s) {
        if (fErrors_as_strings.contains(s) == false) {
            fErrors_as_strings.add(s);
        }
    }

    public void add(final Throwable t) {
        fErrors_as_strings.add(getAsString(t));
        if (t != null && !fErrors_as_throwables.contains(t) == false) {
            fErrors_as_throwables.add(t);
        }
    }

    public void add(final String msg, final Throwable t) {
        fErrors_as_strings.add(msg + "\n" + getAsString(t));
        if (t != null && fErrors_as_throwables.contains(t) == false) {
            fErrors_as_throwables.add(t);
        }
    }

    public boolean isEmpty() {
        boolean isEmpty = fErrors_as_strings.isEmpty();

        if (!isEmpty) {
            return isEmpty;
        }

        return fErrors_as_throwables.isEmpty();
    }

    public Throwable[] getErrors() {
        return (Throwable[]) fErrors_as_throwables.toArray(new Throwable[fErrors_as_throwables.size()]);
    }

    public String getErrors(final boolean html) {

        StringBuffer buf = new StringBuffer("There were errors: ").append(fErrorName).append(" #:").append(fErrors_as_strings.size());
        if (html) {
            buf.append("<br>");
        } else {
            buf.append("\n");
        }

        for (int i = 0; i < fErrors_as_strings.size(); i++) {
            buf.append(fErrors_as_strings.get(i).toString());
            if (html) {
                buf.append("<br>");
            } else {
                buf.append("\n");
            }
        }

        return buf.toString();
    }

    public void barfIfNotEmptyRuntime() throws RuntimeException {
        barfIfNotEmptyRuntime(null);
    }

    public void barfIfNotEmptyRuntime(String msg) throws RuntimeException {
        if (fErrors_as_strings.isEmpty() == false) {
            StringBuffer buf = new StringBuffer();
            if (msg != null && msg.length() > 0) {
                buf.append(msg).append('\n');
            }

            buf.append("There were errors: ").append(fErrorName).append(" #:").append(fErrors_as_strings.size()).append("\n");
            for (int i = 0; i < fErrors_as_strings.size(); i++) {
                buf.append(fErrors_as_strings.get(i).toString()).append('\n');
            }
            throw new RuntimeException(buf.toString());
        } else {
            //klog.info("NO error(s) for: " + fErrorName);
        }
    }

    // duplicated code from TraceUtils due to package restraints
    private static String getAsString(Throwable e) {

        if (e == null) {
            return "null exception";
        }

        StringWriter buf = new StringWriter();

        e.printStackTrace(new PrintWriter(buf));

        return buf.toString();
    }

} // End Errors
