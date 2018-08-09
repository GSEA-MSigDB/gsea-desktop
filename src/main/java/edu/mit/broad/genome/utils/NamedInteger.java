/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.utils;

/**
 * @author Aravind Subramanian
 */
public class NamedInteger {

    private int fValue;
    private String fName;

    /**
     * Class constructor
     *
     * @param value
     * @param name
     */
    public NamedInteger(int value, String name) {
        this.fValue = value;
        this.fName = name;
    }

    public int getValue() {
        return fValue;
    }

    public String toString() {
        return fName;
    }

    /**
     * IMP to note that equality os determined solely on basis of
     * the int value
     *
     * @param obj
     * @return
     */
    public boolean equals(Object obj) {

        if (obj instanceof NamedInteger) {
            return equals(((NamedInteger) obj).fValue);
        } else if (obj instanceof Number) {
            return equals(((Number) obj).intValue());
        } else {
            return false;
        }

    }

    public boolean equals(int val) {
        return val == fValue;
    }

} // End class NamedInteger
