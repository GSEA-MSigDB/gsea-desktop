/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.data.matrix;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ujmp.core.doublematrix.DenseDoubleMatrix2D;
import org.ujmp.core.util.MathUtil;

/**
 * A labeled dense 2-dimensional matrix holding double elements.
 */
// Simple wrapper around a UJMP Matrix.  We could probably drop this entirely and work with that
// library directly, but I'll leave it for the moment as there is still a fair chance we'll switch
// away to a different Matrix library.
public class DoubleMatrix2D {
    private static final transient Logger klog = LoggerFactory.getLogger(DoubleMatrix2D.class);

    // Wrapped data structure; to be unwrapped and used directly if possible.
    // Remember that it has metadata capabilities.
    private final DenseDoubleMatrix2D matrix;

    /**
     * Creates a new matrix
     *
     * @param data The data.
     */
    public DoubleMatrix2D(double[][] data) {
        this.matrix = (DenseDoubleMatrix2D)org.ujmp.core.Matrix.Factory.linkToArray(data);
        fillInRows();
        fillInColumns();
    }

    /**
     * Creates a new matrix
     *
     * @param data        The data.
     * @param rowNames    the row names
     * @param columnNames the column names
     */
    public DoubleMatrix2D(double[][] data, String[] rowNames,
                          String[] columnNames) {
        int rows = data.length;
        if (rows == 0) {
            throw new IllegalArgumentException(
                    "Number of rows must be greater than 0");
        }
        int columns = data[0].length;
        if (columns == 0) {
            throw new IllegalArgumentException(
                    "Number of columns must be greater than 0");
        }
        if (columnNames.length != columns) {
            throw new IllegalArgumentException(
                    "Length of column names must be equal to number of columns in data.");
        }
        if (rowNames.length != rows) {
            throw new IllegalArgumentException(
                    "Length of row names must be equal to number of rows in data.");
        }

        this.matrix = (DenseDoubleMatrix2D)org.ujmp.core.Matrix.Factory.linkToArray(data);
        setRowNames(Arrays.asList(rowNames));
        setColumnNames(Arrays.asList(columnNames));
    }

    private DoubleMatrix2D(DenseDoubleMatrix2D matrix) {
        this.matrix = matrix;
    }
    
    /**
     * Constructs and returns a new matrix that contains the indicated cells.
     * Indices can be in arbitrary order.
     *
     * @param rowIndices    The rows of the cells in the new matrix. To indicate that the
     *                      new matrix should contain all rows, set this parameter to
     *                      null.
     * @param columnIndices The columns of the cells in the new matrix. To indicate that
     *                      the new matrix should contain all columns, set this parameter
     *                      to null.
     * @return the new matrix
     * @throws IllegalArgumentException if an index occcurs more than once.
     */
    public DoubleMatrix2D slice(int[] rowIndices, int[] columnIndices) {
        if (rowIndices == null) {
            rowIndices = new int[getRowCount()];
            for (int i = getRowCount(); --i >= 0;) {
                rowIndices[i] = i;
            }
        }
        if (columnIndices == null) {
            columnIndices = new int[getColumnCount()];
            for (int i = getColumnCount(); --i >= 0;) {
                columnIndices[i] = i;
            }
        }
        
        klog.info("About to slice...");
        
        // TODO: Eventually just accept longs instead.
        long[] r = MathUtil.toLongArray(rowIndices);
        long[] c = MathUtil.toLongArray(columnIndices);
        
        DenseDoubleMatrix2D slice = (DenseDoubleMatrix2D)matrix.select(DenseDoubleMatrix2D.NEW, r, c);
        return new DoubleMatrix2D(slice);
    }

    /**
     * Fills in row names that the user did not specify.
     *
     * @param rowIndex The row index to start filling in rows.
     */
    private void fillInRows() {
        for (int i = 0, rows = getRowCount(); i < rows; i++) {
            String rowName = String.valueOf(i + 1);
            matrix.setRowLabel(i, rowName);
        }
    }

