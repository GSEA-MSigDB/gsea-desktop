/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.reports;

import java.util.regex.Pattern;

import edu.mit.broad.genome.charts.XChart;
import edu.mit.broad.genome.plots.PlotChart;
import edu.mit.broad.genome.plots.XyPlotSpec;

/**
 * ROC (TPR vs FPR) for one gene set.
 */
public final class SsGseaRocChart {

    private SsGseaRocChart() {
    }

    public static XChart createChart(String geneSetName, String class0Label, String class1Label,
            SsGseaRocAnalysis.RocCurve curve, double auc, double wilcoxP) {
        String safe = toFileSafeName(geneSetName);
        String sub = "AUC=" + String.format("%.5f", auc) + "  (Wilcoxon p=" + formatP(wilcoxP) + ") "
                + class0Label + " vs. " + class1Label;
        double[] fpr = curve.fpr;
        double[] tpr = curve.tpr;
        double[] diagX = { 0, 1 };
        double[] diagY = { 0, 1 };
        return new PlotChart(XyPlotSpec.builder("ssgsea_roc_" + safe, geneSetName)
                .caption(sub)
                .xLabel("False positive rate (1 - specificity)")
                .yLabel("True positive rate (sensitivity)")
                .legend(true)
                .addSeries("ROC", fpr, tpr, true, false, 0xFFC82020)
                .addSeries("x=y", diagX, diagY, true, false, 0xFF404040)
                .build());
    }

    private static String formatP(double p) {
        if (Double.isNaN(p) || p < 0) {
            return "n/a";
        }
        if (p < 1e-6) {
            return String.format("%.2e", p);
        }
        return String.format("%.5f", p);
    }

    public static String toFileSafeName(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "set";
        }
        String t = raw.replaceAll("[\\\\/:*?\"<>|]", "_");
        t = Pattern.compile("\\s+").matcher(t).replaceAll("_");
        if (t.length() > 100) {
            t = t.substring(0, 100);
        }
        return t;
    }
}
