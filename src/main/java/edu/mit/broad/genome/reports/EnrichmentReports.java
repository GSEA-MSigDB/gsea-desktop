/*
 * Copyright (c) 2003-2023 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.reports;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.NotImplementedException;
import edu.mit.broad.genome.Printf;
import edu.mit.broad.genome.alg.DatasetGenerators;
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
import edu.mit.broad.genome.swing.GuiHelper;
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
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.BorderArrangement;
import org.jfree.chart.block.EmptyBlock;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.CompositeTitle;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.Layer;
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
    
    public static final Color CHART_FRAME_COLOR = new Color(0xf2, 0xf2, 0xf2);

    public static class Ret {
        public EnrichmentReportDbImpl rdb;
        public EnrichmentDb edb; // with pvalues set
        public File savedInDir;
    }

    private static IntervalMarker[] _markers(final RankedList rl) {
        // @note an optimization
        // some heuristics
        int numRanges = (rl.getSize() < 100) ? rl.getSize() : 100;

        // for the bg shading of the hit plot -- just needs to be made once for all sets on this rl
        return RankedListCharts.createIntervalMarkers(numRanges, rl);
    }


    public static Ret createGseaLikeReport(final EnrichmentDb edb_original, final PrintStream out, final CollapsedDetails cd, final HtmlPage reportIndexPage, final ToolReport report, 
    		final int topXSets, final int minSize, final int maxSize, final boolean makeGeneSetsReport, final boolean makeZippedFile, final boolean createSvgs, final boolean createGcts,
            final GeneSet[] origGeneSets_opt, final String metricName, final String normModeName) {
        FeatureAnnot fann = null;
        if (edb_original.getDataset() != null && edb_original.getDataset().getAnnot() != null) {
            fann = edb_original.getDataset().getAnnot().getFeatureAnnot();
        }

        return createGseaLikeReport(edb_original, out, cd, reportIndexPage, report.getReportDir(), report, topXSets, minSize, maxSize,
                makeGeneSetsReport, makeZippedFile, createSvgs, createGcts, origGeneSets_opt, metricName, normModeName, fann);
    }

    public static Ret createGseaLikeReport(final EnrichmentDb edb_original, final PrintStream out, final CollapsedDetails cd, final HtmlPage reportIndexPage, final ToolReport report, 
    		final int topXSets, final int minSize, final int maxSize, final boolean makeGeneSetsReport, final boolean makeZippedFile, final boolean createSvgs, 
    		final GeneSet[] origGeneSets_opt, final String metricName, final String normModeName, final FeatureAnnot fann_opt) {
        // Note we never create GCTs for this call; this corresponds to Preranked, which has no heatmaps in the report.
        return createGseaLikeReport(edb_original, out, cd, reportIndexPage, report.getReportDir(), report, topXSets, minSize, maxSize,
                makeGeneSetsReport, makeZippedFile, createSvgs, false, origGeneSets_opt, metricName, normModeName, fann_opt);
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
    		final boolean createGcts, final GeneSet[] origGeneSets_opt, final String metricName, final String normModeName, final FeatureAnnot fann_opt) {
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
                "Gene sets enriched in phenotype <b>" + classA_name_long + "<b>", topXSets, makeGeneSetsReport, createSvgs, createGcts, saveInThisDir);
        klog.info("Done FDR reports for positive phenotype");

        final EnrichmentResult[] results_neg = edb.getResults(false);
        final BasicReportStruc neg_basic = createReport(results_neg, name, phenotypeName, classA_name_opt, classB_name_opt, rlReal, template, fann_opt,
                "Gene sets enriched in phenotype <b>" + classB_name_long + "<b>", topXSets, makeGeneSetsReport, createSvgs, createGcts, saveInThisDir);
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
            StringElement line5 = HtmlFormat.Links.hyper("Detailed", "enrichment results in html", pos_basic_html, " format", saveInThisDir);
            StringElement line6 = HtmlFormat.Links.hyper("Detailed", "enrichment results in TSV", pos_basic_tsv, " format (tab delimited text)", saveInThisDir);

            ul.addElement(new LI(line1));
            ul.addElement(new LI(line3));
            ul.addElement(new LI(line2a));
            ul.addElement(new LI(line2b));
            ul.addElement(new LI(line4));
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
            StringElement line5 = HtmlFormat.Links.hyper("Detailed", "enrichment results in html", neg_basic_html, " format", saveInThisDir);
            StringElement line6 = HtmlFormat.Links.hyper("Detailed", "enrichment results in TSV", neg_basic_tsv, " format (tab delimited text)", saveInThisDir);
            ul.addElement(new LI(line1));
            ul.addElement(new LI(line3));
            ul.addElement(new LI(line2a));
            ul.addElement(new LI(line2b));
            ul.addElement(new LI(line4));
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
        if (createSvgs) {
            ul.addElement(new LI(HtmlFormat.Links.hyper("Plot of ", "p-values <i>vs.</i> NES", pvalues_nes_plot_svg_file, "(in compressed SVG format)", saveInThisDir)));
        }
        ul.addElement(new LI(HtmlFormat.Links.hyper("Global ES", global_es_histogram_file, "histogram", saveInThisDir)));
        if (createSvgs) {
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

        // check if there are *any* that are pos
        // actually assume that are are some

        final String[] gsetNames = new String[results.length];
        final TIntObjectHashMap cell_id_linkMap = new TIntObjectHashMap();
        final StringMatrix sm = new StringMatrix(gsetNames.length, BASIC_COL_NAMES.length);

        // for the bg shading of the hit plot -- just needs to be made once for all sets on this rl
        final IntervalMarker[] markers = _markers(rl);

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
                final MyEnrichmentReportImpl mer = createReport(dsName, phenotypeName, phenoClassAName_opt, phenoClassBName_opt, rl, 
                		template_opt, dtg.getGeneSet(), dtg.getScore().getHitIndices(), dtg.getScore().getESProfile(), 
                		dtg.getScore().getESProfile_point_by_point_opt(), result.getScore().getES(), result.getScore().getNES(), 
                		result.getScore().getNP(), result.getScore().getFDR(), result.getScore().getFWER(), dtg.getRndESS(), htmlPage, 
                		fannx, createSvgs, createGcts, markers, saveDetailFilesInDir);

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

                    // @note IMP IMP dont re-use as want this to be light (just files)
                    ereports.add(new EnrichmentReportImpl(htmlFile, plotFile));
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
                img.setSrc(reports[index].getESPlotFile().getName());
                img.setWidth(200);
                img.setHeight(200);
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
    public static MyEnrichmentReportImpl createReport(final String dsName, final String phenotypeName, final String classAName_opt, final String classBName_opt, 
    		final RankedList rl, final Template template_opt, final GeneSet gset, final int[] hitIndices, final Vector esProfile, final Vector esProfile_full_opt, 
    		float es, float nes, float np, final float fdr, final float fwer, final Vector rndEss, final HtmlPage htmlPage, final FeatureAnnot fann_opt, 
    		boolean createSvgs, boolean createGcts, final IntervalMarker[] markers, File saveDetailFilesInDir) {
        TsvPage tsvPage = null;
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
            EnrichmentCharts combo = _createComboChart(gsetName, esProfile, esProfile_full_opt, 
            		_hitIndices2Vector(rl.getSize(), hitIndices), rl, classAName_opt, classBName_opt, markers);
            htmlPage.addChart(combo.comboChart, 500, 500, saveDetailFilesInDir, createSvgs);

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
                markers[i].setAlpha(1.0f);
                // Hide the IntervalMarker line
                markers[i].setOutlineStroke(new BasicStroke(0.0f));
                markers[i].setOutlinePaint(new Color(0, 0, 0, 0));
                plot.addDomainMarker(0, markers[i], Layer.BACKGROUND); // @note add as background
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
        target.setPaint(GuiHelper.COLOR_LIGHT_YELLOW);
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
        private File fHtmlFile;

        private HtmlPage fHtmlPage;
        private TsvPage fTsvPage;

        public File getESPlotFile() {
            return fPlotFile;
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
