/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg.gsea;

import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.RankedList;

/**
 * @author Aravind Subramanian
 */
public interface GeneSetCohortGenerator {

    public GeneSetCohort createGeneSetCohort(final RankedList rl,
                                             final GeneSet[] gsets,
                                             final boolean qualifySetsAgainstRankedList,
                                             final boolean realRankedList); // @todo consider realrl

} // End class GeneSetCohortGenerator
