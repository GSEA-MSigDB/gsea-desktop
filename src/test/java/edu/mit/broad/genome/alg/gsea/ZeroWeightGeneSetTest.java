/*
 * Copyright (c) 2003-2025 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.alg.gsea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.DefaultRankedList;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentScore;

/**
 * A gene set whose members all have a ranking score of 0 has a total weight of 0 under the weighted
 * schemes.  Its hits must get equal weights (not NaN), so the running sum stays finite.
 */
public class ZeroWeightGeneSetTest {
    // a=2, b=1, c=0, d=0, e=0, f=-1, g=-2
    private static RankedList rankedList() {
        return new DefaultRankedList("rl", Arrays.asList("a", "b", "c", "d", "e", "f", "g"),
                new Vector(new float[] { 2f, 1f, 0f, 0f, 0f, -1f, -2f }));
    }

    private static final GeneSet ZERO_SET = new GeneSet("zeros", new String[] { "c", "d", "e" });

    @Test
    public void weightedTablesGiveEqualHitWeightsWhenTheTotalWeightIsZero() {
        RankedList rl = rankedList();
        GeneSetScoringTable[] tables = new GeneSetScoringTable[] {
                new GeneSetScoringTables.Weighted(), new GeneSetScoringTables.WeightedSquared(),
                new GeneSetScoringTables.WeightedOnePointFive() };
        for (GeneSetScoringTable scheme : tables) {
            GeneSetScoringTable table = scheme.createTable(ZERO_SET, rl, rl);
            for (String member : new String[] { "c", "d", "e" }) {
                assertEquals(1.0 / 3, table.getHitScore(member), 1e-6, scheme.getName() + " " + member);
            }
        }
    }

    @Test
    public void enrichmentScoreOfAZeroWeightSetIsFiniteAndEqualsTheClassicScore() {
        RankedList rl = rankedList();
        GeneSet[] gsets = new GeneSet[] { ZERO_SET };
        KSCore core = new KSCore();
        GeneSetCohort.Generator weighted = new GeneSetCohort.Generator(new GeneSetScoringTables.Weighted(), 1, 100);
        GeneSetCohort.Generator classic = new GeneSetCohort.Generator(new GeneSetScoringTables.Classic(), 1, 100);
        EnrichmentScore esWeighted = core.calculateKSScore(weighted.createGeneSetCohort(rl, gsets, true), false)[0];
        EnrichmentScore esClassic = core.calculateKSScore(classic.createGeneSetCohort(rl, gsets, true), false)[0];
        assertTrue(Float.isFinite(esWeighted.getES()));
        // hits at ranks 2, 3, 4 of 7: running sum -1/4, -1/2, then +1/3 per hit: max deviation +1/2 at rank 4
        assertEquals(0.5, esClassic.getES(), 1e-6);
        assertEquals(esClassic.getES(), esWeighted.getES(), 1e-6);
        assertEquals(esClassic.getRankAtES(), esWeighted.getRankAtES());
    }
}
