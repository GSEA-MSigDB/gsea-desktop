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
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import org.genepattern.io.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Headless Java2D renderer for {@link PlotSpec} (CLI/report PNG/SVG).
 * Margin constants are shared with {@code FxPlotPane} hit-testing.
 */
public final class PlotRasterExporter {

    private static final Logger klog = LoggerFactory.getLogger(PlotRasterExporter.class);

    public static final int MARGIN_L = 56;
    public static final int MARGIN_R = 20;
    public static final int MARGIN_T = 36;
    public static final int MARGIN_B = 48;

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font TITLE_FONT_LARGE = new Font("SansSerif", Font.BOLD, 16);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 11);
    private static final Font TICK_FONT = new Font("SansSerif", Font.PLAIN, 9);
    private static final Color GRID = Color.LIGHT_GRAY;
    private static final Color AXIS = new Color(0x64, 0x74, 0x8B);
    private static final Color INK = new Color(0x0F, 0x17, 0x2A);
    private static final Color MARKER = new Color(0xDC, 0x26, 0x26);

    private PlotRasterExporter() {
    }

    public static BufferedImage render(PlotSpec spec, int width, int height) {
        int w = Math.max(80, width);
        int h = Math.max(60, height);
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            draw(spec, g2, w, h);
        } finally {
            g2.dispose();
        }
        return bi;
    }

    public static void draw(PlotSpec spec, Graphics2D g2, int width, int height) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);
        if (spec == null) {
            return;
        }
        if (spec instanceof HistogramPlotSpec hist) {
            drawHistogram(hist, g2, width, height);
        } else if (spec instanceof XyPlotSpec xy) {
            drawXy(xy, g2, width, height);
        } else if (spec instanceof BubblePlotSpec bubble) {
            drawBubble(bubble, g2, width, height);
        } else if (spec instanceof MountainPlotSpec mountain) {
            MountainPlotSpec.draw(mountain, g2, width, height);
        } else {
            g2.setColor(INK);
            g2.setFont(TITLE_FONT);
            g2.drawString(spec.title() != null ? spec.title() : spec.type(), 20, 30);
        }
    }

    public static void savePng(PlotSpec spec, File file, int width, int height) throws IOException {
        BufferedImage bi = render(spec, width, height);
        ImageIO.write(bi, "png", file);
        try {
            PlotJson.writeAlongsidePng(spec, file);
        } catch (IOException e) {
            klog.warn("Could not dual-write PlotSpec JSON beside {}: {}", file, e.getMessage());
        }
    }

    public static void saveSvg(PlotSpec spec, File file, int width, int height) throws IOException {
        Consumer<Graphics2D> drawer = g2 -> draw(spec, g2, width, height);
        ImageUtil.saveAsSVG(drawer, file, width, height, true);
    }

    /** Plot rectangle using default margins (for FX hit-testing). */
    public static Rectangle2D plotArea(int width, int height) {
        return plotArea(width, height, MARGIN_L, MARGIN_R, MARGIN_B);
    }

    public static Rectangle2D plotArea(int width, int height, int leftMargin) {
        return plotArea(width, height, leftMargin, MARGIN_R, MARGIN_B);
    }

    public static Rectangle2D plotArea(int width, int height, int leftMargin, int rightMargin, int bottomMargin) {
        int left = Math.max(MARGIN_L, leftMargin);
        int right = Math.max(MARGIN_R, rightMargin);
        int bottom = Math.max(MARGIN_B, bottomMargin);
        return new Rectangle2D.Double(left, MARGIN_T,
                Math.max(10, width - left - right),
                Math.max(10, height - MARGIN_T - bottom));
    }

    private static void drawHistogram(HistogramPlotSpec spec, Graphics2D g2, int width, int height) {
        drawTitle(g2, spec.title(), width, MARGIN_L, false);
        int n = spec.size();
        Rectangle2D plot = plotArea(width, height);
        double[] y = spec.y();
        double yMax = 1;
        for (double v : y) {
            yMax = Math.max(yMax, v);
        }
        drawGridAndAxes(g2, plot, 0, Math.max(n - 1, 1), 0, yMax, spec.xLabel(), spec.yLabel(), true, true);
        if (n == 0) {
            drawEmptyMessage(g2, plot, spec.caption());
            return;
        }
        double barW = plot.getWidth() / n;
        int[] colors = spec.barArgb();
        int selected = spec.selectedIndex();
        for (int i = 0; i < n; i++) {
            double yh = yMax > 0 ? (y[i] / yMax) * plot.getHeight() : 0;
            double x = plot.getX() + i * barW;
            double yy = plot.getY() + plot.getHeight() - yh;
            Color c = new Color(0x0D, 0x94, 0x88);
            if (colors != null && i < colors.length && colors[i] != 0) {
                c = new Color(colors[i], true);
            }
            if (i == selected) {
                c = Color.YELLOW;
            }
            g2.setColor(c);
            g2.fill(new Rectangle2D.Double(x + 1, yy, Math.max(1, barW - 2), yh));
        }
        if (spec.hasMarker()) {
            double mx = mapX(plot, 0, Math.max(n - 1, 1), spec.markerX());
            g2.setColor(MARKER);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new Line2D.Double(mx, plot.getY(), mx, plot.getMaxY()));
        }
        maybeDrawCategoryTicks(g2, plot, spec.categoryLabels(), n);
    }

    private static void drawXy(XyPlotSpec spec, Graphics2D g2, int width, int height) {
        List<XyPlotSpec.Series> series = spec.series();
        // Room for x ticks/label; legend band scales with series count (butterfly has many).
        int legendExtra = 0;
        if (spec.legend() && spec.legendBottom()) {
            if (spec.hasSecondaryAxis() && series.size() == 2) {
                legendExtra = 28; // NES: single L/R pinned row
            } else {
                int rows = Math.max(1, (series.size() + 2) / 3);
                legendExtra = 16 + rows * 14;
            }
        }
        int bottom = MARGIN_B + legendExtra;
        int left = Math.max(MARGIN_L, 64); // room for rotated Y label
        int right = spec.hasSecondaryAxis() ? Math.max(80, spec.marginR()) : Math.max(MARGIN_R, spec.marginR());
        boolean largeTitle = "butterfly_plot".equals(spec.name());
        drawTitle(g2, spec.title(), width, left, largeTitle);
        Rectangle2D plot = plotArea(width, height, left, right, bottom);

        double xMin = Double.POSITIVE_INFINITY, xMax = Double.NEGATIVE_INFINITY;
        double yMin = Double.POSITIVE_INFINITY, yMax = Double.NEGATIVE_INFINITY;
        double y2Min = Double.POSITIVE_INFINITY, y2Max = Double.NEGATIVE_INFINITY;
        for (XyPlotSpec.Series s : series) {
            for (int i = 0; i < s.x.length; i++) {
                xMin = Math.min(xMin, s.x[i]);
                xMax = Math.max(xMax, s.x[i]);
                double yv = i < s.y.length ? s.y[i] : 0;
                if (s.yAxis == 1) {
                    y2Min = Math.min(y2Min, yv);
                    y2Max = Math.max(y2Max, yv);
                } else {
                    yMin = Math.min(yMin, yv);
                    yMax = Math.max(yMax, yv);
                }
            }
        }
        if (!Double.isFinite(xMin)) {
            xMin = 0;
            xMax = 1;
            yMin = 0;
            yMax = 1;
            drawGridAndAxes(g2, plot, xMin, xMax, yMin, yMax, spec.xLabel(), spec.yLabel(), true, true);
            drawEmptyMessage(g2, plot, spec.caption());
            return;
        }
        if (xMax <= xMin) {
            xMax = xMin + 1;
        }
        if (!Double.isFinite(yMin) || yMax <= yMin) {
            yMin = 0;
            yMax = 1;
        }
        if (spec.hasSecondaryAxis()) {
            if (!Double.isFinite(y2Min) || y2Max <= y2Min) {
                y2Min = 0;
                y2Max = 1;
            }
            // Include zero; keep a modest pad — do not force both axes to 1.
            yMin = Math.min(0, yMin);
            yMax = yMax + (yMax - yMin) * 0.05;
            if (yMax <= yMin) {
                yMax = yMin + 1;
            }
            y2Min = Math.min(0, y2Min);
            y2Max = y2Max + (y2Max - y2Min) * 0.05;
            if (y2Max <= y2Min) {
                y2Max = y2Min + 1;
            }
        } else if (yMax <= yMin) {
            yMax = yMin + 1;
        } else {
            double pad = (yMax - yMin) * 0.05;
            yMin -= pad;
            yMax += pad;
        }

        if (spec.hasYBand()) {
            double by0 = mapY(plot, yMin, yMax, spec.bandYMax());
            double by1 = mapY(plot, yMin, yMax, spec.bandYMin());
            g2.setColor(new Color(spec.bandArgb() != 0 ? spec.bandArgb() : 0xFFFFFFD2, true));
            g2.fill(new Rectangle2D.Double(plot.getX(), by0, plot.getWidth(), Math.max(1, by1 - by0)));
        }

        drawGridAndAxes(g2, plot, xMin, xMax, yMin, yMax, spec.xLabel(), spec.yLabel(), true, !spec.integerYTicks());
        if (spec.integerYTicks()) {
            drawIntegerYTicks(g2, plot, yMin, yMax);
        }
        if (spec.hasSecondaryAxis()) {
            drawSecondaryYAxis(g2, plot, y2Min, y2Max, spec.y2Label());
        }

        for (XyPlotSpec.Series s : series) {
            Color c = new Color(s.argb != 0 ? s.argb : 0xFF0D9488, true);
            g2.setColor(c);
            g2.setStroke(new BasicStroke(1.5f));
            double ymin = s.yAxis == 1 ? y2Min : yMin;
            double ymax = s.yAxis == 1 ? y2Max : yMax;
            for (int i = 0; i < s.x.length; i++) {
                double px = mapX(plot, xMin, xMax, s.x[i]);
                double py = mapY(plot, ymin, ymax, i < s.y.length ? s.y[i] : 0);
                if (s.lines && i > 0) {
                    double px0 = mapX(plot, xMin, xMax, s.x[i - 1]);
                    double py0 = mapY(plot, ymin, ymax, i - 1 < s.y.length ? s.y[i - 1] : 0);
                    g2.draw(new Line2D.Double(px0, py0, px, py));
                }
                if (s.shapes) {
                    // Historic butterfly observed markers ~ Ellipse2D.Float(2,2,2,2).
                    double d = 2.5;
                    g2.fill(new Ellipse2D.Double(px - d / 2, py - d / 2, d, d));
                }
            }
        }

        for (XyPlotSpec.Annotation a : spec.annotations()) {
            drawXyAnnotation(g2, plot, xMin, xMax, yMin, yMax, a);
        }

        if (spec.legend() && !series.isEmpty()) {
            drawXyLegend(g2, plot, series, spec.legendBottom(), spec.hasSecondaryAxis(), height);
        }
        maybeDrawCategoryTicks(g2, plot, spec.categoryLabels(),
                spec.categoryLabels() != null ? spec.categoryLabels().length : 0);
    }

    private static void drawXyAnnotation(Graphics2D g2, Rectangle2D plot,
            double xMin, double xMax, double yMin, double yMax, XyPlotSpec.Annotation a) {
        if (a.text.isBlank()) {
            return;
        }
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        FontMetrics fm = g2.getFontMetrics();
        double py = mapY(plot, yMin, yMax, a.y);
        float tx;
        String align = a.align != null ? a.align : "left";
        if ("pinLeft".equalsIgnoreCase(align)) {
            tx = (float) (plot.getX() + 4);
        } else if ("pinRight".equalsIgnoreCase(align)) {
            tx = (float) (plot.getMaxX() - 4 - fm.stringWidth(a.text));
        } else {
            double px = mapX(plot, xMin, xMax, a.x);
            tx = (float) px;
            if ("right".equalsIgnoreCase(align)) {
                tx = (float) (px - fm.stringWidth(a.text));
            } else if ("center".equalsIgnoreCase(align)) {
                tx = (float) (px - fm.stringWidth(a.text) / 2.0);
            }
            tx = (float) Math.max(plot.getX() + 2, Math.min(tx, plot.getMaxX() - fm.stringWidth(a.text) - 2));
        }
        float ty = (float) (py + fm.getAscent() / 2.0 - 1);
        ty = (float) Math.max(plot.getY() + fm.getAscent(), Math.min(ty, plot.getMaxY() - 2));
        if (a.whiteBg) {
            int pad = 2;
            g2.setColor(Color.WHITE);
            g2.fill(new Rectangle2D.Double(tx - pad, ty - fm.getAscent() - pad + 2,
                    fm.stringWidth(a.text) + 2.0 * pad, fm.getHeight() + pad));
        }
        g2.setColor(new Color(a.argb != 0 ? a.argb : 0xFF000000, true));
        g2.drawString(a.text, tx, ty);
    }

    private static void drawXyLegend(Graphics2D g2, Rectangle2D plot, List<XyPlotSpec.Series> series,
            boolean bottom, boolean dualAxis, int height) {
        g2.setFont(TICK_FONT);
        FontMetrics fm = g2.getFontMetrics();
        if (bottom && dualAxis && series.size() == 2) {
            // Historic NES: FDR legend LEFT, NOM-p legend RIGHT.
            int ly = height - 12;
            XyPlotSpec.Series left = series.get(0);
            XyPlotSpec.Series right = series.get(1);
            int lx = (int) plot.getX();
            g2.setColor(new Color(left.argb != 0 ? left.argb : 0xFF0D9488, true));
            g2.fillRect(lx, ly - 8, 10, 10);
            g2.setColor(INK);
            g2.drawString(left.name, lx + 14, ly);
            int rw = 10 + 4 + fm.stringWidth(right.name);
            int rx = (int) plot.getMaxX() - rw;
            g2.setColor(new Color(right.argb != 0 ? right.argb : 0xFF0D9488, true));
            g2.fillRect(rx, ly - 8, 10, 10);
            g2.setColor(INK);
            g2.drawString(right.name, rx + 14, ly);
            return;
        }
        if (bottom) {
            int ly = height - 12;
            int lx = (int) plot.getX();
            int maxX = (int) plot.getMaxX();
            for (XyPlotSpec.Series s : series) {
                int need = 14 + fm.stringWidth(s.name) + 16;
                if (lx + need > maxX && lx > plot.getX() + 1) {
                    lx = (int) plot.getX();
                    ly -= 14;
                }
                g2.setColor(new Color(s.argb != 0 ? s.argb : 0xFF0D9488, true));
                g2.fillRect(lx, ly - 8, 10, 10);
                g2.setColor(INK);
                g2.drawString(s.name, lx + 14, ly);
                lx += need;
            }
        } else {
            int lx = (int) plot.getX() + 8;
            int ly = (int) plot.getY() + 14;
            for (XyPlotSpec.Series s : series) {
                g2.setColor(new Color(s.argb != 0 ? s.argb : 0xFF0D9488, true));
                g2.fillRect(lx, ly - 8, 10, 10);
                g2.setColor(INK);
                g2.drawString(s.name, lx + 14, ly);
                ly += 14;
            }
        }
    }

    private static void drawSecondaryYAxis(Graphics2D g2, Rectangle2D plot, double yMin, double yMax, String label) {
        g2.setColor(AXIS);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new Line2D.Double(plot.getMaxX(), plot.getY(), plot.getMaxX(), plot.getMaxY()));
        g2.setFont(TICK_FONT);
        FontMetrics fm = g2.getFontMetrics();
        double[] ticks = niceTicks(yMin, yMax, 5);
        double minGap = fm.getHeight();
        double lastPy = Double.NaN;
        for (double y : ticks) {
            double py = mapY(plot, yMin, yMax, y);
            if (!Double.isNaN(lastPy) && Math.abs(py - lastPy) < minGap) {
                continue;
            }
            String lab = formatTick(y);
            g2.drawString(lab, (float) (plot.getMaxX() + 4), (float) (py + 3));
            lastPy = py;
        }
        if (label != null && !label.isBlank()) {
            AffineTransform old = g2.getTransform();
            g2.setFont(LABEL_FONT);
            g2.setColor(INK);
            FontMetrics lfm = g2.getFontMetrics();
            g2.translate(plot.getMaxX() + 36, plot.getCenterY());
            g2.rotate(Math.PI / 2);
            g2.drawString(label, -lfm.stringWidth(label) / 2f, 0);
            g2.setTransform(old);
        }
    }

    private static void drawIntegerYTicks(Graphics2D g2, Rectangle2D plot, double yMin, double yMax) {
        g2.setFont(TICK_FONT);
        g2.setColor(AXIS);
        FontMetrics fm = g2.getFontMetrics();
        int lo = (int) Math.ceil(yMin);
        int hi = (int) Math.floor(yMax);
        int step = Math.max(1, (hi - lo) / 8);
        for (int v = lo; v <= hi; v += step) {
            double py = mapY(plot, yMin, yMax, v);
            String lab = String.valueOf(v);
            g2.drawString(lab, (float) (plot.getX() - 6 - fm.stringWidth(lab)), (float) (py + 3));
        }
    }

    private static void drawBubble(BubblePlotSpec spec, Graphics2D g2, int width, int height) {
        BubblePlotSpec.Options opt = spec.options();
        int left = Math.max(MARGIN_L, spec.leftMarginPx());
        int bottom = MARGIN_B + opt.bottomExtraPx;
        // Historic chart frame was light gray.
        g2.setColor(new Color(0xf2, 0xf2, 0xf2));
        g2.fillRect(0, 0, width, height);
        drawTitle(g2, spec.title(), width, left, false);
        Rectangle2D plot = plotArea(width, height, left, MARGIN_R, bottom);
        g2.setColor(Color.WHITE);
        g2.fill(plot);

        BubblePlotSpec.Point[] pts = spec.points();
        if (pts.length == 0) {
            drawGridAndAxes(g2, plot, 0, 1, 0, 1, spec.xLabel(), "", true, false);
            drawEmptyMessage(g2, plot, spec.caption());
            return;
        }

        double dataXMin = Double.POSITIVE_INFINITY;
        double dataXMax = Double.NEGATIVE_INFINITY;
        double yMin = Double.POSITIVE_INFINITY;
        double yMax = Double.NEGATIVE_INFINITY;
        for (BubblePlotSpec.Point p : pts) {
            dataXMin = Math.min(dataXMin, p.x);
            dataXMax = Math.max(dataXMax, p.x);
            yMin = Math.min(yMin, p.y);
            yMax = Math.max(yMax, p.y);
        }
        double xMin = Double.isFinite(opt.xMin) ? opt.xMin : dataXMin;
        double xMax = Double.isFinite(spec.xMax()) ? Math.max(dataXMax, spec.xMax()) : dataXMax;
        if (!Double.isFinite(xMax) || xMax <= xMin) {
            xMax = xMin + 1;
        }
        if (yMax <= yMin) {
            yMax = yMin + 1;
        }
        // Historic SymbolAxis: ~1% lower / ~24% upper margin.
        double ySpan = yMax - yMin;
        yMin -= ySpan * 0.01;
        yMax += ySpan * 0.24;

        drawGridAndAxes(g2, plot, xMin, xMax, yMin, yMax, spec.xLabel(), "", true, false);

        if (opt.zeroLine && xMin < 0 && xMax > 0) {
            double zx = mapX(plot, xMin, xMax, 0);
            g2.setColor(Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new Line2D.Double(zx, plot.getY(), zx, plot.getMaxY()));
        }

        // NOM-p (or other) vertical threshold markers — historic dash {4,4}, BOTTOM_RIGHT labels.
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{4f, 4f}, 0f));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 8));
        for (int i = 0; i < opt.markerXs.length; i++) {
            double mx = opt.markerXs[i];
            if (mx < xMin || mx > xMax) {
                continue;
            }
            double px = mapX(plot, xMin, xMax, mx);
            g2.setColor(Color.BLACK);
            g2.draw(new Line2D.Double(px, plot.getY(), px, plot.getMaxY()));
            if (i < opt.markerLabels.length && opt.markerLabels[i] != null) {
                String lab = opt.markerLabels[i];
                FontMetrics mfm = g2.getFontMetrics();
                // BOTTOM_RIGHT of a point near the top of the marker line.
                float lx = (float) (px - mfm.stringWidth(lab) - 2);
                float ly = (float) (plot.getY() + mfm.getAscent() + 2);
                g2.drawString(lab, lx, ly);
            }
        }
        g2.setStroke(new BasicStroke(1f));

        // Draw bubbles with a slightly inflated clip so edge markers aren't hard-cut.
        Shape oldClip = g2.getClip();
        g2.setClip(new Rectangle2D.Double(plot.getX() - 12, plot.getY() - 12,
                plot.getWidth() + 24, plot.getHeight() + 24));

        g2.setFont(labelFontForRows(pts.length));
        FontMetrics fm = g2.getFontMetrics();
        for (BubblePlotSpec.Point p : pts) {
            double px = mapX(plot, xMin, xMax, p.x);
            double py = mapY(plot, yMin, yMax, p.y);
            double r = opt.sizeAbsolutePx
                    ? Math.max(4, p.size)
                    : (4 + 18 * Math.max(0, Math.min(1, p.size)));
            Color c = new Color(p.argb != 0 ? p.argb : 0x880D9488, true);
            g2.setColor(c);
            g2.fill(new Ellipse2D.Double(px - r / 2, py - r / 2, r, r));
            g2.setColor(new Color(0x55, 0x55, 0x55));
            g2.draw(new Ellipse2D.Double(px - r / 2, py - r / 2, r, r));

            if (p.star != null && !p.star.isBlank()) {
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                g2.setColor(INK);
                // Historic OUTSIDE1 / BOTTOM_LEFT near upper-right of ellipse.
                g2.drawString(p.star, (float) (px + r / 2 + 2), (float) (py - r / 2 + 8));
                g2.setFont(labelFontForRows(pts.length));
            }
        }
        g2.setClip(oldClip);

        // Gene-set labels in the left margin, right-aligned to the plot (no ellipsis).
        g2.setFont(labelFontForRows(pts.length));
        fm = g2.getFontMetrics();
        g2.setColor(INK);
        for (BubblePlotSpec.Point p : pts) {
            if (p.label == null || p.label.isBlank()) {
                continue;
            }
            double py = mapY(plot, yMin, yMax, p.y);
            float lx = (float) (plot.getX() - 6 - fm.stringWidth(p.label));
            lx = Math.max(2, lx);
            g2.drawString(p.label, lx, (float) (py + fm.getAscent() / 2.0 - 1));
        }

        if (opt.gseaLegends) {
            drawGseaBubbleLegends(g2, plot, width, height, bottom, opt.positiveNes, opt.nesNorm);
        }
    }

    private static void drawGseaBubbleLegends(Graphics2D g2, Rectangle2D plot, int width, int height,
            int bottomMargin, boolean positiveNes, double nesNorm) {
        // Sit below the x-axis title (drawn at plotMaxY+28).
        int legendTop = (int) plot.getMaxY() + 36;
        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
        FontMetrics fm = g2.getFontMetrics();

        // |NES| size legend (left)
        double norm = Math.max(nesNorm, 1e-9);
        double[] refs = {0.5 * norm, 0.75 * norm, norm};
        int lx = (int) plot.getX();
        int cy = legendTop + 14;
        for (double ref : refs) {
            double t = Math.min(1.0, ref / norm);
            double d = 8.0 + 10.0 * t;
            g2.setColor(new Color(0xAA, 0xAA, 0xAA));
            g2.fill(new Ellipse2D.Double(lx, cy - d / 2, d, d));
            g2.setColor(new Color(0x66, 0x66, 0x66));
            g2.draw(new Ellipse2D.Double(lx, cy - d / 2, d, d));
            g2.setColor(INK);
            String lab = "|NES| = " + String.format("%.2f", ref);
            g2.drawString(lab, lx + (int) d + 4, cy + 3);
            lx += (int) d + 4 + fm.stringWidth(lab) + 14;
        }

        // FDR gradient (right) + stars note
        int barW = 240;
        int barH = 10;
        int barX = width - MARGIN_R - barW - 8;
        int barY = legendTop + 4;
        g2.setColor(INK);
        String heading = "FDR q-val";
        g2.drawString(heading, barX + (barW - fm.stringWidth(heading)) / 2, barY - 2);
        for (int x = 0; x < barW; x++) {
            double t = x / (double) Math.max(1, barW - 1);
            float fdr = (float) (0.30d * (1.0d - t));
            g2.setColor(BubblePlotSpec.fdrPaint(fdr, positiveNes));
            g2.drawLine(barX + x, barY, barX + x, barY + barH - 1);
        }
        g2.setColor(new Color(0x66, 0x66, 0x66));
        g2.drawRect(barX, barY, barW, barH);
        g2.setColor(INK);
        g2.drawString("0.30", barX, barY + barH + fm.getAscent() + 2);
        String lo = "0.00";
        g2.drawString(lo, barX + barW - fm.stringWidth(lo), barY + barH + fm.getAscent() + 2);
        g2.drawString("* FDR <= 0.25     ** FDR <= 0.05     *** FDR < 0.01",
                barX, barY + barH + fm.getAscent() + 16);
    }

    private static Font labelFontForRows(int n) {
        if (n > 40) {
            return new Font("SansSerif", Font.PLAIN, 7);
        }
        if (n > 25) {
            return new Font("SansSerif", Font.PLAIN, 8);
        }
        return TICK_FONT;
    }

    private static void drawEmptyMessage(Graphics2D g2, Rectangle2D plot, String caption) {
        g2.setFont(LABEL_FONT);
        g2.setColor(AXIS);
        String msg = caption != null && !caption.isBlank() ? caption : "No data";
        g2.drawString(msg, (float) (plot.getX() + 8), (float) (plot.getY() + plot.getHeight() / 2));
    }

    private static void drawTitle(Graphics2D g2, String title, int width, int leftMargin, boolean large) {
        if (title == null || title.isBlank()) {
            return;
        }
        g2.setFont(large ? TITLE_FONT_LARGE : TITLE_FONT);
        g2.setColor(INK);
        g2.drawString(title, leftMargin, large ? 24 : 22);
    }

    private static void drawGridAndAxes(Graphics2D g2, Rectangle2D plot,
            double xMin, double xMax, double yMin, double yMax, String xLabel, String yLabel,
            boolean drawXTicks, boolean drawYTicks) {
        g2.setColor(GRID);
        g2.setStroke(new BasicStroke(1f));
        for (int i = 0; i <= 4; i++) {
            double yy = plot.getY() + plot.getHeight() * i / 4.0;
            g2.draw(new Line2D.Double(plot.getX(), yy, plot.getMaxX(), yy));
            double xx = plot.getX() + plot.getWidth() * i / 4.0;
            g2.draw(new Line2D.Double(xx, plot.getY(), xx, plot.getMaxY()));
        }
        g2.setColor(AXIS);
        g2.draw(new Rectangle2D.Double(plot.getX(), plot.getY(), plot.getWidth(), plot.getHeight()));
        g2.setFont(LABEL_FONT);
        g2.setColor(INK);
        if (xLabel != null && !xLabel.isBlank()) {
            FontMetrics fm = g2.getFontMetrics();
            float tx = (float) (plot.getX() + (plot.getWidth() - fm.stringWidth(xLabel)) / 2.0);
            g2.drawString(xLabel, tx, (float) (plot.getMaxY() + 28));
        }
        if (yLabel != null && !yLabel.isBlank()) {
            AffineTransform old = g2.getTransform();
            g2.translate(14, plot.getCenterY());
            g2.rotate(-Math.PI / 2);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(yLabel, -fm.stringWidth(yLabel) / 2f, 0);
            g2.setTransform(old);
        }
        g2.setFont(TICK_FONT);
        g2.setColor(AXIS);
        FontMetrics tfm = g2.getFontMetrics();
        if (drawYTicks) {
            double[] ticks = niceTicks(yMin, yMax, 5);
            double minGap = tfm.getHeight();
            double lastPy = Double.NaN;
            for (double y : ticks) {
                double py = mapY(plot, yMin, yMax, y);
                if (!Double.isNaN(lastPy) && Math.abs(py - lastPy) < minGap) {
                    continue;
                }
                String lab = formatTick(y);
                g2.drawString(lab, (float) (plot.getX() - 6 - tfm.stringWidth(lab)), (float) (py + 3));
                lastPy = py;
            }
        }
        if (drawXTicks) {
            for (int i = 0; i <= 4; i++) {
                double xv = xMin + (xMax - xMin) * i / 4.0;
                double px = mapX(plot, xMin, xMax, xv);
                String lab = formatTick(xv);
                float tx = (float) (px - tfm.stringWidth(lab) / 2.0);
                if (i == 0) {
                    tx = (float) px;
                } else if (i == 4) {
                    tx = (float) (px - tfm.stringWidth(lab));
                }
                g2.drawString(lab, tx, (float) (plot.getMaxY() + 12));
            }
        }
    }

    private static void maybeDrawCategoryTicks(Graphics2D g2, Rectangle2D plot, String[] labels, int n) {
        if (labels == null || n <= 0) {
            return;
        }
        g2.setFont(TICK_FONT);
        g2.setColor(AXIS);
        int step = Math.max(1, n / 12);
        for (int i = 0; i < n; i += step) {
            double x = plot.getX() + (i + 0.5) * plot.getWidth() / n;
            String lab = i < labels.length && labels[i] != null ? labels[i] : "";
            if (lab.length() > 10) {
                lab = lab.substring(0, 10) + "…";
            }
            g2.drawString(lab, (float) x - 10, (float) plot.getMaxY() + 12);
        }
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

    private static double mapX(Rectangle2D plot, double xMin, double xMax, double x) {
        return plot.getX() + (x - xMin) / (xMax - xMin) * plot.getWidth();
    }

    private static double mapY(Rectangle2D plot, double yMin, double yMax, double y) {
        return plot.getY() + plot.getHeight() - (y - yMin) / (yMax - yMin) * plot.getHeight();
    }

    private static String formatTick(double v) {
        if (Math.abs(v) >= 100 || Math.abs(v) < 0.01 && v != 0) {
            return String.format("%.2g", v);
        }
        return String.format("%.2f", v);
    }
}
