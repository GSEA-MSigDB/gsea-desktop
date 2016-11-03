/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.gsea;

import edu.mit.broad.genome.alg.DatasetGenerators;
import edu.mit.broad.genome.alg.Metric;
import edu.mit.broad.genome.alg.Metrics;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.FSet;
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

    protected GeneSetMatrixMultiChooserParam fGeneSetMatrixParam;

    // These settings for gene set size correspond to that used in the paper
    protected final IntegerParam fGeneSetMinSizeParam = ParamFactory.createGeneSetMinSizeParam(15, false);
    protected final IntegerParam fGeneSetMaxSizeParam = ParamFactory.createGeneSetMaxSizeParam(500, false);

    protected final IntegerParam fNumPermParam = ParamFactory.createNumPermParam(true);
    protected final RandomSeedTypeParam fRndSeedTypeParam = new RandomSeedTypeParam(false);

    // restrict to just the regular norm mode??
    protected final NormModeReqdParam fNormModeParam = new NormModeReqdParam();
    protected final GeneSetScoringTableReqdParam fGcohGenReqdParam = new GeneSetScoringTableReqdParam();

    /**
     * Class constructor
     *
     * @param properties
     */
    protected AbstractGseaTool() {
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

        //this.fChipParam = new ChipChooserMultiParam(false);
        this.fGeneSetMatrixParam = new GeneSetMatrixMultiChooserParam(true);

        // reqd

        fParamSet.addParam(fGeneSetMatrixParam);
        fParamSet.addParam(fNumPermParam);

        // basic
        fParamSet.addParamBasic(fGcohGenReqdParam);
        fParamSet.addParamBasic(fGeneSetMinSizeParam);
        fParamSet.addParamBasic(fGeneSetMaxSizeParam);
        
        // advanced
        fParamSet.addParamAdv(fRndSeedTypeParam);
        fParamSet.addParamAdv(fNormModeParam);

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

        final GeneSet gset = new FSet(rl.getName(), rl.getName(), rl.getRankedNames(), true);
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
