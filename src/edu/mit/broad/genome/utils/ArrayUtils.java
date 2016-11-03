/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.utils;

/**
 * Native array related utilities.
 * Functionally extends java.util.Arrays.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ArrayUtils {

    private ArrayUtils() {
    }

    /**
     * creates a new integer arry with elements as all consecutive integers between
     * min and max INclusive.
     * Length = max - min + 1
     */
    // TODO: Look for another implementation, then inline.  Apache Commons?
    public static int[] rangeAsElements(final int min, final int max) {

        if (max < min) {
            throw new IllegalArgumentException("Max: " + max + " less than min: " + min);
        }

        int[] ret = new int[max - min + 1];
        int curr = min;

        for (int i = 0; i < (max - min + 1); i++) {
            ret[i] = curr++;
        }

        return ret;
    }
}    // End ArrayUtils
