/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.gsea;

import org.jfree.data.xy.XYSeriesCollection;

/**
 * @author Joshua Gould
 */
class HistogramDataset extends XYSeriesCollection {
    private double binWidth;

    public HistogramDataset(double binWidth) {
        this.binWidth = binWidth;
    }

    public Number getStartX(int series, int item) {
        return getX(series, item);
    }

    public Number getEndX(int series, int item) {
        Number n = getX(series, item);
        return new Double(n.doubleValue() + binWidth);
    }
}