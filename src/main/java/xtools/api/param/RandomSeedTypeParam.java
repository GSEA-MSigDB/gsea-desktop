/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

import edu.mit.broad.genome.math.RandomSeedGenerator;
import edu.mit.broad.genome.math.RandomSeedGenerators;
import edu.mit.broad.genome.swing.fields.GComboBoxField;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;
import xtools.api.Tool;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Aravind Subramanian, David Eby
 */
public class RandomSeedTypeParam extends AbstractParam implements ActionListener {

    // @maint add a type and this array might need updating
    // @imp note that we return strings and NOT the objects -- see the note in lookup for why
    public static final String[] ALL_RSG = 
        new String[]{ RandomSeedGenerator.TIMESTAMP, Long.toString(RandomSeedGenerator.STANDARD_SEED) };

    private GComboBoxField cbOptions;

    private final Tool tool;
    
    public RandomSeedTypeParam(Tool tool) {
        super(RND_SEED, RND_SEED_ENGLISH, String.class, RND_SEED_DESC, ALL_RSG[0], ALL_RSG, true);
        this.tool = tool;
    }

    public boolean isFileBased() {
        return false;
    }

    public RandomSeedGenerator createSeed() {
        Object value = super.getValueRaw();
        return RandomSeedGenerators.create(value, tool);
    }

    public GFieldPlusChooser getSelectionComponent() {
        if (cbOptions == null) {
            cbOptions = ParamHelper.createActionListenerBoundHintsComboBox(true, this, this);
            ParamHelper.safeSelectValueDefaultByString(cbOptions.getComboBox(), this);
        }

        return cbOptions;
    }

    public void actionPerformed(ActionEvent evt) {
        this.setValue(cbOptions.getComboBox().getSelectedItem());
    }
}
