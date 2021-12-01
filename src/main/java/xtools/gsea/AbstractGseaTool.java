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

    public ToolCategory getCategory() { return ToolCategory.GSEA; }

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
        final int numRow = ds.getNumRow();
		if (gset.getNumMembers() == numRow) {
            return ds;
        } else {
            // Preserve any warnings attached to the DS
            if (!ds.getWarnings().isEmpty()) {
                for (String warning : ds.getWarnings()) {
                    fReport.addWarning(warning);
                }
            }
            StringBuilder buf = new StringBuilder();
            buf.append("There were duplicate row identifiers in the specified dataset. One id was arbitarilly choosen. Details are below");
            buf.append("\n<br>Generally this is OK but if you want to avoid this, edit your dataset so that all row ids are unique\n<br>");
            buf.append('\n');
            buf.append("<br># of row ids in original dataset: ").append(numRow).append('\n');
            buf.append("<br># of row UNIQUE ids in original dataset: ").append(gset.getNumMembers()).append('\n');
            buf.append("<br>The duplicates were\n<br><pre>");

            Set<String> all = new HashSet<>();
            Set<String> dup = new HashSet<>();
            int perLine = 0;
            for (int i = 0; i < numRow; i++) {
                String member = ds.getRowName(i);
                if (all.contains(member)) {
            		// Only show each dup once
                	if (!dup.contains(member)) {
                		buf.append(member).append('\t');
                		// Allow 5 per line
                		if (perLine++ > 5) {
                			buf.append('\n');
                			perLine = 0;
                		}
                		dup.add(member);
                	}
                } else {
                    all.add(member);
                }
            }

            buf.append("</pre>");
            fReport.addWarning(buf.toString());
            return new DatasetGenerators().extractRows(ds, gset);
        }
    }

    protected RankedList uniquize(final RankedList rl) {
        final GeneSet gset = new GeneSet(rl.getName(), rl.getName(), rl.getRankedNames(), true);
        final int size = rl.getSize();
		if (gset.getNumMembers() == size) {
            return rl;
        } else {
            // Preserve any warnings attached to the RL
            if (!rl.getWarnings().isEmpty()) {
                for (String warning : rl.getWarnings()) {
                    fReport.addWarning(warning);
                }
            }
            StringBuilder buf = new StringBuilder();
            buf.append("There were duplicate row identifiers in the specified ranked list. One id was arbitarilly choosen. Details are below. ");
            buf.append("\n<br>Generally this is OK but if you want to avoid this, edit your ranked list so that all row ids are unique\n<br>");
            buf.append('\n');
            buf.append("<br># of row ids in original dataset: ").append(size).append('\n');
            buf.append("<br># of row UNIQUE ids in original dataset: ").append(gset.getNumMembers()).append('\n');
            buf.append("<br>The duplicates were\n<br><pre>");

            Set<String> all = new HashSet<>();
            Set<String> dup = new HashSet<>();
            int perLine = 0;
            for (int i = 0; i < size; i++) {
                String member = rl.getRankName(i);
                if (all.contains(member)) {
            		// Only show each dup once
                	if (!dup.contains(member)) {
                		buf.append(member).append('\t');
                		// Allow 5 per line
                		if (perLine++ > 5) {
                			buf.append('\n');
                			perLine = 0;
                		}
                		dup.add(member);
                	}
                } else {
                    all.add(member);
                }
            }

            buf.append("</pre>");
            fReport.addWarning(buf.toString());
            return rl.extractRanked(gset);
        }
    }
}
