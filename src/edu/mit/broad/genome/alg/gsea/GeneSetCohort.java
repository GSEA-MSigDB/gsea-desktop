/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg.gsea;

import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.RankedList;

/**
 * Defines a collection of gene sets and their associated scoring scheme (weights)
 */
public interface GeneSetCohort {

    public RankedList getRankedList();

    public GeneSetCohort clone(final GeneSet[] gsets, final boolean qualifySetsAgainstRankedList);

    // @note justin guinneys addition
    public int[] genesetIndicesForGene(final String geneName); // Indices of those gene SETS that contain the specified gene

    public int getNumGeneSets();

    public int getNumLabels();

    /**
     * SCORING SCHEME RELATED
     */
    public double getHitPoints(int gsetNum, String geneName);

    public double getMissPoints(int gsetNum, String geneName);

    public boolean isMember(int gsetNum, String geneName);

    public int getNumTrue(int gsetNum);

} // End class GeneSetCohort
