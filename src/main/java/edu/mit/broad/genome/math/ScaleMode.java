/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.math;

/**
 * enum construct
 */
public class ScaleMode {

    public static final ScaleMode REL_MEAN = new ScaleMode("Relative & Mean");
    public static final ScaleMode REL_MEDIAN = new ScaleMode("Relative & Median");
    public static final ScaleMode ABSOLUTE = new ScaleMode("Absolute");
    public static final ScaleMode REL_MEAN_ZERO_OMITTED = new ScaleMode("Relative & Mean (zero omitted)");
    private final String fType;

    /**
     * Privatized class constructor.
     */
    private ScaleMode(final String type) {
        this.fType = type;
    }

    public String toString() {
        return fType;
    }

    public boolean isMean() {

        return this.equals(REL_MEAN) || this.equals(REL_MEAN_ZERO_OMITTED);
    }

    public boolean isRelative() {

        return (this.equals(REL_MEAN)) || (this.equals(REL_MEDIAN) || this.equals(REL_MEAN_ZERO_OMITTED));
    }

    public boolean equals(final Object obj) {

        if (obj instanceof ScaleMode) {
            if (((ScaleMode) obj).fType.equals(this.fType)) {
                return true;
            }
        }

        return false;
    }

    public int hashCode() {
        return fType.hashCode();
    }
}    // End ScaleMode
