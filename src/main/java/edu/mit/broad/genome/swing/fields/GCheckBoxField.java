/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing.fields;

import javax.swing.*;

/**
 * Turns a JCheckBox into a GFieldPlusChooser
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */

public class GCheckBoxField implements GFieldPlusChooser {

    private final JCheckBox fCx;

    /**
     * Class Constructor.
     */
    public GCheckBoxField(final JCheckBox cx) {
        this.fCx = cx;
        fCx.setHorizontalAlignment(SwingUtilities.CENTER);
    }

    public Object getValue() {
        return Boolean.valueOf(fCx.isSelected());
    }

    public JComponent getComponent() {
        return fCx;
    }

    public void setValue(Object obj) {

        boolean b;

        if (obj == null) {
            b = false;
        } else if (obj instanceof Boolean) {
            b = ((Boolean) obj).booleanValue();
        } else {
            b = Boolean.valueOf(obj.toString()).booleanValue();
        }

        fCx.setSelected(b);
    }

} // End GCheckBoxField
