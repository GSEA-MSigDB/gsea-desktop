/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.viewers;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.models.DatasetModel;
import edu.mit.broad.genome.objects.Dataset;

import javax.swing.*;

import au.com.pegasustech.demos.layout.PointLayout;

/**
 * Widget that builds the local file system view along with a few
 * easy access buttons
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class DatasetViewer extends AbstractViewer {

    public static final String NAME = "DatasetViewer";
    public static final Icon ICON = JarResources.getIcon("Res16.gif");

    private final Dataset fDataset;

    /**
     * Class constructor
     *
     * @param ds
     */
    public DatasetViewer(final Dataset ds) {
        super(NAME, ICON, ds);

        this.fDataset = ds;
        jbInit();
    }

    private void jbInit() {
        DatasetModel dmodel = new DatasetModel(fDataset);
        JTable table = createTable(dmodel, true, true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        this.setLayout(new PointLayout());
        JTabbedPane tp = new JTabbedPane(JTabbedPane.BOTTOM);
        JScrollPane sp = createAlwaysScrollPane(table);
        tp.addTab("Data", sp);
        tp.addTab("Info", getDatasetInfo(fDataset));
        this.add(tp);
    }

    private JComponent getDatasetInfo(Dataset ds) {
        StringBuffer buf = new StringBuffer("Name: ").append(ds.getName()).append('\n');
        buf.append("Number of rows(features): ").append(ds.getNumRow()).append('\n');
        buf.append("Num of columns(samples): ").append(ds.getNumCol()).append('\n');

        // dont expensive for many datasets
        //buf.append("Max Value: ").append(ds.getM)).append('\n');
        String comm = ds.getComment();
        if ((comm != null) && (comm.length() > 0)) {
            buf.append("\nComments\n");
            buf.append(ds.getComment());
        }

        buf.append("\nColumn Names\n");

        for (int c = 0; c < ds.getNumCol(); c++) {
            buf.append(ds.getColumnName(c)).append('\n');
        }

        JTextArea ta = new JTextArea(buf.toString());

        return new JScrollPane(ta);

    }


}    // End DatasetViewer
