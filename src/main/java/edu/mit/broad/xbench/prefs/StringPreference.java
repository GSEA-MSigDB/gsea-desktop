/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.prefs;

import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;
import edu.mit.broad.genome.swing.fields.GStringField;

/**
 * @author Aravind Subramanian
 */
public class StringPreference extends AbstractPreference {

    private GStringField fField;

    protected StringPreference(String name, String desc, String def, boolean isDebug, boolean needsRestart) {
        super(name, desc, def, isDebug, needsRestart);
    }

    public Object getValue() {
        return kPrefs.get(getName(), getDefault().toString());
    }

    public String getString() {
        return getValue().toString();
    }

    public GFieldPlusChooser getSelectionComponent() {
        if (fField == null) {
            fField = new GStringField();
        }

        fField.setValue(getValue());

        //klog.debug("Getting component");
        return fField;
    }

    public void setValue(Object value) {
        kPrefs.put(getName(), value.toString());
    }

} // End FilePreference

