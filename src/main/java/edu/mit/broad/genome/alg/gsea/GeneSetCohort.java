/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.alg.gsea;

import edu.mit.broad.genome.TraceUtils;
import edu.mit.broad.genome.alg.GeneSetGenerators;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.RankedList;
import gnu.trove.THashSet;
import xtools.api.param.BadParamException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Defines a collection of gene sets and their associated scoring scheme (weights)
 * @author Aravind Subramanian, David Eby
 */
public class GeneSetCohort {
    private Logger log = Logger.getLogger(GeneSetCohort.class);

    private GeneSet[] fGeneSets;
    private THashSet[] fFastSets; // much much faster that gset.ismember!!!
    private RankedList fRankedList;
    // TODO: remove? Seems to be populated but unused 
    private Map<String, GeneSet> fGeneSetNameGeneSetMap;

    private GeneToGeneSetMap fGeneToGeneSetMap;

    private GeneSetScoringTable[] fGeneSetScoringTables;

    private GeneSetScoringTable fOrigTable;

    private GeneSetCohort(final GeneSetScoringTable scoringTable, final RankedList rl, final GeneSet[] gsets,
    		final GeneToGeneSetMap g2gsetmap) {
        if (rl == null) {
		    throw new IllegalArgumentException("Parameter rl cannot be null");
		}
		if (gsets == null) {
		    throw new IllegalArgumentException("Parameter gsets cannot be null");
		}
		if (gsets.length == 0) {
		    log.warn("zero length gsets array: " + gsets.length);
		}
		
        this.fOrigTable = scoringTable;
		this.fRankedList = rl;
		this.fGeneSets = new GeneSet[gsets.length];
		this.fFastSets = new THashSet[gsets.length];
		this.fGeneSetNameGeneSetMap = new HashMap<String, GeneSet>();
		
		boolean atleastonewithits = false; // for a sanity check
		for (int g1 = 0; g1 < gsets.length; g1++) {
			this.fGeneSets[g1] = gsets[g1]; // trust that already qualified
		
			// TODO: remove trove
		    this.fFastSets[g1] = new THashSet(fGeneSets[g1].getMembers());
		    this.fGeneSetNameGeneSetMap.put(fGeneSets[g1].getName(), fGeneSets[g1]);
		    if (!atleastonewithits && fGeneSets[g1].getNumMembers() > 0) {
		        atleastonewithits = true;
		    }
		
			// TODO: eval for performance.
			// Could use out.print() instead, to avoid String concat.  Could also try to avoid the modulo call:
			//   int nextLogPoint = 500; // outside loop
			//   if (g == nextLogPoint) { // inside loop
			//      // print message
			//      nextLogPoint += 500
			//   }
		    if (g1 >= 500 && g1 % 500 == 0) {
		        System.out.println("GeneSetCohorted: " + (g1 + 1) + " / " + fGeneSets.length);
		    }
		}
		
		
		if (!atleastonewithits) {
		    log.info("rl: " + rl.getName() + " \n" + rl.getRankedNames().subList(0, 10));
		
		    TraceUtils.showTrace();
		    log.warn("No hits in the ranked list to any of the gene sets!");
		}
		
		if (g2gsetmap == null) {
		    this.fGeneToGeneSetMap = GeneToGeneSetMap.generateGeneToGenesetMap(fGeneSets);
		} else {
		    this.fGeneToGeneSetMap = g2gsetmap;
		}
        this.fGeneSetScoringTables = new GeneSetScoringTable[fGeneSets.length];

        // populate the table
        for (int g = 0; g < fGeneSets.length; g++) { // @note IMP to use the fgsets and it might have gotten clones
            // compute scoring scheme
            fGeneSetScoringTables[g] = scoringTable.createTable(fGeneSets[g], rl, fOrigTable.getRankedList());
            
        	// TODO: eval for performance.
        	// Could use out.print() instead, to avoid String concat.  Could also try to avoid the modulo call:
        	//   int nextLogPoint = 500; // outside loop
        	//   if (g == nextLogPoint) { // inside loop
        	//      // print message
        	//      nextLogPoint += 500
        	//   }
            if (g >= 500 && g % 500 == 0) {
                System.out.println("GeneSetCohorted_scored: " + (g + 1) + " / " + fGeneSets.length);
            }
        }
    }

    // @note Justin Guinney's addition
    public int[] genesetIndicesForGene(final String geneName) {
        return fGeneToGeneSetMap.getGenesetIndicesForGene(geneName);
    }

    public boolean isMember(int gsetNum, String name) {
        //return fGeneSets[gsetNum].isMember(name); // @note faster??
        return fFastSets[gsetNum].contains(name);
    }

