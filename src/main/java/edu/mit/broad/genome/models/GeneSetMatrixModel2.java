/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.models;

import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.GeneSetMatrix;
import edu.mit.broad.genome.parsers.AuxUtils;

import javax.swing.table.AbstractTableModel;

/**
 * GeneSetMatrix viewer thats like a dataset viewer - just dumps all sets column wise
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class GeneSetMatrixModel2 extends AbstractTableModel {

    private GeneSetMatrix fGmx;

    private int fMaxSetSize;

    /**
     * Class Constructor.
     * Cretaes a default row name based annotation for use.
     */
    public GeneSetMatrixModel2(final GeneSetMatrix gmx) {

        if (gmx == null) {
            throw new IllegalArgumentException("Param ds cannot be null");
        }

        this.fGmx = gmx;
        this.fMaxSetSize = fGmx.getMaxGeneSetSize();
    }

    public int getColumnCount() {
        return fGmx.getNumGeneSets();
    }

    public int getRowCount() {
        return fMaxSetSize;
    }

    public String getColumnName(int col) {
        return AuxUtils.getAuxNameOnlyNoHash(fGmx.getGeneSet(col).getName());
    }

    public Object getValueAt(int row, int col) {
        GeneSet gset = fGmx.getGeneSet(col);
        Object val = null;
        if (gset.getNumMembers() > row) {
            val = gset.getMember(row);
        }

        return val;
    }

    public Class getColumnClass(int col) {
        return String.class;
    }

    public boolean isEditable() {
        return false;
    }

    public boolean isCellEditable(int row, int col) {
        return false;
    }
}    // End GeneSetMatrixModel2
