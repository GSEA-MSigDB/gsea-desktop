/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.alg.gsea;

import edu.mit.broad.genome.alg.DatasetGenerators;
import edu.mit.broad.genome.alg.fdr.FdrAlgs;
import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.LabelledVector;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResult;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentScore;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentScoreImpl;
import edu.mit.broad.genome.objects.strucs.FdrStruc;
import org.apache.log4j.Logger;

/**
 * @author Aravind Subramanian
 */
public class PValueCalculatorImpls {

    private static final Logger klog = Logger.getLogger(PValueCalculatorImpls.class);

    /**
     * The GSEA style implementation of pvalues and FDRs and FWERs
     */
    public static class GseaImpl implements PValueCalculator {

        private String fNormName;

        public GseaImpl(final String normName) {
            this.fNormName = normName;
        }

        // dont do with edb as there are different kinds of edbs and we loose the identity of the one used here
        public EnrichmentResult[] calcNPValuesAndFDR(final EnrichmentResult[] results) {
            return _calcGseaMethod(fNormName, results);
        }
    }

    private static EnrichmentResult[] _calcGseaMethod(final String normName,
                                                      final EnrichmentResult[] results) {

        final LabelledVector realESS = EdbAlgs.createRealES(results);
        final Dataset rndESS = EdbAlgs.createRndESDataset(results);

        klog.debug("Norm mode: " + normName);

        final Norms.Struc struc = Norms.normalize(normName, realESS, rndESS);

        return _calcGseaMethod(realESS, struc.normReal, rndESS, struc.normRnd, results);
    }


    // This is the core calc of the FDRs & FWER
    // we use all points (whether or not the gsets real es was pos or neg)
    private static EnrichmentResult[] _calcGseaMethod(final LabelledVector realESS,
                                                      final LabelledVector realNESS,
                                                      final Dataset rndESS_full,
                                                      final Dataset rndNESS_full,
                                                      final EnrichmentResult[] prev_results) {
        klog.debug("Started calcFdrs_skewed");

        // the rnd maybe a superset of the null needed for the real ess's
        // so qualify (dont need to do this for the rnd ess as its users are  already
        // name (and not index) aware
        // TODO: track down NaNs in the following calls (via rndNESS)
        // Question here is how to represent these bubbling up to later stages.  Could be enough to just
        // let everything calc to NaN or Infinity as it naturally would, and then just deal with those
        // possible values in the later stages.
        // Or, could store them as Null and detect that elsewhere.
        final Dataset rndNESS = new DatasetGenerators().extractRows(rndNESS_full, realESS.getLabels());
        final DatasetModed all_rnd_scores_norm_moded_pos = new DatasetModed(rndNESS, ScoreMode.POS_ONLY, SortMode.REAL, Order.DESCENDING);
        final DatasetModed all_rnd_scores_norm_moded_neg = new DatasetModed(rndNESS, ScoreMode.NEG_ONLY, SortMode.REAL, Order.ASCENDING);

        // @note this takes a few seconds to compute <++++ culprit for most time
        // TODO: track down NaNs in the following call (via realNESS & all_rnd_scores_moded_{pos|neg})
        final FdrAlgs.FdrMap fdrMap = FdrAlgs.calcFdrs_skewed(realESS,
                rndESS_full,
                realNESS,
                all_rnd_scores_norm_moded_pos,
                all_rnd_scores_norm_moded_neg,
                SortMode.REAL);


        klog.debug("done fdrMap");

        final EnrichmentResult[] results = new EnrichmentResult[prev_results.length];

        klog.debug("Started core calcFdrs in _calcGseaMethod for results: " + prev_results.length);
        for (int r = 0; r < prev_results.length; r++) {


            final String gsetName = prev_results[r].getGeneSetName();
            final FdrStruc fdrStruc = fdrMap.getFdr(gsetName);
            
            // If we store NES NaN / Infinity as Null, then some of these names will have no fdrStruc object 
            // in the Map.  Or detect them when building and keep them out of the Map.
            // Then we would skip certain things below...
            
            final float es = realESS.getScore(gsetName);
            final float nes = realNESS.getScore(gsetName);
            final Vector es_rnd_for_this_set = rndESS_full.getRow(gsetName); // @note fetching by name, not index
            // Skip this for null fdrStruc per PT's instructions
            final float np = XMath.getPValueTwoTailed_pos_neg_seperate(es, es_rnd_for_this_set); // NP

            // TODO: track down NaNs in the following call (via nes, rndNESS_full)
            // Or skip as per above
            final float fwer = XMath.getFWERTwoTailed(nes, rndNESS_full.getMatrix()); // FWER

            float fdr_value = fdrStruc.getFdr();

            if (Float.isNaN(fdr_value)) {
                //System.out.println("EANNANANA NAN: " + fdr_value + " " + gsetName);
            }

            // TODO: track down NaNs in the following call (via nes)
            // Or, skip this adjustment in those cases
            if (! XMath.isSameSign(nes, es)) { // @note
                fdr_value = 1.0f;
            }

            if (fdr_value > 1.0f) { // @note
                fdr_value = 1.0f;
            }

            // @note this accessses the rl

            // TODO: track down NaNs in the following call (via nes, fdr_value, fwer)
            // Or skip as per above.  Either represent this as es_new = null, or with null fields as appropriate
            // (prob better).  Then the report generation would detect those
            EnrichmentScore es_new = new EnrichmentScoreImpl(prev_results[r].getScore(), nes, np, fdr_value, fwer);

            // TODO: track down NaNs in the following call (via es_new)
            results[r] = new EnrichmentResult(prev_results[r].getRankedList(), prev_results[r].getTemplate(),
                    prev_results[r].getGeneSet(), prev_results[r].getChip(), es_new, prev_results[r].getRndESS(), fdrStruc);

            /*
            // @note retrievung the rl takes a long time (esp if its in the jit)
            // @todo make NamedRL a wrapper in jit
            results[r] = new EnrichmentResultImpl(null, prev_results[r].getTemplate(),
                   prev_results[r].getGeneSet(), prev_results[r].getChip(), es_new, prev_results[r].getRndESS(), fdrStruc);
            */

            if (r % 25 == 0) {
                klog.debug("Done loop: " + r + " / " + prev_results.length);
            }

        }

        klog.debug("Done core calcFdrs in _calcGseaMethod for results: " + prev_results.length);

        return results;
    }

} // End class PValueCalculatorImpls
