/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.map;

import edu.mit.broad.genome.objects.GeneSetMatrix;

/**
 * @author Aravind Subramanian
 */
public interface MGeneSetMatrix {

    public MGeneSet[] getMappedGeneSets();

    public MGeneSet getMappedGeneSet(final int m);

    public GeneSetMatrix getMappedGeneSetMatrix(final String prefix);

    public int getNumMappedSets();

    public MappingEtiology[] getEtiologies();

} // End interface MGeneSetMatrix
