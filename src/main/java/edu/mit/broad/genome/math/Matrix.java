/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.math;

import edu.mit.broad.genome.objects.AbstractObject;
import edu.mit.broad.genome.utils.ImmutedException;
import gnu.trove.TFloatArrayList;

import org.ujmp.core.calculation.Calculation.Ret;
import org.ujmp.core.enums.ValueType;
import org.ujmp.core.floatmatrix.DenseFloatMatrix2D;
import org.ujmp.core.floatmatrix.impl.DefaultDenseFloatMatrix2D;

public class Matrix extends AbstractObject {

    // Tracking a count of the matrix instances created; tracked in the superclass.
    // TODO: Evaluate if it's really needed.
    private static int name_cnt;

    // Wrapped data structure; to be unwrapped and used directly if possible.
    private DenseFloatMatrix2D ourMatrix;
    
    /**
     * The number of rows in this matrix.
     */
    private int fRowCnt;

    /**
     * The number of columns in this matrix.
     */
    private int fColCnt;

    /**
     * Flag for whether this Matrix can have its values altered or not
     */
    // TODO: evaluate whether we really need to worry about this.
    private boolean fImmuted;

    /**
     * Class Constructor.
     * Constructs an fRowCnt by fColCnt all zero matrix. (as change)
     * Note that even though row and column numbering begins with
     * zero, fRowCnt and fColCnt will be one larger than the maximum
     * possible matrix index values.
     *
     * @param nrows number of rows in this matrix.
     * @param ncols number of columns in this matrix.
     */
    public Matrix(int nrows, int ncols) {
        super.initialize("matrix_" + name_cnt++);
        if (nrows < 0) {
            throw new NegativeArraySizeException(nrows + " < 0");
        }

        if (ncols < 0) {
            throw new NegativeArraySizeException(ncols + " < 0");
        }

        this.fRowCnt = nrows;
        this.fColCnt = ncols;
        this.ourMatrix = (DenseFloatMatrix2D)org.ujmp.core.Matrix.Factory.zeros(ValueType.FLOAT, nrows, ncols);
    }

    public String getQuickInfo() {
        return getNumRow() + " x " + getNumCol();
    }

    public Matrix(int nrows, int ncols, TFloatArrayList floats) {

        if (nrows < 0) {
            throw new NegativeArraySizeException(nrows + " < 0");
        }

        if (ncols < 0) {
            throw new NegativeArraySizeException(ncols + " < 0");
        }

        this.fRowCnt = nrows;
        this.fColCnt = ncols;
        float[] elementData = floats.toNativeArray();
        
        // TODO: Push this up into the caller (in DataframeParser).
        // May not be necessary, however, as we may not actually ever use that parser.
        // Could be vestigial, required by unused file format code.  Unsure yet.
        // Might have faster options based on e.g.
        //DenseFloatMatrix2D replace = org.ujmp.core.Matrix.Factory.linkFromArray(array);
        this.ourMatrix = (DenseFloatMatrix2D)org.ujmp.core.Matrix.Factory.zeros(ValueType.FLOAT, nrows, ncols);
        for (int row = 0; row < nrows; row++) {
            for (int col = 0; col < ncols; col++) {
                ourMatrix.setFloat(elementData[row * fColCnt + col], row, col);
            }
        }
    }

    /**
     * Class Constructor.
     * Constructs a new Matrix and copies the initial values
     * from the parameter matrix.
     *
     * @param matrix the source of the initial values of the new Matrix
     *               <p/>
     *               as added: if shared values, then this matrix shares the elements with the specified amtric
     *               else the array is systemarrycopied
     */
    public Matrix(Matrix matrix) {
        if (matrix == null) {
            throw new IllegalArgumentException("Param matrix cannot be null");
        }

        this.fRowCnt = matrix.fRowCnt;
        this.fColCnt = matrix.fColCnt;

        // DONT thats the whole point! (often)
        //this.fImmutable = matrix.fImmutable;
        this.fImmuted = false;
        
        // Safe/fast?  Not sure yet...
        // Or, use copy()?  matrix.ourMatrix.clone() works for StringMatrix, not available for this?
        this.ourMatrix = (DefaultDenseFloatMatrix2D)org.ujmp.core.Matrix.Factory.copyFromMatrix(matrix.ourMatrix);
    }

    /**
     * The cloned matrix is NOT immutable even if the matrix it was cloned from was.
     */
    public Matrix cloneDeep() {
        return new Matrix(this);
    }

    /**
     * Returns the number of rows in this matrix.
     *
     * @return number of rows in this matrix
     */
    public int getNumRow() {
        return fRowCnt;
        // Best to change callers to expect long.
//        return MathUtil.longToInt(ourMatrix.getRowCount());
    }

    /**
     * Returns the number of colmuns in this matrix.
     *
     * @return number of columns in this matrix
     */
    public int getNumCol() {
        return fColCnt;
        // Best to change callers to expect long.
//        return MathUtil.longToInt(ourMatrix.getColumnCount());
    }

