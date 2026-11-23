/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.reports;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Per-gene set ROC-style metrics for ssGSEA scores vs. a binary (2-class) phenotype, aligned in spirit
 * with the GenePattern <a href="https://github.com/genepattern/ssGSEA.ROC">ssGSEA.ROC</a> module (AUC, MCC,
 * Youden cutpoint, Wilcoxon p-value, etc.).
 */
public final class SsGseaRocAnalysis {

    private SsGseaRocAnalysis() {
    }

    /**
     * One row of results for a single gene set (ssGSEA scores for all samples, parallel to {@code y}).
     * {@code y[i]} = 0 or 1; higher score supports label 1 (as in typical ROC / ROCR).
     */
    public static final class ResultRow {
        public final String geneSetName;
        public final String description;
        public final double auc;
        public final double mcc; // at threshold with max |MCC| (R's ROCR "mat" style)
        public final double youdenCutoff;
        public final double sensitivity;
        public final double specificity;
        public final double ppv;
        public final double npv;
        public final double wilcoxP;

        public ResultRow(String geneSetName, String description, double auc, double mcc, double youdenCutoff,
                         double sensitivity, double specificity, double ppv, double npv, double wilcoxP) {
            this.geneSetName = geneSetName;
            this.description = description;
            this.auc = auc;
            this.mcc = mcc;
            this.youdenCutoff = youdenCutoff;
            this.sensitivity = sensitivity;
            this.specificity = specificity;
            this.ppv = ppv;
            this.npv = npv;
            this.wilcoxP = wilcoxP;
        }
    }

    public static final class RocCurve {
        public final double[] tpr;
        public final double[] fpr;

        public RocCurve(double[] tpr, double[] fpr) {
            this.tpr = tpr;
            this.fpr = fpr;
        }
    }

    public static List<ResultRow> computeAll(
            final List<? extends CharSequence> geneSetNames,
            final List<? extends CharSequence> descriptions,
            final float[][] scores, // [geneSetIndex][sample]
            final int[] y) {

        if (geneSetNames == null || scores == null || y == null) {
            throw new IllegalArgumentException("geneSetNames, scores, and y must be non-null");
        }
        final int nG = scores.length;
        if (nG == 0) {
            return new ArrayList<ResultRow>();
        }
        final int nc = y.length;
        for (int r = 0; r < nG; r++) {
            if (scores[r] == null || scores[r].length != nc) {
                throw new IllegalArgumentException("Row " + r + " score length does not match y length " + nc);
            }
        }
        for (int i = 0; i < nc; i++) {
            if (y[i] != 0 && y[i] != 1) {
                throw new IllegalArgumentException("y must be 0/1; got y[" + i + "]=" + y[i]);
            }
        }

        final int n0 = count(y, 0);
        final int n1 = count(y, 1);
        if (n0 < 1 || n1 < 1) {
            throw new IllegalArgumentException("Two non-empty classes are required for ROC (n0=" + n0 + ", n1=" + n1 + ").");
        }

        final List<ResultRow> out = new ArrayList<ResultRow>(nG);
        for (int r = 0; r < nG; r++) {
            final double[] ds = toDouble(scores[r]);
            final String d = (descriptions != null && r < descriptions.size() && descriptions.get(r) != null)
                    ? descriptions.get(r).toString() : "";
            out.add(computeOne(geneSetNames.get(r).toString(), d, ds, y));
        }
        out.sort(Comparator.comparingDouble((ResultRow a) -> Math.abs(a.mcc)).reversed());
        return out;
    }

    private static ResultRow computeOne(String name, String desc, final double[] scores, final int[] y) {
        final int n0b = count(y, 0);
        final int n1b = count(y, 1);
        final double[] g0 = new double[n0b];
        final double[] g1 = new double[n1b];
        int a = 0, b = 0;
        for (int i = 0; i < y.length; i++) {
            if (y[i] == 0) {
                g0[a++] = scores[i];
            } else {
                g1[b++] = scores[i];
            }
        }
        final double wilcoxP = wilcoxonRankSumTwoSidedP(g0, g1);
        final double auc = mannWhitneyAucFromBinaryLabels(scores, y, n0b, n1b);
        final BestMcc bm = bestMccAndAtYouden(scores, y, n0b, n1b);
        return new ResultRow(name, desc, auc, bm.mcc, bm.cut, bm.sens, bm.spec, bm.ppv, bm.npv, wilcoxP);
    }

