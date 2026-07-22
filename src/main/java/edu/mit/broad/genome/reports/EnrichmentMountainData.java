/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.reports;

import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.math.XMath;
import edu.mit.broad.genome.objects.MetricWeightStruc;
import edu.mit.broad.genome.objects.RankedList;

/**
 * Shared peak / leading-edge / ES-curve / metric wiring for classic+v2 static mountains
 * and interactive EnPlot JSON payloads.
 */
final class EnrichmentMountainData {

    final int listSize;
    final int[] hitRanks;
    final boolean[] leadingEdge;
    /** Dense or sparse ES x ranks (same length as {@link #esY}). */
    final double[] esX;
    final double[] esY;
    final double[] metricY;
    final String metricName;
    final int peakRank;
    final float peakEs;
    final int zeroCross;

    private EnrichmentMountainData(int listSize, int[] hitRanks, boolean[] leadingEdge,
            double[] esX, double[] esY, double[] metricY, String metricName,
            int peakRank, float peakEs, int zeroCross) {
        this.listSize = listSize;
        this.hitRanks = hitRanks;
        this.leadingEdge = leadingEdge;
        this.esX = esX;
        this.esY = esY;
        this.metricY = metricY;
        this.metricName = metricName;
        this.peakRank = peakRank;
        this.peakEs = peakEs;
        this.zeroCross = zeroCross;
    }

    /**
     * @param esAtHits sparse running ES at each hit (same length as {@code hitRanks})
     * @param esFullOpt optional dense rank-by-rank ES; preferred when present
     * @param hitRanks gene-set member ranks in the ordered list
     */
    static EnrichmentMountainData build(Vector esAtHits, Vector esFullOpt, int[] hitRanks, RankedList rl) {
        if (esAtHits == null) {
            throw new IllegalArgumentException("esAtHits cannot be null");
        }
        if (rl == null) {
            throw new IllegalArgumentException("rl cannot be null");
        }
        int[] hits = hitRanks != null ? hitRanks.clone() : new int[0];
        int listSize = rl.getSize();

        double[] esX;
        double[] esY;
        if (esFullOpt != null && esFullOpt.getSize() > 0) {
            esY = esFullOpt.toArrayDouble();
            esX = new double[esY.length];
            for (int i = 0; i < esX.length; i++) {
                esX[i] = i;
            }
        } else {
            int n = esAtHits.getSize();
            esX = new double[n + 2];
            esY = new double[n + 2];
            esX[0] = 0;
            esY[0] = 0;
            for (int i = 0; i < n; i++) {
                esX[i + 1] = i < hits.length ? hits[i] : i;
                esY[i + 1] = esAtHits.getElement(i);
            }
            esX[n + 1] = Math.max(0, listSize - 1);
            esY[n + 1] = 0;
        }

        int peakHit = esAtHits.maxDevFrom0Index();
        float peakEs = esAtHits.maxDevFrom0();
        int peakRank = (peakHit >= 0 && peakHit < hits.length) ? hits[peakHit] : -1;
        boolean posPeak = XMath.isPositive(peakEs);
        boolean[] leading = new boolean[hits.length];
        for (int i = 0; i < hits.length; i++) {
            leading[i] = peakRank >= 0
                    && ((posPeak && hits[i] <= peakRank) || (!posPeak && hits[i] >= peakRank));
        }

        Vector metricScores = Vector.infinityAdjustRankedScoreVector(rl.getScoresV(false));
        double[] metric = metricScores.toArrayDouble();
        MetricWeightStruc mws = rl.getMetricWeightStruc();
        String metricName = mws != null && mws.getMetricName() != null ? mws.getMetricName() : "";
        int zeroCross = mws != null ? mws.getTotalPosLength() : -1;

        return new EnrichmentMountainData(listSize, hits, leading, esX, esY, metric, metricName,
                peakRank, peakEs, zeroCross);
    }
}
