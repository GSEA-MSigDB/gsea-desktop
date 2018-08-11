/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.math;

import edu.mit.broad.genome.objects.LabelledVector;
import gnu.trove.TFloatArrayList;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aravind Subramanian
 */
public class LabelledVectorProcessors {

    // @todo maybe the process for vectors is not as eff as possible (wasteful arrray of str)

    /**
     * For logging support
     */
    protected static final transient Logger klog = Logger.getLogger(LabelledVectorProcessors.class);

    // @maint add a metric and this array might need updating
    public static LabelledVectorProcessor[] createAllProcessors() {
        return new LabelledVectorProcessor[]{
                new None(),
                new NormalizeLinear(),
                new NormalizeToSumTwoSided(),
                new NormalizeToMaxTwoSided(),
                new NormalizeAreaTwoSided(),
                new NormalizeToMaxThenAreaTwoSided()
        };
    }

    public static LabelledVectorProcessor lookupProcessor(final Object obj) {

        if (obj == null) {
            throw new NullPointerException("Cannot lookup for null object");
        }

        if (obj instanceof LabelledVectorProcessor) {
            return (LabelledVectorProcessor) obj;
        }

        LabelledVectorProcessor[] all = createAllProcessors();

        for (int i = 0; i < all.length; i++) {
            if (all[i].getName().equalsIgnoreCase(obj.toString())) {
                return all[i];
            }
        }

        throw new RuntimeException("Cannot lookupProcessor for: " + obj);
    }

    static abstract class AbstractLabelledVectorProcessor implements LabelledVectorProcessor {

        protected AbstractLabelledVectorProcessor() {

        }

        public String toString() {
            return getName();
        }

        public Vector process(final Vector v) {
            return (process(new LabelledVector("foo", v))).getScoresV(false);
        }

        public void process(final List dels) {
            Vector v = process(new Vector(dels));
            for (int i = 0; i < dels.size(); i++) {
                DoubleElement del = (DoubleElement) dels.get(i);
                del.fValue = v.getElement(i); // change value but not the index
                dels.set(i, del);
            }
        }

    }

    public static class None extends AbstractLabelledVectorProcessor {

        public String getName() {
            return "none";
        }

        public LabelledVector process(final LabelledVector rl) {
            return rl;
        }

        public Vector process(final Vector v) {
            return v;
        }

        public void process(final List dels) {
        }
    }

    public static class NormalizeToSumTwoSided extends AbstractLabelledVectorProcessor {

        public String getName() {
            return "norm_to_sum_two_sided";
        }

        public LabelledVector process(final LabelledVector rl) {

            //System.out.println("Doing norm sds");
            double totalPos = rl.getScoresV(false).sum(ScoreMode.POS_ONLY);
            double totalNeg = Math.abs(rl.getScoresV(false).sum(ScoreMode.NEG_ONLY));

            Vector two_sided_norm = new Vector(rl.getSize());
            String[] labels = new String[rl.getSize()];
            for (int r = 0; r < rl.getSize(); r++) {
                double score = rl.getScore(r);
                labels[r] = rl.getLabel(r);
                if (XMath.isPositive(score)) {
                    score = score / totalPos;
                } else {
                    score = score / totalNeg; // @note abs'ed above so the sign persists
                }
                two_sided_norm.setElement(r, score);
            }

            return new LabelledVector(labels, two_sided_norm);
        }

    }

    public static class NormalizeLinear extends AbstractLabelledVectorProcessor {

        public String getName() {
            return "norm_linear";
        }

        public LabelledVector process(final LabelledVector orig) {
            int pos_len = orig.getMetricWeightStruc().getTotalPosLength();
            int neg_len = orig.getMetricWeightStruc().getTotalNegLength();

            float stop_point = (float) XMath.min(pos_len, neg_len);

            //float mid_point = orig.getSize() / 2;

            float stop_point_pos = stop_point;
            float start_point_neg = orig.getSize() - stop_point;

            List names = new ArrayList();
            TFloatArrayList scores = new TFloatArrayList();

            for (int r = 0; r < orig.getSize(); r++) {
                float score;
                if (r < stop_point_pos) {
                    score = (stop_point - r) / stop_point;
                } else if (r > start_point_neg) {
                    score = -1.0f * (stop_point - (orig.getSize() - 1 - r)) / stop_point;
                } else {
                    score = 0;
                }
                scores.add(score);
                names.add(orig.getLabel(r));
            }

            return new LabelledVector(orig.getName() + "_linearized", names, scores);
        }

    }

    public static class NormalizeToMaxTwoSided extends AbstractLabelledVectorProcessor {

        public String getName() {
            return "norm_to_max_two_sided";
        }

        public LabelledVector process(final LabelledVector rl) {

            //System.out.println("Doing norm sds");
            float maxPos = rl.getScoresV(false).max();
            float maxNeg = Math.abs(rl.getScoresV(false).min());

            Vector max_two_sided_norm = new Vector(rl.getSize());
            String[] labels = new String[rl.getSize()];
            for (int r = 0; r < rl.getSize(); r++) {
                float score = rl.getScore(r);
                labels[r] = rl.getLabel(r);
                if (XMath.isPositive(score)) {
                    score = score / maxPos;
                } else {
                    score = score / maxNeg; // @note abs'ed above so the sign persists
                }
                max_two_sided_norm.setElement(r, score);
            }

            return new LabelledVector(labels, max_two_sided_norm);
        }
    }

    public static class NormalizeAreaTwoSided extends AbstractLabelledVectorProcessor {

        public String getName() {
            return "norm_area_two_sided";
        }

        public LabelledVector process(final LabelledVector rl) {

            double totalPos = rl.getScoresV(false).sum(ScoreMode.POS_ONLY);
            double totalNeg = Math.abs(rl.getScoresV(false).sum(ScoreMode.NEG_ONLY));

            double posFactor;
            double negFactor;
            // doesnt matter if we boost/lessen, but for consistency lets always boost
            if (totalPos > totalNeg) {
                posFactor = 1.0d;
                negFactor = totalPos / totalNeg;
            } else {
                posFactor = totalNeg / totalPos;
                negFactor = 1.0d;
            }

            Vector area_norm = new Vector(rl.getSize());
            String[] labels = new String[rl.getSize()];
            for (int r = 0; r < rl.getSize(); r++) {
                double score = rl.getScore(r);
                labels[r] = rl.getLabel(r);
                if (XMath.isPositive(score)) {
                    score = score * posFactor;
                } else {
                    score = score * negFactor; // @note abs'ed above so the sign persists
                }
                area_norm.setElement(r, score);
            }


            System.out.println("pos_sum: " + area_norm.sum(ScoreMode.POS_ONLY) + " neg_sum: " + area_norm.sum(ScoreMode.NEG_ONLY));

            return new LabelledVector(labels, area_norm);
        }

    }


    public static class NormalizeToMaxThenAreaTwoSided extends AbstractLabelledVectorProcessor {

        public String getName() {
            return "norm_max_then_area_two_sided";
        }

        public LabelledVector process(final LabelledVector rl) {
            LabelledVector rln = new NormalizeToMaxTwoSided().process(rl);
            return new NormalizeAreaTwoSided().process(rln);
        }

    }

} // End class RankedListProcessors


