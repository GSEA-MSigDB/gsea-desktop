/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.math;

/**
 * enum construct
 */
public class Orientation {

    public static final Orientation ROW = new Orientation("By Row");
    public static final Orientation COL = new Orientation("By Col");

    private final String fType;

    /**
     * Privatized class constructor.
     */
    private Orientation(String type) {
        this.fType = type;
    }

    public String toString() {
        return fType;
    }

    public boolean isByRow() {

        return this.equals(ROW);

    }

    public boolean isByCol() {

        return this.equals(COL);

    }

    public boolean equals(Object obj) {

        if (obj instanceof Orientation) {
            if (((Orientation) obj).fType.equals(this.fType)) {
                return true;
            }
        }

        return false;
    }

    public int hashCode() {
        return fType.hashCode();
    }
}    // End Orientation
