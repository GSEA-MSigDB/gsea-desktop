/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg.distrib;

import edu.mit.broad.genome.math.*;

/**
 * @author Aravind Subramanian
 */
public class RangeFactory {

    public static Range[] createRanges(int nranges, double minInclusive, double maxInclusive) {

        Range[] ranges = new Range[nranges];
        double spread = (maxInclusive - minInclusive) / nranges;
        double runningmin = minInclusive;

        for (int i = 0; i < nranges; i++) {
            ranges[i] = new SimpleRange(runningmin, runningmin + spread);
            runningmin += spread;
        }

        return ranges;
    }

}
