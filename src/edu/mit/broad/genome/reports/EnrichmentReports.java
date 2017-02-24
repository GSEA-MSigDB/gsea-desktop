/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.reports;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.NotImplementedException;
import edu.mit.broad.genome.Printf;
import edu.mit.broad.genome.alg.ComparatorFactory;
import edu.mit.broad.genome.alg.DatasetGenerators;
import edu.mit.broad.genome.alg.gsea.PValueCalculator;
import edu.mit.broad.genome.alg.gsea.PValueCalculatorImpls;
import edu.mit.broad.genome.alg.markers.PermutationTest;
import edu.mit.broad.genome.charts.*;
import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.models.ASComparable;
import edu.mit.broad.genome.models.XYDatasetMultiTmp;
import edu.mit.broad.genome.models.XYDatasetVERT;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.objects.esmatrix.db.*;
import edu.mit.broad.genome.objects.strucs.CollapsedDetails;
import edu.mit.broad.genome.parsers.AuxUtils;
import edu.mit.broad.genome.parsers.GctParser;
import edu.mit.broad.genome.reports.api.PicFile;
import edu.mit.broad.genome.reports.api.ToolReport;
import edu.mit.broad.genome.reports.pages.*;
import edu.mit.broad.genome.reports.web.LinkedFactory;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.genome.utils.FileUtils;
import edu.mit.broad.vdb.VdbRuntimeResources;
import edu.mit.broad.vdb.chip.Chip;
import edu.mit.broad.vdb.meg.Gene;
import gnu.trove.TIntFloatHashMap;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import org.apache.ecs.StringElement;
import org.apache.ecs.html.*;
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
import org.jfree.data.DomainOrder;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.statistics.HistogramType;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;

import xapps.api.vtools.viewers.VizFactory;

import java.awt.*;
import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Several enrichemnt related reports
 */
public class EnrichmentReports extends ChartHelper {
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


    public static class Ret {
        public EnrichmentReportDbImpl rdb;
        public EnrichmentDb edb; // with pvalues set
        public File savedInDir;
    }

    private static IntervalMarker[] _markers(final RankedList rl) {
        // @note an optimization
        // some heuristics
        int numRanges;
        if (rl.getSize() < 100) {
            numRanges = rl.getSize();
        } else {
            numRanges = 100;
        }
        // for the bg shading of the hit plot -- just needs to be made once for all sets on this rl
        return RankedListCharts.createIntervalMarkers(numRanges, rl);
    }


    private static File _createSubDir(final EnrichmentDb edb,
                                      final ToolReport report,
                                      final boolean makeSubDir) {

        File saveInDir;
        String classA_name;
        String classB_name;

        if (makeSubDir) {
            Template templatex = edb.getTemplate();
            if (templatex != null) {
                if (templatex.isContinuous()) {
                    String nn = AuxUtils.getAuxNameOnlyNoHash(templatex);
                    if (templatex.isContinuous()) {
                        classA_name = nn;
                        classB_name = nn;
                    } else {
                        classA_name = nn + "_pos";
                        classB_name = nn + "_neg";
                    }
                } else {
                    classA_name = templatex.getClassName(0);
                    classB_name = templatex.getClassName(1);
                }
            } else {
                classA_name = "classA";
                classB_name = "classB";
            }

            saveInDir = report.createSubDir(classA_name + "_vs_" + classB_name);
        } else {
            saveInDir = report.getReportDir();
        }

        return saveInDir;
    }

    public static Ret createGseaLikeReport(
            final EnrichmentDb edb_original,
            final PrintStream out,
            final CollapsedDetails cd,
            final HtmlPage reportIndexPage,
            final boolean makeSubDir,
            final ToolReport report,
            final int topXSets,
            final int minSize,
            final int maxSize,
            final boolean makeGeneSetsReport,
            final boolean makeZippedFile,
            final boolean createSvgs,
            final boolean createGcts,
            final GeneSet[] origGeneSets_opt,
            final String metricName,
            final String normModeName) {

        FeatureAnnot fann = null;
        if (edb_original.getDataset() != null && edb_original.getDataset().getAnnot() != null) {
            fann = edb_original.getDataset().getAnnot().getFeatureAnnot();
        }

        return createGseaLikeReport(edb_original, out, cd, reportIndexPage, _createSubDir(edb_original, report, makeSubDir), report,
                topXSets, minSize, maxSize,
                makeGeneSetsReport, makeZippedFile, createSvgs, createGcts, origGeneSets_opt, metricName, normModeName, fann, null, null, null);
    }

    public static Ret createGseaLikeReport(
            final EnrichmentDb edb_original,
            final PrintStream out,
            final CollapsedDetails cd,
            final HtmlPage reportIndexPage,
            final boolean makeSubDir,
            final ToolReport report,
            final int topXSets,
            final int minSize,
            final int maxSize,
            final boolean makeGeneSetsReport,
            final boolean makeZippedFile,
            final boolean createSvgs,
            final GeneSet[] origGeneSets_opt,
            final String metricName,
            final String normModeName,
            final FeatureAnnot fann_opt) {

        // Note we never create GCTs for this call; this corresponds to Preranked, which has no heatmaps in the report.
        return createGseaLikeReport(edb_original, out, cd, reportIndexPage, _createSubDir(edb_original, report, makeSubDir), report,
                topXSets, minSize, maxSize,
                makeGeneSetsReport, makeZippedFile, createSvgs, false, origGeneSets_opt, metricName, normModeName, fann_opt, null, null, null);
    }

    private static String _createPhenotypeName(EnrichmentDb edb) {
        String phenotypeName;

        final Template templatex = edb.getTemplate();
        if (templatex != null) {
            phenotypeName = templatex.getName();
        } else {
            phenotypeName = "NoPhenotypeAvailable";
        }

        return phenotypeName;
    }

