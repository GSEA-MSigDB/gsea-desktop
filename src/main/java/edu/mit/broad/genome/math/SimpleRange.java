/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.math;

/**
 * @author Aravind Subramanian
 */
public class SimpleRange implements Range {

    private double fMin;

    private double fMax;

    public SimpleRange(double min, double max) {
        this.fMin = min;
        this.fMax = max;
    }

    public double getMin() {
        return fMin;
    }

    public double getMax() {
        return fMax;
    }

    public String getName() {
        return fMin + ":" + fMax;
    }

    public String toString() {
        return getName();
    }

    public int hashCode() {
        return toString().hashCode();
    }

} // End SimpleBin

