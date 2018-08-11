/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg;

import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.math.XMath;
import edu.mit.broad.genome.objects.Template;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * Contains several implementation of Metrics
 * <p/>
 * inner classes must be declared static
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 * @note from ma: don't forget that these distance funcs return greater distances
 * for longer vectors.  Need to have normalized equivalents that take
 * the number of components into account when comparing statistics
 * across i.e. different size feature vectors.
 */
//TODO: review use of Metrics outside of this class.
// I suspect they are not all in use.
public class Metrics {

    /**
     * For logging support
     */
    protected static final transient Logger klog = Logger.getLogger(Metrics.class);

    private static final int MIN_NUM_FOR_VAR = 3;

    // @maint add a metric and this array might need updating
    public static Metric[] createAllMetrics() {
        return new Metric[]{
                new Pearson(),
                new Cosine(),
                new Euclidean(),
                new Manhatten(),
                new FeatureVar(),
                new FeatureVarD(),
                new tTest(),
                new Signal2Noise(),
                new None(),
                new RegressionSlope(),
                new Bhattacharyya(),
                new PearsonD(),
                new ClassMeansDiff(), new ClassMediansDiff(),
                new ClassOfInterestMean(), new ClassOfInterestMedian(),
                new ClassMeansRatio(),
                new ClassMediansRatio(),
                new ClassMeansLog2Ratio(),
                new ClassMediansLog2Ratio(),
                new ClassRatio(),
                new ClassDiff(),
                new ClassLog2Ratio(),
                new FeatureVar(),
                new FeatureVarD()
        };
    }

    /**
     * Private class constructor to prevent construction outside.
     */
    private Metrics() {
    }

    public static Metric lookupMetric(Object obj) {

        if (obj == null) {
            throw new NullPointerException("Cannot lookup for null object");
        }

        if (obj instanceof Metric) {
            return (Metric) obj;
        }

        Metric[] all = createAllMetrics();

        for (int i = 0; i < all.length; i++) {
            if (all[i].getName().equalsIgnoreCase(obj.toString())) {
                return all[i];
            }
        }

        throw new RuntimeException("Cannot lookupMetric for: " + obj);
    }

    /**
     * Class constructor
     */
    public static abstract class AbstractMetric implements Metric {

        protected VectorSplitter fSplitter;

        private Type fType;

        public AbstractMetric(final Type type) {
            this.fType = type;
            this.fSplitter = new VectorSplitter(1); // @note default
        }

        public boolean isCategorical() {
            return (fType.equals(CATEGORICAL) || fType.equals(CAT_AND_CONT));
        }

        public boolean isContinuous() {
            return (fType.equals(CONTINUOUS) || fType.equals(CAT_AND_CONT));
        }

        public int hashCode() {
            return getName().hashCode();
        }

        public boolean equals(Object obj) {

            if (obj instanceof Metric) {
                return getName().equalsIgnoreCase(((Metric) obj).getName());
            }

            return false;
        }

        public String toString() {
            return getName();
        }

        public int getMinNumSamplesNeededPerClassForCalculation() {
            return 1;
        }
    }

    /**
     * Euclidean.
     */
    public static class Euclidean extends AbstractMetric {

        public static final String NAME = "Euclidean";

        /**
         * Class Constructor.
         */
        public Euclidean() {
            super(CONTINUOUS);
        }

        /**
         * No parameters.
         * Template is required.
         * Remember, this is only sensible when the template is continuous.
         * Template needs to be equal in length to profile.
         * Always determining distance of profile to template.
         */
        public double getScore(Vector profile, Template template, Map params) {
            return XMath.euclidean(template.synchProfile(profile), template.toVector());
        }

        public String getName() {
            return NAME;
        }

    }


    /**
     * Manhatten
     */
    public static class Manhatten extends AbstractMetric {

        public static final String NAME = "Manhatten";

        /**
         * Class Constructor.
         */
        public Manhatten() {
            super(CONTINUOUS);
        }

        /**
         * No parameters.
         * Template is required.
         * Remember, this is only sensible when the template is continuous.
         * Template needs to be equal in length to profile.
         * Always determining distance of profile to template.
         */
        public double getScore(Vector profile, Template template, Map params) {
            return XMath.manhatten(template.synchProfile(profile), template.toVector());
        }

        public String getName() {
            return NAME;
        }

    }

    /**
     * Pearson.
     */
    public static class Pearson extends AbstractMetric {

        public static final String NAME = "Pearson";

        /**
         * Class Constructor.
         */
        public Pearson() {
            super(CONTINUOUS); // to make it consistent with the others
        }

