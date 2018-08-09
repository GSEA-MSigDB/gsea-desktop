/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.math;

import edu.mit.broad.genome.utils.containers.ClassNamePair;

/**
 * enum construct
 */
public class SortMode {

    public static final SortMode REAL = new SortMode("real");
    public static final SortMode ABSOLUTE = new SortMode("abs");
    public static final SortMode[] ALL = new SortMode[]{REAL, ABSOLUTE};
    public static final ClassNamePair[] ALL_CNP = new ClassNamePair[]{
            new ClassNamePair(REAL.getClass(), REAL.fType),
            new ClassNamePair(ABSOLUTE.getClass(),
                    ABSOLUTE.fType)};
    private final String fType;

    /**
     * Privatized class constructor.
     */
    private SortMode(String type) {
        this.fType = type;
    }

    public String toString() {
        return fType;
    }

    public boolean equals(Object obj) {

        if (obj instanceof SortMode) {
            if (((SortMode) obj).fType.equals(this.fType)) {
                return true;
            }
        }

        return false;
    }

    public int hashCode() {
        return fType.hashCode();
    }

    public boolean isAbsolute() {

        return fType.equals(ABSOLUTE.fType);

    }

    /**
     * a lookup metod for sort mode
     */
    public static SortMode lookup(Object obj) {

        if (obj == null) {
            throw new NullPointerException("Null mode not allowed");
        }

        if (obj instanceof SortMode) {
            return (SortMode) obj;
        }

        String s = obj.toString();

        if (s.equalsIgnoreCase(REAL.fType)) {
            return REAL;
        } else if (s.equalsIgnoreCase(ABSOLUTE.toString())) {
            return ABSOLUTE;
        }

        throw new IllegalArgumentException("Unable to lookup direction String: " + obj);
    }
}    // End SortMode
