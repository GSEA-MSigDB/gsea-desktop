/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.math;

import edu.mit.broad.genome.objects.Dataset;
import gnu.trove.TIntObjectHashMap;

/**
 * @author Aravind Subramanian
 */
public class DatasetModed {

    private Dataset fOrigDataset;

    private TIntObjectHashMap fColIndexExtractedVectorMap; // @todo can make it a weak map later if needed

    private ScoreMode fScoreMode;

    private SortMode fSort;
    private Order fOrder;

    public DatasetModed(final Dataset orig,
                        final ScoreMode smode,
                        final SortMode sort,
                        final Order order) {

        if (orig == null) {
            throw new IllegalArgumentException("Param orig cannot be null");
        }

        if (smode == null) {
            throw new IllegalArgumentException("Param smode cannot be null");
        }

        this.fOrigDataset = orig;
        this.fScoreMode = smode;
        this.fColIndexExtractedVectorMap = new TIntObjectHashMap();
        this.fSort = sort;
        this.fOrder = order;
    }

    public int getNumCol() {
        return fOrigDataset.getNumCol();
    }

    public int getDim_orig() {
        return fOrigDataset.getDim();
    }

    // lazilly filled
    // NOTE: there's no point in doing this lazily since it's only used in one place and is populated
    // completely in one pass shortly after.
    // However, the bigger issue here is that the v.sort() call is going to treat NaN as *greater than*
    // every other element, when in fact we want those to be least where this is used.  At least this is
    // How it looks.
    // Probably better to rename this class to make this sort of thing clear.
    // OR: can we take care of it in FdrAlgs._calc_Fdrs_skewed()?  Maybe that's enough?
    public Vector getColumn_sorted(final int col) {
        Object obj = fColIndexExtractedVectorMap.get(col);
        if (obj == null) {
            Vector v = fOrigDataset.getColumn(col);
            v = v.extract(fScoreMode);
            v.sort(fSort, fOrder);
            obj = v;
            fColIndexExtractedVectorMap.put(col, v);
        }

        return (Vector) obj;
    }

}

