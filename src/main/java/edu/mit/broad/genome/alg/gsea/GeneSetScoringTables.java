/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.alg.gsea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.math.XMath;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.RankedList;

/**
 * @author Aravind Subramanian, David Eby
 */
public class GeneSetScoringTables {
    private static final Logger klog = LoggerFactory.getLogger(GeneSetScoringTables.class);

    public static GeneSetScoringTable[] createAllScoringTables() {
        return new GeneSetScoringTable[]{
                new Classic(),
                new Weighted(),
                new WeightedSquared(),
                new WeightedOnePointFive()
        };
    }

    public static GeneSetScoringTable lookupGeneSetScoringTable(Object obj) {
        if (obj == null) { throw new NullPointerException("Cannot lookup for null object"); }
        if (obj instanceof GeneSetScoringTable) { return (GeneSetScoringTable) obj; }

        GeneSetScoringTable[] all = createAllScoringTables();
        for (int i = 0; i < all.length; i++) {
            if (all[i].getName().equalsIgnoreCase(obj.toString())) {
                return all[i];
            }
        }

        throw new RuntimeException("Cannot lookup GeneSetScoringTable for: " + obj);
    }

    static abstract class AbstractScoringTable implements GeneSetScoringTable {
        public AbstractScoringTable() { }

        public int hashCode() { return getName().hashCode(); }

        public boolean equals(Object obj) {
            if (obj instanceof GeneSetScoringTable) {
                return getName().equalsIgnoreCase(((GeneSetScoringTable) obj).getName());
            }

            return false;
        }

        public String toString() { return getName(); }
    }

    // classic classic scheme (classic)
    public static class Classic extends AbstractScoringTable {
        private static final String NAME = "classic";
        // compute penalties
        private float hitpoints;
        private float misspoints;
        private RankedList rankedList;

        public Classic() { }

        // total score of hits G (1 per hit)
        // total score of misses -G so that total total = 0
        // hence penalty per miss = G / (N-G)
        Classic(GeneSet gset, RankedList rl) {
            this.rankedList = rl;
            int ntrue = gset.getNumMembers();
            int totSize = rl.getSize();

            // compute penalties
            this.hitpoints = 1.0f / (float) ntrue;    // arbitarily
            this.misspoints = 1.0f / ((float) totSize - (float) ntrue);
        }

        public GeneSetScoringTable createTable(GeneSet gset, RankedList rl, RankedList realRankedList) {
            return new Classic(gset, rl);
        }

        public String getName() { return NAME; }

        public RankedList getRankedList() { return rankedList; }

        public float getHitScore(final String name) { return hitpoints; }

        // misses are not weighted
        public float getMissScore(String name) { return misspoints; }
    }

    // Needed as cdna give some nans for the class metric
    private static final float _abs(float score) {
        return (Float.isFinite(score)) ? Math.abs(score) : 0.01f;
    }

    public static class Weighted extends AbstractScoringTable {
        private static final String NAME = "weighted";

        private float totalWeight;
        private float nhExpected;
        private float miss_score;

        private GeneSet gset;
        private RankedList rankedList;

        public Weighted() { }

        public Weighted(final GeneSet gset, final RankedList rl) {
            this.gset = gset;
            this.nhExpected = gset.getNumMembers();

            if (nhExpected == 0) { throw new IllegalArgumentException("Number of members in gene set cannot be 0: " + gset.getName()); }

            this.rankedList = rl;
            for (int i = 0; i < this.gset.getNumMembers(); i++) {
                float score = this.rankedList.getScore(gset.getMember(i));
                totalWeight += _abs(score);
            }

            final float nTotal = rankedList.getSize();
            this.miss_score = 1.0f / (nTotal - nhExpected);
        }

        public String getName() { return NAME; }

        public RankedList getRankedList() { return rankedList; }

        public GeneSetScoringTable createTable(final GeneSet gset, final RankedList rl, final RankedList realRankedList) {
            return new Weighted(gset, rl);
        }

        public float getHitScore(final String name) {
            float score = rankedList.getScore(name);
            score = _abs(score);
            return score / totalWeight;
        }

        // misses are not weighted
        public float getMissScore(String name) { return miss_score; }
    }

    public static class WeightedSquared extends AbstractScoringTable {
        private static final String NAME = "weighted_p2";

        private float totalWeight_sq;
        private float nhExpected;
        private float miss_score;
        private GeneSet gset;
        private RankedList rankedList;

        public WeightedSquared() { }

