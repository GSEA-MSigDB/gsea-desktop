/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.gsea;

import edu.mit.broad.genome.alg.DatasetGenerators;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.StringDataframe;
import edu.mit.broad.genome.objects.strucs.CollapsedDetails;
import edu.mit.broad.genome.parsers.GctParser;
import edu.mit.broad.genome.reports.api.ToolReport;
import edu.mit.broad.vdb.chip.Chip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xtools.api.param.BadParamException;
import xtools.api.param.BooleanParam;
import xtools.api.param.ChipOptParam;
import xtools.api.param.FeatureSpaceReqdParam;
import xtools.api.param.ModeReqdParam;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Shared data preparation for tools that start from an expression GCT/RES dataset: uniquizing row ids
 * and optional chip-based collapse to gene symbols (same path as {@link Gsea}).
 */
public final class GseaExpressionDataPrep {

    private static final Logger log = LoggerFactory.getLogger(GseaExpressionDataPrep.class);

    private GseaExpressionDataPrep() {
    }

    /**
     * @param exportCollapsedGctDir if non-null (e.g. report {@code edb/}), the collapsed symbol dataset
     *                                is written as {@code <datasetName>.gct} in that directory (GSEA "create gcts" behavior)
     */
    public static class CollapseOutcome {
        public final CollapsedDetails.Data data;
        /** present when the dataset was collapsed to symbols (probe → gene etiology table) */
        public final File etiologyTsv;

        public CollapseOutcome(CollapsedDetails.Data data, File etiologyTsv) {
            this.data = data;
            this.etiologyTsv = etiologyTsv;
        }
    }

    /**
     * Deduplicate dataset row names (one row per id), with the same messages as {@code AbstractGseaTool#uniquize(Dataset)}.
     */
    public static Dataset uniquizeDataset(final Dataset ds, final ToolReport report) {
        final GeneSet gset = ds.getRowNamesGeneSet();
        final int numRow = ds.getNumRow();
        if (gset.getNumMembers() == numRow) {
            return ds;
        }
        if (!ds.getWarnings().isEmpty()) {
            for (String warning : ds.getWarnings()) {
                report.addWarning(warning);
            }
        }
        StringBuilder buf = new StringBuilder();
        buf.append("There were duplicate row identifiers in the specified dataset. One id was arbitarilly choosen. Details are below");
        buf.append("\n<br>Generally this is OK but if you want to avoid this, edit your dataset so that all row ids are unique\n<br>");
        buf.append('\n');
        buf.append("<br># of row ids in original dataset: ").append(numRow).append('\n');
        buf.append("<br># of row UNIQUE ids in the original dataset: ").append(gset.getNumMembers()).append('\n');
        buf.append("<br>The duplicates were\n<br><pre>");

        Set<String> all = new HashSet<String>();
        Set<String> dup = new HashSet<String>();
        int perLine = 0;
        for (int i = 0; i < numRow; i++) {
            String member = ds.getRowName(i);
            if (all.contains(member)) {
                if (!dup.contains(member)) {
                    buf.append(member).append('\t');
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
        report.addWarning(buf.toString());
        return new DatasetGenerators().extractRows(ds, gset);
    }

    /**
     * Collapse to symbols when requested; otherwise pass the dataset through unchanged. Saves etiology TSV
     * when collapsing (same as {@link Gsea#getDataset(edu.mit.broad.genome.objects.Dataset)}).
     */
    public static CollapseOutcome collapseForExpressionUse(
            final Dataset origDs,
            final FeatureSpaceReqdParam featureSpace,
            final ChipOptParam chipParam,
            final ModeReqdParam collapseMode,
            final BooleanParam includeOnlySymbols,
            final ToolReport report,
            final File exportCollapsedGctDirOrNull) throws Exception {

        final CollapsedDetails.Data cd = new CollapsedDetails.Data();
        cd.orig = origDs;
        File etiologyTsv = null;

        if (featureSpace.isSymbols()) {
            if (!chipParam.isSpecified()) {
                throw new BadParamException("Chip parameter must be specified as you asked to analyze"
                        + " in the space of gene symbols. Chip is used to collapse probe ids into symbols", 1002);
            }
            final Chip chip = chipParam.getChip();
            final int collapseModeIndex = featureSpace.isRemap() ? 5 : collapseMode.getStringIndexChoosen();
            final DatasetGenerators.CollapsedDataset cds = new DatasetGenerators().collapse(origDs, chip,
                    includeOnlySymbols.isTrue(), collapseModeIndex, null);
            final Dataset collapsed = cds.symbolized;
            log.info("Collapsing dataset: {} -> {}", origDs.getQuickInfo(), collapsed.getQuickInfo());
            StringDataframe et = cds.makeEtiologySdf();
            etiologyTsv = report.savePageTsv(et);

            if (exportCollapsedGctDirOrNull != null) {
                if (!exportCollapsedGctDirOrNull.exists()) {
                    exportCollapsedGctDirOrNull.mkdirs();
                }
                final File collapsedGct = new File(exportCollapsedGctDirOrNull, collapsed.getName() + ".gct");
                new GctParser().export(collapsed, collapsedGct);
            }

            cd.chip = chip;
            cd.wasCollapsed = true;
            cd.collapsed = collapsed;
            if (cd.getNumRow_orig() != 0 && cd.getNumRow_collapsed() == 0) {
                throw new BadParamException("The collapsed dataset was empty when used with chip: " + cd.getChipName(), 1005);
            }
        } else {
            cd.wasCollapsed = false;
            cd.collapsed = origDs;
            log.info("No dataset collapsing was done .. using original as is");
        }

        return new CollapseOutcome(cd, etiologyTsv);
    }
}
