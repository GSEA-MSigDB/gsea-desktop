/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.parsers.DataFormat;
import edu.mit.broad.genome.swing.fields.GComboBoxField;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public abstract class DataFormatAbstractParam extends AbstractParam implements ActionListener {

    private GComboBoxField cbOptions;

    /**
     * Class constructor
     *
     * @param def
     * @param reqd
     */
    public DataFormatAbstractParam(String name, String nameEnglish, String desc, DataFormat def, DataFormat[] hints, boolean reqd) {
        super(name, nameEnglish, DataFormat.class, desc,
                def, hints, reqd);
    }

    public void setValue(Object value) {

        if (value == null) {
            super.setValue(null);
        } else {
            super.setValue(DataFormat.getExtension(value));
        }
    }

    public void setValue(DataFormat df) {
        super.setValue(df);
    }

    public boolean isFileBased() {
        return false;
    }

    public DataFormat getDataFormat() {

        Object val = super.getValue();

        if (val == null) {
            return (DataFormat) getDefault(); // @note
        } else {
            return (DataFormat) val;
        }
    }


    public GFieldPlusChooser getSelectionComponent() {

        if (cbOptions == null) {
            cbOptions = ParamHelper.createActionListenerBoundHintsComboBox(false, this, this);
            ParamHelper.safeSelectValueDefaultByString(cbOptions.getComboBox(), this);
        }

        return cbOptions;

    }

    public void actionPerformed(ActionEvent evt) {
        this.setValue((DataFormat) ((JComboBox) cbOptions.getComponent()).getSelectedItem());
    }


}    // End class AbstractDataFormatParam
