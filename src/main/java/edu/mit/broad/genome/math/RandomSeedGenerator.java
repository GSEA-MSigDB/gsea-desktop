/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.math;

import java.util.Random;

/**
 * Enum construct
 */
public interface RandomSeedGenerator {

    public static final long STANDARD_SEED = 149L;
    public static final String TIMESTAMP = "timestamp";

    public Random getRandom();

} // End class RandomSeedGenerator
