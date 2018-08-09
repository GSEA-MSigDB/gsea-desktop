/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.prefs;

import edu.mit.broad.genome.swing.fields.GCheckBoxField;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;

import javax.swing.*;

/**
 * @author Aravind Subramanian
 */
public class BooleanPreference extends AbstractPreference {

    private GCheckBoxField fField;

    /**
     * @param name
     * @param desc
     * @param def
     */
    protected BooleanPreference(final String name,
                                final String desc,
                                final boolean def,
                                final boolean isDebug,
                                final boolean needsRestart) {
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

    public GFieldPlusChooser getSelectionComponent() {
        if (fField == null) {
            JCheckBox cx = new JCheckBox("", ((Boolean) getValue()).booleanValue());
            fField = new GCheckBoxField(cx);
        }

        //klog.debug("Getting component");
        fField.setValue(getValue());
        return fField;
    }


    public void setValue(final Object value) throws Exception {
        kPrefs.putBoolean(getName(), ((Boolean) value).booleanValue());
    }

    public void setValueOfPref2SelectionComponentValue() {
        if (fField != null) {
            super._setValueOfPref2SelectionComponentValue(fField.getValue());
        }
    }

} // End BooleanPreference