    /**
     * Fills in names that the user did not specify.
     *
     * @param columnIndex The column index to start filling in columns.
     */
    private void fillInColumns() {
        for (int i = 0, columns = getColumnCount(); i < columns; i++) {
            String name = "X" + String.valueOf(i + 1);
            matrix.setColumnLabel(i, name);
        }
    }

    /**
     * Sets the row names to the specified value. Duplicate row names are not
     * allowed. If the length of s is less than getRowCount(), the remaining row
     * names will be filled in automatically
     *
     * @param s The list containing the row names
     */
    private void setRowNames(List s) {
        if (s.size() > getRowCount()) {
            throw new IllegalArgumentException(
                    "Invalid row names length. getRowCount():" + getRowCount()
                            + " row names length:" + s.size());
        }
        for (int i = 0, size = s.size(); i < size; i++) {
            String rowName = (String) s.get(i);
            if (rowName == null) {
                throw new IllegalArgumentException(
                        "Null row names are not allowed.");
            }
            int rowForLabel = MathUtil.longToInt(matrix.getRowForLabel(rowName));
            // Make sure the rowForLabel is in bounds of the array and that we have not already
            // seen this rowName.
            if (rowForLabel >= 0 && rowForLabel < size && rowForLabel != i) {
                throw new IllegalArgumentException(
                        "Duplicate row names are not allowed:" + rowName);
            }

            matrix.setRowLabel(i, rowName);
        }
    }

    /**
     * Sets the column names to the specified value. Duplicate column names are
     * not allowed. If the length of s is less than getColumnCount(), the
     * remaining names will be filled in automatically.
     *
     * @param s The list containing the names
     */
    private void setColumnNames(List s) {
        if (s.size() > getColumnCount()) {
            throw new IllegalArgumentException(
                    "Invalid column names length. getColumnCount():"
                            + getColumnCount() + " column names length:"
                            + s.size());
        }
        for (int i = 0; i < s.size(); i++) {
            String name = (String) s.get(i);
            if (name == null) {
                throw new IllegalArgumentException(
                        "Null column names are not allowed.");
            }
            int columnForLabel = MathUtil.longToInt(matrix.getColumnForLabel(name));
            if (columnForLabel >= 0 && columnForLabel != i) {
                throw new IllegalArgumentException(
                        "Duplicate column names are not allowed:" + name);
            }

            matrix.setColumnLabel(i, name);
        }
    }

    /**
     * Gets the row index for the row name .
     *
     * @param rowName the row name.
     * @return the row index, or -1 if the row name is not contained in this
     *         matrix
     */
    public int getRowIndex(String rowName) {
        return MathUtil.longToInt(matrix.getRowForLabel(rowName));
    }

    /**
     * Gets the column index for the column name .
     *
     * @param columnName the column name.
     * @return the column index, or -1 if the column name is not contained in
     *         this matrix
     */
    public int getColumnIndex(String columnName) {
        return MathUtil.longToInt(matrix.getColumnForLabel(columnName));
    }

    /**
     * Gets a single element
     *
     * @param row    Row index.
     * @param column Column index.
     * @return The value at A[row,column]
     */
    public double get(int row, int column) {
        return matrix.getDouble(row, column);
    }

    /**
     * Gets the row name at the specified index
     *
     * @param rowIndex The row index.
     * @return The row name.
     */

    public String getRowName(int rowIndex) {
        return matrix.getRowLabel(rowIndex);
    }

    /**
     * Gets the column name at the specified index
     *
     * @param columnIndex The column index.
     * @return The column name.
     */
    public String getColumnName(int columnIndex) {
        return matrix.getColumnLabel(columnIndex);
    }

    /**
     * Gets the row dimension.
     *
     * @return m, the number of rows.
     */
    public int getRowCount() {
        return MathUtil.longToInt(matrix.getRowCount());
    }

    /**
     * Gets the column dimension.
     *
     * @return n, the number of columns.
     */
    public int getColumnCount() {
        return MathUtil.longToInt(matrix.getColumnCount());
    }
}
