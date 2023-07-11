/*
 * Copyright (c) 2003-2023 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.models;

import edu.mit.broad.genome.objects.Dataset;

import javax.swing.table.AbstractTableModel;

/**
 * An implementation of AbstractTableModel for Datasets. <br>
 * <p/>
 * Does not replicate any of Dataset's data strcutures.
 * <p/>
 * Why not just make Dataset an AbstractTableModel? Just trying to avoid
 * placing any GUI related code in the core datastructures.
 * <p/>
 * Why extending AbstractTableModel rather than DefaultTableModel?
 * Because dont want several of the "mutability" properties that
 * DefaultTableModel has. i.e setRiwAt etc - this is a Fixture - once
 * constructed its #rows and # columns is set. (the contents can change though)
 *
 * @author Aravind Subramanian
 */
public class DatasetModel extends AbstractTableModel {
    private Dataset fDataset;

    /**
     * Class Constructor.
     * Cretaes a default row name based annotation for use.
     */
    public DatasetModel(Dataset ds) {

        if (ds == null) {
            throw new IllegalArgumentException("Param ds cannot be null");
        }

        this.fDataset = ds;
    }

    public Dataset getDataset() {
        return fDataset;
    }

    public int getColumnCount() {
        return fDataset.getNumCol() + 1;
    }

    public int getRowCount() {
        return fDataset.getNumRow();
    }

    public String getColumnName(int col) {
        if (col == 0) {
            return "Feature";
        } else {
            return fDataset.getColumnName(col - 1);
        }
    }

    public Object getValueAt(int row, int col) {
        if (col == 0) {
            return fDataset.getRowName(row);
        } else {
            return fDataset.getElement(row, col - 1);
        }
    }

    /**
     * JTable uses this method to determine the default renderer
     * editor for each cell.
     * Its always a Float for the data and a String for the row label
     * in this implementation.
     */
    public Class getColumnClass(int col) {
        return getValueAt(0, col).getClass();
    }

    public boolean isEditable() {
        return false;
    }

    public boolean isCellEditable(int row, int col) {
        return false;
    }
}
