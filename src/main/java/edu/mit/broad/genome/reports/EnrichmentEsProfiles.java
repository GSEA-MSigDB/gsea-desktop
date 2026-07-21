/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.reports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.alg.gsea.GeneSetCohort;
import edu.mit.broad.genome.alg.gsea.GeneSetScoringTable;
import edu.mit.broad.genome.alg.gsea.GeneSetScoringTables;
import edu.mit.broad.genome.alg.gsea.KSCore;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResult;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentScore;
import xtools.api.param.Param;

/**
 * Helpers for reconstructing the full (rank-by-rank) running ES curve.
 * <p>
 * {@code results.edb} persists only the hit-wise {@code ES_PROFILE}. The dense
 * point-by-point profile used for smooth mountain plots is available in memory
 * during the original GSEA run but is not written to the EDB. It can be
 * recomputed from the ranked list, gene set, and scoring scheme.
 */
public final class EnrichmentEsProfiles {

    private static final Logger klog = LoggerFactory.getLogger(EnrichmentEsProfiles.class);

    /** Resolved gene-set scoring table plus whether it came from saved report parameters. */
    public record ScoringResolution(GeneSetScoringTable table, boolean explicitFromReport, String label) {
    }

    private EnrichmentEsProfiles() {
    }

    /**
     * Best available full ES curve: in-memory profile if present, otherwise recomputed.
     */
    public static Vector fullEsProfile(final EnrichmentResult result, final GeneSetScoringTable scoring_opt) {
        if (result == null || result.getScore() == null) {
            return null;
        }
        Vector existing = result.getScore().getESProfile_point_by_point_opt();
        if (existing != null && existing.getSize() > 0) {
            return existing;
        }
        return recomputeFullEsProfile(result, scoring_opt);
    }

    /**
     * Recompute the rank-by-rank running ES via {@link KSCore} (same path as the original analysis).
     */
    public static Vector recomputeFullEsProfile(final EnrichmentResult result,
                                               final GeneSetScoringTable scoring_opt) {
        if (result == null) {
            return null;
        }
        RankedList rl = result.getRankedList();
        GeneSet gset = result.getGeneSet();
        if (rl == null || gset == null) {
            return null;
        }
        GeneSetScoringTable scoring = scoring_opt != null ? scoring_opt : new GeneSetScoringTables.Weighted();
        try {
            GeneSetCohort.Generator gen = new GeneSetCohort.Generator(scoring, 0, Integer.MAX_VALUE);
            // Gene sets in the EDB are already list-qualified; skip size filtering.
            GeneSet[] sets = new GeneSet[]{gset};
            GeneSetCohort gcoh = gen.createGeneSetCohort(rl, sets, true);
            EnrichmentScore[] scores = new KSCore().calculateKSScore(gcoh, true);
            if (scores == null || scores.length == 0 || scores[0] == null) {
                return null;
            }
            Vector full = scores[0].getESProfile_point_by_point_opt();
            if (full == null || full.getSize() != rl.getSize()) {
                klog.warn("Recomputed ES profile size mismatch for {} (got {}, expected {})",
                        gset.getName(true),
                        full != null ? full.getSize() : -1,
                        rl.getSize());
                return null;
            }
            return full;
        } catch (Throwable t) {
            klog.warn("Could not recompute full ES profile for {}: {}",
                    gset.getName(true), t.getMessage());
            return null;
        }
    }

    /** Resolve {@code scoring_scheme} from report parameters; default {@code weighted}. */
    public static ScoringResolution resolveScoring(java.util.Properties params) {
        if (params == null) {
            return new ScoringResolution(new GeneSetScoringTables.Weighted(), false, "weighted (default)");
        }
        String raw = params.getProperty(Param.SCORING_SCHEME);
        if (raw == null || raw.isBlank()) {
            raw = params.getProperty("param." + Param.SCORING_SCHEME);
        }
        if (raw == null || raw.isBlank()) {
            return new ScoringResolution(new GeneSetScoringTables.Weighted(), false, "weighted (default)");
        }
        String scheme = raw.trim();
        try {
            return new ScoringResolution(
                    GeneSetScoringTables.lookupGeneSetScoringTable(scheme), true, scheme);
        } catch (Throwable t) {
            klog.warn("Unknown scoring_scheme '{}'; defaulting to weighted", scheme);
            return new ScoringResolution(new GeneSetScoringTables.Weighted(), false,
                    "weighted (unknown scheme: " + scheme + ")");
        }
    }
}
