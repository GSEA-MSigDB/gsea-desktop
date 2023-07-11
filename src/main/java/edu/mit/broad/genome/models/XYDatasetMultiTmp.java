/*
 * Copyright (c) 2003-2023 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.models;

import edu.mit.broad.genome.MismatchedSizeException;
import edu.mit.broad.genome.NotImplementedException;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.math.XYVector;

import org.jfree.data.DomainOrder;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.xy.XYDataset;

public class XYDatasetMultiTmp implements XYDataset {

    private DatasetGroup fGroup;

    private final String[] fSeriesNames;

    private XYVector[] fXYVectors;

    /**
     * Class constructor
     *
     * @param seriesNames
     * @param sharedX
     * @param yss
     */
    public XYDatasetMultiTmp(final String[] seriesNames, final Vector sharedX, final Vector[] yss) {

        for (int i = 0; i < yss.length; i++) {
            if (yss[0].getSize() != yss[i].getSize()) {
                throw new MismatchedSizeException(yss[0], yss[i], i);
            }
        }

        this.fGroup = new DatasetGroup();
        this.fSeriesNames = seriesNames;
        this.fXYVectors = new XYVector[yss.length];
        for (int i = 0; i < yss.length; i++) {
            fXYVectors[i] = new XYVector(sharedX, yss[i]);
        }
    }

    public int indexOf(Comparable seriesKey) {
        if (fSeriesNames == null || seriesKey == null) return -1;
        
        for (int i = 0; i < fSeriesNames.length; i++) {
            if (fSeriesNames[i] != null && fSeriesNames[i].equals(seriesKey)) return i;
        }
        
        return -1;
    }

    public Comparable getSeriesKey(int series) {
        if (fSeriesNames == null) throw new IllegalStateException("Dataset has no series");

        return fSeriesNames[series];
    }


    public DatasetGroup getGroup() {
        return fGroup;
    }

    public void setGroup(DatasetGroup g) {
        this.fGroup = g;
    }

    public double getYValue(int series, int item) {
        XYVector xy = fXYVectors[series];
        return xy.y.getElement(item);
    }

    public double getXValue(int series, int item) {
        XYVector xy = fXYVectors[series];
        return xy.x.getElement(item);
    }

    public int getSeriesCount() {
        return fSeriesNames.length;
    }

    public int getItemCount(int series) {
        XYVector xy = fXYVectors[series];
        return xy.x.getSize();
    }

    public String getSeriesName(int series) {
        return fSeriesNames[series];
    }

    public Number getX(int i, int i1) {
        throw new NotImplementedException();
    }

    public Number getY(int i, int i1) {
        throw new NotImplementedException();
    }

    public void addChangeListener(DatasetChangeListener listener) {
    }

    public void removeChangeListener(DatasetChangeListener listener) {
    }

    public DomainOrder getDomainOrder() {
        return DomainOrder.NONE;
    }
}
