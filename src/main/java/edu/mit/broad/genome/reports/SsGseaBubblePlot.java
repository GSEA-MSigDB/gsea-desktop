/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.reports;

import edu.mit.broad.genome.charts.XChart;
import edu.mit.broad.genome.charts.XChartImpl;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Per-sample bubble plot for ssGSEA scores: Y = gene sets (top by |score|), X = signed score,
 * bubble area ~ |score|, color = red (positive) / blue (negative) by magnitude.
 */
public final class SsGseaBubblePlot {

    private static final int MAX_DEFAULT_TOP = 200;

    private SsGseaBubblePlot() {
    }

    /**
     * @param sampleLabel     displayed in chart title
     * @param geneSetNames    parallel to {@code scores}
     * @param scores          ssGSEA enrichment scores for one sample
     * @param topN            max sets to show; if &lt;= 0, shows up to {@link #MAX_DEFAULT_TOP} finite sets
     */
    public static XChart createPerSampleChart(final String sampleLabel, final List<String> geneSetNames,
                                              final float[] scores, final int topN) {
        if (geneSetNames == null || scores == null || geneSetNames.size() != scores.length) {
            return emptyChart(sampleLabel, "ssGSEA bubble plot (invalid input)");
        }

        final List<Integer> finite = new ArrayList<Integer>();
        for (int i = 0; i < scores.length; i++) {
            if (Float.isFinite(scores[i])) {
                finite.add(i);
            }
        }
        if (finite.isEmpty()) {
            return emptyChart(sampleLabel, "ssGSEA bubble plot (no finite scores)");
        }

        Collections.sort(finite, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return Float.compare(Math.abs(scores[b]), Math.abs(scores[a]));
            }
        });

        final int cap = topN > 0 ? topN : MAX_DEFAULT_TOP;
        final int n = Math.min(cap, finite.size());

        final String[] labels = new String[n];
        final double[] xs = new double[n];
        final double[] ys = new double[n];
        final double[] sizePx = new double[n];
        final Paint[] paints = new Paint[n];

        double maxAbs = 1e-12d;
        int maxLabelLen = 8;
        for (int i = 0; i < n; i++) {
            int ix = finite.get(i);
            maxAbs = Math.max(maxAbs, Math.abs(scores[ix]));
            String lab = geneSetNames.get(ix);
            if (lab != null) {
                maxLabelLen = Math.max(maxLabelLen, lab.length());
            }
        }

        for (int i = 0; i < n; i++) {
            int ix = finite.get(i);
            final float s = scores[ix];
            labels[(n - 1) - i] = geneSetNames.get(ix);
            xs[i] = s;
            ys[i] = (n - 1) - i;
            final double t = Math.min(1.0d, Math.abs(s) / maxAbs);
            sizePx[i] = 8.0d + 14.0d * t;
            paints[i] = paintForSignedScore(s, t);
        }

        final XYSeries series = new XYSeries("ssgsea-bubbles", false, true);
        for (int i = 0; i < n; i++) {
            series.add(xs[i], ys[i]);
        }
        final XYSeriesCollection ds = new XYSeriesCollection(series);

        final BubbleRendererData dataRef = new BubbleRendererData(sizePx, paints);
        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true) {
            @Override
            public Paint getItemPaint(int series, int item) {
                return dataRef.paints[item];
            }

            @Override
            public Shape getItemShape(int series, int item) {
                final double d = dataRef.sizePx[item];
                return new Ellipse2D.Double(-d / 2.0d, -d / 2.0d, d, d);
            }

            @Override
            public void drawItem(Graphics2D g2, org.jfree.chart.renderer.xy.XYItemRendererState state,
                                 Rectangle2D dataArea, org.jfree.chart.plot.PlotRenderingInfo info, XYPlot plot,
                                 org.jfree.chart.axis.ValueAxis domainAxis, org.jfree.chart.axis.ValueAxis rangeAxis,
                                 org.jfree.data.xy.XYDataset dataset, int series, int item,
                                 org.jfree.chart.plot.CrosshairState crosshairState, int pass) {
                final Shape oldClip = g2.getClip();
                try {
                    g2.setClip(null);
                    super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item,
                            crosshairState, pass);
                } finally {
                    g2.setClip(oldClip);
                }
            }
        };
        renderer.setUseOutlinePaint(true);
        renderer.setSeriesOutlinePaint(0, new Color(0x55, 0x55, 0x55));
        renderer.setSeriesOutlineStroke(0, new BasicStroke(1.0f));
        renderer.setDefaultItemLabelGenerator(new XYItemLabelGenerator() {
            @Override
            public String generateLabel(org.jfree.data.xy.XYDataset dataset, int series, int item) {
                return null;
            }
        });
        renderer.setDefaultItemLabelsVisible(false);

        final NumberAxis xAxis = new NumberAxis("ssGSEA enrichment score");
        xAxis.setAutoRangeIncludesZero(true);
        final double pad = Math.max(maxAbs * 0.08d, 1e-6d);
        xAxis.setRange(-maxAbs - pad, maxAbs + pad);

        final SymbolAxis yAxis = new SymbolAxis("", labels);
        yAxis.setGridBandsVisible(false);
        yAxis.setAutoRangeIncludesZero(false);
        yAxis.setVerticalTickLabels(false);
        yAxis.setLowerMargin(0.01d);
        yAxis.setUpperMargin(0.24d);

        final XYPlot plot = new XYPlot(ds, xAxis, yAxis, renderer);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        plot.setAxisOffset(new RectangleInsets(0, 0, 0, 0));

        plot.addDomainMarker(new ValueMarker(0.0d, Color.DARK_GRAY, new BasicStroke(1.2f)));

        final double leftSpace = Math.min(760.0d, Math.max(260.0d, (maxLabelLen * 8.2d) + 70.0d));
        final AxisSpace fixedAxisSpace = new AxisSpace();
        fixedAxisSpace.setLeft(leftSpace);
        plot.setFixedRangeAxisSpace(fixedAxisSpace);

        final String title = "ssGSEA bubble plot: " + (sampleLabel != null ? sampleLabel : "");
        final JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        chart.setBackgroundPaint(EnrichmentReports.CHART_FRAME_COLOR);

        final String safeName = fileSafeName(sampleLabel);
        return new XChartImpl("ssgsea_bubble_" + safeName,
                "ssGSEA per-sample bubble plot: X = score, size = |score|, red = positive, blue = negative",
                chart);
    }

    private static final class BubbleRendererData {
        final double[] sizePx;
        final Paint[] paints;

        BubbleRendererData(double[] sizePx, Paint[] paints) {
            this.sizePx = sizePx;
            this.paints = paints;
        }
    }

    private static Paint paintForSignedScore(float s, double tNorm) {
        if (s >= 0f) {
            final int c = (int) (220 - 175 * tNorm);
            return new Color(255, Math.max(40, c), Math.max(40, c));
        }
        final int c = (int) (220 - 175 * tNorm);
        return new Color(Math.max(40, c), Math.max(40, c), 255);
    }

    private static String fileSafeName(String sampleLabel) {
        if (sampleLabel == null || sampleLabel.isEmpty()) {
            return "sample";
        }
        final StringBuilder b = new StringBuilder();
        for (int i = 0; i < sampleLabel.length(); i++) {
            char ch = sampleLabel.charAt(i);
            if (Character.isLetterOrDigit(ch) || ch == '.' || ch == '-' || ch == '_') {
                b.append(ch);
            } else {
                b.append('_');
            }
        }
        String s = b.toString();
        if (s.length() > 120) {
            s = s.substring(0, 120);
        }
        return s.length() > 0 ? s : "sample";
    }

    private static XChart emptyChart(final String sampleLabel, final String caption) {
        final JFreeChart emptyChart = org.jfree.chart.ChartFactory.createScatterPlot(
                caption, "ssGSEA enrichment score", "Gene set",
                new XYSeriesCollection(), PlotOrientation.VERTICAL, false, false, false);
        emptyChart.setBackgroundPaint(EnrichmentReports.CHART_FRAME_COLOR);
        return new XChartImpl("ssgsea_bubble_" + fileSafeName(sampleLabel), caption, emptyChart);
    }
}
