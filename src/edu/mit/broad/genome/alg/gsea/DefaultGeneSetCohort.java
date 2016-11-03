/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg.gsea;

import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.RankedList;
import org.apache.log4j.Logger;

/**
 * @author Aravind Subramanian
 */
public class DefaultGeneSetCohort extends AbstractGeneSetCohort {

    private boolean fSilent;

    private GeneSetScoringTable[] fGeneSetScoringTables;

    private GeneSetScoringTable fOrigTable;

    /**
     * @param rl
     * @param gsets
     * @param mode
     * @param silent
     * @param qualifySetsAgainstRankedList
     */
    private DefaultGeneSetCohort(final GeneSetScoringTable scoringTable,
                                 final RankedList rl,
                                 final GeneSet[] gsets,
                                 final GeneToGeneSetMap g2gsetmap,
                                 final boolean silent,
                                 final boolean qualifySetsAgainstRankedList) {

        super.init(rl, gsets, g2gsetmap, silent, qualifySetsAgainstRankedList);

        this.fSilent = silent;
        this.fOrigTable = scoringTable;
        this.fGeneSetScoringTables = new GeneSetScoringTable[fGeneSets.length];

        // populate the table
        for (int g = 0; g < fGeneSets.length; g++) { // @note IMP to use the fgsets and it might have gotten clones
            // compute scoring scheme
            fGeneSetScoringTables[g] = scoringTable.createTable(fGeneSets[g], rl, fOrigTable.getRankedList());
            if (!silent && g >= 500 && g % 500 == 0) {
                System.out.println("GeneSetCohorted_scored: " + (g + 1) + " / " + fGeneSets.length);
            }
        }

    }

    public GeneSetCohort clone(final GeneSet[] gsets, final boolean qualifySetsAgainstRankedList) {
        return new DefaultGeneSetCohort(fOrigTable, getRankedList(), gsets, GeneToGeneSetMap.generateGeneToGenesetMap(gsets), fSilent,
                qualifySetsAgainstRankedList);
    }

    public double getHitPoints(final int gsetNum, final String geneName) {
        return fGeneSetScoringTables[gsetNum].getHitScore(geneName);
    }

    public double getMissPoints(final int gsetNum, final String geneName) {
        return fGeneSetScoringTables[gsetNum].getMissScore(geneName);
    }

    /**
     * Internal class
     */
    public static class Generator implements GeneSetCohortGenerator {

        private Logger log = XLogger.getLogger(Generator.class);

        private boolean fSilent;
        private GeneSetScoringTable fOrigTable;

        public Generator(final GeneSetScoringTable scoringTable, final boolean silent) {
            this.fOrigTable = scoringTable;
            this.fSilent = silent;
        }

        public GeneSetCohort createGeneSetCohort(final RankedList rl,
                                                 final GeneSet[] gsets,
                                                 final boolean qualifySetsAgainstRankedList,
                                                 final boolean realRl) {

            GeneToGeneSetMap g2gsetmap = GeneToGeneSetMap.generateGeneToGenesetMap(gsets);
            return createGeneSetCohort(rl, gsets, g2gsetmap, qualifySetsAgainstRankedList, realRl);
        }

        public GeneSetCohort createGeneSetCohort(final RankedList rl,
                                                 final GeneSet[] gsets,
                                                 final GeneToGeneSetMap g2gsetMap,
                                                 final boolean qualifySetsAgainstRankedList,
                                                 final boolean realRl) { // @todo consider the real stuff

            if (realRl && fOrigTable instanceof GeneSetScoringTables.WeightedDoubleSidedAs) {
                log.warn("### SETTING REAL RL: " + rl.getName());
                ((GeneSetScoringTables.WeightedDoubleSidedAs) fOrigTable).setReal(rl);
            }

            if (realRl && fOrigTable instanceof GeneSetScoringTables.WeightedDoubleSidedAs1) {
                log.warn("### SETTING REAL RL: " + rl.getName());
                ((GeneSetScoringTables.WeightedDoubleSidedAs1) fOrigTable).setReal(rl);
            }

            return new DefaultGeneSetCohort(fOrigTable,
                    rl,
                    gsets,
                    g2gsetMap,
                    fSilent,
                    qualifySetsAgainstRankedList);
        }

    } // End class Generator

} // End class DefaultWeightedGeneSetCohort
