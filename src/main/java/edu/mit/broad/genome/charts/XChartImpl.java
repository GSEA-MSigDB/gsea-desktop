/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.charts;

import edu.mit.broad.genome.Constants;

import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;

import java.io.File;
import java.io.IOException;

/**
 * simple wrapper class
 * <p/>
 * requirements
 * <p/>
 * easy setting of "name" of chart (not as complex as the title thing in jfreechart_
 * hides some aspects of jfreechart_test isage from client code
 * <p/>
 * possible to extend later to use some other chartind library in addition to jfreechart_test
 * might later add things like custome sizes and display properties that can have state / user prefs
 *
 * @author Aravind Subramanian, David Eby
 */
public class XChartImpl implements XChart {
    private JFreeChart fFreeChart;
    private String fName;
    private String fCaption;

    public XChartImpl(final String name, final String caption, final JFreeChart chart) {
        init(name, caption, chart);
    }

    public XChartImpl(final String name, final String plotTitle, final String caption, final Plot plot) {
        if (plot == null) {
            throw new IllegalArgumentException("Param chart cannot be null");
        }

        init(name, caption, new JFreeChart(plotTitle, JFreeChart.DEFAULT_TITLE_FONT, plot, false));
    }

    private void init(final String name, final String caption, final JFreeChart jfc) {
        if (jfc == null) {
            throw new IllegalArgumentException("Param jfc cannot be null");
        }

        if (name == null) {
            throw new IllegalArgumentException("Param name cannot be null");
        }

        if (caption == null) {
            this.fCaption = Constants.NA;
            //throw new IllegalArgumentException("Parameter caption cannot be null");
        } else {
            this.fCaption = caption;
        }

        this.fName = name; // must be file name safe
        this.fFreeChart = jfc;
    }

    // imp that it be protected
    // we dont want to expose jfreechart_test usage
    // but sometimes need to
    //protected JFreeChart getFreeChart() {
    public JFreeChart getFreeChart() {
        return fFreeChart;
    }

    // file name safe.
    public String getName() {
        return fName;
    }

    public String getTitle() {
        return fFreeChart.getTitle().getText();
    }

    public String getCaption() {
        return fCaption;
    }

    public void saveAsPNG(File inFile, int width, int height) throws IOException {
        ChartUtils.saveChartAsPNG(inFile, this.fFreeChart, width, height);
    }

    public void saveAsSVG(File toFile, int width, int height) throws IOException {
        // Always compress to GZ.  This is only called in the context of the reports,
        // so we always want the smallest possible images.
        XChartUtils.saveAsSVG(this, toFile, width, height, true);
    }
}
