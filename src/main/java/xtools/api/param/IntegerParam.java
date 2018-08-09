/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.swing.fields.GComboBoxField;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class IntegerParam extends AbstractParam implements ActionListener {

    private GComboBoxField cbOptions;

    /**
     * Class constructor
     *
     * @param name
     * @param englishName
     * @param desc
     * @param def_andonly_hint
     * @param reqd
     */
    public IntegerParam(final String name, final String englishName, final String desc, int def_andonly_hint, boolean reqd) {
        super(name, englishName, Integer.class, desc, new Integer(def_andonly_hint), reqd);
    }

    /**
     * Class constructor
     *
     * @param name
     * @param englishName
     * @param desc
     * @param def_andonly_hint
     * @param reqd
     * @param type
     */
    public IntegerParam(String name, String englishName, String desc, int def_andonly_hint, boolean reqd, Param.Type type) {
        super(name, englishName, Integer.class, desc, new Integer(def_andonly_hint), reqd, type);
    }

    public IntegerParam(String name, String englishName, String desc, int def, int[] hintsanddef, boolean reqd) {
        super(name, englishName, Integer.class, desc, new Integer(def), toInts(hintsanddef), reqd);
    }

    public boolean isFileBased() {
        return false;
    }

    public void setValue(Object value) {
        //log.debug("#####Setting IntegerParam to: " + value);

        if (value == null) {
            super.setValue(value);
        } else if (value instanceof Integer) {
            super.setValue(value);
        } else if (value instanceof String) {
            if (value.toString().length() == 0) {
                super.setValue(new Integer(0));
            } else {
                super.setValue(new Integer(((String) value)));
            }
        } else {
            throw new IllegalArgumentException("Invalid type, only Integer accepted. Specified: "
                    + value + " class: " + value.getClass());
        }
    }

    public int getIValue() {

        Object val = super.getValue();

        if (val == null) {
            throw new NullPointerException("Null param value. Always check isSpecified() before calling");
        }

        return ((Integer) val).intValue();
    }

    public GFieldPlusChooser getSelectionComponent() {

        if (cbOptions == null) {
            cbOptions = ParamHelper.createActionListenerBoundHintsComboBox(true, this, this);
        }

        return cbOptions;
    }

    public void actionPerformed(ActionEvent evt) {
        this.setValue(((JComboBox) cbOptions.getComponent()).getSelectedItem());
    }
}    // End class IntegerParam
