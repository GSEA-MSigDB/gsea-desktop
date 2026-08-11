/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.gsea;

import edu.mit.broad.genome.alg.DatasetGenerators;
import edu.mit.broad.genome.alg.gsea.SsGseaProjection;
import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.math.StringMatrix;
import edu.mit.broad.genome.objects.Annot;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.DefaultDataset;
import edu.mit.broad.genome.objects.FeatureAnnot;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.SampleAnnot;
import edu.mit.broad.genome.objects.StringDataframe;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.objects.TemplateMode;
import edu.mit.broad.genome.objects.strucs.CollapsedDetails;
import edu.mit.broad.genome.objects.strucs.DatasetTemplate;
import edu.mit.broad.genome.charts.XChart;
import edu.mit.broad.genome.parsers.GctParser;
import edu.mit.broad.genome.reports.SsGseaBubblePlot;
import edu.mit.broad.genome.reports.SsGseaRocAnalysis;
import edu.mit.broad.genome.reports.SsGseaRocChart;
import edu.mit.broad.genome.reports.SsGseaRocMccPhenotypeReports;
import edu.mit.broad.genome.reports.pages.HtmlFormat;
import edu.mit.broad.genome.reports.pages.HtmlPage;
import edu.mit.broad.xbench.heatmap.GramImagerImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.ecs.html.Div;
import org.apache.ecs.html.H4;
import org.apache.ecs.html.LI;
import org.apache.ecs.html.UL;
import org.genepattern.heatmap.image.HeatMap;
import xtools.api.AbstractTool;
import xtools.api.ToolCategory;
import xtools.api.param.BadParamException;
import xtools.api.param.BooleanParam;
import xtools.api.param.ChipOptParam;
import xtools.api.param.DatasetReqdParam;
import xtools.api.param.FeatureSpaceReqdParam;
import xtools.api.param.GeneSetMatrixMultiChooserParam;
import xtools.api.param.IntegerParam;
import xtools.api.param.ModeReqdParam;
import xtools.api.param.Param;
import xtools.api.param.StringInputParam;
import xtools.api.param.TemplateSingleChooserParam;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CancellationException;

/**
 * Single-sample GSEA (ssGSEA): per-sample enrichment scores for each gene set,
 * matching the GenePattern module at
 * <a href="https://github.com/GSEA-MSigDB/ssGSEA-gpmodule">ssGSEA-gpmodule</a>.
 * Optional 2-class CLS aligns samples for colored heat maps. When a CLS is supplied, ROC/AUC
 * and related reports run automatically (GenePattern <a href="https://github.com/genepattern/ssGSEA.ROC">ssGSEA.ROC</a> in spirit);
 * use {@code roc_plots} / {@code roc_plot_n} only to control optional ROC PNGs.
 */
public class SsGsea extends AbstractTool {

