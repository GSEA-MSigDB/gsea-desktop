/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
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
    public static void sort(final SortMode sort, final Order order, final List felist) {

        Collections.sort(felist, new DoubleElementComparator(sort, order.isAscending()));

    }

    public static DoubleElement[] sort(final SortMode sort, final Order order, final DoubleElement[] dels) {
        List list = DoubleElement.toList(dels);
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

    public static List toList(DoubleElement[] dels) {
        List list = new ArrayList(dels.length);
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

    static class DoubleElementComparator implements java.util.Comparator {

        private final SortMode fSort;
        private final Boolean fAscending;

        public DoubleElementComparator(SortMode sort, boolean ascending) {
            this.fSort = sort;
            this.fAscending = ascending;
        }

        public int compare(Object obj1, Object obj2) {

            if ((obj1 == null) && (obj2 == null)) {
                return 0;     // cant compare
            } else if (obj1 == null) {
                return fAscending ? -1 : +1;    // null is always least
            } else if (obj2 == null) {
                return fAscending ? +1 : -1;    // null is always least
            }

            double d1 = ((DoubleElement) obj1).fValue;
            double d2 = ((DoubleElement) obj2).fValue;


            if (Double.isNaN(d1) && Double.isNaN(d2)) {
                return 0;
            } else if (Double.isNaN(d1)) {
                return fAscending ? -1 : +1;
            } else if (Double.isNaN(d2)) {
                return fAscending ? +1 : -1;
            }

            if (fSort.isAbsolute()) {
                d1 = Math.abs(d1);
                d2 = Math.abs(d2);
            }

            if (d1 < d2) {
                return fAscending ? -1 : +1;
            } else if (d1 > d2) {
                return fAscending ? +1 : -1;
            } else
            return 0;
        }
    }
}
