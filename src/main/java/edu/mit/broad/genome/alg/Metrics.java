/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.alg;

import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.math.XMath;
import edu.mit.broad.genome.objects.Template;

import java.util.Map;

/**
 * Contains several implementation of Metrics
 * <p/>
 * inner classes must be declared static
 *
 * @author Aravind Subramanian
 * @author David Eby
 * @note from ma: don't forget that these distance funcs return greater distances
 * for longer vectors.  Need to have normalized equivalents that take
 * the number of components into account when comparing statistics
 * across i.e. different size feature vectors.
 */
public class Metrics {
    private static final int MIN_NUM_FOR_VAR = 3;

    // @maint add a metric and this array might need updating
    public static Metric[] METRICS_FOR_GSEA = new Metric[] { new Signal2Noise(), new tTest(), new Cosine(), 
            new Euclidean(), new Manhattan(), new Pearson(), new Spearman(), new ClassRatio(), new ClassDiff(), new ClassLog2Ratio()
    };
    public static Metric NONE_METRIC = new None();

    private Metrics() { }

    public static Metric lookupMetric(Object obj) {
        if (obj == null) {
            throw new NullPointerException("Cannot lookup for null object");
        }
        if (obj instanceof Metric) { return (Metric) obj; }

        // Correct for an old misspelling.
        String lookupName = obj.toString();
        if (Manhattan.TYPO_NAME.equalsIgnoreCase(lookupName)) { lookupName = "Manhattan"; }
        
        for (int i = 0; i < METRICS_FOR_GSEA.length; i++) {
            if (METRICS_FOR_GSEA[i].getName().equalsIgnoreCase(lookupName)) {
                return METRICS_FOR_GSEA[i];
            }
        }

        if (None.NAME.equalsIgnoreCase(lookupName)) { return NONE_METRIC; }
        
        throw new RuntimeException("Cannot lookup Metric for: " + lookupName + "; unsupported Metric.");
    }

    public static abstract class AbstractMetric implements Metric {
        protected VectorSplitter fSplitter;

        private final Type fType;
        private final String fName;
        private final int fMinNumSamplesNeededPerClassForCalculation;

        public AbstractMetric(final Type type, String name) {
            this.fType = type;
            this.fName = name;
            this.fMinNumSamplesNeededPerClassForCalculation = 1;
            this.fSplitter = new VectorSplitter(1); // @note default
        }

        public AbstractMetric(final Type type, String name, int minNumSamplesNeededPerClassForCalculation) {
            this.fType = type;
            this.fName = name;
            this.fMinNumSamplesNeededPerClassForCalculation = minNumSamplesNeededPerClassForCalculation;
            this.fSplitter = new VectorSplitter(1); // @note default
        }

        public boolean isCategorical() {
            return (fType.equals(CATEGORICAL) || fType.equals(CAT_AND_CONT));
        }

        public boolean isContinuous() {
            return (fType.equals(CONTINUOUS) || fType.equals(CAT_AND_CONT));
        }
        
        public String getName() { return this.fName; }
        public String toString() { return getName(); }

        public int hashCode() { return getName().hashCode(); }

        public boolean equals(Object obj) {
            if (obj instanceof Metric) {
                return getName().equalsIgnoreCase(((Metric) obj).getName());
            }

            return false;
        }

        public int getMinNumSamplesNeededPerClassForCalculation() { return this.fMinNumSamplesNeededPerClassForCalculation; }
    }

    /**
     *  The "None" Metric.  This is used only for exporting/importing the EDB file for Preranked analyses and should 
     *  not be used or made available for any other purpose.
     */
    public static class None extends AbstractMetric {
        static final String NAME = "None";

        public None() {
            super(CAT_AND_CONT, NAME);
        }

        /**
         * params:
         * USE_BIASED -> true or false (Boolean objects). Default is FALSE.
         * USE_MEDIAN -> true or false (Boolean objects). Default is TRUE.
         * FIX_LOW    -> true or false (Boolean objects). Default is TRUE
         * Template is required.
         */
        public double getScore(Vector profile, Template template, Map<String, Boolean> params) {
            return profile.getElement(0);
        }
    }

