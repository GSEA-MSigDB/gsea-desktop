/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.gsea;

import edu.mit.broad.genome.StandardException;
import edu.mit.broad.genome.alg.Metric;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.objects.strucs.CollapsedDetails;
import edu.mit.broad.genome.reports.api.ReportIndexState;
import edu.mit.broad.genome.reports.pages.HtmlReportIndexPage;
import xtools.api.param.*;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

/**
 * This is the "xtool" that implements the gsea program.
 * <p/>
 * This program has several checks to provide nice error messages.
 * Its NOT representative of xtool code!!
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class Gsea extends AbstractGsea2Tool {

    private final IntegerParam fShowDetailsForTopXSetsParam = new IntegerParam("plot_top_x", "Plot graphs for the top sets of each phenotype", "Plot GSEA mountain and related plots for the top sets of each phenotype", 20, false, Param.ADVANCED);
    private final BooleanParam fMakeZippedReportParam = ParamFactory.createZipReportParam(false);
    private final BooleanParam fMakeGeneSetReportsParam = new BooleanParam("make_sets", "Make detailed gene set report", "Create detailed gene set reports (heat-map, mountain plot etc) for every enriched gene set", true, false, Param.ADVANCED);

    // Push up to AbstractGseaTool
    private final BooleanParam fCreateSvgsParam = new BooleanParam("create_svgs", "Create SVG plot images", "Create SVG plot images along with PNGs (GZ compressed to save space as these are very large)", false, false, Param.ADVANCED);
    private final StringInputParam fAltDelimParam = new StringInputParam("altDelim", "Alternate delimiter", 
            "Optional alternate delimiter character for gene set names instead of comma", null, false, new char[] { ';' }, Param.ADVANCED);
    private final BooleanParam fCreateGctsParam = new BooleanParam("create_gcts", "Create GCT files", "Create GCT files for the data backing the Gene Set Enrichment Heatmaps", false, false, Param.ADVANCED);

    /**
     * Class constructor
     *
     * @param properties
     */
    public Gsea(final Properties properties) {
        super.init(properties);
    }

    public Gsea(final String[] args) {
        super.init(args);
    }

    /**
     * For ParamSet interrogation use only -- not executable
     *
     * @param name
     */
    public Gsea() {
        declareParams();
    }

    public void execute() throws Exception {

        // to preserve memory & for custom indexing
        final ReportIndexState state = new ReportIndexState(true, false, false, createHeader(fDatasetParam));
        startExec(state);

        final Template template = fTemplateParam.getTemplate();

        // some quick checks
        final Metric metric = fMetricParam.getMetric();
        if (template.isCategorical()) {
            if (!metric.isCategorical()) {
                throw new StandardException("As the phenotype choosen was categorical, only categorical metrics are allowed. Got: " + metric.getName(), 1010);
            }

            if (!template.isCategorical()) {
                throw new IllegalArgumentException("Only categorical templates allowed. Got: " + template.getName());
            }
        }

        if (!template.isCategorical()) {
            if (!metric.isContinuous()) {
                throw new StandardException("As the phenotype choosen was continuous, only continuous class metrics are allowed. Got: " + metric.getName(), 1011);
            }

            if (!template.isContinuous()) {
                throw new IllegalArgumentException("Only continuous templates allowed. Got: " + template.getName());
            }
        }


        Dataset ds = fDatasetParam.getDataset(fChipParam);

        final Dataset fullDs = uniquize(ds);
        final CollapsedDetails.Data cd = super.getDataset(fullDs);

        // Note that we MUST set the altDelim on the fGeneSetMatrixParam if it's present.  This MUST happen
        // before extracting the param value or it will be parsed incorrectly.  Unfortunately, these params
        // don't give any other good way to specify param dependencies except via code.
        if (fAltDelimParam.isSpecified() && StringUtils.isNotBlank(fAltDelimParam.getValue().toString())) {
            fGeneSetMatrixParam.setAlternateDelimiter(fAltDelimParam.getValue().toString());
        }

        //log.debug("# of templates: " + tss.length);
        final GeneSet[] origGeneSets = fGeneSetMatrixParam.getGeneSetMatrixCombo(true).getGeneSets();

        final GeneSet[] gsets = Helper.getGeneSets(cd.getDataset(), origGeneSets, fGeneSetMinSizeParam, fGeneSetMaxSizeParam);

        ParamFactory.checkAndBarfIfZeroSets(gsets);

        final HtmlReportIndexPage htmlReportIndexPage = fReport.getIndexPage();
        final boolean makeSubDir = false;

        execute_one_with_reporting(cd, template, gsets,
                htmlReportIndexPage, makeSubDir, origGeneSets, fShowDetailsForTopXSetsParam.getIValue(), fMakeZippedReportParam.isTrue(), fMakeGeneSetReportsParam.isTrue(), 
                (fCreateSvgsParam.isSpecified() && fCreateSvgsParam.isTrue()),
                (fCreateGctsParam.isSpecified() && fCreateGctsParam.isTrue()));

        if (fMakeZippedReportParam.isTrue()) {
            // custom close before zipping
            fReport.closeReport(true);
            fReport.zipReport();
        }

        doneExec();
    }

    protected Param[] getAdditionalParams() {
        return new Param[]{fShowDetailsForTopXSetsParam, fMakeZippedReportParam, fMakeGeneSetReportsParam, fCreateSvgsParam, fCreateGctsParam, fAltDelimParam};
    }

    public String getDesc() {
        return "Set parameters and run enrichment tests";
    }

    public static void main(String[] args) {
        Gsea tool = new Gsea(args);
        tool_main(tool);
    }

    public static String createHeader(final DatasetReqdParam dsr) {
        try {
            if (dsr.isSpecified()) {
                Dataset ds = dsr.getDataset();
                StringBuffer buf = new StringBuffer();
                buf.append("<div id=\"footer\" style=\"width: 905; height: 35\">\n").append(
                        "<h3 style=\"text-align: left\"><font color=\"#808080\">GSEA Report for ").append(
                        "Dataset ").append(ds.getName()).append("</font></h3>\n").append("</div>");

                return buf.toString();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return null;
    }

}    // End Gsea
