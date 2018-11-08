/*******************************************************************************
 * Copyright (c) 2003-2018 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.gsea;

import edu.mit.broad.genome.Printf;
import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResult;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.reports.api.ReportIndexState;
import edu.mit.broad.genome.reports.pages.HtmlFormat;
import edu.mit.broad.genome.reports.pages.HtmlReportIndexPage;

import org.apache.ecs.StringElement;
import org.apache.ecs.html.*;
import org.genepattern.gsea.GeneHistogram;
import org.genepattern.gsea.HCLAlgorithm;
import org.genepattern.gsea.JaccardHistogram;
import org.genepattern.gsea.LeadingEdgeAnalysis;
import org.genepattern.heatmap.HeatMapComponent;
import org.genepattern.heatmap.image.HeatMap;
import org.genepattern.io.ImageUtil;

import xapps.api.vtools.viewers.VizFactory;
import xtools.api.AbstractTool;
import xtools.api.ToolCategory;
import xtools.api.param.BooleanParam;
import xtools.api.param.DirParam;
import xtools.api.param.Param;
import xtools.api.param.ParamFactory;
import xtools.api.param.StringInputParam;
import xtools.api.param.StringMultiInputParam;

import java.awt.Dimension;
import java.io.File;
import java.util.Properties;

/**
 * Commandline Tool to run the Tests
 * <p/>
 * Runs it on a ffn of dataset+template+geneSetmatrix
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class LeadingEdgeTool extends AbstractTool {
    private DirParam fGseaResultDirParam = new DirParam(true);

    private StringInputParam fAltDelimParam = new StringInputParam("altDelim", "alternate delimiter", 
            "Optional alternate delimiter character for gene set names instead of comma", null, false, new char[] { ';' }, Param.ADVANCED);
    
    private StringMultiInputParam fGeneSetNamesParam = new StringMultiInputParam("gsets", "gene sets", false);

    private StringInputParam fImageFormat = new StringInputParam("imgFormat", "image format", 
            "File format to use for generated images: png (default), svg (will be GZ compressed), or jpg", "png", false);

    private BooleanParam fCreateExtraPlotsParam = new BooleanParam("extraPlots", 
            "create extra LEV plots", false, false);

    private final BooleanParam fMakeZippedReportParam = ParamFactory.createZipReportParam(false);

    /**
     * Class constructor
     *
     * @param properties
     */
    public LeadingEdgeTool(final Properties properties) {
        super.init(properties);
    }

    public LeadingEdgeTool(final String[] args) {
        super.init(args);
    }

    /**
     * For ParamSet interrogation use only -- not executable
     *
     * @param name
     */
    public LeadingEdgeTool() {
        declareParams();
    }

    private Dataset _morph(Dataset ds, RankedList rl) {
        Matrix m = new Matrix(ds.getNumRow(), ds.getNumCol());
        for (int r = 0; r < ds.getNumRow(); r++) {
            for (int c = 0; c < ds.getNumCol(); c++) {
                float score = rl.getScore(ds.getColumnName(c));
                float value = ds.getElement(r, c);
                if (value == 1) {
                    m.setElement(r, c, score);
                } else {
                    m.setElement(r, c, value); // ought to be zero
                }
            }
        }

        return new DefaultDataset(ds.getName(), m, ds.getRowNames(), ds.getColumnNames(), true, ds.getAnnot());
    }

    /**
     * @throws java.lang.Exception
     */
    public void execute() throws Exception {

        // to preserve memory & for custom indexing
        final ReportIndexState state = new ReportIndexState(true, false, false, getHeader(fGseaResultDirParam));
        startExec(state);

        final File gseaResultDir = fGseaResultDirParam.getFile();
        final EnrichmentDb edb = ParserFactory.readEdb(gseaResultDir, true);
        // Note that we MUST set the altDelim on the fGeneSetNamesParam if it's present.  This MUST happen
        // before extracting the param value or it will be parsed incorrectly.  Unfortunately, these params
        // don't give any other good way to specify param dependencies except via code.
        if (fAltDelimParam.isSpecified()) {
            fGeneSetNamesParam.setAlternateDelimiter(fAltDelimParam.getValue().toString());
        }
        String[] gsetNames = null;
        if (fGeneSetNamesParam.isSpecified()) {
            gsetNames = fGeneSetNamesParam.getStrings();
        }
        boolean createExtraPlots = fCreateExtraPlotsParam.isSpecified() && fCreateExtraPlotsParam.isTrue();
        String imgFormat = (fImageFormat.isSpecified()) ? fImageFormat.getValue().toString() : "png";
        EnrichmentResult[] enrichmentResults = null;
        GeneSet[] gsets = null;

        // If no Gene set names were found then we'll use the whole list.  Get them out of the 
        // EnrichmentDb along with the EnrichmentResults and Gene Sets which are needed anyway.
        if (gsetNames == null || gsetNames.length == 0) {
            enrichmentResults = LeadingEdgeAnalysis.getAllResultsFromEdb(edb);
            
            // Bail out if there are no results at all.
            if (enrichmentResults == null || enrichmentResults.length == 0) return;
            
            gsetNames = new String[enrichmentResults.length];
            gsets = new GeneSet[enrichmentResults.length];
            for (int i = 0; i < enrichmentResults.length; i++) {
                gsets[i] = enrichmentResults[i].getGeneSet();
                gsetNames[i] = gsets[i].getName(true);
            }
        } else {
            enrichmentResults = new EnrichmentResult[gsetNames.length];
            gsets = new GeneSet[gsetNames.length];
            for (int r = 0; r < gsetNames.length; r++) {
                enrichmentResults[r] = edb.getResultForGeneSet(gsetNames[r]);
                gsets[r] = enrichmentResults[r].getSignal().getAsGeneSet();
            }
        }
                
        final GeneSetMatrix lev_gmx = new DefaultGeneSetMatrix("leading_edge_matrix_for_" + edb.getName(), gsets);
        final BitSetDataset lev_bsd = new BitSetDataset(lev_gmx);
        final Dataset lev_ds = lev_bsd.toDataset(true, false);
        final File lev_ds_file = fReport.savePage(lev_ds, false);

        Dataset lev_ds_clustered = null;
        try {
            log.info("Clustering signal matrix: " + lev_ds.getQuickInfo() + " " + lev_ds_file.getPath());
            lev_ds_clustered = HCLAlgorithm.cluster(lev_ds);

        } catch (Throwable t) {
            fReport.addError("Trouble clustering", t);
        }
        
        if (createExtraPlots) {
            // TODO: Some duplicate work happening here, no doubt.  Reduce that later.
            // - Attempts to pull out and use the clusteredDataset from the analysis object fail.
            LeadingEdgeAnalysis analysis = LeadingEdgeAnalysis.runAnalysis(edb, gsetNames, null);
    
            JaccardHistogram jaccardHistogram = analysis.getJaccardHistogram();
            // Hard-code the preferred size to match our legacy plots; these changed with JFreeChart 1.5.0
            jaccardHistogram.getChartPanel().setPreferredSize(new Dimension(680, 420));
            File jaccardHistFile = fReport.createFile("jaccard_histogram." + imgFormat, "Jaccard Histogram");
            jaccardHistFile = ImageUtil.saveReportPlotImage(jaccardHistogram.getChartPanel(), jaccardHistFile, imgFormat);
            
            GeneHistogram geneHistogram = analysis.getGeneHistogram();
            geneHistogram.getChartPanel().setPreferredSize(new Dimension(680, 420));
            File geneHistFile = fReport.createFile("gene_histogram." + imgFormat, "Gene Histogram");
            geneHistFile = ImageUtil.saveReportPlotImage(geneHistogram.getChartPanel(), geneHistFile, imgFormat);
            
            HeatMapComponent geneSetSimilarityHeatmap = analysis.getGeneSetSimilarityHeatmap();
            File geneSetSimFile = fReport.createFile("gene_set_similarity_heatmap." + imgFormat, 
                    "Gene Set Similarity Heatmap");
            geneSetSimFile = ImageUtil.saveReportPlotImage(geneSetSimilarityHeatmap.getHeatMap(), geneSetSimFile, imgFormat);
        
            HeatMapComponent leadingEdgeHeatmap = analysis.getLeadingEdgePanel().getHeatMapComponent();
            File leHeatmapFile = fReport.createFile("leading_edge_heatmap." + imgFormat, "Leading Edge Heatmap");
            leHeatmapFile = ImageUtil.saveReportPlotImage(leadingEdgeHeatmap.getHeatMap(), leHeatmapFile, imgFormat);
        }

        // -------------------------------------------------------------------------------------------- //
        // Make the Reports
        final HtmlReportIndexPage reportIndexPage = fReport.getIndexPage();

        Div div = new Div();
        H2 h2 = new H2("Leading edge results for enrichment database: <b>" + edb.getName() + "</b>");
        div.addElement(h2);

        // -------------------------------------------------------------------------------------------- //
        div = new Div();
        div.addElement(new H4("Leading edge analysis: Clustered results"));
        UL ul = new UL();
        ul.addElement(new LI("There were " + enrichmentResults.length + " gene sets used in the leading edge analysis (see below for details)"));

        Dataset lev_ds_clustered_m = _morph(lev_ds_clustered, edb.getRankedList());

        HeatMap heatMap = VizFactory.createGramImager(GPWrappers.createColorScheme_for_lev_with_score(lev_ds_clustered_m)).createBpogHeatMap(lev_ds_clustered_m);
        File lev_clust_hm_file = fReport.createFile("leading_edge_heat_map_clustered." + imgFormat, "foo");
        lev_clust_hm_file = ImageUtil.saveReportPlotImage(heatMap, lev_clust_hm_file, imgFormat);
        StringElement line3 = HtmlFormat.Links.hyper("Heat map of clustered leading edge subsets", lev_clust_hm_file, ". Rows are gene sets and columns are genes. This matrix is clustered", fReport.getReportDir());
        ul.addElement(new LI(line3));
        div.addElement(ul);

        reportIndexPage.addBlock(div, true);

        // -------------------------------------------------------------------------------------------- //

        // Then add details on the gene sets used in the lea
        Table table = new Table();
        TH th = new TH();
        th.addElement(new TD("# members"));
        th.addElement(new TD("# members<br>in signal"));
        th.addElement(new TD("Tag %"));
        th.addElement(new TD("List %"));
        th.addElement(new TD("Signal strength"));
        table.addElement(th);
        for (int r = 0; r < enrichmentResults.length; r++) {
            TR tr = new TR();

            String gsetName = enrichmentResults[r].getGeneSetName();
            File gsetGseaFile = new File(gseaResultDir, gsetName + ".html");
            A link = new A(gsetGseaFile.toURI().toString(), gsetName);

            tr.addElement(new TD(link));
            tr.addElement(new TD("" + enrichmentResults[r].getGeneSet().getNumMembers()));
            GeneSetSignal signal = enrichmentResults[r].getSignal();
            tr.addElement(new TD("" + signal.getSize()));
            tr.addElement(new TD(Printf.format(signal.getTagFraction() * 100, 0) + "%"));
            tr.addElement(new TD(Printf.format(signal.getListFraction() * 100, 0) + "%"));
            tr.addElement(new TD(Printf.format(signal.getSignalStrength() * 100, 0) + "%"));
            table.addElement(tr);
        }

        div = new Div();
        div.addElement(new H4("Details of gene sets and signals used in this analysis"));
        div.addElement(table);
        reportIndexPage.addBlock(div, true);

        // -------------------------------------------------------------------------------------------- //

        div = new Div();
        div.addElement(new H4("Leading edge analysis: other files made"));
        ul = new UL();
        final File lev_ds_clustered_file = fReport.savePage(lev_ds_clustered, false);
        StringElement line2 = HtmlFormat.Links.hyper("Clustered dataset (gct)", lev_ds_clustered_file, "for the specified gene sets. 1's denote membership of a gene in the leading edge subset", fReport.getReportDir());
        ul.addElement(new LI(line2));

        File lev_gmx_file = fReport.savePageGmx(lev_gmx);
        StringElement line1 = HtmlFormat.Links.hyper("GeneSetMatrix (gmx)", lev_gmx_file, "of leading edge subsets", fReport.getReportDir());
        ul.addElement(new LI(line1));

        line2 = HtmlFormat.Links.hyper("Dataset (gct)", lev_ds_file, "for the specified gene sets. 1's denote membership of a gene in the leading edge subset", fReport.getReportDir());
        ul.addElement(new LI(line2));

        HeatMap lev_image = VizFactory.createGramImager(GPWrappers.createColorScheme_for_lev_with_score(lev_ds)).createBpogHeatMap(_morph(lev_ds, edb.getRankedList()));
        File lev_hm_file = fReport.createFile("leading_edge_heat_map_unclustered." + imgFormat, "foo");
        lev_hm_file = ImageUtil.saveReportPlotImage(lev_image, lev_hm_file, imgFormat);
        line3 = HtmlFormat.Links.hyper("Heat map of leading edge subsets", lev_hm_file, ". Rows are gene sets and columns are genes. This matrix is NOT clustered", fReport.getReportDir());
        ul.addElement(new LI(line3));

        div.addElement(ul);

        reportIndexPage.addBlock(div, true);

        // -------------------------------------------------------------------------------------------- //
        // Other
        div = new Div();
        ul = new UL();
        div.addElement(new H4("Other"));
        ul.addElement(new LI(HtmlFormat.Links.hyper("Parameters", fReport.getParamsFile(), "used for this analysis", fReport.getReportDir())));
        div.addElement(ul);
        reportIndexPage.addBlock(div, false);

        reportIndexPage.setAddBrowseFooter(false); // turn off the little browse footer

        if (fMakeZippedReportParam.isTrue()) {
            // custom close before zipping
            fReport.closeReport(true);
            fReport.zipReport();
        }

        doneExec();
    }

    public ToolCategory getCategory() {
        return ToolCategory.GSEA;
    }

    public void declareParams() {

        fParamSet.addParam(fGseaResultDirParam);
        fParamSet.addParam(fAltDelimParam);
        fParamSet.addParam(fGeneSetNamesParam);
        fParamSet.addParam(fImageFormat);
        fParamSet.addParam(fCreateExtraPlotsParam);
        fParamSet.addParam(fMakeZippedReportParam);
    }

    public static void main(String[] args) {

        LeadingEdgeTool tool = new LeadingEdgeTool(args);
        tool_main(tool);
    }

    private static String getHeader(final DirParam dirp) {
        try {
            if (dirp.isSpecified()) {
                String dpn = dirp.getDir().getName();
                StringBuffer buf = new StringBuffer();
                buf.append("<div id=\"footer\" style=\"width: 905; height: 35\">\n").append(
                        "<h3 style=\"text-align: left\"><font color=\"#808080\">Leading edge report for ").append(
                        "GSEA result folder: ").append(dpn).append("</font></h3>\n").append("</div>");

                return buf.toString();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return null;
    }
}    // End LeadingEdgeTool
