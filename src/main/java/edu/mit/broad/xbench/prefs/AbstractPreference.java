/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.prefs;

import org.apache.log4j.Logger;

/**
 * @author Aravind Subramanian
 */
abstract class AbstractPreference implements Preference {

    private String fName;
    private String fDesc;
    private Object fDefault;

    private boolean fDebug;

    private boolean fNeedsRestart;

    protected static final Logger klog = Logger.getLogger(AbstractPreference.class);


    /**
     * Must manually call init() if this form of the protected constructor is used by
     * a subclass
     */
    protected AbstractPreference() {
    }

    /**
     * Class constructor
     *
     * @param name
     * @param desc
     * @param def
     */
    protected AbstractPreference(final String name, final String desc, final Object def, final boolean isDebug, final boolean needsRestart) {
        init(name, desc, def, isDebug, needsRestart);
    }

    /**
     * Class constructor
     *
     * @param name
     * @param desc
     * @param def
     * @param isDebug
     */
    protected void init(final String name, final String desc, final Object def, final boolean isDebug, final boolean needsRestart) {
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

    protected void _setValueOfPref2SelectionComponentValue(Object value) {
        // Object value = getValue(); <- this gets the prefs value and not the components!
        klog.debug("Saving pref: " + getName() + " getValue: " + value);
        if (value != null) {
            kPrefs.put(getName(), value.toString());
        }

    }

} // End class AbstractPreference