    public int getNumGeneSets() {
        return fGeneSets.length;
    }

    public int getNumTrue(int gsetNum) {
        return fGeneSets[gsetNum].getNumMembers();
    }

    public int getNumLabels() {
        return fRankedList.getSize();
    }

    public RankedList getRankedList() {
        return fRankedList;
    }

    public GeneSetCohort clone(final GeneSet[] gsets) {
        return new GeneSetCohort(fOrigTable, getRankedList(), gsets, GeneToGeneSetMap.generateGeneToGenesetMap(gsets));
    }

    public double getHitPoints(final int gsetNum, final String geneName) {
        return fGeneSetScoringTables[gsetNum].getHitScore(geneName);
    }

    public double getMissPoints(final int gsetNum, final String geneName) {
        return fGeneSetScoringTables[gsetNum].getMissScore(geneName);
    }

    public static class Generator {
        private Logger log = Logger.getLogger(Generator.class);
        private int geneSetMinSize;
        private int geneSetMaxSize;
        private GeneSetScoringTable origTable;

        public Generator(final GeneSetScoringTable scoringTable, int geneSetMinSize, int geneSetMaxSize) {
            this.origTable = scoringTable;
            this.geneSetMinSize = geneSetMinSize;
            this.geneSetMaxSize = geneSetMaxSize;
        }

        public GeneSetCohort createGeneSetCohort(final RankedList rl, final GeneSet[] gsets, final boolean realRl) {
            GeneToGeneSetMap g2gsetMap = GeneToGeneSetMap.generateGeneToGenesetMap(gsets);
            if (realRl && origTable instanceof GeneSetScoringTables.WeightedDoubleSidedAs) {
                log.warn("### SETTING REAL RL: " + rl.getName());
                ((GeneSetScoringTables.WeightedDoubleSidedAs) origTable).setReal(rl);
            }

            if (realRl && origTable instanceof GeneSetScoringTables.WeightedDoubleSidedAs1) {
                log.warn("### SETTING REAL RL: " + rl.getName());
                ((GeneSetScoringTables.WeightedDoubleSidedAs1) origTable).setReal(rl);
            }

            return new GeneSetCohort(origTable, rl, gsets, g2gsetMap);
        }

        // The magic here is:
        // the ds and the gene sets have to match
        // The ds prior to this call was either collapsed or not collapsed
        // If not collapsed, do nothing at all. If it was collapsed
        // If they dont match (i.e all gsets are 0) using the chip to map the gsets
        // in two ways:
        // 1) from their source format to gene symbols
        public GeneSet[] filterGeneSetsByMembersAndSize(final RankedList rl, GeneSet[] gsets) throws Exception {
            if (geneSetMaxSize < geneSetMinSize) { throw new IllegalArgumentException("Max size cannot be less than min size"); }

            if (geneSetMinSize != geneSetMaxSize) {
                log.info("Got gsets: " + gsets.length + " now preprocessing them ... min: " + geneSetMinSize + " max: " + geneSetMaxSize);
                gsets = GeneSetGenerators.removeGeneSetsSmallerThan(gsets, geneSetMinSize, rl);
                log.info("Done preproc for smaller than: " + geneSetMinSize);
                // TODO: eval for performance: no need to filter by RL here since we already did it in the min filter
                gsets = GeneSetGenerators.removeGeneSetsLargerThan(gsets, geneSetMaxSize, rl);
                log.info("Done preproc for larger than: " + geneSetMaxSize);
           } else { // @note hack
                log.info("Skipped gene set size filtering: max and min thresholds are equal");
            }

            // Finally remove all 0 size gene sets (if min is 0 these will still be in there)
            gsets = removeAllZeroMemberSets(gsets);
            checkForZeroContentGeneSets(gsets);
            log.debug("Done geneset preproc starting analysis ...");
            return gsets;
        }

        private GeneSet[] removeAllZeroMemberSets(final GeneSet[] gsets) {
            // Finally remove all 0 size gene sets (if min is 0 these will still be in there)
            List<GeneSet> list = new ArrayList<GeneSet>();
            for (int i = 0; i < gsets.length; i++) {
                if (gsets[i].getNumMembers() > 0) {
                    list.add(gsets[i]);
                }
            }

            return list.toArray(new GeneSet[list.size()]);
        }
        
        // Check if size is zero or content is zero for all sets
        private void checkForZeroContentGeneSets(final GeneSet[] qual_gsets) {
            if (qual_gsets != null) {
                for (int i = 0; i < qual_gsets.length; i++) {
                    if (qual_gsets[i].getNumMembers() > 0) {
                        return;
                    }
                }
            }
            throw new BadParamException("After pruning, none of the gene sets passed size thresholds.", 1001);
        }
    }
}
