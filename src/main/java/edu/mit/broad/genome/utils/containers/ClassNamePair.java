/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.utils.containers;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ClassNamePair {

    private final Class fClass;
    private final String fName;
    /**
     * Class Constructor.
     */
    public ClassNamePair(Class cl, String name) {
        this.fClass = cl;
        this.fName = name;
    }

    public String toString() {
        return fName;
    }
}    // End ClassNamePair
