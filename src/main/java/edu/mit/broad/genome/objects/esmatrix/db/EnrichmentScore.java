/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects.esmatrix.db;

import edu.mit.broad.genome.math.Vector;

/**
 * @author Aravind Subramanian
 */
public interface EnrichmentScore {

    public static final Type ES = new Type("es");
    public static final Type NES = new Type("nes");
    public static final Type NP = new Type("np");
    public static final Type FDR = new Type("fwer");

    public float getES();

    public int getRankAtES();

    public float getRankScoreAtES();

    public Vector getESProfile();

    public Vector getESProfile_point_by_point_opt();

    public int getNumHits();

    public int[] getHitIndices();

    public float getNES();

    public float getNP();

    public float getFWER();

    public float getFDR();

    /**
     * Inner class for a type
     */
    public static class Type {

        private String fName;

        Type(String name) {
            this.fName = name;
        }

        public String toString() {
            return fName;
        }

        public int hashCode() {
            return fName.hashCode();
        }

        public boolean equals(Object obj) {
            if (obj instanceof Type) {
                return ((Type) obj).fName.equals(fName);
            }

            return false;
        }
    } // End class Type

} // End class EnrichmentScore
