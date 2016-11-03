/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.models;

/**
 * @author Aravind Subramanian
 */
public class ASComparable implements Comparable {

    private String fName;

    public ASComparable(String name) {
        this.fName = name;
    }

    public int compareTo(Object o) {
        return 1;
    }

    public String toString() {
        return fName;
    }
}
