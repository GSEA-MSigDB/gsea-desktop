/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.map;

import edu.mit.broad.genome.objects.GeneSet;

/**
 * @author Aravind Subramanian
 */
public interface MGeneSet {

    public GeneSet getMappedGeneSet(final boolean simpleName);

    public MappingEtiology getEtiology();

} // End interface MGeneSet
