/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.alg;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.objects.*;

import org.apache.log4j.Logger;

import java.util.*;

/**
 * @author Aravind Subramanian, David Eby
 */
public class GeneSetGenerators {
    private static Logger klog = Logger.getLogger(GeneSetGenerators.class);

    private GeneSetGenerators() { }

    public static GeneSet[] createRandomGeneSetsFixedSize(final int numRndGeneSets, final RankedList rl, final GeneSet gset, final RandomSeedGenerator rst) {
        String prefix = NamingConventions.removeExtension(gset);

        // Qualify as all members may not be in the dataset
        int nmembers = gset.getNumMembers(rl);
        GeneSet[] rndgsets = new GeneSet[numRndGeneSets];

        for (int g = 0; g < numRndGeneSets; g++) {
            // IMP random from 0 to nrows not nmembers. duh!.
            int[] randomrowindices = XMath.randomlySampleWithoutReplacement(nmembers, rl.getSize(), rst);

            if (randomrowindices.length != nmembers) {
                throw new IllegalStateException("random indices generated: " + randomrowindices.length + " not equal to # members: " + nmembers);
            }

            Set<String> members = new HashSet<String>();
            for (int i = 0; i < nmembers; i++) {
                members.add(rl.getRankName(randomrowindices[i]));
            }

            if (members.size() != nmembers) {
                klog.warn("Bad randomization -- repeated rnd members were made members: " + members.size() + " but wanted: " + nmembers);
            }

            rndgsets[g] = new GeneSet(prefix + "_" + g, members);
        }

        return rndgsets;
    }

    private static GeneSet[] removeGeneSetsSmallerThan(final GeneSet[] gsets, final int cutoff) {
        List<GeneSet> list = new ArrayList<GeneSet>(gsets.length);
        for (int i = 0; i < gsets.length; i++) {
            if (gsets[i].getNumMembers() >= cutoff) { list.add(gsets[i]); }
        }
        return list.toArray(new GeneSet[list.size()]);
    }

    public static GeneSet[] removeGeneSetsSmallerThan(final GeneSet[] gsets, final int cutoff, final RankedList rl) {
        // as an optimization first do a filter on the base-size
        GeneSet[] ogsets = removeGeneSetsSmallerThan(gsets, cutoff);

        List<GeneSet> list = new ArrayList<GeneSet>(ogsets.length);
        for (int i = 0; i < ogsets.length; i++) {
            // TODO: eval for performance.  Should lift this to be caller's responsibility?
            GeneSet gset = ogsets[i].cloneDeep(rl);

            int num = gset.getNumMembers();
            if (num >= cutoff) { list.add(gset); }

            // TODO: eval for performance.  See notes in GeneSetCohort(GeneSetScoringTable, RankedList, GeneSet[], GeneToGeneSetMap)
            if (i != 0 && i % 500 == 0) {
                System.out.println("Done removeGeneSetsSmallerThan: " + cutoff + " for: " + (i + 1) + " / " + ogsets.length);
            }
        }

        return list.toArray(new GeneSet[list.size()]);
    }

    public static GeneSet[] removeGeneSetsLargerThan(final GeneSet[] ogsets, final int cutoff, final RankedList rl) {
        List<GeneSet> list = new ArrayList<GeneSet>(ogsets.length);
        for (int i = 0; i < ogsets.length; i++) {
            // TODO: eval for performance.  Should lift this to be caller's responsibility?
            GeneSet gset = ogsets[i].cloneDeep(rl);

            int num = AlgUtils.getNumOfMembers(rl, gset);
            if (num <= cutoff) { list.add(gset); }

            // TODO: eval for performance.  See notes in GeneSetCohort(GeneSetScoringTable, RankedList, GeneSet[], GeneToGeneSetMap)
            if (i != 0 && i % 500 == 0) {
                System.out.println("Done removeGeneSetsLargerThan " + (i + 1) + " / " + ogsets.length);
            }
        }

        return list.toArray(new GeneSet[list.size()]);
    }
}