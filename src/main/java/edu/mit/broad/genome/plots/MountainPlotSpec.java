/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.plots;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Four-panel enrichment mountain for report PNGs.
 * <p>
 * {@link Style#V2} mirrors {@code enplot/viewer.js} layout and chrome.
 * {@link Style#CLASSIC} uses legacy GSEA panel weights and colors (green ES, black hits).
 */
public final class MountainPlotSpec extends AbstractPlotSpec {

    public static final int SPEC_VERSION = 2;

    public enum Style {
        CLASSIC,
        V2
    }

    private static final int PAD_L = 58;
    private static final int PAD_R = 16;
    private static final int PAD_T = 40;
    private static final int PAD_B = 42;
    private static final int GAP_V2 = 4;
    private static final int GAP_CLASSIC = 0;
    /** Domain axis divisions (tick marks + labels at every division). */
    private static final int DOMAIN_TICKS = 8;
    /** Target count for nice Y-axis tick values. */
    private static final int Y_TICK_TARGET = 6;

    private static final Color TEAL = new Color(0x0D, 0x94, 0x88);
    private static final Color TEAL_FILL = new Color(13, 148, 136, 46);
    private static final Color HIT = new Color(0x94, 0xA3, 0xB8);
    private static final Color LE = new Color(0x0F, 0x76, 0x6E);
    private static final Color GRID = new Color(0xE2, 0xE8, 0xF0);
    /** JFreeChart {@code XYPlot.DEFAULT_GRIDLINE_STROKE}: light dashed backdrop. */
    private static final BasicStroke CLASSIC_GRID_STROKE = new BasicStroke(
            0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, new float[]{2f, 2f}, 0f);
    private static final Color CLASSIC_GRID = Color.LIGHT_GRAY;
    private static final Color AXIS = new Color(0x64, 0x74, 0x8B);
    private static final Color METRIC = new Color(0x94, 0xA3, 0xB8);
    private static final Color METRIC_FILL = new Color(148, 163, 184, 69);
    private static final Color POS = new Color(0xDC, 0x26, 0x26);
    private static final Color NEG = new Color(0x25, 0x63, 0xEB);
    private static final Color ZERO = new Color(0x94, 0xA3, 0xB8);
    private static final Color PEAK = new Color(0x0F, 0x76, 0x6E);
    private static final Color INK = new Color(0x0F, 0x17, 0x2A);
    /** Historic JFree classic used {@link Color#GREEN}. */
    private static final Color CLASSIC_ES = Color.GREEN;
    private static final Color CLASSIC_HIT = Color.BLACK;
    private static final Color CLASSIC_METRIC = new Color(0xC0, 0xC0, 0xC0);
    private static final Color CLASSIC_METRIC_FILL = new Color(0xC0, 0xC0, 0xC0, 110);
    private static final Color CLASSIC_ZERO = Color.DARK_GRAY;
    private static final Color ANN_BG = new Color(255, 255, 255, 224);

    private final Style style;
    private final int listSize;
    /** ES curve x ranks (same length as {@link #esY}). */
    private final double[] esX;
    private final double[] esY;
    private final int[] hitRanks;
    private final boolean[] hitLeadingEdge;
    private final double[] metricY;
    private final String metricName;
    private final String classA;
    private final String classB;
    private final ColorBarSegment[] colorBar;
    private final int peakRank;
    private final double peakEs;
    private final float es;
    private final float nes;
    private final float np;
    private final float fdr;
    private final int zeroCross;

    public MountainPlotSpec(String name, String title, String caption,
            Style style, int listSize,
            double[] esX, double[] esY,
            int[] hitRanks, boolean[] hitLeadingEdge,
            double[] metricY, String metricName,
            String classA, String classB,
            ColorBarSegment[] colorBar,
            int peakRank, double peakEs,
            float es, float nes, float np, float fdr,
            int zeroCross) {
        super(name, title, caption, SPEC_VERSION);
        this.style = style != null ? style : Style.V2;
        this.listSize = Math.max(1, listSize);
        this.esX = esX != null ? esX.clone() : new double[0];
        this.esY = esY != null ? esY.clone() : new double[0];
        this.hitRanks = hitRanks != null ? hitRanks.clone() : new int[0];
        this.hitLeadingEdge = hitLeadingEdge != null ? hitLeadingEdge.clone() : new boolean[this.hitRanks.length];
        this.metricY = metricY != null ? metricY.clone() : new double[0];
        this.metricName = metricName != null ? metricName : "";
        this.classA = classA != null ? classA : "";
        this.classB = classB != null ? classB : "";
        this.colorBar = colorBar != null ? colorBar.clone() : null;
        this.peakRank = peakRank;
        this.peakEs = peakEs;
        this.es = es;
        this.nes = nes;
        this.np = np;
        this.fdr = fdr;
        this.zeroCross = zeroCross;
    }

    @Override
    public String type() {
        return "mountain";
    }

    public Style style() {
        return style;
    }

    public int listSize() {
        return listSize;
    }

    @Override
    @SuppressWarnings("unchecked")
    public JSONObject toJson() {
        JSONObject o = baseJson();
        o.put("style", style.name());
        o.put("listSize", listSize);
        o.put("classA", classA);
        o.put("classB", classB);
        o.put("metricName", metricName);
        o.put("peakRank", peakRank);
        o.put("peakEs", peakEs);
        o.put("es", (double) es);
        o.put("nes", (double) nes);
        o.put("np", (double) np);
        o.put("fdr", (double) fdr);
        o.put("zeroCross", zeroCross);
        JSONArray curve = new JSONArray();
        for (int i = 0; i < esY.length; i++) {
            JSONArray pt = new JSONArray();
            pt.add(i < esX.length ? esX[i] : i);
            pt.add(esY[i]);
            curve.add(pt);
        }
        o.put("esCurve", curve);
        JSONArray hits = new JSONArray();
        for (int i = 0; i < hitRanks.length; i++) {
            JSONObject h = new JSONObject();
            h.put("rank", hitRanks[i]);
            h.put("leadingEdge", i < hitLeadingEdge.length && hitLeadingEdge[i]);
            hits.add(h);
        }
        o.put("hits", hits);
        JSONArray met = new JSONArray();
        for (int i = 0; i < metricY.length; i++) {
            JSONArray pt = new JSONArray();
            pt.add(i);
            pt.add(metricY[i]);
            met.add(pt);
        }
        o.put("metric", met);
        if (colorBar != null) {
            JSONArray bar = new JSONArray();
            for (ColorBarSegment seg : colorBar) {
                JSONObject s = new JSONObject();
                s.put("start", seg.start);
                s.put("end", seg.end);
                s.put("argb", seg.argb);
                bar.add(s);
            }
            o.put("colorBar", bar);
        }
        return o;
    }

    static void draw(MountainPlotSpec spec, Graphics2D g2, int width, int height) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);

        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.setColor(INK);
        g2.drawString(spec.title(), PAD_L, 16);
        if (spec.caption() != null && !spec.caption().isBlank()) {
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.setColor(AXIS);
            g2.drawString(spec.caption(), PAD_L, 30);
        }

        boolean classic = spec.style == Style.CLASSIC;
        int gap = classic ? GAP_CLASSIC : GAP_V2;
        int[] weights = classic ? new int[]{12, 4, 1, 8} : new int[]{12, 3, 1, 7};
        Rectangle2D[] panels = panelRects(width, height, weights, gap);
        Ext esExt = extent(spec.esY, 0.12);
        Ext metExt = extent(spec.metricY, 0.10);

        drawEsPanel(g2, panels[0], spec, esExt);
        drawHitsPanel(g2, panels[1], spec);
        drawColorBar(g2, panels[2], spec);
        drawMetricPanel(g2, panels[3], spec, metExt);
        drawDomainAxis(g2, panels[3], spec.listSize, classic);
    }

    private static Rectangle2D[] panelRects(int width, int height, int[] weights, int gap) {
        int avail = height - PAD_T - PAD_B;
        int sum = 0;
        for (int w : weights) {
            sum += w;
        }
        double y = PAD_T;
        double plotW = Math.max(10, width - PAD_L - PAD_R);
        Rectangle2D[] rects = new Rectangle2D[weights.length];
        for (int i = 0; i < weights.length; i++) {
            double hh = Math.max(8, (weights[i] / (double) sum) * (avail - gap * (weights.length - 1)));
            rects[i] = new Rectangle2D.Double(PAD_L, y, plotW, hh);
            y += hh + gap;
        }
        return rects;
    }

    private static void drawEsPanel(Graphics2D g2, Rectangle2D rect, MountainPlotSpec spec, Ext yExt) {
        boolean classic = spec.style == Style.CLASSIC;
        Color line = classic ? CLASSIC_ES : TEAL;
        Color fill = TEAL_FILL;

        drawPanelFrame(g2, rect);

        if (classic) {
            drawClassicGrids(g2, rect, yExt);
        }

        double zy = mapY(rect, yExt, 0);
        g2.setColor(classic ? CLASSIC_ZERO : ZERO);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new Line2D.Double(rect.getX(), zy, rect.getMaxX(), zy));

        if (spec.esY.length > 1) {
            drawSeries(g2, rect, yExt, spec.listSize, spec.esX, spec.esY, zy,
                    classic ? null : fill, line, classic ? 2f : 2.25f);
        }

        if (!classic && spec.peakRank >= 0 && spec.peakRank < spec.listSize) {
            double peakX = mapX(rect, spec.listSize, spec.peakRank);
            g2.setColor(PEAK);
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{4f, 3f}, 0f));
            g2.draw(new Line2D.Double(peakX, rect.getY(), peakX, rect.getMaxY()));
            g2.setStroke(new BasicStroke(1f));

            String ann = String.format("ES=%.3f  NES=%.2f  NOM pVal=%.3f  FDR=%.3f%npeak at rank %d",
                    spec.es, spec.nes, spec.np, spec.fdr, spec.peakRank);
            drawLabelBox(g2, ann, peakX, zy, spec.peakEs >= 0, rect.getMaxX(), true);
        }

        drawYTicks(g2, rect, yExt);
        drawRotatedLabel(g2, "Enrichment score (ES)", 14, rect.getCenterY());
    }

    private static void drawHitsPanel(Graphics2D g2, Rectangle2D rect, MountainPlotSpec spec) {
        boolean classic = spec.style == Style.CLASSIC;
        drawPanelFrame(g2, rect);
        for (int i = 0; i < spec.hitRanks.length; i++) {
            int rank = spec.hitRanks[i];
            if (rank < 0 || rank >= spec.listSize) {
                continue;
            }
            double px = mapX(rect, spec.listSize, rank);
            boolean le = !classic && i < spec.hitLeadingEdge.length && spec.hitLeadingEdge[i];
            g2.setColor(classic ? CLASSIC_HIT : (le ? LE : HIT));
            g2.setStroke(new BasicStroke(classic ? 1f : (le ? 1.5f : 0.85f)));
            g2.draw(new Line2D.Double(px, rect.getY() + 2, px, rect.getMaxY() - 2));
        }
        // Historic classic hit strip had an empty range-axis label; v2 keeps "Set Members".
        if (!classic) {
            drawRotatedLabel(g2, "Set Members", 14, rect.getCenterY());
        }
    }

    private static void drawColorBar(Graphics2D g2, Rectangle2D rect, MountainPlotSpec spec) {
        double y = rect.getY() + 1;
        double h = Math.max(1, rect.getHeight() - 2);
        if (spec.colorBar != null && spec.colorBar.length > 0) {
            for (ColorBarSegment seg : spec.colorBar) {
                double px0 = mapX(rect, spec.listSize, seg.start);
                double px1 = mapX(rect, spec.listSize, seg.end);
                if (px1 < px0) {
                    double t = px0;
                    px0 = px1;
                    px1 = t;
                }
                g2.setColor(seg.paint());
                g2.fill(new Rectangle2D.Double(px0, y, Math.max(1, px1 - px0), h));
            }
        } else {
            // Red → white → blue fallback (viewer.js)
            for (int i = 0; i < (int) rect.getWidth(); i++) {
                float t = i / (float) Math.max(1, (int) rect.getWidth() - 1);
                Color c;
                if (t < 0.5f) {
                    float u = t / 0.5f;
                    c = lerp(new Color(210, 40, 40), Color.WHITE, u);
                } else {
                    float u = (t - 0.5f) / 0.5f;
                    c = lerp(Color.WHITE, new Color(40, 80, 210), u);
                }
                g2.setColor(c);
                g2.fillRect((int) (rect.getX() + i), (int) y, 1, (int) h);
            }
        }
        g2.setColor(GRID);
        g2.draw(new Rectangle2D.Double(rect.getX(), y, rect.getWidth(), h));
    }

    private static void drawMetricPanel(Graphics2D g2, Rectangle2D rect, MountainPlotSpec spec, Ext yExt) {
        boolean classic = spec.style == Style.CLASSIC;
        drawPanelFrame(g2, rect);

        // Classic: JFree LIGHT_GRAY dashed tick grids. V2: solid slate horizontals.
        if (classic) {
            drawClassicGrids(g2, rect, yExt);
        } else {
            g2.setColor(GRID);
            g2.setStroke(new BasicStroke(1f));
            for (int g = 1; g <= 3; g++) {
                double gy = rect.getY() + rect.getHeight() * g / 4.0;
                g2.draw(new Line2D.Double(rect.getX(), gy, rect.getMaxX(), gy));
            }
        }

        if (yExt.min < 0 && yExt.max > 0) {
            double zy = mapY(rect, yExt, 0);
            g2.setColor(classic ? CLASSIC_ZERO : ZERO);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new Line2D.Double(rect.getX(), zy, rect.getMaxX(), zy));
        }

        if (spec.metricY.length > 1) {
            double zy = mapY(rect, yExt, 0);
            drawSeries(g2, rect, yExt, spec.listSize, null, spec.metricY, zy,
                    classic ? CLASSIC_METRIC_FILL : METRIC_FILL,
                    classic ? CLASSIC_METRIC : METRIC,
                    1.25f);
        }

        if (spec.zeroCross >= 0 && spec.zeroCross < spec.listSize) {
            double zx = mapX(rect, spec.listSize, spec.zeroCross);
            // Classic GSEA: thin black dashed domain marker; v2 uses muted axis chrome.
            g2.setColor(classic ? Color.BLACK : AXIS);
            float stroke = classic ? 0.25f : 0.75f;
            float[] dash = classic ? new float[]{5f, 3f, 3f, 3f} : new float[]{4f, 3f};
            g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, dash, 0f));
            g2.draw(new Line2D.Double(zx, rect.getY(), zx, rect.getMaxY()));
            g2.setStroke(new BasicStroke(1f));
            String zcLabel = "Zero cross at " + spec.zeroCross;
            if (classic) {
                // Historic ValueMarker: white label bg, RectangleAnchor.CENTER on the line.
                drawClassicZeroCrossLabel(g2, zcLabel, zx, rect);
            } else {
                drawLabelBox(g2, zcLabel,
                        zx, rect.getY() + rect.getHeight() * 0.48, true, rect.getMaxX(), true);
            }
        }

        drawYTicks(g2, rect, yExt);

        String metricTitle = "Ranked list metric";
        if (!spec.metricName.isBlank()) {
            metricTitle += " (" + spec.metricName + ")";
        }
        drawRotatedLabel(g2, metricTitle, 14, rect.getCenterY());

        // Phenotype labels at actual metric tips (not padded extent), clamped inside this panel.
        Ext dataExt = extent(spec.metricY, 0);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        FontMetrics fm = g2.getFontMetrics();
        if (!spec.classA.isBlank()) {
            g2.setColor(POS);
            String lab = classic
                    ? "'" + spec.classA + "' (positively correlated)"
                    : "'" + spec.classA + "' (pos)";
            double tipY = mapY(rect, yExt, dataExt.max);
            float textY = (float) Math.max(rect.getY() + fm.getAscent() + 2, tipY + fm.getAscent());
            textY = (float) Math.min(textY, rect.getMaxY() - 2);
            g2.drawString(lab, (float) (rect.getX() + 4), textY);
        }
        if (!spec.classB.isBlank()) {
            g2.setColor(NEG);
            String lab = classic
                    ? "'" + spec.classB + "' (negatively correlated)"
                    : "'" + spec.classB + "' (neg)";
            double tipY = mapY(rect, yExt, dataExt.min);
            float textY = (float) Math.max(rect.getY() + fm.getAscent() + 2,
                    Math.min(tipY - 2, rect.getMaxY() - fm.getDescent() - 2));
            // Right-tip anchor (same as v2) so the label sits at the negative end of the list.
            float textX = (float) (rect.getMaxX() - 4 - fm.stringWidth(lab));
            g2.drawString(lab, textX, textY);
        }
    }

    private static void drawDomainAxis(Graphics2D g2, Rectangle2D metricPanel, int listSize, boolean classic) {
        double axisY = metricPanel.getMaxY() + 8;
        double x0 = metricPanel.getX();
        double x1 = metricPanel.getMaxX();
        g2.setColor(AXIS);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new Line2D.Double(x0, axisY, x1, axisY));

        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        FontMetrics fm = g2.getFontMetrics();
        int lastRank = Math.max(0, listSize - 1);
        for (int t = 0; t <= DOMAIN_TICKS; t++) {
            double frac = t / (double) DOMAIN_TICKS;
            int rank = (int) Math.round(frac * lastRank);
            double px = x0 + frac * metricPanel.getWidth();
            double tickH = (t == 0 || t == DOMAIN_TICKS) ? 5 : 3;
            g2.draw(new Line2D.Double(px, axisY, px, axisY + tickH));
            String lab = String.valueOf(rank);
            float tx;
            if (t == 0) {
                tx = (float) px;
            } else if (t == DOMAIN_TICKS) {
                tx = (float) (px - fm.stringWidth(lab));
            } else {
                tx = (float) (px - fm.stringWidth(lab) / 2.0);
            }
            g2.drawString(lab, tx, (float) (axisY + 6 + fm.getAscent()));
        }

        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        String title = classic ? "Rank in Ordered Dataset" : "Rank in ordered dataset";
        float titleX = (float) (x0 + (metricPanel.getWidth() - g2.getFontMetrics().stringWidth(title)) / 2.0);
        g2.drawString(title, titleX, (float) (axisY + 28));
    }

    /**
     * Optional under-curve fill to {@code zeroY}, then stroke. When {@code xs} is null, x = series index.
     */
    private static void drawSeries(Graphics2D g2, Rectangle2D rect, Ext yExt, int listSize,
            double[] xs, double[] ys, double zeroY, Color fillColor, Color lineColor, float lineWidth) {
        if (ys.length < 2) {
            return;
        }
        if (fillColor != null) {
            Path2D fillPath = new Path2D.Double();
            boolean started = false;
            double lastX = rect.getX();
            for (int i = 0; i < ys.length; i++) {
                double rx = xs != null && i < xs.length ? xs[i] : i;
                double px = mapX(rect, listSize, rx);
                double py = mapY(rect, yExt, ys[i]);
                if (!started) {
                    fillPath.moveTo(px, zeroY);
                    fillPath.lineTo(px, py);
                    started = true;
                } else {
                    fillPath.lineTo(px, py);
                }
                lastX = px;
            }
            if (started) {
                fillPath.lineTo(lastX, zeroY);
                fillPath.closePath();
                g2.setColor(fillColor);
                g2.fill(fillPath);
            }
        }
        Path2D path = new Path2D.Double();
        for (int i = 0; i < ys.length; i++) {
            double rx = xs != null && i < xs.length ? xs[i] : i;
            double px = mapX(rect, listSize, rx);
            double py = mapY(rect, yExt, ys[i]);
            if (i == 0) {
                path.moveTo(px, py);
            } else {
                path.lineTo(px, py);
            }
        }
        g2.setColor(lineColor);
        g2.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(path);
    }

    private static void drawPanelFrame(Graphics2D g2, Rectangle2D rect) {
        g2.setColor(Color.WHITE);
        g2.fill(rect);
        g2.setColor(GRID);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(rect);
    }

    /**
     * Historic JFree combo subplots: LIGHT_GRAY dashed grids at domain ticks and nice Y ticks.
     */
    private static void drawClassicGrids(Graphics2D g2, Rectangle2D rect, Ext yExt) {
        g2.setColor(CLASSIC_GRID);
        g2.setStroke(CLASSIC_GRID_STROKE);
        for (int t = 1; t < DOMAIN_TICKS; t++) {
            double px = rect.getX() + (t / (double) DOMAIN_TICKS) * rect.getWidth();
            g2.draw(new Line2D.Double(px, rect.getY(), px, rect.getMaxY()));
        }
        for (double y : yTickValues(yExt)) {
            if (y <= yExt.min + 1e-12 || y >= yExt.max - 1e-12) {
                continue;
            }
            double py = mapY(rect, yExt, y);
            g2.draw(new Line2D.Double(rect.getX(), py, rect.getMaxX(), py));
        }
    }

    /** Historic ValueMarker label: white background, centered on the domain marker. */
    private static void drawClassicZeroCrossLabel(Graphics2D g2, String text, double zx, Rectangle2D rect) {
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(text);
        int th = fm.getAscent() + fm.getDescent();
        int pad = 2;
        double bx = zx - tw / 2.0 - pad;
        double by = rect.getCenterY() - th / 2.0 - pad;
        bx = Math.max(rect.getX() + 1, Math.min(bx, rect.getMaxX() - tw - 2.0 * pad - 1));
        by = Math.max(rect.getY() + 1, Math.min(by, rect.getMaxY() - th - 2.0 * pad - 1));
        g2.setColor(Color.WHITE);
        g2.fill(new Rectangle2D.Double(bx, by, tw + 2.0 * pad, th + 2.0 * pad));
        g2.setColor(Color.BLACK);
        g2.drawString(text, (float) (bx + pad), (float) (by + pad + fm.getAscent()));
    }

    private static double[] niceTicks(double min, double max, int approxCount) {
        double range = max - min;
        if (!(range > 0) || !Double.isFinite(range)) {
            return new double[0];
        }
        double rough = range / Math.max(1, approxCount);
        double mag = Math.pow(10, Math.floor(Math.log10(rough)));
        double residual = rough / mag;
        double step;
        if (residual <= 1) {
            step = mag;
        } else if (residual <= 2) {
            step = 2 * mag;
        } else if (residual <= 5) {
            step = 5 * mag;
        } else {
            step = 10 * mag;
        }
        double start = Math.ceil((min - step * 1e-9) / step) * step;
        int n = (int) Math.floor((max - start) / step + 1.5);
        if (n <= 0) {
            return new double[0];
        }
        double[] ticks = new double[Math.min(n, 32)];
        int count = 0;
        for (double v = start; v <= max + step * 1e-9 && count < ticks.length; v += step) {
            ticks[count++] = v;
        }
        if (count == ticks.length) {
            return ticks;
        }
        double[] trimmed = new double[count];
        System.arraycopy(ticks, 0, trimmed, 0, count);
        return trimmed;
    }

    private static void drawYTicks(Graphics2D g2, Rectangle2D rect, Ext yExt) {
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2.setColor(AXIS);
        g2.setStroke(new BasicStroke(1f));
        FontMetrics fm = g2.getFontMetrics();
        double[] ticks = yTickValues(yExt);
        double minGap = fm.getHeight() + 1;
        double lastLabelY = Double.NaN;
        for (double y : ticks) {
            double py = mapY(rect, yExt, y);
            if (py < rect.getY() - 0.5 || py > rect.getMaxY() + 0.5) {
                continue;
            }
            g2.draw(new Line2D.Double(rect.getX() - 4, py, rect.getX(), py));
            if (!Double.isNaN(lastLabelY) && Math.abs(py - lastLabelY) < minGap) {
                continue;
            }
            String lab = fmt(y);
            float textY = (float) (py + fm.getAscent() * 0.35);
            textY = (float) Math.max(rect.getY() + fm.getAscent() - 2,
                    Math.min(textY, rect.getMaxY() - 1));
            g2.drawString(lab, (float) (rect.getX() - 6 - fm.stringWidth(lab)), textY);
            lastLabelY = py;
        }
    }

    /** Nice Y ticks for the padded extent; always includes 0 when the range crosses it. */
    private static double[] yTickValues(Ext yExt) {
        double[] ticks = niceTicks(yExt.min, yExt.max, Y_TICK_TARGET);
        if (!(yExt.min < 0 && yExt.max > 0)) {
            return ticks;
        }
        for (double t : ticks) {
            if (Math.abs(t) < 1e-12) {
                return ticks;
            }
        }
        double[] withZero = new double[ticks.length + 1];
        int i = 0;
        boolean inserted = false;
        for (double t : ticks) {
            if (!inserted && t > 0) {
                withZero[i++] = 0;
                inserted = true;
            }
            withZero[i++] = t;
        }
        if (!inserted) {
            withZero[i++] = 0;
        }
        if (i == withZero.length) {
            return withZero;
        }
        double[] trimmed = new double[i];
        System.arraycopy(withZero, 0, trimmed, 0, i);
        return trimmed;
    }

    private static void drawRotatedLabel(Graphics2D g2, String text, double x, double y) {
        AffineTransform old = g2.getTransform();
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.setColor(AXIS);
        g2.translate(x, y);
        g2.rotate(-Math.PI / 2);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, -fm.stringWidth(text) / 2f, 0);
        g2.setTransform(old);
    }

    /**
     * Annotation box beside a guide line. When {@code besideLine}, flips to the left if it would
     * overflow {@code plotMaxX} (matches enplot/viewer.js).
     */
    private static void drawLabelBox(Graphics2D g2, String text, double lineX, double anchorY,
            boolean below, double plotMaxX, boolean besideLine) {
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics fm = g2.getFontMetrics();
        String[] lines = text.split("\n");
        int lineH = 14;
        int padX = 5, padY = 3;
        int maxTw = 0;
        for (String line : lines) {
            maxTw = Math.max(maxTw, fm.stringWidth(line));
        }
        int bw = maxTw + padX * 2;
        int bh = lineH * lines.length + padY * 2;
        double gap = 8;
        double bx = lineX + gap;
        if (besideLine && bx + bw > plotMaxX - 2) {
            bx = lineX - gap - bw;
        }
        bx = Math.max(PAD_L, Math.min(bx, plotMaxX - bw));
        double by = below ? anchorY + 4 : anchorY - bh - 4;
        g2.setColor(ANN_BG);
        g2.fill(new Rectangle2D.Double(bx, by, bw, bh));
        g2.setColor(GRID);
        g2.draw(new Rectangle2D.Double(bx, by, bw, bh));
        g2.setColor(INK);
        for (int i = 0; i < lines.length; i++) {
            g2.drawString(lines[i], (float) (bx + padX), (float) (by + padY + fm.getAscent() + i * lineH));
        }
    }

    private static double mapX(Rectangle2D rect, int listSize, double rank) {
        return rect.getX() + (rank / (double) Math.max(listSize - 1, 1)) * rect.getWidth();
    }

    private static double mapY(Rectangle2D rect, Ext yExt, double y) {
        return rect.getY() + rect.getHeight() - (y - yExt.min) / Math.max(1e-9, yExt.max - yExt.min) * rect.getHeight();
    }

    private static Ext extent(double[] y, double padFrac) {
        if (y == null || y.length == 0) {
            return new Ext(-1, 1);
        }
        double min = y[0];
        double max = y[0];
        for (double v : y) {
            if (!Double.isFinite(v)) {
                continue;
            }
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        if (!Double.isFinite(min) || !Double.isFinite(max)) {
            return new Ext(-1, 1);
        }
        if (min == max) {
            min -= 0.1;
            max += 0.1;
        }
        double pad = (max - min) * padFrac;
        return new Ext(min - pad, max + pad);
    }

    private static Color lerp(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = Math.round(a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(r, g, bl);
    }

    private static String fmt(double v) {
        if (!Double.isFinite(v)) {
            return "—";
        }
        return String.format("%.2f", v);
    }

    private static final class Ext {
        final double min;
        final double max;

        Ext(double min, double max) {
            this.min = min;
            this.max = max;
        }
    }
}
