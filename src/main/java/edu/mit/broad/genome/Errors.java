/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Aravind Subramanian, David Eby
 */
public class Errors {
    private List<String> fErrors_as_strings = new ArrayList<String>();
    private List<Throwable> fErrors_as_throwables = new ArrayList<Throwable>();
    private String fErrorName;

    public Errors() { this("ERROR(S)"); }

    public Errors(final String errorName) { this.fErrorName = errorName; }

    public String getName() { return fErrorName; }

    public void add(final String s) {
        if (!fErrors_as_strings.contains(s)) { fErrors_as_strings.add(s); }
    }

    public void add(final Throwable t) {
        fErrors_as_strings.add(getAsString(t));
        if (t != null && !fErrors_as_throwables.contains(t)) { fErrors_as_throwables.add(t); }
    }

    public void add(final String msg, final Throwable t) {
        fErrors_as_strings.add(msg + "\n" + getAsString(t));
        if (t != null && !fErrors_as_throwables.contains(t)) { fErrors_as_throwables.add(t); }
    }

    public boolean isEmpty() { return fErrors_as_strings.isEmpty() && fErrors_as_throwables.isEmpty(); }

    public Throwable[] getErrors() {
        return fErrors_as_throwables.toArray(new Throwable[fErrors_as_throwables.size()]);
    }

    public String[] getErrorsAsStrings() {
        return fErrors_as_strings.toArray(new String[fErrors_as_strings.size()]);
    }
    
    public String getErrors(final boolean html) {
        StringBuilder buf = new StringBuilder("There were errors: ").append(fErrorName).append(" #:").append(fErrors_as_strings.size());
        String lineBreak = (html) ? "<br>" : "\n";
        buf.append(lineBreak);

        for (int i = 0; i < fErrors_as_strings.size(); i++) {
            buf.append(fErrors_as_strings.get(i));
            buf.append(lineBreak);
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
        }
    }

    // duplicated code from TraceUtils due to package restraints
    private static String getAsString(Throwable e) {
        if (e == null) { return "null exception"; }

        StringWriter buf = new StringWriter();
        e.printStackTrace(new PrintWriter(buf));
        return buf.toString();
    }
}
