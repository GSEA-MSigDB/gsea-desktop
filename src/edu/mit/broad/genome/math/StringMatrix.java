/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.math;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.utils.ImmutedException;

import org.ujmp.core.Matrix;
import org.ujmp.core.calculation.Calculation.Ret;
import org.ujmp.core.enums.ValueType;
import org.ujmp.core.stringmatrix.impl.DefaultDenseStringMatrix2D;
import org.ujmp.core.util.MathUtil;

public class StringMatrix {

    // Wrapped data structure; to be unwrapped and used directly if possible.
    // If this remains wrapped, can probably just use Matrix instead of the direct Impl class.
    private final DefaultDenseStringMatrix2D matrix;
    
    /**
     * The number of rows in this matrix.
     */
    private int fRowCnt;

    /**
     * The number of columns in this matrix.
     */
    private int fColCnt;
    // TODO: evaluate whether we really need to worry about this.
    private boolean fImmuted;

    public void replace(final String thisStr, final String withThisStr) {
        // @NOTE HACK
        if (!thisStr.equals("NaN")) {
            checkImmuted();
        }

        matrix.replace(Ret.ORIG, thisStr, withThisStr);
    }

    /**
     * Constructs an fRowCnt by fColCnt all zero matrix. (as change)
     * Note that even though row and column numbering begins with
     * zero, fRowCnt and fColCnt will be one larger than the maximum
     * possible matrix index values.
     *
     * @param nrows number of rows in this matrix.
     * @param ncols number of columns in this matrix.
     */
    public StringMatrix(final int nrows, final int ncols) {

        if (nrows < 0) {
            throw new NegativeArraySizeException(nrows + " < 0");
        }

        if (ncols < 0) {
            throw new NegativeArraySizeException(ncols + " < 0");
        }

        this.fRowCnt = nrows;
        this.fColCnt = ncols;
        
        matrix = (DefaultDenseStringMatrix2D)Matrix.Factory.zeros(ValueType.STRING, nrows, ncols);
        matrix.fill(Ret.ORIG, "");
    }

    /**
     * Constructs a new StringMatrix and copies the initial values
     * from the parameter matrix.
     *
     * @param matrix the source of the initial values of the new StringMatrix
     *               <p/>
     *               as added: if shared values, then this matrix shares the elements with the specified amtric
     *               else the array is systemarrycopied
     */
    private StringMatrix(final StringMatrix matrix) {

        this.fRowCnt = matrix.fRowCnt;
        this.fColCnt = matrix.fColCnt;

        this.fImmuted = false;
        
        this.matrix = (DefaultDenseStringMatrix2D)matrix.matrix.copy();
    }

    /**
     * the cloned matrix is NOT immutable even if the matrix it was cloned from was.
     */
    public StringMatrix cloneDeep() {
        return new StringMatrix(this);
    }

    /**
     * Returns the number of rows in this matrix.
     *
     * @return number of rows in this matrix
     */
    public int getNumRow() {
        return MathUtil.longToInt(matrix.getRowCount());
    }

    /**
     * Returns the number of columns in this matrix.
     *
     * @return number of columns in this matrix
     */
    public int getNumCol() {
        return MathUtil.longToInt(matrix.getColumnCount());
    }

    /**
     * Retrieves the value at the specified row and column of this matrix.
     *
     * @param row    the row number to be retrieved (zero indexed)
     * @param column the column number to be retrieved (zero indexed)
     * @return the value at the indexed element
     */
    public String getElement(int row, int column) {

        if (fRowCnt <= row) {
            throw new ArrayIndexOutOfBoundsException("row:" + row + " > matrix's fRowCnt:"
                    + fRowCnt);
        }

        if (row < 0) {
            throw new ArrayIndexOutOfBoundsException("row:" + row + " < 0");
        }

        if (fColCnt <= column) {
            throw new ArrayIndexOutOfBoundsException("column:" + column + " > matrix's fColCnt:"
                    + fColCnt);
        }

        if (column < 0) {
            throw new ArrayIndexOutOfBoundsException("column:" + column + " < 0");
        }

        return matrix.getString(row, column);
    }

    // Looks like just a way to create an ID per location; unclear if it really must be done this way
    // or from within this class.  Looks like we could use anything at all, so long as it's unique.
    public int getElementPos(int row, int col) {

        if (row >= getNumRow()) {
            throw new ArrayIndexOutOfBoundsException("row:" + row + " > matrix's fRowCnt:" + getNumRow());
        }

        if (col >= getNumCol()) {
            throw new ArrayIndexOutOfBoundsException("col:" + col + " > matrix's fColCnt:" + getNumCol());
        }

        return (row * fColCnt) + col;
    }

