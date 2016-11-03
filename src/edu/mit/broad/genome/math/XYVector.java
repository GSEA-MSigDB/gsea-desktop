/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.math;

import edu.mit.broad.genome.MismatchedSizeException;

/**
 * Contruct to hold 2 related Vectors
 * Not intended to be mutability safe.
 * <p/>
 * Vectors MUST be equal size
 */
public class XYVector {

    public final Vector x;
    public final Vector y;

    public XYVector(final Vector x, final Vector y) {

        this.x = x;
        this.y = y;

        if (x.getSize() != y.getSize()) {
            throw new MismatchedSizeException(x, y);
        }

    }

    public int getSize() {
        return x.getSize();
    }

} // End class XYVector