    public static class Euclidean extends AbstractMetric {
        public Euclidean() { super(CONTINUOUS, "Euclidean"); }

        /**
         * No parameters.
         * Template is required.
         * Remember, this is only sensible when the template is continuous.
         * Template needs to be equal in length to profile.
         * Always determining distance of profile to template.
         */
        public double getScore(Vector profile, Template template, Map<String, Boolean> params) {
            return XMath.euclidean(template.synchProfile(profile), template.toVector());
        }
    }

    public static class Manhattan extends AbstractMetric {
        static final String NAME = "Manhattan";
        static final String TYPO_NAME = "Manhatten";

        public Manhattan() { super(CONTINUOUS, NAME); }

        /**
         * No parameters.
         * Template is required.
         * Remember, this is only sensible when the template is continuous.
         * Template needs to be equal in length to profile.
         * Always determining distance of profile to template.
         */
        public double getScore(Vector profile, Template template, Map<String, Boolean> params) {
            return XMath.manhattan(template.synchProfile(profile), template.toVector());
        }
    }

    public static class Pearson extends AbstractMetric {
        public Pearson() { super(CONTINUOUS, "Pearson"); } // to make it consistent with the others

        /**
         * No parameters
         * Template is required.
         * Remember, this is only sensible when the template is continuous.
         * actualy not necc to to use with categorical vector too
         * Template needs to be equal in length to profile.
         * Always determining distance of profile to template.
         */
        public double getScore(Vector profile, Template template, Map<String, Boolean> params) {
            return XMath.pearson(template.synchProfile(profile), template.toVector());
        }
    }

    public static class Spearman extends AbstractMetric {
        public Spearman() { super(CONTINUOUS, "Spearman"); }

        /**
         * No parameters
         * Template is required.
         * Remember, this is only sensible when the template is continuous.
         * Template needs to be equal in length to profile.
         * Always determining distance of profile to template.
         */
        public double getScore(Vector profile, Template template, Map<String, Boolean> params) {
            return XMath.spearman(template.synchProfile(profile), template.toVector());
        }
    }

    public static class Cosine extends AbstractMetric {
        public Cosine() { super(CONTINUOUS, "Cosine"); }

        /**
         * No parameters
         * Template is required
         * Template needs to be equal in length to profile.
         * Always determining distance of profile to template.
         *
         * @IMP IMP is low good or bad? i think its different in that higher is better
         */
        public double getScore(Vector profile, Template template, Map<String, Boolean> params) {
            return XMath.cosine(template.synchProfile(profile), template.toVector());
        }
    }

    public static class Signal2Noise extends AbstractMetric {
        // Keeping this temporarily due to external name check in DatasetStatsCore.  Should move that
        // validation here instead.
        public static final String NAME = "Signal2Noise";

        public Signal2Noise() { super(CATEGORICAL, NAME, MIN_NUM_FOR_VAR); }
        
        /**
         * params:
         * USE_BIASED -> true or false (Boolean objects). Default is FALSE.
         * USE_MEDIAN -> true or false (Boolean objects). Default is TRUE.
         * FIX_LOW    -> true or false (Boolean objects). Default is TRUE
         * Template is required.
         */
        public double getScore(Vector profile, Template template, Map<String, Boolean> params) {
            boolean usebiased = AlgMap.isBiased(params);
            boolean fixlow = AlgMap.isFixLowVar(params);
            boolean usemedian = AlgMap.isMedian(params);
            Vector[] vs = fSplitter.splitBiphasic_nansafe(profile, template);

            if (vs == null) { return 0.0; }
            int coiIndex = template.getClassOfInterestIndex();
            if (coiIndex == 0) {
                return XMath.s2n(vs[0], vs[1], usebiased, usemedian, fixlow);
            } else {
                return XMath.s2n(vs[1], vs[0], usebiased, usemedian, fixlow);
            }
        }
    }