    public void setElement(int row, int column, int value) {
        setElement(row, column, Integer.toString(value));
    }

    public void setElement(int row, int column, float value) {
        setElement(row, column, Float.toString(value));
    }

    /**
     * Modifies the value at the specified row and column of this matrix.
     *
     * @param row    the row number to be modified (zero indexed)
     * @param column the column number to be modified (zero indexed)
     * @param value  the new matrix element value
     */
    public void setElement(int row, int column, String value) {

        checkImmuted();

        if (fRowCnt <= row) {
            throw new ArrayIndexOutOfBoundsException("row:" + row + " > matrix's fRowCnt:"
                    + fRowCnt);
        }

        if (row < 0) {
            throw new ArrayIndexOutOfBoundsException("row:" + row + " < 0");
        }

        if (fColCnt <= column) {
            throw new ArrayIndexOutOfBoundsException("column:" + column + " > matrix's fColCnt:"
                    + fColCnt);
        }

        if (column < 0) {
            throw new ArrayIndexOutOfBoundsException("column:" + column + " < 0");
        }

        matrix.setString(value, row, column);
    }

    public void setElement(int row, int column, final Number value) {
        if (value != null) {
            this.setElement(row, column, value.toString());
        }
    }

    public void setElement(int row, int column, String[] values) {
        if (values == null || values.length == 0) {
            return;
        }

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) buf.append(Constants.INTRA_FIELD_DELIM);
            buf.append(values[i]);
        }

        this.setElement(row, column, buf.toString());
    }

    /**
     * a safe copy is returned.
     */
    // Might be able to modify the caller to make this method unnecessary.  
    // Not so simple here as the caller uses the copy.  However, we could probably modify that 
    // to use a Matrix supplied by selectColumn() instead.  Will not push this right now.
    public String[] getColumn(int col) {

        if (col < 0) {
            throw new ArrayIndexOutOfBoundsException("col:" + col + " < 0");
        }

        String[] ret = new String[fRowCnt];

        for (int i = 0; i < fRowCnt; i++) {
            ret[i] = matrix.getString(i, col);
        }

        // Can we do it faster?  This doesn't work:
//        DefaultDenseStringMatrix2D entireColumn = (DefaultDenseStringMatrix2D)matrix.selectColumns(Ret.LINK, col);
//        System.arraycopy(entireColumn.getStringArray(), 0, ret, 0, fRowCnt);
        
        return ret;
    }

    /**
     * Returns a string that contains the values of this StringMatrix.
     *
     * @return the String representation
     */
    // Suspect this is irrelevant; need to prove that.
    public String toString() {
        return this.matrix.toString();
    }

    /**
     * Returns a hash number based on the data values in this
     * object.  Two different StringMatrix objects with identical data values
     * (ie, returns true for equals(StringMatrix) ) will return the same hash
     * number.  Two objects with different data members may return the
     * same hash value, although this is not likely.
     *
     * @return the integer hash value
     */
    // Suspect this is irrelevant; need to prove that
    public int hashCode() {
        return this.matrix.hashCode();
    }

    /**
     * Returns true if all of the data members of StringMatrix4d m1 are
     * equal to the corresponding data members in this StringMatrix4d.
     *
     * @param m1 The matrix with which the comparison is made.
     * @return true or false
     */
    public boolean equals(final StringMatrix m1) {

        if (m1 == null) {
            return false;
        }

        if (m1.fRowCnt != fRowCnt) {
            return false;
        }

        if (m1.fColCnt != fColCnt) {
            return false;
        }

        for (int i = 0; i < fRowCnt; i++) {
            for (int j = 0; j < fColCnt; j++) {
//                if (elementData[i * fColCnt + j] != m1.elementData[i * fColCnt + j]) {
//                    return false;
//                }
            }
        }

        return true;
    }

    /**
     * Returns true if the Object o1 is of type StringMatrix and all of the data
     * members of t1 are equal to the corresponding data members in this
     * StringMatrix.
     *
     * @param o1 the object with which the comparison is made.
     */
    // Suspect this is irrelevant; need to prove that
    public boolean equals(Object o1) {
        return this.matrix.equals(o1);
    }

    public void setImmutable() {
        this.fImmuted = true;
    }

    private void checkImmuted() {

        if (fImmuted) {
            throw new ImmutedException();
        }
    }
}    // End StringMatrix
