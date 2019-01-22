/*******************************************************************************
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.prefs;

import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;
import edu.mit.broad.genome.swing.fields.GIntegerField;
import edu.mit.broad.genome.utils.NamedInteger;

/**
 * @author Aravind Subramanian
 */
public class IntPreference extends AbstractPreference {

    protected GFieldPlusChooser fField;

    /**
     * @param name
     * @param desc
     * @param def
     */
    protected IntPreference(String name, String desc, int def, boolean isDebug, boolean needsRestart) {
        super(name, desc, new Integer(def), isDebug, needsRestart);
    }

    public Object getValue() {
        return new Integer(kPrefs.getInt(getName(), ((Integer) getDefault()).intValue()));
    }

    public int getInt() {
        return ((Integer) getValue()).intValue();
    }

    public GFieldPlusChooser getSelectionComponent() {
        if (fField == null) {
            fField = new GIntegerField(getInt(), 10);
        }

        fField.setValue(getValue());
        return fField;
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

    public void setValueOfPref2SelectionComponentValue() {
        if (fField != null) {
            super._setValueOfPref2SelectionComponentValue(fField.getValue());
        }
    }

} // End IntPreference
