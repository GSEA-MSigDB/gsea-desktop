/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.math;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Initial implementation of unit tests for XMath.  Not all operations are covered as we are
 * in the process of deciding which are really needed and which are not.  
 * 
 * Also, certain tests are disabled while we evaluate handling of NaN values, empty structures,
 * and so on.
 */
public class XMathTest {
    final float[] allPos = new float[] { 5.1f, 33.0f, 3f, 0.1f, 99.4f };
    final float[] allNeg = new float[] { -8.15f, -43.8f, -3.6f, -0.1f, -192.6f };
    final float[] mixPosNeg = new float[] { 5.1f, 33.0f, 3f, -0.1f, -192.6f };
    final float[] mixHasNaNs = new float[] { 5.1f, 33.0f, Float.NaN, Float.NaN, -192.6f };
    final float[] empty = new float[] { };

    final Vector[] smallVectorArray = new Vector[] {
            new Vector(allPos), new Vector(allNeg), new Vector(mixPosNeg)
    };
    
    final float[] maxSmallResult = new float[] { 5.1f, 33.0f, 3f, 0.1f, 99.4f };
    final float[] sumSmallResult = new float[] { 2.05f, 22.2f, 2.4f, -0.1f, -285.8f };
    final float[] meanSmallResult = new float[] { 0.68333f, 7.4f, 0.8f, -0.033333f, -95.26667f };
    final float[] medianSmallResult = new float[] { 5.1f, 33.0f, 3f, -0.1f, -192.6f };

    final Vector[] smallVectorWithNaNArray = new Vector[] {
            new Vector(allPos), new Vector(allNeg), new Vector(mixHasNaNs)
    };
    
    final float[] maxSmallWithNaNResult = new float[] { 5.1f, 33.0f, 3f, 0.1f, 99.4f };
    final float[] sumSmallWithNaNResult = new float[] { 2.05f, 22.2f, -0.6f, 0.0f, -285.8f };
    final float[] meanSmallWithNaNResult = new float[] { 0.68333f, 7.4f, -0.3f, 0.0f, -95.2667f };
    final float[] medianSmallWithNaNResult = new float[] { 5.1f, 33.0f, -0.3f, 0.0f, -192.6f };
    
    final Vector[] largeVectorArray = new Vector[] {
            new Vector(new float[] {5.1f, 33.0f}), new Vector(new float[] {5.7f, 3.8f}), new Vector(new float[] {-4.1f, 9.44f}), 
            new Vector(new float[] {6.8f, 4.0f}), new Vector(new float[] {-5.7f, Float.NaN}), new Vector(new float[] {4.89f, 19.14f}), 
            new Vector(new float[] {Float.NaN, 6.2f}), new Vector(new float[] {-6.1f, 51.0f}), new Vector(new float[] {-0.1f, -0.24f}), 
            new Vector(new float[] {14.2f, -9.44f}), new Vector(new float[] {15.2f, Float.NaN}), new Vector(new float[] {-224.1f, 8.7f}), 
            new Vector(new float[] {1.4f, -12.3f}), new Vector(new float[] {6.72f, 43f}), new Vector(new float[] {4.1f, Float.NaN}) 
    };
    
    final float[] maxLargeResult = new float[] { 15.2f, 51.0f };
    final float[] sumLargeResult = new float[] { -175.99f, 156.31f };
    final float[] meanLargeResult = new float[] { 83.999f, 13.025833f };
    final float[] medianLargeResult = new float[] { 4.495f, 7.45f };

    @Test
    void max_normalCasePos() {
        assertEquals(99.4f, XMath.max(allPos), 0.0002f);
    }

    @Test
    void max_normalCaseNeg() {
        assertEquals(-0.1f, XMath.max(allNeg), 0.0002f);
    }

    @Test
    void max_normalCaseMix() {
        assertEquals(33.0f, XMath.max(mixPosNeg), 0.0002f);
    }

    @Test
    void max_normalCaseNaN() {
        assertEquals(33.0f, XMath.max(mixHasNaNs), 0.0002f);
    }

    @Test
    void max_errorEmpty() {
        assertThrows(IllegalArgumentException.class,  () -> XMath.max(empty));
    }

    @Test
    void sum_normalCasePos() {
        assertEquals(140.6f, XMath.sum(allPos), 0.0002f);
    }

    @Test
    void sum_normalCaseNeg() {
        assertEquals(-248.25f, XMath.sum(allNeg), 0.0002f);
    }

    @Test
    void sum_normalCaseMix() {
        assertEquals(-151.6f, XMath.sum(mixPosNeg), 0.0002f);
    }