        /**
         * No parameters
         * Template is required.
         * Remember, this is only sensible when the template is continuous.
         * actualy not necc to to use with categorical vector too
         * Template needs to be equal in length to profile.
         * Always determining distance of profile to template.
         */
        public double getScore(Vector profile, Template template, Map params) {
            final Vector v = template.synchProfile(profile);
            return XMath.pearson(v, template.toVector());
        }

        public String getName() {
            return NAME;
        }

    }

    /**
     * Cosine dist
     */
    public static class Cosine extends AbstractMetric {

        public static final String NAME = "Cosine";

        public Cosine() {
            super(CONTINUOUS);
        }

        /**
         * No parameters
         * Template is required
         * Template needs to be equal in length to profile.
         * Always determining distance of profile to template.
         *
         * @IMP IMP is low good or bad? i think its different in that higher is better
         */
        public double getScore(Vector profile, Template template, Map params) {
            return XMath.cosine(template.synchProfile(profile), template.toVector());
        }

        public String getName() {
            return NAME;
        }

    }    // End Cosine


    /**
     * FeatureVar
     */
    public static class FeatureVar extends AbstractMetric {

        public static final String NAME = "FeatureVariation";

        public FeatureVar() {
            super(CONTINUOUS);
        }

        /**
         * params
         * USE_BIASED -> true or false (Boolean objects). Default is false.
         * FIX_LOW    -> true or false (Boolean objects). Default is true
         * Template is NOT required.
         * Not a *distance* metric -> just measures variation within the profile.
         */
        public double getScore(Vector profile, Template template, Map params) {

            boolean usebiased = AlgMap.isBiased(params);
            boolean fixlow = AlgMap.isFixLowVar(params);

            return profile.var(usebiased, fixlow);
        }

        public String getName() {
            return NAME;
        }

        public int getMinNumSamplesNeededPerClassForCalculation() {
            return MIN_NUM_FOR_VAR;
        }

    }

    /**
     * Dchip style var
     */
    public static class FeatureVarD extends AbstractMetric {

        public static final String NAME = "Feature_stddev_by_mean";

        public FeatureVarD() {
            super(CONTINUOUS);
        }

        /**
         * params
         * USE_BIASED -> true or false (Boolean objects). Default is false.
         * FIX_LOW    -> true or false (Boolean objects). Default is true
         * Template is NOT required.
         * Not a *distance* metric -> just measures variation within the profile.
         */
        public double getScore(Vector profile, Template template, Map params) {

            boolean usebiased = AlgMap.isBiased(params);
            boolean fixlow = AlgMap.isFixLowVar(params);

            return profile.vard(usebiased, fixlow);
        }

        public String getName() {
            return NAME;
        }

        public int getMinNumSamplesNeededPerClassForCalculation() {
            return MIN_NUM_FOR_VAR;
        }

    }

    /**
     * Signal2Noise
     */
    public static class Signal2Noise extends AbstractMetric {

        public static final String NAME = "Signal2Noise";

        public Signal2Noise() {
            super(CATEGORICAL);
        }

        /**
         * params:
         * USE_BIASED -> true or false (Boolean objects). Default is FALSE.
         * USE_MEDIAN -> true or false (Boolean objects). Default is TRUE.
         * FIX_LOW    -> true or false (Boolean objects). Default is TRUE
         * Template is required.
         */
        public double getScore(Vector profile, Template template, Map params) {

            boolean usebiased = AlgMap.isBiased(params);
            boolean fixlow = AlgMap.isFixLowVar(params);
            boolean usemedian = AlgMap.isMedian(params);

            Vector[] vs = fSplitter.splitBiphasic_nansafe(profile, template);

            if (vs == null) {
                return 0;
            } else {

                int coiIndex = template.getClassOfInterestIndex();

                if (coiIndex == 0) {
                    return XMath.s2n(vs[0], vs[1], usebiased, usemedian, fixlow);
                } else {
                    return XMath.s2n(vs[1], vs[0], usebiased, usemedian, fixlow);
                }
            }

        }

        public String getName() {
            return NAME;
        }

        public int getMinNumSamplesNeededPerClassForCalculation() {
            return MIN_NUM_FOR_VAR;
        }

    }


    public static class None extends AbstractMetric {

        public static final String NAME = "None";

        public None() {
            super(CAT_AND_CONT);
        }

        /**
         * params:
         * USE_BIASED -> true or false (Boolean objects). Default is FALSE.
         * USE_MEDIAN -> true or false (Boolean objects). Default is TRUE.
         * FIX_LOW    -> true or false (Boolean objects). Default is TRUE
         * Template is required.
         */
        public double getScore(Vector profile, Template template, Map params) {
            return profile.getElement(0);
        }

