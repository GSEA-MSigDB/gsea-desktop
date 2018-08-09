/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.data.matrix;

import org.genepattern.data.expr.ExpressionData;
import org.genepattern.data.expr.IExpressionData;

/**
 * @author Joshua Gould
 */
public class IExpressionDataUtil {
    private IExpressionDataUtil() {
    }

    /**
     * Gets a sliced view of the given data
     *
     * @param dataset        The data
     * @param _rowIndices    The row indices
     * @param _columnIndices The column indices
     * @return Sliced data
     */
    public static IExpressionData sliceView(final IExpressionData dataset,
                                            int[] _rowIndices, int[] _columnIndices) {
        if (dataset instanceof ExpressionData) {
            return ((ExpressionData) dataset)
                    .slice(_rowIndices, _columnIndices);
        }
        if (_rowIndices == null) {
            _rowIndices = new int[dataset.getRowCount()];
            for (int i = dataset.getRowCount(); --i >= 0;) {
                _rowIndices[i] = i;
            }
        }
        if (_columnIndices == null) {
            _columnIndices = new int[dataset.getColumnCount()];
            for (int i = dataset.getColumnCount(); --i >= 0;) {
                _columnIndices[i] = i;
            }
        }
        final int[] rowIndices = _rowIndices;
        final int[] columnIndices = _columnIndices;

        return new IExpressionData() {

            public String getValueAsString(int row, int column) {
                return dataset.getValueAsString(rowIndices[row],
                        columnIndices[column]);
            }

            public double getValue(int row, int column) {
                return dataset.getValue(rowIndices[row], columnIndices[column]);
            }

            public String getRowName(int row) {
                return dataset.getRowName(rowIndices[row]);
            }

            public int getRowCount() {
                return rowIndices.length;
            }

            public int getColumnCount() {
                return columnIndices.length;
            }

            public String getColumnName(int column) {
                return dataset.getColumnName(columnIndices[column]);
            }

            public String getRowMetadata(int row, String name) {
                return dataset.getColumnMetadata(rowIndices[row], name);
            }

            public String getColumnMetadata(int column, String name) {
                return dataset.getColumnMetadata(columnIndices[column], name);
            }

            public int getRowIndex(String rowName) {
                throw new UnsupportedOperationException();
            }

            public int getColumnIndex(String columnName) {
                throw new UnsupportedOperationException();
            }

            public Object getData(int row, int column, String name) {
                return dataset.getData(rowIndices[row], columnIndices[column],
                        name);
            }

            public String getDataName(int index) {
                return dataset.getDataName(index);
            }

            public int getDataCount() {
                return dataset.getDataCount();
            }

        };
    }

    public static IExpressionData createRandomData(int rows, int columns) {
        double[][] data = new double[rows][columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                data[i][j] = Math.random() * 300 + 1;
            }
        }
        DoubleMatrix2D matrix = new DoubleMatrix2D(data);
        ExpressionData d = new ExpressionData(matrix, null, null);
        return d;
    }
}
