/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.data.matrix;

/**
 * A dense 2-dimensional matrix holding objects.
 *
 * @author Joshua Gould
 */
public class ObjectMatrix2D {
    Object[][] matrix;

    /**
     * Creates a new matrix
     *
     * @param matrix the data
     */
    public ObjectMatrix2D(Object[][] matrix) {
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
     */
    public ObjectMatrix2D slice(int[] rowIndices, int[] columnIndices) {
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

        Object[][] sMatrix = new Object[rowIndices.length][columnIndices.length];

        for (int i = 0, rows = rowIndices.length; i < rows; i++) {
            for (int j = 0, cols = columnIndices.length; j < cols; j++) {
                sMatrix[i][j] = this.matrix[rowIndices[i]][columnIndices[j]];
            }
        }
        return new ObjectMatrix2D(sMatrix);
    }

    /**
     * Gets a single element
     *
     * @param row    Row index.
     * @param column Column index.
     * @return The value at A[row,column]
     */
    public Object get(int row, int column) {
        return matrix[row][column];
    }

    /**
     * Gets the row dimension.
     *
     * @return m, the number of rows.
     */
    public int getRowCount() {
        return matrix.length;
    }

    /**
     * Gets the column dimension.
     *
     * @return n, the number of columns.
     */
    public int getColumnCount() {
        return matrix[0].length;
    }
}
