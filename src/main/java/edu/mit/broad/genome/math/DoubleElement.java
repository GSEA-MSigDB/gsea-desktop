/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An object encapsulating a double element of an array.
 * Captures it (double) value and position in the array.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class DoubleElement {

    public int fIndex;
    public double fValue;

    /**
     * Class Constructor.
     */
    public DoubleElement(int index, double value) {
        this.fIndex = index;
        this.fValue = value;
    }

    /**
     * a list with DoubleElement as elements
     */
    public static void sort(final SortMode sort, final Order order, final List<DoubleElement> felist) {

        Collections.sort(felist, new DoubleElementComparator(sort, order.isAscending()));

    }

    public static DoubleElement[] sort(final SortMode sort, final Order order, final DoubleElement[] dels) {
        List<DoubleElement> list = DoubleElement.toList(dels);
        
        sort(sort, order, list);

        return (DoubleElement[]) list.toArray(new DoubleElement[list.size()]);
    }

    /**
     * @param delist A list with DoubleElement as elements
     *               Typically used after a sort.
     */
    public static int[] indexArray(List delist) {

        //log.debug("## " + delist);
        int[] indexes = new int[delist.size()];

        for (int i = 0; i < delist.size(); i++) {
            indexes[i] = ((DoubleElement) delist.get(i)).fIndex;
        }

        return indexes;
    }

    /**
     * @param delist A list with DoubleElement as elements
     * @return
     */
    public static double[] valueArray(List delist) {

        double[] values = new double[delist.size()];

        for (int i = 0; i < delist.size(); i++) {
            values[i] = ((DoubleElement) delist.get(i)).fValue;
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

    public static float[] toFloats(List dels) {
        float[] floats = new float[dels.size()];
        for (int i = 0; i < dels.size(); i++) {
            floats[i] = (float) ((DoubleElement) dels.get(i)).fValue;
        }

        return floats;
    }

    public static class DoubleElementComparator implements java.util.Comparator<DoubleElement> {

        private final boolean fIsAbsolute;
        private final Boolean fAscending;
        private final int ascendingFirstObjReturn;
        private final int ascendingSecondObjReturn;

        public DoubleElementComparator(SortMode sort, boolean ascending) {
            this.fIsAbsolute = sort.isAbsolute();
            this.fAscending = ascending;
            this.ascendingFirstObjReturn = fAscending ? -1 : +1;
            this.ascendingSecondObjReturn = fAscending ? +1 : -1;
        }

        public int compare(DoubleElement obj1, DoubleElement obj2) {
            
            if (obj1 == null) {
                if (obj2 == null) return 0;     // can't compare
                return ascendingFirstObjReturn;    // null is always least
            }
            if (obj2 == null) {
                return ascendingSecondObjReturn;    // null is always least
            }
            
            double d1 = obj1.fValue;
            double d2 = obj2.fValue;

            if (Double.isNaN(d1)) {
                if (Double.isNaN(d2)) return 0;
                return ascendingFirstObjReturn;
            }
            if (Double.isNaN(d2)) {
                return ascendingSecondObjReturn;
            }

            if (fIsAbsolute) {
                d1 = Math.abs(d1);
                d2 = Math.abs(d2);
            }

            // Note: this does NOT work the same as Double.compare(d1, d2).
            if (d1 < d2) {
                return ascendingFirstObjReturn;
            }
            if (d1 > d2) {
                return ascendingSecondObjReturn;
            }
            return 0;
        }
    }
}
