/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.ui;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.objects.FSet;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.parsers.ParseUtils;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.core.api.DialogDescriptor;
import org.apache.log4j.Logger;
import xtools.api.param.Param;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class GeneSetMatrixChooserWindow {

    private static final Logger klog = Logger.getLogger(GeneSetMatrixChooserWindow.class);
    private JList[] jlGenes;

    private final JTextArea taGenes = new JTextArea();

    private NamedModel[] fModels;
    private int fSelectionMode = ListSelectionModel.SINGLE_SELECTION;


    /**
     * Class Constructor.
     */
    public GeneSetMatrixChooserWindow() {
    }


    /**
     * Model must already filled
     *
     * @return
     */
    public Object[] show() {

        if (jlGenes == null) {
            return null;
        }

        // carefull with rebuild / reset the model here -> that ruins the selection policy
        for (int i = 0; i < jlGenes.length; i++) {
            jlGenes[i].setModel(fModels[i].model);
            jlGenes[i].setSelectionMode(fSelectionMode);
        }

        for (int i = 0; i < jlGenes.length; i++) {
            //GuiHelper.List2.setSelected(selectedOnes, jlGeneMatrices, fModel);
        }

        return _just_show();
    }


    public Object[] showDirectlyWithModels(final NamedModel[] models,
                                           final int selMode,
                                           final DefaultListCellRenderer rend) {
        this.fModels = models;

        // carefull with rebuild / reset the model here -> that ruins the selection policy
        if (jlGenes == null) {
            jlGenes = new JList[models.length];
            for (int i = 0; i < models.length; i++) {
                jlGenes[i] = new JList();
                jlGenes[i].setCellRenderer(rend);
            }
        }

        this.fSelectionMode = selMode;
        for (int i = 0; i < models.length; i++) {
            jlGenes[i].setModel(models[i].model);
            jlGenes[i].setSelectionMode(selMode);
        }

        return _just_show();
    }

    private JTabbedPane tab;

    // just the showing part, abs no setting data
    private Object[] _just_show() {

        String text = "Select a gene set";

        if (fSelectionMode == ListSelectionModel.MULTIPLE_INTERVAL_SELECTION) {
            text = "Select one or more gene sets(s)";
        }

        taGenes.setText(""); // @note
        taGenes.setBorder(BorderFactory.createTitledBorder("Make an 'on-the-fly' gene set: Enter features below, one per line"));

        if (tab == null) {
            tab = new JTabbedPane();
            for (int i = 0; i < jlGenes.length; i++) {
                tab.addTab(fModels[i].name, new JScrollPane(jlGenes[i]));
            }

            tab.addTab("Text entry", new JScrollPane(taGenes));
        }

        JPanel dummy = new JPanel(new BorderLayout()); // @note needed else the input widget comes up real small in the dd
        dummy.add(tab, BorderLayout.CENTER);

        DialogDescriptor desc = Application.getWindowManager().createDialogDescriptor(text, dummy, JarResources.createHelpAction(Param.GMX), true);
        for (int i = 0; i < jlGenes.length; i++) {
            desc.enableDoubleClickableJList(jlGenes[i]);
        }
        int res = desc.show();
        if (res == DialogDescriptor.CANCEL_OPTION) {
            return null;
        } else {

            java.util.List allValues = new ArrayList();

            for (int j = 0; j < jlGenes.length; j++) {
                Object[] sels = jlGenes[j].getSelectedValues();
                if (sels != null) {
                    for (int i = 0; i < sels.length; i++) {
                        if (sels[i] != null) {
                            allValues.add(sels[i]);
                        }
                    }
                }
            }

            // add text are stuff as a gene set
            String s = taGenes.getText();

            if (s != null) {
                String[] strs = ParseUtils.string2strings(s, "\t\n", false); // we want things synched
                if (strs.length != 0) {
                    GeneSet gset = new FSet("from_text_entry_", strs);
                    try {
                        ParserFactory.save(gset, File.createTempFile(gset.getName(), ".grp"));
                    } catch (Throwable t) {
                        klog.error(t);
                    }
                    allValues.add(gset);
                }
            }
            return allValues.toArray(new Object[allValues.size()]);
        }
    }

}        // End GListWindow

