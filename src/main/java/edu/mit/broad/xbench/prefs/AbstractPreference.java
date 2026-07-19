/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.xbench.prefs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractPreference implements Preference {
    private String fName;
    private String fDesc;
    private Object fDefault;
    private boolean fDebug;
    private boolean fNeedsRestart;

    protected static final Logger klog = LoggerFactory.getLogger(AbstractPreference.class);

    protected AbstractPreference() {
    }

    protected AbstractPreference(final String name, final String desc, final Object def,
            final boolean isDebug, final boolean needsRestart) {
        init(name, desc, def, isDebug, needsRestart);
    }

    protected void init(final String name, final String desc, final Object def, final boolean isDebug,
            final boolean needsRestart) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter name cannot be null");
        }
        if (desc == null) {
            throw new IllegalArgumentException("Parameter desc cannot be null");
        }
        if (def == null) {
            throw new IllegalArgumentException("Parameter def cannot be null");
        }
        this.fName = name;
        this.fDesc = desc;
        this.fDefault = def;
        this.fDebug = isDebug;
        this.fNeedsRestart = needsRestart;
    }

    public String getName() {
        return fName;
    }

    public String getDesc() {
        return fDesc;
    }

    public Object getDefault() {
        return fDefault;
    }

    public boolean isDebug() {
        return fDebug;
    }

    public boolean needsRestart() {
        return fNeedsRestart;
    }
}