        public String getName() {
            return NAME;
        }

    }

    /**
     * t-Test
     */
    public static class tTest extends AbstractMetric {

        public static final String NAME = "tTest";

        public tTest() {
            super(CATEGORICAL);
        }

        /**
         * USE_MEDIAN -> true or false (Boolean objects). Default is TRUE.
         * USE_BIASED -> true or false (Boolean objects). Default is FALSE.
         * FIX_LOW    -> true or false (Boolean objects). Default is TRUE
         * Template is required.
         */
        public double getScore(Vector profile, Template template, Map params) {

            boolean usemedian = AlgMap.isMedian(params);
            boolean usebiased = AlgMap.isBiased(params);
            boolean fixlow = AlgMap.isFixLowVar(params);
            Vector[] vs = fSplitter.splitBiphasic_nansafe(profile, template);
            int coiIndex = template.getClassOfInterestIndex();

            if (coiIndex == 0) {
                return XMath.tTest(vs[0], vs[1], usebiased, usemedian, fixlow);
            } else {
                return XMath.tTest(vs[1], vs[0], usebiased, usemedian, fixlow);
            }
        }

        public String getName() {
            return NAME;
        }

        public int getMinNumSamplesNeededPerClassForCalculation() {
            return MIN_NUM_FOR_VAR;
        }

    }

    /**
     * Bhattacharyya
     */
    public static class Bhattacharyya extends AbstractMetric {

        public static final String NAME = "Bhattacharyya";

        public Bhattacharyya() {
            super(CATEGORICAL);
        }

        /**
         * USE_BIASED -> true or false (Boolean objects). Default is FALSE.
         * FIX_LOW    -> true or false (Boolean objects). Default is TRUE
         * Template is required.
         */
        public double getScore(Vector profile, Template template, Map params) {

            boolean usebiased = AlgMap.isBiased(params);
            boolean fixlow = AlgMap.isFixLowVar(params);
            Vector[] vs = fSplitter.splitBiphasic_nansafe(profile, template);
            int coiIndex = template.getClassOfInterestIndex();

            if (coiIndex == 0) {
                return XMath.bhat(vs[0], vs[1], usebiased, fixlow);
            } else {
                return XMath.bhat(vs[1], vs[0], usebiased, fixlow);
            }
        }

        public String getName() {
            return NAME;
        }

    }    // End Bhattacharyya


    /**
     * @todo RegressionSlope
     */
    public static class RegressionSlope extends AbstractMetric {

        public static final String NAME = "RegressionSlope";

        public RegressionSlope() {
            super(CONTINUOUS);
        }

        /**
         * Template is required.
         */
        public double getScore(Vector profile, Template template, Map params) {
            boolean usebiased = AlgMap.isBiased(params);
            boolean fixlow = AlgMap.isFixLowVar(params);

            Vector[] splits = template.splitByTemplateClass(profile);

            return XMath.regressionSlope(profile, template.toVector(), splits, usebiased, fixlow);
        }

        public String getName() {
            return NAME;
        }

    }

    /**
     * Pearson.
     */
    public static class PearsonD extends Pearson {

        public static final String NAME = "Norm.Pearson";

        /**
         * None
         * <p/>
         * Template is required.
         *
         * @todo review with pt
         * Why does it make sense to use Pearson in this fashion - i.e normalized and as a discrete metric?
         * Why does it not make sense to use other cont metrics this way?
         * Pearson gives values from -1 to +1 which is the same range that pnorm on a vector does.
         * <p/>
         * So, needs 2 vectors both in the -1 to +1 range.
         * The ref vector (from template) needs to be biphasic and values at -1 or +1
         * The comp vector needs to be pnormalized.
         * Actually none of those tests are done here.
         * Here its a straigtforward pc calc - ditto to Pearson continuous.
         * <p/>
         * <p/>
         * usebiased and usemedian are meaningless??
         */
        public double getScore(Vector profile, Template template, Map params) {


            Vector[] vs = fSplitter.splitBiphasic_nansafe(profile, template);
            int coiIndex = template.getClassOfInterestIndex();

            if (coiIndex == 0) {
                return XMath.pearsonD(vs[0], vs[1]);
            } else {
                return XMath.pearsonD(vs[1], vs[0]);
            }

        }

        public String getName() {
            return NAME;
        }

    }

    /**
     * Diff of means.
     */
    public static class ClassMeansDiff extends AbstractMetric {

        public static final String NAME = "Diff_of_Means";

        /**
         * Class Constructor.
         * Template is required.
         */
        public ClassMeansDiff() {
            super(CATEGORICAL);
        }

