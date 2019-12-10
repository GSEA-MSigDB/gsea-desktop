/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.MismatchedSizeException;
import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.strucs.DefaultMetricWeightStruc;
import edu.mit.broad.genome.parsers.AuxUtils;
import gnu.trove.*;

import java.util.*;

/**
 * A kind of Vector in which labels can be associated with elements
 * i.e Vector <-> LabelledVector relationship is similar to the
 * Matrix <-> Dataset relationship
 * <p/>
 * (ofcourse, much simpler -- simply row labels)
 * <p/>
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class LabelledVector extends AbstractObject {

    private List<String> fLabels;

    private Vector fVector;

    /**
     * Class constructor
     *
     * @param rl
     * @param shareRowNames
     */
    public LabelledVector(final RankedList rl) {
        init(rl.getName(), rl.getRankedNames(), rl.getScoresV(true));
    }

    /**
     * Class constructor
     *
     * @param name
     * @param labels
     * @param floats
     */
    public LabelledVector(final String name, final String[] labels, final float[] floats) {
        init(name, labels, new Vector(floats));
    }

    /**
     * Class constructor
     *
     * @param name
     * @param labels
     * @param floats
     */
    public LabelledVector(final String name, final String[] labels, final Vector floats) {
        init(name, labels, floats);
    }

    /**
     * Class constructor
     *
     * @param labels
     * @param v
     * @param shareLabels
     */
    public LabelledVector(String name, final List<String> labels, final Vector v) {
    	this.init(name, labels, v);
    }

    /**
     * Class constructor
     *
     * @param labels
     * @param v
     * @param shareLabels
     */
    public LabelledVector(final List<String> labels, final Vector v) {
    	this.init(labels, v);
    }

    /**
     * Class constructor
     * labels must be of same length as the vector
     *
     * @param labels
     * @param v
     * @note vector used directly -- no copy made
     */
    public LabelledVector(final String[] labels, final Vector v) {
        init(labels, v);
    }

    /**
     * Class constructor
     *
     * @param v
     */
    public LabelledVector(String labelPrefix, final Vector v) {
        if (v == null) {
            throw new IllegalArgumentException("Parameter v cannot be null");
        }

        if (labelPrefix == null) {
            labelPrefix = "";
        }

        String[] labels = new String[v.getSize()];
        for (int i = 0; i < v.getSize(); i++) {
            labels[i] = labelPrefix + i;
        }

        init(labels, v);
    }

    private void init(final String[] alabels, final Vector v) {
        init(null, alabels, v);
    }

    // common (pre)initialization routine
    // see below too
    private void init(final String name, final String[] alabels, final Vector v) {

        if (v == null) {
            throw new IllegalArgumentException("Parameter v cannot be null");
        }

        if (alabels == null) {
            throw new IllegalArgumentException("Param alabels cannot be null");
        }

        if (v.getSize() != alabels.length) {
            throw new MismatchedSizeException("Vector", v.getSize(), "Labels", +alabels.length);
        }

        List<String> labels = new ArrayList<String>(alabels.length);
        for (int i = 0; i < alabels.length; i++) {
            labels.add(alabels[i]);
        }

        init(name, labels, v);

    }

    private void init(final List<String> labels, final Vector v) {
        init(null, labels, v);
    }

    // all duplication (if needed) must be done BEFORE calling this method
    private void init(String name, final List<String> labels, final Vector v) {
        if (v == null) {
            throw new IllegalArgumentException("Param v cannot be null");
        }
        if (labels == null) {
            throw new IllegalArgumentException("Param labels cannot be null");
        }

        if (v.getSize() != labels.size()) {
            throw new MismatchedSizeException("Vector", v.getSize(), "Labels", +labels.size());
        }

        // make sure no duplicates
        // @note this check is expensive so disabled for now

        if (name == null) {
            super.initialize(" " + getClass().hashCode() + System.currentTimeMillis());
        } else {
            super.initialize(name);
        }

        this.fVector = v;
        this.fLabels = labels;
        this.fVector.setImmutable(); // @note imp
    }

    public String getQuickInfo() {
        return Integer.toString(fLabels.size()) + " labels";
    }

    public float getScore(final String label) {
        int elemIndex = _labelIndex(label);
        if (elemIndex == -1) {
            System.err.println("Labels are: \n" + getLabels() + "\n");
            throw new IllegalArgumentException("No match to label: " + label);
        }
        return fVector.getElement(elemIndex);
    }

    public float getScore(final int i) {
        return fVector.getElement(i);
    }

    public String getLabel(final int i) {
        return fLabels.get(i);
    }

    public List<String> getLabels() {
        return Collections.unmodifiableList(fLabels);
    }

    /**
     * Does NOT affect current object
     *
     * @param sort
     * @param order
     * @return
     * @maint ALG method placed in object. Because we often need to use this from within other objects
     * and that causes linkage errors if calling an alg class.
     */
    public RankedList sort(final SortMode sort, final Order order) {
        DoubleElement[] dels = this.toDoubleElements();
        dels = DoubleElement.sort(sort, order, dels);

        List<String> labels = new ArrayList<String>(dels.length);
        Vector v = new Vector(dels.length);
        for (int i = 0; i < dels.length; i++) {
            labels.add(this.getLabel(dels[i].fIndex));
            v.setElement(i, (float) dels[i].fValue);
        }

        return new DefaultRankedList(getName(), labels, v);
    }

    private int _labelIndex(final String label) {

        if (fLabelNameLabelIndexMap == null) {
            cacheRowNameIndex();
        }

        // IMP needed as returns 0 and not -1 on not hits!!
        if (fLabelNameLabelIndexMap.containsKey(label)) {
            return fLabelNameLabelIndexMap.get(label);
        }

        // try harder
        String al = AuxUtils.getAuxNameOnlyNoHash(label);
        if (fLabelNameLabelIndexMap.containsKey(al)) {
            return fLabelNameLabelIndexMap.get(al);
        }

        return -1;
    }

    private TObjectIntHashMap fLabelNameLabelIndexMap;

    private void cacheRowNameIndex() {
        if (fLabelNameLabelIndexMap == null) {
            fLabelNameLabelIndexMap = new TObjectIntHashMap();
            for (int rown = 0; rown < getSize(); rown++) {
                fLabelNameLabelIndexMap.put(getLabel(rown), rown);
            }
        }
    }

    /**
     * IMP IMP we dont make a safe copy when returning unless asked for
     * However, the Vector is IMMutable, so same effect, except quicker.
     *
     * @return
     */
    public Vector getScoresV(boolean clonedCopy) {

        if (clonedCopy) {
            return new Vector(fVector, false);
        } else {
            fVector.setImmutable();
            return fVector;
        }

        //return new Vector(fVector, clonedCopy);
    }

    public int getSize() {
        return fVector.getSize();
    }

    public DoubleElement[] toDoubleElements() {
        DoubleElement[] dels = new DoubleElement[getSize()];
        for (int i = 0; i < getSize(); i++) {
            dels[i] = new DoubleElement(i, getScore(i));
        }

        return dels;
    }

    private MetricWeightStruc ws;

    public MetricWeightStruc getMetricWeightStruc() {
        if (ws == null) {
            ws = new DefaultMetricWeightStruc(null, this);
        }

        return ws;
    }

} // End LabelledVector
