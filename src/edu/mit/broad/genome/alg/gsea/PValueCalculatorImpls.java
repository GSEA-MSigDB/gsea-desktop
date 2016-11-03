/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg.gsea;

import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.alg.DatasetGenerators;
import edu.mit.broad.genome.alg.fdr.FdrAlgs;
import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.LabelledVector;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResult;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResultImpl;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentScore;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentScoreImpl;
import edu.mit.broad.genome.objects.strucs.FdrStruc;
import org.apache.log4j.Logger;

/**
 * @author Aravind Subramanian
 */
public class PValueCalculatorImpls {

    private static final Logger klog = XLogger.getLogger(PValueCalculatorImpls.class);

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
        final Dataset rndESS = EdbAlgs.createRndESDataset("some_name", results);

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
        final Dataset rndNESS = new DatasetGenerators().extractRows(rndNESS_full, realESS.getLabels());
        final DatasetModed all_rnd_scores_norm_moded_pos = new DatasetModed(rndNESS, ScoreMode.POS_ONLY, SortMode.REAL, Order.DESCENDING);
        final DatasetModed all_rnd_scores_norm_moded_neg = new DatasetModed(rndNESS, ScoreMode.NEG_ONLY, SortMode.REAL, Order.ASCENDING);

        // @note this takes a few seconds to compute <++++ culprit for most time
        final FdrAlgs.FdrMap fdrMap = FdrAlgs.calcFdrs_skewed(realESS,
                rndESS_full,
                realNESS,
                all_rnd_scores_norm_moded_pos,
                all_rnd_scores_norm_moded_neg,
                SortMode.REAL);


        klog.debug("done fdrMap");

        //final Matrix rndNesMatrix = rndNESS.getMatrix();

        final EnrichmentResult[] results = new EnrichmentResult[prev_results.length];

        // @note Loop takes ~ 11 secs for 1500 gsets with 100 perms (123 secs for 1000 perms)!
        klog.debug("Started core calcFdrs in _calcGseaMethod for results: " + prev_results.length);
        for (int r = 0; r < prev_results.length; r++) {


            final String gsetName = prev_results[r].getGeneSetName();
            final FdrStruc fdrStruc = fdrMap.getFdr(gsetName);
            final float es = realESS.getScore(gsetName);
            final float nes = realNESS.getScore(gsetName);
            final Vector es_rnd_for_this_set = rndESS_full.getRow(gsetName); // @note fetching by name, not index
            final float np = XMath.getPValueTwoTailed_pos_neg_seperate(es, es_rnd_for_this_set); // NP

            final float fwer = XMath.getFWERTwoTailed(nes, rndNESS_full.getMatrix()); // FWER

            float fdr_value = fdrStruc.getFdr();

            if (Float.isNaN(fdr_value)) {
                //System.out.println("EANNANANA NAN: " + fdr_value + " " + gsetName);
            }

            if (XMath.isSameSign(nes, es) == false) { // @note
                fdr_value = 1.0f;
            }

            if (fdr_value > 1.0f) { // @note
                fdr_value = 1.0f;
            }

            // @note this accessses the rl

            EnrichmentScore es_new = new EnrichmentScoreImpl(prev_results[r].getScore(), nes, np, fdr_value, fwer);

            results[r] = new EnrichmentResultImpl(prev_results[r].getRankedList(), prev_results[r].getTemplate(),
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
