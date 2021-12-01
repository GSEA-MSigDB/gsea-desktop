/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.reports;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.XYDataset;

import edu.mit.broad.genome.Printf;
import edu.mit.broad.genome.alg.DatasetGenerators;
import edu.mit.broad.genome.alg.distrib.RangeFactory;
import edu.mit.broad.genome.charts.XChart;
import edu.mit.broad.genome.charts.XChartImpl;
import edu.mit.broad.genome.math.ColorSchemes;
import edu.mit.broad.genome.math.ColorSchemes.ColorScheme;
import edu.mit.broad.genome.math.Range;
import edu.mit.broad.genome.math.ScoreMode;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.models.XYDatasetProxy2;
import edu.mit.broad.genome.objects.ColorDataset;
import edu.mit.broad.genome.objects.MetricWeightStruc;
import edu.mit.broad.genome.objects.RankedList;

/**
 * @author Aravind Subramanian, David Eby
 */
public class RankedListCharts {
    private static ColorScheme RED = new ColorSchemes.BroadCancerRed();
    private static ColorScheme BLUE = new ColorSchemes.BroadCancerBlue();

    public static XChart createRankedListChart(final RankedList rl, final String classAName_opt, final String classBName_opt) {
        String title = "Ranked Gene List Correlation Profile";
        XYPlot plot = _createRankedListChart(rl, classAName_opt, classBName_opt);
        MetricWeightStruc mws = rl.getMetricWeightStruc();

        if (mws != null) {
            Marker midLine = new ValueMarker(mws.getTotalPosLength());
            midLine.setPaint(Color.BLACK);
            midLine.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 5, new float[]{10, 5, 5, 5}, 0));
            float abias = mws.getTotalPosWeight_frac() * 100;
            String abias_s = Printf.format(abias, 1);
            String cl = Printf.format(mws.getTotalPosLength_frac() * 100, 1);
            String label = "Corr. area bias to " + classAName_opt + " = " + abias_s + "% & Zero crossing at rank " + mws.getTotalPosLength() + " (" + cl + "%)";
            midLine.setLabel(label);
            midLine.setLabelBackgroundColor(Color.WHITE);
            midLine.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
            plot.addDomainMarker(midLine);
        }

        return new XChartImpl("ranked_list_corr", title, "Ranked list correlations for " + rl.getName(), plot);
    }

    public static XChart createRankedListChart(final RankedList rl, final String classAName_opt, final String classBName_opt, 
    		final int peakAt) {
        String title = "Ranked Gene List Correlation Profile";
        XYPlot plot = _createRankedListChart(rl, classAName_opt, classBName_opt);
        MetricWeightStruc mws = rl.getMetricWeightStruc();

        if (mws != null) {
            Marker midLine = new ValueMarker(mws.getTotalPosLength());
            midLine.setPaint(Color.BLACK);
            midLine.setStroke(new BasicStroke(0.25f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 3, new float[]{5, 3, 3, 3}, 0));
            String label = "Zero cross at " + mws.getTotalPosLength();
            midLine.setLabel(label);
            midLine.setLabelBackgroundColor(Color.WHITE);
            midLine.setLabelAnchor(RectangleAnchor.CENTER);
            plot.addDomainMarker(midLine);
        }

        return new XChartImpl("ranked_list_corr", title, "Ranked list correlations for " + rl.getName(), plot);
    }

    private static XYPlot _createRankedListChart(final RankedList rl, final String classAName_opt, final String classBName_opt) {
        XYDataset data;
        StandardXYItemRenderer rend;

        String axis_title = "Ranked list metric";
        MetricWeightStruc mws = rl.getMetricWeightStruc();
        if (mws != null && mws.getMetricName() != null) {
            axis_title += " (" + mws.getMetricName() + ")";
        }

        NumberAxis xAxis = new NumberAxis("Gene list location");
        xAxis.setAutoRangeIncludesZero(false); // huh
        NumberAxis yAxis = new NumberAxis(axis_title);

        yAxis.setTickMarksVisible(false);
        yAxis.setTickLabelsVisible(true);

        Vector scores = rl.getScoresV(false).toVectorInfinityAdjusted();
        data = new XYDatasetProxy2(scores, "Ranking metric scores"); // dont show legend
        rend = new StandardXYItemRenderer(StandardXYItemRenderer.DISCONTINUOUS_LINES);
        XYPlot plot = new XYPlot(data, xAxis, yAxis, rend);

        // @note otherwise defaults to red
        plot.getRenderer().setSeriesPaint(0, Color.LIGHT_GRAY);

        if (classAName_opt != null || classBName_opt != null) {
            if (classAName_opt != null && classAName_opt.length() > 0) {
                // represents an interval to be highlighted in some manner
                float max = scores.max();
                IntervalMarker target = new IntervalMarker(max, max); // @note max max so width = 0
                target.setLabel("'" + classAName_opt + "' (positively correlated)");
                target.setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
                target.setLabelAnchor(RectangleAnchor.LEFT);
                target.setLabelTextAnchor(TextAnchor.CENTER_LEFT);
                target.setLabelPaint(Color.RED);
                target.setLabelBackgroundColor(Color.WHITE);
                // Hide the IntervalMarker line
                target.setOutlineStroke(new BasicStroke(0.0f));
                target.setOutlinePaint(new Color(0, 0, 0, 0));
                plot.addRangeMarker(target);
            }

            if (classBName_opt != null && classBName_opt.length() > 0) {
                float min = scores.min();
                IntervalMarker target = new IntervalMarker(min, min);
                target.setLabel("'" + classBName_opt + "' (negatively correlated)");
                target.setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
                target.setLabelAnchor(RectangleAnchor.BOTTOM);
                target.setLabelTextAnchor(TextAnchor.CENTER_LEFT);
                target.setLabelPaint(Color.BLUE);
                target.setLabelBackgroundColor(Color.WHITE);
                target.setOutlineStroke(new BasicStroke(0.0f));
                target.setOutlinePaint(new Color(0, 0, 0, 0));
                plot.addRangeMarker(target);
            }
        }

        return plot;
    }

    public static IntervalMarker[] createIntervalMarkers(final int numRanges, final RankedList rl) {
        List<IntervalMarker> list = new ArrayList<>();
        RankedList rl_pos = rl.extractRanked(ScoreMode.POS_ONLY);
        Range[] ranges_on_full_list = RangeFactory.createRanges(numRanges, 0, rl_pos.getSize());
        IntervalMarker[] markers = _createIntervalMarkers(numRanges, rl_pos, ranges_on_full_list, 0, RankedListCharts.RED);
        list.addAll(Arrays.asList(markers));

        // Then do neg
        RankedList rl_neg = rl.extractRanked(ScoreMode.NEG_ONLY);
        ranges_on_full_list = RangeFactory.createRanges(numRanges, rl_pos.getSize(), rl.getSize());
        markers = _createIntervalMarkers(numRanges, rl_neg, ranges_on_full_list, rl_pos.getSize() + 1, RankedListCharts.BLUE);
        list.addAll(Arrays.asList(markers));

        return list.toArray(new IntervalMarker[list.size()]);
    }

    private static IntervalMarker[] _createIntervalMarkers(final int numRanges, final RankedList rl, final Range[] rangesForMarkers, 
    		final int startX, final ColorScheme cs) {
        final ColorDataset cds = new DatasetGenerators().createColorDataset(numRanges, rl, cs);
        final IntervalMarker[] markers = new IntervalMarker[numRanges];

        // add a markers
        double prev_start = startX;
        for (int c = 0; c < rangesForMarkers.length; c++) {
            markers[c] = new IntervalMarker(prev_start, rangesForMarkers[c].getMax());
            prev_start = rangesForMarkers[c].getMin();

            markers[c].setPaint(cds.getColor(0, c));
        }
        return markers;
    }
}
