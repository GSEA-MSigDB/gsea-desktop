/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.reports;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.Printf;
import edu.mit.broad.genome.alg.DatasetGenerators;
import edu.mit.broad.genome.alg.gsea.GeneSetScoringTable;
import edu.mit.broad.genome.alg.gsea.Norms;
import edu.mit.broad.genome.alg.gsea.PValueCalculator;
import edu.mit.broad.genome.alg.gsea.PValueCalculatorImpls;
import edu.mit.broad.genome.alg.markers.PermutationTest;
import edu.mit.broad.genome.charts.*;
import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.objects.esmatrix.db.*;
import edu.mit.broad.genome.objects.strucs.CollapsedDetails;
import edu.mit.broad.genome.parsers.AuxUtils;
import edu.mit.broad.genome.parsers.GctParser;
import edu.mit.broad.genome.plots.BubblePlotSpec;
import edu.mit.broad.genome.plots.ColorBarSegment;
import edu.mit.broad.genome.plots.PlotChart;
import edu.mit.broad.genome.plots.XyPlotSpec;
import edu.mit.broad.genome.reports.api.PicFile;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.genome.reports.api.ToolReport;
import edu.mit.broad.genome.reports.pages.*;
import edu.mit.broad.genome.reports.web.LinkedFactory;
import edu.mit.broad.xbench.heatmap.GramImagerImpl;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import xapps.gsea.GseaWebResources;

