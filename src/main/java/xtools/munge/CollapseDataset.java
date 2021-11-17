/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.munge;

import edu.mit.broad.genome.alg.DatasetGenerators;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.vdb.chip.Chip;
import xtools.api.AbstractTool;
import xtools.api.ToolCategory;
import xtools.api.param.BooleanParam;
import xtools.api.param.ChipOptParam;
import xtools.api.param.DatasetReqdParam;
import xtools.api.param.ModeReqdParam;
import xtools.api.param.StringInputParam;

import java.util.Properties;

/**
 * 1 file per GeneSet
 * affy probe name, gene name, desc
 *
 * @author Aravind Subramanian, David Eby
 */
public class CollapseDataset extends AbstractTool {
    private final ChipOptParam fChipParam = new ChipOptParam(true);
    private final DatasetReqdParam fDatasetParam = new DatasetReqdParam();

    private final ModeReqdParam fModeParam = new ModeReqdParam("mode", "Collapsing mode for probe sets => 1 gene", "Collapsing mode for probe sets => 1 gene", "Max_probe", new String[]{"Max_probe", "Median_of_probes", "Mean_of_probes", "Sum_of_probes", "Abs_max_of_probes", "Remap_only"});
    private final BooleanParam fIncludeOnlySymbols = new BooleanParam("include_only_symbols", "Omit features with no symbol match", "If there is no known gene symbol match for a probe set omit if from the collapsed dataset", true, false);

    private final StringInputParam fResultFileNameParam = new StringInputParam("out_file", "Result File Name", "Name for the resulting collapsed dataset.  The default is to use a name based on the original plus the collapsing mode", "", false);
    
    public CollapseDataset(Properties properties) { super.init(properties, ""); }

    public CollapseDataset(Properties properties, String paramFilePath) { super.init(properties, paramFilePath); }

    public CollapseDataset(String[] args) { super.init(args); }

    /**
     * For ParamSet interrogation use only -- not executable
     *
     * @param name
     */
    public CollapseDataset() { declareParams(); }
    
    public String getName() { return "CollapseDataset"; }

    public void execute() throws Exception {
        startExec();

        final Chip chip = fChipParam.getChip();

        Dataset ds = fDatasetParam.getDataset();
        DatasetGenerators.CollapsedDataset cds = new DatasetGenerators().collapse(ds, chip, fIncludeOnlySymbols.isTrue(),
                fModeParam.getStringIndexChoosen(), fResultFileNameParam.getValueStringRepresentation(true));

        log.debug("# after collapsing: " + cds.symbolized.getNumRow());

        fReport.savePage(cds.symbolized, true);

        // Make a summary etiology always
        fReport.savePageTsv(cds.makeEtiologySdf());

        doneExec();
    }

    public String getDesc() { return "Collapse expression values from multiple probe sets of a gene to a single value"; }

    public ToolCategory getCategory() { return ToolCategory.DATASET; }

    public void declareParams() {
        fParamSet.addParam(fChipParam);
        fParamSet.addParam(fDatasetParam);
        fParamSet.addParam(fIncludeOnlySymbols);
        fParamSet.addParam(fModeParam);
        fParamSet.addParamAdv(fResultFileNameParam);
    }

    public static void main(String[] args) {
        CollapseDataset tool = new CollapseDataset(args);
        tool_main(tool);
    }
}