    private static String[] _createClassNames(final Template template_opt) {

        String classA_name;
        String classB_name;
        if (template_opt != null) {
            if (template_opt.isContinuous()) {
                String nn = AuxUtils.getAuxNameOnlyNoHash(template_opt);
                classA_name = nn + "_pos";
                classB_name = nn + "_neg";
            } else {
                classA_name = template_opt.getClassName(0);
                classB_name = template_opt.getClassName(1);
            }
        } else {
            classA_name = Constants.NA + "_pos";
            classB_name = Constants.NA + "_neg";
        }

        return new String[]{classA_name, classB_name};
    }

    // @note this is the CORE CORE CORE CORE report making method
    public static Ret createGseaLikeReport(
            final EnrichmentDb edb_original,
            final PrintStream out,
            final CollapsedDetails cd,
            final HtmlPage reportIndexPage,
            final File saveInThisDir,
            final ToolReport report,
            final int topXSets,
            final int minSize,
            final int maxSize,
            final boolean makeGeneSetsReport,
            final boolean makeZippedFile,
            final boolean createSvgs,
            final boolean createGcts,
            final GeneSet[] origGeneSets_opt,
            final String metricName,
            final String normModeName,
            final FeatureAnnot fann_opt,
            String phenotypeName_opt,
            String classA_name_opt,
            String classB_name_opt) {


        if (normModeName == null) {
            throw new IllegalArgumentException("Param normModeName cannot be null");
        }

        if (saveInThisDir == null) {
            throw new IllegalArgumentException("Param saveInThisDir cannot be null");
        }

        if (saveInThisDir.exists() == false) {
            saveInThisDir.mkdir();
        }

        if (phenotypeName_opt == null) {
            phenotypeName_opt = _createPhenotypeName(edb_original);
        }

        if (classA_name_opt == null || classB_name_opt == null) {
            final String[] classNames = _createClassNames(edb_original.getTemplate());
            classA_name_opt = classNames[0];
            classB_name_opt = classNames[1];
        }

        final PValueCalculator pvc = new PValueCalculatorImpls.GseaImpl(normModeName);
        final EnrichmentResult[] results = pvc.calcNPValuesAndFDR(edb_original.getResults());
        final EnrichmentDb edb = edb_original.cloneDeep(results);

        Dataset my_gex_ds_for_heat_map = edb.getDataset();

        final RankedList rlReal = edb.getRankedList();
        final GeneSet[] gsets = edb.getGeneSets();
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
        out.println("Creating marker selection reports ...");
        final RichDataframe rdfGeneList = MiscReports.annotateProbesNames(name, rlReal, fann_opt);
        final File real_gene_list_file_xls = report.savePageXls(rdfGeneList.getDataframe(), "ranked_gene_list_" + classA_name_opt + "_versus_" + classB_name_opt + "_" + report.getTimestamp(), saveInThisDir);
        
        File real_gene_list_heat_map_corr_plot_html_file = null;
        if (my_gex_ds_for_heat_map != null && template != null) {
            HtmlPage real_gene_list_heat_map_corr_plot_html = MiscReports.createDatasetHeatMapAndCorrelationPlots(my_gex_ds_for_heat_map,
                    template, rlReal, 50, saveInThisDir, createSvgs, createGcts);
            real_gene_list_heat_map_corr_plot_html_file = report.savePage(real_gene_list_heat_map_corr_plot_html, saveInThisDir);
        }

        File butterfly_file = null;
        File butterfly_file_svg = null;
        if (edb instanceof EnrichmentDbImplWithPermTest && ((EnrichmentDbImplWithPermTest) edb).getPermutationTest() != null) {
            try {
                final XChart xc = EnrichmentReports.createButterflyChart(((EnrichmentDbImplWithPermTest) edb).getPermutationTest());
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
        out.println("Creating FDR reports ...");
        final EnrichmentResult[] results_pos = edb.getResults(new ComparatorFactory.EnrichmentResultByNESComparator(Order.DESCENDING), true);
        final BasicReportStruc pos_basic = createReport(results_pos, name, phenotypeName_opt, classA_name_opt, classB_name_opt,
                rlReal, template, fann_opt,
                "Gene sets enriched in phenotype <b>" + classA_name_long + "<b>",
                topXSets, makeGeneSetsReport, createSvgs, createGcts, saveInThisDir);
        final RichDataframe pos_basic_rdf = pos_basic.rdf;

        out.println("Done FDR reports for positive phenotype");

        final EnrichmentResult[] results_neg = edb.getResults(new ComparatorFactory.EnrichmentResultByNESComparator(Order.ASCENDING), false);
        final BasicReportStruc neg_basic = createReport(results_neg, name,
                phenotypeName_opt, classA_name_opt, classB_name_opt,
                rlReal, template, fann_opt,
                "Gene sets enriched in phenotype <b>" + classB_name_long + "<b>",
                topXSets, makeGeneSetsReport, createSvgs, createGcts, saveInThisDir);
        final RichDataframe neg_basic_rdf = neg_basic.rdf;

        out.println("Done FDR reports for negative phenotype");

        // Ok done calcs; begin formatting and outputting reports
        final String pos_name = "gsea_report_for_" + classA_name_opt + "_" + report.getTimestamp();
        final String pos_title = "Report for " + classA_name_opt + " " + report.getTimestamp() + " [GSEA]";
        final String neg_name = "gsea_report_for_" + classB_name_opt + "_" + report.getTimestamp();
        final String neg_title = "Report for " + classB_name_opt + " " + report.getTimestamp() + " [GSEA]";

        // Basic statistics
        final File pos_basic_xls = report.savePageXls(pos_basic_rdf, pos_name, saveInThisDir);
        final File neg_basic_xls = report.savePageXls(neg_basic_rdf, neg_name, saveInThisDir);

        HtmlPage htmlPage = new HtmlPage(pos_name, pos_title);
        htmlPage.addTable(pos_basic_rdf, pos_basic_xls.getName(), false, true); // dont show row names (ditto to gs name)
        final File pos_basic_html = report.savePage(htmlPage, saveInThisDir);
        final File pos_snapshot_html = report.savePage(createSnapshotPage(true, pos_basic.reports), saveInThisDir);

        htmlPage = new HtmlPage(neg_name, neg_title);
        htmlPage.addTable(neg_basic_rdf, neg_basic_xls.getName(), false, true); // dont show row names (ditto to gs name)
        final File neg_basic_html = report.savePage(htmlPage, saveInThisDir);
        final File neg_snapshot_html = report.savePage(createSnapshotPage(false, neg_basic.reports), saveInThisDir);

        final XChart pvalues_nes_plot_xc = createNESvsSignificancePlot(edb);
        final File pvalues_nes_plot_file = report.savePage(pvalues_nes_plot_xc, 500, 500, saveInThisDir);
        File pvalues_nes_plot_svg_file = null;
        if (createSvgs) {
            pvalues_nes_plot_svg_file = ImageUtil.getSvgFileFromImgFile(pvalues_nes_plot_file, true);
            report.savePageSvg(pvalues_nes_plot_xc, 500, 500, pvalues_nes_plot_svg_file);
        }

        final XChart global_es_histogram_xc = createGlobalESHistogram(AuxUtils.getAuxNameOnlyNoHash(phenotypeName_opt), edb.getESS_lv());
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

        StringElement line7 = HtmlFormat.Links.hyper("Guide to", "http://www.gsea-msigdb.org/gsea/doc/GSEAUserGuideFrame.html?_Interpreting_GSEA_Results", "interpret results");

        if (edb.getNumScores(true) > 0) {
            StringElement line1 = new StringElement(edb.getNumScores(true) + " / " + gsets.length + " gene sets are upregulated in phenotype <b>" + classA_name_opt + "</b>");
            StringElement line2a = new StringElement(edb.getNumNominallySig(0.01f, true) + " gene sets are significantly enriched at nominal pvalue < 1%");
            StringElement line2b = new StringElement(edb.getNumNominallySig(0.05f, true) + " gene sets are significantly enriched at nominal pvalue < 5%");
            StringElement line3 = new StringElement(edb.getNumFDRSig(0.25f, true) + " gene sets are significant at FDR < 25%");
            StringElement line4 = HtmlFormat.Links.hyper("Snapshot", pos_snapshot_html, "of enrichment results", saveInThisDir);
            StringElement line5 = HtmlFormat.Links.hyper("Detailed", "enrichment results in html", pos_basic_html, " format", saveInThisDir);
            StringElement line6 = HtmlFormat.Links.hyper("Detailed", "enrichment results in excel", pos_basic_xls, " format (tab delimited text)", saveInThisDir);

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
        if (edb.getNumScores(false) > 0) {
            StringElement line1 = new StringElement(edb.getNumScores(false) + " / " + gsets.length + " gene sets are upregulated in phenotype <b>" + classB_name_opt + "</b>");
            StringElement line2a = new StringElement(edb.getNumNominallySig(0.01f, false) + " gene sets are significantly enriched at nominal pvalue < 1%");
            StringElement line2b = new StringElement(edb.getNumNominallySig(0.05f, false) + " gene sets are significantly enriched at nominal pvalue < 5%");
            StringElement line3 = new StringElement(edb.getNumFDRSig(0.25f, false) + " gene sets are significantly enriched at FDR < 25%");
            StringElement line4 = HtmlFormat.Links.hyper("Snapshot", neg_snapshot_html, "of enrichment results", saveInThisDir);
            StringElement line5 = HtmlFormat.Links.hyper("Detailed", "enrichment results in html", neg_basic_html, " format", saveInThisDir);
            StringElement line6 = HtmlFormat.Links.hyper("Detailed", "enrichment results in excel", neg_basic_xls, " format (tab delimited text)", saveInThisDir);
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
            final StringBuffer buf = new StringBuffer("Gene set size filters (min=").append(minSize).append(", max=").append(maxSize).append(")");
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
        StringElement line4 = HtmlFormat.Links.hyper("Detailed", "rank ordered gene list", real_gene_list_file_xls, " for all features in the dataset", saveInThisDir);


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
            StringElement line6 = HtmlFormat.Links.hyper("Buttefly plot", butterfly_file, "of significant genes", saveInThisDir);
            ul.addElement(new LI(line6));
            if (createSvgs) {
                StringElement line6a = HtmlFormat.Links.hyper("Buttefly plot", butterfly_file_svg, "of significant genes (in compressed SVG format)", saveInThisDir);
                ul.addElement(new LI(line6a));
            }
        }
        div.addElement(ul);
        reportIndexPage.addBlock(div, false);

        // ADVANCED REPORTS
        out.println("Creating global reports ...");
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

        out.println("Done all reports!!");

        // @todo @note always save the dataset and template also

        List all_reports = new ArrayList(Arrays.asList(pos_basic.reports));
        all_reports.addAll(Arrays.asList(neg_basic.reports));

        Ret ret = new Ret();
        ret.rdb = new EnrichmentReportDbImpl((EnrichmentReport[]) all_reports.toArray(new EnrichmentReport[all_reports.size()]));
        ret.savedInDir = saveInThisDir;
        ret.edb = edb;
        return ret;
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

        List ereports = new ArrayList();
        for (int r = 0; r < results.length; r++) {
            int coln = 0;
            final EnrichmentResult result = results[r];
            gsetNames[r] = result.getGeneSet().getName(true);
            HtmlPage htmlPage = null;
            sm.setElement(r, coln++, gsetNames[r]);

            if (makeDetailsPage && r < showDetailsForTopXSets) {
                final EnrichmentResult dtg = results[r];
                htmlPage = new HtmlPage(gsetNames[r], "Details for gene set " + gsetNames[r] + "[GSEA]");
                final MyEnrichmentReportImpl mer = createReport(dsName,
                        phenotypeName, phenoClassAName_opt, phenoClassBName_opt,
                        rl, template_opt,
                        dtg.getGeneSet(), dtg.getScore().getHitIndices(),
                        dtg.getScore().getESProfile(),
                        dtg.getScore().getESProfile_point_by_point_opt(),
                        result.getScore().getES(), result.getScore().getNES(), result.getScore().getNP(),
                        result.getScore().getFDR(), result.getScore().getFWER(), dtg.getRndESS(),
                        htmlPage, fannx, true, createSvgs, createGcts, markers, true, saveDetailFilesInDir);

                // dont do this as it saves the pages in memory
                //report.savePage(pages[0]);
                //report.savePage(pages[1]);
                try {
                    File htmlFile = new File(saveDetailFilesInDir, mer.fHtmlPage.getName() + ".html");
                    htmlPage.write(new FileOutputStream(htmlFile));
                    mer.fExcelPage.write(new FileOutputStream(new File(saveDetailFilesInDir, mer.fExcelPage.getName() + ".xls")));

                    PicFile[] pfs = htmlPage.getPicFiles();
                    File plotFile = pfs[0].getFile(); // because image write likes to rename stuff

                    // @note IMP IMP dont re-use as want this to be light (just files)
                    ereports.add(new EnrichmentReportImpl(htmlFile, plotFile));
                } catch (Throwable thr) {
                    klog.error("Error making details: " + gsetNames[r], thr);
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

        StringDataframe sdf = new StringDataframe(dsName + "_basic", sm, gsetNames, BASIC_COL_NAMES, true);
        TIntIntHashMap colPrecision = new TIntIntHashMap();
        // TODO: evaluate these settings for report precision consistency
        colPrecision.put(COL_ES, 2);
        colPrecision.put(COL_NES, 2);
        colPrecision.put(COL_NP, 3);
        colPrecision.put(COL_FDR, 3);
        colPrecision.put(COL_FWER, 3);

        BasicReportStruc struc = new BasicReportStruc();
        RichDataframe.MetaData md = new RichDataframe.MetaData(title, null, null, null, colPrecision);
        struc.rdf = new RichDataframe(sdf, md, null, cell_id_linkMap);
        struc.reports = (EnrichmentReport[]) ereports.toArray(new EnrichmentReport[ereports.size()]);
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

        String name;
        if (pos) {
            name = "pos_snapshot";
        } else {
            name = "neg_snapshot";
        }

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
// one html page and one excel page
    public static MyEnrichmentReportImpl createReport(final String dsName,
                                                      final String phenotypeName,
                                                      final String classAName_opt,
                                                      final String classBName_opt,
                                                      final RankedList rl,
                                                      final Template template_opt,
                                                      final GeneSet gset,
                                                      final int[] hitIndices,
                                                      final Vector esProfile,
                                                      final Vector esProfile_full_opt,
                                                      float es,
                                                      float nes,
                                                      float np,
                                                      final float fdr,
                                                      final float fwer,
                                                      final Vector rndEss,
                                                      final HtmlPage htmlPage,
                                                      final FeatureAnnot fann_opt,
                                                      final boolean doBpog,
                                                      boolean createSvgs,
                                                      boolean createGcts,
                                                      final IntervalMarker[] markers, final boolean horizontal, File saveDetailFilesInDir) {


        ExcelTxtPage excelPage = null;
        EnrichmentCharts combo = null;

        try {

            String gsetName = gset.getName(true);

            if (hitIndices.length != gset.getNumMembers(rl)) {
                throw new IllegalStateException("Mismatched gset: " + gset.getNumMembers(rl) + " and hitIndices: " + hitIndices.length);
            }

            if ((int) esProfile.maxDevFrom0() != (int) es) { // int it as rounding errors
                //TraceUtils.showTrace();
                klog.warn("Possibly mismatched scores: " + esProfile.maxDevFrom0() + " " + es);
            }

            if (esProfile.getSize() != hitIndices.length) {
                throw new IllegalStateException("Mismatched esProfile: " + esProfile.getSize() + " and hitIndices: " + hitIndices.length);
            }

            final RichDataframe rdf = createDetailTable(dsName, hitIndices, esProfile, rl, gset, fann_opt);
            // add summary table and link to details table (excel)

            excelPage = new ExcelTxtPage(htmlPage.getName(), rdf);
            String upInClass;

            if (XMath.isPositive(es)) {
                upInClass = classAName_opt;
            } else {
                upInClass = classBName_opt;
            }

            final KeyValTable table = createSummaryTable(dsName, phenotypeName, upInClass, gset.getName(),
                    es, nes, np,
                    fdr, fwer);

            htmlPage.addTable("GSEA Results Summary", table);

            // add main es plot image (on top -- roels request, makes sense)
            combo = _createComboChart(gsetName, esProfile, esProfile_full_opt,
                    _hitIndices2Vector(rl.getSize(), hitIndices), rl, classAName_opt,
                    classBName_opt, markers, horizontal);
            htmlPage.addChart(combo.comboChart, 500, 500, saveDetailFilesInDir, createSvgs);

            // add detailed report table
            htmlPage.addTable(rdf, excelPage.getName() + "." + excelPage.getExt(), false, true);

            // add rest of the images
            if (rl instanceof ScoredDataset && doBpog) {
                // Build extracted dataset based on gene set, maintaining the order in the ScoredDataset (i.e gset order ignored)
                Dataset extractedDSForGSet = new DatasetGenerators().extractRowsSorted((ScoredDataset) rl, gset);
                htmlPage.addHeatMap(gsetName, "Blue-Pink O' Gram in the Space of the Analyzed GeneSet",
                        VizFactory.createGramImager().createBpogHeatMap(extractedDSForGSet, template_opt),
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
        mer.fPlot = combo;
        mer.fExcelPage = excelPage;
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

        Chip gene_symbol_chip = VdbRuntimeResources.getChip_Gene_Symbol();

        final int maxminIndex = hitIndices[esProfile.maxDevFrom0Index()];
        final float maxmin = esProfile.maxDevFrom0();
        boolean pos = XMath.isPositive(maxmin);

        final TIntIntHashMap colIndexFloatPrecMap = new TIntIntHashMap();
        String[] colNames;
        int symbolIndex;
        int signalIndex;
        if (fann_opt != null && fann_opt.hasNativeDescriptions()) {
            //                         0         1                                2          3
            colNames = new String[]{"PROBE", "DESCRIPTION<br>(from dataset)", "GENE SYMBOL", "GENE_TITLE",
                    "RANK IN GENE LIST", // 4
                    "RANK METRIC SCORE", "RUNNING ES", // 5 and 6
                    "CORE ENRICHMENT"};
            // TODO: evaluate this setting for report precision consistency
            colIndexFloatPrecMap.put(5, 3);
            colIndexFloatPrecMap.put(6, 4);
            symbolIndex = 2;
        } else {
            colNames = new String[]{"PROBE", "GENE SYMBOL", "GENE_TITLE",
                    "RANK IN GENE LIST", // 3
                    "RANK METRIC SCORE", "RUNNING ES", // 4 and 5
                    "CORE ENRICHMENT"};
            symbolIndex = 1;
            // TODO: evaluate this setting for report precision consistency
            colIndexFloatPrecMap.put(4, 3);
            colIndexFloatPrecMap.put(5, 4);
        }

        signalIndex = colNames.length - 1;

        final TIntObjectHashMap cell_id_linkMap = new TIntObjectHashMap();
        final TIntObjectHashMap cell_id_colorMap = new TIntObjectHashMap();
        boolean mightBeGeneSymbolChip = false;

        if (gene_symbol_chip != null) {
            try {
                mightBeGeneSymbolChip = gene_symbol_chip.isProbe(rl.getRankName(hitIndices[0]));
            } catch (Throwable t) {
                mightBeGeneSymbolChip = false;
            }
        }

        final StringMatrix sm = new StringMatrix(hitIndices.length, colNames.length);
        for (int r = 0; r < hitIndices.length; r++) {
            int coln = 0;
            final int rank = hitIndices[r];
            final String probeName = rl.getRankName(rank);
            final double metricScore = rl.getScore(rank);
            final float res = esProfile.getElement(r);
            String desc = null;
            String geneSymbol = null;
            String geneTitle = null;

            try {
                if (fann_opt != null) {
                    desc = fann_opt.getNativeDesc(probeName);
                    geneTitle = fann_opt.getGeneTitle(probeName);
                    geneSymbol = fann_opt.getGeneSymbol(probeName);
                } else if (gene_symbol_chip != null && mightBeGeneSymbolChip) { // try gene symbol chip in any case
                    Gene gene = gene_symbol_chip.getHugo(probeName);
                    if (gene != null) {
                        geneSymbol = gene.getSymbol();
                        geneTitle = gene.getTitle_truncated();
                    }
                }
                if (geneSymbol != null) {
                    cell_id_linkMap.put(sm.getElementPos(r, symbolIndex), LinkedFactory.createLinkedGeneSymbol(geneSymbol));
                }
            } catch (Throwable t) {
                if (r == 0) {
                    t.printStackTrace();
                }
            }

            cell_id_linkMap.put(sm.getElementPos(r, 0), LinkedFactory.createLinkedProbeSet(probeName));

            sm.setElement(r, coln++, probeName);
            if (fann_opt != null && fann_opt.hasNativeDescriptions()) {
                sm.setElement(r, coln++, desc);
            }
            sm.setElement(r, coln++, geneSymbol);
            sm.setElement(r, coln++, geneTitle);
            sm.setElement(r, coln++, new Integer(rank));

            //System.out.println("### >" + probeName + "< " + gset.getPosition(probeName) + "<<<< " + gset.getNumMembers() + " ismember: " + gset.isMember(probeName) + " members: " + gset.getMembers());

            if (!gset.isMember(probeName)) {
                klog.warn("The ranked list content doesnt match the gene set content. Missing mamber: " + probeName);
            }

            //sm.setElement(r, coln++, new Integer(gset.getPosition(probeName)));
            sm.setElement(r, coln++, new Double(metricScore));
            sm.setElement(r, coln++, new Float(res));

            if ((pos && hitIndices[r] <= maxminIndex) || (!pos && hitIndices[r] >= maxminIndex)) {
                sm.setElement(r, coln, "Yes");
                cell_id_colorMap.put(sm.getElementPos(r, signalIndex), "#CCFFCC");
            } else {
                sm.setElement(r, coln, "No");
            }
        }

        final StringDataframe sdf = new StringDataframe(name, sm, colNames, true);
        final RichDataframe.MetaData metaData = new RichDataframe.MetaData("GSEA details", "some caption", null, null, colIndexFloatPrecMap);
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
    public static XChart createHitProfileChart(final Vector hitProfile,
                                               final RankedList rl,
                                               final boolean drawTicks,
                                               final boolean shadeBg,
                                               final boolean horizontal) {

        IntervalMarker[] markers = null;

        if (shadeBg) {
            markers = _markers(rl);
        }

        return _createHitProfileChart(hitProfile, drawTicks, markers, horizontal);
    }

    // for hits we want a vector of length same as ds but 1's were there
    // are hits and 0's where there are no hits
    // Length of hitProfile vector must be the same as the ranked list
    private static XChart _createHitProfileChart(final Vector hitProfile,
                                                 final boolean drawTicks,
                                                 final IntervalMarker[] markers,
                                                 final boolean horizontal) {


        String seriesName;
        if (drawTicks) {
            seriesName = "Hits";
        } else {
            seriesName = "";
        }

        String label = "Position in ranked list";

        XYPlot plot;

        if (horizontal) {
            plot = XChartUtils.lineYHits("HIT_LOCATION", "Position in ranked list", seriesName, hitProfile);
            plot.getDomainAxis().setLabel(label);
        } else {
            XYDataset data = new XYDatasetVERT(hitProfile, seriesName);
            NumberAxis xAxis = new NumberAxis("none");
            xAxis.setAutoRangeIncludesZero(false); // huh
            NumberAxis yAxis = new NumberAxis("Position in ranked list");

            yAxis.setTickMarksVisible(false);
            yAxis.setTickLabelsVisible(true);
            //yAxis.setVisible(false);

            //StandardXYItemRenderer rend = new StandardXYItemRenderer(StandardXYItemRenderer.DISCONTINUOUS_LINES); // makes things dissapear
            StandardXYItemRenderer rend = new StandardXYItemRenderer(StandardXYItemRenderer.LINES);
            // this gives nice shapes
            //StandardXYItemRenderer rend = new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES);
            plot = new XYPlot(data, xAxis, yAxis, rend);
            plot.getRangeAxis().setLabel(label);
        }

        plot.getRangeAxis().setTickLabelsVisible(false);
        plot.getRangeAxis().setTickMarksVisible(false);
        plot.setRangeGridlinesVisible(false);
        plot.getRangeAxis().setLabel("");
        plot.getDomainAxis().setTickLabelsVisible(false);
        plot.getDomainAxis().setTickMarksVisible(false);
        plot.getDomainAxis().setVisible(false);
        plot.setDomainGridlinesVisible(false);


        if (drawTicks) {
            plot.getRenderer().setStroke(new BasicStroke(1.0f));
        } else {
            plot.getRenderer().setStroke(new BasicStroke(0.0f));
        }
        plot.getRenderer().setPaint(Color.BLACK);

        if (markers != null && markers.length > 0) {
            //klog.debug("# SHADING BG of interval markers: " + markers.length);
            for (int i = 0; i < markers.length; i++) {
                if (horizontal) {
                    plot.addDomainMarker(0, markers[i], Layer.BACKGROUND); // @note add as background
                } else {
                    plot.addRangeMarker(0, markers[i], Layer.BACKGROUND); // @note add as background
                }
            }
        }

        return new XChartImpl("hit_locations", "Lines are drawn to represent positions in the ordered Dataset that match a gene in the GeneSet", new JFreeChart(plot));
    }

    public static XChart createNESvsSignificancePlot(final EnrichmentDb edb) {
        final Vector nessX = edb.getNESS();

        Vector fdrs = edb.getFDRs();
        for (int i = 0; i < fdrs.getSize(); i++) {
            fdrs.setElement(i, fdrs.getElement(i) * 100);
        }

        final Vector[] yss = new Vector[1];
        yss[0] = fdrs;

        final JFreeChart chart = XChartUtils.scatterOneXManyY("NES vs. Significance",
                new String[]{"FDR q-value"},
                "NES", // x-axis title
                "FDR q-value", // y-axis title
                nessX,
                yss);


        XYPlot plot = (XYPlot) chart.getPlot();
        NumberAxis axis2 = new NumberAxis("Nominal P-value");
        axis2.setAutoRangeIncludesZero(false);
        plot.setRangeAxis(1, axis2);

        XYDataset data_np = new XYDatasetMultiTmp(new String[]{"nominal p-value"}, nessX, new Vector[]{edb.getNPs()});
        plot.setDataset(1, data_np);
        plot.mapDatasetToRangeAxis(1, 1);

        plot = (XYPlot) chart.getPlot();
        IntervalMarker target = new IntervalMarker(0, 25);
        target.setLabelAnchor(RectangleAnchor.LEFT);
        target.setPaint(GuiHelper.COLOR_LIGHT_YELLOW);
        plot.addRangeMarker(target, Layer.BACKGROUND);

        XYItemRenderer renderer = plot.getRenderer();
        if (renderer instanceof StandardXYItemRenderer) {
            StandardXYItemRenderer rr = (StandardXYItemRenderer) renderer;
            rr.setBaseShapesVisible(true);
            rr.setShapesFilled(true);
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

    /**
     * @param phenotypeName
     * @param realEss
     * @return
     */
    public static XChart createGlobalESHistogram(final String phenotypeName,
                                                 final LabelledVector realEss) {
        int numBins = 20;
        if (realEss.getSize() < 20) {
            numBins = realEss.getSize();
        }

        JFreeChart chart = XChartUtils.createHistogram(phenotypeName,
                false,
                "Enrichment score (ES)",
                "# of gene sets",
                realEss.getScoresV(false),
                true,
                numBins,
                HistogramType.FREQUENCY);

        chart.getXYPlot().getRenderer().setStroke(new BasicStroke(2.0f)); // make it thicker


        return new XChartImpl("global_es_histogram", "Global histogram of ES for <b>" + phenotypeName + "</b>", chart);
    }

    public static XChart createESNullDistribHistogram(final String gsetName,
                                                      final String classAName_opt,
                                                      final String classBName_opt,
                                                      final float realEs,
                                                      final Vector rndEss) {
        int numBins = 20;
        if (rndEss.getSize() < 20) {
            numBins = rndEss.getSize();
        }

        JFreeChart chart = XChartUtils.createHistogram(gsetName + ": Random ES distribution",
                false,
                "ES",
                "P(ES)",
                rndEss,
                true,
                numBins,
                HistogramType.FREQUENCY);


        chart.getXYPlot().getRenderer().setStroke(new BasicStroke(2.0f));
        chart.getXYPlot().getRenderer().setPaint(Color.MAGENTA);

        Marker midLine = new ValueMarker(realEs);
        midLine.setPaint(Color.BLACK);
        midLine.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 3, new float[]{5, 3, 3, 3}, 0));
        String label = "Real ES " + Printf.format(realEs, 1);
        midLine.setLabel(label);
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
            chart.getXYPlot().addDomainMarker(target);
        }

        return new XChartImpl("gset_rnd_es_dist", "Gene set null distribution of ES for <b>" + gsetName + "</b>", chart);
    }

    // @note an optimization: markers made once as its persistent across gene sets for the same ranked list
    public static EnrichmentCharts _createComboChart(final String gsetName,
                                                     final Vector enrichmentScoreProfile,
                                                     final Vector esProfile_full_opt,
                                                     final Vector hitIndices,
                                                     final RankedList rl,
                                                     final String classAName_opt,
                                                     final String classBName_opt,
                                                     final IntervalMarker[] markers,
                                                     final boolean horizontal) {

        if (enrichmentScoreProfile == null) {
            throw new IllegalArgumentException("Param scoreProfile cannot be null");
        }

        if (hitIndices == null) {
            throw new IllegalArgumentException("Param hitProfile cannot be null");
        }

        if (rl == null) {
            throw new IllegalArgumentException("Param rl cannot be null");
        }

        XChart chart0 = createESProfileChart(enrichmentScoreProfile, esProfile_full_opt, hitIndices, horizontal);
        XChart chart1 = createHitProfileChart(hitIndices, rl, true, false, horizontal);
        XChart chart2 = _createHitProfileChart(hitIndices, false, markers, horizontal);
        XChart chart3 = RankedListCharts.createRankedListChart(rl,
                classAName_opt, classBName_opt, enrichmentScoreProfile.maxDevFrom0Index(), horizontal);

        // @IMP dont chnage the prefix
        XComboChart combo;

        if (horizontal) {
            XComboDomainChart cd = new XComboDomainChart(ENPLOT_ + gsetName, "Enrichment plot: " + gsetName,
                    "Profile of the Running ES Score & Positions of GeneSet Members on the Rank Ordered List",
                    "Rank in Ordered Dataset", new XChart[]{chart0, chart1, chart2, chart3}, new int[]{12, 4, 1, 8});
            cd.getCombinedXYPlot().setGap(0);
            cd.getCombinedXYPlot().getDomainAxis().setTickLabelsVisible(true);
            cd.getCombinedXYPlot().getDomainAxis().setTickMarksVisible(true);
            combo = cd;
        } else {
            XComboRangeChart cr = new XComboRangeChart(ENPLOT_ + gsetName, "Enrichment plot: " + gsetName,
                    "Profile of the Running ES Score & Positions of GeneSet Members on the Rank Ordered List",
                    "Rank in Ordered Dataset", new XChart[]{chart0, chart1, chart2, chart3}, new int[]{12, 4, 1, 8});
            cr.getCombinedXYPlot().setGap(0);
            cr.getCombinedXYPlot().getRangeAxis().setTickLabelsVisible(true);
            cr.getCombinedXYPlot().getRangeAxis().setTickMarksVisible(true);
            combo = cr;
        }

        return new EnrichmentCharts(chart0, chart1, chart2, chart3, combo);
    }

    /**
     * @param hitProfile
     * @param esProfile
     * @return
     */
    public static XChart createESProfileChart(final Vector esProfile,
                                              final Vector esProfile_full_opt,
                                              final Vector hitIndices,
                                              final boolean horizontal) {

        JFreeChart chart;

        //klog.debug(">>>>> esProfile: " + esProfile + " esProfile_full_opt: " + esProfile_full_opt);

        if (esProfile_full_opt == null) {
            XYDataset data = new EsProfileDataset("Enrichment profile", esProfile, hitIndices, horizontal);
            chart = ChartFactory.createXYLineChart("Enrichment profile", "Enrichment profile",
                    "Running enrichment score (RES)", data,
                    PlotOrientation.VERTICAL, true, false, false);
        } else {
            XYDataset data = new EsProfileDataset2("Enrichment profile", esProfile_full_opt, horizontal);
            chart = ChartFactory.createXYLineChart("Enrichment profile", "Enrichment profile",
                    "Running enrichment score (RES)", data,
                    PlotOrientation.VERTICAL, true, false, false);
        }

        chart.getXYPlot().getRangeAxis().setTickLabelsVisible(true);
        chart.getXYPlot().getRangeAxis().setTickMarksVisible(true);
        chart.getXYPlot().getDomainAxis().setTickLabelsVisible(true);
        chart.getXYPlot().getDomainAxis().setTickMarksVisible(true);

        if (horizontal) {
            chart.getXYPlot().getRangeAxis().setLabel("Enrichment score (ES)");
            chart.getXYPlot().getDomainAxis().setLabel("Position in ranked list");
            chart.getXYPlot().getDomainAxis().setVisible(true);
        } else {
            chart.getXYPlot().getDomainAxis().setLabel("Enrichment score (ES)");
            chart.getXYPlot().getRangeAxis().setLabel("Position in ranked list");
            chart.getXYPlot().getRangeAxis().setVisible(true);
        }

        chart.getXYPlot().getRenderer().setStroke(new BasicStroke(2.0f));
        chart.getXYPlot().getRenderer().setPaint(Color.GREEN);

        Marker midLine = new ValueMarker(0); // at y = 0
        midLine.setPaint(Color.DARK_GRAY);
        if (horizontal) {
            chart.getXYPlot().addRangeMarker(midLine);
        } else {
            chart.getXYPlot().addDomainMarker(midLine);
        }

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

                geneSets_sizes_file = report.savePageXls(new StringDataframe("gene_set_sizes", sm, rowNames, colNames, true));
            } catch (Throwable t) {
                klog.error(t); // dont penalize - not a critical error
                geneSets_sizes_file = report.createFile("gene_set_sizes_errored_out.txt", "List of gene sets that errored out");
                FileUtils.writeSafely(t.getStackTrace().toString(), geneSets_sizes_file);
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

        private EnrichmentCharts fPlot;
        private HtmlPage fHtmlPage;
        private ExcelTxtPage fExcelPage;

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

        private boolean fHorizontal;

        protected EsProfileDataset() {
        }

        /**
         * Class constructor
         *
         * @param seriesName
         * @param x
         * @param y
         */

        public EsProfileDataset(final String seriesName,
                                final Vector esProfile,
                                final Vector hitIndices_all, // actually ALL indices 0 to n-1
                                final boolean horizontal
        ) {

            this.fGroup = new DatasetGroup();
            this.fSeriesNames = new String[]{seriesName};
            this.fHorizontal = horizontal;

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
            if (fHorizontal) {
                return fYValues.get(item);
            } else {
                return fJustHitIndices.get(item);
            }
        }

        public double getXValue(int series, int item) {
            if (fHorizontal) {
                return fJustHitIndices.get(item);
            } else {
                return fYValues.get(item);
            }
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

        public int indexOf(java.lang.Comparable comparable) {
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
            throw new NotImplementedException();
        }

        ASComparable foo;

        public java.lang.Comparable getSeriesKey(int series) {
            if (foo == null) {
                foo = new ASComparable(fSeriesNames[0]);
            }

            return foo;
        }

    } // End class EsProfileDataset


    static class EsProfileDataset2 implements XYDataset {

        private DatasetGroup fGroup;

        private String[] fSeriesNames;

        private boolean fHorizontal;

        private Vector fEsProfile;

        /**
         * Class constructor
         *
         * @param seriesName
         * @param x
         * @param y
         */
        // unlike the above, we have a profile for every point
        public EsProfileDataset2(final String seriesName,
                                 final Vector esProfile,
                                 final boolean horizontal
        ) {

            //klog.debug(">>>>> DOING FULL PLOT: esProfile: " + esProfile.getSize());

            this.fGroup = new DatasetGroup();
            this.fSeriesNames = new String[]{seriesName};
            this.fHorizontal = horizontal;
            this.fEsProfile = esProfile;
        }

        public double getYValue(int series, int item) {
            if (fHorizontal) {
                return fEsProfile.getElement(item);
            } else {
                return item;
            }
        }

        public double getXValue(int series, int item) {

            if (fHorizontal) {
                return item;
            } else {
                return fEsProfile.getElement(item);
            }
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

        public int indexOf(final java.lang.Comparable comparable) {
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
            throw new NotImplementedException();
        }

        private ASComparable foo;

        public java.lang.Comparable getSeriesKey(int series) {
            if (foo == null) {
                foo = new ASComparable(fSeriesNames[0]);
            }

            return foo;
        }

    } // End class EsProfileDataset2

    // the standard perm analysis plot (see original allaml science paper)
    // but dont do the log thing for y-axis
    public static XChart createButterflyChart(final PermutationTest ptest) {
    
        List xValues = new ArrayList();
    
        Dataset pos_sig_levels = ptest.getSigLevels(true);
        Dataset neg_sig_levels = ptest.getSigLevels(false);
        final Vector yValues = new Vector(ptest.getNumMarkers());
        for (int i = 0; i < ptest.getNumMarkers(); i++) {
            yValues.setElement(i, i + 1);
        }
        final float[] sigLevels = ptest.getSigLevels();
        final List xLabels = new ArrayList();
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
                        (Vector[]) xValues.toArray(new Vector[xValues.size()])
                        , (String[]) xLabels.toArray(new String[xLabels.size()]), "Gene rank",
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
    
        // get a reference to the plot for further customisation...
        XYPlot plot = (XYPlot) chart.getPlot();
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
        List colors = new ArrayList(Arrays.asList(new Color[]{Color.BLACK, Color.PINK, Color.GREEN}));
        int ii = 0;
        for (int i = 2; i < serieses.length; i = i + 2) {
            renderer.setSeriesShapesVisible(i, false);
            renderer.setSeriesShapesVisible(i + 1, false);
            Paint color;
            if (ii < colors.size()) {
                color = (Color) colors.get(ii);
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
            chart.getXYPlot().addRangeMarker(target);
        }
    
        if (classBName_opt != null && classBName_opt.length() > 0) {
            IntervalMarker target = new IntervalMarker(pos, pos);
            target.setLabel("'" + classBName_opt + "' (Neg corr)");
            target.setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
            target.setLabelAnchor(RectangleAnchor.LEFT);
            target.setLabelTextAnchor(TextAnchor.CENTER_LEFT);
            target.setLabelPaint(Color.BLUE);
            chart.getXYPlot().addRangeMarker(target);
        }
    
        return chart;
    }

}
