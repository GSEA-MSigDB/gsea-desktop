/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.xbench.prefs;

public class StringPreference extends AbstractPreference {

    protected StringPreference(String name, String desc, String def, boolean isDebug, boolean needsRestart) {
        super(name, desc, def, isDebug, needsRestart);
    }

    public Object getValue() {
        return kPrefs.get(getName(), getDefault().toString());
    }

    public String getString() {
        return getValue().toString();
    }

    public void setValue(Object value) {
        kPrefs.put(getName(), value.toString());
    }
}
