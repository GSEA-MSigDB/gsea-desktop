/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.math.SortMode;
import edu.mit.broad.genome.swing.fields.GComboBoxField;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * <p> Object to capture commandline params</p>
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class SortParam extends AbstractParam implements ActionListener {

    private GComboBoxField cbOptions;

    /**
     * Class constructor
     *
     * @param name
     * @param desc
     */
    public SortParam(boolean reqd) {
        super(SORT, SORT_ENGLISH, SortMode.class, SORT_DESC, SortMode.ALL, reqd);
    }

    public void setValue(Object value) {

        if (value == null) {
            super.setValue(null);
        } else {
            super.setValue(SortMode.lookup(value));
        }
    }

    public void setValue(SortMode sort) {
        super.setValue(sort);
    }

    public SortMode getMode() {

        Object val = super.getValue();

        if (val == null) {
            throw new NullPointerException("Null param value. Always check isSpecified() before calling");
        }

        return (SortMode) val;
    }

    public GFieldPlusChooser getSelectionComponent() {

        if (cbOptions == null) {
            cbOptions = ParamHelper.createActionListenerBoundHintsComboBox(false, this, this);
            ParamHelper.safeSelectValueDefaultByString(cbOptions.getComboBox(), this);
        }

        return cbOptions;
    }

    public void actionPerformed(ActionEvent evt) {
        this.setValue((SortMode) ((JComboBox) cbOptions.getComponent()).getSelectedItem());
    }

    public boolean isFileBased() {
        return false;
    }
}    // End class SortParam
