/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.parsers.ParserFactory;
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

    private List<String> fRowNames;
    private List<String> fColNames;
    private GeneSet rowNamesGeneSet; // lazilly filled

    private Annot fAnn;

    private File dsFile;

    private String fQuickInfo;

    // Not always available
    private APMMatrix fAPMMatrix = null;

    public APMMatrix getAPMMatrix() {
        return fAPMMatrix;
    }

    public DefaultDataset(final String name,
                          final Matrix matrix,
                          final String[] rowNames,
                          final String[] colNames,
                          final Annot annOpt) {
        this(name, matrix, toList(rowNames), toList(colNames), annOpt);
    }

    public DefaultDataset(final String name, final Matrix matrix) {

        int numRow = matrix.getNumRow();
		List<String> rowNames = new ArrayList<String>(numRow);
        int numCol = matrix.getNumCol();
		List<String> colNames = new ArrayList<String>(numCol);

        for (int r = 0; r < numRow; r++) {
            rowNames.add("row_" + r);
        }

        for (int c = 0; c < numCol; c++) {
            colNames.add("col_" + c);
        }
		init_rows_and_cols(name, rowNames, colNames);
		initMatrix(matrix, numRow, numCol);
    }

    public DefaultDataset(final String name,
                          final Matrix matrix,
                          final List<String> rowNames,
                          final List<String> colNames,
                          final Annot annOpt) {
        this(name, matrix, rowNames, colNames, annOpt, null);
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
                          final List<String> rowNames,
                          final List<String> colNames,
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

        init_rows_and_cols(name, rowNames, colNames);
        initAnnot(annOpt);
		initMatrix(matrix, rowNames.size(), colNames.size());

        this.fAPMMatrix = apm;

    }

    private void initMatrix(final Matrix matrix, final int numRow, final int numCol) {

        if (matrix == null) {
            throw new IllegalArgumentException("Matrix cannot be null");
        }


        if (matrix.getNumRow() != numRow) {
            throw new IllegalArgumentException("Matrix nrows: " + matrix.getNumRow() + " and rownames: "
                    + numRow + " do not match in size");
        }

        if (matrix.getNumCol() != numCol) {
            throw new IllegalArgumentException("Matrix ncols: " + matrix.getNumCol() + " and colnames: "
                    + numCol + " do not match in size");
        }

        this.fMatrix = matrix;
        this.fMatrix.setImmutable();    // IMP notice.
    }

    private void init_rows_and_cols(final String dsName,
                                    final List<String> rowNames,
                                    final List<String> colNames) {

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

        // data integrity checks: no column names must be duplicated
        // ditto for rows but we avoid doing that as its sloooow
        ensureAllUniqueValues(fColNames);
    }

	private void initAnnot(final Annot annOpt) {
		// annot sanity checks
        if (annOpt != null) {
            FeatureAnnot fa = annOpt.getFeatureAnnot();

            //  fannot can be oversize
            if (fa.getNumFeatures() < this.fRowNames.size()) {
                throw new IllegalArgumentException("Annot features is less than dataset rowNames: " + fa.getNumFeatures() + " " + fRowNames.size());
            }

            if (fa.hasNativeDescriptions() && fRowNames.size() > 1) {
                fa.getNativeDesc(fRowNames.get(0)); // just a check
            }
        }
        this.fAnn = annOpt;
	}


    private static void ensureAllUniqueValues(List<String> cols) {
        Set<String> set = new HashSet<String>();
        for (int i = 0; i < cols.size(); i++) {
            String item = cols.get(i);
            if (set.contains(item)) {
                throw new IllegalArgumentException("Duplicate COL names are NOT allowed in Datasets. The offending entry was: " + item + " at pos: " + i);
            }
            set.add(item);
        }

        set.clear();
    }

    //Made lazily
    public Annot getAnnot() {
        if (fAnn == null) {
            final FeatureAnnot fann = new FeatureAnnot(getName(), fRowNames, null);
            final SampleAnnot sann = new SampleAnnot(getName(), getColumnNames());
            this.fAnn = new Annot(fann, sann);
        }

        return fAnn;
    }


    public String getRowName(int rown) {
        return (String) fRowNames.get(rown);
    }

    public List<String> getRowNames() {
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

    public List<String> getColumnNames() {
        return Collections.unmodifiableList(fColNames);
    }

    public GeneSet getRowNamesGeneSet() {
        if (rowNamesGeneSet == null) {
            rowNamesGeneSet = new GeneSet(getName(), new HashSet<String>(fRowNames));
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
                this.initMatrix(((DefaultDataset) ds).fMatrix, fRowNames.size(), fColNames.size());

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

        StringBuilder buf = new StringBuilder().append(getNumRow()).append('x').append(getNumCol());
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

    private static List<String> toList(final String[] ss) {
        final List<String> list = new ArrayList<String>(ss.length);
        for (int i = 0; i < ss.length; i++) {
            list.add(ss[i]);
        }

        return list;
    }
}