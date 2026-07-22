/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.plots;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Bar / histogram plot.
 */
public final class HistogramPlotSpec extends AbstractPlotSpec {

    public static final int SPEC_VERSION = 1;

    private final String xLabel;
    private final String yLabel;
    private final double[] y;
    private final String[] categoryLabels;
    private final int[] barArgb;
    private final int selectedIndex;
    /** Bin-index (or fractional) for a vertical marker; {@code NaN} if none. */
    private final double markerX;

    public HistogramPlotSpec(String name, String title, String caption,
            String xLabel, String yLabel, double[] y, String[] categoryLabels,
            int[] barArgb, int selectedIndex) {
        this(name, title, caption, xLabel, yLabel, y, categoryLabels, barArgb, selectedIndex, Double.NaN);
    }

    public HistogramPlotSpec(String name, String title, String caption,
            String xLabel, String yLabel, double[] y, String[] categoryLabels,
            int[] barArgb, int selectedIndex, double markerX) {
        super(name, title, caption, SPEC_VERSION);
        this.xLabel = xLabel != null ? xLabel : "";
        this.yLabel = yLabel != null ? yLabel : "";
        this.y = y != null ? y.clone() : new double[0];
        this.categoryLabels = categoryLabels != null ? categoryLabels.clone() : null;
        this.barArgb = barArgb != null ? barArgb.clone() : null;
        this.selectedIndex = selectedIndex;
        this.markerX = markerX;
    }

    @Override
    public String type() {
        return "histogram";
    }

    public String xLabel() {
        return xLabel;
    }

    public String yLabel() {
        return yLabel;
    }

    public double[] y() {
        return y.clone();
    }

    public int size() {
        return y.length;
    }

    public String[] categoryLabels() {
        return categoryLabels != null ? categoryLabels.clone() : null;
    }

    public int[] barArgb() {
        return barArgb != null ? barArgb.clone() : null;
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public double markerX() {
        return markerX;
    }

    public boolean hasMarker() {
        return Double.isFinite(markerX);
    }

    @Override
    @SuppressWarnings("unchecked")
    public JSONObject toJson() {
        JSONObject o = baseJson();
        o.put("xLabel", xLabel);
        o.put("yLabel", yLabel);
        o.put("selectedIndex", selectedIndex);
        if (hasMarker()) {
            o.put("markerX", markerX);
        }
        JSONArray ys = new JSONArray();
        for (double v : y) {
            ys.add(v);
        }
        o.put("y", ys);
        if (categoryLabels != null) {
            JSONArray labs = new JSONArray();
            for (String s : categoryLabels) {
                labs.add(s);
            }
            o.put("categoryLabels", labs);
        }
        if (barArgb != null) {
            JSONArray cols = new JSONArray();
            for (int c : barArgb) {
                cols.add(c);
            }
            o.put("barArgb", cols);
        }
        return o;
    }
}
