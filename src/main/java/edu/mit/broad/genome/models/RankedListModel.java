/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.models;

import edu.mit.broad.genome.objects.RankedList;

import javax.swing.table.AbstractTableModel;

/**
 * An implementation of AbstractTableModel for FSet.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class RankedListModel extends AbstractTableModel {

    /**
     * The underlying object being modell'ed
     */
    private final RankedList fRankedList;

    /**
     * Column labels for the table. Using "Class" rather than "Template" as
     * users are more familiar wit that term - i think.
     * dont set a col header to "" -> causes it to dissapear!
     */
    private static final String[] kColNames = {"Feature Name", "Rank", "Score"};

    /**
     * Class Constructor.
     * Initializes model to specified Template.
     */
    public RankedListModel(RankedList rl) {
        this.fRankedList = rl;
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
        return fRankedList.getSize();
    }

    public String getColumnName(int col) {
        return kColNames[col];
    }

    public Object getValueAt(int row, int col) {
        if (col == 0) {
            return fRankedList.getRankName(row);
        } else if (col == 1) {
            return new Integer(row + 1);
        } else {
            return new Float(fRankedList.getScore(row));
        }
    }

    public Class getColumnClass(int col) {
        if (col == 0) {
            return String.class;
        } else if (col == 1) {
            return Integer.class;
        } else {
            return Float.class;
        }
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
}    // End FSetModel
