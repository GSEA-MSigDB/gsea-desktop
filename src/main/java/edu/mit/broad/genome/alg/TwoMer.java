/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg;

/**
 * @author Aravind Subramanian
 */
public class TwoMer {

    private String A;
    private String B;

    /**
     * ORDER is MEANINGLESS @see hashCode & equals below)
     * Class constructor
     *
     * @param a
     * @param b
     */
    public TwoMer(String a, String b) {

        if (a == null) {
            throw new IllegalArgumentException("Parameter A cannot be null");
        }

        if (b == null) {
            throw new IllegalArgumentException("Parameter B cannot be null");
        }

        this.A = a;
        this.B = b;
    }

    public boolean equals(Object obj) {
        if (obj instanceof TwoMer) {
            TwoMer tm = (TwoMer) obj;
            if (tm.A.equals(this.A) && tm.B.equals(this.B)) {
                return true;
            }
            if (tm.A.equals(this.B) && tm.B.equals(this.A)) {
                return true;
            }
        }

        return false;
    }

    public int hashCode() {
        return A.hashCode() + B.hashCode();
    }

} // End class TwoMer
