/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.reports;

import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.plots.ColorBarSegment;
import edu.mit.broad.genome.plots.MountainPlotSpec;
import edu.mit.broad.genome.plots.PlotBuilders;
import edu.mit.broad.genome.plots.PlotChart;

/**
 * Enrichment mountain / combo charts via {@link MountainPlotSpec}.
 * Classic reports use {@link EnrichmentReports#ENPLOT_}; EnPlot v2 uses {@link EnrichmentReports#ENPLOT2_}.
 */
public final class ModernEnrichmentCharts {

    private ModernEnrichmentCharts() {
    }

    public static EnrichmentCharts createComboChart(final String gsetName,
                                                    final Vector enrichmentScoreProfile,
                                                    final Vector esProfile_full_opt,
                                                    final Vector hitIndices,
                                                    final RankedList rl,
                                                    final String classAName_opt,
                                                    final String classBName_opt,
                                                    final ColorBarSegment[] markers,
                                                    final float es,
                                                    final float nes,
                                                    final float np,
                                                    final float fdr) {
        return createComboChart(EnrichmentReports.ENPLOT2_, gsetName, enrichmentScoreProfile,
                esProfile_full_opt, hitIndices, rl, classAName_opt, classBName_opt, markers, es, nes, np, fdr);
    }

    public static EnrichmentCharts createComboChart(final String filePrefix,
                                                    final String gsetName,
                                                    final Vector enrichmentScoreProfile,
                                                    final Vector esProfile_full_opt,
                                                    final Vector hitIndices,
                                                    final RankedList rl,
                                                    final String classAName_opt,
                                                    final String classBName_opt,
                                                    final ColorBarSegment[] markers,
                                                    final float es,
                                                    final float nes,
                                                    final float np,
                                                    final float fdr) {
        if (enrichmentScoreProfile == null) {
            throw new IllegalArgumentException("Param scoreProfile cannot be null");
        }
        if (hitIndices == null) {
            throw new IllegalArgumentException("Param hitProfile cannot be null");
        }
        if (rl == null) {
            throw new IllegalArgumentException("Param rl cannot be null");
        }

        String prefix = filePrefix != null ? filePrefix : EnrichmentReports.ENPLOT2_;
        boolean classic = EnrichmentReports.ENPLOT_.equals(prefix);
        MountainPlotSpec.Style style = classic ? MountainPlotSpec.Style.CLASSIC : MountainPlotSpec.Style.V2;

        int[] hits = PlotBuilders.hitRanksFromMembership(hitIndices);
        EnrichmentMountainData data = EnrichmentMountainData.build(
                enrichmentScoreProfile, esProfile_full_opt, hits, rl);

        // Classic historically stored a long combo description for metadata only; it was never
        // painted under the JFreeChart title. V2 keeps a short stats caption.
        String caption = classic
                ? ""
                : String.format("ES=%.4f NES=%.4f NOM pVal=%.4f FDR=%.4f", es, nes, np, fdr);
        MountainPlotSpec mountain = new MountainPlotSpec(
                prefix + gsetName,
                "Enrichment plot: " + gsetName,
                caption,
                style,
                data.listSize,
                data.esX, data.esY,
                data.hitRanks, data.leadingEdge,
                data.metricY, data.metricName,
                classAName_opt, classBName_opt,
                markers,
                data.peakRank, data.peakEs,
                es, nes, np, fdr,
                data.zeroCross);

        return new EnrichmentCharts(new PlotChart(mountain));
    }
}
