/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class FeatureSpaceReqdParam extends StringReqdParam implements ActionListener {

    private static String[] MODES = new String[]{"true", "false"};

    /**
     * Class constructor
     *
     * @param name
     * @param type
     * @param desc
     */
    public FeatureSpaceReqdParam() {
        super(FEATURE_SPACE, FEATURE_SPACE_ENGLISH, FEATURE_SPACE_DESC, MODES[0], MODES);
    }

    public boolean isSymbols() {
        int index = getStringIndexChoosen();
        if (index == 0) {
            return true;
        } else {
            return false;
        }
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

            //System.out.println("index=" + index);

            if (index == 0) {
                //this.setText("foo");
                this.setText(MODES[0] + " (use 'chip' to collapse dataset to symbols before analysis)");
            } else if (index == 1) {
                //this.setText("bar");
                this.setText(MODES[1] + " (use dataset 'as is' in the original format)");
            } // @note IMP to do specifically for 0 and 1 sometimes gets -1

            return this;
        }
    }    // End CommonLookListRenderer


}    // End class FeatureSpaceReqdParam
