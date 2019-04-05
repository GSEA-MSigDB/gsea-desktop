/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.chip2chip;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.math.StringMatrix;
import edu.mit.broad.genome.objects.GeneSetMatrix;
import edu.mit.broad.genome.objects.StringDataframe;
import edu.mit.broad.genome.parsers.DataFormat;
import edu.mit.broad.genome.reports.RichDataframe;
import edu.mit.broad.genome.reports.api.ReportIndexState;
import edu.mit.broad.genome.reports.api.ToolReport;
import edu.mit.broad.genome.reports.pages.HtmlReportIndexPage;
import edu.mit.broad.genome.reports.web.LinkedFactory;
import edu.mit.broad.vdb.map.Chip2ChipMapper;
import edu.mit.broad.vdb.map.MGeneSetMatrix;
import edu.mit.broad.vdb.map.MappingEtiology;
import gnu.trove.TIntObjectHashMap;
import xtools.api.AbstractTool;
import xtools.api.ReportBlocks;
import xtools.api.ToolCategory;
import xtools.api.param.*;

import java.io.File;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class Chip2Chip extends AbstractTool {

    private final GeneSetMatrixMultiChooserParam fGeneSetMatrixParam = new GeneSetMatrixMultiChooserParam(Param.GMX,
            "Gene sets database (symbols only)", "Gene sets database (symbols only)", true);

    private final BooleanParam fShowEtiology = new BooleanParam("show_etiology", 
            "Output verbose mapping details", "Show the etiology for the features", true, false);

    private final GeneSetMatrixFormatParam fGmFormatParam = new GeneSetMatrixFormatParam(DataFormat.GMT_FORMAT, false);

    private final ChipOptParam fChipTargetParam = new ChipOptParam("chip_target", "Target chip", 
            "The destination chip - into which mappings are converted", true);

    private final StringInputParam fAltDelimParam = new StringInputParam("altDelim", "Alternate delimiter", 
            "Optional alternate delimiter character for gene set names instead of comma", null, false, new char[] { ';' }, Param.ADVANCED);

    private final BooleanParam fMakeZippedReportParam = AbstractTool.createZipReportParam(false);

    public static String createHeader() {
        StringBuffer buf = new StringBuffer();
        buf.append("<div id=\"footer\" style=\"width: 905; height: 35\">\n").append(
                "<h3 style=\"text-align: left\"><font color=\"#808080\">Chip2Chip Report").append("</font></h3>\n").append("</div>");
    
        return buf.toString();
    }

    private static void doMapping(final Chip2ChipMapper mapper,
                                  final GeneSetMatrix gm_to_map,
                                  final boolean showEtiology,
                                  final GeneSetMatrixFormatParam fGmFormatParam,
                                  final ToolReport report) throws Exception {
    
    
        ReportBlocks.SimpleBlockListing list_etiology = null;
        RichDataframe summary_table = null;

        final MGeneSetMatrix mgm = mapper.map(gm_to_map, showEtiology);

        if (showEtiology) {
            list_etiology = new ReportBlocks.SimpleBlockListing("Mapping details", report);

            final MappingEtiology[] mes = mgm.getEtiologies();
            File[] files = new File[mes.length];
            for (int m = 0; m < mes.length; m++) {
                String desc = "Mapping details for " + gm_to_map.getGeneSetName(m) + " by " + mapper.getName();
                files[m] = report.savePageTxt(gm_to_map.getGeneSetName(m) + "_" + mapper.getName() + "_etiology",
                        desc, mes[m].getStory(), true, false);
            }

            // Make a summary etiology always
            final String[] colNames = new String[]{"#SIZE BEFORE MAPPING", "#SIZE AFTER MAPPING", "#SOURCE MEMBERS THAT HAD ONE OR MORE MATCHES", "DETAILS"};
            final String[] rowNames = new String[gm_to_map.getNumGeneSets()];
            final StringMatrix sm = new StringMatrix(rowNames.length, colNames.length);
            final TIntObjectHashMap cell_id_linkMap = new TIntObjectHashMap();
            for (int r = 0; r < gm_to_map.getNumGeneSets(); r++) {
                rowNames[r] = gm_to_map.getGeneSetName(r);
                sm.setElement(r, 0, gm_to_map.getGeneSet(r).getNumMembers());
                sm.setElement(r, 1, mgm.getMappedGeneSet(r).getMappedGeneSet(true).getNumMembers());
                sm.setElement(r, 2, mgm.getMappedGeneSet(r).getEtiology().getTotalNumOfSourceMembersActuallyMapped());
                sm.setElement(r, 3, "Details >>");
                cell_id_linkMap.put(sm.getElementPos(r, 3), new LinkedFactory.SimpleLinkedFile("Details>>", files[r]));
            }

            summary_table = new RichDataframe(new StringDataframe(mapper.getName(), sm, rowNames, colNames, true),
                    null, null, cell_id_linkMap);
        } // End etiology
    
        final GeneSetMatrix mapped_gm = Chip2ChipMapper.createCombinedGeneSetMatrix(NamingConventions.removeExtension(gm_to_map.getName()) + "_mapped_to_" + mapper.getTargetChip().getName(), mgm);
        final File gm_file = report.savePage(mapped_gm, fGmFormatParam);
    
        final ReportBlocks.SimpleBlockListing list = new ReportBlocks.SimpleBlockListing("The following " + gm_to_map.getNumGeneSets() + " gene sets were mapped. <br>The results are in this gmt file", report);
        list.add(gm_file.getName(), gm_file);
        list.close();
    
        // do this after the rest
        if (list_etiology != null) {
            list_etiology.close();
        }
    
        HtmlReportIndexPage htmlPageHtml = report.getIndexPage();
        if (showEtiology) {
            final File xlsFile = report.savePageXls(summary_table);
            htmlPageHtml.addTable(summary_table, xlsFile.getName(), true, true);
        }
    
        report.getIndexPage().setAddBrowseFooter(false);
    
    }

    /**
     * Class constructor
     *
     * @param properties
     */
    public Chip2Chip(Properties properties) {
        super.init(properties);
    }

    public Chip2Chip(String[] args) {
        super.init(args);
    }

    /**
     * For ParamSet interrogation use only -- not executable
     *
     * @param name
     */
    public Chip2Chip() {
        declareParams();
    }

    public void execute() throws Exception {
        // to preserve memory & for custom indexing
        final ReportIndexState state = new ReportIndexState(true, false, true, createHeader());
        startExec(state);

        // Note that we MUST set the altDelim on the fGeneSetMatrixParam if it's present.  This MUST happen
        // before extracting the param value or it will be parsed incorrectly.  Unfortunately, these params
        // don't give any other good way to specify param dependencies except via code.
        if (fAltDelimParam.isSpecified() && StringUtils.isNotBlank(fAltDelimParam.getValue().toString())) {
            fGeneSetMatrixParam.setAlternateDelimiter(fAltDelimParam.getValue().toString());
        }

        final GeneSetMatrix gm = fGeneSetMatrixParam.getGeneSetMatrixCombo(true);
        final Chip2ChipMapper mapper = new Chip2ChipMapper(fChipTargetParam.getChip());

        doMapping(mapper, gm, fShowEtiology.isTrue(), fGmFormatParam, fReport);

        if (fMakeZippedReportParam.isTrue()) {
            // custom close before zipping
            fReport.closeReport(true);
            fReport.zipReport();
        }

        doneExec();
    }

    public String getDesc() {
        return "Map probe sets between chip platforms";
    }

    public void declareParams() {
        fParamSet.addParam(fGeneSetMatrixParam);
        fParamSet.addParam(fGmFormatParam);
        fParamSet.addParam(fShowEtiology);
        fParamSet.addParam(fChipTargetParam);
        fParamSet.addParam(fAltDelimParam);
        fParamSet.addParam(fMakeZippedReportParam);
    }

    public ToolCategory getCategory() {
        return ToolCategory.MAPPING_TOOLS;
    }

    public static void main(String[] args) {
        Chip2Chip tool = new Chip2Chip(args);
        tool_main(tool);
    }

}    // End Symbol2Probe