    public int getDim() {
        return getNumRow() * getNumCol();
        // Best to change callers to expect long.
        // ourMatrix.getDimensionCount();  // Better?
//        return MathUtil.longToInt(ourMatrix.getRowCount() * ourMatrix.getColumnCount());
    }

    /**
     * Retrieves the value at the specified row and column of this matrix.
     *
     * @param row    the row number to be retrieved (zero indexed)
     * @param column the column number to be retrieved (zero indexed)
     * @return the value at the indexed element
     */
    public float getElement(int row, int column) {
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
        
        return this.ourMatrix.getFloat(row, column);
    }

    /**
     * Modifies the value at the specified row and column of this matrix.
     *
     * @param row    the row number to be modified (zero indexed)
     * @param column the column number to be modified (zero indexed)
     * @param value  the new matrix element value
     */
    public void setElement(int row, int column, float value) {

        checkImmutable();

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

        this.ourMatrix.setFloat(value, row, column);
    }

    /**
     * A safe copy is returned.
     * but note efficient as directly copied into Vectors array
     */
    // TODO: look at having the various callers work with the Matrix directly.
    // I believe there's inefficiency built-in here due to the need to copy values
    // out to work on them row-wise (or column-wise, below).  It's too big to refactor 
    // right now and possibly not important.
    public Vector getRowV(int row) {

        if (fRowCnt <= row) {
            throw new ArrayIndexOutOfBoundsException("row:" + row + " > matrix's fRowCnt:"
                    + fRowCnt);
        }

        if (row < 0) {
            throw new ArrayIndexOutOfBoundsException("row:" + row + " < 0");
        }
        
        float[] rowContents = new float[fColCnt];
        for (int i = 0; i < fColCnt; i++) {
            rowContents[i] = ourMatrix.getFloat(row, i);
        }
        return new Vector(rowContents, false);
    }

    /**
     * Data is not shared
     *
     * @param col
     * @return
     */
    // TODO: look at having the various callers work with the Matrix directly.
    // As above.
    public Vector getColumnV(int col) {

        if (fColCnt <= col) {
            throw new ArrayIndexOutOfBoundsException("col:" + col + " > matrix's fColCnt:"
                    + fColCnt);
        }

        if (col < 0) {
            throw new ArrayIndexOutOfBoundsException("col:" + col + " < 0");
        }
        
        float[] colContents = new float[fRowCnt];
        for (int i = 0; i < fRowCnt; i++) {
            colContents[i] = ourMatrix.getFloat(i, col);
        }
        return new Vector(colContents, false);
    }

    private Vector fColMaxes;
    
    public Vector getColumnMaxes() {
        // Code is unsafe in presence of mutable data!  Keeping that assumption for now. 

        if (fColMaxes == null) {
            // Hypothetical alternative here.  Would wrap this into a Util class (or a wrapper like this one)
            // Or, work with the cols: List<org.ujmp.core.Matrix> columnList = ourMatrix.getColumnList();
            org.ujmp.core.Matrix colMaxes = ourMatrix.max(Ret.NEW, org.ujmp.core.Matrix.ROW);
            fColMaxes = new Vector(getNumCol());
            for (int c = 0; c < getNumCol(); c++) {
                fColMaxes.setElement(c, colMaxes.getAsFloat(0, c));
            }
        }

        return fColMaxes;
    }

    private Vector fColMins;

    public Vector getColumnMins() {
        // Code is unsafe in presence of mutable data!  Keeping that assumption for now. 

        if (fColMins == null) {
            // Hypothetical alternative here.  Would wrap this into a Util class (or a wrapper like this one)
            org.ujmp.core.Matrix colMins = ourMatrix.min(Ret.NEW, org.ujmp.core.Matrix.ROW);
            fColMins = new Vector(getNumCol());
            for (int c = 0; c < getNumCol(); c++) {
                fColMins.setElement(c, colMins.getAsFloat(0, c));
            }
        }

        return fColMins;
    }
    
    /**
     * Copy the values from the array into the specified row of this
     * matrix.
     *
     * @param row   the row of this matrix into which the array values
     *              will be copied.
     * @param array the source array
     */
    public void setRow(int row, float array[]) {

        checkImmutable();

        if (fRowCnt <= row) {
            throw new ArrayIndexOutOfBoundsException("row:" + row + " > matrix's fRowCnt:"
                    + fRowCnt);
        }

        if (row < 0) {
            throw new ArrayIndexOutOfBoundsException("row:" + row + " < 0");
        }

        if (array.length < fColCnt) {
            throw new ArrayIndexOutOfBoundsException("array length:" + array.length
                    + " < matrix's fColCnt=" + fColCnt);
        }

        for (int i = 0; i < fColCnt; i++) {
            ourMatrix.setFloat(array[i], row, i);
        }
    }