    private static void throwIfCancelled() {
        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            throw new CancellationException("ssGSEA run canceled");
        }
    }

    private final DatasetReqdParam fDatasetParam = new DatasetReqdParam();
    private final ChipOptParam fChipParam = new ChipOptParam(false);
    private final FeatureSpaceReqdParam fFeatureSpaceParam = new FeatureSpaceReqdParam("Collapse");
    private final ModeReqdParam fCollapseModeParam = new ModeReqdParam("mode", "Collapsing mode for probe sets => 1 gene",
            "Collapsing mode for probe sets => 1 gene", "Max_probe",
            new String[]{"Max_probe", "Median_of_probes", "Mean_of_probes", "Sum_of_probes", "Abs_max_of_probes"});
    private final BooleanParam fIncludeOnlySymbols = new BooleanParam("include_only_symbols",
            "Omit features with no symbol match",
            "If there is no known gene symbol match for a probe set omit it from the collapsed dataset", true, false);

    private GeneSetMatrixMultiChooserParam fGeneSetMatrixParam;
    private final IntegerParam fGeneSetMinSizeParam = new IntegerParam("set_min", "Min gene set size",
            "Gene sets with fewer than this many genes (in the database) are excluded", 2, false);
    private final IntegerParam fGeneSetMaxSizeParam = new IntegerParam("set_max", "Max gene set size",
            "Gene sets with more than this many genes (in the database) are excluded", 2000, false);
    private final IntegerParam fMinOverlapParam = new IntegerParam("min_overlap", "Minimum overlap",
            "Minimum number of genes that must overlap the dataset for a set to be scored", 1, false);
    private final ModeReqdParam fSampleNormParam = new ModeReqdParam("sample_norm", "Sample normalization",
            "Per-sample normalization applied before ranking (as in ssGSEA R: none, rank, log, log_rank)", "none",
            new String[]{"none", "rank", "log", "log_rank"});
    private final ModeReqdParam fCombineModeParam = new ModeReqdParam("combine_mode", "UP/DN combine mode",
            "How to combine paired _UP and _DN gene sets (combine_off, combine_replace, combine_add)",
            "combine_add",
            new String[]{"combine_off", "combine_replace", "combine_add"});
    private final StringInputParam fWeightParam = new StringInputParam("weight", "Weighting exponent",
            "Exponent on |z-score| in the enrichment statistic (0 = unweighted; default in R module is 0.75)", "0.75", false);
    private final StringInputParam fAltDelimParam = new StringInputParam("altDelim", "Alternate delimiter",
            "Optional alternate delimiter character for gene set names instead of comma", null, false, new char[]{';'}, Param.ADVANCED);
    private final BooleanParam fMakeZippedReportParam = AbstractTool.createZipReportParam(false);
    private final BooleanParam fBubblePlotsParam = new BooleanParam("bubble_plots", "Per-sample bubble plots",
            "Create one bubble plot per sample (top gene sets by |ssGSEA score|)", true, false);
    private final IntegerParam fBubbleTopNParam = new IntegerParam("bubble_top_n", "Gene sets per bubble plot",
            "Maximum number of gene sets shown in each per-sample bubble plot (by |score|)", 35, false);
    private final BooleanParam fHeatMapParam = new BooleanParam("heatmap", "Global scores heat map",
            "Blue–Pink O' Gram heat map of all ssGSEA scores (gene sets × samples), same rendering as GSEA expression heat maps",
            true, false);
    private final BooleanParam fHeatMapSvgParam = new BooleanParam("heatmap_svg", "SVG for heat map",
            "Also write compressed SVG images for the global heat map page", false, false, Param.ADVANCED);

    private final TemplateSingleChooserParam fClsParam = new TemplateSingleChooserParam(Param.CLS, "Phenotype labels (CLS) — optional",
            TemplateMode.CATEGORICAL_2_CLASS_ONLY, false);
    private final BooleanParam fRocPlotsParam = new BooleanParam("roc_plots", "ROC curve PNGs (with CLS)",
            "When a 2-class CLS is supplied, optionally write ROC PNGs for selected gene sets in ssgsea_roc_plots/ (see roc_plot_n). AUC, MCC, and tables run whenever a CLS is used; this only controls PNGs.", true, false);
    private final IntegerParam fRocPlotNParam = new IntegerParam("roc_plot_n", "ROC: max sets to plot",
            "Up to this many sets get ROC plots, split between highest and lowest |MCC|; 0 = tables only", 8, false, Param.ADVANCED);

    public SsGsea(final Properties properties) {
        super.init(properties, "");
    }

    public SsGsea(final Properties properties, String paramFilePath) {
        super.init(properties, paramFilePath);
    }

    public SsGsea(final String[] args) {
        super.init(args);
    }

    public SsGsea() {
        declareParams();
    }

    @Override
    public String getName() {
        return "ssGSEA";
    }

    @Override
    public String getDesc() {
        return "ssGSEA (single-sample GSEA): per-sample gene-set scores; optional 2-class CLS (enables ROC/AUC tables and reports when present)";
    }

    @Override
    public ToolCategory getCategory() {
        return ToolCategory.GSEA;
    }

    @Override
    public void declareParams() {
        fParamSet.addParamPseudoReqd(fChipParam);
        fGeneSetMatrixParam = new GeneSetMatrixMultiChooserParam(true);
        fParamSet.addParam(fDatasetParam);
        fParamSet.addParamBasic(fClsParam);
        fParamSet.addParam(fGeneSetMatrixParam);
        fParamSet.addParamBasic(fGeneSetMinSizeParam);
        fParamSet.addParamBasic(fGeneSetMaxSizeParam);
        fParamSet.addParamBasic(fMinOverlapParam);
        fParamSet.addParamBasic(fSampleNormParam);
        fParamSet.addParamBasic(fWeightParam);
        fParamSet.addParamBasic(fCombineModeParam);
        fParamSet.addParamPseudoReqd(fFeatureSpaceParam);
        fParamSet.addParamAdv(fCollapseModeParam);
        fParamSet.addParamAdv(fIncludeOnlySymbols);
        fParamSet.addParamAdv(fAltDelimParam);
        fParamSet.addParamBasic(fBubblePlotsParam);
        fParamSet.addParamBasic(fBubbleTopNParam);
        fParamSet.addParamBasic(fHeatMapParam);
        fParamSet.addParamAdv(fHeatMapSvgParam);
        fParamSet.addParamBasic(fRocPlotsParam);
        fParamSet.addParamAdv(fRocPlotNParam);
        fParamSet.addParamAdv(fMakeZippedReportParam);
    }

    @Override
    public void execute() throws Exception {
        throwIfCancelled();
        startExec();
        final long runStartMs = System.currentTimeMillis();

        if (fAltDelimParam.isSpecified() && StringUtils.isNotBlank(fAltDelimParam.getValue().toString())) {
            fGeneSetMatrixParam.setAlternateDelimiter(fAltDelimParam.getValue().toString());
        }

        GeneSet[] origGeneSets = fGeneSetMatrixParam.getGeneSetMatrixCombo().getGeneSets();
        ToolHelper.validateMixedVersionAndSpecies(origGeneSets, fChipParam.getChip(), fReport, log);

        Dataset ds = fDatasetParam.getDataset(fChipParam);
        Dataset fullDs = GseaExpressionDataPrep.uniquizeDataset(ds, fReport);
        final GseaExpressionDataPrep.CollapseOutcome colOut = GseaExpressionDataPrep.collapseForExpressionUse(
                fullDs, fFeatureSpaceParam, fChipParam, fCollapseModeParam, fIncludeOnlySymbols, fReport, null);
        CollapsedDetails.Data cd = colOut.data;
        final File etiologyTsv = colOut.etiologyTsv;
        File globalHeatMapHtml = null;
        File rocAucTsv = null;
        int rocPngN = 0;
        SsGseaRocMccPhenotypeReports.MccPhenoSplit mccPhenoSplit = null;
        String coiClassNameForRoc = null;
        String otherClassNameForRoc = null;

        int minSz = fGeneSetMinSizeParam.getIValue();
        int maxSz = fGeneSetMaxSizeParam.getIValue();
        int minOverlap = fMinOverlapParam.getIValue();
        double weight;
        try {
            String ws = fWeightParam.getNullSafeValue().trim();
            weight = ws.isEmpty() ? 0.75d : Double.parseDouble(ws);
        } catch (NumberFormatException ex) {
            throw new BadParamException("Parameter weight must be a number (got: " + fWeightParam.getValue() + ").", 1091);
        }

        String normKey = fSampleNormParam.getValue().toString();
        if ("log_rank".equals(normKey)) {
            normKey = "log.rank";
        }

        Dataset work = cd.getDataset();
        Template templateOpt = null;
        Template clsTemplateBeforeExtract = null;
        if (fClsParam.isSpecified()) {
            try {
                final Template t = fClsParam.getTemplate();
                if (!t.isCategorical() || t.getNumClasses() != 2) {
                    throw new BadParamException("A 2-class categorical CLS is required when using phenotype labels with ssGSEA (template: " + t.getName() + ").", 1092);
                }
                clsTemplateBeforeExtract = t;
                final DatasetTemplate dtx = DatasetGenerators.extract(work, t, true);
                work = dtx.getDataset();
                templateOpt = dtx.getTemplate();
                fReport.addComment("Phenotype (CLS) applied: " + templateOpt.getName() + " - " + work.getNumCol() + " samples in analysis order.");
            } catch (BadParamException e) {
                throw e;
            } catch (Exception e) {
                throw new BadParamException("Could not apply the CLS to this dataset: " + e.getMessage(), 1093);
            }
        }
        int nr = work.getNumRow();
        int nc = work.getNumCol();
        float[][] matrix = new float[nr][nc];
        String[] rowNames = new String[nr];
        for (int r = 0; r < nr; r++) {
            if ((r & 0x3F) == 0) {
                throwIfCancelled();
            }
            rowNames[r] = work.getRowName(r);
            for (int c = 0; c < nc; c++) {
                matrix[r][c] = work.getElement(r, c);
            }
        }
        SsGseaProjection.applySampleNormalization(matrix, nr, nc, normKey);

        List<String> colNames = work.getColumnNames();
        int[] yBinary = null;
        if (templateOpt != null) {
            final int coiIdx = resolveClassOfInterestForRoc(clsTemplateBeforeExtract, templateOpt);
            coiClassNameForRoc = templateOpt.getClassName(coiIdx);
            fReport.addComment("ROC / AUC: positive label = GSEA class of interest: \"" + coiClassNameForRoc + "\".");
            if (templateOpt.getNumClasses() == 2 && coiIdx >= 0 && coiIdx < 2) {
                otherClassNameForRoc = templateOpt.getClassName(1 - coiIdx);
            }
            yBinary = new int[nc];
            for (int c = 0; c < nc; c++) {
                final Template.Item it = templateOpt.getItemByProfilePos(c);
                final int classIdx = templateOpt.getClassIndex(templateOpt.getClass(it));
                yBinary[c] = (classIdx == coiIdx) ? 1 : 0;
            }
        }
        List<GsScoreRow> scored = new ArrayList<GsScoreRow>();
        for (GeneSet gs : origGeneSets) {
            throwIfCancelled();
            int nMem = gs.getNumMembers();
            if (nMem < minSz || nMem > maxSz) {
                continue;
            }
            List<String> overlap = overlapMembers(gs, rowNames);
            if (overlap.size() < minOverlap) {
                continue;
            }
            Set<Integer> rowIx = SsGseaProjection.rowsForOverlap(rowNames, overlap);
            float[] colScores = new float[nc];
            for (int c = 0; c < nc; c++) {
                if ((c & 0x0F) == 0) {
                    throwIfCancelled();
                }
                colScores[c] = SsGseaProjection.enrichmentScore(matrix, c, rowIx, weight);
            }
            String desc = gs.getNameEnglish() != null ? gs.getNameEnglish() : "";
            scored.add(new GsScoreRow(gs.getName(), desc, colScores));
        }

        if (scored.isEmpty()) {
            throw new BadParamException("No gene sets produced scores (check overlap, sizes, and gene identifiers).", 1090);
        }

        List<GsScoreRow> outputRows = applyCombineMode(scored, fCombineModeParam.getValue().toString());

        Matrix outMat = new Matrix(outputRows.size(), nc);
        List<String> outRowNames = new ArrayList<String>();
        List<String> outDesc = new ArrayList<String>();
        for (int r = 0; r < outputRows.size(); r++) {
            if ((r & 0x3F) == 0) {
                throwIfCancelled();
            }
            GsScoreRow row = outputRows.get(r);
            outRowNames.add(row.name);
            outDesc.add(row.desc);
            for (int c = 0; c < nc; c++) {
                outMat.setElement(r, c, row.scores[c]);
            }
        }

        String outName = work.getName() + "_ssgsea_scores";
        List<String> descForAnnot = null;
        for (String d : outDesc) {
            if (d != null && d.trim().length() > 0) {
                descForAnnot = outDesc;
                break;
            }
        }
        FeatureAnnot outFa = new FeatureAnnot(outName + "_feat", outRowNames, descForAnnot);
        SampleAnnot outSa = new SampleAnnot(outName + "_samples", colNames);
        Annot outAnnot = new Annot(outFa, outSa);
        DefaultDataset outDs = new DefaultDataset(outName, outMat, outRowNames, colNames, outAnnot);
        fReport.addComment("ssGSEA scores: " + outputRows.size() + " gene sets × " + nc + " samples.");
        fReport.savePage(outDs, true);

        final File gct = new File(fReport.getReportDir(), outName + ".gct");
        new GctParser().export(outDs, gct);

        if (fHeatMapParam.isTrue()) {
            try {
                HtmlPage hmPage = new HtmlPage("ssgsea_global_heatmap", "ssGSEA scores — global heat map");
                hmPage.addBreak();
                hmPage.addHtml("&nbsp;&nbsp;");
                HeatMap hm = new GramImagerImpl().createBpogHeatMap(outDs, templateOpt);
                hmPage.addHeatMap("ssgsea_scores_heatmap",
                        "Blue–Pink O' Gram of ssGSEA enrichment scores (gene sets × samples). "
                                + "Color scale is row-relative, as in GSEA gene-set heat maps.",
                        hm, fReport.getReportDir(), fHeatMapSvgParam.isSpecified() && fHeatMapSvgParam.isTrue());
                final File hmFile = fReport.savePage(hmPage, fReport.getReportDir());
                if (hmFile != null) {
                    globalHeatMapHtml = hmFile;
                }
                fReport.addComment("Global heat map page: " + (hmFile != null ? hmFile.getName() : "—"));
            } catch (Throwable t) {
                log.warn("ssGSEA global heat map failed", t);
                fReport.addError("ssGSEA global heat map", t);
            }
        }

        if (fBubblePlotsParam.isTrue()) {
            final List<String> plotNames = new ArrayList<String>(outputRows.size());
            for (GsScoreRow gr : outputRows) {
                plotNames.add(gr.name);
            }
            final File bubbleDir = fReport.createSubDir("ssgsea_bubble_plots");
            final int topN = fBubbleTopNParam.getIValue();
            int rowsShown = topN > 0 ? topN : 200;
            rowsShown = Math.min(rowsShown, outputRows.size());
            final int plotH = Math.min(1600, Math.max(460, 240 + rowsShown * 20));
            fReport.addComment("Per-sample bubble plots: " + nc + " PNGs in ssgsea_bubble_plots/");
            for (int c = 0; c < nc; c++) {
                final float[] colScores = new float[outputRows.size()];
                for (int r = 0; r < outputRows.size(); r++) {
                    colScores[r] = outputRows.get(r).scores[c];
                }
                final XChart chart = SsGseaBubblePlot.createPerSampleChart(colNames.get(c), plotNames, colScores, topN);
                fReport.savePage(chart, 1000, plotH, bubbleDir);
            }
        }

        if (templateOpt != null) {
            try {
                final int nG = outputRows.size();
                final float[][] scmat = new float[nG][nc];
                final List<String> nm = new ArrayList<String>(nG);
                final List<String> dlist = new ArrayList<String>(nG);
                for (int r = 0; r < nG; r++) {
                    GsScoreRow g = outputRows.get(r);
                    nm.add(g.name);
                    dlist.add(g.desc);
                    for (int c = 0; c < nc; c++) {
                        scmat[r][c] = g.scores[c];
                    }
                }
                final List<SsGseaRocAnalysis.ResultRow> rocRows = SsGseaRocAnalysis.computeAll(nm, dlist, scmat, yBinary);
                final String[] tsvCols = SsGseaRocMccPhenotypeReports.ROC_TSV_COLS;
                final StringMatrix sm = new StringMatrix(rocRows.size(), tsvCols.length);
                for (int r = 0; r < rocRows.size(); r++) {
                    final SsGseaRocAnalysis.ResultRow row = rocRows.get(r);
                    sm.setElement(r, 0, row.geneSetName);
                    sm.setElement(r, 1, row.description == null ? "" : row.description);
                    sm.setElement(r, 2, row.auc);
                    sm.setElement(r, 3, row.mcc);
                    sm.setElement(r, 4, row.youdenCutoff);
                    sm.setElement(r, 5, row.sensitivity);
                    sm.setElement(r, 6, row.specificity);
                    sm.setElement(r, 7, row.ppv);
                    sm.setElement(r, 8, row.npv);
                    sm.setElement(r, 9, row.wilcoxP);
                }
                final String[] tsvRowNames = new String[rocRows.size()];
                for (int r = 0; r < rocRows.size(); r++) {
                    tsvRowNames[r] = "r" + (r + 1);
                }
                final String classA = SsGseaRocChart.toFileSafeName(templateOpt.getClassName(0));
                final String classB = SsGseaRocChart.toFileSafeName(templateOpt.getClassName(1));
                final String rocBase = work.getName() + "." + classA + "vs" + classB;
                final StringDataframe sdf = new StringDataframe(rocBase + "_ssgsea_roc", sm, tsvRowNames, tsvCols);
                final File rocTsv = fReport.savePageTsv(sdf, rocBase + ".Results", fReport.getReportDir());
                rocAucTsv = rocTsv;
                fReport.addComment("ssGSEA ROC/AUC (2-class) results: " + rocTsv.getName());
                File rocDir = null;
                if (fRocPlotsParam.isTrue() && fRocPlotNParam.getIValue() > 0) {
                    final int maxPlot = fRocPlotNParam.getIValue();
                    final int half = (maxPlot + 1) / 2;
                    final LinkedHashSet<String> toPlot = new LinkedHashSet<String>();
                    for (int i = 0; i < Math.min(half, rocRows.size()); i++) {
                        toPlot.add(rocRows.get(i).geneSetName);
                    }
                    for (int i = Math.max(0, rocRows.size() - half); i < rocRows.size(); i++) {
                        toPlot.add(rocRows.get(i).geneSetName);
                    }
                    rocDir = fReport.createSubDir("ssgsea_roc_plots");
                    for (String gs : toPlot) {
                        int rowIx = -1;
                        for (int u = 0; u < outputRows.size(); u++) {
                            if (outputRows.get(u).name.equals(gs)) {
                                rowIx = u;
                                break;
                            }
                        }
                        if (rowIx < 0) {
                            continue;
                        }
                        SsGseaRocAnalysis.ResultRow met = null;
                        for (SsGseaRocAnalysis.ResultRow x : rocRows) {
                            if (x.geneSetName.equals(gs)) {
                                met = x;
                                break;
                            }
                        }
                        if (met == null) {
                            continue;
                        }
                        final double[] dscores = toDoubleForRoc(outputRows.get(rowIx).scores);
                        final SsGseaRocAnalysis.RocCurve cr = SsGseaRocAnalysis.buildRocCurve(dscores, yBinary);
                        final XChart cht = SsGseaRocChart.createChart(
                                gs, templateOpt.getClassName(0), templateOpt.getClassName(1), cr, met.auc, met.wilcoxP);
                        fReport.savePage(cht, 720, 600, rocDir);
                    }
                    rocPngN = toPlot.size();
                    fReport.addComment("ROC curve PNGs in: ssgsea_roc_plots/ (" + rocPngN + " file(s))");
                }
                final File rocDirForMcc = new File(fReport.getReportDir(), "ssgsea_roc_plots");
                mccPhenoSplit = SsGseaRocMccPhenotypeReports.write(
                        fReport, fReport.getReportDir(), rocBase, coiClassNameForRoc, rocRows,
                        rocDirForMcc.isDirectory() ? rocDirForMcc : null);
                if (mccPhenoSplit != null) {
                    fReport.addComment("MCC>0 / MCC<0 ssGSEA ROC report pages: "
                            + (mccPhenoSplit.posBasicHtml != null ? mccPhenoSplit.posBasicHtml.getName() : "—")
                            + " / " + (mccPhenoSplit.negBasicHtml != null ? mccPhenoSplit.negBasicHtml.getName() : "—"));
                }
            } catch (Throwable t) {
                log.warn("ssGSEA ROC analysis failed", t);
                fReport.addError("ssGSEA ROC and AUC", t);
            }
        }

        if (fReport.getIndexPage() != null) {
            addSsGseaIndexBlocks(cd, work, gct, etiologyTsv, globalHeatMapHtml, fHeatMapParam.isTrue(), rocAucTsv, rocPngN,
                    fBubblePlotsParam.isTrue(), fGeneSetMinSizeParam.getIValue(), fGeneSetMaxSizeParam.getIValue(), fMinOverlapParam.getIValue(),
                    outputRows.size(), templateOpt, mccPhenoSplit, coiClassNameForRoc, otherClassNameForRoc, fMakeZippedReportParam);
        }

        if (fMakeZippedReportParam.isTrue()) {
            fReport.closeReport(true);
            fReport.zipReport();
        }

        final long elapsedSeconds = (System.currentTimeMillis() - runStartMs) / 1000L;
        fReport.addComment("Run time (ssGSEA execute): " + elapsedSeconds + " seconds.");
        doneExec();
    }

    /**
     * Main index: with a 2-class CLS and ROC, mirrors GSEA report order (phenotype A / phenotype B, then dataset, then other viz).
     * Without CLS/ROC, keeps a compact “basic” list.
     */
    private void addSsGseaIndexBlocks(final CollapsedDetails.Data cd, final Dataset work, final File mainGct, final File etiologyTsv,
            final File globalHeatMapHtml, final boolean heatMapEnabled, final File rocAucTsv, final int rocPngN, final boolean bubbleEnabled,
            final int setMin, final int setMax, final int minOverlap, final int nScored, final Template clsTemplateOpt,
            final SsGseaRocMccPhenotypeReports.MccPhenoSplit mccPheno,
            final String coiClassNameOpt, final String otherClassNameOpt, final BooleanParam makeZip) {

        if (fReport.getIndexPage() == null) {
            return;
        }
        final File rdir = fReport.getReportDir();
        final boolean gseaLayoutRoc = rocAucTsv != null
                && clsTemplateOpt != null
                && coiClassNameOpt != null
                && otherClassNameOpt != null;

        Div d1 = HtmlFormat.Divs.reportSection();
        d1.addElement(new H4("ssGSEA results"));
        UL u1 = new UL();
        u1.addElement(new LI(HtmlFormat.Links.hyper("ssGSEA scores (GCT)", mainGct,
                "— gene set × sample enrichment matrix", rdir)));
        u1.addElement(new LI(nScored + " gene sets scored × " + work.getNumCol() + " samples after size/overlap filters "
                + "(set_min=" + setMin + ", set_max=" + setMax + ", min_overlap=" + minOverlap + ")."));
        d1.addElement(u1);
        fReport.getIndexPage().addBlock(d1, false);

        if (gseaLayoutRoc) {
            Div dCmp = HtmlFormat.Divs.reportSection();
            dCmp.addElement(new H4("Phenotype labels (CLS)"));
            final UL uCmp = new UL();
            uCmp.addElement(new LI("Template: " + clsTemplateOpt.getName()));
            uCmp.addElement(new LI("Comparison: " + otherClassNameOpt + " vs. " + coiClassNameOpt
                    + ". ROC/AUC is computed from ssGSEA scores vs. binary CLS labels; the positive class is \""
                    + coiClassNameOpt + "\"."));
            dCmp.addElement(uCmp);
            fReport.getIndexPage().addBlock(dCmp, false);

            int nPos = 0, nNeg = 0, nZ = 0;
            if (mccPheno != null) {
                nPos = mccPheno.nMccPos;
                nNeg = mccPheno.nMccNeg;
                nZ = mccPheno.nMccZero;
            }
            final Div dCoi = HtmlFormat.Divs.reportSection();
            dCoi.addElement(new H4("ROC in phenotype: " + coiClassNameOpt + " (MCC &gt; 0)"));
            final UL uCoi = new UL();
            if (nScored > 0) {
                uCoi.addElement(new LI(nPos + " / " + nScored + " gene sets have MCC &gt; 0 (at the MCC-selected ROC cutoff, higher ssGSEA score supports phenotype \""
                        + coiClassNameOpt + "\")."));
            }
            if (nZ > 0) {
                uCoi.addElement(new LI(nZ + " / " + nScored + " gene sets have MCC = 0 and are omitted from the two phenotype-specific tables."));
            }
            if (nScored > 0 && nPos == 0) {
                uCoi.addElement(new LI("None of the scored gene sets have MCC &gt; 0 for this comparison."));
            }
            if (mccPheno != null) {
                if (mccPheno.posSnapshotHtml != null) {
                    uCoi.addElement(new LI(HtmlFormat.Links.hyper("Snapshot", mccPheno.posSnapshotHtml, "of ROC results", rdir)));
                }
                if (mccPheno.posBasicHtml != null) {
                    uCoi.addElement(new LI(HtmlFormat.Links.hyper("Detailed", "ROC results in html", mccPheno.posBasicHtml, " format", rdir)));
                }
                if (mccPheno.posTsv != null) {
                    uCoi.addElement(new LI(HtmlFormat.Links.hyper("Detailed", "ROC results in TSV", mccPheno.posTsv,
                            " format (tab delimited text)", rdir)));
                }
            } else {
                uCoi.addElement(new LI("Phenotype-specific table was not produced (see report comments for errors)."));
            }
            uCoi.addElement(new LI(HtmlFormat.Links.hyper("Guide to", getHelpURL() + "GSEA/GSEA_User_Guide/#interpreting-gsea-results",
                    "interpret results")));
            dCoi.addElement(uCoi);
            fReport.getIndexPage().addBlock(dCoi, false);

            final Div dOth = HtmlFormat.Divs.reportSection();
            dOth.addElement(new H4("ROC in phenotype: " + otherClassNameOpt + " (MCC &lt; 0)"));
            final UL uOth = new UL();
            if (nScored > 0) {
                uOth.addElement(new LI(nNeg + " / " + nScored + " gene sets have MCC &lt; 0 (at the MCC-selected ROC cutoff, score direction opposes the class of interest)."));
            }
            if (nScored > 0 && nNeg == 0) {
                uOth.addElement(new LI("None of the scored gene sets have MCC &lt; 0 for this comparison."));
            }
            if (mccPheno != null) {
                if (mccPheno.negSnapshotHtml != null) {
                    uOth.addElement(new LI(HtmlFormat.Links.hyper("Snapshot", mccPheno.negSnapshotHtml, "of ROC results", rdir)));
                }
                if (mccPheno.negBasicHtml != null) {
                    uOth.addElement(new LI(HtmlFormat.Links.hyper("Detailed", "ROC results in html", mccPheno.negBasicHtml, " format", rdir)));
                }
                if (mccPheno.negTsv != null) {
                    uOth.addElement(new LI(HtmlFormat.Links.hyper("Detailed", "ROC results in TSV", mccPheno.negTsv,
                            " format (tab delimited text)", rdir)));
                }
            } else {
                uOth.addElement(new LI("Phenotype-specific table was not produced (see report comments for errors)."));
            }
            uOth.addElement(new LI(HtmlFormat.Links.hyper("Guide to", getHelpURL() + "GSEA/GSEA_User_Guide/#interpreting-gsea-results",
                    "interpret results")));
            dOth.addElement(uOth);
            fReport.getIndexPage().addBlock(dOth, false);

            final Div dGlob = HtmlFormat.Divs.reportSection();
            dGlob.addElement(new H4("Global statistics and tables"));
            final UL uGl = new UL();
            uGl.addElement(new LI(HtmlFormat.Links.hyper("Detailed", "ROC and AUC for all gene sets (TSV)", rocAucTsv,
                    "sorted by |MCC| (MCC is the split/sort statistic, not NES)", rdir)));
            if (rocPngN > 0) {
                final File rocD = new File(rdir, "ssgsea_roc_plots");
                if (rocD.isDirectory()) {
                    uGl.addElement(new LI(HtmlFormat.Links.hyperDir("ROC curve PNGs (subset by |MCC|)", rocD,
                            "(" + rocPngN + " file(s))")));
                }
            }
            dGlob.addElement(uGl);
            fReport.getIndexPage().addBlock(dGlob, false);
        }

        Div dDs = HtmlFormat.Divs.reportSection();
        dDs.addElement(new H4("Dataset details"));
        UL uDs = new UL();
        if (cd != null) {
            if (cd.wasCollapsed) {
                uDs.addElement(new LI("The dataset has " + cd.getNumRow_orig() + " native features."));
                uDs.addElement(new LI("After collapsing features into gene symbols, there are: " + cd.getNumRow_collapsed()
                        + " genes (chip: " + cd.getChipName() + ")."));
            } else {
                uDs.addElement(new LI("The dataset has " + work.getNumRow() + " features (genes); no probe → symbol collapse was applied."));
            }
        }
        uDs.addElement(new LI("Expression / scores matrix name: " + work.getName()));
        dDs.addElement(uDs);
        fReport.getIndexPage().addBlock(dDs, false);

        if (etiologyTsv != null) {
            final Div dE = HtmlFormat.Divs.reportSection();
            dE.addElement(new H4("Probe → gene (collapse)"));
            final UL uE = new UL();
            uE.addElement(new LI(HtmlFormat.Links.hyper("Chip mapping / etiology", etiologyTsv, "(as in the GSEA tool)", rdir)));
            dE.addElement(uE);
            fReport.getIndexPage().addBlock(dE, false);
        }

        final Div dP = HtmlFormat.Divs.reportSection();
        dP.addElement(new H4(gseaLayoutRoc ? "Visualizations" : "Visualizations and tables"));
        final UL uP = new UL();
        if (globalHeatMapHtml != null) {
            uP.addElement(new LI(HtmlFormat.Links.hyper("Global heat map (HTML)", globalHeatMapHtml,
                    "— Blue–Pink O' Gram of scored gene sets × samples (CLS sample bar when a CLS is used)", rdir)));
        } else if (heatMapEnabled) {
            uP.addElement(new LI("Global heat map was not produced (see report comments for errors)."));
        }
        if (bubbleEnabled) {
            final File bdir = new File(rdir, "ssgsea_bubble_plots");
            if (bdir.isDirectory() && bdir.list() != null && bdir.list().length > 0) {
                uP.addElement(new LI(HtmlFormat.Links.hyperDir("Per-sample bubble plot PNGs", bdir,
                        "(" + bdir.list().length + " file(s))")));
            }
        }
        if (!gseaLayoutRoc) {
            if (rocAucTsv != null) {
                uP.addElement(new LI(HtmlFormat.Links.hyper("ROC and AUC (TSV)", rocAucTsv,
                        "all gene sets; AUC, MCC, Youden, Wilcoxon (two-class CLS)", rdir)));
            }
            if (mccPheno != null) {
                if (mccPheno.posSnapshotHtml != null) {
                    uP.addElement(new LI(HtmlFormat.Links.hyper("Snapshot", mccPheno.posSnapshotHtml, "of ROC results (MCC &gt; 0)", rdir)));
                }
                if (mccPheno.posBasicHtml != null) {
                    uP.addElement(new LI(HtmlFormat.Links.hyper("Detailed", "ROC results in html (MCC &gt; 0)", mccPheno.posBasicHtml, " format", rdir)));
                }
                if (mccPheno.posTsv != null) {
                    uP.addElement(new LI(HtmlFormat.Links.hyper("Detailed", "ROC results in TSV (MCC &gt; 0)", mccPheno.posTsv,
                            " format (tab delimited text)", rdir)));
                }
                if (mccPheno.negSnapshotHtml != null) {
                    uP.addElement(new LI(HtmlFormat.Links.hyper("Snapshot", mccPheno.negSnapshotHtml, "of ROC results (MCC &lt; 0)", rdir)));
                }
                if (mccPheno.negBasicHtml != null) {
                    uP.addElement(new LI(HtmlFormat.Links.hyper("Detailed", "ROC results in html (MCC &lt; 0)", mccPheno.negBasicHtml, " format", rdir)));
                }
                if (mccPheno.negTsv != null) {
                    uP.addElement(new LI(HtmlFormat.Links.hyper("Detailed", "ROC results in TSV (MCC &lt; 0)", mccPheno.negTsv,
                            " format (tab delimited text)", rdir)));
                }
            }
            if (rocPngN > 0) {
                final File rocD = new File(rdir, "ssgsea_roc_plots");
                if (rocD.isDirectory()) {
                    uP.addElement(new LI(HtmlFormat.Links.hyperDir("ROC curve PNGs (subset)", rocD, "(" + rocPngN + " file(s))")));
                }
            }
        }
        dP.addElement(uP);
        fReport.getIndexPage().addBlock(dP, false);

        if (clsTemplateOpt != null && !gseaLayoutRoc) {
            final Div dC = HtmlFormat.Divs.reportSection();
            dC.addElement(new H4("Phenotype (CLS)"));
            final UL uCl = new UL();
            uCl.addElement(new LI("2-class template: " + clsTemplateOpt.getName() + " (sample bar in the heat map when provided)."));
            dC.addElement(uCl);
            fReport.getIndexPage().addBlock(dC, false);
        }

        final Div dO = HtmlFormat.Divs.reportSection();
        dO.addElement(new H4("Other"));
        final UL uO = new UL();
        final File pfile = fReport.getParamsFile();
        if (pfile != null && pfile.exists()) {
            uO.addElement(new LI(HtmlFormat.Links.hyper("Parameters", pfile, "used for this analysis", rdir)));
        }
        if (makeZip != null && makeZip.isTrue()) {
            uO.addElement(new LI(HtmlFormat.Links.hyper("Zipped", fReport.getZipReportFile(), "file with all results", rdir)));
        }
        dO.addElement(uO);
        fReport.getIndexPage().addBlock(dO, false);
    }

    private static List<String> overlapMembers(GeneSet gs, String[] rowNames) {
        Set<String> rows = new HashSet<String>();
        for (String r : rowNames) {
            rows.add(r);
        }
        List<String> o = new ArrayList<String>();
        for (String m : gs.getMembers()) {
            if (rows.contains(m)) {
                o.add(m);
            }
        }
        return o;
    }

    /**
     * Port of ssGSEA.Library.R combine_mode for _UP / _DN suffixes.
     */
    private static List<GsScoreRow> applyCombineMode(List<GsScoreRow> scored, String combineMode) {
        if ("combine_off".equals(combineMode)) {
            return scored;
        }
        int n = scored.size();
        String[] names = new String[n];
        for (int i = 0; i < n; i++) {
            names[i] = scored.get(i).name;
        }

        List<GsScoreRow> out = new ArrayList<GsScoreRow>();
        for (int i = 0; i < n; i++) {
            String name = names[i];
            ParsedSuffix ps = parseUpDnSuffix(name);
            if (ps == null) {
                out.add(scored.get(i));
                continue;
            }
            if ("UP".equals(ps.suffix)) {
                int loc = indexOfName(names, ps.body + "_DN");
                if (loc >= 0) {
                    float[] combined = subtract(scored.get(i).scores, scored.get(loc).scores);
                    out.add(new GsScoreRow(ps.body, scored.get(i).desc + " | combined UP & DN", combined));
                    if ("combine_add".equals(combineMode)) {
                        out.add(scored.get(i));
                    }
                } else {
                    out.add(scored.get(i));
                }
            } else if ("DN".equals(ps.suffix)) {
                int loc = indexOfName(names, ps.body + "_UP");
                if (loc < 0) {
                    out.add(scored.get(i));
                } else if ("combine_add".equals(combineMode)) {
                    out.add(scored.get(i));
                }
            }
        }
        return out;
    }

    private static int indexOfName(String[] names, String target) {
        for (int i = 0; i < names.length; i++) {
            if (target.equals(names[i])) {
                return i;
            }
        }
        return -1;
    }

    private static float[] subtract(float[] a, float[] b) {
        float[] r = new float[a.length];
        for (int i = 0; i < a.length; i++) {
            r[i] = a[i] - b[i];
        }
        return r;
    }

    private static ParsedSuffix parseUpDnSuffix(String name) {
        int u = name.lastIndexOf("_UP");
        if (u != -1 && u == name.length() - 3) {
            return new ParsedSuffix(name.substring(0, u), "UP");
        }
        int d = name.lastIndexOf("_DN");
        if (d != -1 && d == name.length() - 3) {
            return new ParsedSuffix(name.substring(0, d), "DN");
        }
        return null;
    }

    private static final class ParsedSuffix {
        final String body;
        final String suffix;

        ParsedSuffix(String body, String suffix) {
            this.body = body;
            this.suffix = suffix;
        }
    }

    private static final class GsScoreRow {
        final String name;
        final String desc;
        final float[] scores;

        GsScoreRow(String name, String desc, float[] scores) {
            this.name = name;
            this.desc = desc;
            this.scores = scores;
        }
    }

    private static double[] toDoubleForRoc(final float[] a) {
        final double[] d = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            d[i] = a[i];
        }
        return d;
    }

    /**
     * GSEA's {@link Template#getClassOfInterestIndex()} (from the parsed CLS) defines which class is
     * "phenotype positive" for analysis. After {@code DatasetGenerators.extract},
     * the post-extract template may be a new instance (defaulting COI to 0), so we resolve COI
     * by class name, then by index, as a fallback.
     */
    private static int resolveClassOfInterestForRoc(final Template clsAsLoaded, final Template afterExtract) {
        if (afterExtract == null) {
            return 0;
        }
        if (clsAsLoaded != null) {
            final String name = clsAsLoaded.getClassOfInterestName();
            if (name != null) {
                for (int i = 0; i < afterExtract.getNumClasses(); i++) {
                    if (name.equalsIgnoreCase(afterExtract.getClass(i).getName())) {
                        return i;
                    }
                }
            }
            final int idx = clsAsLoaded.getClassOfInterestIndex();
            if (idx >= 0 && idx < afterExtract.getNumClasses()) {
                return idx;
            }
        }
        if (afterExtract.getNumClasses() == 1) {
            return 0;
        }
        return 0;
    }

    public static void main(String[] args) {
        SsGsea tool = new SsGsea(args);
        tool_main(tool);
    }
}