        public WeightedSquared(final GeneSet gset, final RankedList rl) {
            this.gset = gset;
            this.nhExpected = gset.getNumMembers();

            if (nhExpected == 0) { throw new IllegalArgumentException("Number of members in gene set cannot be 0: " + gset.getName()); }

            this.rankedList = rl;
            for (int i = 0; i < this.gset.getNumMembers(); i++) {
                float score = this.rankedList.getScore(gset.getMember(i));
                float score_pow = score * score;
                totalWeight_sq += Float.isFinite(score_pow) ? score_pow : 0.000001f;
            }

            final float nTotal = rankedList.getSize();
            this.miss_score = 1.0f / (nTotal - nhExpected);
        }

        public String getName() { return NAME; }

        public RankedList getRankedList() { return rankedList; }

        public GeneSetScoringTable createTable(GeneSet gset, RankedList rl, RankedList realRankedList) {
            return new WeightedSquared(gset, rl);
        }

        public float getHitScore(String name) {
            float score = rankedList.getScore(name);
            float hitScore = (score * score) / totalWeight_sq;
            return Float.isFinite(hitScore) ? hitScore : 0.000001f;
        }

        // misses are not weighted
        public float getMissScore(String name) { return miss_score; }
    }

    public static class WeightedOnePointFive extends AbstractScoringTable {
        private static final String NAME = "weighted_p1.5";

        private float totalWeight_sq;
        private float nhExpected;
        private float miss_score;
        private GeneSet gset;
        private RankedList rankedList;

        public WeightedOnePointFive() { }

        public WeightedOnePointFive(final GeneSet gset, final RankedList rl) {
            this.gset = gset;
            this.nhExpected = gset.getNumMembers();
            if (nhExpected == 0) { throw new IllegalArgumentException("Number of members in gene set cannot be 0: " + gset.getName()); }

            this.rankedList = rl;
            for (int i = 0; i < this.gset.getNumMembers(); i++) {
                float score = this.rankedList.getScore(gset.getMember(i));
                float score_pow = (float) Math.pow(Math.abs(score), 1.5);
                totalWeight_sq += Float.isFinite(score_pow) ? score_pow : 0.000001f;
            }

            final float nTotal = rankedList.getSize();
            this.miss_score = 1.0f / (nTotal - nhExpected);
        }

        public String getName() { return NAME; }

        public RankedList getRankedList() { return rankedList; }

        public GeneSetScoringTable createTable(GeneSet gset, RankedList rl, RankedList realRankedList) {
            return new WeightedOnePointFive(gset, rl);
        }

        public float getHitScore(String name) {
            float score = rankedList.getScore(name);
            float ss = (float) Math.pow(score, 1.5);
            float hitScore = ss / totalWeight_sq;
            return Float.isFinite(hitScore) ? hitScore : 0.000001f;
        }

        // misses are not weighted
        public float getMissScore(String name) { return miss_score; }
    }

    /**
     * Shared hit/miss arithmetic for the two double-sided weighting schemes.
     * They differ only in whether each side is scaled by its hit fraction.
     */
    private static final class DoubleSidedWeights {
        private RankedList rankedList;
        private final int maxPosRealRank;
        private final float totalPosWeight;
        private final float totalNegWeight;
        private final float nhPosByNh;
        private final float nhNegByNh;
        private final float missPosScore;
        private final float missNegScore;

        private DoubleSidedWeights(RankedList rankedList, int maxPosRealRank, float totalPosWeight,
                float totalNegWeight, float nhPosByNh, float nhNegByNh, float missPosScore, float missNegScore) {
            this.rankedList = rankedList;
            this.maxPosRealRank = maxPosRealRank;
            this.totalPosWeight = totalPosWeight;
            this.totalNegWeight = totalNegWeight;
            this.nhPosByNh = nhPosByNh;
            this.nhNegByNh = nhNegByNh;
            this.missPosScore = missPosScore;
            this.missNegScore = missNegScore;
        }

