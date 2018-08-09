/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.math.Vector;

import java.util.List;

/**
 * A Math object conceptually similar to R's Dataframe but with annotations
 * Basically a labelled Matrix.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public interface Dataset extends PersistentObject {

    /**
     * @param rown
     * @return Vector of values of rown.
     */
    public Vector getRow(final int rown);

    /**
     * @param rowName
     * @return
     */
    public Vector getRow(final String rowName);

    public Vector[] getRows(final GeneSet gset);

    /**
     * @param coln
     * @return Vector of values of coln.
     */
    public Vector getColumn(final int coln);

    /**
     * @param row
     * @param column
     * @return Value of element at specified cell.
     */
    public float getElement(final int row, final int column);

    /**
     * @param rown
     * @return Name of row at rown
     */
    public String getRowName(final int rown);

    /**
     * generally should return unmodifiable list
     */
    public List getRowNames();

    /**
     * @return
     */
    public GeneSet getRowNamesGeneSet();

    /**
     * generally should return unmodifiable list
     */
    public List getColumnNames();

    /**
     * @param coln
     * @return Name of column at position coln
     */
    public String getColumnName(final int coln);

    /**
     * @param rowname Name of row
     * @return Position of row in the Dataset. -1 if not found.
     */
    public int getRowIndex(final String rowName);

    /**
     * @param colname Name of column
     * @return Position of specified column in the Dataset. -1 if not found.
     */
    public int getColumnIndex(final String colName);

    /**
     * @return Number of rows in Dataset
     */
    public int getNumRow();

    /**
     * @return Number of columns in Dataset
     */
    public int getNumCol();

    /**
     * Should return an safe i.e unmodifiable Matrix.
     * Generally implementors might prefer to return an immutable view rather
     * than a copy.
     * Similar to returning an unmodifiable view of a Collection.
     */
    public Matrix getMatrix();

    public int getDim();

    public Annot getAnnot();


}    // End Dataset
