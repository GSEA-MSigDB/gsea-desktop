/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome;

import edu.mit.broad.genome.math.Vector;

/**
 * For use by classes that wich to make an object immutable.
 * (in cases where the 2 class object iimmuatbelobject pattern is hard
 * (or unintuitive) to implement
 * <p/>
 * Immuted -> made immutable
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class MismatchedSizeException extends RuntimeException {

    private static final String ERROR = "Mismatched col/row//array/vector lengths";

    public MismatchedSizeException() {
        super(ERROR);
    }

    public MismatchedSizeException(Vector a, Vector b, int bIndex) {
        super(ERROR + " a: " + a.getSize() + " & b: " + b.getSize() + " at b_index " + bIndex);
    }

    public MismatchedSizeException(Vector a, Vector b) {
        super(ERROR + " a: " + a.getSize() + " & b: " + b.getSize());
    }

    public MismatchedSizeException(String arrayEnglishNameA, int lenA, String arrayEnglishnameB, int lenB) {
        super(ERROR + " for " + arrayEnglishNameA + ": " + lenA + " & " + arrayEnglishnameB + " :" + lenB);
    }

}    // End MismatchedSizeException

