/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.prefs;

import edu.mit.broad.genome.swing.fields.GComboBoxField;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;
import edu.mit.broad.genome.utils.NamedInteger;

import javax.swing.*;

/**
 * Where to place the tabs
 */
public class LinuxBrowserPreference extends IntPreference {

    /**
     * @param name
     * @param desc
     * @param def
     */
    protected LinuxBrowserPreference(String name, String desc, int def) {
        super(name, desc, def, false, false);
    }

    public GFieldPlusChooser getSelectionComponent() {
        if (fField == null) {
            fField = createOptions(getInt());
        }

        fField.setValue(getValue());
        return fField;
    }

    public static GComboBoxField createOptions(int def) {

        final NamedInteger[] options = new NamedInteger[]{
                new NamedInteger(1, "mozilla"),
                new NamedInteger(0, "netscape"),
        };
        JComboBox cb = new JComboBox(new DefaultComboBoxModel(options));

        for (int i = 0; i < options.length; i++) {
            if (options[i].getValue() == def) {
                cb.setSelectedIndex(i);
                break;
            }
        }

        return new GComboBoxField(cb);
    }

} // End TabPlacementPreference

