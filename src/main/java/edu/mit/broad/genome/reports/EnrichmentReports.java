/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.reports;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.NotImplementedException;
import edu.mit.broad.genome.Printf;
import edu.mit.broad.genome.alg.DatasetGenerators;
import edu.mit.broad.genome.alg.gsea.GeneSetScoringTable;
import edu.mit.broad.genome.alg.gsea.Norms;
import edu.mit.broad.genome.alg.gsea.PValueCalculator;
import edu.mit.broad.genome.alg.gsea.PValueCalculatorImpls;
import edu.mit.broad.genome.alg.markers.PermutationTest;
import edu.mit.broad.genome.charts.*;
import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.models.XYDatasetMultiTmp;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.objects.esmatrix.db.*;
import edu.mit.broad.genome.objects.strucs.CollapsedDetails;
import edu.mit.broad.genome.parsers.AuxUtils;
import edu.mit.broad.genome.parsers.GctParser;
import edu.mit.broad.genome.reports.api.PicFile;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.genome.reports.api.ToolReport;
import edu.mit.broad.genome.reports.pages.*;
import edu.mit.broad.genome.reports.web.LinkedFactory;
import edu.mit.broad.xbench.heatmap.GramImagerImpl;
import gnu.trove.TIntFloatHashMap;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import xapps.gsea.GseaWebResources;

import org.apache.commons.io.FileUtils;
import org.apache.ecs.StringElement;
import org.apache.ecs.html.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.genepattern.io.ImageUtil;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.BorderArrangement;
import org.jfree.chart.block.EmptyBlock;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.CompositeTitle;
import org.jfree.chart.title.ImageTitle;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.DomainOrder;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.Font;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Several enrichemnt related reports
 */
public class EnrichmentReports {
    private static final double NOM_P_005_X = -Math.log10(0.05d);
    private static final double NOM_P_025_X = -Math.log10(0.25d);
    private static final double EPSILON = 1e-10d;
    // Explicit FDR legend dimensions (px) to avoid padding-based layout drift.
    private static final int FDR_LEGEND_IMAGE_WIDTH = 300;
    private static final int FDR_LEGEND_IMAGE_HEIGHT = 52;
    private static final int FDR_BAR_WIDTH = 240;
    private static final int FDR_BAR_HEIGHT = 10;
    private static final int FDR_BAR_RIGHT_PAD = 8;

    private static final class BubblePlotData {
        private final int n;
        private final String[] labels;
        private final double[] xs;
        private final double[] ys;
        private final double[] sizePx;
        private final float[] fdrs;
        private final Paint[] itemPaints;
        private final double nesNorm;

        private BubblePlotData(int n, String[] labels, double[] xs, double[] ys,
                               double[] sizePx, float[] fdrs, Paint[] itemPaints, double nesNorm) {
            this.n = n;
            this.labels = labels;
            this.xs = xs;
            this.ys = ys;
            this.sizePx = sizePx;
            this.fdrs = fdrs;
            this.itemPaints = itemPaints;
            this.nesNorm = nesNorm;
        }
    }

    private static final class BubbleCanvasSpec {
        private final double sharedMaxNegLog10NomP;
        private final double sharedLeftAxisSpacePx;
        private final double sharedNesNorm;

        private BubbleCanvasSpec(double sharedMaxNegLog10NomP, double sharedLeftAxisSpacePx, double sharedNesNorm) {
            this.sharedMaxNegLog10NomP = sharedMaxNegLog10NomP;
            this.sharedLeftAxisSpacePx = sharedLeftAxisSpacePx;
            this.sharedNesNorm = sharedNesNorm;
        }
    }

