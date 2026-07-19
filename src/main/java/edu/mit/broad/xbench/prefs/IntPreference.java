/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.xbench.prefs;

import edu.mit.broad.genome.utils.NamedInteger;

public class IntPreference extends AbstractPreference {

    protected IntPreference(String name, String desc, int def, boolean isDebug, boolean needsRestart) {
        super(name, desc, def, isDebug, needsRestart);
    }

    public Object getValue() {
        return kPrefs.getInt(getName(), ((Integer) getDefault()));
    }

    public int getInt() {
        return ((Integer) getValue());
    }

    public void setValue(Object value) throws Exception {
        int ival;
        if (value instanceof NamedInteger) {
            ival = ((NamedInteger) value).getValue();
        } else {
            ival = Integer.parseInt(value.toString());
        }
        kPrefs.putInt(getName(), ival);
    }
}
