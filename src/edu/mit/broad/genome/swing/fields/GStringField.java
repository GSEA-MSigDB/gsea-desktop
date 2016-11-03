/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing.fields;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * A JTextField subclass that accepts only letters and "."
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
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

    /**
     * Does the string-enforcement.
     */
    public void processKeyEvent(KeyEvent ev) {

        char c = ev.getKeyChar();

        if (Character.isDigit(c) && !ev.isAltDown()) {
            ev.consume();

            return;
        }

        /*
        if((Character.isDigit(c) && !ev.isAltDown()) || Strings.badchars().pattern().indexOf(c) > -1) {
            ev.consume();
            return;
        }
        */
        if ((c == '-') && (getDocument().getLength() > 0)) {
            ev.consume();
        } else {
            super.processKeyEvent(ev);
        }
    }
}    // End IntegerTextField
