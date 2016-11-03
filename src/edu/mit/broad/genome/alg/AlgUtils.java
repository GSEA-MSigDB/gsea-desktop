/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg;

import java.util.HashSet;
import java.util.Set;

import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.RankedList;

/**
 * Several alg related utility methods. Dumping ground for ones that fit
 * well in other places.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class AlgUtils {

    /**
     * Privatized class constructor to prevent instantiation.
     */
    private AlgUtils() {
    }

    public static int unionAllCount(final GeneSet[] gsets) {

        Set all = new HashSet();
        for (int f = 0; f < gsets.length; f++) {
            for (int i = 0; i < gsets[f].getNumMembers(); i++) {
                all.add(gsets[f].getMember(i));
            }
        }

        return all.size();
    }

    public static int getNumOfMembers(final Dataset ds, final GeneSet gs) {

        int ntrue = 0;

        for (int i = 0; i < ds.getNumRow(); i++) {
            if (gs.isMember(ds.getRowName(i))) {
                ntrue++;
            }
        }

        return ntrue;
    }

    public static int getNumOfMembers(final RankedList rl, final GeneSet gs) {

        int ntrue = 0;

        for (int r = 0; r < rl.getSize(); r++) {
            if (gs.isMember(rl.getRankName(r))) {
                ntrue++;
            }
        }

        return ntrue;
    }

    public static int intersectSize(final GeneSet a, final GeneSet b) {

        Set common = new HashSet();

        for (int i = 0; i < a.getNumMembers(); i++) {
            String member = a.getMember(i);
            if (b.isMember(member)) {
                common.add(member);
            }
        }

        return common.size();
    }

}    // End AlgUtils
