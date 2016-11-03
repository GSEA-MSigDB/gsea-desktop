/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects.esmatrix.db;

import edu.mit.broad.genome.Errors;
import edu.mit.broad.genome.alg.Metric;
import edu.mit.broad.genome.math.LabelledVectorProcessor;
import edu.mit.broad.genome.math.Order;
import edu.mit.broad.genome.math.SortMode;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.vdb.chip.Chip;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Aravind Subramanian
 *         <p/>
 *         <p/>
 *         <p/>
 */
public class EnrichmentDbImpl_one_shared_rl extends AbstractEnrichmentDb {

    private EnrichmentResult[] fResults;

    private RankedList fRankedList_shared;

    // @note na for pre-ranked list based rnds
    private Template fTemplate_opt_shared;

    private Dataset fDataset_shared;

    private Chip fChip_shared;

    /**
     * Class constructor
     *
     * @param strucs
     * @param ds_shared_opt
     * @param t
     * @param gset
     * @param rl
     */
    public EnrichmentDbImpl_one_shared_rl(final String name,
                                          final RankedList ranked_list_shared,
                                          final Dataset ds_shared_opt,
                                          final Template template_shared_opt,
                                          final EnrichmentResult[] results,
                                          final LabelledVectorProcessor lvp,
                                          final Metric metric,
                                          final Map metricParams,
                                          final SortMode sort,
                                          final Order order,
                                          final int numPerm,
                                          final File edb_dir_opt
    ) {

        initHere(name, ranked_list_shared, ds_shared_opt, template_shared_opt, results, lvp, metric,
                metricParams, sort, order, numPerm, edb_dir_opt);

    }

    /**
     * Class constructor
     *
     * @param name
     * @param results
     * @param lvp
     * @param metric
     * @param metricParams
     * @param sort
     * @param order
     * @param numPerm
     */
    public EnrichmentDbImpl_one_shared_rl(final String name,
                                          final EnrichmentResult[] results,
                                          final Dataset ds_shared_opt,
                                          final LabelledVectorProcessor lvp,
                                          final Metric metric,
                                          final Map metricParams,
                                          final SortMode sort,
                                          final Order order,
                                          final int numPerm,
                                          final File edb_dir_opt
    ) {

        this(name, _rl_shared(results), ds_shared_opt, template_shared(results), results, lvp, metric,
                metricParams, sort, order, numPerm, edb_dir_opt);
    }

    private void initHere(final String name,
                          final RankedList ranked_list_shared,
                          final Dataset ds_shared_opt,
                          final Template template_shared_opt,
                          final EnrichmentResult[] results,
                          final LabelledVectorProcessor lvp,
                          final Metric metric,
                          final Map metricParams,
                          final SortMode sort,
                          final Order order,
                          final int numPerms,
                          final File edb_dir_opt
    ) {


        if (results == null) {
            throw new IllegalArgumentException("Param results cannot be null");
        }

        if (ranked_list_shared == null) {
            throw new IllegalArgumentException("Shared ranked list cannot be null");
        }

        this.fResults = results;
        this.fRankedList_shared = ranked_list_shared;
        this.fTemplate_opt_shared = template_shared_opt;
        this.fDataset_shared = ds_shared_opt;
        if (fDataset_shared != null) {
            this.fChip_shared = fDataset_shared.getAnnot().getChip();
        }

        // @note must call after results is set
        super.init(name, lvp, metric, metricParams, sort, order, numPerms, edb_dir_opt);
        // @note must call
        super.initResultMaps();
    }

    public EnrichmentDb cloneDeep(final EnrichmentResult[] results) {
        return new EnrichmentDbImpl_one_shared_rl(getName(), getRankedList(), getDataset(), getTemplate(), results,
                getRankedListProcessor(), getMetric(),
                getMetricParams(), getSortMode(), getOrder(), getNumPerm(), getEdbDir());
    }

    // -------------------------------------------------------------------------------------------- //
    // THESE because its a "shared" impl
    public Template getTemplate() {
        return fTemplate_opt_shared;
    }

    public Dataset getDataset() {
        return fDataset_shared;
    }

    public RankedList getRankedList() {
        return fRankedList_shared;
    }

    public EnrichmentResult[] getResults() {
        return fResults;
    }

    public List getResultsList() {
        return Arrays.asList(fResults);
    }

    private static RankedList _rl_shared(final EnrichmentResult[] results) {

        final Errors errors = new Errors();
        final String theName = results[0].getRankedList().getName();
        final int theSize = results[0].getRankedList().getSize();

        for (int r = 0; r < results.length; r++) {
            String name = results[r].getRankedList().getName();
            int size = results[r].getRankedList().getSize();
            if (name.equals(theName) == false) {
                errors.add("Mismatched rl theName: " + theName + " name: " + name + " at r: " + r + " # rls: " + results.length);
            }

            if (size != theSize) {
                errors.add("Mismatched rl theName: " + theName + " name: " + name + " at r: " + r + " # rls: " + results.length);
            }
        }

        errors.barfIfNotEmptyRuntime();

        return results[0].getRankedList();
    }

    private static Template template_shared(final EnrichmentResult[] results) {
        final Errors errors = new Errors();

        if (results[0].getTemplate() == null) {
            return null;
        }

        final String theName = results[0].getTemplate().getName();

        for (int r = 0; r < results.length; r++) {
            String name = results[r].getTemplate().getName();
            if (name.equals(theName) == false) {
                errors.add("Mismatched template theName: " + theName + " name: " + name + " at r: " + r + " # results: " + results.length);
            }
        }

        errors.barfIfNotEmptyRuntime();

        return results[0].getTemplate();
    }

} // End EnrichmentDbImpl
