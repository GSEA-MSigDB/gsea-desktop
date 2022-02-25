/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.gsea;

import edu.mit.broad.genome.StandardException;
import edu.mit.broad.genome.alg.DatasetGenerators;
import edu.mit.broad.genome.alg.Metric;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.objects.strucs.CollapsedDetails;
import edu.mit.broad.genome.parsers.GctParser;
import edu.mit.broad.genome.reports.api.ReportIndexState;
import edu.mit.broad.vdb.chip.Chip;
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
        CollapsedDetails.Data cd = new CollapsedDetails.Data();
        cd.orig = origDs;
    
        if (fFeatureSpaceParam.isSymbols()) {
            if (!fChipParam.isSpecified()) {
                // dont as the chip param isnt really reqd (and hence isnt caught in the usual way)
                //throw new MissingReqdParamException(_getMissingChipMessage());
                throw new BadParamException("Chip parameter must be specified as you asked to analyze" +
                        " in the space of gene symbols. Chip is used to collapse probe ids into symbols", 1002);
            }
    
            final Chip chip = fChipParam.getChip();
            // Remap_only is actually implemented as a Collapse Mode beneath everything else.
            // Also note: we do not allow result file renaming when collapsing via the GSEA tool.
            int collapseModeIndex = fFeatureSpaceParam.isRemap() ? 5 : fCollapseModeParam.getStringIndexChoosen();
            DatasetGenerators.CollapsedDataset cds = new DatasetGenerators().collapse(origDs, chip,
                    fIncludeOnlySymbols.isTrue(), collapseModeIndex, null);

    		Dataset collapsed = cds.symbolized;
            log.info("Collapsing dataset was done. Original: {} collapsed: {}", origDs.getQuickInfo(), collapsed.getQuickInfo());

            // Make a summary etiology always
            fReport.savePageTsv(cds.makeEtiologySdf());

            // Also save the collapsed dataset if Create GCTs is true
            if (fCreateGctsParam.isSpecified() && fCreateGctsParam.isTrue()) {
                File reportDir = fReport.getReportDir();
                File edbDir = new File(reportDir, "edb");
                if (!edbDir.exists()) { edbDir.mkdirs(); }
                File collapsedGct = new File(edbDir, collapsed.getName() + ".gct");
                GctParser gctExporter = new GctParser();
                gctExporter.export(collapsed, collapsedGct);
            }
            
            cd.chip = chip;
            cd.wasCollapsed = true;
            cd.collapsed = collapsed;
            if (cd.getNumRow_orig() != 0 && cd.getNumRow_collapsed() == 0) {
                throw new BadParamException("The collapsed dataset was empty when used with chip:" + cd.getChipName(), 1005);
            }
    
        } else {
            cd.wasCollapsed = false;
            cd.collapsed = origDs;
            log.info("No dataset collapsing was done .. using original as is");
        }
    
        return cd;
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
        final CollapsedDetails.Data cd = getDataset(fullDs);

        // Note that we MUST set the altDelim on the fGeneSetMatrixParam if it's present.  This MUST happen
        // before extracting the param value or it will be parsed incorrectly.  Unfortunately, these params
        // don't give any other good way to specify param dependencies except via code.
        if (fAltDelimParam.isSpecified() && StringUtils.isNotBlank(fAltDelimParam.getValue().toString())) {
            fGeneSetMatrixParam.setAlternateDelimiter(fAltDelimParam.getValue().toString());
        }

        final GeneSet[] origGeneSets = fGeneSetMatrixParam.getGeneSetMatrixCombo(true).getGeneSets();

        execute_one_with_reporting(cd, template, origGeneSets, fShowDetailsForTopXSetsParam.getIValue(), 
                (fMakeZippedReportParam.isSpecified() && fMakeZippedReportParam.isTrue()), (fMakeGeneSetReportsParam.isSpecified() && fMakeGeneSetReportsParam.isTrue()), 
                (fCreateSvgsParam.isSpecified() && fCreateSvgsParam.isTrue()), (fCreateGctsParam.isSpecified() && fCreateGctsParam.isTrue()));

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

    public String getDesc() { return "Set parameters and run enrichment tests"; }

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
}
