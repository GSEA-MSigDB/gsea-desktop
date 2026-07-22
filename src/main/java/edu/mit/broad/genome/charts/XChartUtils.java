/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.charts;

import java.io.File;
import java.io.IOException;

import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.plots.HistogramPlotSpec;
import edu.mit.broad.genome.plots.PlotBuilders;
import edu.mit.broad.genome.plots.PlotChart;
import edu.mit.broad.genome.plots.XyPlotSpec;

/**
 * Common plot builders (PlotSpec / PlotChart).
 */
public class XChartUtils {

    private static final int ARGB_TEAL = 0xFF0D9488;

    private XChartUtils() {
    }

    public static XChart scatterOneXManyY(final String comboTitle, final String[] ytitles,
            final String xaxisTitle, final String yaxisTitle,
            final Vector xcommon, final Vector[] yss) {
        XyPlotSpec.Builder b = XyPlotSpec.builder(
                safeName(comboTitle), comboTitle != null ? comboTitle : "Scatter")
                .xLabel(xaxisTitle)
                .yLabel(yaxisTitle)
                .legend(ytitles != null && ytitles.length > 1);
        double[] x = xcommon != null ? xcommon.toArrayDouble() : new double[0];
        int[] palette = { ARGB_TEAL, 0xFFDC2626, 0xFF2563EB, 0xFFCA8A04, 0xFF9333EA };
        if (yss != null) {
            for (int i = 0; i < yss.length; i++) {
                String name = ytitles != null && i < ytitles.length ? ytitles[i] : ("Y" + i);
                double[] y = yss[i] != null ? yss[i].toArrayDouble() : new double[0];
                double[] xx = x.length == y.length ? x : indexAxis(y.length);
                b.addSeries(name, xx, y, false, true, palette[i % palette.length]);
            }
        }
        return new PlotChart(b.build());
    }

    public static XChart createHistogramChart(final String title, final String categoryAxisLabel,
            final String valueAxisLabel, final Vector v, final int numBins) {
        return createHistogramChart(title, categoryAxisLabel, valueAxisLabel, v, numBins,
                Float.NaN, "");
    }

    public static XChart createHistogramChart(final String title, final String categoryAxisLabel,
            final String valueAxisLabel, final Vector v, final int numBins,
            final float markerValue, final String caption) {
        HistogramPlotSpec spec = histogramFromVector(title, categoryAxisLabel, valueAxisLabel, v, numBins,
                markerValue, caption);
        return new PlotChart(spec);
    }

    private static HistogramPlotSpec histogramFromVector(String title, String xLabel, String yLabel,
            Vector v, int numBins, float markerValue, String caption) {
        if (v == null || v.getSize() == 0 || numBins < 1) {
            return PlotBuilders.histogram(safeName(title), title, caption != null ? caption : "",
                    xLabel, yLabel, new double[0], null, null, -1);
        }
        double[] data = v.toArrayDouble();
        double min = data[0];
        double max = data[0];
        for (double d : data) {
            min = Math.min(min, d);
            max = Math.max(max, d);
        }
        if (Float.isFinite(markerValue)) {
            min = Math.min(min, markerValue);
            max = Math.max(max, markerValue);
        }
        if (max <= min) {
            max = min + 1;
        }
        double[] counts = new double[numBins];
        String[] labels = new String[numBins];
        double width = (max - min) / numBins;
        for (int i = 0; i < numBins; i++) {
            labels[i] = String.format("%.2g", min + i * width);
        }
        for (double d : data) {
            int bin = (int) Math.floor((d - min) / width);
            if (bin < 0) {
                bin = 0;
            }
            if (bin >= numBins) {
                bin = numBins - 1;
            }
            counts[bin]++;
        }
        int[] colors = new int[numBins];
        for (int i = 0; i < numBins; i++) {
            colors[i] = ARGB_TEAL;
        }
        double markerX = Double.NaN;
        if (Float.isFinite(markerValue)) {
            markerX = (markerValue - min) / width;
            if (markerX < 0) {
                markerX = 0;
            }
            if (markerX > numBins - 1) {
                markerX = numBins - 1;
            }
        }
        return PlotBuilders.histogram(safeName(title), title, caption != null ? caption : "",
                xLabel, yLabel, counts, labels, colors, -1, markerX);
    }

    public static final void saveAsSVG(XChart xChart, File toFile, int width, int height, boolean gZip)
            throws IOException {
        if (xChart == null) {
            return;
        }
        xChart.saveAsSVG(toFile, width, height);
    }

    private static double[] indexAxis(int n) {
        double[] x = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = i;
        }
        return x;
    }

    private static String safeName(String title) {
        if (title == null || title.isBlank()) {
            return "chart";
        }
        return title.replaceAll("[^A-Za-z0-9._-]+", "_");
    }
}
