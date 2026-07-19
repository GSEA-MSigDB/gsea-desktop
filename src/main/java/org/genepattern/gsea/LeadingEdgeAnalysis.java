/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  gnu.trove.TFloatIntHashMap
 *  gnu.trove.TFloatIntIterator
 *  gnu.trove.TIntIntHashMap
 *  org.jfree.chart.ChartFactory
 *  org.jfree.chart.JFreeChart
 *  org.jfree.chart.axis.NumberAxis
 *  org.jfree.chart.axis.SymbolAxis
 *  org.jfree.chart.axis.ValueAxis
 *  org.jfree.chart.plot.PlotOrientation
 *  org.jfree.chart.plot.XYPlot
 *  org.jfree.chart.renderer.xy.StandardXYBarPainter
 *  org.jfree.chart.renderer.xy.XYBarPainter
 *  org.jfree.chart.renderer.xy.XYBarRenderer
 *  org.jfree.chart.renderer.xy.XYItemRenderer
 *  org.jfree.chart.ui.RectangleInsets
 *  org.jfree.data.xy.IntervalXYDataset
 *  org.jfree.data.xy.XYSeries
 *  org.jfree.data.xy.XYSeriesCollection
 */
package org.genepattern.gsea;

import edu.mit.broad.genome.alg.AlgUtils;
import edu.mit.broad.genome.alg.ComparatorFactory;
import edu.mit.broad.genome.alg.GeneSetStats;
import edu.mit.broad.genome.alg.gsea.PValueCalculatorImpls;
import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.math.Order;
import edu.mit.broad.genome.objects.BitSetDataset;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.DefaultDataset;
import edu.mit.broad.genome.objects.DefaultGeneSetMatrix;
import edu.mit.broad.genome.objects.GPWrappers;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResult;
import edu.mit.broad.genome.reports.EnrichmentReports;
import edu.mit.broad.xbench.heatmap.DisplayState;
import edu.mit.broad.xbench.heatmap.GramImagerImpl;
import gnu.trove.TFloatIntHashMap;
import gnu.trove.TFloatIntIterator;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIntIterator;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.genepattern.data.expr.IExpressionData;
import org.genepattern.gsea.HCLAlgorithm;
import org.genepattern.heatmap.ColorScheme;
import org.genepattern.heatmap.image.HeatMap;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public final class LeadingEdgeAnalysis {
    private LeadingEdgeAnalysis() {
    }

    public static EnrichmentResult[] getAllResultsFromEdb(EnrichmentDb edb_original) {
        String normModeName = "meandiv";
        PValueCalculatorImpls.GseaImpl pvc = new PValueCalculatorImpls.GseaImpl(normModeName);
        EnrichmentResult[] results = pvc.calcNPValuesAndFDR(edb_original.getResults());
        EnrichmentDb edb = edb_original.cloneDeep(results);
        return edb.getResults(new ComparatorFactory.EnrichmentResultByNESComparator(Order.DESCENDING));
    }

    public static Result runAnalysis(EnrichmentDb edb, String[] gsetNames) {
        Dataset clusteredDataset;
        if (edb == null) {
            throw new IllegalArgumentException("EnrichmentDb cannot be null");
        }
        if (gsetNames == null || gsetNames.length < 2) {
            throw new IllegalArgumentException("Select at least two gene sets for leading edge analysis");
        }
        GeneSet[] gsets = new GeneSet[gsetNames.length];
        for (int r = 0; r < gsetNames.length; ++r) {
            EnrichmentResult result = edb.getResultForGeneSet(gsetNames[r]);
            gsets[r] = result.getSignal().getAsGeneSet();
        }
        DefaultGeneSetMatrix lev_gmx = new DefaultGeneSetMatrix("leading_edge_matrix_for_" + edb.getName(), gsets);
        RankedList rankedList = edb.getRankedList();
        Dataset lev_ds = new BitSetDataset(lev_gmx).toDataset();
        try {
            clusteredDataset = HCLAlgorithm.cluster(lev_ds);
        }
        catch (Throwable t) {
            clusteredDataset = lev_ds;
        }
        Dataset lev_ds_clustered_m = rankedList != null ? LeadingEdgeAnalysis.morph(clusteredDataset, rankedList) : clusteredDataset;
        ColorScheme leColorScheme = rankedList != null ? GPWrappers.createColorScheme_for_lev_with_score(lev_ds_clustered_m) : LeadingEdgeAnalysis.binaryMembershipColorScheme(lev_ds_clustered_m);
        HeatMap leHeatMap = new GramImagerImpl(new DisplayState(leColorScheme)).createBpogHeatMap(lev_ds_clustered_m);
        GeneSet[] reorderedGeneSets = LeadingEdgeAnalysis.reorderGeneSets(gsets, lev_ds_clustered_m);
        Dataset similarityDs = LeadingEdgeAnalysis.buildJaccardSimilarityDataset(reorderedGeneSets);
        ColorScheme simColorScheme = LeadingEdgeAnalysis.jaccardColorScheme(similarityDs);
        HeatMap simHeatMap = new GramImagerImpl(new DisplayState(simColorScheme)).createBpogHeatMap(similarityDs);
        GeneSetStats.RedStruc rs = new GeneSetStats().calcRedundancy(gsets, false);
        JFreeChart geneHistChart = LeadingEdgeAnalysis.createGeneHistogramChart(rs.featureFreq, rankedList);
        JFreeChart jaccardHistChart = LeadingEdgeAnalysis.createJaccardHistogramChart(rs.jaccardDistrib, 0.02);
        return new Result(leHeatMap, simHeatMap, leColorScheme, simColorScheme, geneHistChart, jaccardHistChart, lev_ds_clustered_m, similarityDs, reorderedGeneSets, rs.featureFreq, rankedList, rs.jaccardDistrib, edb.getEdbDir());
    }

    public static JFreeChart createJaccardHistogramChart(TFloatIntHashMap jaccardToOccurrencesMap, double binWidth) {
        if (jaccardToOccurrencesMap == null || binWidth <= 0.0 || binWidth > 1.0) {
            return ChartFactory.createHistogram((String)"", (String)"Jaccard", (String)"Number of Occurences", (IntervalXYDataset)new XYSeriesCollection(), (PlotOrientation)PlotOrientation.VERTICAL, (boolean)false, (boolean)true, (boolean)false);
        }
        TIntIntHashMap binNumberToOccurencesMap = new TIntIntHashMap();
        XYSeries series = new XYSeries((Comparable)((Object)""));
        XYSeriesCollection coll = new XYSeriesCollection();
        coll.addSeries(series);
        TFloatIntIterator it = jaccardToOccurrencesMap.iterator();
        while (it.hasNext()) {
            it.advance();
            float value = it.key();
            int occurences = it.value();
            int bin = (int)((double)value / binWidth);
            int priorOccurences = binNumberToOccurencesMap.get(bin);
            binNumberToOccurencesMap.put(bin, occurences + priorOccurences);
        }
        TIntIntIterator binIt = binNumberToOccurencesMap.iterator();
        while (binIt.hasNext()) {
            binIt.advance();
            series.add((double)binIt.key() * binWidth, (double)binIt.value());
        }
        JFreeChart chart = ChartFactory.createHistogram((String)"", (String)"Jaccard", (String)"Number of Occurences", (IntervalXYDataset)coll, (PlotOrientation)PlotOrientation.VERTICAL, (boolean)false, (boolean)true, (boolean)false);
        LeadingEdgeAnalysis.styleHistogramPlot(chart);
        chart.setBackgroundPaint((Paint)EnrichmentReports.CHART_FRAME_COLOR);
        return chart;
    }

    public static BufferedImage renderJaccardHistogram(TFloatIntHashMap jaccardToOccurrencesMap, double binWidth, int width, int height) {
        return LeadingEdgeAnalysis.createJaccardHistogramChart(jaccardToOccurrencesMap, binWidth).createBufferedImage(width, height);
    }

    public static JFreeChart createGeneHistogramChart(RankedList featureFrequency, RankedList scores) {
        return LeadingEdgeAnalysis.createGeneHistogramChart(featureFrequency, scores, null);
    }

    public static JFreeChart createGeneHistogramChart(RankedList featureFrequency, RankedList scores, AtomicInteger selectedGeneIndex) {
        if (featureFrequency == null || featureFrequency.getSize() == 0) {
            return ChartFactory.createHistogram((String)"", (String)"Gene", (String)"Number Of Gene Sets", (IntervalXYDataset)new XYSeriesCollection(), (PlotOrientation)PlotOrientation.VERTICAL, (boolean)false, (boolean)false, (boolean)false);
        }
        XYSeries series = new XYSeries((Comparable)((Object)"Histogram"));
        XYSeriesCollection coll = new XYSeriesCollection();
        coll.addSeries(series);
        for (int i = 0; i < featureFrequency.getSize(); ++i) {
            series.add((double)i, (double)featureFrequency.getScore(i));
        }
        JFreeChart chart = ChartFactory.createHistogram((String)"", (String)"Gene", (String)"Number Of Gene Sets", (IntervalXYDataset)coll, (PlotOrientation)PlotOrientation.VERTICAL, (boolean)false, (boolean)true, (boolean)false);
        XYPlot plot = chart.getXYPlot();
        LeadingEdgeAnalysis.styleHistogramPlot(chart);
        SymbolAxis xAxis = new SymbolAxis("Gene", featureFrequency.getRankedNamesArray());
        xAxis.setVerticalTickLabels(true);
        xAxis.setTickLabelFont(new Font("SansSerif", 0, 9));
        xAxis.setTickMarkPaint((Paint)Color.GRAY);
        xAxis.setTickMarkStroke((Stroke)new BasicStroke(1.0f));
        xAxis.setAxisLinePaint((Paint)Color.GRAY);
        plot.setDomainAxis((ValueAxis)xAxis);
        final RankedList scoresFinal = scores;
        final RankedList freqFinal = featureFrequency;
        final AtomicInteger selected = selectedGeneIndex;
        XYBarRenderer renderer = new XYBarRenderer(){

            public Paint getItemPaint(int seriesIndex, int item) {
                if (selected != null && item == selected.get()) {
                    return Color.YELLOW;
                }
                if (scoresFinal != null) {
                    float value = scoresFinal.getScore(freqFinal.getRankName(item));
                    return value > 0.0f ? Color.RED : Color.BLUE;
                }
                return super.getItemPaint(seriesIndex, item);
            }
        };
        renderer.setGradientPaintTransformer(null);
        renderer.setBarPainter((XYBarPainter)new StandardXYBarPainter());
        renderer.setShadowVisible(false);
        plot.setRenderer((XYItemRenderer)renderer);
        chart.setBackgroundPaint((Paint)EnrichmentReports.CHART_FRAME_COLOR);
        return chart;
    }

    private static BufferedImage renderGeneHistogram(RankedList featureFrequency, RankedList scores, int width, int height) {
        return LeadingEdgeAnalysis.createGeneHistogramChart(featureFrequency, scores).createBufferedImage(width, height);
    }

    private static void styleHistogramPlot(JFreeChart chart) {
        XYPlot plot = chart.getXYPlot();
        plot.getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        plot.setAxisOffset(new RectangleInsets(0.0, 0.0, 0.0, 0.0));
        plot.setBackgroundPaint((Paint)Color.WHITE);
        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint((Paint)Color.LIGHT_GRAY);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint((Paint)Color.LIGHT_GRAY);
        plot.getRangeAxis().setAxisLinePaint((Paint)Color.GRAY);
        plot.getRangeAxis().setTickMarkPaint((Paint)Color.GRAY);
        if (plot.getRenderer() instanceof XYBarRenderer) {
            XYBarRenderer renderer = (XYBarRenderer)plot.getRenderer();
            renderer.setGradientPaintTransformer(null);
            renderer.setBarPainter((XYBarPainter)new StandardXYBarPainter());
            renderer.setShadowVisible(false);
        }
    }

    private static GeneSet[] reorderGeneSets(GeneSet[] gsets, Dataset clustered) {
        HashMap<String, Integer> geneSetName2Index = new HashMap<String, Integer>();
        for (int i = 0; i < gsets.length; ++i) {
            geneSetName2Index.put(gsets[i].getName(true), i);
        }
        GeneSet[] reordered = new GeneSet[gsets.length];
        for (int i = 0; i < gsets.length; ++i) {
            String geneSetName = clustered.getRowName(i);
            Integer idx = (Integer)geneSetName2Index.get(geneSetName);
            if (idx == null) {
                throw new IllegalStateException("Clustered row name not found in gene sets: " + geneSetName);
            }
            reordered[i] = gsets[idx];
        }
        return reordered;
    }

    private static Dataset buildJaccardSimilarityDataset(GeneSet[] geneSets) {
        int n = geneSets.length;
        Matrix m = new Matrix(n, n);
        String[] names = new String[n];
        for (int i = 0; i < n; ++i) {
            names[i] = geneSets[i].getName(true);
            for (int j = i; j < n; ++j) {
                int intersection = AlgUtils.intersectSize(geneSets[i], geneSets[j]);
                int union = AlgUtils.unionAllCount(new GeneSet[]{geneSets[i], geneSets[j]});
                double jaccard = union == 0 ? 0.0 : (double)intersection / (double)union;
                m.setElement(i, j, (float)jaccard);
            }
        }
        return new DefaultDataset("gene_set_similarity", m, names, names, null);
    }

    private static Dataset morph(Dataset ds, RankedList rl) {
        Matrix m = new Matrix(ds.getNumRow(), ds.getNumCol());
        for (int r = 0; r < ds.getNumRow(); ++r) {
            for (int c = 0; c < ds.getNumCol(); ++c) {
                float score = rl.getScore(ds.getColumnName(c));
                float value = ds.getElement(r, c);
                if (value == 1.0f) {
                    m.setElement(r, c, score);
                    continue;
                }
                m.setElement(r, c, value);
            }
        }
        return new DefaultDataset(ds.getName(), m, ds.getRowNames(), ds.getColumnNames(), ds.getAnnot());
    }

    private static ColorScheme binaryMembershipColorScheme(final Dataset ds) {
        return new ColorScheme(){

            @Override
            public Color getColor(int row, int column) {
                return ds.getElement(row, column) == 0.0f ? Color.WHITE : Color.YELLOW;
            }

            @Override
            public void setDataset(IExpressionData d) {
            }
        };
    }

    private static ColorScheme jaccardColorScheme(final Dataset ds) {
        // Swing GeneSetSimilarityPanel: GradientColorScheme(0,1,0.5, GREEN, RED, WHITE)
        // with setUseDoubleGradient(false) → single WHITE→GREEN map across [0,1].
        return new ColorScheme(){

            @Override
            public Color getColor(int row, int column) {
                float v = ds.getElement(row, column);
                if (v <= 0.0f) {
                    return Color.WHITE;
                }
                if (v >= 1.0f) {
                    return Color.GREEN;
                }
                return LeadingEdgeAnalysis.blend(Color.WHITE, Color.GREEN, v);
            }

            @Override
            public void setDataset(IExpressionData d) {
            }
        };
    }

    private static Color blend(Color a, Color b, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int r = (int)((float)a.getRed() + (float)(b.getRed() - a.getRed()) * t);
        int g = (int)((float)a.getGreen() + (float)(b.getGreen() - a.getGreen()) * t);
        int bl = (int)((float)a.getBlue() + (float)(b.getBlue() - a.getBlue()) * t);
        return new Color(r, g, bl);
    }

    private static BufferedImage emptyImage(int width, int height) {
        return new BufferedImage(Math.max(1, width), Math.max(1, height), 1);
    }

    public static final class Result {
        private final HeatMap leadingEdgeHeatMapModel;
        private final HeatMap similarityHeatMapModel;
        private final ColorScheme leadingEdgeColorScheme;
        private final ColorScheme similarityColorScheme;
        private final BufferedImage leadingEdgeHeatMap;
        private final BufferedImage similarityHeatMap;
        private final JFreeChart geneHistogramChart;
        private final JFreeChart jaccardHistogramChart;
        private final BufferedImage geneHistogram;
        private final BufferedImage jaccardHistogram;
        private final Dataset clusteredMorphed;
        private final Dataset similarityDataset;
        private final GeneSet[] reorderedGeneSets;
        private final RankedList featureFrequency;
        private final RankedList geneScores;
        private final TFloatIntHashMap jaccardDistrib;
        private final File resultDirectory;

        Result(HeatMap leadingEdgeHeatMapModel, HeatMap similarityHeatMapModel, ColorScheme leadingEdgeColorScheme, ColorScheme similarityColorScheme, JFreeChart geneHistogramChart, JFreeChart jaccardHistogramChart, Dataset clusteredMorphed, Dataset similarityDataset, GeneSet[] reorderedGeneSets, RankedList featureFrequency, RankedList geneScores, TFloatIntHashMap jaccardDistrib, File resultDirectory) {
            this.leadingEdgeHeatMapModel = leadingEdgeHeatMapModel;
            this.similarityHeatMapModel = similarityHeatMapModel;
            this.leadingEdgeColorScheme = leadingEdgeColorScheme;
            this.similarityColorScheme = similarityColorScheme;
            this.leadingEdgeHeatMap = leadingEdgeHeatMapModel != null ? leadingEdgeHeatMapModel.snapshot() : null;
            this.similarityHeatMap = similarityHeatMapModel != null ? similarityHeatMapModel.snapshot() : null;
            this.geneHistogramChart = geneHistogramChart;
            this.jaccardHistogramChart = jaccardHistogramChart;
            this.geneHistogram = geneHistogramChart != null ? geneHistogramChart.createBufferedImage(720, 360) : null;
            this.jaccardHistogram = jaccardHistogramChart != null ? jaccardHistogramChart.createBufferedImage(720, 360) : null;
            this.clusteredMorphed = clusteredMorphed;
            this.similarityDataset = similarityDataset;
            this.reorderedGeneSets = reorderedGeneSets;
            this.featureFrequency = featureFrequency;
            this.geneScores = geneScores;
            this.jaccardDistrib = jaccardDistrib;
            this.resultDirectory = resultDirectory;
        }

        public HeatMap getLeadingEdgeHeatMapModel() {
            return this.leadingEdgeHeatMapModel;
        }

        public HeatMap getSimilarityHeatMapModel() {
            return this.similarityHeatMapModel;
        }

        public ColorScheme getLeadingEdgeColorScheme() {
            return this.leadingEdgeColorScheme;
        }

        public ColorScheme getSimilarityColorScheme() {
            return this.similarityColorScheme;
        }

        public BufferedImage getLeadingEdgeHeatMap() {
            return this.leadingEdgeHeatMap;
        }

        public BufferedImage getSimilarityHeatMap() {
            return this.similarityHeatMap;
        }

        public JFreeChart getGeneHistogramChart() {
            return this.geneHistogramChart;
        }

        public JFreeChart getJaccardHistogramChart() {
            return this.jaccardHistogramChart;
        }

        public BufferedImage getGeneHistogram() {
            return this.geneHistogram;
        }

        public BufferedImage getJaccardHistogram() {
            return this.jaccardHistogram;
        }

        public Dataset getClusteredMorphed() {
            return this.clusteredMorphed;
        }

        public Dataset getSimilarityDataset() {
            return this.similarityDataset;
        }

        public GeneSet[] getReorderedGeneSets() {
            return this.reorderedGeneSets;
        }

        public RankedList getFeatureFrequency() {
            return this.featureFrequency;
        }

        public RankedList getGeneScores() {
            return this.geneScores;
        }

        public TFloatIntHashMap getJaccardDistrib() {
            return this.jaccardDistrib;
        }

        public File getResultDirectory() {
            return this.resultDirectory;
        }
    }
}