import org.apache.commons.io.FileUtils;
import org.apache.ecs.StringElement;
import org.apache.ecs.html.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.genepattern.io.ImageUtil;

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

    private static final class BubblePlotData {
        private final int n;
        private final String[] labels;
        private final double[] xs;
        private final double[] ys;
        private final double[] sizePx;
        private final float[] fdrs;
        private final int[] itemArgbs;
        private final double nesNorm;

        private BubblePlotData(int n, String[] labels, double[] xs, double[] ys,
                               double[] sizePx, float[] fdrs, int[] itemArgbs, double nesNorm) {
            this.n = n;
            this.labels = labels;
            this.xs = xs;
            this.ys = ys;
            this.sizePx = sizePx;
            this.fdrs = fdrs;
            this.itemArgbs = itemArgbs;
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
        final List<BubblePlotSpec.Point> points = new ArrayList<>();
        for (int i = 0; i < data.n; i++) {
            String star = "";
            if (data.fdrs[i] < 0.01f) star = "***";
            else if (data.fdrs[i] <= 0.05f) star = "**";
            else if (data.fdrs[i] <= 0.25f) star = "*";
            String label = data.labels[(int) Math.round(data.ys[i])];
            points.add(new BubblePlotSpec.Point(data.xs[i], data.ys[i], data.sizePx[i], data.itemArgbs[i], label, star));
        }
        final String phenoTitle = (phenotypeLabel != null && phenotypeLabel.length() > 0) ? phenotypeLabel : Constants.NA;
        final String caption = "X=-log10(NOM p-value), size=|NES|, color=FDR; * FDR<=0.25 ** FDR<=0.05 *** FDR<0.01"
                + " |NES| norm=" + Printf.format((float) data.nesNorm, 2)
                + " | xMax~" + Printf.format((float) canvasSpec.sharedMaxNegLog10NomP, 2);
        BubblePlotSpec.Options options = BubblePlotSpec.Options.gsea(
                positiveNes,
                data.nesNorm,
                0.0d,
                new double[]{NOM_P_025_X, NOM_P_005_X},
                new String[]{"NOM p = 0.25", "NOM p = 0.05"});
        return new PlotChart(new BubblePlotSpec(
                "gsea_bubble_plot_" + (positiveNes ? "pos" : "neg"),
                "Bubble plot of enrichment in phenotype: " + phenoTitle,
                caption,
                "-log10(NOM p-value)",
                "Gene set",
                points.toArray(new BubblePlotSpec.Point[0]),
                (int) Math.round(canvasSpec.sharedLeftAxisSpacePx),
                canvasSpec.sharedMaxNegLog10NomP,
                options));
    }

    private static XChart createEmptyBubbleChart(final boolean positiveNes, final String description) {
        return new PlotChart(new BubblePlotSpec(
                "gsea_bubble_plot_" + (positiveNes ? "pos" : "neg"),
                "Bubble plot of enrichment results",
                description,
                "-log10(NOM p-value)",
                "Gene set",
                new BubblePlotSpec.Point[0],
                260,
                Double.NaN));
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
        final int[] itemArgbs = new int[n];

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
            itemArgbs[i] = BubblePlotSpec.fdrArgb(fdr, positiveNes);
        }

        return new BubblePlotData(n, labels, xs, ys, sizePx, fdrs, itemArgbs, nesNorm);
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

    protected static final transient Logger klog = LoggerFactory.getLogger(EnrichmentReports.class);

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
    
    public static class Ret {
        public EnrichmentReportDbImpl rdb;
        public EnrichmentDb edb; // with pvalues set
        public File savedInDir;
    }

    /** Color-bar / hit-plot interval markers for a ranked list (recreated per chart to avoid shared mutation). */
    public static ColorBarSegment[] colorBarMarkers(final RankedList rl) {
        return ColorBarSegment.forRankedList(rl);
    }

    /**
     * Build a classic or EnPlot v2 combo chart from an enrichment result (e.g. on-demand in the report viewer).
     * Recreates hit-plot markers each call so charts do not share mutable color-bar state.
     * Recomputes the full rank-by-rank ES curve when it was not persisted in the EDB.
     */
    public static EnrichmentCharts createComboChart(final EnrichmentResult result,
                                                    final boolean enplotV2,
                                                    final String classAName_opt,
                                                    final String classBName_opt,
                                                    final GeneSetScoringTable scoring_opt) {
        return createComboChart(result, enplotV2, classAName_opt, classBName_opt, scoring_opt, null);
    }

    /**
     * @param metricName_opt ranking metric label for the Y-axis (e.g. {@code Signal2Noise});
     *                       applied when the ranked list's {@link MetricWeightStruc} has no name yet
     *                       (common when regenerating plots from a loaded EDB)
     */
    public static EnrichmentCharts createComboChart(final EnrichmentResult result,
                                                    final boolean enplotV2,
                                                    final String classAName_opt,
                                                    final String classBName_opt,
                                                    final GeneSetScoringTable scoring_opt,
                                                    final String metricName_opt) {
        if (result == null) {
            throw new IllegalArgumentException("result cannot be null");
        }
        RankedList rl = result.getRankedList();
        if (rl == null) {
            throw new IllegalArgumentException("result ranked list cannot be null");
        }
        ensureMetricName(rl, metricName_opt);
        EnrichmentScore score = result.getScore();
        Vector hitVector = _hitIndices2Vector(rl.getSize(), score.getHitIndices());
        ColorBarSegment[] markers = colorBarMarkers(rl);
        String gsetName = result.getGeneSet().getName(true);
        Vector fullEs = EnrichmentEsProfiles.fullEsProfile(result, scoring_opt);
        if (enplotV2) {
            return ModernEnrichmentCharts.createComboChart(gsetName, score.getESProfile(),
                    fullEs, hitVector, rl,
                    classAName_opt, classBName_opt, markers,
                    score.getES(), score.getNES(), score.getNP(), score.getFDR());
        }
        return _createComboChart(gsetName, score.getESProfile(),
                fullEs, hitVector, rl,
                classAName_opt, classBName_opt, markers);
    }

    /** Set the ranked-list metric name when missing so mountain axis titles can include it. */
    public static void ensureMetricName(final RankedList rl, final String metricName) {
        if (rl == null || metricName == null || metricName.isBlank()) {
            return;
        }
        MetricWeightStruc mws = rl.getMetricWeightStruc();
        if (mws != null && (mws.getMetricName() == null || mws.getMetricName().isBlank())) {
            mws.setMetricName(metricName);
        }
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
        ensureMetricName(rlReal, metricName);
        final MetricWeightStruc mws = rlReal.getMetricWeightStruc();

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
                    xc.saveAsSVG(butterfly_file_svg, 500, 500);
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
            addPlotJsonLink(ul, bubble_plot_pos_file, "Bubble plot", saveInThisDir);
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
            addPlotJsonLink(ul, bubble_plot_neg_file, "Bubble plot", saveInThisDir);
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
            addPlotJsonLink(ul, butterfly_file, "Butterfly plot", saveInThisDir);
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
        addPlotJsonLink(ul, pvalues_nes_plot_file, "p-values vs. NES plot", saveInThisDir);
        if (createSvgs && pvalues_nes_plot_svg_file != null) {
            ul.addElement(new LI(HtmlFormat.Links.hyper("Plot of ", "p-values <i>vs.</i> NES", pvalues_nes_plot_svg_file, "(in compressed SVG format)", saveInThisDir)));
        }
        ul.addElement(new LI(HtmlFormat.Links.hyper("Global ES", global_es_histogram_file, "histogram", saveInThisDir)));
        addPlotJsonLink(ul, global_es_histogram_file, "Global ES histogram", saveInThisDir);
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

    private static void addPlotJsonLink(final UL ul, final File pngFile, final String label, final File baseDir) {
        if (ul == null || pngFile == null || !pngFile.exists()) {
            return;
        }
        String path = pngFile.getPath();
        File json = path.toLowerCase().endsWith(".png")
                ? new File(path.substring(0, path.length() - 4) + ".plot.json")
                : new File(pngFile.getParentFile(), pngFile.getName() + ".plot.json");
        if (json.exists()) {
            ul.addElement(new LI(HtmlFormat.Links.hyper("", label + " data", json, "(PlotSpec JSON)", baseDir)));
        }
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
        final ColorBarSegment[] markers = colorBarMarkers(rl);

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
    		boolean createSvgs, boolean createGcts, boolean createModernEnplots, final ColorBarSegment[] markers, File saveDetailFilesInDir) {
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
                        hitVector, rl, classAName_opt, classBName_opt, markers, es, nes, np, fdr);
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

    // Length of hitProfile vector must be the same as the ranked list
    // (membership vector helpers live in PlotBuilders.hitRanksFromMembership)

    public static XChart createNESvsSignificancePlot(final EnrichmentDb edb) {
        final Vector nessX = edb.getNESS();
        Vector fdrs = edb.getFDRs();
        Vector nps = edb.getNPs();
        if (nps != null && nessX != null && fdrs != null) {
            double[] x = nessX.toArrayDouble();
            // Historic: dual Y axes (FDR left, NOM p right), yellow FDR≤0.25 band, title "NES vs. Significance".
            XyPlotSpec.Builder b = XyPlotSpec.builder("pvalues_vs_nes_plot", "NES vs. Significance")
                    .caption("p-values vs. NES for " + edb.getName())
                    .xLabel("NES")
                    .yLabel("FDR q-value")
                    .y2Label("Nominal P-value")
                    .yBand(0, 0.25, 0xFFFFFFD2)
                    .legend(true)
                    .legendBottom(true)
                    .marginR(88);
            b.addSeries("FDR q-value", x, fdrs.toArrayDouble(), false, true, 0xFFFF00FF, 0);
            b.addSeries("nominal p-value", x, nps.toArrayDouble(), false, true, 0xFF000000, 1);
            return new PlotChart(b.build());
        }
        return XChartUtils.scatterOneXManyY("NES vs. Significance", new String[]{"FDR q-value"},
                "NES", "FDR q-value", nessX, new Vector[]{fdrs});
    }

    public static XChart createGlobalESHistogram(final String phenotypeName, final LabelledVector realEss) {
        int numBins = (realEss.getSize() < 20) ? realEss.getSize() : 20;
        return XChartUtils.createHistogramChart(phenotypeName, "Enrichment score (ES)", "# of gene sets",
                realEss.getScoresV(false), numBins);
    }

    public static XChart createESNullDistribHistogram(final String gsetName, final String classAName_opt, final String classBName_opt, final float realEs, 
            final Vector rndEss) {
        int numBins = (rndEss.getSize() < 20) ? Math.max(1, rndEss.getSize()) : 20;
        String title = gsetName + ": Random ES distribution";
        StringBuilder caption = new StringBuilder("Observed ES=").append(Printf.format(realEs, 4));
        if (classAName_opt != null && !classAName_opt.isBlank()) {
            caption.append("; pos=").append(classAName_opt);
        }
        if (classBName_opt != null && !classBName_opt.isBlank()) {
            caption.append("; neg=").append(classBName_opt);
        }
        return XChartUtils.createHistogramChart(title, "ES", "P(ES)", rndEss, numBins, realEs, caption.toString());
    }
    
    // @note an optimization: markers made once as its persistent across gene sets for the same ranked list
    private static EnrichmentCharts _createComboChart(final String gsetName, final Vector enrichmentScoreProfile, final Vector esProfile_full_opt,
    		final Vector hitIndices, final RankedList rl, final String classAName_opt, final String classBName_opt, final ColorBarSegment[] markers) {
        return ModernEnrichmentCharts.createComboChart(EnrichmentReports.ENPLOT_, gsetName, enrichmentScoreProfile, esProfile_full_opt,
                hitIndices, rl, classAName_opt, classBName_opt, markers, 0f, 0f, 0f, 0f);
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

        return _createButterflyChart("Butterfly plot for: " + ptest.getName(),
                "Score (" + ptest.getMetric().getName() + ")",
                xValues.toArray(new Vector[xValues.size()]),
                xLabels.toArray(new String[xLabels.size()]),
                "Gene rank",
                yValues, classAName, classBName);
    }

    // assumes that vectors and labels are: pos, neg, pos, neg ...
    private static XChart _createButterflyChart(final String chartTitle,
                                                    final String xaxisLabel,
                                                    final Vector[] xValues,
                                                    final String[] xNames,
                                                    final String yaxisLabel,
                                                    final Vector yValues,
                                                    final String classAName_opt,
                                                    final String classBName_opt) {

        if (xNames.length != xValues.length) {
            throw new IllegalArgumentException("Unequal lengths for xvectors: " + xValues.length + " and xlabels: " + xNames.length);
        }

        final int[] palette = new int[]{0xFFFF0000, 0xFF0000FF, 0xFF000000, 0xFFFFC0CB, 0xFF00FF00};
        XyPlotSpec.Builder b = XyPlotSpec.builder("butterfly_plot", chartTitle)
                .caption("Butterfly plot of significance"
                        + (classAName_opt != null ? " | Pos: " + classAName_opt : "")
                        + (classBName_opt != null ? " | Neg: " + classBName_opt : ""))
                .xLabel(xaxisLabel)
                .yLabel(yaxisLabel)
                .legend(true)
                .legendBottom(true)
                .integerYTicks(true);
        double[] ys = yValues.toArrayDouble();
        for (int i = 0; i < xValues.length; i++) {
            double[] xs = xValues[i].toArrayDouble();
            boolean shapes = i < 2;
            boolean lines = i >= 2;
            int argb;
            if (i < 2) {
                argb = palette[i];
            } else {
                int pair = (i - 2) / 2;
                argb = palette[Math.min(2 + pair, palette.length - 1)];
            }
            b.addSeries(xNames[i], xs, ys, lines, shapes, argb);
        }
        // Historic phenotype labels: IntervalMarkers at mid rank, pinned to plot edges with white plates.
        double midY = 0;
        for (double y : ys) {
            midY = Math.max(midY, y);
        }
        midY = midY / 2.0;
        if (classAName_opt != null && !classAName_opt.isBlank()) {
            b.addAnnotation(0, midY, "'" + classAName_opt + "' (Pos corr)", 0xFFFF0000, "pinRight", true);
        }
        if (classBName_opt != null && !classBName_opt.isBlank()) {
            b.addAnnotation(0, midY, "'" + classBName_opt + "' (Neg corr)", 0xFF0000FF, "pinLeft", true);
        }
        return new PlotChart(b.build());
    }
}
