/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.math;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xtools.api.Tool;

import java.util.Random;

/**
 * @author Aravind Subramanian, David Eby
 */
public class RandomSeedGenerators {
    private static final Logger klog = LoggerFactory.getLogger(RandomSeedGenerators.class);

    public static RandomSeedGenerator lookup(String seedString, Tool tool) {
        if (StringUtils.isBlank(seedString)) { 
            throw new IllegalArgumentException("Random seed should not be blank; must be a long integer value or the string 'timestamp'");
        }
        if (StringUtils.equalsIgnoreCase(seedString, "timestamp")) {
            if (tool == null) { return new RandomSeedGenerators.Timestamp(); }
            return new RandomSeedGenerators.Timestamp(tool.getReport().getTimestamp());
        } else {
            try {
                long seed = Long.parseLong(seedString);
                return new RandomSeedGenerators.Custom(seed);
            } catch (NumberFormatException nfe) {
                // Fall-through to throw exception below...
            }
        }
        throw new IllegalArgumentException("Invalid random seed: must be a long integer value or the string 'timestamp'");
    }
    
    public static RandomSeedGenerator create(Object obj, Tool tool) {
        if (obj == null || tool == null) {
            throw new IllegalArgumentException("Cannot create generator for empty/blank/null tool or seed object");
        }
        
        Long seed;
        if (obj instanceof Long) {
            seed = (Long) obj;
        } else {
            String s = obj.toString();
            if (StringUtils.isBlank(s)) {
                throw new IllegalArgumentException("Random seed should not be blank; must be a long integer value or the string 'timestamp'");
            }
            
            if (s.equalsIgnoreCase(RandomSeedGenerator.TIMESTAMP)) {
                return new RandomSeedGenerators.Timestamp(tool.getReport().getTimestamp());
            }

            try {
                seed = Long.parseLong(s);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Invalid random seed: must be a long integer value or the string 'timestamp'");
            }
        }

        if (klog.isDebugEnabled()) { klog.debug("Creating a new seed with long: {}", obj.toString()); }
        return new RandomSeedGenerators.Custom(seed.longValue());
    }

    public static class Standard implements RandomSeedGenerator {
        private long fSeed;
        private Random fRandom;

        private Standard(long seed) {
            this.fSeed = seed;
            this.fRandom = new Random(fSeed);
        }

        public Standard() {
            this(STANDARD_SEED);
        }

        public String toString() {
            return "" + fSeed;
        }

        public Random getRandom() {
            return fRandom;
        }
    }

    public static class Custom extends Standard {
        public Custom(long seed) {
            super(seed);
        }
    }

    public static class Timestamp implements RandomSeedGenerator {
        private Random fRandom;
        private final long timestamp;

        public Timestamp() {
            // uses systems current time stamp to init
            this(System.currentTimeMillis());
        }

        public Timestamp(long timestamp) {
            this.timestamp = timestamp;
            this.fRandom = new Random(timestamp);
        }

        public String toString() {
            return TIMESTAMP;
        }

        public Random getRandom() {
            return fRandom;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
}
