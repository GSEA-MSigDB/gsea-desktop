/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.vdb.sampledb.SampleAnnot;
import gnu.trove.TObjectIntHashMap;

import java.io.File;
import java.util.*;

/**
 * Basic implementation of a Dataset.
 * Several forms of constructors to create Datasets.
 * <p/>
 * Data is always duplicated and never shared. Dataset is immutable after creation.
 * This is the chief class that provides access to Dataset objects. Its used everywhere!
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class DefaultDataset extends AbstractObject implements Dataset {

    private Matrix fMatrix;

    // these are Lists and not Sets as we want to be able to maintain the order
    // we however check for duplicates in the constructors

    private List fRowNames;
    private List fColNames;
    private GeneSet rowNamesGeneSet; // lazilly filled

    private Annot fAnn;

    private File dsFile;

    private String fQuickInfo;

    // Not always available
    private APMMatrix fAPMMatrix;

    public APMMatrix getAPMMatrix() {
        return fAPMMatrix;
    }

    /**
     * subclasses MUST call init after using this form of the constructor.
     */
    protected DefaultDataset() {
    }

    /**
     * @param name
     * @param matrix
     * @param rowNames
     * @param colNames
     * @param shareAll
     */
    public DefaultDataset(final String name,
                          final Matrix matrix,
                          final List rowNames,
                          final List colNames,
                          final boolean shareAll,
                          final Annot annOpt) {
        this(name, matrix, rowNames, colNames, shareAll, shareAll, shareAll, annOpt);
    }

    public DefaultDataset(final String name,
                          final Matrix matrix,
                          final List rowNames,
                          final List colNames,
                          final boolean shareAll,
                          final Annot annOpt,
                          final APMMatrix apm) {
        this(name, matrix, rowNames, colNames, shareAll, shareAll, shareAll, annOpt, apm);
        //log.debug("SETTING APM TO: " + apm);
    }

    public DefaultDataset(final String name,
                          final Matrix matrix,
                          final String[] rowNames,
                          final String[] colNames,
                          final boolean shareMatrix,
                          final Annot annOpt) {
        this(name, matrix, toList(rowNames), toList(colNames), shareMatrix, true, true, annOpt);
    }

    /**
     * Class constructor
     *
     * @param name
     * @param matrix
     */
    public DefaultDataset(final String name, final Matrix matrix, final Annot annOpt) {

        List rowNames = new ArrayList(matrix.getNumRow());
        List colNames = new ArrayList(matrix.getNumCol());

        for (int r = 0; r < matrix.getNumRow(); r++) {
            rowNames.add("row_" + r);
        }

        for (int c = 0; c < matrix.getNumCol(); c++) {
            colNames.add("col_" + c);
        }

        init(name, matrix, rowNames, colNames, annOpt);
    }

    public DefaultDataset(final String name,
                          final Matrix matrix,
                          final List rowNames,
                          final List colNames,
                          final boolean shareMatrix,
                          final boolean shareRowNames,
                          final boolean shareColNames,
                          final Annot annOpt) {
        this(name, matrix, rowNames, colNames, shareMatrix, shareRowNames, shareColNames, annOpt, null);
    }

    /**
     * Class Constructor.
     * Specified data is copied over - data is shared only on demand
     * if share is true then Data is NOT cloned, its shared
     *
     * @todo does NOT yet work for a subset of rows/cols <-- ??? not sure what this means
     * see DatasetGen extract method for that impl.
     */
    public DefaultDataset(final String name,
                          final Matrix matrix,
                          final List rowNames,
                          final List colNames,
                          final boolean shareMatrix,
                          final boolean shareRowNames,
                          final boolean shareColNames,
                          final Annot annOpt,
                          final APMMatrix apm) {

        if (matrix == null) {
            throw new IllegalArgumentException("Param matrix cant be null");
        }

        if (rowNames == null) {
            throw new IllegalArgumentException("Param rowNames cant be null");
        }

        if (colNames == null) {
            throw new IllegalArgumentException("Param colNames cant be null");
        }

        Matrix dmatrix;
        List drowNames;
        List dcolNames;

        if (shareMatrix) {
            dmatrix = matrix;
        } else {
            dmatrix = matrix.cloneDeep();
        }

        if (shareRowNames) {
            drowNames = rowNames;
        } else {
            drowNames = new ArrayList(rowNames);
        }

        if (shareColNames) {
            dcolNames = colNames;
        } else {
            dcolNames = new ArrayList(colNames);
        }

        init(name, dmatrix, drowNames, dcolNames, annOpt, apm);

    }

    protected void init(final String dsname,
                        final Matrix matrix,
                        final List rowNames,
                        final List colNames,
                        final Annot annOpt) {
        init(dsname, matrix, rowNames, colNames, annOpt, null);
    }

    /**
     * assumes that when i am called the data has already been duplicated
     * i.e callers within this method are responsible for passing me
     * already duplicated (or manipulated/subsetted) data.
     * <p/>
     */
    protected void init(final String dsName,
                        final Matrix matrix,
                        final List rowNames,
                        final List colNames,
                        final Annot annOpt,
                        final APMMatrix apm) {

        init_rows_and_cols(dsName, rowNames, colNames, annOpt, apm);
        initMatrix(matrix, rowNames, colNames);
    }

    // This is one half of the CORE init (see below)
    private void initMatrix(final Matrix matrix, final List rowNames, final List colNames) {

        if (matrix == null) {
            throw new IllegalArgumentException("Matrix cannot be null");
        }


        if (matrix.getNumRow() != rowNames.size()) {
            throw new IllegalArgumentException("Matrix nrows: " + matrix.getNumRow() + " and rownames: "
                    + rowNames.size() + " do not match in size");
        }

        if (matrix.getNumCol() != colNames.size()) {
            throw new IllegalArgumentException("Matrix ncols: " + matrix.getNumCol() + " and colnames: "
                    + colNames.size() + " do not match in size");
        }

        this.fMatrix = matrix;
        this.fMatrix.setImmutable();    // IMP notice.
    }

    // This is one half of the CORE init (see above)
    protected void init_rows_and_cols(final String dsName,
                                      final List rowNames,
                                      final List colNames,
                                      final Annot annOpt,
                                      final APMMatrix apm) {

        super.initialize(dsName);

        if (rowNames == null) {
            throw new IllegalArgumentException("rowNames cannot be null");
        }

        if (colNames == null) {
            throw new IllegalArgumentException("colNames cannot be null");
        }

        this.fRowNames = rowNames;
        this.fRowNames = Collections.unmodifiableList(fRowNames);
        this.fColNames = colNames;
        this.fColNames = Collections.unmodifiableList(fColNames);

        // annot sanity checks
        if (annOpt != null) {
            FeatureAnnot fa = annOpt.getFeatureAnnot();

            //  fannot can be oversize
            if (fa.getNumFeatures() < this.fRowNames.size()) {
                throw new IllegalArgumentException("Annot features is less than dataset rowNames: " + fa.getNumFeatures() + " " + fRowNames.size());
            }

            if (fa.hasNativeDescriptions() && fRowNames.size() > 1) {
                fa.getNativeDesc(fRowNames.get(0).toString()); // just a check
            }
        }
        this.fAnn = annOpt;

        // data integrity checks: no column names must be duplicated
        // ditto for rows but we avoid doing that as its sloooow
        ensureAllUniqueValues(fColNames);

        this.fAPMMatrix = apm;
    }


    protected static void ensureAllUniqueValues(List list) {
        Set set = new HashSet();
        for (int i = 0; i < list.size(); i++) {
            Object obj = list.get(i);
            if (set.contains(obj)) {
                throw new IllegalArgumentException("Duplicate COL names are NOT allowed in Datasets. The offending entry was: " + obj + " at pos: " + i);
            }
            set.add(obj);
        }

        set.clear();
    }

    //Made lazilly
    public Annot getAnnot() {
        if (fAnn == null) {
            //log.debug("Annotation is null -- will use a default ann " + getName());
            ColorMap.Rows cm = null;
            final FeatureAnnot fann = new FeatureAnnotImpl(getName(), fRowNames, null, cm);
            final SampleAnnot sann = new SampleAnnotImpl(getName(), getColumnNames(), null);
            this.fAnn = new AnnotImpl(fann, sann);
        }

        return fAnn;
    }


    public String getRowName(int rown) {
        return (String) fRowNames.get(rown);
    }

    public List getRowNames() {
        return Collections.unmodifiableList(fRowNames);
    }

    // @todo check impact: trove added Jan 2006
    private TObjectIntHashMap fRowIndexNameHashMap;

    public int getRowIndex(String rowName) {

        if (rowName == null) {
            throw new IllegalArgumentException("rowName cannot be null: " + rowName);
        }

        if (fRowIndexNameHashMap == null) {
            fRowIndexNameHashMap = new TObjectIntHashMap();
            for (int r = 0; r < fRowNames.size(); r++) {
                fRowIndexNameHashMap.put(fRowNames.get(r), r);
            }
        }

        int index = fRowIndexNameHashMap.get(rowName);

        // Theres som confusion over whether missing returns 0 or -1 from Trove
        if (index == 0) {
            if (fRowIndexNameHashMap.containsKey(rowName) == false) {
                return index = -1;
            }
        }

        return index;
    }

    public List getColumnNames() {
        return Collections.unmodifiableList(fColNames);
    }

    public GeneSet getRowNamesGeneSet() {
        if (rowNamesGeneSet == null) {
            rowNamesGeneSet = new FSet(getName(), new HashSet(fRowNames));
        }
        return rowNamesGeneSet;
    }

    public int getColumnIndex(String colName) {
        return fColNames.indexOf(colName);
    }

    public String getColumnName(int coln) {
        return (String) fColNames.get(coln);
    }

    public int getNumRow() {
        return fRowNames.size();
    }

    public int getNumCol() {
        return fColNames.size();
    }

    public Vector getRow(String rowName) {
        return getRow(getRowIndex(rowName));
    }

    public Vector[] getRows(GeneSet gset) {
        Vector[] vss = new Vector[gset.getNumMembers()];
        for (int i = 0; i < gset.getNumMembers(); i++) {
            //System.out.println(">> " + gset.getMember(i));
            vss[i] = getRow(gset.getMember(i));
        }

        return vss;
    }

    public float getElement(int rown, int coln) {
        return _matrix().getElement(rown, coln);
    }

    public Vector getRow(int rown) {
        return _matrix().getRowV(rown);
    }

    public Vector getColumn(int coln) {
        return _matrix().getColumnV(coln);
    }

    public int getDim() {
        return _matrix().getDim();
    }

    /**
     * @return An immutable view of the Matrix
     */
    public Matrix getMatrix() {
        _matrix().setImmutable();
        return _matrix();
    }

    private Matrix _matrix() {
        if (fMatrix == null) {

            if (dsFile == null || !dsFile.exists()) {
                throw new IllegalStateException("ds file for lazy matrix loading is null or missing: " + dsFile);
            }

            try {
                log.debug("LAZY loading dataset from: " + dsFile.getPath());
                // @note the annotation, row names , col names etc are NOT used
                final Dataset ds = ParserFactory.readDataset(dsFile, true, true);
                this.initMatrix(((DefaultDataset) ds).fMatrix, fRowNames, fColNames);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return fMatrix;
    }

    public String getQuickInfo() {
        if (fQuickInfo == null) {
            _setQuickInfo();
        }

        return fQuickInfo;
    }

    private void _setQuickInfo() {

        StringBuffer buf = new StringBuffer().append(getNumRow()).append('x').append(getNumCol());
        if (this.getAnnot() != null && getAnnot().getFeatureAnnot() != null && getAnnot().getSampleAnnot_global() != null) {
            FeatureAnnot fa = getAnnot().getFeatureAnnot();
            SampleAnnot sa = getAnnot().getSampleAnnot_global();

            buf.append(" (ann: ");
            if (fa != null) {
                buf.append(fa.getNumFeatures());
            } else {
                buf.append("na");
            }
            buf.append(",");

            if (sa != null) {
                buf.append(sa.getNumSamples());
            } else {
                buf.append("na");
            }

            buf.append(",");

            if (getAnnot().getChip() != null) {
                buf.append(getAnnot().getChip().getName());
            } else {
                buf.append("chip na");
            }

            buf.append(")");

        }
        fQuickInfo = buf.toString();

    }

    private static List toList(final String[] ss) {
        final List list = new ArrayList(ss.length);
        for (int i = 0; i < ss.length; i++) {
            list.add(ss[i]);
        }

        return list;
    }

}    // End DefaultDataset

/*--- Formatted in Sun Java Convention Style on Fri, Sep 27, '02 ---*/