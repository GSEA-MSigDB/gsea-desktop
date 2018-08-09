/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing.fields;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.event.KeyEvent;
import java.io.File;

/**
 * A JTextField subclass that behaves as though a file location is being type in.
 * Blue -> if correct path and red if incorrect.
 * <p/>
 * Diff from GFileFieldPlusChooser in that there is no button to launch a fcd to select a file
 * (But GFileFieldPlusChooser uses this class internally)
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class GFileField extends JTextField implements GFieldPlusChooser {

    /**
     * Class Constructor.
     * Constructs a new GFileField.  A default model is created, the initial
     * Integer is null, and the number of columns is set to 0.
     */
    public GFileField() {
        super(50);
    }

    public void setText(String text) {
        super.setText(text);
        this.setForeground(GFieldUtils._getColor(text));
    }

    /**
     * @return The user specified File
     */
    public Object getValue() {

        if (this.getText() == null) {
            return null;
        } else {
            return new File(this.getText());
        }
    }

    public void setValue(Object obj) {
        String p;
        if (obj == null) {
            p = "";
        } else if (obj instanceof File) {
            p = ((File) obj).getAbsolutePath();
        } else {
            p = obj.toString();
        }

        this.setText(p);
    }

    public JComponent getComponent() {
        return this;
    }

    /**
     * Does the coloring
     */
    public void processKeyEvent(KeyEvent ev) {

        Document doc = getDocument();

        try {
            String text = getDocument().getText(0, doc.getLength());
            this.setForeground(GFieldUtils.getFileFieldColor(text));
            //ev.consume();
            super.processKeyEvent(ev);
        } catch (javax.swing.text.BadLocationException e) {
            super.processKeyEvent(ev);
        }
    }
}    // End GFileField
