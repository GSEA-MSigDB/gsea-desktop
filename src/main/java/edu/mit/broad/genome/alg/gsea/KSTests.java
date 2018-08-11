/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg.gsea;

import edu.mit.broad.genome.MismatchedSizeException;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.alg.*;
import edu.mit.broad.genome.alg.markers.PermutationTestBuilder;
import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.objects.esmatrix.db.*;
import edu.mit.broad.genome.objects.strucs.DatasetTemplate;
import edu.mit.broad.genome.objects.strucs.TemplateRandomizerType;
import edu.mit.broad.vdb.chip.Chip;
import org.apache.log4j.Logger;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kolmogorov-Smirnov Enrichment Test related methods
 * <p/>
 * Many things here are specific to gsea;
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 * @see KSCore
 */
public class KSTests {

    private final Logger log = Logger.getLogger(this.getClass());

    private final KSCore core;

    private static final int LOG_FREQ = 5;

    private PrintStream sout;

    /**
     * Class Constructor.
     * Almost Stateless
     * os -> for quick stdout NOT for logging
     */
    public KSTests(final PrintStream os) {
        this.sout = os;
        this.core = new KSCore();
    }

    /**
     * Class constructor
     * Default output is System.out
     */
    public KSTests() {
        this(System.out);
    }

    public EnrichmentDb executeGsea(final DatasetTemplate dt,
                                    final GeneSet[] gsets,
                                    final int nperm,
                                    final Metric metric,
                                    final SortMode sort,
                                    final Order order,
                                    final LabelledVectorProcessor lvp,
                                    final RandomSeedGenerator rst,
                                    final TemplateRandomizerType rt,
                                    final Map mps,
                                    final GeneSetCohortGenerator gcohgen,
                                    final boolean permuteTemplate,
                                    final int numMarkers,
                                    final List store_rnd_ranked_lists_here_opt) throws Exception {

        return executeGsea(dt.getDataset(false), dt.getTemplate(), gsets,
                nperm, metric, sort, order, lvp, rst, rt, mps, gcohgen,
                permuteTemplate, numMarkers, store_rnd_ranked_lists_here_opt);
    }

    public EnrichmentDb executeGsea(final Dataset ds,
                                    final Template t,
                                    final GeneSet[] gsets,
                                    final int nperm,
                                    final Metric metric,
                                    final SortMode sort,
                                    final Order order,
                                    final LabelledVectorProcessor lvp,
                                    final RandomSeedGenerator rst,
                                    final TemplateRandomizerType rt,
                                    final Map mps,
                                    final GeneSetCohortGenerator gcohgen,
                                    final boolean permuteTemplate,
                                    final int numMarkers,
                                    final List store_rnd_ranked_lists_here_opt) {


        log.debug("!!!! Executing for: " + ds.getName() + " # samples: " + ds.getNumCol());

        EnrichmentDb edb;
        if (permuteTemplate) {
            edb = shuffleTemplate(nperm, metric, sort, order, mps, lvp,
                    ds, t, gsets, gcohgen, rt, rst, numMarkers, store_rnd_ranked_lists_here_opt);
        } else {
            edb = shuffleGeneSet(nperm, metric, sort, order, mps, lvp, ds, t, gsets, gcohgen, rst, true);
        }

        sout.println("Finished permutations ... creating reports");

        // Done algorithmics, now the reports
        return edb;
    }

    public EnrichmentDb executeGsea(final RankedList rl_real,
                                    final GeneSet[] gsets,
                                    final int nperm,
                                    final RandomSeedGenerator rst,
                                    final Chip chip,
                                    final GeneSetCohortGenerator gcohgen) throws Exception {

        log.debug("!!!! Executing for: " + rl_real.getName() + " # features: " + rl_real.getSize());

        EnrichmentResult[] results = shuffleGeneSet_precannedRankedList(nperm, rl_real, null, gsets, chip, gcohgen, rst, false, true, true);
        return new EnrichmentDbImpl_one_shared_rl(rl_real.getName(),
                rl_real, null, null, results, new LabelledVectorProcessors.None(),
                new Metrics.None(),
                new HashMap(),
                SortMode.REAL,
                Order.DESCENDING, nperm, null);
    }

