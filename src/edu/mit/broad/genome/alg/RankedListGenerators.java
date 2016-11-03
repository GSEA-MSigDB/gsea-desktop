/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg;

import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.objects.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aravind Subramanian
 */
public class RankedListGenerators {

    /**
     * Class constructor
     */
    private RankedListGenerators() {
    }


    /**
     * @param rl
     * @param sort
     * @param order
     * @return
     */
    public static RankedList createBySorting(final RankedList rl, final SortMode sort, final Order order) {
        LabelledVector lv = new LabelledVector(rl, true);
        return lv.sort(sort, order); // creates a completely new LV
    }

    public static RankedList createBySorting(final String name, final String[] names, final float[] scores, final SortMode sort, final Order order) {
        LabelledVector lv = new LabelledVector(name, names, scores);
        return lv.sort(sort, order); // creates a completely new LV
    }

    /**
     * @param iv
     * @param sort
     * @param order
     * @param names
     * @return
     */
    public static RankedList sortByVectorAndGetRankedList(final String name, final Vector iv, final SortMode sort,
                                                          final Order order, final List names) {

        final DoubleElement[] dels = _sort(new AddressedVector(iv, true), sort, order);
        final Vector v = new Vector(dels.length);
        final List clonedLabels = new ArrayList();
        for (int i = 0; i < dels.length; i++) {
            v.setElement(i, (float) dels[i].fValue);
            clonedLabels.add(names.get(dels[i].fIndex));
        }

        if (name == null) {
            return new DefaultRankedList(clonedLabels, v, true, true);
        } else {
            return new DefaultRankedList(name, clonedLabels, v, true, true);
        }
    }

    public static RankedList sortByVectorAndGetRankedList(final Vector iv, final SortMode sort,
                                                          final Order order, final List names) {
        return RankedListGenerators.sortByVectorAndGetRankedList(null, iv, sort, order, names);
    }

    protected static DoubleElement[] _sort(final AddressedVector av, final SortMode sort, final Order order) {
        DoubleElement[] dels = new DoubleElement[av.getSize()];
        for (int i = 0; i < av.getSize(); i++) {
            dels[i] = new DoubleElement(av.getAddress(i), av.getScore(i));
        }

        return DoubleElement.sort(sort, order, dels);
    }


} // End class RankedListGenerators