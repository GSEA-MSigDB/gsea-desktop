/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.NotImplementedException;
import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.objects.strucs.DefaultMetricWeightStruc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * A Dataset that is scored and sorted/ordered in some way. The data IS SHARED between original
 * Dataset and (this) (re)ordered Dataset. So efficient for use.
 * (Note: the getMatrix method does NOT return shared data - it duplicates as needed)
 * In addition, the scores that caused the ranking are available through getScore()
 * <p/>
 * IMP IMP IMP: Dataset is NOT sorted -> thats what the indexed vector is for
 * see the sdsrown2posinds method
 *
 * @author Aravind Subramanian, David Eby
 */
public class ScoredDatasetImpl extends AbstractObject implements ScoredDataset {

    private Dataset fDataset;

    private AddressedVector fIndVector;

    private List<String> fRowNamesInSdsOrder;

    private GeneSet fRowNamesGeneSet; // lazilly filled

    /**
     * Class constructor
     *
     * @param iv
     * @param ds
     */
    /**
     * Class Constructor.
     * Dataset data is NOT duplicated.
     * But see note for the getMatrix() method below.
     * The original dataset is not touched in any way.
     * Indexedvector data IS copied
     *
     * @todo hmm the row names are replicated. How to avoid?
     * @todo Hmm to avoid duplicating data we need a sorted matrix.
     * If this is possible to impl at a later stage, look into doing the same with FeatureList too.
     */
    public ScoredDatasetImpl(final AddressedVector iv, final Dataset ds) {
        if (iv == null) {
            throw new NullPointerException("Param AddressedVector cannot be null");
        }
        
        if (ds == null) {
            throw new NullPointerException("Param Dataset cannot be null");
        }
        
        super.initialize(ds.getName());
        
        final int num = iv.getSize();
        if (num < 0) {
            throw new IllegalArgumentException("# features: " + num + " cannot be less than zero");
        }
        
        if (num > ds.getNumRow()) {
            throw new IllegalArgumentException("# sorted elements: " + num
                    + " cannot be > Dataset length: " + ds.getNumRow());
        }
        
        this.fDataset = ds;
        this.fIndVector = iv; //new AddressedVector(num, iv);    // data copied
        this.fRowNamesInSdsOrder = new ArrayList<String>(num);
        
        for (int sdsrown = 0; sdsrown < num; sdsrown++) {
            int posinds = sdsrown2posinds(sdsrown);
            fRowNamesInSdsOrder.add(ds.getRowName(posinds)); // IMP -> note adding converted index order
        }
        
        this.fRowNamesInSdsOrder = Collections.unmodifiableList(fRowNamesInSdsOrder);
    }

    public RankedList cloneShallowRL(final String newName) {
        super.setName(newName);
        return this;
    }

    public Annot getAnnot() {
        return fDataset.getAnnot();
    }

    public Vector getRow(final int sdsrown) {
        return fDataset.getRow(sdsrown2posinds(sdsrown));
    }

    public Vector getRow(final String rowName) {
        return fDataset.getRow(rowName);
    }

    public Vector[] getRows(final GeneSet gset) {
        return fDataset.getRows(gset);
    }
    // vv expensove to implement

    public Vector getColumn(final int coln) {
        throw new NotImplementedException();
        // this is wrong because the order of entries in the column in the dataset is obviously not going to be the same as the order in the rls
        //return fDataset.getColumn(coln);
    }

    public float getElement(final int sdsrown, final int coln) {
        return fDataset.getElement(sdsrown2posinds(sdsrown), coln);
    }

    public String getRowName(final int sdsrown) {
        // dont have to sdsrown2posinds as already done at init for efficiency
        return fRowNamesInSdsOrder.get(sdsrown);
    }

    public List<String> getRankedNames() {
        return Collections.unmodifiableList(fRowNamesInSdsOrder);
    }

    public List<String> getRowNames() {
        return getRankedNames();
    }

    public GeneSet getRowNamesGeneSet() {
        if (fRowNamesGeneSet == null) {
            fRowNamesGeneSet = new GeneSet(getName(), new HashSet<String>(getRankedNames()));
        }
        return fRowNamesGeneSet;
    }

    /**
     * changed to do this automatically on request
     * IMP very very slow for large
     * To avoid that first call cacheRowNameIndex
     * usual perf/memory tradeoffs
     *
     * @param rowName
     * @return
     * @see getRowNameIndexMap()
     * @see cacheRowNameIndex()
     */
    public int getRowIndex(final String rowName) {
        if (fRowNameSdsRowIndexMap == null) { cacheRowNameIndex(); }
        Integer index = fRowNameSdsRowIndexMap.get(rowName);
        if (index == null) { return -1; }
        return index;
    }

