/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.data.expr;

import org.genepattern.data.matrix.DoubleMatrix2D;
import org.genepattern.data.matrix.ObjectMatrix2D;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Implementation of IExpressionData interface
 *
 * @author Joshua Gould
 */
public class ExpressionData implements IExpressionData {
    protected DoubleMatrix2D dataset;

    protected Map matrices;

    protected MetaData rowMetaData;

    protected MetaData columnMetaData;

    /**
     * Creates a new <tt>ExpressionData</tt> instance
     *
     * @param dataset             The dataset
     * @param _rowDescriptions    The row descriptions or <tt>null</tt>
     * @param _columnDescriptions The column descriptions or <tt>null</tt>>
     */
    public ExpressionData(DoubleMatrix2D dataset, String[] _rowDescriptions,
                          String[] _columnDescriptions) {
        this.dataset = dataset;
        this.rowMetaData = new MetaData(dataset.getRowCount());

        if (_rowDescriptions != null) {
            if (_rowDescriptions.length != dataset.getRowCount()) {
                throw new IllegalArgumentException(
                        "Length of row descriptions not equal to number of rows in matrix.");
            }
            rowMetaData.setMetaData(ExpressionConstants.DESC, _rowDescriptions);
        }

        this.columnMetaData = new MetaData(dataset.getColumnCount());
        if (_columnDescriptions != null) {
            if (_columnDescriptions.length != dataset.getColumnCount()) {
                throw new IllegalArgumentException(
                        "Length of column descriptions not equal to number of columns in matrix.");
            }
            columnMetaData.setMetaData(ExpressionConstants.DESC,
                    _columnDescriptions);
        }
        matrices = new HashMap();
    }

    /**
     * * Creates a new <tt>ExpressionData</tt> instance
     *
     * @param dataset        The dataset
     * @param rowMetaData    the row meta data
     * @param columnMetaData the column meta data
     */
    public ExpressionData(DoubleMatrix2D dataset, MetaData rowMetaData,
                          MetaData columnMetaData, Map matrices) {
        this.dataset = dataset;
        this.rowMetaData = rowMetaData;
        this.columnMetaData = columnMetaData;
        this.matrices = matrices;
        if (this.matrices == null) {
            this.matrices = new HashMap();
        }
    }

    /**
     * Constructs and returns a new <tt>ExpressionData</tt> instance that
     * contains the indicated cells. Indices can be in arbitrary order.
     *
     * @param rowIndices    The rows of the cells in the new matrix. To indicate that the
     *                      new matrix should contain all rows, set this parameter to
     *                      null.
     * @param columnIndices The columns of the cells in the new matrix. To indicate that
     *                      the new matrix should contain all columns, set this parameter
     *                      to null.
     * @return the new ExpressionData
     * @throws IllegalArgumentException if an index occcurs more than once.
     * @see #slice(String[],String[])
     */
    public ExpressionData slice(int[] rowIndices, int[] columnIndices) {
        if (rowIndices == null) {
            rowIndices = new int[dataset.getRowCount()];
            for (int i = dataset.getRowCount(); --i >= 0;) {
                rowIndices[i] = i;
            }
        }
        if (columnIndices == null) {
            columnIndices = new int[dataset.getColumnCount()];
            for (int i = dataset.getColumnCount(); --i >= 0;) {
                columnIndices[i] = i;
            }
        }

        DoubleMatrix2D newDoubleMatrix2D = dataset.slice(rowIndices,
                columnIndices);

        MetaData newRowMetaData = rowMetaData.slice(rowIndices);
        MetaData newColumnMetaData = columnMetaData.slice(columnIndices);
        HashMap newMatrices = new HashMap();
        for (Iterator it = matrices.keySet().iterator(); it.hasNext();) {
            String key = (String) it.next();
            ObjectMatrix2D m = (ObjectMatrix2D) matrices.get(key);
            newMatrices.put(key, m.slice(rowIndices, columnIndices));
        }

        return new ExpressionData(newDoubleMatrix2D, newRowMetaData,
                newColumnMetaData, newMatrices);
    }

    public int getRowCount() {
        return dataset.getRowCount();
    }

    public int getColumnCount() {
        return dataset.getColumnCount();
    }

    public double getValue(int row, int column) {
        return dataset.get(row, column);
    }

    public String getValueAsString(int row, int column) {
        return String.valueOf(dataset.get(row, column));
    }

    public String getColumnName(int column) {
        return dataset.getColumnName(column);
    }

    public String getRowName(int row) {
        return dataset.getRowName(row);
    }

    /**
     * Gets the row index for the row name .
     *
     * @param rowName the row name.
     * @return the row index, or -1 if the row name is not contained in this
     *         matrix
     */
    public int getRowIndex(String rowName) {
        return dataset.getRowIndex(rowName);
    }

    /**
     * Gets the column index for the column name .
     *
     * @param columnName the column name.
     * @return the column index, or -1 if the column name is not contained in
     *         this matrix
     */
    public int getColumnIndex(String columnName) {
        return dataset.getColumnIndex(columnName);
    }

    public Object getData(int row, int column, String name) {
        ObjectMatrix2D matrix = (ObjectMatrix2D) matrices.get(name);
        return matrix != null ? matrix.get(row, column) : null;
    }

    public String getRowMetadata(int row, String name) {
        return rowMetaData.getMetaData(row, name);
    }

    public String getColumnMetadata(int column, String name) {
        return columnMetaData.getMetaData(column, name);
    }

    public String getDataName(int index) {
        return ((String[]) matrices.keySet().toArray(new String[0]))[index];
    }

    public int getDataCount() {
        return matrices.size();
    }
}
