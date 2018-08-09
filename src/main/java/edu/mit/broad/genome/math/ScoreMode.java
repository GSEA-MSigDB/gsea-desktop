/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.math;

/**
 * @author Aravind Subramanian
 */
public class ScoreMode {

    public static final ScoreMode POS_ONLY = new ScoreMode("pos_only");
    public static final ScoreMode NEG_ONLY = new ScoreMode("neg_only");
    public static final ScoreMode POS_AND_NEG_TOGETHER = new ScoreMode("pos_and_neg_together");
    public static final ScoreMode POS_AND_NEG_SEPERATELY = new ScoreMode("pos_and_neg_seperately");

    private String fMode;

    ScoreMode(String mode) {
        this.fMode = mode;
    }

    public String toString() {
        return fMode;
    }

    public boolean equals(Object obj) {

        if (obj instanceof ScoreMode) {
            if (((ScoreMode) obj).fMode.equals(this.fMode)) {
                return true;
            }
        }

        return false;
    }

    public int hashCode() {
        return fMode.hashCode();
    }

    public String getName() {
        return fMode;
    }

    public boolean isPostiveOnly() {

        return fMode.equals(POS_ONLY.fMode);

    }

    public boolean isPostiveAndNegTogether() {

        return fMode.equals(POS_AND_NEG_TOGETHER.fMode);

    }

    public boolean isPostiveAndNegSeperately() {

        return fMode.equals(POS_AND_NEG_SEPERATELY.fMode);

    }

    public boolean isNegativeOnly() {

        return fMode.equals(NEG_ONLY.fMode);

    }

} // End class ScoreMode

