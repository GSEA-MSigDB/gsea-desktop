/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.plots;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Multi-series line / scatter XY plot (profiles, NES scatter, butterfly, etc.).
 */
public final class XyPlotSpec extends AbstractPlotSpec {

    public static final int SPEC_VERSION = 2;

    public static final class Series {
        public final String name;
        public final double[] x;
        public final double[] y;
        public final boolean lines;
        public final boolean shapes;
        public final int argb;
        /** 0 = primary (left) Y axis; 1 = secondary (right) Y axis. */
        public final int yAxis;

        public Series(String name, double[] x, double[] y, boolean lines, boolean shapes, int argb) {
            this(name, x, y, lines, shapes, argb, 0);
        }

        public Series(String name, double[] x, double[] y, boolean lines, boolean shapes, int argb, int yAxis) {
            this.name = name != null ? name : "";
            this.x = x != null ? x.clone() : new double[0];
            this.y = y != null ? y.clone() : new double[0];
            this.lines = lines;
            this.shapes = shapes;
            this.argb = argb;
            this.yAxis = yAxis == 1 ? 1 : 0;
        }
    }

    /** Free-floating text annotation in data coordinates (primary Y axis). */
    public static final class Annotation {
        public final double x;
        public final double y;
        public final String text;
        public final int argb;
        /** left | right | center | pinLeft | pinRight */
        public final String align;
        /** Historic IntervalMarker-style white label plate. */
        public final boolean whiteBg;

        public Annotation(double x, double y, String text, int argb, String align) {
            this(x, y, text, argb, align, false);
        }

        public Annotation(double x, double y, String text, int argb, String align, boolean whiteBg) {
            this.x = x;
            this.y = y;
            this.text = text != null ? text : "";
            this.argb = argb;
            this.align = align != null ? align : "left";
            this.whiteBg = whiteBg;
        }
    }

    private final String xLabel;
    private final String yLabel;
    private final String y2Label;
    private final String[] categoryLabels;
    private final List<Series> series;
    private final boolean legend;
    private final boolean legendBottom;
    private final boolean integerYTicks;
    private final double bandYMin;
    private final double bandYMax;
    private final int bandArgb;
    private final List<Annotation> annotations;
    private final int marginR;

    public XyPlotSpec(String name, String title, String caption,
            String xLabel, String yLabel, String[] categoryLabels,
            List<Series> series, boolean legend) {
        this(name, title, caption, xLabel, yLabel, "", categoryLabels, series, legend,
                false, false, Double.NaN, Double.NaN, 0, List.of(), 20);
    }

    public XyPlotSpec(String name, String title, String caption,
            String xLabel, String yLabel, String y2Label, String[] categoryLabels,
            List<Series> series, boolean legend, boolean legendBottom, boolean integerYTicks,
            double bandYMin, double bandYMax, int bandArgb,
            List<Annotation> annotations, int marginR) {
        super(name, title, caption, SPEC_VERSION);
        this.xLabel = xLabel != null ? xLabel : "";
        this.yLabel = yLabel != null ? yLabel : "";
        this.y2Label = y2Label != null ? y2Label : "";
        this.categoryLabels = categoryLabels != null ? categoryLabels.clone() : null;
        this.series = series != null ? List.copyOf(series) : List.of();
        this.legend = legend;
        this.legendBottom = legendBottom;
        this.integerYTicks = integerYTicks;
        this.bandYMin = bandYMin;
        this.bandYMax = bandYMax;
        this.bandArgb = bandArgb;
        this.annotations = annotations != null ? List.copyOf(annotations) : List.of();
        this.marginR = Math.max(20, marginR);
    }

    @Override
    public String type() {
        return "xy";
    }

    public String xLabel() {
        return xLabel;
    }

    public String yLabel() {
        return yLabel;
    }

    public String y2Label() {
        return y2Label;
    }

    public boolean hasSecondaryAxis() {
        return y2Label != null && !y2Label.isBlank();
    }

    public String[] categoryLabels() {
        return categoryLabels != null ? categoryLabels.clone() : null;
    }

    public List<Series> series() {
        return series;
    }

    public boolean legend() {
        return legend;
    }

    public boolean legendBottom() {
        return legendBottom;
    }

    public boolean integerYTicks() {
        return integerYTicks;
    }

    public boolean hasYBand() {
        return Double.isFinite(bandYMin) && Double.isFinite(bandYMax) && bandYMax > bandYMin;
    }

    public double bandYMin() {
        return bandYMin;
    }

