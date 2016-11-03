/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.charts;

import java.io.File;
import java.io.IOException;

/**
 * @author Aravind Subramanian
 */
public interface XComboChart {

    public XChart getCombinedChart();

    public void saveAsSVG(File toFile, int width, int height) throws IOException;
}