    // ------------------------------------------------------------------------ //
    // --------------------------- TEMPLATE CALCULATIONS ----------------------//
    // ------------------------------------------------------------------------ //
    public EnrichmentDb shuffleTemplate(final int nperm,
                                        final Metric metric,
                                        final SortMode sort,
                                        final Order order,
                                        final Map metricParams,
                                        final LabelledVectorProcessor lvp,
                                        final Dataset ds,
                                        final Template template,
                                        final GeneSet[] gsets,
                                        final GeneSetCohortGenerator gcohgen,
                                        final TemplateRandomizerType rt,
                                        final RandomSeedGenerator rst,
                                        final int numMarkers,
                                        final List store_rnd_ranked_lists_here_opt) {

        final Template[] rndTemplates = TemplateFactoryRandomizer.createRandomTemplates(nperm, template, rt, rst);
        log.debug("Done generating rnd templates: " + rndTemplates.length);
        return shuffleTemplate_canned_templates(metric, sort, order, metricParams, lvp, ds,
                template, gsets, gcohgen, rndTemplates, null, null, numMarkers, store_rnd_ranked_lists_here_opt);
    }

    public EnrichmentDb shuffleTemplate_canned_templates(final Metric metric,
                                                         final SortMode sort,
                                                         final Order order,
                                                         final Map metricParams,
                                                         final LabelledVectorProcessor lvp,
                                                         final Dataset ds,
                                                         final Template template,
                                                         final GeneSet[] gsets,
                                                         final GeneSetCohortGenerator gcohgen,
                                                         final Template[] rndTemplates,
                                                         final RankedList realRankedList_pre_calculated_opt,
                                                         final RankedList[] rndRankedLists_pre_calculated_opt,
                                                         final int numMarkers,
                                                         final List store_rnd_ranked_lists_here_opt) {

        log.debug("shuffleTemplate with -- nperm: " + rndTemplates.length + " Order: " + order + " Sort: " + sort + " gsets: " + gsets.length);
        final String dstName = NamingConventions.generateName(ds, template, true);
        final Chip chip = ds.getAnnot().getChip();

        final DatasetMetrics dm = new DatasetMetrics();
        final RankedList rlReal;
        PermutationTestBuilder ptest = new PermutationTestBuilder(dstName, numMarkers, rndTemplates.length, lvp,
                metric, sort, order, metricParams, ds, template, null, template.isCategorical());

        if (realRankedList_pre_calculated_opt != null) {
            rlReal = realRankedList_pre_calculated_opt;
        } else {
            rlReal = dm.scoreDataset(metric, sort, order, metricParams, lvp, ds, template);
        }

        // calc real scores
        if (rlReal.getSize() != ds.getNumRow()) {// sanity check
            throw new MismatchedSizeException();
        }

        final GeneSetCohort gcoh = gcohgen.createGeneSetCohort(rlReal, gsets, false, true); // @note ASSUME already qualified
        final EnrichmentScore[] realScores = core.calculateKSScore(gcoh, true); // need to store details as we need the hit indices
        final Vector[] rndEss = new Vector[gsets.length];
        for (int g = 0; g < gsets.length; g++) {
            rndEss[g] = new Vector(rndTemplates.length);
        }

        // Each row is a "geneset", and each column a randomization
        for (int c = 0; c < rndTemplates.length; c++) {
            final RankedList rndRl;

            if (rndRankedLists_pre_calculated_opt != null) {
                rndRl = rndRankedLists_pre_calculated_opt[c];
            } else {
                rndRl = dm.scoreDataset(metric, sort, order, metricParams, lvp, ds, rndTemplates[c]);
            }

            if (store_rnd_ranked_lists_here_opt != null) {
                store_rnd_ranked_lists_here_opt.add(rndRl);
            }

            if (c % LOG_FREQ == 0) {
                StringBuffer ib = new StringBuffer("Iteration: ").append(c + 1).append('/').append(rndTemplates.length);
                ib.append(" for ").append(dstName);
                //sout.println(ib.toString());    // dont use log!
                System.out.println(ib.toString());
            }

            // DO THE RND CALC
            // @note better to just clone the existing real gcoh rather than generate a whole new one
            // as only the ranked list has changed and not the feature or gene set content
            final GeneSetCohort gcohRnd = gcohgen.createGeneSetCohort(rndRl, gsets, false, false);
            //System.out.println("starting calc: " + gcoh.getNumGeneSets());
            final EnrichmentScore[] rndScores = core.calculateKSScore(gcohRnd, false);
            //System.out.println("done calc");

            for (int g = 0; g < gsets.length; g++) {
                rndEss[g].setElement(c, rndScores[g].getES());
            }

            ptest.addRnd(rndTemplates[c], rndRl);

        } // End computation loop

        // 1 result for every gene set
        final EnrichmentResult[] results = new EnrichmentResult[gsets.length];
        for (int g = 0; g < gsets.length; g++) {
            results[g] = new EnrichmentResultImpl(rlReal, template, gsets[g], chip, realScores[g], rndEss[g]);
        }

        ptest.doCalc();

        return new EnrichmentDbImplWithPermTest(dstName, rlReal, ds, template,
                results, lvp, metric, metricParams, sort, order, rndTemplates.length, null, ptest);
    }