    /**
     * Single-phenotype bubble plot.
     * X = -log10(NOM p-value), bubble size = |NES|, bubble color encodes FDR intensity
     * (white->red for positive NES, white->blue for negative NES).
     */
    private static XChart createBubblePlotForGseaResults(final List<EnrichmentResult> topResults,
                                                        final String phenotypeLabel, final boolean positiveNes,
                                                        final BubbleCanvasSpec canvasSpec) {
        if (topResults.isEmpty()) {
            return createEmptyBubbleChart(positiveNes, "No finite enrichment results available for plotting");
        }

        final BubblePlotData data = buildBubblePlotData(topResults, positiveNes, canvasSpec.sharedNesNorm);
        final XYSeries series = new XYSeries("bubble-points", false, true);
        for (int i = 0; i < data.n; i++) {
            series.add(data.xs[i], data.ys[i]);
        }
        final XYSeriesCollection ds = new XYSeriesCollection(series);

        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true) {
            @Override
            public Paint getItemPaint(int series, int item) {
                return data.itemPaints[item];
            }

            @Override
            public Shape getItemShape(int series, int item) {
                final double d = data.sizePx[item];
                return new Ellipse2D.Double(-d / 2.0d, -d / 2.0d, d, d);
            }

            @Override
            public void drawItem(Graphics2D g2,
                                 XYItemRendererState state,
                                 Rectangle2D dataArea,
                                 PlotRenderingInfo info,
                                 XYPlot plot,
                                 ValueAxis domainAxis,
                                 ValueAxis rangeAxis,
                                 XYDataset dataset,
                                 int series,
                                 int item,
                                 CrosshairState crosshairState,
                                 int pass) {
                final Shape oldClip = g2.getClip();
                try {
                    // Draw bubbles above the frame clipping region so edge bubbles are not cut.
                    g2.setClip(null);
                    super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item, crosshairState, pass);
                } finally {
                    g2.setClip(oldClip);
                }
            }
        };
        renderer.setUseOutlinePaint(true);
        renderer.setSeriesOutlinePaint(0, new Color(0x55, 0x55, 0x55));
        renderer.setSeriesOutlineStroke(0, new BasicStroke(1.0f));
        renderer.setDefaultItemLabelGenerator(new XYItemLabelGenerator() {
            @Override
            public String generateLabel(XYDataset dataset, int series, int item) {
                final float fdr = data.fdrs[item];
                if (fdr < 0.01f) return "***";
                if (fdr <= 0.05f) return "**";
                if (fdr <= 0.25f) return "*";
                return null;
            }
        });
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelPaint(Color.BLACK);
        renderer.setDefaultItemLabelFont(new Font("SansSerif", Font.BOLD, 10));
        renderer.setDefaultPositiveItemLabelPosition(
                new ItemLabelPosition(ItemLabelAnchor.OUTSIDE1, TextAnchor.BOTTOM_LEFT));

        final NumberAxis xAxis = new NumberAxis("-log10(NOM p-value)");
        final SymbolAxis yAxis = new SymbolAxis("", data.labels);
        yAxis.setGridBandsVisible(false);
        yAxis.setAutoRangeIncludesZero(false);
        yAxis.setVerticalTickLabels(false);
        yAxis.setLowerMargin(0.01d);
        yAxis.setUpperMargin(0.24d);

        final XYPlot plot = new XYPlot(ds, xAxis, yAxis, renderer);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);

        final AxisSpace fixedAxisSpace = new AxisSpace();
        fixedAxisSpace.setLeft(Math.max(260.0d, canvasSpec.sharedLeftAxisSpacePx));
        plot.setFixedRangeAxisSpace(fixedAxisSpace);

        final BasicStroke thresholdStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, new float[]{4.0f, 4.0f}, 0.0f);
        plot.addDomainMarker(new ValueMarker(NOM_P_025_X, Color.BLACK, thresholdStroke));
        plot.addDomainMarker(new ValueMarker(NOM_P_005_X, Color.BLACK, thresholdStroke));

        final double lowerBound = 0.0d;
        final double upperBound = Math.max(canvasSpec.sharedMaxNegLog10NomP, NOM_P_005_X + 0.15d);
        xAxis.setRange(lowerBound, upperBound);

        final double thresholdLabelY = (data.n - 1) + 0.95d;
        final XYTextAnnotation line025 = new XYTextAnnotation("NOM p = 0.25", NOM_P_025_X, thresholdLabelY);
        line025.setTextAnchor(TextAnchor.BOTTOM_RIGHT);
        line025.setFont(new Font("SansSerif", Font.PLAIN, 8));
        line025.setPaint(Color.BLACK);
        plot.addAnnotation(line025);
        final XYTextAnnotation line005 = new XYTextAnnotation("NOM p = 0.05", NOM_P_005_X, thresholdLabelY);
        line005.setTextAnchor(TextAnchor.BOTTOM_RIGHT);
        line005.setFont(new Font("SansSerif", Font.PLAIN, 8));
        line005.setPaint(Color.BLACK);
        plot.addAnnotation(line005);
        final String phenoTitle = (phenotypeLabel != null && phenotypeLabel.length() > 0) ? phenotypeLabel : Constants.NA;
        final JFreeChart chart = new JFreeChart("Bubble plot of enrichment in phenotype: " + phenoTitle, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        chart.setBackgroundPaint(CHART_FRAME_COLOR);

        chart.addSubtitle(createBottomAlignedLegendRow(positiveNes, data.nesNorm));

        return new XChartImpl("gsea_bubble_plot_" + (positiveNes ? "pos" : "neg"),
                "Bubble plot: X=-log10(NOM p-value), size=|NES|, color=FDR intensity",
                chart);
    }

    private static XChart createEmptyBubbleChart(final boolean positiveNes, final String description) {
        JFreeChart emptyChart = ChartFactory.createScatterPlot(
                "Bubble plot of enrichment results", "-log10(NOM p-value)", "Gene set",
                new XYSeriesCollection(), PlotOrientation.VERTICAL, false, false, false);
        emptyChart.setBackgroundPaint(CHART_FRAME_COLOR);
        return new XChartImpl("gsea_bubble_plot_" + (positiveNes ? "pos" : "neg"), description, emptyChart);
    }

    private static List<EnrichmentResult> selectTopFiniteResults(final EnrichmentResult[] inputResults, final int topXSets) {
        final List<EnrichmentResult> finiteResults = new ArrayList<EnrichmentResult>();
        for (int i = 0; i < inputResults.length; i++) {
            final EnrichmentScore s = inputResults[i].getScore();
            if (Float.isFinite(s.getNES()) && Float.isFinite(s.getNP()) && Float.isFinite(s.getFDR())) {
                finiteResults.add(inputResults[i]);
            }
        }

        finiteResults.sort((a, b) -> Float.compare(Math.abs(b.getScore().getNES()), Math.abs(a.getScore().getNES())));
        final int requestedTopN = (topXSets > 0) ? topXSets : finiteResults.size();
        return finiteResults.subList(0, Math.min(requestedTopN, finiteResults.size()));
    }

    private static BubblePlotData buildBubblePlotData(final List<EnrichmentResult> topResults, final boolean positiveNes, final double sharedNesNorm) {
        final int n = topResults.size();
        final String[] labels = new String[n];
        final double[] xs = new double[n];
        final double[] ys = new double[n];
        final double[] sizePx = new double[n];
        final float[] fdrs = new float[n];
        final Paint[] itemPaints = new Paint[n];

        final double nesNorm = Math.max(sharedNesNorm, 1e-9d);

        for (int i = 0; i < n; i++) {
            final EnrichmentResult r = topResults.get(i);
            final EnrichmentScore s = r.getScore();
            final float nes = s.getNES();
            final float nomP = Math.max(s.getNP(), (float) EPSILON);
            final float fdr = Math.max(s.getFDR(), (float) EPSILON);
            final double tSize = Math.min(1.0d, Math.abs(nes) / nesNorm);

            labels[(n - 1) - i] = r.getGeneSet().getName(true);
            xs[i] = -Math.log10(nomP);
            ys[i] = (n - 1) - i;
            sizePx[i] = 8.0d + 14.0d * tSize;
            fdrs[i] = fdr;
            itemPaints[i] = createFdrPaint(fdr, positiveNes);
        }

        return new BubblePlotData(n, labels, xs, ys, sizePx, fdrs, itemPaints, nesNorm);
    }

    private static BubbleCanvasSpec computeBubbleCanvasSpec(final List<EnrichmentResult> posTop, final List<EnrichmentResult> negTop) {
        double sharedBubbleXMax = NOM_P_005_X + 0.15d;
        double sharedNesNorm = 1e-9d;
        int maxLabelLen = 8;

        for (var rows : Arrays.asList(posTop, negTop)) {
            for (int i = 0; i < rows.size(); i++) {
                final EnrichmentScore s = rows.get(i).getScore();
                final float np = Math.max(s.getNP(), (float) EPSILON);
                final double x = -Math.log10(np) + 0.15d;
                sharedBubbleXMax = Math.max(sharedBubbleXMax, x);
                sharedNesNorm = Math.max(sharedNesNorm, Math.abs(s.getNES()));

                final String nameLabel = rows.get(i).getGeneSet().getName(true);
                if (nameLabel != null) {
                    maxLabelLen = Math.max(maxLabelLen, nameLabel.length());
                }
            }
        }

        final double sharedLeftAxisSpacePx = Math.min(760.0d, Math.max(260.0d, (maxLabelLen * 8.2d) + 70.0d));
        return new BubbleCanvasSpec(sharedBubbleXMax, sharedLeftAxisSpacePx, sharedNesNorm);
    }

    private static Paint createFdrPaint(final float fdr, final boolean positiveNes) {
        if (positiveNes) {
            if (fdr < 0.01f) return new Color(255, 20, 20);
            if (fdr <= 0.05f) return new Color(255, 70, 70);
            if (fdr <= 0.25f) return new Color(255, 140, 140);
            return new Color(255, 220, 220);
        }
        if (fdr < 0.01f) return new Color(20, 20, 255);
        if (fdr <= 0.05f) return new Color(70, 70, 255);
        if (fdr <= 0.25f) return new Color(140, 140, 255);
        return new Color(220, 220, 255);
    }

    private static ImageTitle createColorGradientLegend(final boolean positiveNes) {
        final BufferedImage img = new BufferedImage(FDR_LEGEND_IMAGE_WIDTH, FDR_LEGEND_IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, FDR_LEGEND_IMAGE_WIDTH, FDR_LEGEND_IMAGE_HEIGHT);

            final int barX = FDR_LEGEND_IMAGE_WIDTH - FDR_BAR_WIDTH - FDR_BAR_RIGHT_PAD;
            final int barY = 18;

            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.PLAIN, 9));
            final FontMetrics fm = g.getFontMetrics();
            final String fdrHeading = "FDR q-val";
            final int headingW = fm.stringWidth(fdrHeading);
            final int headingX = barX + (FDR_BAR_WIDTH - headingW) / 2;
            g.drawString(fdrHeading, headingX, barY - 4);

            for (int x = 0; x < FDR_BAR_WIDTH; x++) {
                final double t = (double) x / (double) Math.max(1, (FDR_BAR_WIDTH - 1));
                final float fdr = (float) (0.30d * (1.0d - t));
                g.setPaint(createFdrPaint(fdr, positiveNes));
                g.drawLine(barX + x, barY, barX + x, barY + FDR_BAR_HEIGHT - 1);
            }
            g.setColor(new Color(0x66, 0x66, 0x66));
            g.drawRect(barX, barY, FDR_BAR_WIDTH, FDR_BAR_HEIGHT);

            g.setColor(Color.BLACK);
            final int tickY = barY + FDR_BAR_HEIGHT + fm.getAscent() + 2;
            final String tickHi = "0.30";
            final String tickLo = "0.00";
            g.drawString(tickHi, barX, tickY);
            g.drawString(tickLo, barX + FDR_BAR_WIDTH - fm.stringWidth(tickLo), tickY);
        } finally {
            g.dispose();
        }

        final ImageTitle legend = new ImageTitle(img);
        legend.setPosition(RectangleEdge.BOTTOM);
        legend.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        return legend;
    }

    private static TextTitle createFdrStarsSubtitle() {
        final TextTitle starsNote = new TextTitle("* FDR <= 0.25     ** FDR <= 0.05     *** FDR < 0.01");
        starsNote.setFont(new Font("SansSerif", Font.PLAIN, 9));
        starsNote.setPosition(RectangleEdge.BOTTOM);
        starsNote.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        return starsNote;
    }

    private static CompositeTitle createBottomAlignedLegendRow(final boolean positiveNes, final double nesNorm) {
        final LegendTitle nesLegend = createSizeLegendTitle(nesNorm);
        nesLegend.setHorizontalAlignment(HorizontalAlignment.LEFT);

        final TextTitle starsNote = createFdrStarsSubtitle();
        final ImageTitle fdrLegend = createColorGradientLegend(positiveNes);

        final BlockContainer rightBlock = new BlockContainer(new BorderArrangement());
        rightBlock.add(fdrLegend, RectangleEdge.TOP);
        rightBlock.add(starsNote, RectangleEdge.BOTTOM);

        final BlockContainer row = new BlockContainer(new BorderArrangement());
        // Same pattern as createNESvsSignificancePlot: LEFT + RIGHT + wide EmptyBlock pins both to panel edges.
        row.add(nesLegend, RectangleEdge.LEFT);
        row.add(rightBlock, RectangleEdge.RIGHT);
        row.add(new EmptyBlock(2000, 0));

        final CompositeTitle legends = new CompositeTitle(row);
        legends.setPosition(RectangleEdge.BOTTOM);
        legends.setHorizontalAlignment(HorizontalAlignment.LEFT);
        return legends;
    }

    private static LegendTitle createSizeLegendTitle(final double nesNorm) {
        final LegendItemCollection sizeLegendItems = new LegendItemCollection();
        final double[] nesRefs = new double[]{0.5d * nesNorm, 0.75d * nesNorm, nesNorm};
        for (int i = 0; i < nesRefs.length; i++) {
            final double t = Math.min(1.0d, nesRefs[i] / nesNorm);
            final double d = 8.0d + 10.0d * t;
            final double half = d / 2.0d;
            sizeLegendItems.add(new LegendItem("|NES| = " + Printf.format((float) nesRefs[i], 2), null, null, null,
                    new Ellipse2D.Double(-half, -half, d, d),
                    new Color(0xAA, 0xAA, 0xAA),
                    new BasicStroke(1.0f),
                    new Color(0x66, 0x66, 0x66)));
        }
        final LegendTitle legend = new LegendTitle(new LegendItemSource() {
            @Override
            public LegendItemCollection getLegendItems() {
                return sizeLegendItems;
            }
        });
        legend.setItemFont(new Font("SansSerif", Font.PLAIN, 9));
        legend.setPosition(RectangleEdge.BOTTOM);
        legend.setHorizontalAlignment(HorizontalAlignment.LEFT);
        return legend;
    }

    protected static final transient Logger klog = LoggerFactory.getLogger(EnrichmentReports.class);

    public static Shape createCircleShape() {
        return new Ellipse2D.Float(2f, 2f, 2f, 2f);
    }
    //                                                             // 0   1          2        3     4     5
    protected static final String[] BASIC_COL_NAMES = new String[]{"GS<br> follow link to MSigDB",
            "GS DETAILS", "SIZE", "ES", "NES", "NOM p-val",
            "FDR q-val", "FWER p-val", "RANK AT MAX", "LEADING EDGE"
    };
    private static final int COL_ES = 3;
    private static final int COL_NES = 4;
    private static final int COL_NP = 5;
    private static final int COL_FDR = 6;
    private static final int COL_FWER = 7;
    public static final String ENPLOT_ = "enplot_";
    /** EnPlot v2 enrichment plot file prefix (PNG/SVG/JSON). */
    public static final String ENPLOT2_ = "enplot2_";
    
    public static final Color CHART_FRAME_COLOR = new Color(0xf2, 0xf2, 0xf2);

    public static class Ret {
        public EnrichmentReportDbImpl rdb;
        public EnrichmentDb edb; // with pvalues set
        public File savedInDir;
    }

    /** Color-bar / hit-plot interval markers for a ranked list (recreated per chart to avoid shared mutation). */
    public static IntervalMarker[] colorBarMarkers(final RankedList rl) {
        int numRanges = (rl.getSize() < 100) ? rl.getSize() : 100;
        return RankedListCharts.createIntervalMarkers(numRanges, rl);
    }

    private static IntervalMarker[] _markers(final RankedList rl) {
        return colorBarMarkers(rl);
    }

    /**
     * Build a classic or EnPlot v2 combo chart from an enrichment result (e.g. on-demand in the report viewer).
     * Recreates hit-plot markers each call so charts do not share mutable {@link IntervalMarker} state.
     * Recomputes the full rank-by-rank ES curve when it was not persisted in the EDB.
     */
    public static EnrichmentCharts createComboChart(final EnrichmentResult result,
                                                    final boolean enplotV2,
                                                    final String classAName_opt,
                                                    final String classBName_opt,
                                                    final GeneSetScoringTable scoring_opt) {
        if (result == null) {
            throw new IllegalArgumentException("result cannot be null");
        }
        RankedList rl = result.getRankedList();
        if (rl == null) {
            throw new IllegalArgumentException("result ranked list cannot be null");
        }
        EnrichmentScore score = result.getScore();
        Vector hitVector = _hitIndices2Vector(rl.getSize(), score.getHitIndices());
        IntervalMarker[] markers = _markers(rl);
        String gsetName = result.getGeneSet().getName(true);
        Vector fullEs = EnrichmentEsProfiles.fullEsProfile(result, scoring_opt);
        if (enplotV2) {
            return ModernEnrichmentCharts.createComboChart(gsetName, score.getESProfile(),
                    fullEs, hitVector, rl,
                    classAName_opt, classBName_opt, markers,
                    score.getES(), score.getNES(), score.getFDR());
        }
        return _createComboChart(gsetName, score.getESProfile(),
                fullEs, hitVector, rl,
                classAName_opt, classBName_opt, markers);
    }


    public static Ret createGseaLikeReport(final EnrichmentDb edb_original, final PrintStream out, final CollapsedDetails cd, final HtmlPage reportIndexPage, final ToolReport report, 
    		final int topXSets, final int minSize, final int maxSize, final boolean makeGeneSetsReport, final boolean makeZippedFile, final boolean createSvgs, final boolean createGcts,
            final GeneSet[] origGeneSets_opt, final String metricName, final String normModeName) {
        return createGseaLikeReport(edb_original, out, cd, reportIndexPage, report, topXSets, minSize, maxSize,
                makeGeneSetsReport, makeZippedFile, createSvgs, createGcts, false, origGeneSets_opt, metricName, normModeName);
    }

    public static Ret createGseaLikeReport(final EnrichmentDb edb_original, final PrintStream out, final CollapsedDetails cd, final HtmlPage reportIndexPage, final ToolReport report, 
    		final int topXSets, final int minSize, final int maxSize, final boolean makeGeneSetsReport, final boolean makeZippedFile, final boolean createSvgs, final boolean createGcts,
    		final boolean createModernEnplots, final GeneSet[] origGeneSets_opt, final String metricName, final String normModeName) {
        FeatureAnnot fann = null;
        if (edb_original.getDataset() != null && edb_original.getDataset().getAnnot() != null) {
            fann = edb_original.getDataset().getAnnot().getFeatureAnnot();
        }
        return createGseaLikeReport(edb_original, out, cd, reportIndexPage, report.getReportDir(), report, topXSets, minSize, maxSize,
                makeGeneSetsReport, makeZippedFile, createSvgs, createGcts, createModernEnplots, origGeneSets_opt, metricName, normModeName, fann);
    }

    /** Preranked / annotated entry (no GCT heatmaps). */
    public static Ret createGseaLikeReport(final EnrichmentDb edb_original, final PrintStream out, final CollapsedDetails cd, final HtmlPage reportIndexPage, final ToolReport report, 
    		final int topXSets, final int minSize, final int maxSize, final boolean makeGeneSetsReport, final boolean makeZippedFile, final boolean createSvgs, 
    		final GeneSet[] origGeneSets_opt, final String metricName, final String normModeName, final FeatureAnnot fann_opt) {
        return createGseaLikeReport(edb_original, out, cd, reportIndexPage, report, topXSets, minSize, maxSize,
                makeGeneSetsReport, makeZippedFile, createSvgs, false, origGeneSets_opt, metricName, normModeName, fann_opt);
    }

    public static Ret createGseaLikeReport(final EnrichmentDb edb_original, final PrintStream out, final CollapsedDetails cd, final HtmlPage reportIndexPage, final ToolReport report, 
    		final int topXSets, final int minSize, final int maxSize, final boolean makeGeneSetsReport, final boolean makeZippedFile, final boolean createSvgs,
    		final boolean createModernEnplots, final GeneSet[] origGeneSets_opt, final String metricName, final String normModeName, final FeatureAnnot fann_opt) {
        return createGseaLikeReport(edb_original, out, cd, reportIndexPage, report.getReportDir(), report, topXSets, minSize, maxSize,
                makeGeneSetsReport, makeZippedFile, createSvgs, false, createModernEnplots, origGeneSets_opt, metricName, normModeName, fann_opt);
    }

    private static String _createPhenotypeName(EnrichmentDb edb) {
        final Template templatex = edb.getTemplate();
        return (templatex != null) ? templatex.getName() : "NoPhenotypeAvailable";
    }

    private static String[] _createClassNames(final Template template_opt) {
        if (template_opt != null) {
            if (template_opt.isContinuous()) {
                String nn = AuxUtils.getAuxNameOnlyNoHash(template_opt);
                return new String[] { nn + "_pos", nn + "_neg"};
            } else {
                return new String[] { template_opt.getClassName(0), template_opt.getClassName(1) };
            }
        } else {
            return new String[] { Constants.NA + "_pos", Constants.NA + "_neg"};
        }
    }

    // @note this is the CORE CORE CORE CORE report making method
    public static Ret createGseaLikeReport(final EnrichmentDb edb_original, final PrintStream out, final CollapsedDetails cd, final HtmlPage reportIndexPage, final File saveInThisDir, 
    		final ToolReport report, final int topXSets, final int minSize, final int maxSize, final boolean makeGeneSetsReport, final boolean makeZippedFile, final boolean createSvgs, 
    		final boolean createGcts, final boolean createModernEnplots, final GeneSet[] origGeneSets_opt, final String metricName, final String normModeName, final FeatureAnnot fann_opt) {
        if (normModeName == null) {
            throw new IllegalArgumentException("Param normModeName cannot be null");
        }

        if (saveInThisDir == null) {
            throw new IllegalArgumentException("Param saveInThisDir cannot be null");
        }

        if (!saveInThisDir.exists()) {
            saveInThisDir.mkdir();
        }

        // Copy over any warnings from the RankedList(s)/Dataset(s) and EnrichmentDB to be displayed as Report Comments
        // TODO: consider doing likewise for Template, FeatureAnnotation, etc
        copyPobWarnings(cd.orig, report);
        if (cd.wasCollapsed) { copyPobWarnings(cd.collapsed, report); }
        copyPobWarnings(edb_original, report);
        
        String phenotypeName = _createPhenotypeName(edb_original);

        final String[] classNames = _createClassNames(edb_original.getTemplate());
        String classA_name_opt = classNames[0];
        String classB_name_opt = classNames[1];

        final PValueCalculator pvc = new PValueCalculatorImpls.GseaImpl(normModeName);
        final EnrichmentResult[] results = pvc.calcNPValuesAndFDR(edb_original.getResults());
        final EnrichmentDb edb = edb_original.cloneDeep(results);

        if (Norms.MEDIAN_OF_RATIOS.equals(normModeName) && cd instanceof CollapsedDetails.Data) {
            try {
                final Dataset datasetToNormalize = ((CollapsedDetails.Data) cd).getDataset();
                final int rowCount = datasetToNormalize.getNumRow();
                final int colCount = datasetToNormalize.getNumCol();
                final Matrix normalizedMatrix = new Matrix(rowCount, colCount);
                final double[] sizeFactors = new double[colCount];
                final double[] geometricMeans = new double[rowCount];
                final boolean[] includeRow = new boolean[rowCount];

                int usableRows = 0;
                for (int r = 0; r < rowCount; r++) {
                    final Vector row = datasetToNormalize.getRow(r);
                    boolean usable = true;
                    double sumLog = 0.0d;

                    for (int c = 0; c < colCount; c++) {
                        final double value = row.getElement(c);
                        if (!Double.isFinite(value) || value <= 0.0d) {
                            usable = false;
                            break;
                        }
                        sumLog += Math.log(value);
                    }

                    if (usable) {
                        geometricMeans[r] = Math.exp(sumLog / colCount);
                        includeRow[r] = true;
                        usableRows++;
                    } else {
                        geometricMeans[r] = Double.NaN;
                        includeRow[r] = false;
                    }
                }

                for (int c = 0; c < colCount; c++) {
                    if (usableRows == 0) {
                        sizeFactors[c] = 1.0d;
                        continue;
                    }

                    final double[] ratios = new double[usableRows];
                    int ratioIndex = 0;
                    for (int r = 0; r < rowCount; r++) {
                        if (!includeRow[r]) {
                            continue;
                        }
                        ratios[ratioIndex++] = datasetToNormalize.getRow(r).getElement(c) / geometricMeans[r];
                    }

                    Arrays.sort(ratios);
                    final double median;
                    if (ratios.length % 2 == 0) {
                        median = (ratios[(ratios.length / 2) - 1] + ratios[ratios.length / 2]) / 2.0d;
                    } else {
                        median = ratios[ratios.length / 2];
                    }

                    sizeFactors[c] = (Double.isFinite(median) && median > 0.0d) ? median : 1.0d;
                }

                for (int r = 0; r < rowCount; r++) {
                    final Vector row = datasetToNormalize.getRow(r);
                    final Vector normalizedRow = new Vector(colCount);
                    for (int c = 0; c < colCount; c++) {
                        final float value = row.getElement(c);
                        if (Float.isFinite(value)) {
                            normalizedRow.setElement(c, (float) (value / sizeFactors[c]));
                        } else {
                            normalizedRow.setElement(c, value);
                        }
                    }
                    normalizedMatrix.setRow(r, normalizedRow);
                }

                final Dataset normalizedDataset = new DefaultDataset(datasetToNormalize.getName() + "_normalized_" + normModeName,
                        normalizedMatrix, datasetToNormalize.getRowNames(), datasetToNormalize.getColumnNames(), datasetToNormalize.getAnnot());
                final File edbDir = new File(saveInThisDir, "edb");
                if (!edbDir.exists()) {
                    edbDir.mkdirs();
                }
                final File normalizedGct = new File(edbDir, normalizedDataset.getName() + ".gct");
                new GctParser().export(normalizedDataset, normalizedGct);
            } catch (Throwable t) {
                report.addError("Trouble exporting normalized dataset GCT", t);
            }
        }

        boolean haveInfiniteOrNaN = false;
        for (int i = 0; i < results.length; i++) {
            EnrichmentScore score = results[i].getScore();
            if (!Float.isFinite(score.getES()) || !Float.isFinite(score.getNES()) || !Float.isFinite(score.getNP())
                    || !Float.isFinite(score.getFDR()) || !Float.isFinite(score.getFWER())) {
                haveInfiniteOrNaN = true;
                klog.warn("Scoring of {} produced infinite Or NaN value(s)", results[i].getGeneSetName());
            }
        }
        if (haveInfiniteOrNaN) {
            report.addWarning("Scoring produced infinite or NaNs values which may have prevented plotting for certain gene sets.  See the log for more details.");
        }
        
        Dataset my_gex_ds_for_heat_map = edb.getDataset();

        final GeneSet[] gsets = edb.getGeneSets();
        if (gsets.length == 1) {
            report.addWarning("FDR values were computed but only one gene set was detected in the output. Reported FDRs are not an accurate representation of the true false discovery rate when derived from a single gene set.");
        }
        
        final RankedList rlReal = edb.getRankedList();
        final MetricWeightStruc mws = rlReal.getMetricWeightStruc();
        if (mws != null && metricName != null && mws.getMetricName() == null) {
            mws.setMetricName(metricName);
        }

        String classA_name_long;
        String classB_name_long;
        File geneSets_sizes_file = null;

        final Template template = edb.getTemplate();

        if (template != null) {

            if (template.isContinuous()) {
                classA_name_long = "positive correlation with profile";
                classB_name_long = "negative correlation with profile";
                //String nn = AuxUtils.getAuxNameOnlyNoHash(template);
            } else {
                classA_name_long = template.getClassName(0) + " (" + template.getClass(0).getSize() + " samples)";
                classB_name_long = template.getClassName(1) + " (" + template.getClass(1).getSize() + " samples)";
            }
        } else {
            classA_name_long = classB_name_long = Constants.NA;
        }


        final String name = edb.getName();

        // Then the GENE LIST AMD MARKER SELECTION REPORTS
        klog.info("Creating marker selection reports ...");
        final StringDataframe sdfGeneList = MiscReports.createRankOrderedGeneList(name, rlReal, fann_opt);
        final File real_gene_list_file_tsv = report.savePageTsv(sdfGeneList, "ranked_gene_list_" + classA_name_opt + "_versus_" + classB_name_opt + "_" + report.getTimestamp(), saveInThisDir);
        
        File real_gene_list_heat_map_corr_plot_html_file = null;
        if (my_gex_ds_for_heat_map != null && template != null) {
            HtmlPage real_gene_list_heat_map_corr_plot_html = MiscReports.createDatasetHeatMapAndCorrelationPlots(my_gex_ds_for_heat_map,
                    template, rlReal, saveInThisDir, createSvgs, createGcts);
            real_gene_list_heat_map_corr_plot_html_file = report.savePage(real_gene_list_heat_map_corr_plot_html, saveInThisDir);
        }

        File butterfly_file = null;
        File butterfly_file_svg = null;
        if (edb.getPermutationTest() != null) {
            try {
                final XChart xc = EnrichmentReports.createButterflyChart(edb.getPermutationTest());
                butterfly_file = new File(saveInThisDir, "butterfly_plot.png");
                xc.saveAsPNG(butterfly_file, 500, 500);
                if (createSvgs) {
                    butterfly_file_svg = ImageUtil.getSvgFileFromImgFile(butterfly_file, true);
                    ImageUtil.saveAsSVG(xc.getFreeChart(), butterfly_file_svg, 500, 500, true);
                }
            } catch (Throwable t) {
                report.addError("Trouble making butterfly plot", t);
            }
        }

        // Then the FDR reports
        klog.info("Creating FDR reports ...");
        final EnrichmentResult[] results_pos = edb.getResults(true);
        final BasicReportStruc pos_basic = createReport(results_pos, name, phenotypeName, classA_name_opt, classB_name_opt, rlReal, template, fann_opt,
                "Gene sets enriched in phenotype <b>" + classA_name_long + "<b>", topXSets, makeGeneSetsReport, createSvgs, createGcts, createModernEnplots, saveInThisDir);
        klog.info("Done FDR reports for positive phenotype");

        final EnrichmentResult[] results_neg = edb.getResults(false);
        final BasicReportStruc neg_basic = createReport(results_neg, name, phenotypeName, classA_name_opt, classB_name_opt, rlReal, template, fann_opt,
                "Gene sets enriched in phenotype <b>" + classB_name_long + "<b>", topXSets, makeGeneSetsReport, createSvgs, createGcts, createModernEnplots, saveInThisDir);
        klog.info("Done FDR reports for negative phenotype");

        // Ok done calcs; begin formatting and outputting reports
        final String pos_name = "gsea_report_for_" + classA_name_opt + "_" + report.getTimestamp();
        final String pos_title = "Report for " + classA_name_opt + " " + report.getTimestamp() + " [GSEA]";
        final String neg_name = "gsea_report_for_" + classB_name_opt + "_" + report.getTimestamp();
        final String neg_title = "Report for " + classB_name_opt + " " + report.getTimestamp() + " [GSEA]";

        // Basic statistics
        final File pos_basic_tsv = report.savePageTsv(pos_basic.rdf, pos_name, saveInThisDir);
        final File neg_basic_tsv = report.savePageTsv(neg_basic.rdf, neg_name, saveInThisDir);

        HtmlPage htmlPage = new HtmlPage(pos_name, pos_title);
        htmlPage.addTable(pos_basic.rdf, pos_basic_tsv.getName(), false); // dont show row names (ditto to gs name)
        final File pos_basic_html = report.savePage(htmlPage, saveInThisDir);
        final File pos_snapshot_html = report.savePage(createSnapshotPage(true, pos_basic.reports), saveInThisDir);

        htmlPage = new HtmlPage(neg_name, neg_title);
        htmlPage.addTable(neg_basic.rdf, neg_basic_tsv.getName(), false); // dont show row names (ditto to gs name)
        final File neg_basic_html = report.savePage(htmlPage, saveInThisDir);
        final File neg_snapshot_html = report.savePage(createSnapshotPage(false, neg_basic.reports), saveInThisDir);

        final XChart pvalues_nes_plot_xc = createNESvsSignificancePlot(edb);
        final File pvalues_nes_plot_file = report.savePage(pvalues_nes_plot_xc, 500, 500, saveInThisDir);
        File pvalues_nes_plot_svg_file = null;
        if (createSvgs) {
            pvalues_nes_plot_svg_file = ImageUtil.getSvgFileFromImgFile(pvalues_nes_plot_file, true);
            report.savePageSvg(pvalues_nes_plot_xc, 500, 500, pvalues_nes_plot_svg_file);
        }

        // Build a single shared canvas from first principles, then populate it separately by sign.
        final List<EnrichmentResult> posTop = selectTopFiniteResults(results_pos, topXSets);
        final List<EnrichmentResult> negTop = selectTopFiniteResults(results_neg, topXSets);
        final BubbleCanvasSpec bubbleCanvasSpec = computeBubbleCanvasSpec(posTop, negTop);

        // --- Enrichment bubble plots by phenotype (PNG / optional SVG) ---
        final XChart bubble_plot_pos_xc = createBubblePlotForGseaResults(posTop, classA_name_opt, true, bubbleCanvasSpec);
        final File bubble_plot_pos_file = report.savePage(bubble_plot_pos_xc, 1300, 700, saveInThisDir);
        File bubble_plot_pos_svg_file = null;
        if (createSvgs) {
            bubble_plot_pos_svg_file = ImageUtil.getSvgFileFromImgFile(bubble_plot_pos_file, true);
            report.savePageSvg(bubble_plot_pos_xc, 1300, 700, bubble_plot_pos_svg_file);
        }
        final XChart bubble_plot_neg_xc = createBubblePlotForGseaResults(negTop, classB_name_opt, false, bubbleCanvasSpec);
        final File bubble_plot_neg_file = report.savePage(bubble_plot_neg_xc, 1300, 700, saveInThisDir);
        File bubble_plot_neg_svg_file = null;
        if (createSvgs) {
            bubble_plot_neg_svg_file = ImageUtil.getSvgFileFromImgFile(bubble_plot_neg_file, true);
            report.savePageSvg(bubble_plot_neg_xc, 1300, 700, bubble_plot_neg_svg_file);
        }

        final XChart global_es_histogram_xc = createGlobalESHistogram(AuxUtils.getAuxNameOnlyNoHash(phenotypeName), edb.getESS_lv());
        final File global_es_histogram_file = report.savePage(global_es_histogram_xc, 500, 500, saveInThisDir);
        File global_es_histogram_svg_file = null;
        if (createSvgs) {
            global_es_histogram_svg_file = ImageUtil.getSvgFileFromImgFile(global_es_histogram_file, true);
            report.savePageSvg(global_es_histogram_xc, 500, 500, global_es_histogram_svg_file);
        }

        // Ok, build the page

        // Class A
        Div div = new Div();
        H4 h4 = new H4("Enrichment in phenotype: <b>" + classA_name_long + "</b>");
        div.addElement(h4);
        UL ul = new UL();

        StringElement line7 = HtmlFormat.Links.hyper("Guide to", GseaWebResources.getGseaHelpURL() + "GSEA/GSEA_User_Guide/#interpreting-gsea-results", "interpret results");

        final int numPosScores = edb.getNumScores(true);
		if (numPosScores > 0) {
            StringElement line1 = new StringElement(numPosScores + " / " + gsets.length + " gene sets are upregulated in phenotype <b>" + classA_name_opt + "</b>");
            StringElement line2a = new StringElement(edb.getNumNominallySig(0.01f, true) + " gene sets are significantly enriched at nominal pvalue < 1%");
            StringElement line2b = new StringElement(edb.getNumNominallySig(0.05f, true) + " gene sets are significantly enriched at nominal pvalue < 5%");
            StringElement line3 = new StringElement(edb.getNumFDRSig(0.25f, true) + " gene sets are significant at FDR < 25%");
            StringElement line4 = HtmlFormat.Links.hyper("Snapshot", pos_snapshot_html, "of enrichment results", saveInThisDir);
            StringElement line4a = HtmlFormat.Links.hyper("", "Bubble plot", bubble_plot_pos_file, "of enrichment results", saveInThisDir);
            StringElement line5 = HtmlFormat.Links.hyper("Detailed", "enrichment results in html", pos_basic_html, " format", saveInThisDir);
            StringElement line6 = HtmlFormat.Links.hyper("Detailed", "enrichment results in TSV", pos_basic_tsv, " format (tab delimited text)", saveInThisDir);

            ul.addElement(new LI(line1));
            ul.addElement(new LI(line3));
            ul.addElement(new LI(line2a));
            ul.addElement(new LI(line2b));
            ul.addElement(new LI(line4));
            ul.addElement(new LI(line4a));
            if (createSvgs && bubble_plot_pos_svg_file != null) {
                ul.addElement(new LI(HtmlFormat.Links.hyper("", "Bubble plot", bubble_plot_pos_svg_file, "of enrichment results (SVG)", saveInThisDir)));
            }
            ul.addElement(new LI(line5));
            ul.addElement(new LI(line6));
            ul.addElement(new LI(line7));
        } else {
            StringElement line1 = new StringElement("None of the gene sets are enriched in phenotype <b>" + classA_name_opt + "</b>");
            ul.addElement(new LI(line1));
            ul.addElement(new LI(line7));
        }

        div.addElement(ul);
        reportIndexPage.addBlock(div, false);

        // Class B
        div = new Div();
        h4 = new H4("Enrichment in phenotype: <b>" + classB_name_long + "</b>");
        div.addElement(h4);
        ul = new UL();
        final int numNegScores = edb.getNumScores(false);
		if (numNegScores > 0) {
            StringElement line1 = new StringElement(numNegScores + " / " + gsets.length + " gene sets are upregulated in phenotype <b>" + classB_name_opt + "</b>");
            StringElement line2a = new StringElement(edb.getNumNominallySig(0.01f, false) + " gene sets are significantly enriched at nominal pvalue < 1%");
            StringElement line2b = new StringElement(edb.getNumNominallySig(0.05f, false) + " gene sets are significantly enriched at nominal pvalue < 5%");
            StringElement line3 = new StringElement(edb.getNumFDRSig(0.25f, false) + " gene sets are significantly enriched at FDR < 25%");
            StringElement line4 = HtmlFormat.Links.hyper("Snapshot", neg_snapshot_html, "of enrichment results", saveInThisDir);
            StringElement line4a = HtmlFormat.Links.hyper("", "Bubble plot", bubble_plot_neg_file, "of enrichment results", saveInThisDir);
            StringElement line5 = HtmlFormat.Links.hyper("Detailed", "enrichment results in html", neg_basic_html, " format", saveInThisDir);
            StringElement line6 = HtmlFormat.Links.hyper("Detailed", "enrichment results in TSV", neg_basic_tsv, " format (tab delimited text)", saveInThisDir);
            ul.addElement(new LI(line1));
            ul.addElement(new LI(line3));
            ul.addElement(new LI(line2a));
            ul.addElement(new LI(line2b));
            ul.addElement(new LI(line4));
            ul.addElement(new LI(line4a));
            if (createSvgs && bubble_plot_neg_svg_file != null) {
                ul.addElement(new LI(HtmlFormat.Links.hyper("", "Bubble plot", bubble_plot_neg_svg_file, "of enrichment results (SVG)", saveInThisDir)));
            }
            ul.addElement(new LI(line5));
            ul.addElement(new LI(line6));
            ul.addElement(new LI(line7));
        } else {
            StringElement line1 = new StringElement("None of the gene sets are enriched in phenotype <b>" + classB_name_opt + "</b>");
            ul.addElement(new LI(line1));
            ul.addElement(new LI(line7));
        }

        div.addElement(ul);
        reportIndexPage.addBlock(div, false);

        // Dataset details
        div = new Div();
        ul = new UL();
        h4 = new H4("Dataset details");
        div.addElement(h4);
        if (cd.wasCollapsed) {
            final StringElement lined1 = new StringElement("The dataset has " + cd.getNumRow_orig() + " native features");
            final StringElement lined2 = new StringElement("After collapsing features into gene symbols, there are: " + rlReal.getSize() + " genes");
            ul.addElement(new LI(lined1));
            ul.addElement(new LI(lined2));
        } else {
            StringElement lined1 = new StringElement("The dataset has " + rlReal.getSize() + " features (genes)");
            ul.addElement(new LI(lined1));
            ul.addElement(new LI("No probe set => gene symbol collapsing was requested, so all " + rlReal.getSize() + " features were used"));
        }

        div.addElement(ul);
        reportIndexPage.addBlock(div, false);

        // Gene set details
        if (origGeneSets_opt != null) {
            div = new Div();
            ul = new UL();
            h4 = new H4("Gene set details");
            div.addElement(h4);
            final int xs = origGeneSets_opt.length - gsets.length;
            final StringBuilder buf = new StringBuilder("Gene set size filters (min=").append(minSize).append(", max=").append(maxSize).append(")");
            buf.append(" resulted in filtering out ").append(xs).append(" / ").append(origGeneSets_opt.length).append(" gene sets");
            ul.addElement(new LI(buf.toString()));
            geneSets_sizes_file = _getGeneSetSizesFile(gsets, origGeneSets_opt, geneSets_sizes_file, report);
            ul.addElement(new LI("The remaining " + gsets.length + " gene sets were used in the analysis"));
            ul.addElement(new LI(HtmlFormat.Links.hyper("List of", "gene sets used and their sizes",
                    geneSets_sizes_file, "(restricted to features in the specified dataset)", saveInThisDir)));
            div.addElement(ul);

            reportIndexPage.addBlock(div, false);
        }

        // Gene Markers
        div = new Div();
        if (template != null && template.isContinuous()) {
            h4 = new H4("Gene markers for the neighbors of " + classA_name_opt);
        } else {
            h4 = new H4("Gene markers for the <b>" + classA_name_opt + "</b><i> versus </i><b>" + classB_name_opt + "</b> comparison");
        }
        div.addElement(h4);

        StringElement line1 = new StringElement("The dataset has " + rlReal.getSize() + " features (genes)");
        StringElement line2 = new StringElement("# of markers for phenotype <b>" + classA_name_opt + "</b>: " + mws.getTotalPosLength() + " (" + Printf.format(mws.getTotalPosLength_frac() * 100, 1) + "% )" + " with correlation area " + Printf.format(mws.getTotalPosWeight_frac() * 100, 1) + "%");
        StringElement line3 = new StringElement("# of markers for phenotype <b>" + classB_name_opt + "</b>: " + mws.getTotalNegLength() + " (" + Printf.format(mws.getTotalNegLength_frac() * 100, 1) + "% )" + " with correlation area " + Printf.format(mws.getTotalNegWeight_frac() * 100, 1) + "%");
        StringElement line4 = HtmlFormat.Links.hyper("Detailed", "rank ordered gene list", real_gene_list_file_tsv, " for all features in the dataset", saveInThisDir);


        ul = new UL();
        ul.addElement(new LI(line1));
        if (template != null && !template.isContinuous()) {
            ul.addElement(new LI(line2));
            ul.addElement(new LI(line3));
        }

        ul.addElement(new LI(line4));
        if (real_gene_list_heat_map_corr_plot_html_file != null) {
            StringElement line5 = HtmlFormat.Links.hyper("Heat map and gene list correlation ", real_gene_list_heat_map_corr_plot_html_file, " profile for all features in the dataset", saveInThisDir);
            ul.addElement(new LI(line5));
        }

        if (butterfly_file != null && butterfly_file.exists()) {
            StringElement line6 = HtmlFormat.Links.hyper("Butterfly plot", butterfly_file, "of significant genes", saveInThisDir);
            ul.addElement(new LI(line6));
            if (createSvgs) {
                StringElement line6a = HtmlFormat.Links.hyper("Butterfly plot", butterfly_file_svg, "of significant genes (in compressed SVG format)", saveInThisDir);
                ul.addElement(new LI(line6a));
            }
        }
        div.addElement(ul);
        reportIndexPage.addBlock(div, false);

        // ADVANCED REPORTS
        klog.info("Creating global reports ...");
        div = new Div();
        ul = new UL();
        div.addElement(new H4("Global statistics and plots"));
        ul.addElement(new LI(HtmlFormat.Links.hyper("Plot of ", "p-values <i>vs.</i> NES", pvalues_nes_plot_file, "", saveInThisDir)));
        if (createSvgs && pvalues_nes_plot_svg_file != null) {
            ul.addElement(new LI(HtmlFormat.Links.hyper("Plot of ", "p-values <i>vs.</i> NES", pvalues_nes_plot_svg_file, "(in compressed SVG format)", saveInThisDir)));
        }
        ul.addElement(new LI(HtmlFormat.Links.hyper("Global ES", global_es_histogram_file, "histogram", saveInThisDir)));
        if (createSvgs && global_es_histogram_svg_file != null) {
            ul.addElement(new LI(HtmlFormat.Links.hyper("Global ES", global_es_histogram_svg_file, "histogram (in compressed SVG format)", saveInThisDir)));
        }
        div.addElement(ul);
        reportIndexPage.addBlock(div, false);

        // Other
        div = new Div();
        ul = new UL();
        div.addElement(new H4("Other"));
        ul.addElement(new LI(HtmlFormat.Links.hyper("Parameters", report.getParamsFile(), "used for this analysis", saveInThisDir)));

        if (makeZippedFile) {
            File zipped_report = report.getZipReportFile();
            ul.addElement(new LI(HtmlFormat.Links.hyper("Zipped", zipped_report, "file with all results", saveInThisDir)));
        }

        div.addElement(ul);
        reportIndexPage.addBlock(div, false);


        if (reportIndexPage instanceof HtmlReportIndexPage) {
            ((HtmlReportIndexPage) reportIndexPage).setAddBrowseFooter(false); // turn off the little browse footer
        }

        klog.info("Done all reports!!");

        // @todo @note always save the dataset and template also

        List<EnrichmentReport> all_reports = new ArrayList<EnrichmentReport>(Arrays.asList(pos_basic.reports));
        all_reports.addAll(Arrays.asList(neg_basic.reports));

        Ret ret = new Ret();
        ret.rdb = new EnrichmentReportDbImpl(all_reports.toArray(new EnrichmentReport[all_reports.size()]));
        ret.savedInDir = saveInThisDir;
        ret.edb = edb;
        return ret;
    }

    private static void copyPobWarnings(PersistentObject pob, Report report) {
        List<String> warnings = pob.getWarnings();
        for (String warning : warnings) { report.addWarning(warning); }
    }

    public static BasicReportStruc createReport(final EnrichmentResult[] results,
                                                final String dsName,
                                                final String phenotypeName,
                                                final String phenoClassAName_opt,
                                                final String phenoClassBName_opt,
                                                final RankedList rl,
                                                final Template template_opt,
                                                final FeatureAnnot fannx,
                                                final String title,
                                                final int showDetailsForTopXSets,
                                                final boolean makeDetailsPage,
                                                final boolean createSvgs,
                                                final boolean createGcts,
                                                final File saveDetailFilesInDir) {
        return createReport(results, dsName, phenotypeName, phenoClassAName_opt, phenoClassBName_opt, rl, template_opt, fannx,
                title, showDetailsForTopXSets, makeDetailsPage, createSvgs, createGcts, false, saveDetailFilesInDir);
    }

    public static BasicReportStruc createReport(final EnrichmentResult[] results,
                                                final String dsName,
                                                final String phenotypeName,
                                                final String phenoClassAName_opt,
                                                final String phenoClassBName_opt,
                                                final RankedList rl,
                                                final Template template_opt,
                                                final FeatureAnnot fannx,
                                                final String title,
                                                final int showDetailsForTopXSets,
                                                final boolean makeDetailsPage,
                                                final boolean createSvgs,
                                                final boolean createGcts,
                                                final boolean createModernEnplots,
                                                final File saveDetailFilesInDir) {

        // check if there are *any* that are pos
        // actually assume that are are some

        final String[] gsetNames = new String[results.length];
        final TIntObjectHashMap cell_id_linkMap = new TIntObjectHashMap();
        final StringMatrix sm = new StringMatrix(gsetNames.length, BASIC_COL_NAMES.length);

        // for the bg shading of the hit plot -- just needs to be made once for all sets on this rl
        final IntervalMarker[] markers = _markers(rl);

        // EnPlot v2 interactive JSON for every gene set (not limited to plot_top_x).
        if (createModernEnplots) {
            ModernEnrichmentPlotJson.ensureViewerAssets(saveDetailFilesInDir);
            for (int r = 0; r < results.length; r++) {
                final EnrichmentResult result = results[r];
                try {
                    ModernEnrichmentPlotJson.writePayload(saveDetailFilesInDir,
                            result.getGeneSet().getName(true), rl,
                            result.getScore().getHitIndices(),
                            result.getScore().getESProfile(),
                            EnrichmentEsProfiles.fullEsProfile(result, null),
                            phenoClassAName_opt, phenoClassBName_opt,
                            result.getScore().getES(), result.getScore().getNES(),
                            result.getScore().getNP(), result.getScore().getFDR(),
                            result.getScore().getFWER(),
                            markers);
                } catch (Throwable jt) {
                    klog.warn("Could not write EnPlot v2 JSON for {}: {}",
                            result.getGeneSet().getName(true), jt.getMessage());
                }
            }
        }

        List<EnrichmentReport> ereports = new ArrayList<EnrichmentReport>();
        for (int r = 0; r < results.length; r++) {
            int coln = 0;
            final EnrichmentResult result = results[r];
            gsetNames[r] = result.getGeneSet().getName(true);
            HtmlPage htmlPage = null;
            sm.setElement(r, coln++, gsetNames[r]);

            if (makeDetailsPage && r < showDetailsForTopXSets) {
                final EnrichmentResult dtg = results[r];
                htmlPage = new HtmlPage(gsetNames[r], "Details for gene set " + gsetNames[r] + "[GSEA]");
                final Vector fullEs = EnrichmentEsProfiles.fullEsProfile(dtg, null);
                final MyEnrichmentReportImpl mer = createReport(dsName, phenotypeName, phenoClassAName_opt, phenoClassBName_opt, rl, 
                		template_opt, dtg.getGeneSet(), dtg.getScore().getHitIndices(), dtg.getScore().getESProfile(), 
                		fullEs, result.getScore().getES(), result.getScore().getNES(), 
                		result.getScore().getNP(), result.getScore().getFDR(), result.getScore().getFWER(), dtg.getRndESS(), htmlPage, 
                		fannx, createSvgs, createGcts, createModernEnplots, markers, saveDetailFilesInDir);

                // dont do this as it saves the pages in memory
                //report.savePage(pages[0]);
                //report.savePage(pages[1]);
                try {
                    File htmlFile = new File(saveDetailFilesInDir, mer.fHtmlPage.getName() + ".html");
                    htmlPage.write(new FileOutputStream(htmlFile));
                    mer.fTsvPage.write(new FileOutputStream(new File(saveDetailFilesInDir, 
                            mer.fTsvPage.getName() + "." + Constants.TSV)));
                    PicFile[] pfs = htmlPage.getPicFiles();
                    File plotFile = pfs[0].getFile(); // because image write likes to rename stuff
                    File modernPlotFile = mer.fModernPlotFile;

                    // @note IMP IMP dont re-use as want this to be light (just files)
                    ereports.add(new EnrichmentReportImpl(htmlFile, plotFile, modernPlotFile));
                } catch (Throwable thr) {
                    klog.error("Error making details: {}", gsetNames[r]);
                    klog.error(thr.getMessage(), thr);
                }
                sm.setElement(r, coln++, "Details ..."); // i.e desc
            } else {
                sm.setElement(r, coln++, ""); // i.e desc
            }

            sm.setElement(r, coln++, result.getScore().getNumHits());
            sm.setElement(r, coln++, result.getScore().getES());
            sm.setElement(r, coln++, result.getScore().getNES());
            sm.setElement(r, coln++, result.getScore().getNP());
            sm.setElement(r, coln++, result.getScore().getFDR());
            sm.setElement(r, coln++, result.getScore().getFWER());
            sm.setElement(r, coln++, result.getSignal().getRankAtMax());
            sm.setElement(r, coln, getLeadingEdge(result));

            if (htmlPage != null) {
                cell_id_linkMap.put(sm.getElementPos(r, 0), LinkedFactory.createLinkedGeneSet(result.getGeneSet()));
                cell_id_linkMap.put(sm.getElementPos(r, 1), new LinkedFactory.SimpleLinkedPage("Details", htmlPage));
            }
        }

        StringDataframe sdf = new StringDataframe(dsName + "_basic", sm, gsetNames, BASIC_COL_NAMES);
        TIntIntHashMap colPrecision = new TIntIntHashMap();
        // TODO: evaluate these settings for report precision consistency
        colPrecision.put(COL_ES, 2);
        colPrecision.put(COL_NES, 2);
        colPrecision.put(COL_NP, 3);
        colPrecision.put(COL_FDR, 3);
        colPrecision.put(COL_FWER, 3);

        BasicReportStruc struc = new BasicReportStruc();
        RichDataframe.MetaData md = new RichDataframe.MetaData(title, colPrecision);
        struc.rdf = new RichDataframe(sdf, md, null, cell_id_linkMap);
        struc.reports = ereports.toArray(new EnrichmentReport[ereports.size()]);
        return struc;
    }

    public static HtmlPage createSnapshotPage(final boolean pos, final EnrichmentReport[] reports) {

        Table table = new Table();
        int index = 0;
        for (int r = 0; r < reports.length; r++) {
            TR tr = new TR();
            for (int c = 0; c < 3; c++) {
                A a = new A();
                a.setName("");
                a.setHref(reports[index].getHtmlFile().getName()); // assume relative
                IMG img = new IMG();
                File plotFile = reports[index].getModernESPlotFile();
                boolean modern = plotFile != null && plotFile.isFile();
                if (!modern) {
                    plotFile = reports[index].getESPlotFile();
                }
                img.setSrc(plotFile.getName());
                img.setWidth(200);
                img.setHeight(modern ? 213 : 200);
                a.addElement(img);
                TD td = new TD(a);
                tr.addElement(td);
                index++;
                if (index >= reports.length) {
                    break;
                }
            }
            table.addElement(tr);

            if (index >= reports.length) {
                break;
            }
        }

        String name = (pos) ? "pos_snapshot" : "neg_snapshot";
        HtmlPage page = new HtmlPage(name, "Snapshot of " + reports.length + " enrichment plots");
        page.addTable("Snapshot of enrichment results", table);
        return page;
    }

    public static String getLeadingEdge(final EnrichmentResult result) {
        GeneSetSignal signal = result.getSignal();
        StringBuffer buf = new StringBuffer();
        buf.append("tags=").append(Printf.format(signal.getTagFraction() * 100, 0)).append("%, ");
        buf.append("list=").append(Printf.format(signal.getListFraction() * 100, 0)).append("%, ");
        buf.append("signal=").append(Printf.format(signal.getSignalStrength() * 100, 0)).append("%");
        return buf.toString();
    }

    // does the real page creation
    // one html page and one TSV page
    private static MyEnrichmentReportImpl createReport(final String dsName, final String phenotypeName, final String classAName_opt, final String classBName_opt, 
    		final RankedList rl, final Template template_opt, final GeneSet gset, final int[] hitIndices, final Vector esProfile, final Vector esProfile_full_opt, 
    		float es, float nes, float np, final float fdr, final float fwer, final Vector rndEss, final HtmlPage htmlPage, final FeatureAnnot fann_opt, 
    		boolean createSvgs, boolean createGcts, boolean createModernEnplots, final IntervalMarker[] markers, File saveDetailFilesInDir) {
        TsvPage tsvPage = null;
        File modernPlotFile = null;
        try {
            String gsetName = gset.getName(true);

            if (hitIndices.length != gset.getNumMembers(rl)) {
                throw new IllegalStateException("Mismatched gset: " + gset.getNumMembers(rl) + " and hitIndices: " + hitIndices.length);
            }

            if ((int) esProfile.maxDevFrom0() != (int) es) { // int it as rounding errors
                klog.warn("Possibly mismatched scores: {} {}", esProfile.maxDevFrom0(), es);
            }

            if (esProfile.getSize() != hitIndices.length) {
                throw new IllegalStateException("Mismatched esProfile: " + esProfile.getSize() + " and hitIndices: " + hitIndices.length);
            }

            final RichDataframe rdf = createDetailTable(dsName, hitIndices, esProfile, rl, gset, fann_opt);
            
            // add summary table and link to details table (TSV)
            tsvPage = new TsvPage(htmlPage.getName(), rdf);
            String upInClass;

            if (XMath.isPositive(es)) {
                upInClass = classAName_opt;
            } else {
                upInClass = classBName_opt;
            }

            final KeyValTable table = createSummaryTable(dsName, phenotypeName, upInClass, gset.getName(), es, nes, np, fdr, fwer);
            htmlPage.addTable("GSEA Results Summary", table);

            // add main es plot image (on top -- roels request, makes sense)
            final Vector hitVector = _hitIndices2Vector(rl.getSize(), hitIndices);
            EnrichmentCharts combo = _createComboChart(gsetName, esProfile, esProfile_full_opt, 
            		hitVector, rl, classAName_opt, classBName_opt, markers);
            htmlPage.addChart(combo.comboChart, 500, 500, saveDetailFilesInDir, createSvgs);

            if (createModernEnplots) {
                EnrichmentCharts modern = ModernEnrichmentCharts.createComboChart(gsetName, esProfile, esProfile_full_opt,
                        hitVector, rl, classAName_opt, classBName_opt, markers, es, nes, fdr);
                int picBefore = htmlPage.getPicFiles().length;
                htmlPage.addChart(modern.comboChart, 900, 960, saveDetailFilesInDir, createSvgs);
                PicFile[] pics = htmlPage.getPicFiles();
                if (pics.length > picBefore) {
                    modernPlotFile = pics[picBefore].getFile();
                }
                // JSON is written for all gene sets in createReport(...); boot .js for file:// HTML links.
                File jsonFile = ModernEnrichmentPlotJson.jsonFile(saveDetailFilesInDir, gsetName);
                if (jsonFile.isFile()) {
                    try {
                        File bootJs = ModernEnrichmentPlotJson.writeBootScript(saveDetailFilesInDir, gsetName, jsonFile);
                        Div div = new Div();
                        A link = new A();
                        link.setHref(ModernEnrichmentPlotJson.sharedViewerHref(bootJs.getName()));
                        link.addElement("Interactive EnPlot v2");
                        div.addElement(link);
                        htmlPage.addBlock(div, true);
                    } catch (Throwable jt) {
                        klog.warn("Could not write EnPlot v2 boot script for {}: {}", gsetName, jt.getMessage());
                    }
                }
            }

            // add detailed report table
            htmlPage.addTable(rdf, tsvPage.getName() + "." + tsvPage.getExt(), false);

            // add rest of the images
            if (rl instanceof ScoredDataset && true) {
                // Build extracted dataset based on gene set, maintaining the order in the ScoredDataset (i.e gset order ignored)
                Dataset extractedDSForGSet = new DatasetGenerators().extractRowsSorted((ScoredDataset) rl, gset);
                htmlPage.addHeatMap(gsetName, "Blue-Pink O' Gram in the Space of the Analyzed GeneSet",
                        new GramImagerImpl().createBpogHeatMap(extractedDSForGSet, template_opt),
                        saveDetailFilesInDir, createSvgs);
                if (createGcts) {
                    File gctFile = new File(saveDetailFilesInDir, gsetName + ".gct");
                    GctParser gctExporter = new GctParser();
                    gctExporter.export(extractedDSForGSet, gctFile);
                    StringElement gctLink =
                            HtmlFormat.Links.hyper("GCT file  ", gctFile, " for the data backing the heatmap (for use in external visualizations)", saveDetailFilesInDir);
                    Div div = new Div();
                    htmlPage.addBlock(div, true);
                    div.addElement(gctLink);
                }
            }

            if (rndEss != null && rndEss.getSize() != 0) {
                XChart chart = createESNullDistribHistogram(gsetName, classAName_opt, classBName_opt, es, rndEss);
                htmlPage.addChart(chart, 500, 500, saveDetailFilesInDir, createSvgs);
            }
        } catch (Throwable t) {
            htmlPage.addError("Trouble making HtmlPage", t);
        }

        MyEnrichmentReportImpl mer = new MyEnrichmentReportImpl();
        mer.fHtmlPage = htmlPage;
        mer.fTsvPage = tsvPage;
        mer.fModernPlotFile = modernPlotFile;
        return mer;
    }

    /**
     * @param hitIndices
     * @param esProfile
     * @param rl
     * @param gset
     * @param fann_opt
     * @return
     */
    public static RichDataframe createDetailTable(final String name,
                                                  final int[] hitIndices,
                                                  final Vector esProfile,
                                                  final RankedList rl,
                                                  final GeneSet gset,
                                                  final FeatureAnnot fann_opt) {

        if (rl == null) {
            throw new IllegalArgumentException("Param rl cannot be null");
        }

        final int maxminIndex = hitIndices[esProfile.maxDevFrom0Index()];
        final float maxmin = esProfile.maxDevFrom0();
        boolean pos = XMath.isPositive(maxmin);

        final TIntIntHashMap colIndexFloatPrecMap = new TIntIntHashMap();
        String[] colNames;
        if (fann_opt != null && fann_opt.hasNativeDescriptions()) {
            //                         0         1
            colNames = new String[]{"SYMBOL", "TITLE", 
                    "RANK IN GENE LIST", // 2
                    "RANK METRIC SCORE", "RUNNING ES", // 3 and 4
                    "CORE ENRICHMENT"};
            // TODO: evaluate this setting for report precision consistency
            colIndexFloatPrecMap.put(3, 3);
            colIndexFloatPrecMap.put(4, 4);
        } else {
            colNames = new String[]{"SYMBOL", // 0
                    "RANK IN GENE LIST", // 1
                    "RANK METRIC SCORE", "RUNNING ES", // 2 and 3
                    "CORE ENRICHMENT"};
            // TODO: evaluate this setting for report precision consistency
            colIndexFloatPrecMap.put(2, 3);
            colIndexFloatPrecMap.put(3, 4);
        }

        int signalIndex = colNames.length - 1;

        final TIntObjectHashMap cell_id_linkMap = new TIntObjectHashMap();
        final TIntObjectHashMap cell_id_colorMap = new TIntObjectHashMap();

        final StringMatrix sm = new StringMatrix(hitIndices.length, colNames.length);
        for (int r = 0; r < hitIndices.length; r++) {
            int coln = 0;
            final int rank = hitIndices[r];
            final String symbol = rl.getRankName(rank);
            final double metricScore = rl.getScore(rank);
            final float res = esProfile.getElement(r);

            cell_id_linkMap.put(sm.getElementPos(r, 0), LinkedFactory.createLinkedSymbol(symbol));

            sm.setElement(r, coln++, symbol);
            if (fann_opt != null && fann_opt.hasNativeDescriptions()) {
            	sm.setElement(r, coln++, fann_opt.getNativeDesc(symbol));
            }
            sm.setElement(r, coln++, rank);

            if (!gset.isMember(symbol)) {
                klog.warn("The ranked list content doesnt match the gene set content. Missing member: {}", symbol);
            }

            sm.setElement(r, coln++, metricScore);
            sm.setElement(r, coln++, res);

            if ((pos && hitIndices[r] <= maxminIndex) || (!pos && hitIndices[r] >= maxminIndex)) {
                sm.setElement(r, coln, "Yes");
                cell_id_colorMap.put(sm.getElementPos(r, signalIndex), "#CCFFCC");
            } else {
                sm.setElement(r, coln, "No");
            }
        }

        final StringDataframe sdf = new StringDataframe(name, sm, colNames);
        final RichDataframe.MetaData metaData = new RichDataframe.MetaData("GSEA details", colIndexFloatPrecMap);
        return new RichDataframe(sdf, metaData, cell_id_colorMap, cell_id_linkMap);
    }

    /**
     * @param rl
     * @param template
     * @param gset
     * @param esRaw
     * @param esAreaNorm
     * @param np
     * @return
     */
    public static KeyValTable createSummaryTable(final String dsName,
                                                 final String phenotypeName,
                                                 final String upInClassName_opt,
                                                 final String gsetName,
                                                 final float esRaw,
                                                 final float nes,
                                                 final float np,
                                                 final float fdr,
                                                 final float fwer) {

        KeyValTable table = new KeyValTable();
        table.addRow("Dataset", NamingConventions.splitLongHashName(dsName, "<br>"));
        table.addRow("Phenotype", phenotypeName);
        table.addRow("Upregulated in class", upInClassName_opt);
        table.addRow("GeneSet", gsetName);
        // TODO: review these for report precision consistency.
        // I think it may be enough to simply format these here on the spot with the desired precision.  Something like:
        // table.addRow(key, Printf.format(value));
        table.addRow("Enrichment Score (ES)", esRaw);
        table.addRow("Normalized Enrichment Score (NES)", nes);
        table.addRow("Nominal p-value", np);
        table.addRow("FDR q-value", fdr);
        table.addRow("FWER p-Value", fwer);
        return table;
    }

    // for hits we want a vector of length same as ds but 1's were there
    // are hits and 0's where there are no hits
    // Length of hitProfile vector must be the same as the ranked list
    public static XChart createHitProfileChart(final Vector hitProfile, final RankedList rl) {
    	return _createHitProfileChart(hitProfile, true, null);
    }

    // for hits we want a vector of length same as ds but 1's were there
    // are hits and 0's where there are no hits
    // Length of hitProfile vector must be the same as the ranked list
    private static XChart _createHitProfileChart(final Vector hitProfile, final boolean drawTicks, final IntervalMarker[] markers) {
        String seriesName;
        if (drawTicks) {
            seriesName = "Hits";
        } else {
            seriesName = "";
        }

        String label = "Position in ranked list";

        XYPlot plot = XChartUtils.lineYHits("HIT_LOCATION", "Position in ranked list", seriesName, hitProfile);
        plot.getDomainAxis().setLabel(label);
        plot.getRangeAxis().setTickLabelsVisible(false);
        plot.getRangeAxis().setTickMarksVisible(false);
        plot.setRangeGridlinesVisible(false);
        plot.getRangeAxis().setLabel("");
        plot.getDomainAxis().setTickLabelsVisible(false);
        plot.getDomainAxis().setTickMarksVisible(false);
        plot.getDomainAxis().setVisible(false);
        plot.setDomainGridlinesVisible(false);


        if (drawTicks) {
            plot.getRenderer().setSeriesStroke(0, new BasicStroke(1.0f));
        } else {
            plot.getRenderer().setSeriesStroke(0, new BasicStroke(0.0f));
            plot.setOutlinePaint(Color.BLACK);
        }
        plot.getRenderer().setSeriesPaint(0, Color.BLACK);

        if (markers != null && markers.length > 0) {
            for (int i = 0; i < markers.length; i++) {
                // Clone so gene-set charts do not share mutable IntervalMarker instances.
                IntervalMarker copy = new IntervalMarker(markers[i].getStartValue(), markers[i].getEndValue());
                copy.setPaint(markers[i].getPaint());
                copy.setAlpha(1.0f);
                copy.setOutlineStroke(new BasicStroke(0.0f));
                copy.setOutlinePaint(new Color(0, 0, 0, 0));
                plot.addDomainMarker(0, copy, Layer.BACKGROUND);
            }
        }

        return new XChartImpl("hit_locations", "Lines are drawn to represent positions in the ordered Dataset that match a gene in the GeneSet", new JFreeChart(plot));
    }

    public static XChart createNESvsSignificancePlot(final EnrichmentDb edb) {
        final Vector nessX = edb.getNESS();

        Vector fdrs = edb.getFDRs();

        final Vector[] yss = new Vector[1];
        yss[0] = fdrs;

        final JFreeChart chart = XChartUtils.scatterOneXManyY("NES vs. Significance", new String[]{"FDR q-value"},
                "NES", // x-axis title
                "FDR q-value", // y-axis title
                nessX, yss);
        chart.setBackgroundPaint(CHART_FRAME_COLOR);

        XYPlot plot = (XYPlot) chart.getPlot();
        
        // Adjust plot to match our legacy settings; these changed with JFreeChart 1.5.0
        plot.setAxisOffset(new RectangleInsets(0,0,0,0));
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        NumberAxis axis2 = new NumberAxis("Nominal P-value");
        axis2.setAutoRangeIncludesZero(false);
        plot.setRangeAxis(1, axis2);

        XYDataset data_np = new XYDatasetMultiTmp(new String[]{"nominal p-value"}, nessX, new Vector[]{edb.getNPs()});
        plot.setDataset(1, data_np);
        plot.mapDatasetToRangeAxis(1, 1);

        plot = (XYPlot) chart.getPlot();
        IntervalMarker target = new IntervalMarker(0, 0.25);
        target.setLabelAnchor(RectangleAnchor.LEFT);
        target.setPaint(new Color(255, 255, 210));
        plot.addRangeMarker(target, Layer.BACKGROUND);

        XYItemRenderer renderer = plot.getRenderer();
        if (renderer instanceof StandardXYItemRenderer) {
            StandardXYItemRenderer rr = (StandardXYItemRenderer) renderer;
            rr.setBaseShapesVisible(true);
            rr.setBaseShapesFilled(true);
            rr.setSeriesPaint(0, Color.MAGENTA);
        }

        StandardXYItemRenderer renderer2 = new StandardXYItemRenderer();
        renderer2.setSeriesPaint(0, Color.BLACK);
        renderer2.setBaseShapesVisible(true);
        renderer2.setDrawSeriesLineAsPath(false);
        plot.setRenderer(1, renderer2);

        LegendTitle legend1 = new LegendTitle(renderer);
        LegendTitle legend2 = new LegendTitle(renderer2);
        BlockContainer container = new BlockContainer(new BorderArrangement());
        container.add(legend1, RectangleEdge.LEFT);
        container.add(legend2, RectangleEdge.RIGHT);
        container.add(new EmptyBlock(2000, 0));
        CompositeTitle legends = new CompositeTitle(container);
        legends.setPosition(RectangleEdge.BOTTOM);
        chart.addSubtitle(legends);

        return new XChartImpl("pvalues_vs_nes_plot", "p-values vs. NES for " + edb.getName(), chart);
    }

    public static XChart createGlobalESHistogram(final String phenotypeName, final LabelledVector realEss) {
        int numBins = (realEss.getSize() < 20) ? realEss.getSize() : 20;

        JFreeChart chart = XChartUtils.createHistogram(phenotypeName, "Enrichment score (ES)", "# of gene sets", realEss.getScoresV(false), 
        		numBins);

        chart.getXYPlot().getRenderer().setSeriesStroke(0, new BasicStroke(2.0f)); // make it thicker

        return new XChartImpl("global_es_histogram", "Global histogram of ES for <b>" + phenotypeName + "</b>", chart);
    }

    public static XChart createESNullDistribHistogram(final String gsetName, final String classAName_opt, final String classBName_opt, final float realEs, 
    		final Vector rndEss) {
        int numBins = (rndEss.getSize() < 20) ? rndEss.getSize() : 20;

        JFreeChart chart = XChartUtils.createHistogram(gsetName + ": Random ES distribution", "ES", "P(ES)", rndEss, numBins);
        chart.setBackgroundPaint(CHART_FRAME_COLOR);

        chart.getXYPlot().getRenderer().setSeriesPaint(0, Color.MAGENTA);
        chart.getXYPlot().getRenderer().setSeriesStroke(0, new BasicStroke(2.0f));

        Marker midLine = new ValueMarker(realEs);
        midLine.setPaint(Color.BLACK);
        midLine.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 3, new float[]{5, 3, 3, 3}, 0));
        String label = "Real ES " + Printf.format(realEs, 1);
        midLine.setLabel(label);
        midLine.setLabelBackgroundColor(Color.WHITE);
        if (XMath.isPositive(realEs)) {
            midLine.setLabelAnchor(RectangleAnchor.BOTTOM_LEFT);
        } else {
            midLine.setLabelAnchor(RectangleAnchor.BOTTOM_RIGHT);
        }

        chart.getXYPlot().addDomainMarker(midLine);

        // represents an interval to be highlighted in some manner
        float bias = 100f * (((float) rndEss.extract(ScoreMode.POS_ONLY).getSize()) / ((float) rndEss.getSize()));
        XYTextAnnotation xyt = new XYTextAnnotation("Sets with pos scores: " + Printf.format(bias, 0) + "%", 0, 2);
        chart.getXYPlot().addAnnotation(xyt);

        if (classAName_opt != null && classAName_opt.length() > 0) {
            // represents an interval to be highlighted in some manner
            float max = rndEss.max() - 0.25f;
            IntervalMarker target = new IntervalMarker(max, max); // @note max max so width = 0
            target.setLabel("'" + classAName_opt + "' (Pos ES)");
            target.setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
            target.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
            target.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
            target.setLabelPaint(Color.RED);
            target.setLabelBackgroundColor(Color.WHITE);
            // Hide the IntervalMarker line
            target.setOutlineStroke(new BasicStroke(0.0f));
            target.setOutlinePaint(new Color(0, 0, 0, 0));
            chart.getXYPlot().addDomainMarker(target);
        }

        if (classBName_opt != null && classBName_opt.length() > 0) {
            float min = rndEss.min() + 0.25f;
            IntervalMarker target = new IntervalMarker(min, min);
            target.setLabel("'" + classBName_opt + "' (Neg ES)");
            target.setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
            target.setLabelAnchor(RectangleAnchor.TOP_LEFT);
            target.setLabelTextAnchor(TextAnchor.TOP_LEFT);
            target.setLabelPaint(Color.BLUE);
            target.setLabelBackgroundColor(Color.WHITE);
            // Hide the IntervalMarker line
            target.setOutlineStroke(new BasicStroke(0.0f));
            target.setOutlinePaint(new Color(0, 0, 0, 0));
            chart.getXYPlot().addDomainMarker(target);
        }

        return new XChartImpl("gset_rnd_es_dist", "Gene set null distribution of ES for <b>" + gsetName + "</b>", chart);
    }
    
    // @note an optimization: markers made once as its persistent across gene sets for the same ranked list
    private static EnrichmentCharts _createComboChart(final String gsetName, final Vector enrichmentScoreProfile, final Vector esProfile_full_opt,
    		final Vector hitIndices, final RankedList rl, final String classAName_opt, final String classBName_opt, final IntervalMarker[] markers) {
        if (enrichmentScoreProfile == null) {
            throw new IllegalArgumentException("Param scoreProfile cannot be null");
        }

        if (hitIndices == null) {
            throw new IllegalArgumentException("Param hitProfile cannot be null");
        }

        if (rl == null) {
            throw new IllegalArgumentException("Param rl cannot be null");
        }

        XChart chart0 = createESProfileChart(enrichmentScoreProfile, esProfile_full_opt, hitIndices);
        XChart chart1 = createHitProfileChart(hitIndices, rl);
        XChart chart2 = _createHitProfileChart(hitIndices, false, markers);
        XChart chart3 = RankedListCharts.createRankedListChart(rl, classAName_opt, classBName_opt, enrichmentScoreProfile.maxDevFrom0Index());
        
        // Lots of tweaks to the plots to more closely match our legacy settings; these changed with JFreeChart 1.5.0
        XYPlot plot = chart0.getFreeChart().getXYPlot();
        plot.setAxisOffset(new RectangleInsets(-1,0,0,0));
        plot.setOutlineStroke(new BasicStroke(0.5f,BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        plot.setOutlinePaint(Color.GRAY);
        plot.getRangeAxis().setAxisLinePaint(Color.GRAY);
        plot.getRangeAxis().setAxisLineStroke(new BasicStroke(1.0f,BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        plot = chart1.getFreeChart().getXYPlot();
        plot.setAxisOffset(new RectangleInsets(-1,0,0,0));
        plot.setOutlineStroke(new BasicStroke(0.5f,BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        plot.setOutlinePaint(Color.GRAY);
        plot.getRangeAxis().setAxisLinePaint(Color.GRAY);
        plot.getRangeAxis().setAxisLineStroke(new BasicStroke(1.0f,BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        plot = chart2.getFreeChart().getXYPlot();
        plot.setAxisOffset(new RectangleInsets(-1,0,0,0));
        plot.setOutlineStroke(new BasicStroke(0.5f,BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        plot.setOutlinePaint(Color.GRAY);
        plot.getRangeAxis().setAxisLinePaint(Color.GRAY);
        plot.getRangeAxis().setAxisLineStroke(new BasicStroke(1.0f,BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        plot = chart3.getFreeChart().getXYPlot();
        plot.setAxisOffset(new RectangleInsets(-1,0,0,0));
        plot.setOutlineStroke(new BasicStroke(0.5f,BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        plot.setOutlinePaint(Color.GRAY);        
        plot.getDomainAxis().setAxisLineVisible(true);
        plot.getDomainAxis().setAxisLinePaint(Color.GRAY);
        plot.getDomainAxis().setAxisLineStroke(new BasicStroke(1.0f,BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        plot.getRangeAxis().setAxisLinePaint(Color.GRAY);
        plot.getRangeAxis().setAxisLineStroke(new BasicStroke(1.0f,BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

        // @IMP dont change the prefix
        XComboDomainChart combo = new XComboDomainChart(ENPLOT_ + gsetName, "Enrichment plot: " + gsetName,
        		"Profile of the Running ES Score & Positions of GeneSet Members on the Rank Ordered List", 
        		"Rank in Ordered Dataset", new XChart[]{chart0, chart1, chart2, chart3}, new int[]{12, 4, 1, 8});
        combo.getCombinedXYPlot().setGap(0.0f);
        combo.getCombinedXYPlot().getDomainAxis().setTickLabelsVisible(true);
        combo.getCombinedXYPlot().getDomainAxis().setTickMarksVisible(true);
        combo.getCombinedXYPlot().getDomainAxis().setTickMarkStroke(new BasicStroke(1.0f));
        combo.getCombinedXYPlot().getDomainAxis().setTickMarkPaint(Color.GRAY);
        return new EnrichmentCharts(chart0, chart1, chart2, chart3, combo);
    }

    public static XChart createESProfileChart(final Vector esProfile, final Vector esProfile_full_opt, final Vector hitIndices) {
        JFreeChart chart;
        if (esProfile_full_opt == null) {
            XYDataset data = new EsProfileDataset("Enrichment profile", esProfile, hitIndices);
            chart = ChartFactory.createXYLineChart("Enrichment profile", "Enrichment profile",
                    "Running enrichment score (RES)", data,
                    PlotOrientation.VERTICAL, true, false, false);
        } else {
            XYDataset data = new EsProfileDataset2("Enrichment profile", esProfile_full_opt);
            chart = ChartFactory.createXYLineChart("Enrichment profile", "Enrichment profile",
                    "Running enrichment score (RES)", data,
                    PlotOrientation.VERTICAL, true, false, false);
        }

        XYPlot plot = chart.getXYPlot();
        plot.getRangeAxis().setTickLabelsVisible(true);
        plot.getRangeAxis().setTickMarksVisible(true);
        plot.getRangeAxis().setTickMarkStroke(new BasicStroke(1.0f));
        plot.getRangeAxis().setTickMarkPaint(Color.GRAY);
        plot.getDomainAxis().setTickLabelsVisible(true);
        plot.getDomainAxis().setTickMarksVisible(true);

        // Adjust plot to match our legacy settings; these changed with JFreeChart 1.5.0
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        plot.getRangeAxis().setLabel("Enrichment score (ES)");
        plot.getDomainAxis().setLabel("Position in ranked list");
        plot.getDomainAxis().setVisible(true);

        plot.getRenderer().setSeriesStroke(0, new BasicStroke(2.0f));
        plot.getRenderer().setSeriesPaint(0, Color.GREEN);

        Marker midLine = new ValueMarker(0); // at y = 0
        midLine.setPaint(Color.DARK_GRAY);
        plot.addRangeMarker(midLine);

        return new XChartImpl("Enrichment profile", "Enrichment profile", chart);
    }

    private static Vector _hitIndices2Vector(final int numRows, final int[] hitIndices) {
        // Only set the locations of hits to 1, all else is 0
        Vector v = new Vector(numRows);
        for (int i = 0; i < hitIndices.length; i++) {
            v.setElement(hitIndices[i], 1.0f);
        }

        return v;
    }

    private static File _getGeneSetSizesFile(final GeneSet[] gsets_restricted_to_dataset,
                                             final GeneSet[] origGeneSets,
                                             File geneSets_sizes_file,
                                             final ToolReport report) {

        if (geneSets_sizes_file == null) {
            try {
                final String[] colNames = new String[]{"ORIGINAL SIZE", "AFTER RESTRICTING TO DATASET", "STATUS"};
                final String[] rowNames = new String[origGeneSets.length];
                final StringMatrix sm = new StringMatrix(origGeneSets.length, colNames.length);
                final GeneSetMatrix tmp = new DefaultGeneSetMatrix("after", gsets_restricted_to_dataset);
                for (int r = 0; r < origGeneSets.length; r++) {
                    String gsetName = AuxUtils.getAuxNameOnlyNoHash(origGeneSets[r].getName());
                    rowNames[r] = gsetName;
                    sm.setElement(r, 0, origGeneSets[r].getNumMembers()); // @note _NOT_ after restricting to the rl
                    if (tmp.containsSet(gsetName)) {
                        sm.setElement(r, 1, tmp.getGeneSet(gsetName).getNumMembers()); // already ds qualifed
                    } else {
                        sm.setElement(r, 2, "Rejected!");
                    }
                }

                geneSets_sizes_file = report.savePageTsv(new StringDataframe("gene_set_sizes", sm, rowNames, colNames));
            } catch (Throwable t) {
                klog.error(t.getMessage(), t); // dont penalize - not a critical error
                geneSets_sizes_file = report.createFile("gene_set_sizes_errored_out.txt", "List of gene sets that errored out");
                try {
                    FileUtils.writeStringToFile(geneSets_sizes_file, t.getStackTrace().toString());
                }
                catch (IOException ie) {
                    klog.error(ie.getMessage(), ie);
                }
            }
        }

        return geneSets_sizes_file;
    }


    public static class BasicReportStruc {
        public RichDataframe rdf;
        public EnrichmentReport[] reports; // @note IMP not for ALL just the ones that pass etc
    }

    static class MyEnrichmentReportImpl implements EnrichmentReport {

        private File fPlotFile;
        private File fModernPlotFile;
        private File fHtmlFile;

        private HtmlPage fHtmlPage;
        private TsvPage fTsvPage;

        public File getESPlotFile() {
            return fPlotFile;
        }

        public File getModernESPlotFile() {
            return fModernPlotFile;
        }

        public File getHtmlFile() {
            return fHtmlFile;
        }

    } // End class EnrichmentResultReportImpl

    static class EsProfileDataset implements XYDataset {
        private DatasetGroup fGroup;
        private String[] fSeriesNames;
        private TIntFloatHashMap fYValues;
        private TIntFloatHashMap fJustHitIndices;

        protected EsProfileDataset() { }

        public EsProfileDataset(final String seriesName, final Vector esProfile, final Vector hitIndices_all // actually ALL indices 0 to n-1
        		) {
            this.fGroup = new DatasetGroup();
            this.fSeriesNames = new String[]{seriesName};
            this.fYValues = new TIntFloatHashMap();
            this.fJustHitIndices = new TIntFloatHashMap();

            int cnt = 0;

            // fill in first rank -- starts at 0, score is 0 (might get overwritten below if first is really a hit
            if (hitIndices_all.getElement(0) != 1) {
                fYValues.put(0, 0);
                fJustHitIndices.put(0, 0);
                cnt++;
            }

            int numFound = 0;
            for (int r = 0; r < hitIndices_all.getSize(); r++) {
                if (hitIndices_all.getElement(r) == 1) {
                    fYValues.put(cnt, esProfile.getElement(numFound)); // cnt'th element has a score of foo_es
                    fJustHitIndices.put(cnt, r); // cntth element has an x axis score (i.e rank) of r
                    cnt++;
                    numFound++;
                }
            }

            // fill in last position -- ends at 0
            int lastRank = hitIndices_all.getSize() - 1; // 0 to n-1
            if (hitIndices_all.getElement(lastRank) != 1) {
                fYValues.put(cnt, 0);
                fJustHitIndices.put(cnt, lastRank);
            }

            if (numFound != esProfile.getSize()) {
                throw new IllegalStateException("numFound: " + numFound + " esProfile: " + esProfile.getSize());
            }
        }

        public double getYValue(int series, int item) {
            return fYValues.get(item);
        }

        public double getXValue(int series, int item) {
            return fJustHitIndices.get(item);
        }

        public int getItemCount(int series) {
            return fYValues.size();
        }

        public String toString() {
            return fSeriesNames[0];
        }

        public int getSeriesCount() {
            return fSeriesNames.length;
        }

        public DatasetGroup getGroup() {
            return fGroup;
        }

        public void setGroup(DatasetGroup g) {
            this.fGroup = g;
        }

        // TODO: parameterize the type on the comparable
        public int indexOf(Comparable comparable) {
            return 0;
        }

        public Number getX(int i, int i1) {
            throw new NotImplementedException();
        }

        public Number getY(int i, int i1) {
            throw new NotImplementedException();
        }

        public void addChangeListener(DatasetChangeListener listener) {
        }

        public void removeChangeListener(DatasetChangeListener listener) {
        }

        public DomainOrder getDomainOrder() {
            return DomainOrder.NONE;
        }

        // TODO: parameterize the type on the comparable
        public Comparable getSeriesKey(int series) {
            if (fSeriesNames == null) throw new IllegalStateException("Dataset has no series");

            return fSeriesNames[series];
        }
    }

    static class EsProfileDataset2 implements XYDataset {

        private DatasetGroup fGroup;

        private String[] fSeriesNames;

        private Vector fEsProfile;

        // unlike the above, we have a profile for every point
        public EsProfileDataset2(final String seriesName, final Vector esProfile) {
            this.fGroup = new DatasetGroup();
            this.fSeriesNames = new String[]{seriesName};
            this.fEsProfile = esProfile;
        }

        public double getYValue(int series, int item) {
            return fEsProfile.getElement(item);
        }

        public double getXValue(int series, int item) {
            return item;
        }

        public int getItemCount(int series) {
            return fEsProfile.getSize();
        }

        public String toString() {
            return fSeriesNames[0];
        }

        public int getSeriesCount() {
            return fSeriesNames.length;
        }

        public DatasetGroup getGroup() {
            return fGroup;
        }

        public void setGroup(DatasetGroup g) {
            this.fGroup = g;
        }

        // TODO: parameterize the type on the comparable
        public int indexOf(final Comparable comparable) {
            return 0;
        }

        public Number getX(final int i, final int i1) {
            throw new NotImplementedException();
        }

        public Number getY(int i, int i1) {
            throw new NotImplementedException();
        }

        public void addChangeListener(DatasetChangeListener listener) {
        }

        public void removeChangeListener(DatasetChangeListener listener) {
        }

        public DomainOrder getDomainOrder() {
            return DomainOrder.NONE;
        }

        // TODO: parameterize the type on the comparable
        public Comparable getSeriesKey(int series) {
            if (fSeriesNames == null) throw new IllegalStateException("Dataset has no series");

            return fSeriesNames[series];
        }

    } // End class EsProfileDataset2

    // the standard perm analysis plot (see original allaml science paper)
    // but dont do the log thing for y-axis
    public static XChart createButterflyChart(final PermutationTest ptest) {
    
        List<Vector> xValues = new ArrayList<Vector>();
    
        Dataset pos_sig_levels = ptest.getSigLevels(true);
        Dataset neg_sig_levels = ptest.getSigLevels(false);
        final Vector yValues = new Vector(ptest.getNumMarkers());
        for (int i = 0; i < ptest.getNumMarkers(); i++) {
            yValues.setElement(i, i + 1);
        }
        final float[] sigLevels = ptest.getSigLevels();
        final List<String> xLabels = new ArrayList<String>();
        xLabels.add("Observed pos");
        xLabels.add("Observed neg");
        final RankedList rlReal = ptest.getRankedList();
    
        Vector x1 = new Vector(ptest.getNumMarkers());
        for (int r = 0; r < ptest.getNumMarkers(); r++) {
            x1.setElement(r, rlReal.getScore(r));
        }
    
        xValues.add(x1);
    
        int r = rlReal.getSize() - 1;
        x1 = new Vector(ptest.getNumMarkers());
        for (int cnt = 0; cnt < ptest.getNumMarkers(); cnt++, r--) {
            x1.setElement(cnt, rlReal.getScore(r));
        }
        xValues.add(x1);
    
        for (int c = 0; c < pos_sig_levels.getNumCol(); c++) {
            xValues.add(pos_sig_levels.getColumn(c));
            xValues.add(neg_sig_levels.getColumn(c));
            xLabels.add("Permuted pos " + Printf.format(sigLevels[c] * 100, 0) + "%");
            xLabels.add("Permuted neg " + Printf.format(sigLevels[c] * 100, 0) + "%");
        }
    
        String classAName = null;
        String classBName = null;
        if (ptest.getTemplate().isCategorical()) {
            classAName = ptest.getTemplate().getClassName(0);
            classBName = ptest.getTemplate().getClassName(1);
        }
    
    
        return new XChartImpl("butterfly_plot", "Butterfly plot of significance",
                _createButterflyChart("Butterfly plot for: " + ptest.getName(), "Score (" + ptest.getMetric().getName() + ")",
                        xValues.toArray(new Vector[xValues.size()]),
                        xLabels.toArray(new String[xLabels.size()]), "Gene rank",
                        yValues, classAName, classBName));
    }

    // assumes that vectors and labels are: pos, neg, pos, neg ...
    private static JFreeChart _createButterflyChart(final String chartTitle,
                                                    final String xaxisLabel,
                                                    final Vector[] xValues,
                                                    final String[] xNames,
                                                    final String yaxisLabel,
                                                    final Vector yValues,
                                                    final String classAName_opt,
                                                    final String classBName_opt) {
    
        if (xNames.length != xValues.length) {
            throw new IllegalArgumentException("Unequal lengths for xvectors: " + xValues.length + " and xlabels: " + xNames);
        }
    
        // dont see any way but to duplicate the data
        XYSeries[] serieses = new XYSeries[xValues.length];
        XYSeriesCollection dataset = new XYSeriesCollection();
    
        for (int i = 0; i < xValues.length; i++) {
            serieses[i] = new XYSeries(xNames[i]);
            for (int j = 0; j < xValues[i].getSize(); j++) {
                serieses[i].add(xValues[i].getElement(j), yValues.getElement(j));
            }
            dataset.addSeries(serieses[i]);
        }
    
        // create the chart...
        JFreeChart chart = ChartFactory.createXYLineChart(chartTitle,
                xaxisLabel,
                yaxisLabel,
                dataset,
                PlotOrientation.VERTICAL,
                true, // include legend
                true, // tooltips
                false // urls
        );
    
        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 16));
        chart.getLegend().setBorder(1.0, 1.0, 1.0, 1.0);
    
        // get a reference to the plot for further customisation...
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setAxisOffset(new RectangleInsets(0,0,0,0));
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.GRAY);
        plot.setRangeGridlinePaint(Color.GRAY);
    
        // make the real have dots and not line
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesPaint(1, Color.BLUE);
    
        renderer.setSeriesShape(0, createCircleShape());
        renderer.setSeriesShape(1, createCircleShape());
        renderer.setSeriesLinesVisible(0, false); // realpos
        renderer.setSeriesLinesVisible(1, false); // real neg
    
        // Make colors same for pos and neg for teh same sig level
        // TODO: this should be static, and wrapping the array should be unnecessary
        List<Color> colors = new ArrayList<Color>(Arrays.asList(new Color[]{Color.BLACK, Color.PINK, Color.GREEN}));
        int ii = 0;
        for (int i = 2; i < serieses.length; i = i + 2) {
            renderer.setSeriesShapesVisible(i, false);
            renderer.setSeriesShapesVisible(i + 1, false);
            Paint color;
            if (ii < colors.size()) {
                color = colors.get(ii);
            } else {
                color = renderer.getSeriesPaint(i);
            }
            renderer.setSeriesPaint(i, color);
            renderer.setSeriesPaint(i + 1, color);
    
            ii++;
        }
    
        plot.setRenderer(0, renderer);
    
        // change the auto tick unit selection to integer units only...
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    
        float pos = yValues.max() / 2f;
    
        if (classAName_opt != null && classAName_opt.length() > 0) {
            // represents an interval to be highlighted in some manner
            IntervalMarker target = new IntervalMarker(pos, pos); // @note max max so width = 0
            target.setLabel("'" + classAName_opt + "' (Pos corr)");
            target.setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
            target.setLabelAnchor(RectangleAnchor.RIGHT);
            target.setLabelTextAnchor(TextAnchor.CENTER_RIGHT);
            target.setLabelPaint(Color.RED);
            target.setLabelBackgroundColor(Color.WHITE);
            // Hide the IntervalMarker line
            target.setOutlineStroke(new BasicStroke(0.0f));
            target.setOutlinePaint(new Color(0, 0, 0, 0));
            chart.getXYPlot().addRangeMarker(target);            
        }
    
        if (classBName_opt != null && classBName_opt.length() > 0) {
            IntervalMarker target = new IntervalMarker(pos, pos);
            target.setLabel("'" + classBName_opt + "' (Neg corr)");
            target.setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
            target.setLabelAnchor(RectangleAnchor.LEFT);
            target.setLabelTextAnchor(TextAnchor.CENTER_LEFT);
            target.setLabelPaint(Color.BLUE);
            target.setLabelBackgroundColor(Color.WHITE);
            target.setOutlineStroke(new BasicStroke(0.0f));
            target.setOutlinePaint(new Color(0, 0, 0, 0));
            chart.getXYPlot().addRangeMarker(target);
        }
    
        return chart;
    }
}