    @Disabled("Disabled until NaN handling is fixed")
    @Test
    void sum_normalCaseNaN() {
        assertEquals(-154.5f, XMath.sum(mixHasNaNs), 0.0002f);
    }

    @Disabled("Disabled until NaN handling is fixed")
    @Test
    void sum_normalCaseEmpty() {
        assertTrue(Float.isNaN(XMath.sum(empty)));
    }

    @Test
    void mean_normalCasePos() {
        assertEquals(28.12f, XMath.mean(allPos), 0.0002f);
    }

    @Test
    void mean_normalCaseNeg() {
        assertEquals(-49.65f, XMath.mean(allNeg), 0.0002f);
    }

    @Test
    void mean_normalCaseMix() {
        assertEquals(-30.32f, XMath.mean(mixPosNeg), 0.0002f);
    }

    @Disabled("Disabled until NaN handling is fixed")
    @Test
    void mean_normalCaseNaN() {
        assertEquals(30.9f, XMath.mean(mixHasNaNs), 0.0002f);
    }

    @Test
    void mean_normalCaseEmpty() {
        assertTrue(Float.isNaN(XMath.mean(empty)));
    }

    @Test
    void median_normalCasePos() {
        assertEquals(5.1f, XMath.median(allPos), 0.0002f);
    }

    @Test
    void median_normalCaseNeg() {
        assertEquals(-8.15f, XMath.median(allNeg), 0.0002f);
    }

    @Test
    void median_normalCaseMix() {
        assertEquals(3f, XMath.median(mixPosNeg), 0.0002f);
    }

    @Disabled("Disabled until NaN handling is fixed")
    @Test
    void median_normalCaseNaN() {
        assertEquals(5.1f, XMath.median(mixHasNaNs), 0.0002f);
    }

    @Test
    void median_normalCaseEmpty() {
        assertTrue(Float.isNaN(XMath.median(empty)));
    }
    
    @Test
    void max_smallVector() {
        assertArrayEquals(maxSmallResult, XMath.maxVector(smallVectorArray).elementData, 0.0002f);
    }
    
    @Test
    void max_smallVectorWithNaN() {
        assertArrayEquals(maxSmallWithNaNResult, XMath.maxVector(smallVectorWithNaNArray).elementData, 0.0002f);
    }
    
    @Test
    void max_largeVector() {
        assertArrayEquals(maxLargeResult, XMath.maxVector(largeVectorArray).elementData, 0.0002f);
    }
    
    @Test
    void sum_smallVector() {
        assertArrayEquals(sumSmallResult, XMath.sumVector(smallVectorArray).elementData, 0.0002f);
    }
    
    @Disabled("Disabled until NaN handling is fixed")
    @Test
    void sum_smallVectorWithNaN() {
        assertArrayEquals(sumSmallWithNaNResult, XMath.sumVector(smallVectorWithNaNArray).elementData, 0.0002f);
    }
    
    @Disabled("Disabled until NaN handling is fixed")
    @Test
    void sum_largeVector() {
        assertArrayEquals(sumLargeResult, XMath.sumVector(largeVectorArray).elementData, 0.0002f);
    }
    
    @Test
    void mean_smallVector() {
        assertArrayEquals(meanSmallResult, XMath.meanVector(smallVectorArray).elementData, 0.0002f);
    }
    
    @Disabled("Disabled until NaN handling is fixed")
    @Test
    void mean_smallVectorWithNaN() {
        assertArrayEquals(meanSmallWithNaNResult, XMath.meanVector(smallVectorWithNaNArray).elementData, 0.0002f);
    }
    
    @Disabled("Disabled until NaN handling is fixed")
    @Test
    void mean_largeVector() {
        assertArrayEquals(meanLargeResult, XMath.meanVector(largeVectorArray).elementData, 0.0002f);
    }
    
    @Test
    void median_smallVector() {
        assertArrayEquals(medianSmallResult, XMath.medianVector(smallVectorArray).elementData, 0.0002f);
    }
    
    @Disabled("Disabled until NaN handling is fixed")
    @Test
    void median_smallVectorWithNaN() {
        assertArrayEquals(medianSmallWithNaNResult, XMath.medianVector(smallVectorWithNaNArray).elementData, 0.0002f);
    }
    
    @Disabled("Disabled until NaN handling is fixed")
    @Test
    void median_largeVector() {
        assertArrayEquals(medianLargeResult, XMath.medianVector(largeVectorArray).elementData, 0.0002f);
    }
}
