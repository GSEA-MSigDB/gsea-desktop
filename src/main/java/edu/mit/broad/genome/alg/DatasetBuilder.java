/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg;

import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.Annot;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.DefaultDataset;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to incrementally build a Dataset.
 * <p/>
 * Dont need to worry about data sharing in the methods of this class as the generate
 * method uses DefaultDataset which does the data duplication upon creation.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class DatasetBuilder {

    public String fName;
    public List fRows;
    public List fRowNames;
    public List fRowDescs;
    public List fColNames;
    public boolean fDoneBuilding;

    /**
     * Class Constructor.
     * <p/>
     * New dataset made has NO data shared with input params.
     */
    public DatasetBuilder(final String name, final List colNames) {

        if (name == null) {
            throw new IllegalArgumentException("Name param cant be null");
        }

        if (colNames == null) {
            throw new IllegalArgumentException("colanmes param cant be null");
        }

        this.fName = name;
        this.fDoneBuilding = false;
        this.fColNames = new ArrayList(colNames); // safe copy
        this.fRowNames = new ArrayList();
        this.fRows = new ArrayList();
        this.fRowDescs = new ArrayList();
    }

    /**
     * Add row i from dataset fromds at current position
     * to the dataset being built.
     *
     * @param rowInDs
     * @param fromDs
     */
    public void addRow(final int rowInDs, final Dataset fromDs) {
        checkBuild();
        fRows.add(fromDs.getRow(rowInDs));
        fRowNames.add(fromDs.getRowName(rowInDs));
    }

    /**
     * New dataset made has NO shared data
     */
    public Dataset generate(final Annot ann) {

        Matrix matrix = new Matrix(fRows.size(), fColNames.size());

        for (int i = 0; i < fRows.size(); i++) {
            matrix.setRow(i, (Vector) fRows.get(i)); // this does a copy of the vector's floats so safe
        }

        // we reuse the data from this class in the default constructor as this class has NO
        // shared data with anyone else & hence safe.
        DefaultDataset ds = new DefaultDataset(fName, matrix, fRowNames, fColNames, true, ann);
        fDoneBuilding = true;
        return ds;
    }

    private void checkBuild() {

        if (fDoneBuilding) {
            throw new RuntimeException("Already done building -- DatasetBuilder cannot be reused");
        }
    }
}    // End DatasetBuilder
