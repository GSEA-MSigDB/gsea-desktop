/*
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.swing.fields;

import javax.swing.*;

/**
 * A plain, unrestricted single-line text field for use as a {@link GFieldPlusChooser}.
 *
 * @author Aravind Subramanian
 */
public class GStringField extends JTextField implements GFieldPlusChooser {

    /**
     * Class Constructor.
     * Constructs a new GStringField.  A default model is created, the initial
     * string is null, and the number of columns is set to 0.
     */
    public GStringField() {
        super();
    }

    /**
     * @return The user specified String
     */
    public Object getValue() {
        return this.getText();
    }

    public void setValue(Object obj) {

        if (obj == null) {
            super.setText(null);
        } else {
            super.setText(obj.toString());
        }

    }

    public JComponent getComponent() {
        return this;
    }
}