    /**
     * Data is NOT shared
     * Copy the values from the array into the specified row of this
     * matrix.
     *
     * @param row    the row of this matrix into which the vector values
     *               will be copied.
     * @param vector the source vector
     */
    public void setRow(int row, Vector vector) {

        checkImmutable();

        if (fRowCnt <= row) {
            throw new ArrayIndexOutOfBoundsException("row:" + row + " > matrix's fRowCnt:"
                    + fRowCnt);
        }

        if (row < 0) {
            throw new ArrayIndexOutOfBoundsException("row:" + row + " < 0");
        }

        int vecSize = vector.getSize();

        if (vecSize < fColCnt) {
            throw new ArrayIndexOutOfBoundsException("vector's size:" + vecSize
                    + " < matrix's fColCnt=" + fColCnt);
        }

        for (int i = 0; i < fColCnt; i++) {
            ourMatrix.setFloat(vector.getElement(i), row, i);
        }

        // Hypothetical alternative here.
//        DefaultDenseFloatMatrix2D entireRow = (DefaultDenseFloatMatrix2D)ourMatrix.selectRows(Ret.ORIG, row);
//        // This will apparently work; the matrix is exposing its internals
//        System.arraycopy(vector.elementData, 0, entireRow.getFloatArray(), 0, fColCnt);
    }

    /**
     * Copy the values from the array into the specified column of this
     * matrix.
     *
     * @param col    the column of this matrix into which the vector values
     *               will be copied.
     * @param vector the source vector
     */

    public void setColumn(int col, Vector vector) {
        checkImmutable();

        if (fColCnt <= col) {
            throw new ArrayIndexOutOfBoundsException("col:" + col + " > matrix's fColCnt="
                    + fColCnt);
        }

        if (col < 0) {
            throw new ArrayIndexOutOfBoundsException("col:" + col + " < 0");
        }

        int vecSize = vector.getSize();

        if (vecSize < fRowCnt) {
            throw new ArrayIndexOutOfBoundsException("vector size:" + vecSize
                    + " < matrix's fRowCnt=" + fRowCnt);
        }

        for (int i = 0; i < vector.getSize(); i++) {
            ourMatrix.setFloat(vector.getElement(i), i, col);
        }
    }

    /**
     * Returns a string that contains the values of this Matrix.
     *
     * @return the String representation
     */
    // Suspect this is irrelevant; need to prove that
    public String toString() {
        return ourMatrix.toString();
    }
    
    
    /**
     * Returns a hash number based on the data values in this
     * object.  Two different Matrix objects with identical data values
     * (ie, returns true for equals(Matrix) ) will return the same hash
     * number.  Two objects with different data members may return the
     * same hash value, although this is not likely.
     *
     * @return the integer hash value
     */
    // Suspect this is irrelevant; need to prove that
    public int hashCode() {
      return ourMatrix.hashCode();
    }

    /**
     * Returns true if the Object o1 is of type Matrix and all of the data
     * members of t1 are equal to the corresponding data members in this
     * Matrix.
     *
     * @param o1 the object with which the comparison is made.
     */
    // Suspect this is irrelevant; need to prove that
    public boolean equals(Object o1) {
        return ourMatrix.equals(o1);
    }

    /**
     * every element is this / m1
     *
     * @param m1
     */
    // Used only by BitSetDataset.  Need to figure out why we use floats for those functions; seems
    // like it should always be int or even boolean.  Review of that class seems like this is the
    // case; need to refactor my way towards that at some point, though.
    public void divide(Matrix m1, boolean setDivByZeroAsZero) {

        checkImmutable();

        _enforceEqualDimensions(this, m1);

        for (int i = 0; i < fRowCnt; i++) {
            for (int j = 0; j < fColCnt; j++) {
                float f1 = m1.ourMatrix.getFloat(i, j);
                if (setDivByZeroAsZero && f1 == 0) {
                    this.ourMatrix.setFloat(0.0f, i, j);
                } else {
                    float f2 = this.getElement(i, j);
                    this.ourMatrix.setFloat(f2 / f1, i, j);
                }
            }
        }
    }

    // Do we actually care about immutability?  Why?
    public void setImmutable() {
        this.fImmuted = true;
    }

    private void checkImmutable() {

        if (fImmuted) {
            throw new ImmutedException();
        }
    }

    private static void _enforceEqualDimensions(Matrix a, Matrix b) {
        if (a.getNumRow() != b.getNumRow()) {
            throw new IllegalArgumentException("Mismatched matrices: must be of equal row lengths a: " + a.getNumRow() + " b: " + b.getNumRow());
        }

        if (a.getNumCol() != b.getNumCol()) {
            throw new IllegalArgumentException("Mismatched matrices: must be of equal col lengths a: " + a.getNumCol() + " b: " + b.getNumCol());
        }
    }
}        // End Matrix
