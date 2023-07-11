/*
 * Copyright (c) 2003-2023 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.models;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

/**
 * proxy model that adds a row # as the first column to the specified real model
 *
 * @author Aravind Subramanian
 */
public class NumberedProxyModel extends AbstractTableModel {
    private TableModel fRealModel;
    private int fStartNumberingFromRowIndex;

    public NumberedProxyModel(TableModel model) { this(model, 0); }

    public NumberedProxyModel(TableModel model, int startNumberingFromRowIndex) {
        if (model == null) { throw new IllegalArgumentException("Param model cannot be null"); }

        this.fRealModel = model;
        this.fStartNumberingFromRowIndex = startNumberingFromRowIndex;
    }

    public int getColumnCount() {
        return fRealModel.getColumnCount() + 1;
    }

    public int getRowCount() {
        return fRealModel.getRowCount();
    }

    public String getColumnName(int col) {
        if (col == 0) {
            return " "; // wierd, ret "" cause column headers to dissepear
        } else {
            return fRealModel.getColumnName(col - 1);
        }
    }

    public Object getValueAt(int row, int col) {
        if (row < fStartNumberingFromRowIndex && col == 0) {
            return "";
        }

        if (col == 0) {
            return Integer.toString(row + 1 - fStartNumberingFromRowIndex);
        } else {
            return fRealModel.getValueAt(row, col - 1);
        }
    }

    public Class getColumnClass(int col) {
        if (col == 0) {
            return String.class;
        } else {
            return fRealModel.getColumnClass(col - 1);
        }
    }

    public boolean isEditable() {
        return false;
    }

    public boolean isCellEditable(int row, int col) {
        return false;
    }
}
