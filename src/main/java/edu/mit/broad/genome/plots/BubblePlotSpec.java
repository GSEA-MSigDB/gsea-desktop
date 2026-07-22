/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.plots;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.Color;

/**
 * Bubble chart (x, y, size) with optional left-margin category labels and GSEA chrome
 * (NOM-p thresholds, FDR/size legends, zero line).
 */
public final class BubblePlotSpec extends AbstractPlotSpec {

    public static final int SPEC_VERSION = 2;

    public static final class Point {
        public final double x;
        public final double y;
        /** Diameter in pixels when {@link Options#sizeAbsolutePx} is true; otherwise relative. */
        public final double size;
        public final int argb;
        /** Left-margin gene-set name (no FDR stars). */
        public final String label;
        /** Optional FDR stars drawn beside the bubble ({@code *}/{@code **}/{@code ***}). */
        public final String star;

        public Point(double x, double y, double size, int argb, String label) {
            this(x, y, size, argb, label, "");
        }

        public Point(double x, double y, double size, int argb, String label, String star) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.argb = argb;
            this.label = label != null ? label : "";
            this.star = star != null ? star : "";
        }
    }

    /** Optional domain markers, legends, and axis behavior. */
    public static final class Options {
        public final double xMin;
        public final boolean sizeAbsolutePx;
        public final boolean zeroLine;
        public final double[] markerXs;
        public final String[] markerLabels;
        public final boolean gseaLegends;
        public final boolean positiveNes;
        public final double nesNorm;
        public final int bottomExtraPx;

        public Options(double xMin, boolean sizeAbsolutePx, boolean zeroLine,
                double[] markerXs, String[] markerLabels,
                boolean gseaLegends, boolean positiveNes, double nesNorm, int bottomExtraPx) {
            this.xMin = xMin;
            this.sizeAbsolutePx = sizeAbsolutePx;
            this.zeroLine = zeroLine;
            this.markerXs = markerXs != null ? markerXs.clone() : new double[0];
            this.markerLabels = markerLabels != null ? markerLabels.clone() : new String[0];
            this.gseaLegends = gseaLegends;
            this.positiveNes = positiveNes;
            this.nesNorm = nesNorm;
            this.bottomExtraPx = Math.max(0, bottomExtraPx);
        }

        public static Options defaults() {
            return new Options(Double.NaN, true, false, null, null, false, true, 1.0, 0);
        }

        public static Options gsea(boolean positiveNes, double nesNorm, double xMin,
                double[] markerXs, String[] markerLabels) {
            return new Options(xMin, true, false, markerXs, markerLabels, true, positiveNes, nesNorm, 110);
        }

        public static Options ssgsea(double xMin) {
            return new Options(xMin, true, true, null, null, false, true, 1.0, 0);
        }
    }

    private final String xLabel;
    private final String yLabel;
    private final Point[] points;
    private final int leftMarginPx;
    private final double xMax;
    private final Options options;

    public BubblePlotSpec(String name, String title, String caption,
            String xLabel, String yLabel, Point[] points) {
        this(name, title, caption, xLabel, yLabel, points, 0, Double.NaN, Options.defaults());
    }

    public BubblePlotSpec(String name, String title, String caption,
            String xLabel, String yLabel, Point[] points,
            int leftMarginPx, double xMax) {
        this(name, title, caption, xLabel, yLabel, points, leftMarginPx, xMax, Options.defaults());
    }

    public BubblePlotSpec(String name, String title, String caption,
            String xLabel, String yLabel, Point[] points,
            int leftMarginPx, double xMax, Options options) {
        super(name, title, caption, SPEC_VERSION);
        this.xLabel = xLabel != null ? xLabel : "";
        this.yLabel = yLabel != null ? yLabel : "";
        this.points = points != null ? points.clone() : new Point[0];
        this.leftMarginPx = Math.max(0, leftMarginPx);
        this.xMax = xMax;
        this.options = options != null ? options : Options.defaults();
    }

    @Override
    public String type() {
        return "bubble";
    }

    public String xLabel() {
        return xLabel;
    }

    public String yLabel() {
        return yLabel;
    }

    public Point[] points() {
        return points.clone();
    }

    public int leftMarginPx() {
        return leftMarginPx;
    }

    public double xMax() {
        return xMax;
    }

    public Options options() {
        return options;
    }

    /**
     * Historic GSEA bubble FDR intensity (white→red for positive NES, white→blue for negative).
     */
    public static Color fdrPaint(float fdr, boolean positiveNes) {
        if (positiveNes) {
            if (fdr < 0.01f) {
                return new Color(255, 20, 20);
            }
            if (fdr <= 0.05f) {
                return new Color(255, 70, 70);
            }
            if (fdr <= 0.25f) {
                return new Color(255, 140, 140);
            }
            return new Color(255, 220, 220);
        }
        if (fdr < 0.01f) {
            return new Color(20, 20, 255);
        }
        if (fdr <= 0.05f) {
            return new Color(70, 70, 255);
        }
        if (fdr <= 0.25f) {
            return new Color(140, 140, 255);
        }
        return new Color(220, 220, 255);
    }

    public static int fdrArgb(float fdr, boolean positiveNes) {
        return fdrPaint(fdr, positiveNes).getRGB();
    }

    @Override
    @SuppressWarnings("unchecked")
    public JSONObject toJson() {
        JSONObject o = baseJson();
        o.put("xLabel", xLabel);
        o.put("yLabel", yLabel);
        o.put("leftMarginPx", leftMarginPx);
        if (Double.isFinite(xMax)) {
            o.put("xMax", xMax);
        }
        o.put("sizeAbsolutePx", options.sizeAbsolutePx);
        o.put("zeroLine", options.zeroLine);
        o.put("gseaLegends", options.gseaLegends);
        JSONArray arr = new JSONArray();
        for (Point p : points) {
            JSONObject po = new JSONObject();
            po.put("x", p.x);
            po.put("y", p.y);
            po.put("size", p.size);
            po.put("argb", p.argb);
            po.put("label", p.label);
            if (!p.star.isBlank()) {
                po.put("star", p.star);
            }
            arr.add(po);
        }
        o.put("points", arr);
        return o;
    }
}
