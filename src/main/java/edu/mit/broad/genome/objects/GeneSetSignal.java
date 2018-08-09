/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

/**
 * @author Aravind Subramanian
 */
public interface GeneSetSignal {

    // DONT ADD GET ORIGINAL GENESET

    public int getSize();

    public GeneSet getAsGeneSet();

    public int getRankAtMax();

    // % of gene tags before (for + es) or after (for -ve es)
    // the peak in the res// Thus the larger the % the more tags in the gene set
    // contribute to the final es
    public float getTagFraction();

    // in the gene list
    public float getListFraction();

    public float getSignalStrength();


} // End interface GeneSetSignal