        /**
         * No parameters.
         * Template is reqd.
         * Diff in means between classes as split by template
         * (NOT b/w profile and template)
         */
        public double getScore(Vector profile, Template template, Map params) {

            Vector[] vs = fSplitter.splitBiphasic_nansafe(profile, template);
            int coiIndex = template.getClassOfInterestIndex();

            if (coiIndex == 0) {
                return XMath.meansdiff(vs[0], vs[1]);
            } else {
                return XMath.meansdiff(vs[1], vs[0]);
            }
        }

        public String getName() {
            return NAME;
        }

    }

    public static class ClassMeansRatio extends AbstractMetric {

        public static final String NAME = "Ratio_of_Means";

        /**
         * Class Constructor.
         * Template is required.
         */
        public ClassMeansRatio() {
            super(CATEGORICAL);
        }

        /**
         * No parameters.
         * Template is reqd.
         * Diff in means between classes as split by template
         * (NOT b/w profile and template)
         */
        public double getScore(Vector profile, Template template, Map params) {

            Vector[] vs = fSplitter.splitBiphasic(profile, template);
            int coiIndex = template.getClassOfInterestIndex();

            if (coiIndex == 0) {
                return XMath.meansratio(vs[0], vs[1]);
            } else {
                return XMath.meansratio(vs[1], vs[0]);
            }
        }

        public String getName() {
            return NAME;
        }

    }

    public static class ClassRatio extends AbstractMetric {

        public static final String NAME = "Ratio_of_Classes";

        /**
         * Class Constructor.
         * Template is required.
         */
        public ClassRatio() {
            super(CATEGORICAL);
        }

        /**
         * No parameters.
         * Template is reqd.
         * Diff in means between classes as split by template
         * (NOT b/w profile and template)
         */
        public double getScore(Vector profile, Template template, Map params) {

            Vector[] vs = fSplitter.splitBiphasic(profile, template);
            int coiIndex = template.getClassOfInterestIndex();

            final boolean useMean = AlgMap.isMean(params);

            if (coiIndex == 0) {
                return XMath.meanOrMedianRatio(vs[0], vs[1], useMean);
            } else {
                return XMath.meanOrMedianRatio(vs[1], vs[0], useMean);
            }
        }

        public String getName() {
            return NAME;
        }

    } // End class ClassRatio


    public static class ClassLog2Ratio extends AbstractMetric {

        public static final String NAME = "log2_Ratio_of_Classes";

        /**
         * Class Constructor.
         * Template is required.
         */
        public ClassLog2Ratio() {
            super(CATEGORICAL);
        }

        /**
         * No parameters.
         * Template is reqd.
         * Diff in means between classes as split by template
         * (NOT b/w profile and template)
         */
        public double getScore(Vector profile, Template template, Map params) {

            Vector[] vs = fSplitter.splitBiphasic(profile, template);
            int coiIndex = template.getClassOfInterestIndex();

            final boolean useMean = AlgMap.isMean(params);

            if (coiIndex == 0) {
                return XMath.log2(XMath.meanOrMedianRatio(vs[0], vs[1], useMean));
            } else {
                return XMath.log2(XMath.meanOrMedianRatio(vs[1], vs[0], useMean));
            }
        }

        public String getName() {
            return NAME;
        }

    } // End class ClassLog2Ratio

    public static class ClassDiff extends AbstractMetric {

        public static final String NAME = "Diff_of_Classes";

        /**
         * Class Constructor.
         * Template is required.
         */
        public ClassDiff() {
            super(CATEGORICAL);
        }

        /**
         * No parameters.
         * Template is reqd.
         * Diff in means between classes as split by template
         * (NOT b/w profile and template)
         */
        public double getScore(Vector profile, Template template, Map params) {

            Vector[] vs = fSplitter.splitBiphasic(profile, template);
            int coiIndex = template.getClassOfInterestIndex();

            final boolean useMean = AlgMap.isMean(params);

            if (coiIndex == 0) {
                return XMath.meanOrMedianDiff(vs[0], vs[1], useMean);
            } else {
                return XMath.meanOrMedianDiff(vs[1], vs[0], useMean);
            }
        }

        public String getName() {
            return NAME;
        }

    }


    public static class ClassMediansRatio extends AbstractMetric {

        public static final String NAME = "Ratio_of_Medians";

        /**
         * Class Constructor.
         * Template is required.
         */
        public ClassMediansRatio() {
            super(CATEGORICAL);
        }