    private Map<String, Integer> fRowNameSdsRowIndexMap;

    private void cacheRowNameIndex() {
        if (fRowNameSdsRowIndexMap == null) {
            fRowNameSdsRowIndexMap = new HashMap<String, Integer>();
            int numRow = getNumRow();
            for (int sdsrown = 0; sdsrown < numRow; sdsrown++) {
                fRowNameSdsRowIndexMap.put(getRowName(sdsrown), sdsrown);
            }
        }
    }

    public String[] getRankedNamesArray() {
        return fRowNamesInSdsOrder.toArray(new String[fRowNamesInSdsOrder.size()]);
    }

    public List<String> getColumnNames() {
        return fDataset.getColumnNames();
    }

    public int getColumnIndex(String colName) {
        return fDataset.getColumnIndex(colName);
    }

    public String getColumnName(int coln) {
        return fDataset.getColumnName(coln);
    }

    /**
     * @note IMP this is not necc the same size as ds
     */
    public int getNumRow() {
        return fIndVector.getSize();
    }

    public int getNumCol() {
        return fDataset.getNumCol();
    }

    public int getDim() {
        return fDataset.getDim();
    }

    public String getQuickInfo() {
        StringBuffer buf = new StringBuffer().append(getNumRow()).append('x').append(getNumCol());
        return buf.toString();
    }

    /**
     * A brand new matrix is generated on each call.
     * Memory space is NOT shared.
     * So use with care
     */
    // Hmm to avoid duplicating data we need a sorted matrix - but dont want that.
    public Matrix getMatrix() {
        Matrix matrix = new Matrix(fIndVector.getSize(), fDataset.getNumCol());

        for (int i = 0; i < fIndVector.getSize(); i++) {
            matrix.setRow(i, fDataset.getRow(sdsrown2posinds(i)));
        }

        return matrix;
    }

    // very efficient as direct lookup in an array
    private int sdsrown2posinds(int index) {
        return fIndVector.getAddress(index);
    }

    /**
     * --------------------------------------------------------------
     * <p/>
     * ADDITIONAL SCORING / ORDERING / SORTING RELATED METHODS
     * IN ADDITION TO REGULAR DATASET METHODS
     * <p/>
     * --------------------------------------------------------------
     * return a safe copy of the sort order
     * <p/>
     * <p/>
     * return a safe copy of the sort order
     */

    /**
     * synonym for getRowIndex()
     *
     * @param rowname
     * @return
     */
    public int getRank(String rowname) {
        return getRowIndex(rowname);
    }

    /**
     * score of gene at position i in the ScoredDataset
     *
     * @param posinds
     * @return
     */
    public float getScore(int sdsrown) {
        return fIndVector.getScore(sdsrown);
    }

    public float getScore(String rowName) {
        int sdsrown = getRowIndex(rowName);

        if (sdsrown == -1) {
            throw new IllegalArgumentException("Could not find feat index for: " + sdsrown + " " + rowName);
        }

        return fIndVector.getScore(sdsrown);
    }

    /**
     * @return Immutable Vector of scores
     */
    public Vector getScoresV(boolean clonedCopy) {
        return fIndVector.getScoresV(clonedCopy);
    }

    // RankedList impl
    public int getSize() {
        return getNumRow();
    }

    public int getSize(final ScoreMode smode) {
        return getScoresV(false).getSize(smode);
    }

    // RankedList impl
    public String getRankName(int rank) {
        return getRowName(rank);
    }

    public float[] getScores(final GeneSet gset) {
        float[] scores = new float[gset.getNumMembers()];
        for (int i = 0; i < gset.getNumMembers(); i++) {
            scores[i] = getScore(gset.getMember(i));
        }

        return scores;
    }

    public List<String> getNamesOfUpOrDnXRanks(int topOrBotX, boolean top) {
        return Helper.getLabelsOfUpOrDnXRanks(topOrBotX, top, this);
    }

    public RankedList extractRanked(final ScoreMode smode) {
        return Helper.extractRanked(smode, this);
    }

    public RankedList extractRanked(final GeneSet gset) {
        return RankedList.Helper.extract(gset, this);
    }

    private MetricWeightStruc ws;

    public MetricWeightStruc getMetricWeightStruc() {
        if (ws == null) {
            ws = new DefaultMetricWeightStruc(null, this);
        }

        return ws;
    }
}