    public static RocCurve buildRocCurve(final double[] scores, final int[] y) {
        final int n = scores.length;
        final int n0 = count(y, 0);
        final int n1 = count(y, 1);
        if (n0 < 1 || n1 < 1) {
            return new RocCurve(new double[]{0, 1}, new double[]{0, 1});
        }
        final Integer[] ord = new Integer[n];
        for (int i = 0; i < n; i++) {
            ord[i] = i;
        }
        Arrays.sort(ord, (i, j) -> Double.compare(scores[j], scores[i]));

        final List<Double> fprList = new ArrayList<Double>();
        final List<Double> tprList = new ArrayList<Double>();
        fprList.add(0.0);
        tprList.add(0.0);

        int tp = 0, fp = 0;
        int i = 0;
        while (i < n) {
            final double s = scores[ord[i]];
            int j = i;
            while (j < n && Double.compare(scores[ord[j]], s) == 0) {
                j++;
            }
            for (int k = i; k < j; k++) {
                if (y[ord[k]] == 1) {
                    tp++;
                } else {
                    fp++;
                }
            }
            fprList.add((double) fp / n0);
            tprList.add((double) tp / n1);
            i = j;
        }
        if (fprList.get(fprList.size() - 1) < 1.0 - 1e-9 || tprList.get(tprList.size() - 1) < 1.0 - 1e-9) {
            fprList.add(1.0);
            tprList.add(1.0);
        }

        final int m = fprList.size();
        final double[] fpr = new double[m];
        final double[] tpr = new double[m];
        for (int k = 0; k < m; k++) {
            fpr[k] = fprList.get(k);
            tpr[k] = tprList.get(k);
        }
        return new RocCurve(tpr, fpr);
    }

    private static final class BestMcc {
        final double mcc;
        final double cut;
        final double sens, spec, ppv, npv;

        BestMcc(double mcc, double cut, double sens, double spec, double ppv, double npv) {
            this.mcc = mcc;
            this.cut = cut;
            this.sens = sens;
            this.spec = spec;
            this.ppv = ppv;
            this.npv = npv;
        }
    }

    /**
     * MCC at the threshold with maximum |MCC| (R: performance(pred, "mat")).
     * Youden point: cut that maximizes sensitivity + specificity (ssGSEA_ROC.R).
     */
    private static BestMcc bestMccAndAtYouden(final double[] scores, final int[] y, int n0, int n1) {
        final int n = scores.length;
        final double[] u = Arrays.copyOf(scores, n);
        Arrays.sort(u);
        final List<Double> cutList = new ArrayList<Double>();
        for (int i = 0; i < n; i++) {
            if (i == 0 || u[i] != u[i - 1]) {
                cutList.add(u[i]);
            }
        }
        for (int i = 0; i + 1 < u.length; i++) {
            if (u[i] != u[i + 1]) {
                cutList.add(0.5 * (u[i] + u[i + 1]));
            }
        }
        cutList.add(Double.NEGATIVE_INFINITY);
        cutList.add(Double.POSITIVE_INFINITY);

        double bestAbsM = -1.0;
        double bestM = Double.NaN;
        for (double cut2 : cutList) {
            final int[] cm = confusionAtCut(scores, y, n, cut2);
            final int tp = cm[0], fp = cm[1], tn = cm[2], fn = cm[3];
            final double m = mccFrom(tp, fp, tn, fn);
            if (Double.isFinite(m) && Math.abs(m) > bestAbsM) {
                bestAbsM = Math.abs(m);
                bestM = m;
            }
        }
        if (!Double.isFinite(bestM)) {
            bestM = 0.0;
        }

        double bestSum = -1.0;
        double yCut = Double.NaN, ySens = Double.NaN, ySpec = Double.NaN, yPpv = Double.NaN, yNpv = Double.NaN;
        for (double cut2 : cutList) {
            if (Double.isNaN(cut2)) {
                continue;
            }
            final int[] cm = confusionAtCut(scores, y, n, cut2);
            final int tp = cm[0], fp = cm[1], tn = cm[2], fn = cm[3];
            final double sens = n1 == 0 ? 0.0 : (double) tp / n1;
            final double spec = n0 == 0 ? 0.0 : (double) tn / n0;
            if (sens + spec > bestSum) {
                bestSum = sens + spec;
                yCut = cut2;
                ySens = sens;
                ySpec = spec;
                yPpv = (tp + fp) == 0 ? Double.NaN : (double) tp / (tp + fp);
                yNpv = (tn + fn) == 0 ? Double.NaN : (double) tn / (tn + fn);
            }
        }
        if (Double.isNaN(yCut) && !cutList.isEmpty()) {
            yCut = cutList.get(0);
        }
        return new BestMcc(bestM, yCut, ySens, ySpec, yPpv, yNpv);
    }

    private static int[] confusionAtCut(final double[] scores, final int[] y, int n, double cut2) {
        int tp = 0, fp = 0, tn = 0, fn = 0;
        for (int k = 0; k < n; k++) {
            int pred1;
            if (cut2 == Double.NEGATIVE_INFINITY) {
                pred1 = 1;
            } else if (cut2 == Double.POSITIVE_INFINITY) {
                pred1 = 0;
            } else {
                pred1 = scores[k] >= cut2 ? 1 : 0;
            }
            if (y[k] == 1 && pred1 == 1) {
                tp++;
            } else if (y[k] == 0 && pred1 == 1) {
                fp++;
            } else if (y[k] == 0 && pred1 == 0) {
                tn++;
            } else {
                fn++;
            }
        }
        return new int[]{tp, fp, tn, fn};
    }

