/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.models;

import edu.mit.broad.genome.NotImplementedException;
import edu.mit.broad.genome.math.Vector;
import org.jfree.data.DomainOrder;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.xy.XYDataset;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aravind Subramanian
 */
public class XYDatasetProxy2 implements XYDataset {

    private final List fXValues;
    private final List fYValues;

    private final String fSeriesName;
    private DatasetGroup fGroup;

    private boolean fFlatYAxis;


    public XYDatasetProxy2(final Vector v, final String seriesname) {
        this(v, seriesname, false);
    }

    /**
     * Class constructor
     *
     * @param v
     * @param seriesname
     */
    public XYDatasetProxy2(final Vector v, final String seriesname, final boolean flatYAxis) {
        this.fFlatYAxis = flatYAxis;
        this.fXValues = new ArrayList();
        this.fYValues = new ArrayList();

        //System.out.println("vector size: " + v.getSize());

        for (int i = 0; i < v.getSize(); i++) {
            float val = v.getElement(i);
            //System.out.println(">> " + val);

            fXValues.add(new Integer(i));
            if (!flatYAxis) {
                fYValues.add(new Float(val));
            }
            // add an x whose value is same as x but y is 0 // @note this is the magic
            if (val != 0) {
                //klog.debug("Hit at x: " + i);
                fXValues.add(new Integer(i));
                if (!flatYAxis) {
                    fYValues.add(new Float(0));
                }
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
        return ((Integer) fXValues.get(item)).doubleValue();
    }

    public double getYValue(int series, int item) {
        if (fFlatYAxis) {
            return 4.0f;
        } else {
            return ((Float) fYValues.get(item)).doubleValue();
        }
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

