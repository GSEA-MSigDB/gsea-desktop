/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg.gsea;

import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.RankedList;

/**
 * @author Aravind Subramanian
 */
public interface GeneSetScoringTable {

    public String getName();

    public float getHitScore(final String name);

    public float getMissScore(final String name);

    public GeneSetScoringTable createTable(GeneSet gset, RankedList rl, RankedList realRankedList);

    public RankedList getRankedList();

}
