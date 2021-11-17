/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.gsea;

import edu.mit.broad.genome.alg.DatasetGenerators;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.RankedList;
import xtools.api.AbstractTool;
import xtools.api.ToolCategory;
import xtools.api.param.*;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Aravind Subramanian, David Eby
 */
public abstract class AbstractGseaTool extends AbstractTool {
    protected GeneSetMatrixMultiChooserParam fGeneSetMatrixParam;
    protected ChipOptParam fChipParam = new ChipOptParam(false);

    // These settings for gene set size correspond to that used in the paper
    protected final IntegerParam fGeneSetMinSizeParam = new IntegerParam("set_min", "Min size: exclude smaller sets", "Gene sets smaller than this number are EXLCUDED from the analysis", 15, false);
    protected final IntegerParam fGeneSetMaxSizeParam = new IntegerParam("set_max", "Max size: exclude larger sets", "Gene sets larger than this number are EXLCUDED from the analysis", 500, false);

    protected final IntegerParam fNumPermParam = new IntegerParam("nperm", "Number of permutations", "The number of permutations", 1000, new int[]{0, 1, 10, 100, 1000}, true);
    protected final RandomSeedTypeParam fRndSeedTypeParam = new RandomSeedTypeParam(this);

    protected final ModeReqdParam fCollapseModeParam; 
    protected final FeatureSpaceReqdParam fFeatureSpaceParam;
    protected final BooleanParam fIncludeOnlySymbols = new BooleanParam("include_only_symbols", "Omit features with no symbol match", "If there is no known gene symbol match for a probe set omit if from the collapsed dataset", true, false);

    // restrict to just the regular norm mode??
    protected final NormModeReqdParam fNormModeParam = new NormModeReqdParam();
    protected final GeneSetScoringTableReqdParam fGcohGenReqdParam = new GeneSetScoringTableReqdParam();

    protected AbstractGseaTool(String defFeatureSpace, String defCollapseMode) {
        fFeatureSpaceParam = new FeatureSpaceReqdParam(defFeatureSpace);
        fCollapseModeParam = new ModeReqdParam("mode", "Collapsing mode for probe sets => 1 gene", "Collapsing mode for probe sets => 1 gene", defCollapseMode, new String[]{"Max_probe", "Median_of_probes", "Mean_of_probes", "Sum_of_probes", "Abs_max_of_probes"});
    }

    public ToolCategory getCategory() {
        return ToolCategory.GSEA;
    }

    protected abstract Param[] getAdditionalParams();

    protected void doAdditionalParams() { }

    public void declareParams() {
        fParamSet.addParamPseudoReqd(fChipParam);
        this.fGeneSetMatrixParam = new GeneSetMatrixMultiChooserParam(true);

        // reqd
        fParamSet.addParam(fGeneSetMatrixParam);
        fParamSet.addParam(fNumPermParam);

        // basic
        fParamSet.addParamBasic(fGcohGenReqdParam);
        fParamSet.addParamBasic(fGeneSetMinSizeParam);
        fParamSet.addParamBasic(fGeneSetMaxSizeParam);
        fParamSet.addParamPseudoReqd(fFeatureSpaceParam);
        fParamSet.addParamAdv(fCollapseModeParam);
        
        // advanced
        fParamSet.addParamAdv(fRndSeedTypeParam);
        fParamSet.addParamAdv(fNormModeParam);
        fParamSet.addParamAdv(fIncludeOnlySymbols);

        doAdditionalParams();

        Param[] add = getAdditionalParams();
        for (int i = 0; i < add.length; i++) {
            fParamSet.addParam(add[i]);
        }
    }

    protected Dataset uniquize(final Dataset ds) {
        final GeneSet gset = ds.getRowNamesGeneSet();
        if (gset.getNumMembers() == ds.getNumRow()) {
            return ds;
        } else {
            StringBuilder buf = new StringBuilder();
            buf.append("There were duplicate row identifiers in the specified dataset. One id was arbitarilly choosen. Details are below");
            buf.append("\nGenerally, this is OK, but if you want to avoid this automagic, edit your dataset so that all row ids are unique\n");
            buf.append('\n');
            buf.append("# of row ids in original dataset: ").append(ds.getNumRow()).append('\n');
            buf.append("# of row UNIQUE ids in original dataset: ").append(gset.getNumMembers()).append('\n');
            buf.append("# The duplicates were\n");

            Set<String> all = new HashSet<>();
            for (int i = 0; i < ds.getNumRow(); i++) {
                String member = ds.getRowName(i);
                if (all.contains(member)) {
                    buf.append(member).append('\n');
                }

                all.add(member);
            }

            fReport.addComment(buf.toString());
            return new DatasetGenerators().extractRows(ds, gset);
        }
    }

    protected RankedList uniquize(final RankedList rl) {
        final GeneSet gset = new GeneSet(rl.getName(), rl.getName(), rl.getRankedNames(), true);
        if (gset.getNumMembers() == rl.getSize()) {
            return rl;
        } else {
            StringBuilder buf = new StringBuilder();
            buf.append("There were duplicate row identifiers in the specified ranked list. One id was arbitarilly choosen. Details are below. ");
            buf.append("\nGenerally, this is OK, but if you want to avoid this automagic, edit your ranked list so that all row ids are unique\n");
            buf.append('\n');
            buf.append("# of row ids in original dataset: ").append(rl.getSize()).append('\n');
            buf.append("# of row UNIQUE ids in original dataset: ").append(gset.getNumMembers()).append('\n');
            buf.append("# The duplicates were\n<br><pre>");

            Set<String> all = new HashSet<>();
            for (int i = 0; i < rl.getSize(); i++) {
                String member = rl.getRankName(i);
                if (all.contains(member)) {
                    buf.append(member).append('\n');
                }

                all.add(member);
            }

            buf.append("</pre>");
            fReport.addComment(buf.toString());
            return rl.extractRanked(gset);
        }
    }
}