    public static class tTest extends AbstractMetric {
        // Keeping this temporarily due to external name check in DatasetStatsCore.  Should move that
        // validation here instead.
        public static final String NAME = "tTest";

        public tTest() { super(CATEGORICAL, NAME, MIN_NUM_FOR_VAR); }

        /**
         * USE_MEDIAN -> true or false (Boolean objects). Default is TRUE.
         * USE_BIASED -> true or false (Boolean objects). Default is FALSE.
         * FIX_LOW    -> true or false (Boolean objects). Default is TRUE
         * Template is required.
         */
        public double getScore(Vector profile, Template template, Map<String, Boolean> params) {
            boolean usemedian = AlgMap.isMedian(params);
            boolean usebiased = AlgMap.isBiased(params);
            boolean fixlow = AlgMap.isFixLowVar(params);
            Vector[] vs = fSplitter.splitBiphasic_nansafe(profile, template);

            if (vs == null) { return 0.0; }
            int coiIndex = template.getClassOfInterestIndex();
            if (coiIndex == 0) {
                return XMath.tTest(vs[0], vs[1], usebiased, usemedian, fixlow);
            } else {
                return XMath.tTest(vs[1], vs[0], usebiased, usemedian, fixlow);
            }
        }
    }

    public static class ClassRatio extends AbstractMetric {
        public ClassRatio() { super(CATEGORICAL, "Ratio_of_Classes"); }

        /**
         * No parameters.
         * Template is reqd.
         * Diff in means between classes as split by template
         * (NOT b/w profile and template)
         */
        public double getScore(Vector profile, Template template, Map<String, Boolean> params) {
            Vector[] vs = fSplitter.splitBiphasic_nansafe(profile, template);
            int coiIndex = template.getClassOfInterestIndex();

            final boolean useMean = AlgMap.isMean(params);
            if (vs == null) { return 0.0; }
            if (coiIndex == 0) {
                return XMath.meanOrMedianRatio(vs[0], vs[1], useMean);
            } else {
                return XMath.meanOrMedianRatio(vs[1], vs[0], useMean);
            }
        }
    }

    public static class ClassLog2Ratio extends AbstractMetric {
        public ClassLog2Ratio() { super(CATEGORICAL, "log2_Ratio_of_Classes"); }

        /**
         * No parameters.
         * Template is reqd.
         * Diff in means between classes as split by template
         * (NOT b/w profile and template)
         */
        public double getScore(Vector profile, Template template, Map<String, Boolean> params) {
            Vector[] vs = fSplitter.splitBiphasic_nansafe(profile, template);
            int coiIndex = template.getClassOfInterestIndex();

            final boolean useMean = AlgMap.isMean(params);
            if (vs == null) { return 0.0; }
            if (coiIndex == 0) {
                return XMath.log2(XMath.meanOrMedianRatio(vs[0], vs[1], useMean));
            } else {
                return XMath.log2(XMath.meanOrMedianRatio(vs[1], vs[0], useMean));
            }
        }
    }

    public static class ClassDiff extends AbstractMetric {
        public ClassDiff() { super(CATEGORICAL, "Diff_of_Classes"); }

        /**
         * No parameters.
         * Template is reqd.
         * Diff in means between classes as split by template
         * (NOT b/w profile and template)
         */
        public double getScore(Vector profile, Template template, Map<String, Boolean> params) {
            Vector[] vs = fSplitter.splitBiphasic_nansafe(profile, template);
            int coiIndex = template.getClassOfInterestIndex();

            final boolean useMean = AlgMap.isMean(params);
            if (vs == null) { return 0.0; }
            if (coiIndex == 0) {
                return XMath.meanOrMedianDiff(vs[0], vs[1], useMean);
            } else {
                return XMath.meanOrMedianDiff(vs[1], vs[0], useMean);
            }
        }
    }
}
