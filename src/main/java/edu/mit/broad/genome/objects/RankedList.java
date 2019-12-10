/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.math.*;
import gnu.trove.TFloatArrayList;

import java.util.ArrayList;
import java.util.List;

/**
 * Different from LabelledList, here there IS  a sense of order (rankings) as well as a
 * name associated with every rank.
 * <p/>
 * IMP IMP: Ranks are always always from 0 to n-1
 * (scores can be something else, but ranks are always contiguous and 0 to n-1)
 */
public interface RankedList extends PersistentObject {

    public RankedList cloneShallowRL(String newName);

    public int getSize();

    public int getSize(ScoreMode smode);

    public float getScore(final int rank);

    public float getScore(final String label);

    public float[] getScores(final GeneSet gset);

    public Vector getScoresV(final boolean clonedCopy);

    public int getRank(final String label);

    public String getRankName(final int rank);

    public List<String> getRankedNames();

    public String[] getRankedNamesArray();

    public List<String> getNamesOfUpOrDnXRanks(final int upOrDnX, final boolean up);

    public RankedList extractRanked(final GeneSet gset);

    public RankedList extractRanked(final ScoreMode smode);

    public MetricWeightStruc getMetricWeightStruc();

    static class Helper {

        // collect and sort by rank
        // Remember that ranks are always 0(good) to n-1(bad)
        public static RankedList extract(final GeneSet gset, final RankedList rl) {

            DoubleElement[] dels = new DoubleElement[gset.getNumMembers()];
            for (int i = 0; i < gset.getNumMembers(); i++) {
                int rank = rl.getRank(gset.getMember(i));
                dels[i] = new DoubleElement(i, rank); // index is position in the gset
            }

            dels = DoubleElement.sort(SortMode.REAL, Order.ASCENDING, dels);

            final List<String> rankedLabels = new ArrayList<String>();
            final Vector rankedScores = new Vector(dels.length);

            for (int i = 0; i < dels.length; i++) {
                String name = gset.getMember(dels[i].fIndex);
                rankedLabels.add(name);
                rankedScores.setElement(i, rl.getScore(name));
            }

            return new DefaultRankedList(gset.getName(true), rankedLabels, rankedScores);
        }

        public static List<String> getLabelsOfUpOrDnXRanks(int upOrDnX, boolean up, final RankedList rl) {
            List<String> list = new ArrayList<String>();
            if (upOrDnX > rl.getSize()) {
                upOrDnX = rl.getSize();
            }

            if (up) {
                for (int r = 0; r < upOrDnX; r++) {
                    list.add(rl.getRankName(r));
                }
            } else {
                int start = rl.getSize() - 1;
                for (int r = 0; r < upOrDnX; r++) {
                    list.add(rl.getRankName(start));
                    start--;
                }
            }

            return list;
        }

        public static RankedList extractRanked(final ScoreMode smode, final RankedList rl) {
            List<String> rankedNames = new ArrayList<String>();
            TFloatArrayList rankedScores = new TFloatArrayList();
            String suffix = smode.getName();

            for (int i = 0; i < rl.getSize(); i++) {
                float score = rl.getScore(i);
                if (smode.isPostiveOnly() && XMath.isPositive(score)) {
                    rankedNames.add(rl.getRankName(i));
                    rankedScores.add(score);
                } else if (smode.isNegativeOnly() && XMath.isNegative(score)) {
                    rankedNames.add(rl.getRankName(i));
                    rankedScores.add(score);
                } else {
                    // do nothing
                }
            }

            return new DefaultRankedList(rl.getName() + "_" + suffix, rankedNames, new Vector(rankedScores));
        }
    }
}