/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.math.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Imp - this is seperate and distinct form a Dataset.
 * Eventhough it is very very similar in structure (except that no annotations/desc ever)
 * But thats not the main diff. The main diff is the intended use -
 * dfs are meant to be the xomics representation of the R dataframe / matrix structures.
 * dfs -> basically any labelled matrix (not necc a gene expression dataset)
 * <p/>
 * They can be seamlessless assocaited with actions that launch R scripts.
 * They are generally not as big as Datasets (though can be) -- for instance the
 * pairwise matrix formed after a Mantel test.
 * We dont not want Dataframes to automatically be available to the application
 * as a Dataset -- as they are not always interchangable. Options are available
 * to explicitly convert (and easily) a given df into a ds and the use the resulting
 * ds in whatever analysis that ds are available for.
 * But it would be wierd if a pairwise comparison matrix were available as a bpog option.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */

// intentionally differntiate this from a dataset

public class Dataframe extends AbstractObject implements IDataframe {

    private Matrix fMatrix;
    private List fRowNames;
    private List fColNames;

    /**
     * subclasses MUST call init after using this form of the constructor.
     */
    protected Dataframe() {
    }

    /**
     * Class constructor
     *
     * @param name
     * @param matrix
     * @param rowNames
     * @param colNames
     * @param shareMatrix
     * @param shareRowNames
     * @param shareColNames
     */
    public Dataframe(final String name, final Matrix matrix, final List rowNames, final List colNames,
                     final boolean shareMatrix, final boolean shareRowNames, final boolean shareColNames) {

        if (matrix == null) {
            throw new IllegalArgumentException("Param matrix cant be null");
        }

        if (rowNames == null) {
            throw new IllegalArgumentException("Param rownames cant be null");
        }

        if (colNames == null) {
            throw new IllegalArgumentException("Param rownames cant be null");
        }

        Matrix dmatrix;
        List drowNames;
        List dcolNames;

        if (shareMatrix) {
            dmatrix = matrix;
        } else {
            dmatrix = matrix.cloneDeep();
        }

        if (shareRowNames) {
            drowNames = rowNames;
        } else {
            drowNames = new ArrayList(rowNames);
        }

        if (shareColNames) {
            dcolNames = colNames;
        } else {
            dcolNames = new ArrayList(colNames);
        }

        init(name, dmatrix, drowNames, dcolNames);

    }

    /**
     * assumes that when i am called the data has already been duplicated
     * i.e callers within this method are responsible for passing me
     * already duplicated data.
     */
    private void init(String dsname, Matrix matrix, List rowNames, List colNames) {

        super.initialize(dsname);

        if (matrix == null) {
            throw new IllegalArgumentException("Matrix cannot be null");
        }

        if (rowNames == null) {
            throw new IllegalArgumentException("rowNames cannot be null");
        }

        if (colNames == null) {
            throw new IllegalArgumentException("colNames cannot be null");
        }

        if (matrix.getNumRow() != rowNames.size()) {
            throw new IllegalArgumentException("Matrix numrow: " + matrix.getNumRow() + " and rowNames: "
                    + rowNames.size() + " do not match in size");
        }

        if (matrix.getNumCol() != colNames.size()) {
            throw new IllegalArgumentException("Matrix numcol: " + matrix.getNumCol() + " and colNames: "
                    + colNames.size() + " do not match in size");
        }

        this.fMatrix = matrix;
        fMatrix.setImmutable();    // IMP notice.
        this.fRowNames = Collections.unmodifiableList(rowNames);
        ;
        this.fColNames = Collections.unmodifiableList(colNames);
        ;
    }

    public Vector getRow(int rown) {
        return fMatrix.getRowV(rown);
    }

    public Object getElementObj(int rown, int coln) {
        return new Float(fMatrix.getElement(rown, coln));
    }

    public String getRowName(int rown) {
        return (String) fRowNames.get(rown);
    }

    public String getColumnName(int coln) {
        return (String) fColNames.get(coln);
    }

    public int getNumRow() {
        return fMatrix.getNumRow();
    }

    public int getNumCol() {
        return fMatrix.getNumCol();
    }

    public String getQuickInfo() {
        StringBuffer buf = new StringBuffer().append(getNumRow()).append('x').append(getNumCol());
        return buf.toString();
    }

}    // End Dataframe
