/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg.gsea;

import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.math.XMath;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.RankedList;
import org.apache.log4j.Logger;

/**
 * @author Aravind Subramanian
 */
public class GeneSetScoringTables {

    private static final Logger klog = XLogger.getLogger(GeneSetScoringTables.class);

    public static GeneSetScoringTable[] createAllScoringTables() {
        return new GeneSetScoringTable[]{
                new Classic(),
                new Weighted(),
                new WeightedSquared(),
                new WeightedOnePointFive()
        };
    }

    public static GeneSetScoringTable lookupGeneSetScoringTable(Object obj) {

        if (obj == null) {
            throw new NullPointerException("Cannot lookup for null object");
        }

        if (obj instanceof GeneSetScoringTable) {
            return (GeneSetScoringTable) obj;
        }

        GeneSetScoringTable[] all = createAllScoringTables();

        for (int i = 0; i < all.length; i++) {
            if (all[i].getName().equalsIgnoreCase(obj.toString())) {
                return all[i];
            }
        }

        throw new RuntimeException("Cannot lookup GeneSetScoringTable for: " + obj);
    }


    static abstract class AbstractScoringTable implements GeneSetScoringTable {


        public AbstractScoringTable() {
        }

        public int hashCode() {
            return getName().hashCode();
        }

        public boolean equals(Object obj) {

            if (obj instanceof GeneSetScoringTable) {
                return getName().equalsIgnoreCase(((GeneSetScoringTable) obj).getName());
            }

            return false;
        }

        public String toString() {
            return getName();
        }
    }

    // classic classic scheme (classic)
    public static class Classic extends AbstractScoringTable {

        private static final String NAME = "classic";
        // compute penalties
        private float hitpoints;
        private float misspoints;
        private RankedList rankedList;

        public Classic() {
        }

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

        public String getName() {
            return NAME;
        }

        public RankedList getRankedList() {
            return rankedList;
        }

        public float getHitScore(final String name) {
            return hitpoints;
        }

        // misses are not weighted
        public float getMissScore(String name) {
            return misspoints;
        }

    }

    // Needed as cdna give some nans for the class metric
    private static float _abs(float score) {
        if (Float.isNaN(score) || Float.isInfinite(score)) {
            return 0.01f;
        } else {
            return Math.abs(score);
        }
    }


    public static class Weighted extends AbstractScoringTable {

        private static final String NAME = "weighted";

        private float totalWeight;
        private float nhExpected;

        private float miss_score;

        private GeneSet gset;
        private RankedList rankedList;

        public Weighted() {
        }

        public Weighted(final GeneSet gset, final RankedList rl) {

            this.gset = gset;
            this.nhExpected = gset.getNumMembers();

            if (nhExpected == 0) {
                throw new IllegalArgumentException("Number of members in gene set cannot be 0: " + gset.getName());
            }

            this.rankedList = rl;
            for (int i = 0; i < this.gset.getNumMembers(); i++) {
                float score = this.rankedList.getScore(gset.getMember(i));
                totalWeight += _abs(score);
            }

            final float nTotal = rankedList.getSize();
            this.miss_score = 1.0f / (nTotal - nhExpected);
        }

        public String getName() {
            return NAME;
        }

        public RankedList getRankedList() {
            return rankedList;
        }

        public GeneSetScoringTable createTable(final GeneSet gset, final RankedList rl, final RankedList realRankedList) {
            return new Weighted(gset, rl);
        }

        public float getHitScore(final String name) {

            float score = rankedList.getScore(name);

            score = _abs(score);

            if (XMath.isPositive(score)) {
                return score / totalWeight;
            } else {
                return score / totalWeight;
            }
        }

        // misses are not weighted
        public float getMissScore(String name) {
            return miss_score;
        }
    } // End class Weighted


    public static class WeightedSquared extends AbstractScoringTable {

        private static final String NAME = "weighted_p2";

        private float totalWeight_sq;
        private float nhExpected;

        private float miss_score;

        private GeneSet gset;
        private RankedList rankedList;

        public WeightedSquared() {
        }

