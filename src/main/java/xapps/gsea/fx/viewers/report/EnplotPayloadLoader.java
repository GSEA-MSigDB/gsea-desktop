/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import edu.mit.broad.genome.alg.gsea.GeneSetScoringTable;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResult;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentScore;
import edu.mit.broad.genome.reports.EnrichmentEsProfiles;
import edu.mit.broad.genome.reports.EnrichmentReports;
import edu.mit.broad.genome.reports.ModernEnrichmentPlotJson;

/**
 * Resolves interactive EnPlot v2 JSON from disk cache or in-memory enrichment results.
 * <p>
 * Disk reads are safe off the FX thread. Building from {@link EnrichmentResult} must run
 * on the FX thread (EDB / ranked list are not thread-safe).
 */
public final class EnplotPayloadLoader {

    private EnplotPayloadLoader() {
    }

    public static File jsonFile(File reportDir, String geneSetName) {
        return reportDir != null ? ModernEnrichmentPlotJson.jsonFile(reportDir, geneSetName) : null;
    }

    /** Background-safe: read validated JSON from disk only. */
    public static String readCachedJson(File jsonFile, int listSize) throws IOException {
        if (jsonFile == null || !jsonFile.isFile()) {
            return null;
        }
        String json = Files.readString(jsonFile.toPath());
        return ModernEnrichmentPlotJson.isCurrentPayload(json, listSize) ? json : null;
    }

    /** FX-thread only: build JSON from enrichment result (may recompute ES curve). */
    public static String buildFromResult(EnrichmentResult result, String classA, String classB,
            GeneSetScoringTable scoring) {
        if (result == null || result.getGeneSet() == null || result.getRankedList() == null
                || result.getScore() == null) {
            return null;
        }
        String geneSetName = result.getGeneSet().getName(true);
        RankedList rl = result.getRankedList();
        EnrichmentScore score = result.getScore();
        return ModernEnrichmentPlotJson.buildPayloadJson(geneSetName, rl, score.getHitIndices(),
                score.getESProfile(), EnrichmentEsProfiles.fullEsProfile(result, scoring),
                classA, classB, score.getES(), score.getNES(), score.getNP(),
                score.getFDR(), score.getFWER(),
                EnrichmentReports.colorBarMarkers(rl));
    }
}