    private static double mccFrom(int tp, int fp, int tn, int fn) {
        long num = (long) tp * tn - (long) fp * fn;
        final double den = (tp + fp) * (double) (tp + fn) * (tn + fp) * (tn + fn);
        if (den <= 0) {
            return 0.0;
        }
        return num / Math.sqrt(den);
    }

    /** AUC = Mann–Whitney U: sum of 1-based ranks in class 1, normalized. */
    private static double mannWhitneyAucFromBinaryLabels(final double[] scores, final int[] y, int n0, int n1) {
        final int n = scores.length;
        final Integer[] o = new Integer[n];
        for (int i = 0; i < n; i++) {
            o[i] = i;
        }
        Arrays.sort(o, (a, b) -> {
            int c = Double.compare(scores[a], scores[b]);
            return c != 0 ? c : Integer.compare(a, b);
        });
        final double[] rank1based = new double[n];
        int s = 0;
        while (s < n) {
            int e = s + 1;
            while (e < n && Double.compare(scores[o[s]], scores[o[e]]) == 0) {
                e++;
            }
            double sum = 0.0;
            for (int k = s; k < e; k++) {
                sum += (k + 1);
            }
            final double ravg = sum / (e - s);
            for (int k = s; k < e; k++) {
                rank1based[o[k]] = ravg;
            }
            s = e;
        }
        double rPos = 0.0;
        for (int t = 0; t < n; t++) {
            if (y[t] == 1) {
                rPos += rank1based[t];
            }
        }
        return (rPos - n1 * (n1 + 1) / 2.0) / (n0 * n1);
    }

    private static int count(int[] y, int v) {
        int c = 0;
        for (int t : y) {
            if (t == v) {
                c++;
            }
        }
        return c;
    }

    private static double[] toDouble(float[] a) {
        final double[] d = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            d[i] = a[i];
        }
        return d;
    }

    /** Two-sided Wilcoxon rank-sum, normal approx with tie correction. */
    private static double wilcoxonRankSumTwoSidedP(final double[] g0, final double[] g1) {
        final int n0c = g0.length;
        final int n1c = g1.length;
        if (n0c == 0 || n1c == 0) {
            return Double.NaN;
        }
        final int n = n0c + n1c;
        final double[] v = new double[n];
        final int[] group = new int[n];
        int p = 0;
        for (int i = 0; i < n0c; i++) {
            v[p] = g0[i];
            group[p++] = 0;
        }
        for (int i = 0; i < n1c; i++) {
            v[p] = g1[i];
            group[p++] = 1;
        }
        final Integer[] o = new Integer[n];
        for (int i = 0; i < n; i++) {
            o[i] = i;
        }
        Arrays.sort(o, (a, b) -> {
            int c = Double.compare(v[a], v[b]);
            return c != 0 ? c : Integer.compare(a, b);
        });
        final double[] r = new double[n];
        int q = 0;
        while (q < n) {
            int e = q + 1;
            while (e < n && v[o[q]] == v[o[e]]) {
                e++;
            }
            double sumR = 0.0;
            for (int k = q; k < e; k++) {
                sumR += (k + 1);
            }
            final double ravg = sumR / (e - q);
            for (int k = q; k < e; k++) {
                r[o[k]] = ravg;
            }
            q = e;
        }
        // W1 = sum of ranks in class 0 (1-based, same as for class 1 in R wilcox for first group)
        double w0 = 0.0;
        for (int i = 0; i < n; i++) {
            if (group[i] == 0) {
                w0 += r[i];
            }
        }
        final double ew0 = n0c * (n + 1) / 2.0;
        long tcorr = 0L;
        q = 0;
        while (q < n) {
            int e = q + 1;
            while (e < n && v[o[q]] == v[o[e]]) {
                e++;
            }
            int d = e - q;
            tcorr += (long) d * d * d - d;
            q = e;
        }
        final double varW0 = n0c * n1c / 12.0
                * ((n + 1) - tcorr / (n * (n - 1.0)));
        if (varW0 <= 0) {
            return 1.0;
        }
        final double z0 = (w0 - ew0) / Math.sqrt(varW0);
        return 2.0 * (1.0 - normalCdf(Math.abs(z0)));
    }

    private static double normalCdf(double x) {
        return 0.5 * (1.0 + erf(x / Math.sqrt(2.0)));
    }

    private static double erf(double x) {
        final double t = 1.0 / (1.0 + 0.3275911 * Math.abs(x));
        final double a1 = 0.254829592, a2 = -0.284496736, a3 = 1.421413741, a4 = -1.453152027, a5 = 1.061405429;
        final double p = 1.0 - (((((a5 * t + a4) * t + a3) * t + a2) * t + a1) * t * Math.exp(-x * x));
        return x >= 0 ? p : -p;
    }
}
