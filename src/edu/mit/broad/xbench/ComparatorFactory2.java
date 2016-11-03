/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench;

import xtools.api.ToolCategory;

import java.util.Comparator;

/**
 * A collection of commonly useful comparators.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ComparatorFactory2 {

    /**
     * Privatized class constructor.
     * Only static methods.
     */
    private ComparatorFactory2() {
    }


    public static class IntegerComparator implements Comparator {

        public int compare(Object pn1, Object pn2) {

            Integer s1 = new Integer(Integer.MIN_VALUE);

            try {
                if (pn1 != null) {
                    s1 = new Integer(pn1.toString());
                }
            } catch (Throwable t) {
            }

            Integer s2 = new Integer(Integer.MIN_VALUE);

            try {
                if (pn2 != null) {
                    s2 = new Integer(pn2.toString());
                }
            } catch (Throwable t) {
            }

            return s1.compareTo(s2);
        }

        public boolean equals(Object o2) {
            return false;
        }
    }    // End IntegerComparator

    public static class FloatComparator implements Comparator {

        public int compare(Object pn1, Object pn2) {

            Float s1 = new Float(Float.MIN_VALUE);

            try {
                if (pn1 != null) {
                    s1 = new Float(pn1.toString());
                }
            } catch (Throwable t) {
            }

            Float s2 = new Float(Float.MIN_VALUE);

            try {
                if (pn2 != null) {
                    s2 = new Float(pn2.toString());
                }
            } catch (Throwable t) {
            }

            return s1.compareTo(s2);
        }

        public boolean equals(Object o2) {
            return false;
        }
    }    // End FloatComparator


    /**
     * Class ToolCategoryComparator
     *
     * @author Aravind Subramanian
     * @version %I%, %G%
     */
    public static class ToolCategoryComparator implements Comparator {

        /**
         * Return -1 if o1 is less than o2, 0 if they're equal, +1 if o1 is greater than o2.
         */
        public int compare(Object pn1, Object pn2) {

            String s1 = pn1.toString();
            String s2 = pn2.toString();

            if (pn1 instanceof ToolCategory) {
                s1 = ((ToolCategory) pn1).getName();
            }

            if (pn2 instanceof ToolCategory) {
                s2 = ((ToolCategory) pn2).getName();
            }

            return s1.compareTo(s2);
        }

        /**
         * Return true if this equals o2.
         */
        public boolean equals(Object o2) {
            return false;
        }
    }    // End StringComparator

}    // End ComparatorFactory2
