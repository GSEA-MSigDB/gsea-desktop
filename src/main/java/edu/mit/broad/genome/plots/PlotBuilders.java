/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.plots;

import java.util.List;

import org.genepattern.data.expr.IExpressionData;

import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.GPWrappers;
import edu.mit.broad.genome.objects.RankedList;
import gnu.trove.TFloatIntHashMap;
import gnu.trove.TFloatIntIterator;

/** Build {@link PlotSpec} instances from analysis objects. */
public final class PlotBuilders {

    private static final int ARGB_RED = 0xFFFF0000;
    private static final int ARGB_BLUE = 0xFF0000FF;
    private static final int ARGB_YELLOW = 0xFFFFFF00;
    private static final int ARGB_TEAL = 0xFF0D9488;
    private static final int ARGB_GRAY = 0xFF64748B;

    private PlotBuilders() {
    }

    public static HistogramPlotSpec histogram(String name, String title, String caption,
            String xLabel, String yLabel, double[] y, String[] categoryLabels,
            int[] barArgb, int selectedIndex) {
        return new HistogramPlotSpec(name, title, caption, xLabel, yLabel, y, categoryLabels, barArgb, selectedIndex);
    }

    public static HistogramPlotSpec geneHistogram(RankedList featureFrequency, RankedList scores,
            Integer selectedGeneIndex) {
        if (featureFrequency == null || featureFrequency.getSize() == 0) {
            return histogram("gene_histogram", "Gene Histogram", "",
                    "Gene", "Number Of Gene Sets", new double[0], null, null, -1);
        }
        int n = featureFrequency.getSize();
        double[] y = new double[n];
        String[] labels = featureFrequency.getRankedNamesArray();
        int[] colors = new int[n];
        int selected = selectedGeneIndex != null ? selectedGeneIndex : -1;
        for (int i = 0; i < n; i++) {
            y[i] = featureFrequency.getScore(i);
            if (i == selected) {
                colors[i] = ARGB_YELLOW;
            } else if (scores != null) {
                float v = scores.getScore(featureFrequency.getRankName(i));
                colors[i] = v > 0f ? ARGB_RED : ARGB_BLUE;
            } else {
                colors[i] = ARGB_TEAL;
            }
        }
        return histogram("gene_histogram", "Gene Histogram", "",
                "Gene", "Number Of Gene Sets", y, labels, colors, selected);
    }

    public static HistogramPlotSpec jaccardHistogram(TFloatIntHashMap jaccardToOccurrences, double binWidth) {
        if (jaccardToOccurrences == null || jaccardToOccurrences.isEmpty() || binWidth <= 0) {
            return histogram("jaccard_histogram", "Jaccard Histogram", "",
                    "Jaccard", "Number of Occurences", new double[0], null, null, -1);
        }
        int bins = (int) Math.ceil(1.0 / binWidth);
        if (bins < 1) {
            bins = 1;
        }
        double[] y = new double[bins];
        String[] labels = new String[bins];
        for (int i = 0; i < bins; i++) {
            double start = i * binWidth;
            labels[i] = String.format("%.2f", start);
        }
        TFloatIntIterator it = jaccardToOccurrences.iterator();
        while (it.hasNext()) {
            it.advance();
            float key = it.key();
            int idx = (int) Math.min(bins - 1, Math.floor(key / binWidth));
            if (idx < 0) {
                idx = 0;
            }
            y[idx] += it.value();
        }
        int[] colors = new int[bins];
        for (int i = 0; i < bins; i++) {
            colors[i] = ARGB_TEAL;
        }
        return histogram("jaccard_histogram", "Jaccard Histogram of Gene Sets", "",
                "Jaccard", "Number of Occurences", y, labels, colors, -1);
    }

    public static XyPlotSpec profile(Dataset dataset, int[] rowIndices) {
        if (dataset == null) {
            return XyPlotSpec.builder("profile", "Profile").xLabel("Column").yLabel("Value").build();
        }
        IExpressionData data = GPWrappers.createIExpressionData(dataset);
        int cols = data.getColumnCount();
        String[] colNames = new String[cols];
        double[] xs = new double[cols];
        for (int j = 0; j < cols; j++) {
            colNames[j] = data.getColumnName(j);
            xs[j] = j;
        }
        XyPlotSpec.Builder b = XyPlotSpec.builder("profile", "Profile")
                .xLabel("Column")
                .yLabel("Value")
                .categoryLabels(colNames);
        int rows = rowIndices != null && rowIndices.length > 0 ? rowIndices.length : data.getRowCount();
        int[] palette = { ARGB_TEAL, ARGB_RED, ARGB_BLUE, 0xFFCA8A04, 0xFF9333EA, ARGB_GRAY };
        for (int i = 0; i < rows; i++) {
            int index = rowIndices != null && rowIndices.length > 0 ? rowIndices[i] : i;
            double[] ys = new double[cols];
            for (int j = 0; j < cols; j++) {
                ys[j] = data.getValue(index, j);
            }
            b.addSeries(data.getRowName(index), xs, ys, true, true, palette[i % palette.length]);
        }
        if (rowIndices != null && rowIndices.length > 0 && rowIndices.length <= 5) {
            b.legend(true);
        }
        return b.build();
    }

    public static HistogramPlotSpec histogram(String name, String title, String caption,
            String xLabel, String yLabel, double[] y, String[] categoryLabels,
            int[] barArgb, int selectedIndex, double markerX) {
        return new HistogramPlotSpec(name, title, caption, xLabel, yLabel, y, categoryLabels,
                barArgb, selectedIndex, markerX);
    }

    /**
     * Convert a 0/1 membership vector over the ranked list into hit rank indices.
     */
    public static int[] hitRanksFromMembership(Vector membership) {
        if (membership == null) {
            return new int[0];
        }
        int count = 0;
        for (int i = 0; i < membership.getSize(); i++) {
            if (membership.getElement(i) != 0f) {
                count++;
            }
        }
        int[] hits = new int[count];
        int n = 0;
        for (int i = 0; i < membership.getSize(); i++) {
            if (membership.getElement(i) != 0f) {
                hits[n++] = i;
            }
        }
        return hits;
    }

    public static BubblePlotSpec bubble(String name, String title, String caption,
            String xLabel, String yLabel, List<BubblePlotSpec.Point> points) {
        return bubble(name, title, caption, xLabel, yLabel, points, 0, Double.NaN);
    }

    public static BubblePlotSpec bubble(String name, String title, String caption,
            String xLabel, String yLabel, List<BubblePlotSpec.Point> points,
            int leftMarginPx, double xMax) {
        return bubble(name, title, caption, xLabel, yLabel, points, leftMarginPx, xMax,
                BubblePlotSpec.Options.defaults());
    }

    public static BubblePlotSpec bubble(String name, String title, String caption,
            String xLabel, String yLabel, List<BubblePlotSpec.Point> points,
            int leftMarginPx, double xMax, BubblePlotSpec.Options options) {
        BubblePlotSpec.Point[] arr = points != null
                ? points.toArray(new BubblePlotSpec.Point[0])
                : new BubblePlotSpec.Point[0];
        return new BubblePlotSpec(name, title, caption, xLabel, yLabel, arr, leftMarginPx, xMax, options);
    }
}