        static DoubleSidedWeights compute(GeneSet gset, RankedList rl, RankedList real, boolean scaleByHitFraction) {
            if (gset.getNumMembers() == 0) {
                throw new IllegalArgumentException("Number of members in gene set cannot be 0: " + gset.getName());
            }
            int maxPosRealRank;
            if (real == null) {
                klog.warn("Null real_rl so assuming real: {}", rl.getName());
                maxPosRealRank = rl.getMetricWeightStruc().getTotalPosLength();
            } else {
                maxPosRealRank = real.getMetricWeightStruc().getTotalPosLength();
            }

            float totalPosWeight = 0f;
            float totalNegWeight = 0f;
            float nhPosActual = 0f;
            float nhNegActual = 0f;
            for (int i = 0; i < gset.getNumMembers(); i++) {
                String name = gset.getMember(i);
                float score = rl.getScore(name);
                int rank = rl.getRank(name);
                if (XMath.isPositive(score)) {
                    if (rank <= maxPosRealRank) {
                        totalPosWeight += score;
                        nhPosActual++;
                    } else {
                        nhNegActual++;
                    }
                } else if (rank <= maxPosRealRank) {
                    nhPosActual++;
                } else {
                    totalNegWeight += score;
                    nhNegActual++;
                }
            }

            float numPosScores = 0f;
            float numNegScores = 0f;
            for (int r = 0; r < rl.getSize(); r++) {
                float score = rl.getScore(r);
                if (XMath.isPositive(score) && r <= maxPosRealRank) {
                    numPosScores++;
                } else {
                    numNegScores++;
                }
            }

            float nhExpected = gset.getNumMembers();
            if (nhPosActual + nhNegActual != nhExpected) {
                throw new IllegalArgumentException("nhPosActual: " + nhPosActual + " nhNegActual: " + nhNegActual
                        + " nhExpected: " + nhExpected);
            }

            float nhPosByNh = scaleByHitFraction ? nhPosActual / nhExpected : 1.0f;
            float nhNegByNh = scaleByHitFraction ? nhNegActual / nhExpected : 1.0f;
            return new DoubleSidedWeights(rl, maxPosRealRank, totalPosWeight, totalNegWeight, nhPosByNh, nhNegByNh,
                    nhPosByNh * (1 / (numPosScores - nhPosActual)),
                    nhNegByNh * (1 / (numNegScores - nhNegActual)));
        }

        void setReal(RankedList rl) {
            this.rankedList = rl;
        }

        RankedList rankedList() {
            return rankedList;
        }

        float hitScore(String name) {
            float score = rankedList.getScore(name);
            int rank = rankedList.getRank(name);
            if (XMath.isPositive(score)) {
                if (rank <= maxPosRealRank) {
                    return (score / totalPosWeight) * nhPosByNh;
                }
                return 0.0f;
            }
            if (rank <= maxPosRealRank) {
                return 0.0f;
            }
            return (score / totalNegWeight) * nhNegByNh;
        }

        float missScore(String name) {
            if (XMath.isPositive(rankedList.getScore(name))) {
                return missPosScore;
            }
            return missNegScore;
        }
    }

    public static class WeightedDoubleSidedAs extends AbstractScoringTable {
        private static final String NAME = "weighted_as";
        private DoubleSidedWeights weights;
        /** Set on the unbound prototype; {@link GeneSetCohort} reads it back as the real list. */
        private RankedList prototypeReal;

        public WeightedDoubleSidedAs() {
        }

        public WeightedDoubleSidedAs(final GeneSet gset, final RankedList rl, final RankedList real) {
            this.weights = DoubleSidedWeights.compute(gset, rl, real, true);
        }

        public void setReal(RankedList rl) {
            this.prototypeReal = rl;
            if (weights != null) {
                weights.setReal(rl);
            }
        }

        public GeneSetScoringTable createTable(GeneSet gset, RankedList rl, RankedList real) {
            return new WeightedDoubleSidedAs(gset, rl, real);
        }

        public String getName() {
            return NAME;
        }

        public RankedList getRankedList() {
            return weights != null ? weights.rankedList() : prototypeReal;
        }

        public float getHitScore(final String name) {
            return weights.hitScore(name);
        }

        public float getMissScore(String name) {
            return weights.missScore(name);
        }
    }

    public static class WeightedDoubleSidedAs1 extends AbstractScoringTable {
        private static final String NAME = "weighted_as_1";
        private DoubleSidedWeights weights;
        /** Set on the unbound prototype; {@link GeneSetCohort} reads it back as the real list. */
        private RankedList prototypeReal;

        public WeightedDoubleSidedAs1() {
        }

        public WeightedDoubleSidedAs1(final GeneSet gset, final RankedList rl, final RankedList real) {
            this.weights = DoubleSidedWeights.compute(gset, rl, real, false);
        }

        public void setReal(RankedList rl) {
            this.prototypeReal = rl;
            if (weights != null) {
                weights.setReal(rl);
            }
        }

        public GeneSetScoringTable createTable(GeneSet gset, RankedList rl, RankedList real) {
            return new WeightedDoubleSidedAs1(gset, rl, real);
        }

        public String getName() {
            return NAME;
        }

        public RankedList getRankedList() {
            return weights != null ? weights.rankedList() : prototypeReal;
        }

        public float getHitScore(final String name) {
            return weights.hitScore(name);
        }

        public float getMissScore(String name) {
            return weights.missScore(name);
        }
    }
}
