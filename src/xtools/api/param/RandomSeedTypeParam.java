/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.math.RandomSeedGenerator;
import edu.mit.broad.genome.math.RandomSeedGenerators;
import edu.mit.broad.genome.swing.fields.GComboBoxField;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class RandomSeedTypeParam extends AbstractParam implements ActionListener {

    private GComboBoxField cbOptions;

    /**
     * Class constructor
     *
     * @param name
     * @param desc
     */
    public RandomSeedTypeParam(boolean reqd) {
        this(createAll(), reqd);
    }

    public RandomSeedTypeParam(String[] rsts, boolean reqd) {
        super(RND_SEED, RND_SEED_ENGLISH, String.class, RND_SEED_DESC,
                rsts[0], rsts, reqd);
    }

    public boolean isFileBased() {
        return false;
    }

    public void setValue(Object value) {

        if (value == null) {
            super.setValue(null);
        } else {
            super.setValue(RandomSeedGenerators.lookup(value));
        }
    }

    public RandomSeedGenerator createSeed() {

        Object val = super.getValue();

        if (val == null) {
            throw new NullPointerException("Null param value. Always check isSpecified() before calling");
        }

        return RandomSeedGenerators.lookup(val);
    }

    public GFieldPlusChooser getSelectionComponent() {

        if (cbOptions == null) {
            cbOptions = ParamHelper.createActionListenerBoundHintsComboBox(true, this, this);
            ParamHelper.safeSelectValueDefaultByString(cbOptions.getComboBox(), this);
        }

        return cbOptions;

    }

    public void actionPerformed(ActionEvent evt) {
        this.setValue(((JComboBox) cbOptions.getComponent()).getSelectedItem());
    }

    // @maint add a type and this array might need updating
    // @imp note that we return strings and NOT the objects -- see the note in lookup for why
    public static String[] createAll() {

        return new String[]{
                new RandomSeedGenerators.Timestamp().toString(),
                new RandomSeedGenerators.Standard().toString(),
        };
    }

}    // End class RandomSeedTypeParam
