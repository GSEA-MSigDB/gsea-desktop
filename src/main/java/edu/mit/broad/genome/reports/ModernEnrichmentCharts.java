/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.reports;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.XYDataset;

import edu.mit.broad.genome.Printf;
import edu.mit.broad.genome.charts.XChart;
import edu.mit.broad.genome.charts.XChartImpl;
import edu.mit.broad.genome.charts.XChartUtils;
import edu.mit.broad.genome.charts.XComboDomainChart;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.math.XMath;
import edu.mit.broad.genome.models.XYDatasetProxy2;
import edu.mit.broad.genome.objects.MetricWeightStruc;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.reports.EnrichmentReports.EsProfileDataset;
import edu.mit.broad.genome.reports.EnrichmentReports.EsProfileDataset2;

/**
 * EnPlot v2 GSEA mountain / enrichment combo charts ({@code enplot2_*}).
 */
public final class ModernEnrichmentCharts {

    private static final Color ES_LINE = new Color(0x0D, 0x94, 0x88);
    private static final Color ES_FILL = new Color(0x0D, 0x94, 0x88, 70);
    private static final Color ZERO_LINE = new Color(0x94, 0xA3, 0xB8);
    private static final Color GRID = new Color(0xE2, 0xE8, 0xF0);
    private static final Color AXIS = new Color(0x64, 0x74, 0x8B);
    private static final Color HIT_TICK = new Color(0x94, 0xA3, 0xB8);
    private static final Color LE_TICK = new Color(0x0F, 0x76, 0x6E);
    private static final Color FRAME = Color.WHITE;
    private static final Color METRIC_LINE = new Color(0x94, 0xA3, 0xB8);
    private static final Color POS_LABEL = new Color(0xDC, 0x26, 0x26);
    private static final Color NEG_LABEL = new Color(0x25, 0x63, 0xEB);
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 10);

    private ModernEnrichmentCharts() {
    }

    public static EnrichmentCharts createComboChart(final String gsetName,
                                                    final Vector enrichmentScoreProfile,
                                                    final Vector esProfile_full_opt,
                                                    final Vector hitIndices,
                                                    final RankedList rl,
                                                    final String classAName_opt,
                                                    final String classBName_opt,
                                                    final IntervalMarker[] markers,
                                                    final float es,
                                                    final float nes,
                                                    final float fdr) {
        if (enrichmentScoreProfile == null) {
            throw new IllegalArgumentException("Param scoreProfile cannot be null");
        }
        if (hitIndices == null) {
            throw new IllegalArgumentException("Param hitProfile cannot be null");
        }
        if (rl == null) {
            throw new IllegalArgumentException("Param rl cannot be null");
        }

        XChart chart0 = createESProfileChart(enrichmentScoreProfile, esProfile_full_opt, hitIndices, es, nes, fdr);
        XChart chart1 = createHitTicksChart(hitIndices, enrichmentScoreProfile);
        XChart chart2 = createColorBarChart(hitIndices, markers);
        XChart chart3 = createRankedMetricChart(rl, classAName_opt, classBName_opt);

        styleSubplotChrome(chart0.getFreeChart().getXYPlot(), true);
        styleSubplotChrome(chart1.getFreeChart().getXYPlot(), false);
        styleSubplotChrome(chart2.getFreeChart().getXYPlot(), false);
        styleSubplotChrome(chart3.getFreeChart().getXYPlot(), true);

        XComboDomainChart combo = new XComboDomainChart(
                EnrichmentReports.ENPLOT2_ + gsetName,
                "Enrichment plot: " + gsetName,
                "Running enrichment score, gene-set hits, and ranked list metric",
                "Rank in Ordered Dataset",
                new XChart[]{chart0, chart1, chart2, chart3},
                new int[]{12, 3, 1, 7});
        combo.getCombinedXYPlot().setGap(2.0f);
        combo.getCombinedXYPlot().setBackgroundPaint(FRAME);
        combo.getCombinedXYPlot().setOutlineVisible(false);
        combo.getCombinedXYPlot().getDomainAxis().setTickLabelsVisible(true);
        combo.getCombinedXYPlot().getDomainAxis().setTickMarksVisible(true);
        combo.getCombinedXYPlot().getDomainAxis().setTickMarkStroke(new BasicStroke(0.75f));
        combo.getCombinedXYPlot().getDomainAxis().setTickMarkPaint(AXIS);
        combo.getCombinedXYPlot().getDomainAxis().setAxisLinePaint(AXIS);
        combo.getCombinedXYPlot().getDomainAxis().setLabelPaint(AXIS);
        combo.getCombinedXYPlot().getDomainAxis().setTickLabelPaint(AXIS);
        combo.getCombinedChart().getFreeChart().setBackgroundPaint(FRAME);
        combo.getCombinedChart().getFreeChart().setTitle(new TextTitle("Enrichment plot: " + gsetName, TITLE_FONT));
        if (combo.getCombinedChart().getFreeChart().getLegend() != null) {
            combo.getCombinedChart().getFreeChart().removeLegend();
        }
        return new EnrichmentCharts(chart0, chart1, chart2, chart3, combo);
    }

    static XChart createESProfileChart(final Vector esProfile,
                                       final Vector esProfile_full_opt,
                                       final Vector hitIndices,
                                       final float es,
                                       final float nes,
                                       final float fdr) {
        final boolean fullCurve = esProfile_full_opt != null;
        final JFreeChart chart;
        if (fullCurve) {
            XYDataset data = new EsProfileDataset2("Enrichment profile", esProfile_full_opt);
            chart = ChartFactory.createXYLineChart(null, null, "Enrichment score (ES)", data,
                    PlotOrientation.VERTICAL, false, false, false);
        } else {
            XYDataset data = new EsProfileDataset("Enrichment profile", esProfile, hitIndices);
            chart = ChartFactory.createXYLineChart(null, null, "Enrichment score (ES)", data,
                    PlotOrientation.VERTICAL, false, false, false);
        }

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(FRAME);
        plot.setDomainGridlinePaint(GRID);
        plot.setRangeGridlinePaint(GRID);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        plot.setAxisOffset(new RectangleInsets(2, 2, 2, 2));

        // Area fill only for dense point-by-point profiles; sparse hit curves look better as a line.
        if (fullCurve) {
            XYAreaRenderer area = new XYAreaRenderer(XYAreaRenderer.AREA);
            area.setSeriesPaint(0, ES_FILL);
            area.setSeriesOutlinePaint(0, ES_LINE);
            area.setSeriesOutlineStroke(0, new BasicStroke(2.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            area.setOutline(true);
            plot.setRenderer(area);
        } else {
            plot.getRenderer().setSeriesPaint(0, ES_LINE);
            plot.getRenderer().setSeriesStroke(0, new BasicStroke(2.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        }

        Marker midLine = new ValueMarker(0);
        midLine.setPaint(ZERO_LINE);
        midLine.setStroke(new BasicStroke(1.0f));
        plot.addRangeMarker(midLine);

        double peakX = findPeakRankFromHitVector(hitIndices, esProfile);
        float peakEs = esProfile.maxDevFrom0();
        Marker peak = new ValueMarker(peakX);
        peak.setPaint(LE_TICK);
        peak.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 4f,
                new float[]{4f, 3f}, 0f));
        plot.addDomainMarker(peak);

        // Stats stay on the peak rank line, but sit in the empty half opposite the ES peak
        // (positive peak → below zero; negative peak → above zero).
        String ann = "ES=" + Printf.format(es, 3)
                + "  NES=" + Printf.format(nes, 2)
                + "  FDR=" + Printf.format(fdr, 3)
                + "\npeak at rank " + Math.round(peakX);
        XYTextAnnotation label = new XYTextAnnotation(ann, peakX, 0.0d);
        label.setFont(LABEL_FONT);
        label.setPaint(new Color(0x0F, 0x17, 0x2A));
        if (XMath.isPositive(peakEs)) {
            label.setTextAnchor(TextAnchor.TOP_CENTER);
        } else {
            label.setTextAnchor(TextAnchor.BOTTOM_CENTER);
        }
        plot.addAnnotation(label);

        plot.getRangeAxis().setLabel("Enrichment score (ES)");
        plot.getRangeAxis().setLabelPaint(AXIS);
        plot.getRangeAxis().setTickLabelPaint(AXIS);
        plot.getDomainAxis().setVisible(false);

        chart.setBackgroundPaint(FRAME);
        return new XChartImpl("Enrichment profile", "Enrichment profile", chart);
    }

    /** Map max-dev hit in the ES-at-hits profile to its ranked-list index. */
    private static int findPeakRankFromHitVector(final Vector hitIndices, final Vector esProfile) {
        int peakHit = esProfile.maxDevFrom0Index();
        int found = 0;
        for (int r = 0; r < hitIndices.getSize(); r++) {
            if (hitIndices.getElement(r) == 1) {
                if (found == peakHit) {
                    return r;
                }
                found++;
            }
        }
        for (int r = 0; r < hitIndices.getSize(); r++) {
            if (hitIndices.getElement(r) == 1) {
                return r;
            }
        }
        return 0;
    }

    static XChart createHitTicksChart(final Vector hitProfile, final Vector esProfile) {
        int peakRank = findPeakRankFromHitVector(hitProfile, esProfile);
        boolean pos = XMath.isPositive(esProfile.maxDevFrom0());
        int n = hitProfile.getSize();
        Vector otherHits = new Vector(n);
        Vector leHits = new Vector(n);
        for (int r = 0; r < n; r++) {
            if (hitProfile.getElement(r) != 1) {
                continue;
            }
            boolean leading = (pos && r <= peakRank) || (!pos && r >= peakRank);
            if (leading) {
                leHits.setElement(r, 1.0f);
            } else {
                otherHits.setElement(r, 1.0f);
            }
        }

        XYPlot plot = XChartUtils.lineYHits("HIT_LOCATION", "Position in ranked list", "Hits", otherHits);
        plot.setDataset(1, new XYDatasetProxy2(leHits, "Leading edge"));
        StandardXYItemRenderer leRend = new StandardXYItemRenderer(StandardXYItemRenderer.DISCONTINUOUS_LINES);
        plot.setRenderer(1, leRend);

        plot.getDomainAxis().setVisible(false);
        plot.getRangeAxis().setTickLabelsVisible(false);
        plot.getRangeAxis().setTickMarksVisible(false);
        plot.getRangeAxis().setLabel("");
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setBackgroundPaint(FRAME);
        plot.getRenderer(0).setSeriesStroke(0, new BasicStroke(0.8f));
        plot.getRenderer(0).setSeriesPaint(0, HIT_TICK);
        plot.getRenderer(1).setSeriesStroke(0, new BasicStroke(1.6f));
        plot.getRenderer(1).setSeriesPaint(0, LE_TICK);

        JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        chart.setBackgroundPaint(FRAME);
        return new XChartImpl("hit_locations", "Gene-set member positions", chart);
    }

    static XChart createColorBarChart(final Vector hitProfile, final IntervalMarker[] markers) {
        XYPlot plot = XChartUtils.lineYHits("HIT_LOCATION", "Position in ranked list", "", hitProfile);
        plot.getDomainAxis().setVisible(false);
        plot.getRangeAxis().setTickLabelsVisible(false);
        plot.getRangeAxis().setTickMarksVisible(false);
        plot.getRangeAxis().setLabel("");
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setBackgroundPaint(FRAME);
        plot.getRenderer().setSeriesStroke(0, new BasicStroke(0.0f));
        plot.getRenderer().setSeriesPaint(0, new Color(0, 0, 0, 0));
        // Clone markers so classic and v2 plots do not share IntervalMarker instances.
        if (markers != null) {
            for (IntervalMarker marker : markers) {
                IntervalMarker copy = new IntervalMarker(marker.getStartValue(), marker.getEndValue());
                copy.setPaint(marker.getPaint());
                copy.setAlpha(1.0f);
                copy.setOutlineStroke(new BasicStroke(0.0f));
                copy.setOutlinePaint(new Color(0, 0, 0, 0));
                plot.addDomainMarker(0, copy, Layer.BACKGROUND);
            }
        }
        JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        chart.setBackgroundPaint(FRAME);
        return new XChartImpl("hit_colorbar", "Phenotype color bar", chart);
    }

    static XChart createRankedMetricChart(final RankedList rl,
                                          final String classAName_opt,
                                          final String classBName_opt) {
        String axisTitle = "Ranked list metric";
        MetricWeightStruc mws = rl.getMetricWeightStruc();
        if (mws != null && mws.getMetricName() != null) {
            axisTitle += " (" + mws.getMetricName() + ")";
        }

        NumberAxis xAxis = new NumberAxis("Gene list location");
        xAxis.setAutoRangeIncludesZero(false);
        NumberAxis yAxis = new NumberAxis(axisTitle);
        yAxis.setTickMarksVisible(false);
        yAxis.setTickLabelsVisible(true);
        yAxis.setLabelPaint(AXIS);
        yAxis.setTickLabelPaint(AXIS);

        Vector scores = Vector.infinityAdjustRankedScoreVector(rl.getScoresV(false));
        XYDataset data = new XYDatasetProxy2(scores, "Ranking metric scores");
        StandardXYItemRenderer rend = new StandardXYItemRenderer(StandardXYItemRenderer.DISCONTINUOUS_LINES);
        XYPlot plot = new XYPlot(data, xAxis, yAxis, rend);
        plot.getRenderer().setSeriesPaint(0, METRIC_LINE);
        plot.getRenderer().setSeriesStroke(0, new BasicStroke(1.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        plot.setBackgroundPaint(FRAME);
        plot.setDomainGridlinePaint(GRID);
        plot.setRangeGridlinePaint(GRID);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(true);

        if (classAName_opt != null && classAName_opt.length() > 0) {
            float max = scores.max();
            IntervalMarker target = new IntervalMarker(max, max);
            target.setLabel("'" + classAName_opt + "' (pos)");
            target.setLabelFont(LABEL_FONT);
            target.setLabelAnchor(RectangleAnchor.LEFT);
            target.setLabelTextAnchor(TextAnchor.CENTER_LEFT);
            target.setLabelPaint(POS_LABEL);
            target.setLabelBackgroundColor(FRAME);
            target.setOutlineStroke(new BasicStroke(0.0f));
            target.setOutlinePaint(new Color(0, 0, 0, 0));
            plot.addRangeMarker(target);
        }
        if (classBName_opt != null && classBName_opt.length() > 0) {
            float min = scores.min();
            IntervalMarker target = new IntervalMarker(min, min);
            target.setLabel("'" + classBName_opt + "' (neg)");
            target.setLabelFont(LABEL_FONT);
            target.setLabelAnchor(RectangleAnchor.BOTTOM);
            target.setLabelTextAnchor(TextAnchor.CENTER_LEFT);
            target.setLabelPaint(NEG_LABEL);
            target.setLabelBackgroundColor(FRAME);
            target.setOutlineStroke(new BasicStroke(0.0f));
            target.setOutlinePaint(new Color(0, 0, 0, 0));
            plot.addRangeMarker(target);
        }

        if (mws != null) {
            Marker midLine = new ValueMarker(mws.getTotalPosLength());
            midLine.setPaint(AXIS);
            midLine.setStroke(new BasicStroke(0.75f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 3,
                    new float[]{4, 3}, 0));
            midLine.setLabel("Zero cross at " + mws.getTotalPosLength());
            midLine.setLabelFont(LABEL_FONT);
            midLine.setLabelBackgroundColor(FRAME);
            midLine.setLabelAnchor(RectangleAnchor.CENTER);
            plot.addDomainMarker(midLine);
        }

        JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        chart.setBackgroundPaint(FRAME);
        return new XChartImpl("ranked_list_corr", "Ranked list metric", chart);
    }

    private static void styleSubplotChrome(final XYPlot plot, final boolean showRangeAxisLine) {
        plot.setAxisOffset(new RectangleInsets(0, 0, 0, 0));
        plot.setOutlineVisible(false);
        if (plot.getRangeAxis() != null) {
            plot.getRangeAxis().setAxisLinePaint(AXIS);
            plot.getRangeAxis().setAxisLineStroke(new BasicStroke(0.75f));
            plot.getRangeAxis().setAxisLineVisible(showRangeAxisLine);
            plot.getRangeAxis().setTickMarkPaint(AXIS);
        }
    }
}