    // ------------------------------------------------------------------------ //
    // -------------------------------- GENE TAG CALCULATIONS ------------------//
    // ------------------------------------------------------------------------ //

    /**
     * @param nperm
     * @param rlReal
     * @param gsetsReal
     * @param gcohgen
     * @param rst
     * @return
     * @throws Exception
     */
    // this is the CORE method
    public EnrichmentResult[] shuffleGeneSet_precannedRankedList(final int nperm,
                                                                 final RankedList rlReal,
                                                                 final Template t_opt,
                                                                 final GeneSet[] gsetsReal,
                                                                 final Chip chip_opt,
                                                                 final GeneSetCohortGenerator gcohgen,
                                                                 final RandomSeedGenerator rst,
                                                                 final boolean qualifyGeneSets,
                                                                 final boolean storeDeepForRL,
                                                                 final boolean storeRESPointByPoint_for_real) {

        final EnrichmentResult[] results = new EnrichmentResult[gsetsReal.length];
        final GeneSetCohort gcohReal = gcohgen.createGeneSetCohort(rlReal, gsetsReal, qualifyGeneSets, true);

        final EnrichmentScore[] real_scores = core.calculateKSScore(gcohReal, storeDeepForRL, storeRESPointByPoint_for_real); // @note ususally always store deep for the real one

        // The make rnd gene sets for every real one
        for (int g = 0; g < gsetsReal.length; g++) {
            if (g % LOG_FREQ == 0) {
                sout.println("shuffleGeneSet for GeneSet " + (g + 1) + "/" + gsetsReal.length + " nperm: " + nperm);
            }

            // now create random GeneSets and calc the ksscore for every rnd GeneSet
            //log.debug("started gsets");
            Vector rndEss;
            if (nperm > 0) {
                final GeneSet[] rndgsets = GeneSetGenerators.createRandomGeneSetsFixedSize(nperm, rlReal, gsetsReal[g], rst);
                final GeneSetCohort gcohRnd = gcohReal.clone(rndgsets, false);
                rndEss = new Vector(rndgsets.length);
                final EnrichmentScore[] rnds = core.calculateKSScore(gcohRnd, false); // never store deep for rnds
                for (int r = 0; r < rndgsets.length; r++) {
                    rndEss.setElement(r, rnds[r].getES());
                }
            } else {
                rndEss = new Vector(0);
            }

            results[g] = new EnrichmentResultImpl(rlReal, t_opt,
                    gsetsReal[g], chip_opt, real_scores[g], rndEss);
        }

        return results;
    }

    /**
     * @param nperm
     * @param metric
     * @param sort
     * @param order
     * @param metricParams
     * @param ds
     * @param template
     * @param gsets
     * @param gen
     * @param rst
     * @return
     * @throws Exception
     */
    public EnrichmentDb shuffleGeneSet(final int nperm,
                                       final Metric metric,
                                       final SortMode sort,
                                       final Order order,
                                       final Map metricParams,
                                       final LabelledVectorProcessor lvp,
                                       final Dataset ds,
                                       final Template template,
                                       final GeneSet[] gsets,
                                       final GeneSetCohortGenerator gen,
                                       final RandomSeedGenerator rst,
                                       final boolean storeRESPointByPoint_for_real) {

        if (ds == null) {
            throw new IllegalArgumentException("Param ds cannot be null");
        }

        // The same (real template) scored dataset for all gsets
        final DatasetMetrics dm = new DatasetMetrics();
        final ScoredDataset rlReal = dm.scoreDataset(metric, sort, order, metricParams, lvp, ds, template);
        final Chip chip = ds.getAnnot().getChip();

        final EnrichmentResult[] results = shuffleGeneSet_precannedRankedList(nperm,
                rlReal, template, gsets, chip, gen, rst, false, true, storeRESPointByPoint_for_real); // @note store deep

        final String name = NamingConventions.generateName(ds, template, true);
        return new EnrichmentDbImpl_one_shared_rl(name, rlReal, ds, template,
                results, lvp, metric, metricParams, sort, order, nperm, null);
    }
}    // End KSTests

/*--- Formatted in Sun Java Convention Style on Mon, Nov 11, '02 ---*/