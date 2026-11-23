/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.alg.gsea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Single-sample GSEA enrichment score per sample and gene set, following the
 * ssGSEA GenePattern module ({@code Project.to.GeneSet} in ssGSEA.Library.R).
 */
public final class SsGseaProjection {

    private SsGseaProjection() {
    }

    /**
     * Per-sample normalization of a genes × samples matrix (rows = genes, columns = samples).
     * Types match R ssGSEA: none, rank, log, log.rank.
     */
    public static void applySampleNormalization(float[][] matrix, int nRows, int nCols, String normType) {
        if ("none".equals(normType)) {
            return;
        }
        if ("log".equals(normType)) {
            for (int c = 0; c < nCols; c++) {
                for (int r = 0; r < nRows; r++) {
                    float v = matrix[r][c];
                    if (v < 1f) {
                        v = 1f;
                    }
                    matrix[r][c] = (float) Math.log(v + Math.E);
                }
            }
        } else if ("rank".equals(normType) || "log.rank".equals(normType)) {
            float[] col = new float[nRows];
            int[] order = new int[nRows];
            for (int c = 0; c < nCols; c++) {
                for (int r = 0; r < nRows; r++) {
                    col[r] = matrix[r][c];
                    order[r] = r;
                }
                sortIndicesByDescendingValue(order, col);
                float[] ranks = averageRanks(col, order);
                for (int r = 0; r < nRows; r++) {
                    float scaled = 10000f * ranks[r] / (float) nRows;
                    if ("log.rank".equals(normType)) {
                        matrix[r][c] = (float) Math.log(scaled + Math.E);
                    } else {
                        matrix[r][c] = scaled;
                    }
                }
            }
        }
    }

    private static void sortIndicesByDescendingValue(int[] order, float[] values) {
        Integer[] boxed = new Integer[order.length];
        for (int i = 0; i < order.length; i++) {
            boxed[i] = i;
        }
        Arrays.sort(boxed, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return Float.compare(values[b], values[a]);
            }
        });
        for (int i = 0; i < order.length; i++) {
            order[i] = boxed[i];
        }
    }

    /**
     * Average-rank for ties (ties.method = "average" in R), 1-based ranks.
     */
    private static float[] averageRanks(float[] col, int[] orderDesc) {
        int n = col.length;
        float[] rank = new float[n];
        int i = 0;
        while (i < n) {
            int j = i;
            float v = col[orderDesc[i]];
            while (j + 1 < n && col[orderDesc[j + 1]] == v) {
                j++;
            }
            float avgRank = (i + j + 2) / 2f;
            for (int k = i; k <= j; k++) {
                rank[orderDesc[k]] = avgRank;
            }
            i = j + 1;
        }
        return rank;
    }

    /**
     * Enrichment score for one gene set and one sample column.
     *
     * @param matrix     genes × samples, rows aligned with {@code rowNames}
     * @param col        sample index
     * @param rowInSet   row indices present in both the gene set and the dataset
     * @param weight     exponent on |z-score| (0 matches unweighted ssGSEA)
     */
    public static float enrichmentScore(float[][] matrix, int col, Set<Integer> rowInSet, double weight) {
        int n = matrix.length;
        int nh = rowInSet.size();
        if (nh == 0) {
            return Float.NaN;
        }
        int nm = n - nh;
        if (nm <= 0) {
            return Float.NaN;
        }

        Integer[] geneList = new Integer[n];
        for (int i = 0; i < n; i++) {
            geneList[i] = i;
        }
        Arrays.sort(geneList, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return Float.compare(matrix[b][col], matrix[a][col]);
            }
        });

        float[] zByRank = new float[n];
        if (weight == 0d) {
            Arrays.fill(zByRank, 1f);
        } else {
            double sum = 0d;
            double sumSq = 0d;
            for (int i = 0; i < n; i++) {
                double x = matrix[geneList[i]][col];
                sum += x;
                sumSq += x * x;
            }
            double mean = sum / n;
            double var = sumSq / n - mean * mean;
            double sd = Math.sqrt(Math.max(var, 0d));
            if (sd <= 1e-12d) {
                sd = 1d;
            }
            for (int i = 0; i < n; i++) {
                zByRank[i] = (float) ((matrix[geneList[i]][col] - mean) / sd);
            }
        }

        List<Integer> hitRanks = new ArrayList<Integer>();
        for (int i = 0; i < n; i++) {
            if (rowInSet.contains(geneList[i])) {
                hitRanks.add(i);
            }
        }
        int nhHits = hitRanks.size();
        if (nhHits == 0) {
            return Float.NaN;
        }

        float[] weighted = new float[nhHits];
        float sumW = 0f;
        for (int i = 0; i < nhHits; i++) {
            int rk = hitRanks.get(i);
            float mag = Math.abs(zByRank[rk]);
            float w = weight == 0d ? 1f : (float) Math.pow(mag, weight);
            weighted[i] = w;
            sumW += w;
        }
        if (sumW <= 0f) {
            return Float.NaN;
        }

        int[] gaps = new int[nhHits + 1];
        gaps[0] = hitRanks.get(0);
        for (int i = 1; i < nhHits; i++) {
            gaps[i] = hitRanks.get(i) - hitRanks.get(i - 1) - 1;
        }
        gaps[nhHits] = (n - 1) - hitRanks.get(nhHits - 1);

        float[] up = new float[nhHits];
        for (int i = 0; i < nhHits; i++) {
            up[i] = weighted[i] / sumW;
        }

        float[] down = new float[nhHits + 1];
        for (int i = 0; i < nhHits + 1; i++) {
            down[i] = gaps[i] / (float) nm;
        }

        float[] inc = new float[nhHits + 1];
        for (int i = 0; i < nhHits; i++) {
            inc[i] = up[i] - down[i];
        }
        inc[nhHits] = up[nhHits - 1] - down[nhHits];

        float[] res = new float[nhHits + 1];
        float acc = 0f;
        for (int i = 0; i < nhHits + 1; i++) {
            acc += inc[i];
            res[i] = acc;
        }

        float[] valleys = new float[nhHits];
        for (int i = 0; i < nhHits; i++) {
            valleys[i] = res[i] - up[i];
        }

        for (int i = 0; i < nhHits + 1; i++) {
            gaps[i] += 1;
        }

        float es = 0f;
        for (int i = 0; i < nhHits + 1; i++) {
            float valleyOrZero = i < nhHits ? valleys[i] : 0f;
            float vecA = i == 0 ? 0f : res[i - 1];
            float vecB = i < nhHits ? valleys[i] : 0f;
            es += valleyOrZero * gaps[i] + 0.5f * (vecA - vecB) * gaps[i];
        }
        return es;
    }

    /**
     * Row indices of genes present in {@code geneNames} and in the overlap set (intersection).
     */
    public static Set<Integer> rowsForOverlap(String[] geneNames, List<String> overlap) {
        Set<Integer> set = new HashSet<Integer>(overlap.size());
        for (String g : overlap) {
            for (int r = 0; r < geneNames.length; r++) {
                if (g.equals(geneNames[r])) {
                    set.add(r);
                    break;
                }
            }
        }
        return set;
    }
}
