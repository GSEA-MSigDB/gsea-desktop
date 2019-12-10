/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.alg;

import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.objects.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aravind Subramanian
 */
public class RankedListGenerators {

    private RankedListGenerators() {
    }

    /**
     * @param rl
     * @param sort
     * @param order
     * @return
     */
    public static RankedList createBySorting(final RankedList rl, final SortMode sort, final Order order) {
        LabelledVector lv = new LabelledVector(rl);
        return lv.sort(sort, order); // creates a completely new LV
    }

    public static RankedList createBySorting(final String name, final String[] names, final float[] scores, final SortMode sort, final Order order) {
        LabelledVector lv = new LabelledVector(name, names, scores);
        return lv.sort(sort, order); // creates a completely new LV
    }

    public static RankedList sortByVectorAndGetRankedList(final Vector iv, final SortMode sort,
                                                          final Order order, final List<String> names) {
        final DoubleElement[] dels = RankedListGenerators._sort(new AddressedVector(iv, true), sort, order);
		final Vector v = new Vector(dels.length);
		final List<String> clonedLabels = new ArrayList<String>();
		for (int i = 0; i < dels.length; i++) {
		    v.setElement(i, (float) dels[i].fValue);
		    clonedLabels.add(names.get(dels[i].fIndex));
		}
		
		return new DefaultRankedList(null, clonedLabels, v);
    }

    protected static DoubleElement[] _sort(final AddressedVector av, final SortMode sort, final Order order) {
        DoubleElement[] dels = new DoubleElement[av.getSize()];
        for (int i = 0; i < av.getSize(); i++) {
            dels[i] = new DoubleElement(av.getAddress(i), av.getScore(i));
        }

        return DoubleElement.sort(sort, order, dels);
    }
}