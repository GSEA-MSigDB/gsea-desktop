/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing.fields;

import javax.swing.*;

/**
 * Turns a JComboBox into a GFieldPlusChooser
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */

public class GComboBoxField implements GFieldPlusChooser {

    private final JComboBox fBox;

    /**
     * @param box
     */
    public GComboBoxField(JComboBox box) {
        this.fBox = box;
    }

    public Object getValue() {

        return fBox.getSelectedItem();
    }

    public JComponent getComponent() {
        return fBox;
    }

    public JComboBox getComboBox() {
        return fBox;
    }

    public void setValue(Object obj) {
        fBox.setSelectedItem(obj);
    }

} // End GComboBoxField