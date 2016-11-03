/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects.strucs;

import edu.mit.broad.genome.MismatchedSizeException;
import edu.mit.broad.genome.math.DatasetModed;
import edu.mit.broad.genome.math.ScoreMode;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.math.XMath;
import edu.mit.broad.genome.objects.RankedList;

/**
 * @author Aravind Subramanian
 */
public class SkewCorrectedFdrStruc implements FdrStruc {

    private String name;

    private float nominalP;

    private float realScore;
    private float realScoreNorm;
    private float fdr;

    private int totRealCnt;
    private int totRndCnt;

    private int moreRealThan;


    /**
     * @param name
     * @param real_score_of_name
     * @param real_score_norm_of_name
     * @param rnd_scores
     * @param all_rnd_scores_norm_sorted
     * @param moreRealThan
     * @param totRealCnt
     * @param order
     */
    public SkewCorrectedFdrStruc(final String name,
                                 final float real_score_of_name,
                                 final RankedList all_real_scores_norm_sorted,
                                 final Vector rnd_scores_of_name_only,
                                 final DatasetModed all_rnd_scores_norm_by_col_pos,
                                 final DatasetModed all_rnd_scores_norm_by_col_neg,
                                 final boolean doPos) {

        // checks
        if (rnd_scores_of_name_only == null) {
            throw new IllegalArgumentException("Param rnd_scores_of_name_only cannot be null");
        }

        if (all_rnd_scores_norm_by_col_pos == null) {
            throw new IllegalArgumentException("Param all_rnd_scores_norm_by_col_pos cannot be null");
        }

        if (all_rnd_scores_norm_by_col_neg == null) {
            throw new IllegalArgumentException("Param all_rnd_scores_norm_by_col_neg cannot be null");
        }

        if (all_rnd_scores_norm_by_col_pos.getNumCol() != all_rnd_scores_norm_by_col_neg.getNumCol()) {
            throw new MismatchedSizeException("num_cols pos", all_rnd_scores_norm_by_col_pos.getNumCol(), "num cols neg", all_rnd_scores_norm_by_col_neg.getNumCol());
        }

        if (rnd_scores_of_name_only.getSize() != all_rnd_scores_norm_by_col_pos.getNumCol()) {
            throw new MismatchedSizeException("calc_all", rnd_scores_of_name_only.getSize(), "num_col", all_rnd_scores_norm_by_col_pos.getNumCol());
        }

        if (rnd_scores_of_name_only.getSize() != all_rnd_scores_norm_by_col_neg.getNumCol()) {
            throw new MismatchedSizeException("calc_all", rnd_scores_of_name_only.getSize(), "num_col", all_rnd_scores_norm_by_col_neg.getNumCol());
        }

        boolean pos;
        final float real_score_norm_of_name = all_real_scores_norm_sorted.getScore(name);
        if (XMath.isPositive(real_score_of_name)) {
            pos = true;
        } else {
            pos = false;
        }

        // Ok, do the actual work
        float nominalP;
        if (pos) {
            nominalP = (float) XMath.getPValue(real_score_of_name, rnd_scores_of_name_only);
        } else {
            nominalP = (float) XMath.getPValueLessThan(real_score_of_name, rnd_scores_of_name_only);
        }

        final int moreRealThan = all_real_scores_norm_sorted.getRank(name) + 1;
        final int totalRealCnt = all_real_scores_norm_sorted.getSize();
        final float ncols = all_rnd_scores_norm_by_col_pos.getNumCol(); // same for pos and neg

        // go to every COLUMN of the rnd norm matrix and calc #(more than obs nes) / # (pos (or neg) scores)
        float col_mean_sum = 0;
        int totalRndCnt_used = 0;
        int moreRndThan = 0;
        int ncols_actual = 0;
        for (int c = 0; c < ncols; c++) {
            float moreOrLessThan;
            float size;
            if (pos) {
                Vector rndColNorm_sorted = all_rnd_scores_norm_by_col_pos.getColumn_sorted(c);
                size = rndColNorm_sorted.getSize();
                moreOrLessThan = XMath.getMoreThanCount(real_score_norm_of_name, rndColNorm_sorted);
            } else {
                Vector rndColNorm_sorted = all_rnd_scores_norm_by_col_neg.getColumn_sorted(c);
                size = rndColNorm_sorted.getSize();
                moreOrLessThan = XMath.getLessThanCount(real_score_norm_of_name, rndColNorm_sorted);
            }

            totalRndCnt_used += size;
            moreRndThan += moreOrLessThan;

            if (size == 0) { // skip, no pos or neg ones here
                //System.out.println("col_mean: " + col_mean + " moreOrLessThan: " + moreOrLessThan + " size: " + size + " pos: " + pos);
                //throw new IllegalArgumentException("col_mean" + col_mean);
            } else {
                float col_mean = moreOrLessThan / size;

                if (Float.isNaN(col_mean)) {
                    System.out.println("col_mean: " + col_mean + " moreOrLessThan: " + moreOrLessThan + " size: " + size + " pos: " + pos);
                    throw new IllegalArgumentException();
                }

                col_mean_sum += col_mean;
                ncols_actual++;
            }
        }

        final float numr = col_mean_sum / ncols_actual;
        final float numSets;
        if (pos) {
            numSets = all_real_scores_norm_sorted.getSize(ScoreMode.POS_ONLY);
        } else {
            numSets = all_real_scores_norm_sorted.getSize(ScoreMode.NEG_ONLY);
        }

        final float denr = ((float) moreRealThan) / numSets;
        float fdrcalc = numr / denr;

        if (doPos && pos) {
            //fdrcalc = fdrcalc;
        } else if (doPos && !pos) {
            fdrcalc = 1.0f;
        } else if (pos) {
            fdrcalc = 1.0f;
        } else {
            // do nothing
        }

        /*
        if (Float.isNaN(fdrcalc)) {
            System.out.println("For set: " + name + " numSets: " + numSets + " denr: " + denr + " numr: " + numr + " fdr: " + fdrcalc + " col_mean_sum: " + col_mean_sum + " ncols: " + ncols + " ncols_actual: " + ncols_actual);
            throw new RuntimeException();
        }
        */

        init(name,
                real_score_of_name,
                real_score_norm_of_name,
                nominalP,
                (int) numSets,
                moreRealThan,
                moreRndThan,
                totalRealCnt,
                totalRndCnt_used,
                all_rnd_scores_norm_by_col_pos.getDim_orig(),
                fdrcalc, numr, denr);
    }

    public int numSetsUsed;
    private float numr;
    private float denr;
    private int moreRndThan;
    public int totRndCnt_used;

    private void init(final String name,
                      final float real_score,
                      final float real_score_norm,
                      final float nominalP,
                      final int numSetsUsed,
                      final int moreRealThan,
                      final int moreRndThan,
                      final int totRealCnt,
                      final int totRndCnt_used,
                      final int totRndCnt,
                      final float fdr,
                      final float numr,
                      final float denr) {

        this.name = name;
        this.realScore = real_score;
        this.realScoreNorm = real_score_norm;
        this.moreRealThan = moreRealThan;
        this.nominalP = nominalP;
        this.totRealCnt = totRealCnt;
        this.totRndCnt = totRndCnt;
        this.fdr = fdr;

        this.moreRndThan = moreRndThan;
        this.totRndCnt_used = totRndCnt_used;


        this.numSetsUsed = numSetsUsed;
        this.denr = denr;
        this.numr = numr;
    }

    public float getRealScore() {
        return realScore;
    }

    public float getFdr() {
        return fdr;
    }

} // End class DefaultFdr
