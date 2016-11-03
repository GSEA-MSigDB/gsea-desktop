/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.charts;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedRangeXYPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * simple wrapper class
 * <p/>
 * similar to Chart except combo chart
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */

public class XComboRangeChart implements XComboChart {

    private CombinedRangeXYPlot fCombinedXYPlot;

    private XChart fCombinedChart;

    private JFreeChart fCombinedFreeChart;

    private String fComboCaption;

    /**
     * contains Charts as elements
     */
    private java.util.List fSubCharts;

    public XComboRangeChart(final String name,
                            final String comboTitle,
                            final String comboCaption,
                            final String domainAxisLabel,
                            final XChart[] charts,
                            final int[] weights) {

        XYPlot[] xyplots = new XYPlot[charts.length];
        String[] subplotTitles = new String[charts.length];
        for (int i = 0; i < charts.length; i++) {
            Plot plot = charts[i].getFreeChart().getPlot();
            if (plot instanceof XYPlot) {
                xyplots[i] = (XYPlot) plot; // combinedxy plots work off xyplots only
            } else {
                throw new IllegalArgumentException("Only XYPlots allowed. Found: " + plot.getClass() + " at: " + i);
            }

            subplotTitles[i] = charts[i].getName();
        }

        init(name, comboTitle, comboCaption, domainAxisLabel, xyplots, weights);

        for (int i = 0; i < getNumSubPlots(); i++) {
            //log.debug("reusing chart: " + charts[i].getDesc());
            fSubCharts.add(charts[i]); // reuse
        }
    }

    // common initialization routine
    /**
     * shared domain axis
     * doc says cannot use the ChartFactory to make charts - need to use constructors.
     * Each of the subplots has to have a null domain axis (X), since they share the
     * parent plots axis.
     */
    private void init(final String name,
                      final String comboTitle,
                      final String comboCaption,
                      final String rangeAxisLabel,
                      final XYPlot[] xyplots,
                      final int[] weights) {

        if (xyplots == null) {
            throw new IllegalArgumentException("Param xyplots cannot be null");
        }

        this.fCombinedXYPlot = new CombinedRangeXYPlot(new NumberAxis(rangeAxisLabel));

        fCombinedXYPlot.setOrientation(PlotOrientation.VERTICAL); // @note


        for (int i = 0; i < xyplots.length; i++) {
            //xyplots[i].setDomainAxis(null);// needed i think (yes)
            //xyplots[i].setRangeAxes(null);// needed i think (yes)
            fCombinedXYPlot.add(xyplots[i], weights[i]);    // weight - amt of space to give it
        }

        this.fComboCaption = comboCaption;
        this.fCombinedFreeChart = new JFreeChart(comboTitle, JFreeChart.DEFAULT_TITLE_FONT, fCombinedXYPlot, false); // dont make a legend

        this.fCombinedChart = new XChartImpl(name, fComboCaption, fCombinedFreeChart);
        this.fSubCharts = new ArrayList();
    }

    public CombinedRangeXYPlot getCombinedXYPlot() {
        return fCombinedXYPlot;
    }

    public XChart getCombinedChart() {
        return fCombinedChart;
    }

    public int getNumSubPlots() {
        return fCombinedXYPlot.getSubplots().size();
    }

    public void saveAsSVG(File toFile, int width, int height) throws IOException {
        // Always compress to GZ.  This is only called in the context of the reports,
        // so we always want the smallest possible images.
        XChartUtils.saveAsSVG(this.getCombinedChart(), toFile, width, height, true);
    }
} // End Chart


