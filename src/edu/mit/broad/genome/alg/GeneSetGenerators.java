/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.objects.*;

import org.apache.log4j.Logger;

import java.util.*;

/**
 * @author Aravind Subramanian
 */
public class GeneSetGenerators {

    private static Logger klog = XLogger.getLogger(GeneSetGenerators.class);

    /**
     * Privatized class constructor
     * Only static methods
     */
    private GeneSetGenerators() {
    }

    public static GeneSet[] createRandomGeneSetsFixedSize(final int numRndGeneSets,
                                                          final RankedList rl,
                                                          final GeneSet gset,
                                                          final RandomSeedGenerator rst) {

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

            Set members = new HashSet();

            for (int i = 0; i < nmembers; i++) {
                members.add(rl.getRankName(randomrowindices[i]));
            }

            if (members.size() != nmembers) {
                klog.warn("Bad randomization -- repeated rnd members were made members: " + members.size() + " but wanted: " + nmembers);
            }

            rndgsets[g] = new FSet(prefix + "_" + g, members);
        }

        return rndgsets;
    }

    public static GeneSet[] removeGeneSetsSmallerThan(final GeneSet[] gsets, final int cutoff) {

        List list = new ArrayList();

        for (int i = 0; i < gsets.length; i++) {
            if (gsets[i].getNumMembers() < cutoff) {
                // ignore
            } else {
                list.add(gsets[i]);
            }
        }

        return (GeneSet[]) list.toArray(new GeneSet[list.size()]);
    }

    public static GeneSet[] removeGeneSetsSmallerThan(final GeneSet[] gsets, final int cutoff,
                                                      final Object ds_or_rl, final boolean firstQualifyByCloning) {
        if (ds_or_rl instanceof Dataset) {
            return removeGeneSetsSmallerThan(gsets, cutoff, (Dataset) ds_or_rl, firstQualifyByCloning);
        } else {
            return removeGeneSetsSmallerThan(gsets, cutoff, (RankedList) ds_or_rl, firstQualifyByCloning);
        }
    }

    public static GeneSet[] removeGeneSetsSmallerThan(final GeneSet[] gsets, final int cutoff, final RankedList rl, final boolean firstQualifyByCloning) {
        // as an opt first apply the ds less filter
        GeneSet[] ogsets = removeGeneSetsSmallerThan(gsets, cutoff);

        List list = new ArrayList();
        for (int i = 0; i < ogsets.length; i++) {
            GeneSet gset;

            if (firstQualifyByCloning) {
                gset = ogsets[i].cloneDeep(rl);
            } else {
                gset = ogsets[i];
            }

            int num = gset.getNumMembers();
            if (num < cutoff) {
                // ignore
            } else {
                list.add(gset);
            }

            if (i != 0 && i % 500 == 0) {
                System.out.println("Done removeGeneSetsSmallerThan: " + cutoff + " for: " + (i + 1) + " / " + ogsets.length);
            }
        }

        return (GeneSet[]) list.toArray(new GeneSet[list.size()]);
    }

    public static GeneSet[] removeGeneSetsSmallerThan(final GeneSet[] gsets, final int cutoff, final Dataset ds, final boolean firstQualifyByCloning) {
        // as an opt first apply the ds less filter
        GeneSet[] ogsets = removeGeneSetsSmallerThan(gsets, cutoff);

        List list = new ArrayList();
        for (int i = 0; i < ogsets.length; i++) {
            GeneSet gset;

            if (firstQualifyByCloning) {
                gset = ogsets[i].cloneDeep(ds);
            } else {
                gset = ogsets[i];
            }

            int num = gset.getNumMembers();
            if (num < cutoff) {
                // ignore
            } else {
                list.add(gset);
            }

            if (i != 0 && i % 500 == 0) {
                System.out.println("Done removeGeneSetsSmallerThan: " + cutoff + " for: " + (i + 1) + " / " + ogsets.length);
            }
        }

        return (GeneSet[]) list.toArray(new GeneSet[list.size()]);
    }

    public static GeneSet[] removeGeneSetsLargerThan(final GeneSet[] gsets,
                                                     final int cutoff,
                                                     final Object ds_or_rl,
                                                     final boolean firstQualifyByCloning) {
        if (ds_or_rl instanceof Dataset) {
            return removeGeneSetsLargerThan(gsets, cutoff, (Dataset) ds_or_rl, firstQualifyByCloning);
        } else {
            return removeGeneSetsLargerThan(gsets, cutoff, (RankedList) ds_or_rl, firstQualifyByCloning);
        }
    }

    public static GeneSet[] removeGeneSetsLargerThan(final GeneSet[] ogsets,
                                                     final int cutoff,
                                                     final RankedList rl,
                                                     final boolean firstQualifyByCloning) {

        List list = new ArrayList();
        for (int i = 0; i < ogsets.length; i++) {

            GeneSet gset;

            if (firstQualifyByCloning) {
                gset = ogsets[i].cloneDeep(rl);
            } else {
                gset = ogsets[i];
            }

            int num = AlgUtils.getNumOfMembers(rl, gset);
            if (num > cutoff) {
                // ignore
            } else {
                list.add(gset);
            }

            if (i != 0 && i % 500 == 0) {
                System.out.println("Done removeGeneSetsLargerThan " + (i + 1) + " / " + ogsets.length);
            }
        }

        return (GeneSet[]) list.toArray(new GeneSet[list.size()]);
    }

    public static GeneSet[] removeGeneSetsLargerThan(final GeneSet[] ogsets,
                                                     final int cutoff,
                                                     final Dataset ds,
                                                     final boolean firstQualifyByCloning) {

        List list = new ArrayList();
        for (int i = 0; i < ogsets.length; i++) {

            GeneSet gset;

            if (firstQualifyByCloning) {
                gset = ogsets[i].cloneDeep(ds);
            } else {
                gset = ogsets[i];
            }

            int num = AlgUtils.getNumOfMembers(ds, gset);
            if (num > cutoff) {
                // ignore
            } else {
                list.add(gset);
            }

            if (i != 0 && i % 500 == 0) {
                System.out.println("Done removeGeneSetsLargerThan " + (i + 1) + " / " + ogsets.length);
            }
        }

        return (GeneSet[]) list.toArray(new GeneSet[list.size()]);
    }
}