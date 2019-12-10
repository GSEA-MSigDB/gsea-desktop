/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.math;

import edu.mit.broad.genome.MismatchedSizeException;

import java.util.List;

/**
 * Vector of scores
 * that can also hold an address for every element
 * <p/>
 * So, every element: score (as usual) + address (eg a row index in a ds)
 * <p/>
 * So really a collection of DoubleElements
 *
 * @author
 * @version %I%, %G%
 */

//Dont extend, rather compose. There are methods in Vector that change
//the elements/element order and we dont want that hapenning without
//us knowing (i.e dont want the order changing without us knowing)
//public class AddressedVector extends Vector {

public class AddressedVector {

    /**
     * creates a new integer array with elements as all consecutive integers between
     * min and max INclusive.
     * Length = max - min + 1
     */
    // TODO: Look for another implementation, then inline.  Apache Commons?
    private static int[] rangeAsElements(final int min, final int max) {
    
        if (max < min) {
            throw new IllegalArgumentException("Max: " + max + " less than min: " + min);
        }
    
        int[] ret = new int[max - min + 1];
        int curr = min;
    
        for (int i = 0; i < ret.length; i++) {
            ret[i] = curr++;
        }
    
        return ret;
    }

    /**
     * @maint IMP IMP IMP
     * If class vars are added make sure to check the constructors below
     * to see if they need to be updated
     * <p/>
     * The indices corresponding to the values/metrics/scores in fVector
     * <p/>
     * <p/>
     * The indices corresponding to the values/metrics/scores in fVector
     */

    // IMP IMMUTABILITY OF THIS OBJECT IS IMPORTANT

    /**
     * The indices corresponding to the values/metrics/scores in fVector
     */
    private int[] fAddresses;

    /**
     * Typically holds some set of values/metrics/scores
     */
    private Vector fVector;

    /**
     * Class Constructor.
     * Data is NOT shared - it is copied.
     * Indices are inferred (in order) from the specified vector
     *
     * @param v
     */
    public AddressedVector(final Vector v, final boolean share) {
        final int[] addresses = rangeAsElements(0, _max(v));
		if (v == null) {
		    throw new IllegalArgumentException("Param v cannot be null");
		}
		
		if (addresses == null) {
		    throw new IllegalArgumentException("Parameter addresses cannot be null");
		}
		
		if (share) {
		    init(v, addresses);
		} else {
		    Vector dupv = new Vector(v);
		    int[] dadd = new int[addresses.length];
		    System.arraycopy(addresses, 0, dadd, 0, addresses.length);
		    init(dupv, dadd);
		}
    }

    private static int _max(Vector v) {
        int max = v.getSize() - 1;
        if (v.getSize() == 0) {
            max = 0;
        }
        return max;
    }

    /**
     * Data is NOT shared - it is copied.
     * Copied from 0 to numElements2Copy
     *
     * @param numElements2Copy
     * @param iv
     */
    public AddressedVector(final int numElements2Copy, final AddressedVector iv) {
        Vector v = new Vector(numElements2Copy, iv.fVector);
        int[] addr = new int[numElements2Copy];
        System.arraycopy(iv.fAddresses, 0, addr, 0, numElements2Copy); // @note duplication
        init(v, addr);
    }

    /**
     * Class Constructor.
     * Data is NOT shared - it is copied.
     * list of DoubleElements
     */
    public AddressedVector(final List<DoubleElement> listWithDoubleElements) {

        if (listWithDoubleElements == null) {
            throw new IllegalArgumentException("Param listWithDoubleElements cannot be null");
        }

        Vector v = new Vector((DoubleElement.valueArray(listWithDoubleElements)));
        int[] addr = DoubleElement.indexArray(listWithDoubleElements);
        init(v, addr);
    }

    // things must already be duplicated (if necessary) before calling here
    private void init(final Vector v, final int[] addresses) {

        if (v == null) {
            throw new IllegalArgumentException("Parameter v cannot be null");
        }

        if (addresses == null) {
            throw new IllegalArgumentException("Parameter addresses cannot be null");
        }

        if (v.getSize() != addresses.length) {
            throw new MismatchedSizeException("Unequal lengths - Vector", v.getSize(),
                    "Index addresses", addresses.length);
        }

        this.fVector = v;
        this.fAddresses = addresses;
        this.fVector.setImmutable(); // @note IMP, so that if shared doesnt change on us
    }

    public int getAddress(final int vector_element_index) {
        return fAddresses[vector_element_index];
    }

    public float getScore(final int vector_element_index) {
        return fVector.getElement(vector_element_index);
    }

    public Vector getScoresV(final boolean clonedCopy) {
        return new Vector(fVector, clonedCopy);
    }

    public int getSize() {
        return fVector.getSize();
    }

}    // End AddressedVector


