/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.munge;

import edu.mit.broad.genome.alg.DatasetGenerators;
import edu.mit.broad.genome.math.StringMatrix;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.StringDataframe;
import edu.mit.broad.vdb.chip.Chip;
import xtools.api.AbstractTool;
import xtools.api.ToolCategory;
import xtools.api.param.BooleanParam;
import xtools.api.param.ChipChooserMultiParam;
import xtools.api.param.DatasetReqdParam;
import xtools.api.param.ModeReqdParam;

import java.util.Properties;

/**
 * 1 file per GeneSet
 * affy probe name, gene name, desc
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class CollapseDataset extends AbstractTool {

    private final ChipChooserMultiParam fChipParam = new ChipChooserMultiParam(true);
    private final DatasetReqdParam fDatasetParam = new DatasetReqdParam();

    private final ModeReqdParam fModeParm = new ModeReqdParam("mode", "Collapsing mode for probe sets => 1 gene", "Collapsing mode for probe sets => 1 gene", new String[]{"Max_probe", "Median_of_probes"});
    private final BooleanParam fIncludeOnlySymbols = new BooleanParam("include_only_symbols", "Omit features with no symbol match", "If there is no known gene symbol match for a probe set omit if from the collapsed dataset", true, false);

    /**
     * Class constructor
     *
     * @param properties
     */
    public CollapseDataset(Properties properties) {
        super.init(properties);
    }

    public CollapseDataset(String[] args) {
        super.init(args);
    }

    /**
     * For ParamSet interrogation use only -- not executable
     *
     * @param name
     */
    public CollapseDataset() {
        declareParams();
    }

    public void execute() throws Exception {
        startExec();

        final Chip chip = fChipParam.getChipCombo();

        Dataset ds = fDatasetParam.getDataset();

        DatasetGenerators.CollapsedDataset cds = new DatasetGenerators().collapse_core(ds, chip,
                fIncludeOnlySymbols.isTrue(), fModeParm.getStringIndexChoosen());

        log.debug("# after collapsing: " + cds.symbolized.getNumRow());

        fReport.savePage(cds.symbolized);

        // Make a report with the etiology

        // Make a summary etiology always
        final String[] colNames = new String[]{"# MATCHING PROBE SETS", "MATCHING PROBE SET(S)"};
        final String[] rowNames = new String[cds.symbolized.getNumRow()];
        final StringMatrix sm = new StringMatrix(rowNames.length, colNames.length);
        for (int r = 0; r < cds.symbolized.getNumRow(); r++) {
            rowNames[r] = cds.symbolized.getRowName(r);
            DatasetGenerators.CollapseStruc cs = (DatasetGenerators.CollapseStruc) cds.symbolCollapseStrucMap.get(cds.symbolized.getRowName(r));
            sm.setElement(r, 0, cs.getProbes().length);
            sm.setElement(r, 1, cs.getProbes());
        }

        fReport.savePageXls(new StringDataframe("Symbol_to_probe_set_mapping_details", sm, rowNames, colNames, true));

        doneExec();
    }

    public String getDesc() {
        return "Collapse expression values from multiple probe sets of a gene to a single value";
    }

    public ToolCategory getCategory() {
        return ToolCategory.DATASET;
    }

    public void declareParams() {
        fParamSet.addParam(fChipParam);
        fParamSet.addParam(fDatasetParam);
        fParamSet.addParam(fIncludeOnlySymbols);
        fParamSet.addParam(fModeParm);
    }

    public static void main(String[] args) {
        CollapseDataset tool = new CollapseDataset(args);
        tool_main(tool);
    }

}    // End CollapseDataset