/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
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
import edu.mit.broad.vdb.VdbRuntimeResources;
import edu.mit.broad.vdb.map.Chip2ChipMapper;
import edu.mit.broad.vdb.map.Chip2ChipMapperSymbol;
import edu.mit.broad.vdb.map.MGeneSetMatrix;
import edu.mit.broad.vdb.map.MapUtils;
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

    private final BooleanParam fShowEtiology = ParamFactory.createShowEtiologyParam(true, false);

    private final GeneSetMatrixFormatParam fGmFormatParam = new GeneSetMatrixFormatParam(DataFormat.GMT_FORMAT, false);

    protected final ChipChooserMultiParam fChipsTargetParam = ParamFactory.createChipsTargetParam(true);

    private final StringInputParam fAltDelimParam = new StringInputParam("altDelim", "Alternate delimiter", 
            "Optional alternate delimiter character for gene set names instead of comma", null, false, new char[] { ';' }, Param.ADVANCED);

    public static String createHeader() {
        StringBuffer buf = new StringBuffer();
        buf.append("<div id=\"footer\" style=\"width: 905; height: 35\">\n").append(
                "<h3 style=\"text-align: left\"><font color=\"#808080\">Chip2Chip Report").append("</font></h3>\n").append("</div>");
    
        return buf.toString();
    }

    protected static void doMapping(final Chip2ChipMapper[] mappers,
                                    final GeneSetMatrix gm_to_map,
                                    final boolean showEtiology,
                                    final GeneSetMatrixFormatParam fGmFormatParam,
                                    final ToolReport report) throws Exception {
    
        final MGeneSetMatrix[] mgms = new MGeneSetMatrix[mappers.length];
    
        ReportBlocks.SimpleBlockListing list_etiology = null;
        RichDataframe[] summary_tables = new RichDataframe[mappers.length];
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < mappers.length; i++) {
            mgms[i] = mappers[i].map(gm_to_map, showEtiology);
            buf.append(mappers[i].getTargetChip().getName());
    
            // @note one per mapper
            if (showEtiology) {
                if (list_etiology == null) {
                    list_etiology = new ReportBlocks.SimpleBlockListing("Mapping details", report);
                }
    
                final MappingEtiology[] mes = mgms[i].getEtiologies();
                File[] files = new File[mes.length];
                for (int m = 0; m < mes.length; m++) {
                    String desc = "Mapping details for " + gm_to_map.getGeneSetName(m) + " by " + mappers[i].getName();
                    files[m] = report.savePageTxt(gm_to_map.getGeneSetName(m) + "_" + mappers[i].getName() + "_etiology",
                            desc,
                            mes[m].getStory(), true, false);
                    //list_etiology.add("Mapping details for", gm.getGeneSetName(m), " by " + mappers[i].getName(), files[m]);
                }
    
                // Make a summary etiology always
                final String[] colNames = new String[]{"#SIZE BEFORE MAPPING", "#SIZE AFTER MAPPING", "#SOURCE PROBES THAT HAD ONE OR MORE MATCHES", "DETAILS"};
                final String[] rowNames = new String[gm_to_map.getNumGeneSets()];
                final StringMatrix sm = new StringMatrix(rowNames.length, colNames.length);
                final TIntObjectHashMap cell_id_linkMap = new TIntObjectHashMap();
                for (int r = 0; r < gm_to_map.getNumGeneSets(); r++) {
                    rowNames[r] = gm_to_map.getGeneSetName(r);
                    sm.setElement(r, 0, gm_to_map.getGeneSet(r).getNumMembers());
                    sm.setElement(r, 1, mgms[i].getMappedGeneSet(r).getMappedGeneSet(true).getNumMembers());
                    sm.setElement(r, 2, mgms[i].getMappedGeneSet(r).getEtiology().getTotalNumOfSourceProbesActuallyMapped());
                    sm.setElement(r, 3, "Details >>");
                    cell_id_linkMap.put(sm.getElementPos(r, 3), new LinkedFactory.SimpleLinkedFile("Details>>", files[r]));
                }
    
                summary_tables[i] = new RichDataframe(new StringDataframe(mappers[i].getName(), sm, rowNames, colNames, true),
                        null, null, cell_id_linkMap);
            } // End etiology
        }
    
        final GeneSetMatrix mapped_gm = MapUtils.createCombinedGeneSetMatrix(NamingConventions.removeExtension(gm_to_map.getName()) + "_mapped_to_" + buf.toString(), mgms);
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
            for (int r = 0; r < summary_tables.length; r++) {
                final File xlsFile = report.savePageXls(summary_tables[r]);
                htmlPageHtml.addTable(summary_tables[r], xlsFile.getName(), true, true);
            }
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
        final Chip2ChipMapper[] mappers = new Chip2ChipMapper[]{new Chip2ChipMapperSymbol(VdbRuntimeResources.getChip_Gene_Symbol(), fChipsTargetParam.getChipCombo())};

        doMapping(mappers, gm, fShowEtiology.isTrue(), fGmFormatParam, fReport);

        doneExec();
    }

    public String getDesc() {
        return "Map probe sets between chip platforms";
    }

    public void declareParams() {
        fParamSet.addParam(fGeneSetMatrixParam);
        fParamSet.addParam(fGmFormatParam);
        fParamSet.addParam(fShowEtiology);
        fParamSet.addParam(fChipsTargetParam);
        fParamSet.addParam(fAltDelimParam);
    }

    public ToolCategory getCategory() {
        return ToolCategory.MAPPING_TOOLS;
    }

    public static void main(String[] args) {
        Chip2Chip tool = new Chip2Chip(args);
        tool_main(tool);
    }

}    // End Symbol2Probe