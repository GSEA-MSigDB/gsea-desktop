/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.heatmap;

import org.genepattern.data.expr.IExpressionData;

import java.awt.*;

/**
 * @author Joshua Gould
 */
public interface ColorScheme {
    public Color getColor(int row, int column);

    public void setDataset(IExpressionData d);

    public Component getLegend();
}