    public double bandYMax() {
        return bandYMax;
    }

    public int bandArgb() {
        return bandArgb;
    }

    public List<Annotation> annotations() {
        return annotations;
    }

    public int marginR() {
        return marginR;
    }

    @Override
    @SuppressWarnings("unchecked")
    public JSONObject toJson() {
        JSONObject o = baseJson();
        o.put("xLabel", xLabel);
        o.put("yLabel", yLabel);
        if (!y2Label.isBlank()) {
            o.put("y2Label", y2Label);
        }
        o.put("legend", legend);
        if (categoryLabels != null) {
            JSONArray labs = new JSONArray();
            for (String s : categoryLabels) {
                labs.add(s);
            }
            o.put("categoryLabels", labs);
        }
        JSONArray arr = new JSONArray();
        for (Series s : series) {
            JSONObject so = new JSONObject();
            so.put("name", s.name);
            so.put("lines", s.lines);
            so.put("shapes", s.shapes);
            so.put("argb", s.argb);
            so.put("yAxis", s.yAxis);
            JSONArray xs = new JSONArray();
            JSONArray ys = new JSONArray();
            for (int i = 0; i < s.x.length; i++) {
                xs.add(s.x[i]);
                ys.add(i < s.y.length ? s.y[i] : 0d);
            }
            so.put("x", xs);
            so.put("y", ys);
            arr.add(so);
        }
        o.put("series", arr);
        return o;
    }

    public static Builder builder(String name, String title) {
        return new Builder(name, title);
    }

    public static final class Builder {
        private final String name;
        private final String title;
        private String caption = "";
        private String xLabel = "";
        private String yLabel = "";
        private String y2Label = "";
        private String[] categoryLabels;
        private final List<Series> series = new ArrayList<>();
        private boolean legend;
        private boolean legendBottom;
        private boolean integerYTicks;
        private double bandYMin = Double.NaN;
        private double bandYMax = Double.NaN;
        private int bandArgb;
        private final List<Annotation> annotations = new ArrayList<>();
        private int marginR = 20;

        private Builder(String name, String title) {
            this.name = name;
            this.title = title;
        }

        public Builder caption(String c) {
            this.caption = c;
            return this;
        }

        public Builder xLabel(String l) {
            this.xLabel = l;
            return this;
        }

        public Builder yLabel(String l) {
            this.yLabel = l;
            return this;
        }

        public Builder y2Label(String l) {
            this.y2Label = l;
            return this;
        }

        public Builder categoryLabels(String[] labels) {
            this.categoryLabels = labels;
            return this;
        }

        public Builder legend(boolean v) {
            this.legend = v;
            return this;
        }

        public Builder legendBottom(boolean v) {
            this.legendBottom = v;
            return this;
        }

        public Builder integerYTicks(boolean v) {
            this.integerYTicks = v;
            return this;
        }

        public Builder yBand(double min, double max, int argb) {
            this.bandYMin = min;
            this.bandYMax = max;
            this.bandArgb = argb;
            return this;
        }

        public Builder marginR(int px) {
            this.marginR = px;
            return this;
        }

        public Builder addAnnotation(Annotation a) {
            if (a != null) {
                annotations.add(a);
            }
            return this;
        }

        public Builder addAnnotation(double x, double y, String text, int argb, String align) {
            return addAnnotation(new Annotation(x, y, text, argb, align, false));
        }

        public Builder addAnnotation(double x, double y, String text, int argb, String align, boolean whiteBg) {
            return addAnnotation(new Annotation(x, y, text, argb, align, whiteBg));
        }

        public Builder addSeries(Series s) {
            if (s != null) {
                series.add(s);
            }
            return this;
        }

        public Builder addSeries(String name, double[] x, double[] y, boolean lines, boolean shapes, int argb) {
            return addSeries(new Series(name, x, y, lines, shapes, argb, 0));
        }

        public Builder addSeries(String name, double[] x, double[] y, boolean lines, boolean shapes, int argb, int yAxis) {
            return addSeries(new Series(name, x, y, lines, shapes, argb, yAxis));
        }

        public XyPlotSpec build() {
            return new XyPlotSpec(name, title, caption, xLabel, yLabel, y2Label, categoryLabels,
                    Collections.unmodifiableList(new ArrayList<>(series)), legend, legendBottom, integerYTicks,
                    bandYMin, bandYMax, bandArgb,
                    Collections.unmodifiableList(new ArrayList<>(annotations)), marginR);
        }
    }
}
