/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.MismatchedSizeException;
import edu.mit.broad.genome.math.ScoreMode;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.strucs.DefaultMetricWeightStruc;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Aravind Subramanian
 */
public class DefaultRankedList extends AbstractObject implements RankedList {

    private List<String> fRankedNames;

    private Vector fRankedScores;

    /**
     * Class constructor
     *
     * @param name
     * @param rankedNames
     * @param rankedScores
     */
    public DefaultRankedList(final String name,
                             final List<String> rankedNames,
                             final Vector rankedScores) {
        this.init(name, rankedNames, new Vector(rankedScores, true));
    }

    public RankedList cloneShallowRL(final String newName) {
        super.setName(newName);
        return this;
    }

    // all duplication (if needed) and SORTING must be done BEFORE calling this method
    // the vector must already be sorted as desired!!
    private void init(final String name, final List<String> rankedNames, final Vector rankedScores) {
        if (rankedScores == null) {
            throw new IllegalArgumentException("Param v cannot be null");
        }
        if (rankedNames == null) {
            throw new IllegalArgumentException("Param labels cannot be null");
        }

        if (rankedScores.getSize() != rankedNames.size()) {
            throw new MismatchedSizeException("Vector", rankedScores.getSize(), "Labels", +rankedNames.size());
        }

        // make sure no duplicates
        // @note this check is expensive so disabled for now

        if (name == null) {
            super.initialize(" " + getClass().hashCode() + System.currentTimeMillis());
        } else {
            super.initialize(name);
        }

        this.fRankedScores = rankedScores;
        this.fRankedNames = Collections.unmodifiableList(rankedNames);
        this.fRankedScores.setImmutable(); // @note imp
    }

    public String getQuickInfo() {
        return getSize() + " names";
    }

    public String getRankName(final int rank) {
        return fRankedNames.get(rank);
    }

    public List<String> getRankedNames() {
        return Collections.unmodifiableList(fRankedNames);
    }

    public String[] getRankedNamesArray() {
        return (String[]) fRankedNames.toArray(new String[fRankedNames.size()]);
    }

    public Vector getScoresV(final boolean clonedCopy) {
        return new Vector(fRankedScores, clonedCopy);
    }

    public float getScore(final int rank) {
        return fRankedScores.getElement(rank);
    }

    public int getRank(final String name) {
        int rank = _index(name);
        return rank;
    }

    public float getScore(final String name) {
        int index = _index(name);

        if (index == -1) {
            throw new IllegalArgumentException("No such name: " + name + " \navailable: " + fRankedNames + "\n but no: " + name);
        }

        return fRankedScores.getElement(index);
    }

    // dont need this as index is same as rank ??
    private int _index(final String rowName) {

        if (fRowNameSdsRowIndexMap == null) {
            cacheRowNameIndex();
        }
        
        Integer index = fRowNameSdsRowIndexMap.get(rowName);

        // IMP needed as returns 0 and not -1 on no hits!!
        if (index == null) {
            if (!fRowNameSdsRowIndexMap.containsKey(rowName)) {
                return -1;
            } else {
                return 0;
            }
        }

        return index;
    }

    private Map<String, Integer> fRowNameSdsRowIndexMap;

    private void cacheRowNameIndex() {
        if (fRowNameSdsRowIndexMap == null) {
            fRowNameSdsRowIndexMap = new HashMap<String, Integer>();
            for (int r = 0; r < getSize(); r++) {
                fRowNameSdsRowIndexMap.put(fRankedNames.get(r), Integer.valueOf(r));
            }
        }
    }

    public float[] getScores(final GeneSet gset) {
        float[] scores = new float[gset.getNumMembers()];
        for (int i = 0; i < gset.getNumMembers(); i++) {
            scores[i] = getScore(gset.getMember(i));
        }

        return scores;
    }

    public RankedList extractRanked(final GeneSet gset) {
        return RankedList.Helper.extract(gset, this);
    }

    public int getSize() {
        return fRankedScores.getSize();
    }

    public int getSize(final ScoreMode smode) {
        return fRankedScores.getSize(smode);
    }

    public List<String> getNamesOfUpOrDnXRanks(int topOrBotX, boolean top) {
        return Helper.getLabelsOfUpOrDnXRanks(topOrBotX, top, this);
    }

    public RankedList extractRanked(final ScoreMode smode) {
        return Helper.extractRanked(smode, this);
    }

    private MetricWeightStruc mws;

    public MetricWeightStruc getMetricWeightStruc() {
        if (mws == null) {
            mws = new DefaultMetricWeightStruc(null, this);
        }

        return mws;
    }


} // End DefaultRankedList
