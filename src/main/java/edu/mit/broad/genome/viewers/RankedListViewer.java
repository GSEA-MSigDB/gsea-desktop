/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.viewers;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.models.RankedListModel;
import edu.mit.broad.genome.objects.RankedList;

import javax.swing.*;
import java.awt.*;

/**
 * FSet viewer.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class RankedListViewer extends AbstractViewer {

    public static final String NAME = "RankedListViewer";
    public static final Icon ICON = JarResources.getIcon("Rnk.png");
    private RankedList fRankedList;

    /**
     * Builds a Viewer on specified FSet object
     *
     * @param rl
     */
    public RankedListViewer(final RankedList rl) {
        super(NAME, ICON, rl);

        this.fRankedList = rl;

        jbInit();
    }

    private void jbInit() {
        RankedListModel model = new RankedListModel(fRankedList);
        JTable table = createTable(model, false, true);
        //setColumnSize(100, 1, table, false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        this.setLayout(new BorderLayout());
        this.add(createAlwaysScrollPane(table), BorderLayout.CENTER);
        this.revalidate();

    }

    public JMenuBar getJMenuBar() {
        return EMPTY_MENU_BAR;
    }

}        // End RankedListViewer
