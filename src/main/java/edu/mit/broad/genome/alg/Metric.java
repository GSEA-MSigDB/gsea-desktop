/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.alg;

import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.Template;

import java.util.Map;

/**
 * Specification of a Metric
 *
 * @author Aravind Subramanian
 */
public interface Metric {

    public static final Type CATEGORICAL = new Type("categorical");

    public static final Type CONTINUOUS = new Type("continuous");

    public static final Type CAT_AND_CONT = new Type("cat_and-cont");

    public String getName();

    /**
     * Contract: small numbers should correspond to small distance between vectors
     * and hence close neighbors.
     * <p/>
     * keys of the params map correspond to one of the constants defined above.
     * Typical example of params are USE_MEDIAN and USE_BIASED
     */
    public double getScore(Vector profile, Template template, Map<String, Boolean> params);

    public boolean isCategorical();

    public boolean isContinuous();

    public int getMinNumSamplesNeededPerClassForCalculation();

    /**
     * Inner class struc
     */
    static class Type {

        private String name;

        Type(String name) {
            this.name = name;
        }

        public boolean equals(Object obj) {
            if (obj instanceof Type) {
                return (((Type) obj).name).equals(name);
            }

            return false;
        }

        public int hashCode() {
            return name.hashCode();
        }

    }

}    // End Metric
