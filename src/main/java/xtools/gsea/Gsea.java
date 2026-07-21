/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.gsea;

import edu.mit.broad.genome.StandardException;
import edu.mit.broad.genome.alg.Metric;
import edu.mit.broad.genome.alg.Metrics;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.objects.strucs.CollapsedDetails;
import edu.mit.broad.genome.reports.api.ReportIndexState;
import edu.mit.broad.genome.reports.pages.HtmlFormat;
import xtools.api.AbstractTool;
import xtools.api.param.*;

import java.io.File;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

/**
 * This is the "xtool" that implements the gsea program.
 * <p/>
 * This program has several checks to provide nice error messages.
 * Its NOT representative of xtool code!!
 *
 * @author Aravind Subramanian, David Eby
 */
public class Gsea extends AbstractGsea2Tool {
    private final IntegerParam fShowDetailsForTopXSetsParam = new IntegerParam("plot_top_x", "Plot graphs for the top sets of each phenotype", "Plot GSEA mountain and related plots for the top sets of each phenotype", 20, false, Param.ADVANCED);
    private final BooleanParam fMakeZippedReportParam = AbstractTool.createZipReportParam(false);
    private final BooleanParam fMakeGeneSetReportsParam = new BooleanParam("make_sets", "Make detailed gene set report", "Create detailed gene set reports (heat-map, mountain plot etc) for every enriched gene set", true, false, Param.ADVANCED);

    // Push up to AbstractGseaTool
    private final BooleanParam fCreateSvgsParam = new BooleanParam("create_svgs", "Create SVG plot images", "Create SVG plot images along with PNGs (GZ compressed to save space as these are very large)", false, false, Param.ADVANCED);
    private final BooleanParam fCreateModernEnplotsParam = new BooleanParam("create_enplot_v2", "Create EnPlot v2 outputs",
            "Also create EnPlot v2 static images (enplot2_*) and interactive plot data (JSON) for use in the report explorer and HTML detail pages", false, false, Param.ADVANCED);
    private final StringInputParam fAltDelimParam = new StringInputParam("altDelim", "Alternate delimiter", 
            "Optional alternate delimiter character for gene set names instead of comma", null, false, new char[] { ';' }, Param.ADVANCED);
    private final BooleanParam fCreateGctsParam = new BooleanParam("create_gcts", "Create GCT files", "Create GCT files for the data backing the Gene Set Enrichment Heatmaps", false, false, Param.ADVANCED);

    public Gsea(final Properties properties) {
        super("Collapse");
        super.init(properties, "");
    }

    public Gsea(final Properties properties, String paramFilePath) {
        super("Collapse");
        super.init(properties, paramFilePath);
    }

    public Gsea(final String[] args) {
        super("Collapse");
        super.init(args);
    }

    /**
     * For ParamSet interrogation use only -- not executable
     *
     * @param name
     */
    public Gsea() {
        super("Collapse");
        declareParams();
    }
    
    public String getName() { return "GSEA"; }

    protected CollapsedDetails.Data getDataset(final Dataset origDs) throws Exception {
        final File edbForGct = (fCreateGctsParam.isSpecified() && fCreateGctsParam.isTrue())
                ? new File(fReport.getReportDir(), "edb")
                : null;
        return GseaExpressionDataPrep.collapseForExpressionUse(origDs, fFeatureSpaceParam, fChipParam,
                fCollapseModeParam, fIncludeOnlySymbols, fReport, edbForGct).data;
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

        // Note that we MUST set the altDelim on the fGeneSetMatrixParam if it's present.  This MUST happen
        // before extracting the param value or it will be parsed incorrectly.  Unfortunately, these params
        // don't give any other good way to specify param dependencies except via code.
        if (fAltDelimParam.isSpecified() && StringUtils.isNotBlank(fAltDelimParam.getValue().toString())) {
            fGeneSetMatrixParam.setAlternateDelimiter(fAltDelimParam.getValue().toString());
        }

        final GeneSet[] origGeneSets = fGeneSetMatrixParam.getGeneSetMatrixCombo().getGeneSets();
        
        ToolHelper.validateMixedVersionAndSpecies(origGeneSets, fChipParam.getChip(), fReport, log);
        
        Dataset ds = fDatasetParam.getDataset(fChipParam);

        final boolean waldZSelected = Metrics.isWaldZFamily(metric);
        final CollapsedDetails.Data cd;
        if (waldZSelected) {
            // For Wald Z-style metrics, preserve all rows through collapse first.
            // De-duplicating before collapse can arbitrarily drop rows and distort count-model ranking.
            cd = getDataset(ds);
            final Dataset deDuped = uniquize(cd.getDataset());
            if (cd.wasCollapsed) {
                cd.collapsed = deDuped;
            } else {
                cd.orig = deDuped;
            }
        } else {
            final Dataset fullDs = uniquize(ds);
            cd = getDataset(fullDs);
        }

        execute_one_with_reporting(cd, template, origGeneSets, fShowDetailsForTopXSetsParam.getIValue(), 
                (fMakeZippedReportParam.isSpecified() && fMakeZippedReportParam.isTrue()), (fMakeGeneSetReportsParam.isSpecified() && fMakeGeneSetReportsParam.isTrue()), 
                (fCreateSvgsParam.isSpecified() && fCreateSvgsParam.isTrue()), (fCreateGctsParam.isSpecified() && fCreateGctsParam.isTrue()),
                (fCreateModernEnplotsParam.isSpecified() && fCreateModernEnplotsParam.isTrue()));

        if (fMakeZippedReportParam.isTrue()) {
            // custom close before zipping
            fReport.closeReport(true);
            fReport.zipReport();
        }

        doneExec();
    }

    protected Param[] getAdditionalParams() {
        return new Param[]{fShowDetailsForTopXSetsParam, fMakeZippedReportParam, fMakeGeneSetReportsParam, fCreateSvgsParam, fCreateModernEnplotsParam, fCreateGctsParam, fAltDelimParam};
    }

    public String getDesc() { return "Set parameters and run enrichment tests"; }

    public static void main(String[] args) {
        Gsea tool = new Gsea(args);
        tool_main(tool);
    }

    public static String createHeader(final DatasetReqdParam dsr) {
        try {
            if (dsr.isSpecified()) {
                Dataset ds = dsr.getDataset();
                return HtmlFormat.reportHeader("GSEA Report for Dataset " + ds.getName());
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return null;
    }
}
