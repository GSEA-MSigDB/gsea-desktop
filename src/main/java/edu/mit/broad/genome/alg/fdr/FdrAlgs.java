/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg.fdr;

import edu.mit.broad.genome.MismatchedSizeException;
import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.LabelledVector;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.objects.strucs.SkewCorrectedFdrStruc;
import gnu.trove.THashMap;

/**
 * False discovery rate algorithms.
 *
 * @author Aravind Subramanian
 */
public class FdrAlgs {

    public static class FdrMap extends THashMap {

        FdrMap() {
            super();
        }

        public SkewCorrectedFdrStruc getFdr(final String name) {
            Object obj = get(name);
            if (obj == null) {
                throw new IllegalArgumentException("No fdr data for: " + name);
            } else {
                return (SkewCorrectedFdrStruc) obj;
            }
        }

        private void add(final String name, final SkewCorrectedFdrStruc fdr) {
            if (containsKey(name)) {
                throw new IllegalArgumentException("Already have fdr entry for: " + fdr);
            } else {
                put(name, fdr);
            }
        }

    }

    // @note IMP IMP this returns fdr only for those that are pos (or neg) and NOT for all
    public static FdrMap calcFdrs_skewed(final LabelledVector real_scores,
                                         final Dataset rnd_scores_by_row_FULL,
                                         final LabelledVector real_scores_norm,
                                         final DatasetModed all_rnd_scores_norm_moded_pos,
                                         final DatasetModed all_rnd_scores_norm_moded_neg,
                                         final SortMode sort) {

        FdrMap map = new FdrMap();

        _calcFdrs_skewed(real_scores, rnd_scores_by_row_FULL, real_scores_norm,
                all_rnd_scores_norm_moded_pos, all_rnd_scores_norm_moded_neg, sort, Order.DESCENDING, true, map);

        _calcFdrs_skewed(real_scores, rnd_scores_by_row_FULL, real_scores_norm,
                all_rnd_scores_norm_moded_pos, all_rnd_scores_norm_moded_neg, sort, Order.ASCENDING, false, map);

        return map;
    }

    // @note IMP IMP this returns fdr only for those that are pos (or neg) and NOT for all
    private static void _calcFdrs_skewed(final LabelledVector real_scores,
                                         final Dataset rnd_scores_by_row_FULL, // can have excess rows
                                         final LabelledVector real_scores_norm,
                                         final DatasetModed all_rnd_scores_norm_moded_pos,
                                         final DatasetModed all_rnd_scores_norm_moded_neg,
                                         final SortMode sort,
                                         final Order order,
                                         final boolean doPos,
                                         final FdrMap map) {

        // sanity checks
        if (real_scores == null) {
            throw new IllegalArgumentException("Param real_scores cannot be null");
        }

        if (rnd_scores_by_row_FULL == null) {
            throw new IllegalArgumentException("Param rnd_scores_by_row_FULL cannot be null");
        }

        /* dont check -- it can have excess
        if (real_scores.getSize() != rnd_scores_by_row.getNumRow()) {
            throw new MismatchedSizeException("real_scores", real_scores.getSize(), "rnd_scores_matrix", rnd_scores_by_row.getNumRow());
        }
        */

        if (real_scores.getSize() != real_scores_norm.getSize()) {
            throw new MismatchedSizeException("real_scores", real_scores.getSize(), "real_scores_norm", real_scores_norm.getSize());
        }

        // NOTE: need to determine how the sort call treats NaN / Infinity.  We want a particular sort here,
        // where these are NOT considered the greatest.  Note idea of storing Null instead...
        final RankedList real_scores_norm_sorted = real_scores_norm.sort(sort, order);

        for (int r = 0; r < real_scores.getSize(); r++) {
            final String name = real_scores.getLabel(r);
            final float real_score_of_name = real_scores.getScore(r);

            // ... then, if storing Null we could just skip fdr.
            // Or, it might be possible to check for NaN / Infinity here and skip.
            if (doPos && XMath.isPositive(real_score_of_name)) {
                final SkewCorrectedFdrStruc fdr = new SkewCorrectedFdrStruc(name,
                        real_score_of_name,
                        real_scores_norm_sorted,
                        rnd_scores_by_row_FULL.getRow(name), // @note fetching by name, not index
                        all_rnd_scores_norm_moded_pos,
                        all_rnd_scores_norm_moded_neg,
                        doPos);
                map.add(name, fdr);
            } else if (!doPos && XMath.isNegative(real_score_of_name)) {
                final SkewCorrectedFdrStruc fdr = new SkewCorrectedFdrStruc(name,
                        real_score_of_name,
                        real_scores_norm_sorted,
                        rnd_scores_by_row_FULL.getRow(name), // // @note fetching by name, not index
                        all_rnd_scores_norm_moded_pos,
                        all_rnd_scores_norm_moded_neg,
                        doPos);
                map.add(name, fdr);
            }
        }
    }

} // End class FdrAlgs
