/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.charts;

import edu.mit.broad.genome.JarResources;

import org.jfree.chart.JFreeChart;

import javax.swing.*;

import java.io.File;
import java.io.IOException;

/**
 * simple wrapper Interface
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */


public interface XChart {

    public static final Icon ICON = JarResources.getIcon("Chart.gif");

    // imp that it be protected
    // we dont want to expose jfreechart_test usage
    // but sometimes need to
    //protected JFreeChart getFreeChart() {
    public JFreeChart getFreeChart();

    // Name is NOT the same as Title - name is simple and file name safe. Title is short but can be 'English'.
    public String getName();

    // Usually same as the title of the JFreeChart
    public String getTitle();

    // Caption is a more verbose form of Title
    public String getCaption();

    public void saveAsPNG(File inFile, int width, int height) throws IOException;
    
    public void saveAsSVG(File toFile, int width, int height) throws IOException;
} // End XChart

