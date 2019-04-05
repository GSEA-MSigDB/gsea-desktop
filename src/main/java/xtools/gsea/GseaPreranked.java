/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.gsea;

import edu.mit.broad.genome.alg.gsea.GeneSetCohortGenerator;
import edu.mit.broad.genome.alg.gsea.KSTests;
import edu.mit.broad.genome.math.RandomSeedGenerator;
import edu.mit.broad.genome.math.RandomSeedGenerators;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import edu.mit.broad.genome.objects.strucs.CollapsedDetails;
import edu.mit.broad.genome.parsers.EdbFolderParser;
import edu.mit.broad.genome.reports.EnrichmentReports;
import edu.mit.broad.genome.reports.api.ReportIndexState;
import edu.mit.broad.genome.reports.pages.HtmlReportIndexPage;
import edu.mit.broad.vdb.chip.Chip;
import xtools.api.AbstractTool;
import xtools.api.param.*;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

/**
 * This is the "xtool" that implements the gsea program. For a far simpler tour of how to use the gsea
 * core libaries, see xexamples.GseaExample.
 * <p/>
 * This program has several checks to provide nice error messages.
 * Its NOT representative of xtool code!!
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class GseaPreranked extends AbstractGseaTool {

    private final RankedListReqdParam fRankedListParam = new RankedListReqdParam();

    private final IntegerParam fShowDetailsForTopXSetsParam = new IntegerParam("plot_top_x", "Plot graphs for the top sets of each phenotype", "Plot GSEA mountain and related plots for the top sets of each phenotype", 20, false, Param.ADVANCED);
    private final BooleanParam fMakeZippedReportParam = AbstractTool.createZipReportParam(false);
    private final BooleanParam fMakeGeneSetReportsParam = new BooleanParam("make_sets", "Make detailed gene set report", "Create detailed gene set reports (heat-map, mountain plot etc) for every enriched gene set", true, false, Param.ADVANCED);

    // Push up to AbstractGseaTool
    private final BooleanParam fCreateSvgsParam = new BooleanParam("create_svgs", "Create SVG plot images", "Create SVG plot images along with PNGs (GZ compressed to save space as these are very large)", false, false, Param.ADVANCED);
    private final StringInputParam fAltDelimParam = new StringInputParam("altDelim", "Alternate delimiter", 
            "Optional alternate delimiter character for gene set names instead of comma", null, false, new char[] { ';' }, Param.ADVANCED);

    private GeneSet[] fOrigGeneSets;

    /**
     * Class constructor
     *
     * @param properties
     */
    public GseaPreranked(final Properties properties) {
        super.init(properties);
    }

    public GseaPreranked(final String[] args) {
        super.init(args);
    }

    /**
     * For ParamSet interrogation use only -- not executable
     *
     * @param name
     */
    public GseaPreranked() {
        declareParams();
    }

    public void execute() throws Exception {

        // to preserve memory & for custom indexing
        final ReportIndexState state = new ReportIndexState(true, false, false, createHeader(fRankedListParam));
        startExec(state);

        final RankedList fullRl = uniquize(fRankedListParam.getRankedList());
        if (fullRl.getSize() == 0) {
            throw new IllegalArgumentException("The chip and the ranked list did not match");
        }

        final CollapsedDetails.Ranked cd = getRankedList(fullRl);

        // Note that we MUST set the altDelim on the fGeneSetMatrixParam if it's present.  This MUST happen
        // before extracting the param value or it will be parsed incorrectly.  Unfortunately, these params
        // don't give any other good way to specify param dependencies except via code.
        if (fAltDelimParam.isSpecified() && StringUtils.isNotBlank(fAltDelimParam.getValue().toString())) {
            fGeneSetMatrixParam.setAlternateDelimiter(fAltDelimParam.getValue().toString());
        }

        this.fOrigGeneSets = fGeneSetMatrixParam.getGeneSetMatrixCombo(true).getGeneSets();

        final GeneSet[] gsets = Helper.getGeneSets(cd.getRankedList(), fOrigGeneSets, fGeneSetMinSizeParam, fGeneSetMaxSizeParam);

        checkAndBarfIfZeroSets(gsets);

        final HtmlReportIndexPage htmlReportIndexPage = fReport.getIndexPage();

        execute_one(cd, gsets, htmlReportIndexPage);

        if (fMakeZippedReportParam.isTrue()) {
            // custom close before zipping
            fReport.closeReport(true);
            fReport.zipReport();
        }

        doneExec();
    }


    /**
     * @throws java.lang.Exception
     */
    private void execute_one(final edu.mit.broad.genome.objects.strucs.CollapsedDetails fullRL,
                             final GeneSet[] gsets,
                             final HtmlReportIndexPage reportIndexPage) throws Exception {

        final int nperms = fNumPermParam.getIValue();
        final int topXSets = fShowDetailsForTopXSetsParam.getIValue();
        final RandomSeedGenerator rst = fRndSeedTypeParam.createSeed();
        final GeneSetCohortGenerator gcohgen = fGcohGenReqdParam.createGeneSetCohortGenerator(false);
        final int minSize = fGeneSetMinSizeParam.getIValue();
        final int maxSize = fGeneSetMaxSizeParam.getIValue();
        final boolean createSvgs = fCreateSvgsParam.isSpecified() && fCreateSvgsParam.isTrue();
        Chip chip = null;
        RankedList rl = ((CollapsedDetails.Ranked) fullRL).getRankedList();
        FeatureAnnot fann = new FeatureAnnot(rl.getName(), rl.getRankedNames(), null);

        final KSTests tests = new KSTests(getOutputStream());
        
        // If we have a RandomSeedGenerator.Timestamp instance, save the timestamp for later reference
        if (rst instanceof RandomSeedGenerators.Timestamp) {
            fReport.addComment("Timestamp used as the random seed: " + 
                    ((RandomSeedGenerators.Timestamp)rst).getTimestamp());
        }

        EnrichmentDb edb = tests.executeGsea(
                rl,
                gsets,
                nperms,
                rst,
                chip,
                gcohgen
        );

        // Make the report
        EnrichmentReports.Ret ret = EnrichmentReports.createGseaLikeReport(edb, getOutputStream(),
                fullRL, reportIndexPage, false,
                fReport, topXSets, minSize, maxSize,
                fMakeGeneSetReportsParam.isTrue(), fMakeZippedReportParam.isTrue(),
                createSvgs, fOrigGeneSets, "PreRanked", fNormModeParam.getNormModeName(), fann);

        // Make an edb folder thing
        new EdbFolderParser().export(ret.edb, ret.savedInDir);

    }

    protected Param[] getAdditionalParams() {
        return new Param[]{fRankedListParam, fShowDetailsForTopXSetsParam, fMakeZippedReportParam, fMakeGeneSetReportsParam, fCreateSvgsParam, fAltDelimParam};
    }

    public String getDesc() {
        return "Run GSEA on a pre-ranked (with external tools) gene list";
    }

    protected CollapsedDetails.Ranked getRankedList(final RankedList origRL) throws Exception {
        CollapsedDetails.Ranked cd = new CollapsedDetails.Ranked();
        cd.orig = origRL;
        cd.wasCollapsed = false;
        cd.collapsed = origRL;
        return cd;
    }

    public static void main(String[] args) {
        GseaPreranked tool = new GseaPreranked(args);
        tool_main(tool);
    }

    static String createHeader(final RankedListReqdParam dsr) {
        try {
            if (dsr.isSpecified()) {
                RankedList rl = dsr.getRankedList();
                StringBuffer buf = new StringBuffer();
                buf.append("<div id=\"footer\" style=\"width: 905; height: 35\">\n").append(
                        "<h3 style=\"text-align: left\"><font color=\"#808080\">GSEA Report for ").append(
                        "Dataset ").append(rl.getName()).append("</font></h3>\n").append("</div>");

                return buf.toString();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return null;
    }

}    // End GseaPreranked