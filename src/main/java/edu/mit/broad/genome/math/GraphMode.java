/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.math;

/**
 * enum construct
 */
public class GraphMode {

    public static final GraphMode LINEAR = new GraphMode("Linear");
    public static final GraphMode LOG = new GraphMode("Log");

    private final String fType;

    /**
     * @maint manually keep array in sync with declarations above and below
     * <p/>
     * Privatized class constructor.
     * <p/>
     * <p/>
     * Privatized class constructor.
     */

    /**
     * Privatized class constructor.
     */
    private GraphMode(String type) {
        this.fType = type;
    }

    public String toString() {
        return fType;
    }

    public boolean isLinear() {

        if (this.fType.equalsIgnoreCase(LINEAR.fType)) {
            return true;
        }

        return false;
    }

    public boolean IsLog() {

        if (this.fType.equalsIgnoreCase(LOG.fType)) {
            return true;
        }

        return false;
    }

    public boolean equals(Object obj) {

        if (obj instanceof GraphMode) {
            if (((GraphMode) obj).fType.equals(this.fType)) {
                return true;
            }
        }

        return false;
    }

    public int hashCode() {
        return fType.hashCode();
    }
}    // End GraphMode
