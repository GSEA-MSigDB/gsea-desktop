/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.viewers;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.models.GeneSetModel;
import edu.mit.broad.genome.objects.GeneSet;

import javax.swing.*;
import java.awt.*;


/**
 * FSet viewer.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class GeneSetViewer extends AbstractViewer {

    public static final String NAME = "GeneSetViewer";

    public static final Icon ICON = JarResources.getIcon("Grp.gif");

    private GeneSet fGeneSet;

    /**
     * Builds a Viewer on specified FSet object
     *
     * @param gset
     */
    public GeneSetViewer(final GeneSet gset) {
        super(NAME, ICON, gset);

        this.fGeneSet = gset;

        jbInit();
    }

    private void jbInit() {
        GeneSetModel model = new GeneSetModel(fGeneSet);
        JTable table = createTable(model, true, true);
        setColumnSize(100, 1, table, false);
        this.setLayout(new BorderLayout());
        this.add(createAlwaysScrollPane(table), BorderLayout.CENTER);
        this.revalidate();

    }

    public JMenuBar getJMenuBar() {
        return EMPTY_MENU_BAR;
    }

}        // End FSetViewer
