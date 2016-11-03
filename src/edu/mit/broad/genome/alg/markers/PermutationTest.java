/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg.markers;

import edu.mit.broad.genome.alg.Metric;
import edu.mit.broad.genome.objects.*;

/**
 * @author Aravind Subramanian
 */
//TODO: revisit unused code.
// First attempt resulted in an error in GpUnit test run (invalid_too_few_samples_test)
public interface PermutationTest {

    // @note DONT put reporting gunk in here (use permutation report instead)

    // Meta data

    public String getName();

    public int getNumMarkers();

    public Metric getMetric();

    public float[] getSigLevels();

    // Data structire
    public RankedList getRankedList();

    public Template getTemplate();

    // Results
    public Dataset getSigLevels(final boolean up);

} // End interface PermutationTest
