/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg;

import edu.mit.broad.genome.Headers;
import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class DatasetMetrics {

    private final Logger log = XLogger.getLogger(this.getClass());

    /**
     * Class Constructor.
     */
    public DatasetMetrics() {
    }


    public static Map getDefaultMetricParams() {
        Map params = new HashMap();
        params.put(Headers.USE_MEDIAN, XPreferencesFactory.kMedian.getBooleanO());
        params.put(Headers.FIX_LOW, XPreferencesFactory.kFixLowVar.getBooleanO());
        params.put(Headers.USE_BIASED, XPreferencesFactory.kBiasedVar.getBooleanO());
        return params;
    }

    /**
     * Score AND sort/order a Dataset according to specified parameters
     * <p/>
     * Also see the Unsorted version below for better perf in some
     * cases.
     *
     * @param metric
     * @param sort
     * @param order
     * @param ds
     * @param template
     * @return
     */
    public ScoredDataset scoreDataset(final Metric metric,
                                      final SortMode sort,
                                      final Order order,
                                      final Map metricParams,
                                      final LabelledVectorProcessor lvp,
                                      final Dataset ds,
                                      final Template template) {

        AddressedVector av = calcSortedMetric(metric, sort, order, metricParams, lvp, ds, template);

        return new ScoredDatasetImpl(av, ds);
    }

    public ScoredStruc scoreDatasetStruc(final Metric metric,
                                         final SortMode sort,
                                         final Order order,
                                         final Map metricParams,
                                         final LabelledVectorProcessor lvp,
                                         final Dataset ds,
                                         final Template template) {

        ScoredStruc ss = calcSortedMetricStruc(metric, sort, order, metricParams, lvp, ds, template);
        ss.sds = new ScoredDatasetImpl(ss.av, ds);
        return ss;
    }

    /**
     * Score AND sort/order a Dataset according to specified parameters
     * Results are sorted/ordered.
     * <p/>
     * Also see the Unsorted version below for better perf in some
     * cases.
     *
     * @param metric
     * @param sort
     * @param order
     * @param ds
     * @param template
     * @param metricParams
     * @return
     */
    public AddressedVector calcSortedMetric(final Metric metric,
                                            final SortMode sort,
                                            final Order order,
                                            final Map metricParams,
                                            final LabelledVectorProcessor lvp,
                                            final Dataset ds,
                                            final Template template) {

        return calcSortedMetricStruc(metric, sort, order, metricParams, lvp, ds, template).av;
    }

    public ScoredStruc calcSortedMetricStruc(final Metric metric,
                                             final SortMode sort,
                                             final Order order,
                                             final Map metricParams,
                                             final LabelledVectorProcessor lvp,
                                             final Dataset ds,
                                             final Template template) {

        if (ds == null) {
            throw new IllegalArgumentException("Param ds cannot be null");
        }

        // DONT check for Template -- it can be null for some metrics
        if (sort == null) {
            throw new IllegalArgumentException("Param sort cannot be null");
        }

        if (order == null) {
            throw new IllegalArgumentException("Param order cannot be null");
        }

        //log.info("Running: " + metric.getName() + " on: " + ds.getName());
        List dels = new ArrayList(ds.getNumRow());
        DoubleElement[] datasetSynchedDels = new DoubleElement[ds.getNumRow()];

        for (int i = 0; i < ds.getNumRow(); i++) {
            double dist = metric.getScore(ds.getRow(i), template, metricParams);
            DoubleElement del = new DoubleElement(i, dist);
            dels.add(del);
            datasetSynchedDels[i] = del;
        }

        DoubleElement.sort(sort, order, dels);

        lvp.process(dels); // @note

        return new ScoredStruc(datasetSynchedDels, new AddressedVector(dels));
    }

    /**
     * Inner class representing ???
     */
    public static class ScoredStruc {
        public AddressedVector av;
        public DoubleElement[] datasetSynchedDels; // synched with the dataset that was scored, but not score sorted

        public ScoredDataset sds;

        ScoredStruc(DoubleElement[] dels, AddressedVector av) {
            this.datasetSynchedDels = dels;
            this.av = av;
        }

    }

}    // End DatasetMetrics
