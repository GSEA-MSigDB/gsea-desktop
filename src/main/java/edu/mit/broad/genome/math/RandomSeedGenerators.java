/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.math;

import edu.mit.broad.genome.XLogger;
import org.apache.log4j.Logger;

import java.util.Random;

/**
 * @author Aravind Subramanian
 */
public class RandomSeedGenerators {

    private static final Logger klog = XLogger.getLogger(RandomSeedGenerators.class);


    // @maint
    //@note IMP we dont want the timestamp based param created at startup of the tool
    // but instead created each time getSeed is called
    public static RandomSeedGenerator lookup(Object obj) {

        if (obj == null) {
            throw new NullPointerException("Cannot lookup for null object");
        }

        //klog.debug("Doing lookup for obj: " + obj + " class: " + obj.getClass());

        if (obj instanceof RandomSeedGenerators.Timestamp) {
            klog.debug("Creating a new timestamp based rnd seed");
            return new RandomSeedGenerators.Timestamp(); // #note need to clone this as it was set at the start
        }

        if (obj instanceof RandomSeedGenerator) {
            return (RandomSeedGenerator) obj;
        }

        Long seed;

        if (obj instanceof Long) {
            seed = (Long) obj;
        } else {

            String s = obj.toString();
            if (s.equalsIgnoreCase(RandomSeedGenerator.TIMESTAMP)) {
                return new RandomSeedGenerators.Timestamp();
            }

            seed = new Long(Long.parseLong(s));

        }

        klog.debug("Creating a new seed with long: " + seed.longValue());
        return new RandomSeedGenerators.Custom(seed.longValue());
    }

    /**
     * Standard seed
     */
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

    /**
     * Custom specifiable long seed
     */
    public static class Custom extends Standard {

        public Custom(long seed) {
            super(seed);
        }

    }

    /**
     * Standard seed
     */
    public static class Timestamp implements RandomSeedGenerator {

        private Random fRandom;
        private final long timestamp;

        public Timestamp() {
            // uses systems current time stamp to init
            timestamp = System.currentTimeMillis();
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
