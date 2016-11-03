/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.map;

import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.GeneSetMatrix;

import java.util.Set;

/**
 * @author Aravind Subramanian
 */
public interface Mapper {

    // MAPPING RELATED -----------------------
    public MGeneSet map(final GeneSet sourceGeneSet, final boolean maintainEtiology) throws Exception;

    public MGeneSetMatrix map(final GeneSetMatrix sourceGm, final boolean maintainEtiology) throws Exception;

    public Set map(final String sourceProbeName) throws Exception;

}
