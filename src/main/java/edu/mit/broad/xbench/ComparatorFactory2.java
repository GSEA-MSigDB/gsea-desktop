/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench;

import xtools.api.ToolCategory;

import java.util.Comparator;

/**
 * A collection of commonly useful comparators.
 *
 * @author Aravind Subramanian
 */
public class ComparatorFactory2 {

    /**
     * Privatized class constructor.
     * Only static methods.
     */
    private ComparatorFactory2() {
    }


    public static class IntegerComparator implements Comparator<Integer> {

        public int compare(Integer pn1, Integer pn2) {

            Integer s1 = pn1 != null ? pn1 : Integer.MIN_VALUE;
            Integer s2 = pn2 != null ? pn2 : Integer.MIN_VALUE;
            return s1.compareTo(s2);
        }

        public boolean equals(Object o2) {
            return false;
        }
    }

    public static class FloatComparator implements Comparator<Float> {

        public int compare(Float pn1, Float pn2) {
            Float s1 = pn1 != null ? pn1 : Float.MIN_VALUE;
            Float s2 = pn2 != null ? pn2 : Float.MIN_VALUE;
            return s1.compareTo(s2);
        }

        public boolean equals(Object o2) {
            return false;
        }
    }

    /**
     * Class ToolCategoryComparator
     *
     * @author Aravind Subramanian
     */
    public static class ToolCategoryComparator implements Comparator<ToolCategory> {

        public int compare(ToolCategory pn1, ToolCategory pn2) {
            String s1 = pn1.getName();
            String s2 = pn2.getName();
            return s1.compareTo(s2);
        }

        public boolean equals(Object o2) {
            return false;
        }
    }
}