/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.math.StringMatrix;

import java.util.*;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */

public class StringDataframe extends AbstractObject implements IDataframe {

    protected StringMatrix fMatrix;

    protected List fRowNames;

    protected List fColNames;

    // name of the first row
    private String fRowLabelName;

    /**
     * Class Constructor.
     * Specified data is copied over - data is NOT shared
     *
     * @param name
     * @param smatrix
     * @param rowNames
     * @param colNames
     */
    public StringDataframe(final String name,
                           final StringMatrix smatrix,
                           final List rowNames,
                           final List colNames,
                           boolean shareAll) {
        this(name, smatrix, rowNames, colNames, shareAll, shareAll, shareAll);
    }

    /**
     * Class constructor
     *
     * @param name
     * @param smatrix
     * @param rowNames
     * @param colNames
     * @param shareAll
     */
    public StringDataframe(final String name,
                           final StringMatrix smatrix,
                           final String[] rowNames,
                           final String[] colNames,
                           boolean shareAll) {
        this(name, smatrix, toList(rowNames), toList(colNames), shareAll, shareAll, shareAll);
    }

    public StringDataframe(final String name,
                           final StringMatrix smatrix,
                           final String[] colNames,
                           final boolean shareAll) {
        this(name, smatrix, createSeriesStrings(0, smatrix.getNumRow(), "row_"),
                toList(colNames), shareAll, shareAll, shareAll);
    }

    public StringDataframe(final String name,
                           final StringMatrix smatrix,
                           final List rowNames,
                           final List colNames,
                           final boolean shareMatrix,
                           final boolean shareRowNames,
                           final boolean shareColNames) {

        if (smatrix == null) {
            throw new IllegalArgumentException("Param matrix cant be null");
        }

        if (rowNames == null) {
            throw new IllegalArgumentException("Param rowNames cant be null");
        }

        if (colNames == null) {
            throw new IllegalArgumentException("Param rowNames cant be null");
        }

        //log.debug(rowNames);

        StringMatrix dmatrix;
        List drowNames;
        List dcolNames;

        if (shareMatrix) {
            dmatrix = smatrix;
        } else {
            dmatrix = smatrix.cloneDeep();
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

    public StringDataframe(final String name,
                           final StringMatrix smatrix,
                           final List rowNames,
                           final String[] colNames,
                           final boolean shareMatrix,
                           final boolean shareRowNames) {
        this(name, smatrix, rowNames, toList(colNames), shareMatrix, shareRowNames, true);
    }

    /**
     * assumes that when i am called the data has already been duplicated
     * i.e callers within this method are responsible for passing me
     * already duplicated data.
     */
    protected void init(final String name,
                        final StringMatrix smatrix,
                        final List rowNames,
                        final List colNames) {

        super.initialize(name);

        if (smatrix.getNumRow() != rowNames.size()) {
            throw new IllegalArgumentException("Matrix: " + smatrix.getNumRow() + " and rowNames: "
                    + rowNames.size() + " do not match in size");
        }

        if (smatrix.getNumCol() != colNames.size()) {
            throw new IllegalArgumentException("Matrix: " + smatrix.getNumCol() + " and colNames: "
                    + colNames.size() + " do not match in size");
        }

        if (smatrix.getNumCol() == 0) {
            log.warn("zero cols in StringMatrix");
        }

        if (smatrix.getNumRow() == 0) {
            //TraceUtils.showTrace();
            log.debug("zero rows in StringMatrix");
        }

        this.fMatrix = smatrix;
        this.fMatrix.setImmutable();    // IMP notice.

        this.fRowNames = rowNames;
        this.fColNames = colNames;
    }

    public String[] getColumn(final int coln) {
        return fMatrix.getColumn(coln);
    }

    public String getElement(final int rown, final int coln) {
        return fMatrix.getElement(rown, coln);
    }

    public Object getElementObj(final int rown, final int coln) {
        return getElement(rown, coln);
    }

    public String getRowName(int rown) {
        return (String) fRowNames.get(rown);
    }

    public String[] getRowNamesArray() {
        return (String[]) fRowNames.toArray(new String[fRowNames.size()]);
    }

    public String getColumnName(int coln) {
        return fColNames.get(coln).toString();
    }

    public int getNumRow() {
        return fMatrix.getNumRow();
    }

    public int getNumCol() {
        return fMatrix.getNumCol();
    }

    public String getRowLabelName() {
        return fRowLabelName;
    }

    public String getQuickInfo() {
        StringBuffer buf = new StringBuffer().append(getNumRow()).append('x').append(getNumCol());
        return buf.toString();
    }

    public static List createSeriesStrings(int start, int stop, String commonPrefix) {
        List list = new ArrayList(stop - start);
        for (int i = start; i < stop; i++) {
            list.add(commonPrefix + i);
        }

        return list;
    }

    private static List toList(String[] ss) {
        List list = new ArrayList(ss.length);
        for (int i = 0; i < ss.length; i++) {
            list.add(ss[i]);
        }

        return list;
    }

    public void replace(String thisStr, String withThisStr) {
        fMatrix.replace(thisStr, withThisStr);
    }

}    // End StringDataframe
