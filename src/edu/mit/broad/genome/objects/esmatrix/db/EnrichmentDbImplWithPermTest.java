/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects.esmatrix.db;

import edu.mit.broad.genome.alg.Metric;
import edu.mit.broad.genome.alg.markers.PermutationTest;
import edu.mit.broad.genome.math.LabelledVectorProcessor;
import edu.mit.broad.genome.math.Order;
import edu.mit.broad.genome.math.SortMode;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.objects.Template;

import java.io.File;
import java.util.Map;

/**
 * @author Aravind Subramanian
 */
public class EnrichmentDbImplWithPermTest extends EnrichmentDbImpl_one_shared_rl {

    private PermutationTest fPermTest;

    /**
     * Class constructor
     *
     * @param name
     * @param ranked_list_shared
     * @param ds_shared_opt
     * @param template_shared_opt
     * @param results
     * @param lvp
     * @param metric
     * @param metricParams
     * @param sort
     * @param order
     * @param numPerm
     * @param ptest_opt
     */
    public EnrichmentDbImplWithPermTest(final String name,
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
                                        final File edb_dir_opt,
                                        final PermutationTest ptest_opt) {

        super(name, ranked_list_shared, ds_shared_opt, template_shared_opt, results,
                lvp, metric, metricParams, sort, order, numPerm, edb_dir_opt);

        this.fPermTest = ptest_opt;
    }

    // Imp to override to keep the ptest around
    public EnrichmentDb cloneDeep(final EnrichmentResult[] results) {
        return new EnrichmentDbImplWithPermTest(getName(), getRankedList(), getDataset(), getTemplate(), results,
                getRankedListProcessor(), getMetric(),
                getMetricParams(), getSortMode(), getOrder(), getNumPerm(), getEdbDir(), getPermutationTest());
    }

    // na for tag rnd
    public PermutationTest getPermutationTest() {
        return fPermTest;
    }

} // End class EnrichmentDbImplWithPermTest
