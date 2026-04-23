/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.reports;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.Color;
import java.util.regex.Pattern;

import edu.mit.broad.genome.charts.XChart;
import edu.mit.broad.genome.charts.XChartImpl;

/**
 * ROC (TPR vs FPR) for one gene set, compatible with the GenePattern ssGSEA.ROC PDF plots in spirit.
 */
public final class SsGseaRocChart {

    private SsGseaRocChart() {
    }

    public static XChart createChart(String geneSetName, String class0Label, String class1Label,
            SsGseaRocAnalysis.RocCurve curve, double auc, double wilcoxP) {
        String safe = toFileSafeName(geneSetName);
        final String title = geneSetName;
        String sub = "AUC=" + String.format("%.5f", auc) + "  (Wilcoxon p=" + formatP(wilcoxP) + ")\n"
                + class0Label + " vs. " + class1Label;
        if (sub.length() > 200) {
            sub = "AUC=" + String.format("%.5f", auc) + " | " + class0Label + " vs. " + class1Label;
        }
        final XYSeries s = new XYSeries("ROC", false, true);
        for (int i = 0; i < curve.fpr.length; i++) {
            s.add(curve.fpr[i], curve.tpr[i]);
        }
        final XYSeries diag = new XYSeries("x=y", false, true);
        diag.add(0, 0);
        diag.add(1, 1);
        final XYSeriesCollection c = new XYSeriesCollection();
        c.addSeries(s);
        c.addSeries(diag);
        final NumberAxis x = new NumberAxis("False positive rate (1 - specificity)");
        x.setRange(0, 1);
        final NumberAxis y = new NumberAxis("True positive rate (sensitivity)");
        y.setRange(0, 1);
        final XYLineAndShapeRenderer ren = new XYLineAndShapeRenderer();
        ren.setSeriesPaint(0, new Color(200, 32, 32));
        ren.setSeriesStroke(0, new java.awt.BasicStroke(1.5f));
        ren.setSeriesShapesVisible(0, false);
        ren.setSeriesPaint(1, Color.DARK_GRAY);
        ren.setSeriesStroke(1, new java.awt.BasicStroke(0.7f, java.awt.BasicStroke.CAP_ROUND,
                java.awt.BasicStroke.JOIN_BEVEL, 0, new float[]{3f, 3f}, 0f));
        ren.setSeriesShapesVisible(1, false);
        final XYPlot p = new XYPlot(c, x, y, ren);
        p.setDomainCrosshairVisible(false);
        p.setRangeCrosshairVisible(false);
        p.setBackgroundPaint(Color.WHITE);
        p.setOrientation(PlotOrientation.VERTICAL);
        return new XChartImpl("ssgsea_roc_" + safe, sub,
                new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, p, true));
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
