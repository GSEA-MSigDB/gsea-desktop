/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An object encapsulating a double element of an array.
 * Captures it (double) value and position in the array.
 *
 * @author Aravind Subramanian, David Eby
 */
public class DoubleElement {
    public int fIndex;
    public double fValue;

    public DoubleElement(int index, double value) {
        this.fIndex = index;
        this.fValue = value;
    }

    public static DoubleElement[] sort(final SortMode sort, final Order order, final DoubleElement[] dels) {
        List<DoubleElement> list = DoubleElement.toList(dels);
        // TODO: evaluate for performance: can use Arrays.parallelSort() instead
        // (either on a copy or the array itself?), but the effect might be minimal. 
		Collections.sort(list, new DoubleElementComparator(sort, order.isAscending()));

        return list.toArray(new DoubleElement[list.size()]);
    }

    /**
     * @param delist A list with the DoubleElement indexes as elements
     *               Typically used after a sort.
     */
    public static int[] indexArray(List<DoubleElement> delist) {
        int[] indexes = new int[delist.size()];
        for (int i = 0; i < delist.size(); i++) {
            indexes[i] = delist.get(i).fIndex;
        }
        return indexes;
    }

    /**
     * @param delist A list with the DoubleElement values as elements
     * @return
     */
    public static double[] valueArray(List<DoubleElement> delist) {
        double[] values = new double[delist.size()];
        for (int i = 0; i < delist.size(); i++) {
            values[i] = delist.get(i).fValue;
        }
        return values;
    }

    public static List<DoubleElement> toList(DoubleElement[] dels) {
        List<DoubleElement> list = new ArrayList<DoubleElement>(dels.length);
        for (int i = 0; i < dels.length; i++) {
            list.add(dels[i]);
        }
        return list;
    }

    public static float[] toFloats(List<DoubleElement> dels) {
        float[] floats = new float[dels.size()];
        for (int i = 0; i < dels.size(); i++) {
            floats[i] = (float) dels.get(i).fValue;
        }
        return floats;
    }

    public static class DoubleElementComparator implements java.util.Comparator<DoubleElement> {
        private final boolean fIsAbsolute;
        private final Boolean fAscending;
        private final int firstObjReturn;
        private final int secondObjReturn;

        public DoubleElementComparator(SortMode sort, boolean ascending) {
            this.fIsAbsolute = sort.isAbsolute();
            this.fAscending = ascending;
            this.firstObjReturn = fAscending ? -1 : +1;
            this.secondObjReturn = fAscending ? +1 : -1;
        }

        public int compare(final DoubleElement element1, final DoubleElement element2) {
            if (element1 == null) {
                if (element2 == null) { return 0; }    // can't compare
                return firstObjReturn;    // null is always least
            }
            if (element2 == null) { return secondObjReturn; }    // null is always least
            
            // Note: this does NOT work the same as Double.compare(d1, d2).
            double value1 = element1.fValue;
            double value2 = element2.fValue;

            if (Double.isNaN(value1)) {
                if (Double.isNaN(value2)) { return 0; }
                return firstObjReturn;
            }
            if (Double.isNaN(value2)) { return secondObjReturn; }

            if (fIsAbsolute) {
                value1 = Math.abs(value1);
                value2 = Math.abs(value2);
            }

            if (value1 < value2) { return firstObjReturn; }
            if (value1 > value2) { return secondObjReturn; }
            return 0;
        }
    }

    // Based on DoubleElementComparator but used where it is safe to assume no NaNs (or nulls) are present.
    // Also includes a helper for optimizing Spearman, to check whether any ties were detected.
    public static class DoubleElementNaNlessComparator implements java.util.Comparator<DoubleElement> {
        private final Boolean fAscending;
        private final int firstObjReturn;
        private final int secondObjReturn;
        private boolean tiesDetected = false;

        public DoubleElementNaNlessComparator(boolean ascending) {
            this.fAscending = ascending;
            this.firstObjReturn = fAscending ? -1 : +1;
            this.secondObjReturn = fAscending ? +1 : -1;
        }
        
        public boolean isTiesDetected() { return tiesDetected; }

        public int compare(final DoubleElement element1, final DoubleElement element2) {
            // We skip any Null checks because we never use this with Null items
            final double value1 = element1.fValue;
            final double value2 = element2.fValue;
            if (value1 < value2) { return firstObjReturn; }
            if (value1 > value2) { return secondObjReturn; }

            // Equal values indicates a tie, though not when comparing to an identical reference.
            if (element1 != element2) { tiesDetected = true; }

            // Treat the index as a secondary comparison field. We mostly don't care about this but need
            // to break such ties to keep both values in a collection (e.g. TreeSet).
            final int index1 = element1.fIndex;
            final int index2 = element2.fIndex;
            if (index1 < index2) { return firstObjReturn; }
            if (index1 > index2) { return secondObjReturn; }

            // Otherwise they are equal.  This shouldn't happen with our originally intended use but 
            // we'll handle it anyway for completeness.
            return 0;
        }
    }
}
