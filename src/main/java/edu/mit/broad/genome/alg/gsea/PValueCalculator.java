/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg.gsea;

import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResult;

/**
 * @author Aravind Subramanian
 */
public interface PValueCalculator {

    public EnrichmentResult[] calcNPValuesAndFDR(final EnrichmentResult[] results);

} // End class PValueSetter
