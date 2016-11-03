/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.viewers;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.models.GeneSetMatrixModel2;
import edu.mit.broad.genome.objects.GeneSetMatrix;

import javax.swing.*;

import au.com.pegasustech.demos.layout.PointLayout;

/**
 * Widget that builds the local file system view along with a few
 * easy access buttons
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class GeneSetMatrixViewer2 extends AbstractViewer {

    public static final String NAME = "GeneSetMatrixViewer2";
    public static final Icon ICON = JarResources.getIcon("Gmx.png");

    private final GeneSetMatrix fGmx;

    /**
     * Class constructor
     *
     * @param gmx
     */
    public GeneSetMatrixViewer2(final GeneSetMatrix gmx) {
        super(NAME, ICON, gmx);

        this.fGmx = gmx;

        init();
    }

    private void init() {
        GeneSetMatrixModel2 gmodel = new GeneSetMatrixModel2(fGmx);
        JTable table = createTable(gmodel, true, true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        this.setLayout(new PointLayout());
        JTabbedPane tp = new JTabbedPane(JTabbedPane.BOTTOM);
        JScrollPane sp = createAlwaysScrollPane(table);
        tp.addTab("Data", sp);
        tp.addTab("Info", getGeneSetMatrixInfo(fGmx));
        this.add(tp);
    }

    private JComponent getGeneSetMatrixInfo(final GeneSetMatrix gmx) {
        StringBuffer buf = new StringBuffer("Name: ").append(gmx.getName()).append('\n');
        buf.append("Number of sets: ").append(gmx.getNumGeneSets()).append('\n');
        buf.append("Total number of unique features: ").append(gmx.getAllMemberNamesOnlyOnceS().size()).append('\n');

        String comm = gmx.getComment();
        if ((comm != null) && (comm.length() > 0)) {
            buf.append("\nComments\n");
            buf.append(gmx.getComment());
        }

        buf.append("\nGene Set Names\n");

        for (int i = 0; i < gmx.getNumGeneSets(); i++) {
            buf.append(gmx.getGeneSet(i).getName()).append('\t').append(gmx.getGeneSet(i).getNumMembers()).append('\n');
        }

        JTextArea ta = new JTextArea(buf.toString());

        return new JScrollPane(ta);
    }

}    // End GeneSetMatrixViewer2
