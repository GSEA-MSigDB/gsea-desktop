/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.gsea;

import edu.mit.broad.genome.alg.DatasetGenerators;
import edu.mit.broad.genome.alg.Metric;
import edu.mit.broad.genome.alg.Metrics;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.RankedList;
import xtools.api.AbstractTool;
import xtools.api.ToolCategory;
import xtools.api.param.*;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public abstract class AbstractGseaTool extends AbstractTool {

    // barfs if size is zero or content is zero for all sets
    public static void checkAndBarfIfZeroSets(final GeneSet[] qual_gsets) {
    
        boolean wasError = false;
        if (qual_gsets.length == 0) {
            wasError = true;
        } else {
            boolean at_least_one_non_empty_set = false;
            for (int i = 0; i < qual_gsets.length; i++) {
                if (qual_gsets[i].getNumMembers() > 0) {
                    at_least_one_non_empty_set = true;
                    break;
                }
            }
            if (!at_least_one_non_empty_set) {
                wasError = true;
            }
        }
    
        if (wasError) {
            throw new BadParamException("After pruning, none of the gene sets passed size thresholds.", 1001);
        }
    }

    protected GeneSetMatrixMultiChooserParam fGeneSetMatrixParam;
    protected ChipOptParam fChipParam = new ChipOptParam(false);

    // These settings for gene set size correspond to that used in the paper
    protected final IntegerParam fGeneSetMinSizeParam = new IntegerParam("set_min", "Min size: exclude smaller sets", "Gene sets smaller than this number are EXLCUDED from the analysis", 15, false);
    protected final IntegerParam fGeneSetMaxSizeParam = new IntegerParam("set_max", "Max size: exclude larger sets", "Gene sets larger than this number are EXLCUDED from the analysis", 500, false);

    protected final IntegerParam fNumPermParam = new IntegerParam("nperm", "Number of permutations", "The number of permutations", 1000, new int[]{0, 1, 10, 100, 1000}, true);
    protected final RandomSeedTypeParam fRndSeedTypeParam = new RandomSeedTypeParam(false);

    protected final ModeReqdParam fCollapseModeParam = new ModeReqdParam("mode", "Collapsing mode for probe sets => 1 gene", "Collapsing mode for probe sets => 1 gene", "Max_probe", new String[]{"Max_probe", "Median_of_probes", "Mean_of_probes", "Sum_of_probes"});
    protected final FeatureSpaceReqdParam fFeatureSpaceParam;
    protected final BooleanParam fIncludeOnlySymbols = new BooleanParam("include_only_symbols", "Omit features with no symbol match", "If there is no known gene symbol match for a probe set omit if from the collapsed dataset", true, false);

    // restrict to just the regular norm mode??
    protected final NormModeReqdParam fNormModeParam = new NormModeReqdParam();
    protected final GeneSetScoringTableReqdParam fGcohGenReqdParam = new GeneSetScoringTableReqdParam();

    /**
     * Class constructor
     *
     * @param properties
     */
    protected AbstractGseaTool(String defFeatureSpace) {
        fFeatureSpaceParam = new FeatureSpaceReqdParam(defFeatureSpace);
    }

    public ToolCategory getCategory() {
        return ToolCategory.GSEA;
    }

    // @maint add a metric and this array might need updating
    public static Metric[] createMetricsForGsea() {
        return new Metric[]{
                new Metrics.Signal2Noise(),
                new Metrics.tTest(),
                new Metrics.Cosine(),
                new Metrics.Euclidean(),
                new Metrics.Manhatten(),
                new Metrics.Pearson(),
                // new Metrics.None(),
                new Metrics.ClassRatio(),
                new Metrics.ClassDiff(),
                new Metrics.ClassLog2Ratio()
        };
    }

    protected abstract Param[] getAdditionalParams();

    protected void doAdditionalParams() {
    }

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
            StringBuffer buf = new StringBuffer();
            buf.append("There were duplicate row identifiers in the specified dataset. One id was arbitarilly choosen. Details are below");
            buf.append("\nGenerally, this is OK, but if you want to avoid this automagic, edit your dataset so that all row ids are unique\n");
            buf.append('\n');
            buf.append("# of row ids in original dataset: ").append(ds.getNumRow()).append('\n');
            buf.append("# of row UNIQUE ids in original dataset: ").append(gset.getNumMembers()).append('\n');
            buf.append("# The duplicates were\n");

            Set all = new HashSet();
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
            StringBuffer buf = new StringBuffer();
            buf.append("There were duplicate row identifiers in the specified ranked list. One id was arbitarilly choosen. Details are below. ");
            buf.append("\nGenerally, this is OK, but if you want to avoid this automagic, edit your ranked list so that all row ids are unique\n");
            buf.append('\n');
            buf.append("# of row ids in original dataset: ").append(rl.getSize()).append('\n');
            buf.append("# of row UNIQUE ids in original dataset: ").append(gset.getNumMembers()).append('\n');
            buf.append("# The duplicates were\n<br><pre>");

            Set all = new HashSet();
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

}    // End AbstractGseaTool
