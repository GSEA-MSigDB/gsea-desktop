/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.math.Vector;

import java.util.List;

/**
 * Wrapper to cover a Dataset when it has to be returned by a method.
 * Though a Dataset is immutabke, objects with methods that return a Dataset can run into
 * trouble if they return a ref to their real Dataset
 * So, instead return a wrapper.
 */
public class DatasetProxy extends AbstractObject implements Dataset {

    private Dataset fRealDataset;

    /**
     * Class constructor
     *
     * @param newName
     * @param ds
     */
    public DatasetProxy(final String newName, final Dataset ds) {
        super(newName);

        if (ds == null) {
            throw new IllegalArgumentException("Parameter ds cannot be null");
        }

        this.fRealDataset = ds;
    }

    public Annot getAnnot() {
        return fRealDataset.getAnnot();
    }

    public Vector getRow(final int rown) {
        return fRealDataset.getRow(rown);
    }

    public Vector getColumn(final int coln) {
        return fRealDataset.getColumn(coln);
    }

    public float getElement(final int rown, final int coln) {
        return fRealDataset.getElement(rown, coln);
    }

    public String getRowName(final int rown) {
        return fRealDataset.getRowName(rown);
    }

    public Vector getRow(final String rowName) {
        return fRealDataset.getRow(rowName);
    }

    public Vector[] getRows(final GeneSet gset) {
        return fRealDataset.getRows(gset);
    }

    public List<String> getRowNames() {
        return fRealDataset.getRowNames();
    }

    public int getRowIndex(final String rowname) {
        return fRealDataset.getRowIndex(rowname);
    }

    public List<String> getColumnNames() {
        return fRealDataset.getColumnNames();
    }

    public int getColumnIndex(final String colname) {
        return fRealDataset.getColumnIndex(colname);
    }

    public String getColumnName(final int coln) {
        return fRealDataset.getColumnName(coln);
    }

    public int getNumRow() {
        return fRealDataset.getNumRow();
    }

    public int getNumCol() {
        return fRealDataset.getNumCol();
    }

    public int getDim() {
        return fRealDataset.getDim();
    }

    public Matrix getMatrix() {
        return fRealDataset.getMatrix();
    }

    public String getQuickInfo() {
        return fRealDataset.getQuickInfo();
    }

    public GeneSet getRowNamesGeneSet() {
        return fRealDataset.getRowNamesGeneSet();
    }

} // End class DatasetWrapper
