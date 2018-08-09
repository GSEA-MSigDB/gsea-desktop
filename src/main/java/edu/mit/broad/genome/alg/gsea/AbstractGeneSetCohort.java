/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg.gsea;

import edu.mit.broad.genome.TraceUtils;
import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.RankedList;
import gnu.trove.THashSet;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * @author Aravind Subramanian
 */
abstract class AbstractGeneSetCohort implements GeneSetCohort {

    protected GeneSet[] fGeneSets;
    private THashSet[] fFastSets; // much much faster that gset.ismember!!!
    private RankedList fRankedList;
    private Map fGeneSetNameGeneSetMap;

    protected Logger log = XLogger.getLogger(AbstractGeneSetCohort.class);

    protected boolean fSilent;

    private GeneToGeneSetMap fGeneToGeneSetMap;

    /**
     * Class constructor
     */
    protected AbstractGeneSetCohort() {
    }

    // @note implementing classes MUST call
    protected void init(final RankedList rl,
                        final GeneSet[] gsets,
                        final GeneToGeneSetMap g2gsetmap,
                        final boolean silent,
                        final boolean qualifySetsAgainstRankedList) {
        if (rl == null) {
            throw new IllegalArgumentException("Parameter rl cannot be null");
        }

        if (gsets == null) {
            throw new IllegalArgumentException("Parameter gsets cannot be null");
        }

        if (gsets.length == 0) {
            log.warn("zero length gsets array: " + gsets.length);
        }

        this.fRankedList = rl;
        this.fGeneSets = new GeneSet[gsets.length];
        this.fFastSets = new THashSet[gsets.length];
        this.fGeneSetNameGeneSetMap = new HashMap();

        this.fSilent = silent;

        boolean atleastonewithits = false; // for a sanity check
        for (int g = 0; g < gsets.length; g++) {

            if (qualifySetsAgainstRankedList) {
                this.fGeneSets[g] = gsets[g].cloneDeep(rl);
            } else { // trust!!
                this.fGeneSets[g] = gsets[g]; // trust that already qualified
            }

            this.fFastSets[g] = new THashSet(fGeneSets[g].getMembers());
            this.fGeneSetNameGeneSetMap.put(fGeneSets[g].getName(), fGeneSets[g]);
            if (!atleastonewithits && fGeneSets[g].getNumMembers() > 0) {
                atleastonewithits = true;
            }

            if (!silent && g >= 500 && g % 500 == 0) {
                System.out.println("GeneSetCohorted: " + (g + 1) + " / " + fGeneSets.length);
            }
        }


        if (!atleastonewithits) {
            System.out.println("rl: " + rl.getName() + " \n" + rl.getRankedNames().subList(0, 10));
            if (gsets.length > 0) {
                System.out.println("gset: " + gsets[0].getMembersS());
            }

            TraceUtils.showTrace();
            log.warn("No hits in the ranked list to any of the gene sets!");
            //throw new IllegalArgumentException("No hits in the ranked list to any of the gene sets!");
        }

        if (g2gsetmap == null) {
            this.fGeneToGeneSetMap = GeneToGeneSetMap.generateGeneToGenesetMap(fGeneSets);
        } else {
            this.fGeneToGeneSetMap = g2gsetmap;
        }

    }

    public int[] genesetIndicesForGene(final String geneName) {
        return fGeneToGeneSetMap.getGenesetIndicesForGene(geneName);
    }

    public boolean isMember(int gsetNum, String name) {
        //return fGeneSets[gsetNum].isMember(name); // @noyte faster??
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

} // End class AbstractGeneSetCohort
