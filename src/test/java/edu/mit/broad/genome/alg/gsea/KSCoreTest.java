/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.alg.gsea;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.DefaultRankedList;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentScore;

/**
 * Classic Kolmogorov–Smirnov walk on a four-gene list.
 * Hit weight is 1/Nh and miss weight is 1/(N-Nh), so a set that occupies
 * the top or the bottom of the list reaches an enrichment score of ±1.
 */
class KSCoreTest {

    @Test
    void classicScoreIsPositiveWhenHitsLeadTheList() {
        assertEquals(1.0f, score(new GeneSet("top", new String[] { "A", "B" })), 1e-5f);
    }

    @Test
    void classicScoreIsNegativeWhenHitsTrailTheList() {
        assertEquals(-1.0f, score(new GeneSet("bottom", new String[] { "C", "D" })), 1e-5f);
    }

    private static float score(GeneSet set) {
        RankedList ranked = new DefaultRankedList("rl", List.of("A", "B", "C", "D"),
                new Vector(new float[] { 4f, 3f, 2f, 1f }));
        GeneSetCohort cohort = new GeneSetCohort.Generator(new GeneSetScoringTables.Classic(), 1, 500)
                .createGeneSetCohort(ranked, new GeneSet[] { set }, true);
        EnrichmentScore[] scores = new KSCore().calculateKSScore(cohort, false);
        return scores[0].getES();
    }
}
