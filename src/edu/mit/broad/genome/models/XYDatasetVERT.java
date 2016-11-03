/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.models;

import edu.mit.broad.genome.NotImplementedException;
import edu.mit.broad.genome.math.Vector;
import gnu.trove.TFloatArrayList;
import org.jfree.data.DomainOrder;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.xy.XYDataset;

/**
 * @author Aravind Subramanian
 */
public class XYDatasetVERT implements XYDataset {

    private TFloatArrayList fXValues;
    private TFloatArrayList fYValues;

    private String fSeriesName;
    private DatasetGroup fGroup;

    /**
     * Class constructor
     *
     * @param v
     * @param seriesname
     */
    public XYDatasetVERT(final Vector v, final String seriesname) {
        this.fXValues = new TFloatArrayList();
        this.fYValues = new TFloatArrayList();

        //System.out.println("vector size: " + v.getSize());

        // for every rank pos add 2 points (x1 y1 x2 y2)

        for (int i = 0; i < v.getSize(); i++) {
            float val = v.getElement(i);

            int x1 = 0;
            int y1 = i;
            int y2 = i;
            float x2 = 0; // (i.e no line)

            fXValues.add(x1);
            fYValues.add(y1);

            if (val != 0) {
                //System.out.println("Hit at x: " + i);
                x2 = val;// @note this is the magic
                fXValues.add(x2);
                fYValues.add(y2);
            }
        }

        this.fSeriesName = seriesname;

        // required
        //super.setSeriesNames(new String[]{seriesname});
    }

    public int indexOf(java.lang.Comparable comparable) {
        return -1;
    }

    ASComparable foo;

    public java.lang.Comparable getSeriesKey(int series) {
        if (foo == null) {
            foo = new ASComparable(getSeriesName(0));
        }

        return foo;
    }

    public String toString() {
        return fSeriesName;
    }

    public String getSeriesName(int series) {
        return fSeriesName;
    }

    public int getItemCount(int series) {
        return fXValues.size();
    }

    public double getXValue(int series, int item) {
        return (double) fXValues.get(item);
    }

    public double getYValue(int series, int item) {
        return (double) fYValues.get(item);
    }

    public DomainOrder getDomainOrder() {
        throw new NotImplementedException();
    }

    public int getSeriesCount() {
        return 1;
    }

    public void addChangeListener(DatasetChangeListener listener) {
    }

    public void removeChangeListener(DatasetChangeListener listener) {
    }

    public DatasetGroup getGroup() {
        return fGroup;
    }

    public void setGroup(DatasetGroup g) {
        this.fGroup = g;
    }

    public Number getX(int i, int i1) {
        throw new NotImplementedException();
    }

    public Number getY(int i, int i1) {
        throw new NotImplementedException();
    }

} // End class XYDatasetProxy2