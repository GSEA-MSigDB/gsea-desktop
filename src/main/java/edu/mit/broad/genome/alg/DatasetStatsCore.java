/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.alg;

import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xtools.api.param.BadParamException;

import java.util.*;

/**
 * Object that captures statistics for dataset rows
 *
 * @author Aravind Subramanian, David Eby
 */
public class DatasetStatsCore {
    private Logger log = LoggerFactory.getLogger(DatasetStatsCore.class);

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

        // This is the only field currently used.  Added recently for tracking rows with missing samples for later removal.
        // This can probably be accomplished a better way, possibly allowing removal of the entire class.
        public boolean omit = false;
        
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

    // key -> feature name, value -> TwoClassMarkerStats
    // Currently only called from PermutationTestBuilder, which does not make use of the
    // TwoClassMarkerStats map.  Relies on calling this for error checks and nothing more.
    //TODO: refactor to separate error checks from the rest.
    public boolean calc2ClassCategoricalMetricMarkerScores(final Dataset ds, final Template template, final Metric metric, 
            final Map<String, Boolean> params, Map<String, TwoClassMarkerStats> markerScores) {
        if (!metric.isCategorical()) {
            throw new IllegalArgumentException("Not a 2 class categorical metric: " + metric);
        }
        if (template.getNumClasses() != 2) {
            throw new RuntimeException("Template is not biphasic. Name: " + template.getName() + 
                    "\n<br>This metric can only be used with 2 class comparisons");
        }

        final boolean reqThreeSamplesPerClass = metric.getName().equalsIgnoreCase(Metrics.Signal2Noise.NAME) || metric.getName().equalsIgnoreCase(Metrics.tTest.NAME);
        if (reqThreeSamplesPerClass) {
            if (ds.getNumCol() < 6) { 
                throw new BadParamException("Too few samples in the dataset to use this metric", 1006);
            }
            if (template.getClass(0).getSize() < 3) {
                throw new BadParamException("Too few samples in class A of the dataset to use this metric", 1006);
            }
            if (template.getClass(1).getSize() < 3) {
                throw new BadParamException("Too few samples in class B of the dataset to use this metric", 1006);
            }
        }

        boolean usebiased = AlgMap.isBiased(params);
        boolean fixlow = AlgMap.isFixLowVar(params);
        boolean useMedian = AlgMap.isMedian(params);

        int minSampleCount = metric.getMinNumSamplesNeededPerClassForCalculation();
        VectorSplitter splitter = new VectorSplitter(minSampleCount);

        boolean foundRowsWithMissingData = false;
        for (int r = 0; r < ds.getNumRow(); r++) { 
            String rowName = ds.getRowName(r);

            Vector profile = ds.getRow(r);
            Vector[] vs = splitter.splitBiphasic(profile, template);
            vs[0] = vs[0].toVectorNaNless();
            vs[1] = vs[1].toVectorNaNless();

            // TODO: these stats seem to never be used.  Eval for removal
            // At the moment they are only used for tracking rows with too many missing values via the 'omit' field.
            // The remaining question, really, is whether they have a meaningful side-effect since they are never
            // directly used.  Otherwise it appears that the computations in this loop could be removed.
            // We will keep this in place for now until that can be checked.
            TwoClassMarkerStats stats = new TwoClassMarkerStats();

            if (vs == null || vs[0] == null || vs[1] == null || vs[0].getSize() == 0 || vs[1].getSize() == 0) {
                // sometimes, esp with cdna data there might not be enough or any values in the split vectors
                stats.score = Float.NaN;
                stats.me0 = Float.NaN;
                stats.me1 = Float.NaN;
                stats.me_all = Float.NaN;
                stats.stdev0 = Float.NaN;
                stats.stdev1 = Float.NaN;
                stats.stdev_all = Float.NaN;
                log.warn("Omitting row {} of this dataset with name '{}' as all the data is missing for one or both of the classes.", (r+1), rowName);
                stats.omit = true;
                foundRowsWithMissingData = true;
            } else {
                if (vs[0].getSize() < minSampleCount || vs[1].getSize() < minSampleCount) {
                    log.warn("In row {} of this dataset with name '{}', one or both of the classes has too few samples to use the chosen metric", (r+1), rowName);
                }

                stats.score = (float) metric.getScore(profile, template, params);
                Vector full = new Vector(vs);

                if (useMedian) {
                    stats.me0 = vs[0].median();
                    stats.me1 = vs[1].median();
                    stats.me_all = full.median();
                } else {
                    stats.me0 = vs[0].meanNaNsafe();
                    stats.me1 = vs[1].meanNaNsafe();
                    stats.me_all = full.meanNaNsafe();
                }

                stats.stdev0 = (float) vs[0].stddev(usebiased, fixlow);
                stats.stdev1 = (float) vs[1].stddev(usebiased, fixlow);
                stats.stdev_all = (float) full.stddev(usebiased, fixlow);
            }

            if (markerScores.containsKey(rowName)) {
                throw new RuntimeException("Duplicate row names in dataset: " + ds.getName() + " rowname: " + rowName + " row: " + (r+1));
            }
            markerScores.put(rowName, stats);
        }

        return foundRowsWithMissingData;
    }
}
