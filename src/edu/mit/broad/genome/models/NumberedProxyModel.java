/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.models;

import edu.mit.broad.genome.XLogger;
import org.apache.log4j.Logger;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

/**
 * proxy model that addds a rwo # as the first column to the specified real model
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class NumberedProxyModel extends AbstractTableModel {

    private final Logger log = XLogger.getLogger(NumberedProxyModel.class);
    private TableModel fRealModel;
    private int fStartNumberingFromRowIndex;

    /**
     * Class Constructor.
     * Cretaes a default row name based annotation for use.
     */
    public NumberedProxyModel(TableModel model) {
        this(model, 0);
    }

    public NumberedProxyModel(TableModel model, int startNumberingFromRowIndex) {

        if (model == null) {
            throw new IllegalArgumentException("Param model cannot be null");
        }

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
            return new Integer(row + 1 - fStartNumberingFromRowIndex).toString();
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
}    // End NumberedProxyModel