        public WeightedSquared(final GeneSet gset, final RankedList rl) {

            this.gset = gset;
            this.nhExpected = gset.getNumMembers();

            if (nhExpected == 0) {
                throw new IllegalArgumentException("Number of members in gene set cannot be 0: " + gset.getName());
            }

            this.rankedList = rl;
            for (int i = 0; i < this.gset.getNumMembers(); i++) {
                float score = this.rankedList.getScore(gset.getMember(i));
                totalWeight_sq += score * score;
            }

            final float nTotal = rankedList.getSize();
            this.miss_score = 1.0f / (nTotal - nhExpected);
        }

        public String getName() {
            return NAME;
        }

        public RankedList getRankedList() {
            return rankedList;
        }

        public GeneSetScoringTable createTable(GeneSet gset, RankedList rl, RankedList realRankedList) {
            return new WeightedSquared(gset, rl);
        }

        public float getHitScore(String name) {
            float score = rankedList.getScore(name);
            return (score * score) / totalWeight_sq;
        }

        // misses are not weighted
        public float getMissScore(String name) {
            return miss_score;
        }
    } // End class WeightedSquared

    public static class WeightedOnePointFive extends AbstractScoringTable {

        private static final String NAME = "weighted_p1.5";

        private float totalWeight_sq;

        private float nhExpected;

        private float miss_score;

        private GeneSet gset;

        private RankedList rankedList;

        public WeightedOnePointFive() {
        }

        public WeightedOnePointFive(final GeneSet gset, final RankedList rl) {

            this.gset = gset;
            this.nhExpected = gset.getNumMembers();

            if (nhExpected == 0) {
                throw new IllegalArgumentException("Number of members in gene set cannot be 0: " + gset.getName());
            }

            this.rankedList = rl;
            for (int i = 0; i < this.gset.getNumMembers(); i++) {
                float score = this.rankedList.getScore(gset.getMember(i));
                totalWeight_sq += Math.pow(score, 1.5);
            }

            final float nTotal = rankedList.getSize();
            this.miss_score = 1.5f / (nTotal - nhExpected);
        }

        public String getName() {
            return NAME;
        }

        public RankedList getRankedList() {
            return rankedList;
        }

        public GeneSetScoringTable createTable(GeneSet gset, RankedList rl, RankedList realRankedList) {
            return new WeightedOnePointFive(gset, rl);
        }

        public float getHitScore(String name) {
            float score = rankedList.getScore(name);
            float ss = (float) Math.pow(score, 0.5);
            return ss / totalWeight_sq;
        }

        // misses are not weighted
        public float getMissScore(String name) {
            return miss_score;
        }
    } // End class WeightedHalf

    public static class WeightedDoubleSidedAs extends AbstractScoringTable {

        private static final String NAME = "weighted_as";

        private float totalPosWeight_of_hits;
        private float totalNegWeight_of_hits;

        private float numPosScores;
        private float numNegScores;

        private float nhExpected;
        private float nhPosActual;
        private float nhNegActual;

        private float nhPos_by_nh;
        private float nhNeg_by_nh;

        private float miss_pos_score;
        private float miss_neg_score;

        private int maxPosRealRank;

        private GeneSet gset;
        private RankedList rankedList;

        public void setReal(RankedList rl) {
            this.rankedList = rl;
        }

        public WeightedDoubleSidedAs() {
        }

        static int cnt = 0;

