/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.io.expr.cls;

/**
 * An interface for receiving notification of the content of a cls document
 *
 * @author Joshua Gould
 */
public interface IClsHandler {
    /**
     * @param x the array containing the class name for each sample
     */
    public void assignments(String[] x);

    /**
     * @param classes the array containing all the class names contained in the cls
     *                document
     */
    public void classes(String[] classes);
}