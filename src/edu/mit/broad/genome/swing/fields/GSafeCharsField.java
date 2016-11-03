/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing.fields;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * A JTextField subclass that accepts only "safe" chars
 * <p/>
 * This class needs a better impl.
 * <p/>
 * safe chars are: all letters, digits, .. see list below
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class GSafeCharsField extends JTextField implements GFieldPlusChooser {

    /**
     * Class Constructor.
     * Constructs a new GSafeCharsField.  A default model is created, the initial
     * string is null, and the number of columns is set to 0.
     */
    public GSafeCharsField() {
        super();
    }

    /**
     * Class Constructor.
     * <p/>
     * Constructs a new GSafeCharsField initialized with the specified Integer.
     * A default model is created and the number of columns is 0.
     *
     * @param s the String to be displayed, or null
     */
    public GSafeCharsField(String s) {
        super(s);
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

    private char[] fAddSafeChars;

    public void addSafeChars(char[] cs) {
        this.fAddSafeChars = cs;
    }

    /**
     * Does the string-enforcement.
     */
    public void processKeyEvent(KeyEvent ev) {

        char c = ev.getKeyChar();

        /*
        if (Character.isWhitespace(c)) {
            ev.consume();
            return;
        }

        if (Character.isSpaceChar(c)) {
            ev.consume();
            return;
        }

        if (Character.isISOControl(c)) {
            ev.consume();
            return;
        }
        */
        boolean proceed = false;

        if (Character.isLetterOrDigit(c)) {
            proceed = true;
        }

        if ((c == ',') || (c == '-') || (c == '_') || (c == '<') || (c == '>') || (c == '.') || (c == '/') || (c == '\\') || (c == '=') || (c == ' ')) {
            proceed = true;
        }

        if (fAddSafeChars != null) {
            for (int i = 0; i < fAddSafeChars.length; i++) {
                if (c == fAddSafeChars[i]) {
                    proceed = true;
                    break;
                }
            }
        }

        if (Character.isISOControl(c)) { // for delete
            proceed = true;
        }

        if ((ev.getKeyCode() == KeyEvent.VK_LEFT) || (ev.getKeyCode() == KeyEvent.VK_RIGHT)) {
            proceed = true;
        }

        if ((c == '-') && (getDocument().getLength() > 0)) {
            ev.consume();
        } else {

            if (proceed == true) {
                super.processKeyEvent(ev);
            } else {
                return;
            }
        }

        /*
        if((c == '-') && (getDocument().getLength() > 0)) {
            ev.consume();
        } else {
            super.processKeyEvent(ev);
        }
        */
    }
}    // End IntegerTextField
