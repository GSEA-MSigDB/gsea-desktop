/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.math.Matrix;

/**
 * Could not use boolean matrix as need a, p and m
 */
// TODO: replace with an enum for A/P/M and an ObjectMatrix.
public class APMMatrix extends Matrix {

    public static final String PRESENT_STR = "P";
    public static final String ABSENT_STR = "A";
    public static final String MISSING_STR = "M";
    public static final float PRESENT = (float) 1.0;
    public static final float ABSENT = (float) 0.0;

    // using NaN doesnt work as for some reason Float.NaN == Float.NaN doesnt work
    // @see IsMissing below
    // -> solved -- should use Float.isNaN() not '=='
    public static final float MISSING = Float.NaN;

    /**
     * Class Constructor.
     */
    public APMMatrix(final int nrows, final int ncols) {
        super(nrows, ncols);
    }

    public char getElement_char(int row, int col) {
        float f = getElement(row, col);
        if (f == PRESENT) {
            return PRESENT_STR.toCharArray()[0];
        } else if (f == ABSENT) {
            return ABSENT_STR.toCharArray()[0];
        } else if (Float.isNaN(f)) {
            return MISSING_STR.toCharArray()[0];
        } else {
            throw new IllegalStateException("Unknown value: " + f + " at row: " + row + " col: " + col);
        }
    }

    public static float valueOf(String s) {

        if (s == null) {
            throw new NullPointerException("Null value of param not allowed");
        }

        if (s.equalsIgnoreCase(PRESENT_STR)) {
            return PRESENT;
        } else if (s.equalsIgnoreCase(ABSENT_STR)) {
            return ABSENT;
        } else if (s.equalsIgnoreCase(MISSING_STR)) {
            return MISSING;
        } else if (s.equals("0")) {
            return MISSING;    // @todo confirm see breast.res in gcm dataset
        } else {
            throw new IllegalArgumentException("Unknown AP call value >" + s + "<");
        }
    }
}    // End APMatrix
