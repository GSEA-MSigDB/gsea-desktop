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
 * Object to capture a boolean parameter
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class BooleanParam extends AbstractParam implements ActionListener {

    private GComboBoxField cbOptions;

    public BooleanParam(String name, String desc, boolean reqd) {
        super(name, Boolean.class, desc, new Boolean(reqd), // the default
                new Boolean[]{Boolean.TRUE, Boolean.FALSE}, reqd);
    }

    public BooleanParam(String name, String nameEnglish, String desc, boolean def, boolean reqd) {
        super(name, nameEnglish, Boolean.class, desc, new Boolean(def), new Boolean[]{Boolean.TRUE,
                Boolean.FALSE}, reqd);
    }

    public BooleanParam(String name, String nameEnglish, String desc, boolean def, boolean reqd, Param.Type type) {
        super(name, nameEnglish, Boolean.class, desc, new Boolean(def), new Boolean[]{Boolean.TRUE,
                Boolean.FALSE}, reqd, type);
    }

    public BooleanParam(String name, String desc, boolean def, boolean reqd) {
        super(name, Boolean.class, desc, new Boolean(def), new Boolean[]{Boolean.TRUE,
                Boolean.FALSE}, reqd);
    }

    public void setValue(Object value) {

        //log.debug(">>> " + value);
        if (value == null) {
            super.setValue(value);
        } else if (value instanceof Boolean) {
            super.setValue(value);
        } else if (value instanceof String) {
            this.setValue((String) value);
        } else {
            throw new IllegalArgumentException("Invalid type, only Boolean accepted. Specified: "
                    + value + " class: " + value.getClass());
        }
    }

    public void setValue(Boolean value) {
        super.setValue(value);
    }

    public void setValue(String trueorfalse) {
        setValue(Boolean.valueOf(trueorfalse));
    }

    public boolean isFileBased() {
        return false;
    }

    public boolean isTrue() {

        Object val = getValue();

        if (val == null) {
            throw new NullPointerException("Null param value. Always check isSpecified() before calling");
        }

        return ((Boolean) val).booleanValue();

        //return false; // if null -- hmm no
    }

    public boolean isFalse() {

        Object val = getValue();

        if (val == null) {
            throw new NullPointerException("Null param value. Always check isSpecified() before calling");
        }

        boolean valb = ((Boolean) val).booleanValue();
        if (valb == true) {
            return false;
        } else {
            return true;
        }
    }

    public GFieldPlusChooser getSelectionComponent() {

        if (cbOptions == null) {
            cbOptions = ParamHelper.createActionListenerBoundHintsComboBox(false, this, this);
            ParamHelper.safeSelectValueDefaultOrNone(cbOptions.getComboBox(), this);
        }

        return cbOptions;
    }

    public void actionPerformed(ActionEvent evt) {
        this.setValue(((JComboBox) cbOptions.getComponent()).getSelectedItem());
    }
}    // End class BooleanParam
