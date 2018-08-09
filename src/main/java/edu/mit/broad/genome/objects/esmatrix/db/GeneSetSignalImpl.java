/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects.esmatrix.db;

import edu.mit.broad.genome.math.XMath;
import edu.mit.broad.genome.objects.*;
import gnu.trove.TIntArrayList;

import java.util.HashSet;
import java.util.Set;

/**
 * Inner class representing one gene set signal
 */
public class GeneSetSignalImpl implements GeneSetSignal {

    private TIntArrayList fCoreRanks;
    private EnrichmentResult fEr;

    /**
     * Class constructor
     *
     * @param er
     */
    public GeneSetSignalImpl(final EnrichmentResult er) {
        this.fEr = er;
        this.fCoreRanks = new TIntArrayList();
        int[] hitIndices = er.getScore().getHitIndices();
        int coreRank = er.getScore().getRankAtES();
        float es = er.getScore().getES();

        for (int i = 0; i < hitIndices.length; i++) {
            if (XMath.isPositive(es)) {
                if (hitIndices[i] <= coreRank) {
                    fCoreRanks.add(hitIndices[i]);
                }
            } else {
                if (hitIndices[i] >= coreRank) {
                    fCoreRanks.add(hitIndices[i]);
                }
            }
        }

        //System.out.println("# of core ranks: " + fCoreRanks.size() + " core rank: " + coreRank + " hitIndices: " + hitIndices.length);

    }

    public String getName() {
        return fEr.getGeneSetName() + "_signal";
    }

    public int getSize() {
        return fCoreRanks.size();
    }

    private GeneSet fCoreGeneSet; // make lazilly

    public GeneSet getAsGeneSet() {
        if (fCoreGeneSet == null) {
            RankedList rl = fEr.getRankedList();
            final Set set = new HashSet();
            for (int i = 0; i < fCoreRanks.size(); i++) {
                //System.out.println("rank: " + fCoreRanks.get(i));
                set.add(rl.getRankName(fCoreRanks.get(i)));
                //System.out.println(">>>>>>>>> " + Printf.outs(rl));
            }
            this.fCoreGeneSet = new FSet(getName(), set);
        }

        //System.out.println("# members: " + fCoreGeneSet.getNumMembers() + " " + fCoreGeneSet.getName() + " " + fCoreRanks.size());

        return fCoreGeneSet;
    }

    public int getRankAtMax() {

        float es = fEr.getScore().getES();

        if (XMath.isPositive(es)) {
            return fEr.getScore().getRankAtES();
        } else {
            return fEr.getRankedList().getSize() - fEr.getScore().getRankAtES();
        }
    }

    // in the gene set
    public float getTagFraction() {
        return (float) getSize() / (float) fEr.getGeneSet().getNumMembers(); // num of gene
    }

    // in the gene list
    public float getListFraction() {
        return (float) getRankAtMax() / (float) fEr.getRankedList().getSize();
    }

    public float getSignalStrength() {
        float tag = getTagFraction();
        float list = getListFraction();
        float numr = tag / (1.0f - list);
        float N = (float) fEr.getRankedList().getSize();
        float Nh = (float) fEr.getGeneSet().getNumMembers();
        float denr = N / (N - Nh);
        return numr / denr;
    }

} // End class MyGeneSetSignal
