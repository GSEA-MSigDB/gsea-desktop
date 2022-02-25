/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionListener;

/**
 * @author Aravind Subramanian
 */
public class FeatureSpaceReqdParam extends StringReqdParam implements ActionListener {
    // TODO: switch to an Enum rather than hard-coded Strings
    private static String[] MODES = new String[]{"Remap_Only", "Collapse", "No_Collapse"};

    public FeatureSpaceReqdParam() {
        super(FEATURE_SPACE, FEATURE_SPACE_ENGLISH, FEATURE_SPACE_DESC, MODES[1], MODES);
    }

    public FeatureSpaceReqdParam(String def) {
        super(FEATURE_SPACE, FEATURE_SPACE_ENGLISH, FEATURE_SPACE_DESC, def, MODES);
    }

    // Special case for legacy use: accept "true" for "Collapse" and "false" for "No_Collapse"
    @Override
    public void setValue(Object value) {
        if ("true".equals(value)) {
            value = "Collapse";
        }
        if ("false".equals(value)) {
            value = "No_Collapse";
        }
        super.setValue(value);
    }
    
    public boolean isSymbols() {
        int index = getStringIndexChoosen();
        return (index <= 1);
    }
    
    public boolean isRemap() {
        int index = getStringIndexChoosen();
        return (index == 0);
    }

    public GFieldPlusChooser getSelectionComponent() {

        if (cbOptions == null) {

            cbOptions = ParamHelper.createActionListenerBoundHintsComboBox(false, this, this);
            ParamHelper.safeSelectValueDefaultByString(cbOptions.getComboBox(), this);
            cbOptions.getComboBox().setRenderer(new MyListRenderer());
        }

        return cbOptions;

    }

    public static class MyListRenderer extends DefaultListCellRenderer {

        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {

            // doesnt work properly unless called
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (index == 0) {
                this.setText(MODES[0] + " (remap dataset with 'chip' without mathematical collapse)");
            } else if (index == 1) {
                this.setText(MODES[1] + " (use 'chip' to collapse dataset to symbols before analysis)");
            } else if (index == 2) {
                this.setText(MODES[2] + " (use dataset 'as is' in the original format)");
            } // @note IMP to do specifically for 0 and 1 sometimes gets -1

            return this;
        }
    }
}
