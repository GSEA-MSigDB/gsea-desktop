/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

/**
 * @author Aravind Subramanian
 */
public interface MetricWeightStruc {

    public void setMetricName(String name);

    public String getMetricName();

    public int getTotalPosLength();

    public float getTotalPosLength_frac();

    public int getTotalNegLength();

    public float getTotalNegLength_frac();

    public float getTotalNegWeight_frac();

    public float getTotalPosWeight_frac();

} // End class MetricWeightStruc
