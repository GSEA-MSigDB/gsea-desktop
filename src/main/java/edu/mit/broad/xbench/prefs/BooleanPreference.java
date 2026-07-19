/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.xbench.prefs;

public class BooleanPreference extends AbstractPreference {

    protected BooleanPreference(final String name, final String desc, final boolean def,
            final boolean isDebug, final boolean needsRestart) {
        super(name, desc, Boolean.valueOf(def), isDebug, needsRestart);
    }

    public boolean getBoolean() {
        return ((Boolean) getValue()).booleanValue();
    }

    public Boolean getBooleanO() {
        return (Boolean) getValue();
    }

    public Object getValue() {
        return Boolean.valueOf(kPrefs.getBoolean(getName(), ((Boolean) getDefault()).booleanValue()));
    }

    public void setValue(final Object value) throws Exception {
        kPrefs.putBoolean(getName(), ((Boolean) value).booleanValue());
    }
}