        public WeightedDoubleSidedAs(final GeneSet gset, final RankedList rl, final RankedList real) {

            this.gset = gset;
            this.rankedList = rl;
            this.nhExpected = gset.getNumMembers();

            if (nhExpected == 0) {
                throw new IllegalArgumentException("Number of members in gene set cannot be 0: " + gset.getName());
            }

            if (real == null) {
                klog.warn("Null real_rl so assuming real: " + rl.getName());
                this.maxPosRealRank = rl.getMetricWeightStruc().getTotalPosLength();
            } else {
                this.maxPosRealRank = real.getMetricWeightStruc().getTotalPosLength();
            }

            for (int i = 0; i < this.gset.getNumMembers(); i++) {
                String name = this.gset.getMember(i);
                float score = rl.getScore(name);
                int rank = rl.getRank(name);

                if (XMath.isPositive(score)) {
                    // if the rank is beyond the max pos rank of the real list
                    // then the weight is 0 and the gene wasnt really on the list
                    if (rank <= maxPosRealRank) {
                        totalPosWeight_of_hits += score;
                        nhPosActual++;
                    } else {// weight add is 0 and its NOT a pos list hit
                        totalNegWeight_of_hits += 0;
                        nhNegActual++;
                    }
                } else {
                    // if the rank is lower than the cross over point
                    // then the weight is 0 and the gene wasnt really on the list
                    if (rank <= maxPosRealRank) {
                        totalPosWeight_of_hits += 0;
                        nhPosActual++;
                    } else {
                        totalNegWeight_of_hits += score;
                        nhNegActual++;
                    }
                }
            }

            for (int r = 0; r < rl.getSize(); r++) {
                float score = rl.getScore(r);
                if (XMath.isPositive(score) && r <= maxPosRealRank) {
                    numPosScores++;
                } else {
                    numNegScores++;
                }
            }

            if (nhPosActual + nhNegActual != nhExpected) {
                throw new IllegalArgumentException("nhPosActual: " + nhPosActual + " nhNegActual: " + nhNegActual + " nhExpected: " + nhExpected);
            }


            this.nhPos_by_nh = nhPosActual / nhExpected;
            this.nhNeg_by_nh = nhNegActual / nhExpected;

            this.miss_pos_score = nhPos_by_nh * (1 / (numPosScores - nhPosActual));
            this.miss_neg_score = nhNeg_by_nh * (1 / (numNegScores - nhNegActual));

            if (cnt % 250 == 0) {
                System.out.println("xover: " + maxPosRealRank + " nhPos_by_nh: " + nhPos_by_nh + " nhNeg_by_nh: " + nhNeg_by_nh);
            }
            cnt++;

            //System.out.println("miss_pos_score: " + miss_pos_score);
            //System.out.println("miss_neg_score: " + miss_neg_score);

        }

        public GeneSetScoringTable createTable(GeneSet gset, RankedList rl, RankedList real) {
            return new WeightedDoubleSidedAs(gset, rl, real);
        }

        public String getName() {
            return NAME;
        }

        public RankedList getRankedList() {
            return rankedList;
        }

        public float getHitScore(final String name) {

            float score = rankedList.getScore(name);
            int rank = rankedList.getRank(name);

            float weight;

            if (XMath.isPositive(score)) {

                if (rank <= maxPosRealRank) {
                    weight = score / totalPosWeight_of_hits;
                    return weight * nhPos_by_nh; // this will be a positive number (i.e go UP)
                } else {
                    return 0.0f;
                }


            } else {

                if (rank <= maxPosRealRank) {
                    return 0.0f;
                } else {
                    weight = score / totalNegWeight_of_hits;
                    return weight * nhNeg_by_nh; // this will be a negative number (i.e go DOWN)
                }
            }
        }

        // misses are not weighted
        public float getMissScore(String name) {

            float score = rankedList.getScore(name);

            if (XMath.isPositive(score)) {
                return miss_pos_score;
            } else {
                return miss_neg_score; // a bit unintuitive but the kscore alg does a minus for misses
                //return -1 * miss_neg_score; // a bit unintuitive byt the kscore alg does a minus for misses
            }
        }
    }

    public static class WeightedDoubleSidedAs1 extends AbstractScoringTable {

        private static final String NAME = "weighted_as_1";

        private float totalPosWeight_of_hits;
        private float totalNegWeight_of_hits;

        private float numPosScores;
        private float numNegScores;

        private float nhExpected;
        private float nhPosActual;
        private float nhNegActual;

        private float nhPos_by_nh;
        private float nhNeg_by_nh;

        private float miss_pos_score;
        private float miss_neg_score;

        private int maxPosRealRank;

        private GeneSet gset;
        private RankedList rankedList;

        public void setReal(RankedList rl) {
            this.rankedList = rl;
        }

        public WeightedDoubleSidedAs1() {
        }

        static int cnt = 0;

