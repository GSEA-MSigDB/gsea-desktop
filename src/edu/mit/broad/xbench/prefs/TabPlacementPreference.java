/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.prefs;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTabbedPane;

import edu.mit.broad.genome.swing.fields.GComboBoxField;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;
import edu.mit.broad.genome.utils.NamedInteger;

/**
 * Where to place the tabs
 */
public class TabPlacementPreference extends IntPreference {

    public static GComboBoxField createTabPlacementField(int def) {

        NamedInteger[] options = new NamedInteger[]{
                new NamedInteger(JTabbedPane.TOP, "Top"),
                new NamedInteger(JTabbedPane.BOTTOM, "Bottom"),
                new NamedInteger(JTabbedPane.LEFT, "Left"),
                new NamedInteger(JTabbedPane.RIGHT, "Right")
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

    /**
     * @param name
     * @param desc
     * @param def
     */
    protected TabPlacementPreference(String name, String desc, int def) {
        super(name, desc, def, false, true);
    }

    public GFieldPlusChooser getSelectionComponent() {
        if (fField == null) {
            fField = createTabPlacementField(getInt());
        }

        fField.setValue(getValue());
        return fField;
    }

} // End TabPlacementPreference

