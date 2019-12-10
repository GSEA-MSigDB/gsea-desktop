/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.math.StringMatrix;

import java.util.*;

/**
 * @author Aravind Subramanian
 */

public class StringDataframe extends AbstractObject implements IDataframe {

    private StringMatrix fMatrix;

    private List<String> fRowNames;
    private List<String> fColNames;

    // name of the first row
    private String fRowLabelName;

    public StringDataframe(final String name,
                           final StringMatrix smatrix,
                           final String[] rowNames,
                           final String[] colNames) {
    	this(name, smatrix, toList(rowNames), toList(colNames));
    }

    public StringDataframe(final String name,
                           final StringMatrix smatrix,
                           final String[] colNames) {
        this(name, smatrix, createSeriesStrings(0, smatrix.getNumRow(), "row_"),
                toList(colNames));
    }

    public StringDataframe(final String name,
                           final StringMatrix smatrix,
                           final List<String> rowNames,
                           final String[] colNames) {
        this(name, smatrix, rowNames, toList(colNames));
    }

    public StringDataframe(final String name,
                           final StringMatrix smatrix,
                           final List<String> rowNames,
                           final List<String> colNames) {
        if (smatrix == null) {
            throw new IllegalArgumentException("Param matrix cant be null");
        }

        if (rowNames == null) {
            throw new IllegalArgumentException("Param rowNames cant be null");
        }

        if (colNames == null) {
            throw new IllegalArgumentException("Param rowNames cant be null");
        }

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
        return fRowNames.toArray(new String[fRowNames.size()]);
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

    private static List<String> createSeriesStrings(int start, int stop, String commonPrefix) {
        List<String> list = new ArrayList<String>(stop - start);
        for (int i = start; i < stop; i++) {
            list.add(commonPrefix + i);
        }

        return list;
    }

    private static List<String> toList(String[] ss) {
        List<String> list = new ArrayList<String>(ss.length);
        for (int i = 0; i < ss.length; i++) {
            list.add(ss[i]);
        }

        return list;
    }

    public void replace(String thisStr, String withThisStr) {
        fMatrix.replace(thisStr, withThisStr);
    }
}