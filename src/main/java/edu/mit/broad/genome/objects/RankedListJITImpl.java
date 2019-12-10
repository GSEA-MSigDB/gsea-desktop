/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.math.ScoreMode;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.parsers.ParserFactory;

import java.io.File;
import java.util.List;

/**
 * @author Aravind Subramanian
 */
public class RankedListJITImpl extends AbstractObject implements RankedList {

    private File fRlFile;

    private RankedList fRl;

    /**
     * Class constructor
     *
     * @param rlFile
     * @param useCache
     */
    public RankedListJITImpl(final File rlFile) {
        if (rlFile == null || rlFile.exists() == false) {
            throw new IllegalArgumentException("rlFile doesnt not exist: " + rlFile);
        }

        super.initialize(rlFile.getName());
        this.fRlFile = rlFile;
    }

    private RankedList _rl() {
        try {
            if (fRl == null) {
                this.fRl = ParserFactory.readRankedList(fRlFile);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return fRl;
    }


    public RankedList cloneShallowRL(String newName) {
        return _rl().cloneShallowRL(newName);
    }

    public String getQuickInfo() {
        return _rl().getQuickInfo();
    }

    public String getRankName(final int rank) {
        return _rl().getRankName(rank);
    }

    public List<String> getRankedNames() {
        return _rl().getRankedNames();
    }

    public String[] getRankedNamesArray() {
        return _rl().getRankedNamesArray();
    }

    public int getRank(final String name) {
        return _rl().getRank(name);
    }

    public Vector getScoresV(final boolean clonedCopy) {
        return _rl().getScoresV(clonedCopy);
    }

    public float getScore(final int rank) {
        return _rl().getScore(rank);
    }

    public float getScore(final String name) {
        return _rl().getScore(name);
    }

    public float[] getScores(final GeneSet gset) {
        return _rl().getScores(gset);

    }

    public RankedList extractRanked(final GeneSet gset) {
        return _rl().extractRanked(gset);
    }

    public int getSize() {
        return _rl().getSize();
    }

    public int getSize(final ScoreMode smode) {
        return _rl().getSize(smode);
    }

    public List<String> getNamesOfUpOrDnXRanks(int topOrBotX, boolean top) {
        return _rl().getNamesOfUpOrDnXRanks(topOrBotX, top);
    }

    public MetricWeightStruc getMetricWeightStruc() {
        return _rl().getMetricWeightStruc();
    }

    public RankedList extractRanked(final ScoreMode smode) {
        return _rl().extractRanked(smode);
    }

} // End class RankedListJITImpl
