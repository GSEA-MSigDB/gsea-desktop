/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.objects.strucs.TemplateRandomizerType;
import edu.mit.broad.genome.swing.fields.GComboBoxField;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Object to capture RandomizerType in commandline params</p>
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class TemplateRandomizerTypeParam extends AbstractParam implements ActionListener {

    private GComboBoxField cbOptions;

    /**
     * Class constructor
     *
     * @param def
     * @param options
     * @param reqd
     */
    public TemplateRandomizerTypeParam(final TemplateRandomizerType def,
                                       final TemplateRandomizerType[] options,
                                       final boolean reqd) {
        super(RNDTYPE, RNDTYPE_ENGLISH, TemplateRandomizerType.class, RNDTYPE_DESC,
                def, options, reqd);
    }

    public void setValue(final Object value) {

        if (value == null) {
            super.setValue(null);
        } else {
            super.setValue(TemplateRandomizerType.lookupRandomizerType(value));
        }
    }

    public void setValue(final TemplateRandomizerType rt) {
        super.setValue(rt);
    }

    public TemplateRandomizerType getRandomizerType() {

        Object val = super.getValue();

        if (val == null) {
            throw new NullPointerException("Null param value. Always check isSpecified() before calling");
        }

        return (TemplateRandomizerType) val;
    }


    public GFieldPlusChooser getSelectionComponent() {

        if (cbOptions == null) {
            cbOptions = ParamHelper.createActionListenerBoundHintsComboBox(false, this, this);
            ParamHelper.safeSelectValueDefaultByString(cbOptions.getComboBox(), this);
        }

        return cbOptions;

    }

    public void actionPerformed(ActionEvent evt) {
        this.setValue((TemplateRandomizerType) ((JComboBox) cbOptions.getComponent()).getSelectedItem());

    }

    public boolean isFileBased() {
        return false;
    }

}    // End class RandomizerTypeParam