        public WeightedDoubleSidedAs1(final GeneSet gset, final RankedList rl, final RankedList real) {

            this.gset = gset;
            this.rankedList = rl;
            this.nhExpected = gset.getNumMembers();

            if (nhExpected == 0) {
                throw new IllegalArgumentException("Number of members in gene set cannot be 0: " + gset.getName());
            }

            if (real == null) {
                klog.warn("Null real_rl so assuming real: " + rl.getName());
                this.maxPosRealRank = rl.getMetricWeightStruc().getTotalPosLength();
            } else {
                this.maxPosRealRank = real.getMetricWeightStruc().getTotalPosLength();
            }


            for (int i = 0; i < this.gset.getNumMembers(); i++) {
                String name = this.gset.getMember(i);
                float score = rl.getScore(name);
                int rank = rl.getRank(name);

                if (XMath.isPositive(score)) {
                    // if the rank is beyond the max pos rank of the real list
                    // then the weight is 0 and the gene wasnt really on the list
                    if (rank <= maxPosRealRank) {
                        totalPosWeight_of_hits += score;
                        nhPosActual++;
                    } else {// weight add is 0 and its NOT a pos list hit
                        totalNegWeight_of_hits += 0;
                        nhNegActual++;
                    }
                } else {
                    // if the rank is lower than the cross over point
                    // then the weight is 0 and the gene wasnt really on the list
                    if (rank <= maxPosRealRank) {
                        totalPosWeight_of_hits += 0;
                        nhPosActual++;
                    } else {
                        totalNegWeight_of_hits += score;
                        nhNegActual++;
                    }
                }
            }

            for (int r = 0; r < rl.getSize(); r++) {
                float score = rl.getScore(r);
                if (XMath.isPositive(score) && r <= maxPosRealRank) {
                    numPosScores++;
                } else {
                    numNegScores++;
                }
            }

            if (nhPosActual + nhNegActual != nhExpected) {
                throw new IllegalArgumentException("nhPosActual: " + nhPosActual + " nhNegActual: " + nhNegActual + " nhExpected: " + nhExpected);
            }

            //this.nhPos_by_nh = nhPosActual / nhExpected;
            //this.nhNeg_by_nh = nhNegActual / nhExpected;

            this.nhPos_by_nh = 1.0f;
            this.nhNeg_by_nh = 1.0f;


            this.miss_pos_score = nhPos_by_nh * (1 / (numPosScores - nhPosActual));
            this.miss_neg_score = nhNeg_by_nh * (1 / (numNegScores - nhNegActual));

            if (cnt % 250 == 0) {
                System.out.println("xover: " + maxPosRealRank + " nhPos_by_nh: " + nhPos_by_nh + " nhNeg_by_nh: " + nhNeg_by_nh);
            }
            cnt++;

            //System.out.println("miss_pos_score: " + miss_pos_score);
            //System.out.println("miss_neg_score: " + miss_neg_score);

        }

        public GeneSetScoringTable createTable(GeneSet gset, RankedList rl, RankedList real) {
            return new WeightedDoubleSidedAs1(gset, rl, real);
        }

        public String getName() {
            return NAME;
        }

        public RankedList getRankedList() {
            return rankedList;
        }

        public float getHitScore(final String name) {

            float score = rankedList.getScore(name);
            int rank = rankedList.getRank(name);

            float weight;

            if (XMath.isPositive(score)) {

                if (rank <= maxPosRealRank) {
                    weight = score / totalPosWeight_of_hits;
                    return weight * nhPos_by_nh; // this will be a positive number (i.e go UP)
                } else {
                    return 0.0f;
                }


            } else {

                if (rank <= maxPosRealRank) {
                    return 0.0f;
                } else {
                    weight = score / totalNegWeight_of_hits;
                    return weight * nhNeg_by_nh; // this will be a negative number (i.e go DOWN)
                }
            }
        }

        // misses are not weighted
        public float getMissScore(String name) {

            float score = rankedList.getScore(name);

            if (XMath.isPositive(score)) {
                return miss_pos_score;
            } else {
                return miss_neg_score; // a bit unintuitive but the kscore alg does a minus for misses
                //return -1 * miss_neg_score; // a bit unintuitive byt the kscore alg does a minus for misses
            }
        }
    }


} // End class GeneSetCohorts