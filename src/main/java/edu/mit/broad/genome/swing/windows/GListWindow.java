/*
 * Copyright (c) 2003-2023 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.swing.windows;

import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.core.api.DialogDescriptor;

import javax.swing.*;

import java.awt.*;

/**
 * JList that pops up in a new window by itself with 2 buttons - cancel and accept
 *
 * @author Aravind Subramanian
 */
public class GListWindow {
    private final JList jlOptions = new JList();
    private DefaultListModel fModel = new DefaultListModel();
    private int fSelectionMode = ListSelectionModel.SINGLE_SELECTION;

    private Action fHelp_action_opt;

    /**
     * comp on north of the dialog desc box optional
     */
    private JComponent fNorthComponent;

    public GListWindow(final Action help_action_opt) {
        this.fHelp_action_opt = help_action_opt;
    }

    public JList getJList() {
        return jlOptions;
    }

    /**
     * selectedOnes can be null
     *
     * @param options
     * @param selectedOnes
     * @return
     */
    public Object[] show(Object[] options, Object[] selectedOnes) {
        fillModel(options);
        return _show(selectedOnes);
    }

    /**
     * Model must already filled
     *
     * @return
     */
    public Object[] show() {
        return _show(new Object[]{});
    }

    /**
     * @return value selected or null is user cancelled
     */
    private Object[] _show(final Object[] selectedOnes) {
        // carefull with rebuild / reset the model here -> that ruins the selection policy
        jlOptions.setModel(fModel);
        jlOptions.setSelectionMode(fSelectionMode);

        GuiHelper.List2.setSelected(selectedOnes, jlOptions, fModel);

        // just the showing part, abs no setting data
        String text = "Select an option";

        if (fSelectionMode == ListSelectionModel.MULTIPLE_INTERVAL_SELECTION) {
            text = "Select one or more option(s)";
        }

        DialogDescriptor desc;

        if (fNorthComponent == null) {
            desc = Application.getWindowManager().createDialogDescriptor(text, new JScrollPane(jlOptions), fHelp_action_opt);
        } else {
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(fNorthComponent, BorderLayout.NORTH);
            panel.add(new JScrollPane(jlOptions), BorderLayout.CENTER);
            desc = Application.getWindowManager().createDialogDescriptor(text, panel, fHelp_action_opt);
        }

        desc.enableDoubleClickableJList(jlOptions);
        int res = desc.show();

        if (res == DialogDescriptor.CANCEL_OPTION) {
            return null;
        } else {
            return jlOptions.getSelectedValuesList().toArray();    // @note value*s*
        }

    }

    private void fillModel(Object[] options) {
        fModel = new DefaultListModel();

        for (int i = 0; i < options.length; i++) {
            fModel.addElement(options[i]);
        }
    }

    /**
     * @param mode one of the ListSelectionModel constants
     */
    public void setListSelectionMode(int mode) {

        this.fSelectionMode = mode;

        jlOptions.setSelectionMode(fSelectionMode);
    }
}
