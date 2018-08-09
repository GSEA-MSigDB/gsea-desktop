/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.models;

import edu.mit.broad.genome.objects.GeneSet;

import javax.swing.table.AbstractTableModel;

/**
 * An implementation of AbstractTableModel for FSet.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class GeneSetModel extends AbstractTableModel {

    /**
     * The underlying object being modell'ed
     */
    private final GeneSet fGeneSet;

    /**
     * Column labels for the table. Using "Class" rather than "Template" as
     * users are more familiar wit that term - i think.
     * dont set a col header to "" -> causes it to dissapear!
     */
    private static final String[] kColNames = {"Member Name"};

    /**
     * Class Constructor.
     * Initializes model to specified Template.
     */
    public GeneSetModel(GeneSet gset) {
        this.fGeneSet = gset;
    }

    /**
     * Always kColNames.length
     */
    public int getColumnCount() {
        return kColNames.length;
    }

    /**
     * As many rows as there are elements
     */
    public int getRowCount() {
        return fGeneSet.getNumMembers();
    }

    public String getColumnName(int col) {
        return kColNames[col];
    }

    public Object getValueAt(int row, int col) {
        return fGeneSet.getMember(row);
    }

    public Class getColumnClass(int col) {
        return String.class;
    }

    /**
     * Is the model editable or not?
     */
    public boolean isEditable() {
        return false;
    }

    /**
     * Either the entire table model is editable or the entire table is not editable.
     * That property is set using <code>setEditable()</code>
     */
    public boolean isCellEditable(int row, int col) {
        return false;
    }
}    // End GeneSetModel