        /**
         * No parameters.
         * Template is reqd.
         * Diff in means between classes as split by template
         * (NOT b/w profile and template)
         */
        public double getScore(Vector profile, Template template, Map params) {

            Vector[] vs = fSplitter.splitBiphasic(profile, template);
            int coiIndex = template.getClassOfInterestIndex();

            if (coiIndex == 0) {
                return XMath.mediansratio(vs[0], vs[1]);
            } else {
                return XMath.mediansratio(vs[1], vs[0]);
            }
        }

        public String getName() {
            return NAME;
        }

    }

    public static class ClassMeansLog2Ratio extends AbstractMetric {

        public static final String NAME = "log2_ratio_of_means";

        /**
         * Class Constructor.
         * Template is required.
         */
        public ClassMeansLog2Ratio() {
            super(CATEGORICAL);
        }

        /**
         * No parameters.
         * Template is reqd.
         * Diff in means between classes as split by template
         * (NOT b/w profile and template)
         */
        public double getScore(Vector profile, Template template, Map params) {

            Vector[] vs = fSplitter.splitBiphasic_nansafe(profile, template);
            int coiIndex = template.getClassOfInterestIndex();

            if (coiIndex == 0) {
                return XMath.log2(XMath.meansratio(vs[0], vs[1]));
            } else {
                return XMath.log2(XMath.meansratio(vs[1], vs[0]));
            }
        }

        public String getName() {
            return NAME;
        }


    }

    public static class ClassMediansLog2Ratio extends AbstractMetric {


        public static final String NAME = "log2_ratio_of_medians";

        /**
         * Class Constructor.
         * Template is required.
         */
        public ClassMediansLog2Ratio() {
            super(CATEGORICAL);
        }

        /**
         * No parameters.
         * Template is reqd.
         * Diff in means between classes as split by template
         * (NOT b/w profile and template)
         */
        public double getScore(Vector profile, Template template, Map params) {

            Vector[] vs = fSplitter.splitBiphasic_nansafe(profile, template);
            int coiIndex = template.getClassOfInterestIndex();

            if (coiIndex == 0) {
                return XMath.log2(XMath.mediansratio(vs[0], vs[1]));
            } else {
                return XMath.log2(XMath.mediansratio(vs[1], vs[0]));
            }
        }

        public String getName() {
            return NAME;
        }

    }

    /**
     * mean of only the class of interest
     *
     * @author Aravind Subramanian
     * @version %I%, %G%
     */
    public static class ClassOfInterestMean extends AbstractMetric {

        public static final String NAME = "ClassOfInterestMean";

        /**
         * Class Constructor.
         * Template is required.
         */
        public ClassOfInterestMean() {
            super(CATEGORICAL);
        }

        /**
         * No parameters.
         * Template is reqd
         */
        public double getScore(Vector profile, Template template, Map params) {

            Vector[] vs = fSplitter.splitBiphasic_nansafe(profile, template);
            int coiIndex = template.getClassOfInterestIndex();

            return vs[coiIndex].mean();
        }

        public String getName() {
            return NAME;
        }

    }

    /**
     * median of only the class of interest
     *
     * @author Aravind Subramanian
     * @version %I%, %G%
     */
    public static class ClassOfInterestMedian extends AbstractMetric {

        public static final String NAME = "ClassOfInterestMedian";

        /**
         * Class Constructor.
         * Template is required.
         */
        public ClassOfInterestMedian() {
            super(CATEGORICAL);
        }

        /**
         * No parameters.
         * Template is reqd
         */
        public double getScore(Vector profile, Template template, Map params) {

            Vector[] vs = fSplitter.splitBiphasic_nansafe(profile, template);
            int coiIndex = template.getClassOfInterestIndex();

            return vs[coiIndex].median();
        }

        public String getName() {
            return NAME;
        }

    }

    /**
     * Diff of medians
     */
    public static class ClassMediansDiff extends AbstractMetric {

        public static final String NAME = "Diff_of_Medians";

        /**
         * Class Constructor.
         */
        public ClassMediansDiff() {
            super(CATEGORICAL);
        }

        /**
         * No params.
         * <p/>
         * Template is reqd.
         * Diff in medians between classes as split by template
         * (NOT b/w profile and template)
         */
        public double getScore(Vector profile, Template template, Map params) {

            Vector[] vs = fSplitter.splitBiphasic_nansafe(profile, template);
            int coiIndex = template.getClassOfInterestIndex();

            if (coiIndex == 0) {
                return XMath.mediansdiff(vs[0], vs[1]);
            } else {
                return XMath.mediansdiff(vs[1], vs[0]);
            }
        }

        public String getName() {
            return NAME;
        }

    }

}    // End Metrics
