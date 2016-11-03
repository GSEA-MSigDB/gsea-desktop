/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg.gsea;

import edu.mit.broad.genome.math.Vector;

/**
 * @author Aravind Subramanian
 */
public interface Norm {

    public Vector getRandomNorm();

    public float getRealNorm();

}
