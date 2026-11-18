/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import java.util.*;

/**
 * @author Aravind Subramanian
 */
class GeneSetMatrixHelper {

    /**
     * Privatized class constructor
     */
    private GeneSetMatrixHelper() {
    }

    /**
     * The number of members in the biggest GeneSet.
     *
     * @return
     */
    static int getMaxMemberCount(final List<GeneSet> genesets) {
        int max = 0;
        for (int i = 0; i < genesets.size(); i++) {
            GeneSet gset = genesets.get(i);
            if (max < gset.getNumMembers()) {
                max = gset.getNumMembers();
            }
        }

        return max;
    }

    /**
     * non-redundant list of names of features across
     * gsets in this GeneSetMatrix
     *
     * @return
     */
    static String[] getAllMemberNames(final List<GeneSet> genesets) {
        Set<String> names = getAllMemberNamesS(genesets);
        return names.toArray(new String[names.size()]);
    }

    // does the real stuff
    static Set<String> getAllMemberNamesS(final List<GeneSet> genesets) {
        Set<String> names = new HashSet<String>();
        for (int i = 0; i < genesets.size(); i++) {
            GeneSet gset = genesets.get(i);
            names.addAll(gset.getMembers());
        }

        return names;
    }

    static String[] getAllMemberNameOccurrences(final List<GeneSet> genesets) {
        List<String> names = new ArrayList<String>();
        for (int i = 0; i < genesets.size(); i++) {
            GeneSet gset = genesets.get(i);
            names.addAll(gset.getMembers());
        }

        return names.toArray(new String[names.size()]);
    }

} // End GeneSetGeneSetMatrixHelper
