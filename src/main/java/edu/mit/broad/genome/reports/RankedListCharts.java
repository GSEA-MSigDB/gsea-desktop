/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.reports;

import edu.mit.broad.genome.charts.XChart;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.MetricWeightStruc;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.plots.ColorBarSegment;
import edu.mit.broad.genome.plots.PlotChart;
import edu.mit.broad.genome.plots.XyPlotSpec;

/**
 * Ranked-list metric profile charts (PlotSpec-backed).
 */
public class RankedListCharts {

    public static XChart createRankedListChart(final RankedList rl, final String classAName_opt, final String classBName_opt) {
        return createRankedListChart(rl, classAName_opt, classBName_opt, -1);
    }

    public static XChart createRankedListChart(final RankedList rl, final String classAName_opt, final String classBName_opt,
            final int peakAt) {
        String title = "Ranked Gene List Correlation Profile";
        String axis_title = "Ranked list metric";
        MetricWeightStruc mws = rl.getMetricWeightStruc();
        if (mws != null && mws.getMetricName() != null) {
            axis_title += " (" + mws.getMetricName() + ")";
        }
        Vector scores = Vector.infinityAdjustRankedScoreVector(rl.getScoresV(false));
        double[] y = scores.toArrayDouble();
        double[] x = new double[y.length];
        for (int i = 0; i < y.length; i++) {
            x[i] = i;
        }
        String caption = "Ranked list correlations for " + rl.getName();
        if (classAName_opt != null) {
            caption += "; pos=" + classAName_opt;
        }
        if (classBName_opt != null) {
            caption += "; neg=" + classBName_opt;
        }
        if (peakAt >= 0) {
            caption += "; peak@" + peakAt;
        }
        return new PlotChart(XyPlotSpec.builder("ranked_list_corr", title)
                .caption(caption)
                .xLabel("Gene list location")
                .yLabel(axis_title)
                .addSeries("Ranking metric scores", x, y, true, false, 0xFFC0C0C0)
                .build());
    }

    public static ColorBarSegment[] createColorBarSegments(final int numRanges, final RankedList rl) {
        return ColorBarSegment.forRankedList(numRanges, rl);
    }
}
