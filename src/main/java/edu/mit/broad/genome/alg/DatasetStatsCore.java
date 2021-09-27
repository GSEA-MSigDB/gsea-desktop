/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.alg;

import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.*;
import org.apache.log4j.Logger;
import xtools.api.param.BadParamException;

import java.util.*;

/**
 * Object that captures statistics for dataset rows
 *
 * @author Aravind Subramanian
 */
public class DatasetStatsCore {

    private Logger log = Logger.getLogger(DatasetStatsCore.class);

    public DatasetStatsCore() { }

    /**
     * Captures the scores associated with a Marker
     * Only for categorical 2 way distinctions
     */
    //TODO: Drop this class?
    // Static analysis says that none of these fields or methods are used.  However, review leads back to PermutationTest,
    // where earlier changes caused unexpected errors.  Need to look at those two changes together.
    public static class TwoClassMarkerStats {
        private double score;
        private double me0;
        private double stdev0;
        private double me1;
        private double stdev1;

        private double me_all;
        private double stdev_all;

        public double getScore() {
            return score;
        }

        public double getMe0() {
            return me0;
        }

        public double getMe_all() {
            return me_all;
        }

        public double getStdev0() {
            return stdev0;
        }

        public double getMe1() {
            return me1;
        }

        public double getStdev1() {
            return stdev1;
        }

        public double getStdev_all() {
            return stdev_all;
        }
    }

    public static void check2ClassCategoricalDS(final Dataset ds, final Template template, final Metric metric) {
        if (!metric.isCategorical()) {
            throw new IllegalArgumentException("Not a 2 class categorical metric: " + metric);
        }

        VectorSplitter splitter = new VectorSplitter(metric.getMinNumSamplesNeededPerClassForCalculation());

        Vector profile = ds.getRow(0);
        Vector[] vs = splitter.splitBiphasic_nansafe(profile, template);

        // Pause for a helpful error message for gsea GUI
        // need to watch out for throwing out baby with bathwater: sometimes, esp with cdna data there might not be enough or any values in the split vectors
        // DOC this rather than catching

        // TODO: eval whether we can just do these checks in the Metric code
        if (metric.getName().equalsIgnoreCase(Metrics.Signal2Noise.NAME) || metric.getName().equalsIgnoreCase(Metrics.tTest.NAME)) {
            if (ds.getNumCol() < 6) {
                throw new BadParamException("Too few samples in the dataset to use this metric", 1006);
            }

            if (vs == null) {
                throw new BadParamException("One of the classes in this dataset has too few samples in one of the classes of the dataset to use this metric", 1006);
            }

            // These checks are probably redundant since splitter.splitBiphasic_nansafe(profile, template) appears to return
            // null if either class has too few samples.
            if (vs[0].getSize() < 3) {
                throw new BadParamException("Too few samples in class A of the dataset to use this metric", 1006);
            }

            if (vs[1].getSize() < 3) {
                throw new BadParamException("Too few samples in class B of the dataset to use this metric", 1006);
            }
        }
    }
    
    // key -> feature name, value -> TwoClassMarkerStats
    // Currently only called from PermutationTestBuilder, which does not make use of the
    // return value.  Relies on this for error checks and nothing more.
    //TODO: refactor to separate error checks from the rest.
    public Map<String, TwoClassMarkerStats> calc2ClassCategoricalMetricMarkerScores(final Dataset ds,
    		final Template template, final Metric metric, final Map<String, Boolean> params) {

        if (!metric.isCategorical()) {
            throw new IllegalArgumentException("Not a 2 class categorical metric: " + metric);
        }

        Map<String, TwoClassMarkerStats> all = new HashMap<String, TwoClassMarkerStats>();

        boolean usebiased = AlgMap.isBiased(params);
        boolean fixlow = AlgMap.isFixLowVar(params);
        boolean useMedian = AlgMap.isMedian(params);

        VectorSplitter splitter = new VectorSplitter(metric.getMinNumSamplesNeededPerClassForCalculation());

        for (int r = 0; r < ds.getNumRow(); r++) { 

            String rowName = ds.getRowName(r);
            //System.out.println(">>" + rowName);

            Vector profile = ds.getRow(r);
            Vector[] vs = splitter.splitBiphasic_nansafe(profile, template);

            // Pause for a helpful error message for gsea GUI
            // need to watch out for throwing out baby with bathwater: sometimes, esp with cdna data there might not be enough or any values in the split vectors
            // DOC this rather than catching

            if (r == 0) {
                if (metric.getName().equalsIgnoreCase(Metrics.Signal2Noise.NAME) || metric.getName().equalsIgnoreCase(Metrics.tTest.NAME)) {
                    if (ds.getNumCol() < 6) {
                        throw new BadParamException("Too few samples in the dataset to use this metric", 1006);
                    }

                    if (vs == null) {
                        throw new BadParamException("One of the classes in this dataset has too few samples in one of the classes of the dataset to use this metric", 1006);
                    }

                    // These checks are probably redundant since splitter.splitBiphasic_nansafe(profile, template) appears to return
                    // null if either class has too few samples.
                    if (vs[0].getSize() < 3) {
                        throw new BadParamException("Too few samples in class A of the dataset to use this metric", 1006);
                    }

                    if (vs[1].getSize() < 3) {
                        throw new BadParamException("Too few samples in class B of the dataset to use this metric", 1006);
                    }
                }
            }

            TwoClassMarkerStats stats = new TwoClassMarkerStats();

            // sometimes, esp with cdna data there might not be enough or any values in the split vectors
            if (vs == null || vs[0] == null || vs[1] == null) {
                stats.score = Float.NaN;
                stats.me0 = Float.NaN;
                stats.me1 = Float.NaN;
                stats.me_all = Float.NaN;
                stats.stdev0 = Float.NaN;
                stats.stdev1 = Float.NaN;
                stats.stdev_all = Float.NaN;
                log.warn("Omitting as too few good data points: " + rowName);
                //throw new RuntimeException("Omitting as too few good data points: " + rowName + " " + template.getName() + "\n" + template.getAsString(false));
            } else {
                stats.score = (float) metric.getScore(profile, template, params);
                // @note not all of the cols in the ds might be used in the template
                // so this test is not correct
                /*
                final Vector full_row_as_is = template.extractProfile(r, ds);
                if (vs[0].getSize() + vs[1].getSize() != full_row.getSize()) {
                    throw new IllegalStateException();
                }
                */
                Vector full = new Vector(vs);

                if (useMedian) {
                    stats.me0 = vs[0].median();
                    stats.me1 = vs[1].median();
                    stats.me_all = full.median();
                } else {
                    stats.me0 = vs[0].mean();
                    stats.me1 = vs[1].mean();
                    stats.me_all = full.meanNaNsafe();
                }

                stats.stdev0 = (float) vs[0].stddev(usebiased, fixlow);
                stats.stdev1 = (float) vs[1].stddev(usebiased, fixlow);
                stats.stdev_all = (float) full.stddev(usebiased, fixlow);
            }

            if (all.containsKey(rowName)) {
                throw new RuntimeException("Duplicate row names in dataset: " + ds.getName() + " rowname: " + rowName + " row: " + r);
            }
            all.put(rowName, stats);

        }

        return Collections.unmodifiableMap(all);
    }
}
