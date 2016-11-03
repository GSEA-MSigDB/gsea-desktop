/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.core.api.DialogDescriptor;
import xtools.api.ui.NamedModel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class WChipChooserWindow {

    private JList[] jlChips;

    private NamedModel[] fModels;

    private int fSelectionMode = ListSelectionModel.SINGLE_SELECTION;


    /**
     * Class Constructor.
     */
    public WChipChooserWindow() {
    }


    /**
     * Model must already filled
     *
     * @return
     */
    public Object[] show() {

        if (jlChips == null) {
            return null;
        }

        // carefull with rebuild / reset the model here -> that ruins the selection policy
        for (int i = 0; i < jlChips.length; i++) {
            jlChips[i].setModel(fModels[i].model);
            jlChips[i].setSelectionMode(fSelectionMode);
        }

        return _just_show();
    }


    public Object[] showDirectlyWithModels(final NamedModel[] models,
                                           final int selMode,
                                           final DefaultListCellRenderer rend) {
        this.fModels = models;

        // carefull with rebuild / reset the model here -> that ruins the selection policy
        if (jlChips == null) {
            jlChips = new JList[models.length];
            for (int i = 0; i < models.length; i++) {
                jlChips[i] = new JList();
                jlChips[i].setCellRenderer(rend);
            }
        }

        this.fSelectionMode = selMode;
        for (int i = 0; i < models.length; i++) {
            jlChips[i].setModel(models[i].model);
            jlChips[i].setSelectionMode(selMode);
        }

        return _just_show();
    }

    private JTabbedPane tab;

    // just the showing part, abs no setting data
    private Object[] _just_show() {

        String text = "Select a chip";

        if (fSelectionMode == ListSelectionModel.MULTIPLE_INTERVAL_SELECTION) {
            text = "Select one or more chip(s)";
        }


        if (tab == null) {
            tab = new JTabbedPane();
            for (int i = 0; i < jlChips.length; i++) {
                tab.addTab(fModels[i].name, new JScrollPane(jlChips[i]));
            }

        }

        JPanel dummy = new JPanel(new BorderLayout()); // @note needed else the input widget comes up real small in the dd
        dummy.add(tab, BorderLayout.CENTER);

        DialogDescriptor desc = Application.getWindowManager().createDialogDescriptor(text, dummy, JarResources.createHelpAction(Param.GMX));
        for (int i = 0; i < jlChips.length; i++) {
            desc.enableDoubleClickableJList(jlChips[i]);
        }
        int res = desc.show();
        if (res == DialogDescriptor.CANCEL_OPTION) {
            return null;
        } else {

            java.util.List allValues = new ArrayList();

            for (int j = 0; j < jlChips.length; j++) {
                Object[] sels = jlChips[j].getSelectedValues();
                if (sels != null) {
                    for (int i = 0; i < sels.length; i++) {
                        if (sels[i] != null) {
                            allValues.add(sels[i]);
                        }
                    }
                }
            }

            return allValues.toArray(new Object[allValues.size()]);
        }
    }

}        // End GListWindow
