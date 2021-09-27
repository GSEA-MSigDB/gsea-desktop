/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Initial implementation of unit tests for Vector.  Not all operations are covered as we are
 * in the process of deciding which are really needed and which are not.  
 * 
 * Also, certain tests are disabled while we evaluate handling of NaN values, empty structures,
 * and so on.
 */
public class VectorTest {
    final float[] allPos = new float[] { 5.1f, 33.0f, 3f, 0.1f, 99.4f };
    final float[] allNeg = new float[] { -8.15f, -43.8f, -3.6f, -0.1f, -192.6f };
    final float[] mixPosNeg = new float[] { 5.1f, 33.0f, 3f, -0.1f, -192.6f };
    final float[] mixHasNaNs = new float[] { 5.1f, 33.0f, Float.NaN, Float.NaN, -192.6f };
    final float[] empty = new float[] { };

    final Vector allPosVector = new Vector(allPos);
    final Vector allNegVector = new Vector(allNeg);
    final Vector mixPosNegVector = new Vector(mixPosNeg);
    final Vector mixHasNaNsVector = new Vector(mixHasNaNs);
    final Vector emptyVector = new Vector(empty);

    @Test
    void max_normalCasePos() {
        assertEquals(99.4f, allPosVector.max(), 0.0002d);
    }

    @Test
    void max_normalCaseNeg() {
        assertEquals(-0.1f, allNegVector.max(), 0.0002d);
    }

    @Test
    void max_normalCaseMix() {
        assertEquals(33.0f, mixPosNegVector.max(), 0.0002d);
    }

    @Test
    void max_normalCaseNaN() {
        assertEquals(33.0f, mixHasNaNsVector.max(), 0.0002d);
    }

    @Test
    void max_normalEmpty() {
        assertTrue(Double.isNaN(emptyVector.max()));
    }

    @Test
    void sum_normalCasePos() {
        assertEquals(140.6d, allPosVector.sum(), 0.0002d);
        assertEquals(140.6d, allPosVector.sumNaNsafe(), 0.0002d);
    }

    @Test
    void sum_normalCaseNeg() {
        assertEquals(-248.25d, allNegVector.sum(), 0.0002d);
        assertEquals(-248.25d, allNegVector.sumNaNsafe(), 0.0002d);
    }

    @Test
    void sum_normalCaseMix() {
        assertEquals(-151.6d, mixPosNegVector.sum(), 0.0002d);
        assertEquals(-151.6d, mixPosNegVector.sumNaNsafe(), 0.0002d);
    }

    /**
     * Note that Vector.sum() is intended to be called on a "NaN-less" vector and does not check or ignore NaN
     * values.  If it *is* called on a vector containing NaNs then the result will be NaN.  Thus the following test
     * expects NaN even though the method should not be used that way.
     */
    @Test
    void sum_expectNaN() {
        assertTrue(Double.isNaN(mixHasNaNsVector.sum()));
    }

    @Test
    void sumNaNsafe_normalCaseNaN() {
        assertEquals(-154.5d, mixHasNaNsVector.sumNaNsafe(), 0.0002d);
    }

    @Test
    void sum_normalCaseEmpty() {
        assertTrue(Double.isNaN(emptyVector.sum()));
        assertTrue(Double.isNaN(emptyVector.sumNaNsafe()));
    }

    @Test
    void mean_normalCasePos() {
        assertEquals(28.12d, allPosVector.mean(), 0.0002d);
        assertEquals(28.12d, allPosVector.meanNaNsafe(), 0.0002d);
    }

    @Test
    void mean_normalCaseNeg() {
        assertEquals(-49.65d, allNegVector.mean(), 0.0002d);
        assertEquals(-49.65d, allNegVector.meanNaNsafe(), 0.0002d);
    }

    @Test
    void mean_normalCaseMix() {
        assertEquals(-30.32d, mixPosNegVector.mean(), 0.0002d);
        assertEquals(-30.32d, mixPosNegVector.meanNaNsafe(), 0.0002d);
    }

    /**
     * Note that Vector.mean() is intended to be called on a "NaN-less" vector and does not check or ignore NaN
     * values.  If it *is* called on a vector containing NaNs then the result will be NaN.  Thus the following test
     * expects NaN even though the method should not be used that way.
     */
    @Test
    void mean_expectNaN() {
        assertTrue(Double.isNaN(mixHasNaNsVector.mean()));
    }
    
    @Test
    void meanNaNsafe_normalCaseNaN() {
        assertEquals(-51.5d, mixHasNaNsVector.meanNaNsafe(), 0.0002d);
    }

    @Test
    void mean_normalCaseEmpty() {
        assertTrue(Double.isNaN(emptyVector.mean()));
        assertTrue(Double.isNaN(emptyVector.meanNaNsafe()));
    }

    @Test
    void median_normalCasePos() {
        assertEquals(5.1d, allPosVector.median(), 0.0002d);
    }

    @Test
    void median_normalCaseNeg() {
        assertEquals(-8.15d, allNegVector.median(), 0.0002d);
    }

    @Test
    void median_normalCaseMix() {
        assertEquals(3d, mixPosNegVector.median(), 0.0002d);
    }

    @Test
    void median_normalCaseNaN() {
        assertEquals(5.1d, mixHasNaNsVector.median(), 0.0002d);
    }

    @Test
    void median_normalCaseEmpty() {
        assertTrue(Double.isNaN(emptyVector.median()));
    }
}
