/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.charts;

import java.io.File;
import java.io.IOException;

import org.genepattern.io.ImageUtil;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.data.xy.XYDataset;

import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.models.XYDatasetMultiTmp;
import edu.mit.broad.genome.models.XYDatasetProxy2;

/**
 * Methods to easily create commonly used plot types.
 * Not meant to be very customizable - meant as:
 * <p/>
 * 1) quickie charts to use when
 * developing / investigating new visualizations.
 * 2) Underlies the cannedcharts functionality - works of simple datatstructures and not
 * all the rich objects that algs etc produce.
 * <p/>
 * IMP: This produces JFreeChart and similar chart objects - it should NOT produce XCharts
 * as XCharts are usually customizable for name, title, caption etc.
 * <p/>
 * <p/>
 * <p/>
 * Notes
 * <p/>
 * 1) 1 plot, 2 vertical y axis: http://serveurwind.cci-brest.fr/index.jsp
 * http://www.object-refinery.com/phorum-3.3.2a/read.php?f=2&i=7430&t=7430
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class XChartUtils {

    /**
     * Privatized Class Constructor.
     */
    private XChartUtils() {
    }

    public static JFreeChart scatterOneXManyY(final String comboTitle,
                                              final String[] ytitles,
                                              final String xaxisTitle,
                                              final String yaxisTitle,
                                              final Vector xcommon,
                                              final Vector[] yss) {

        XYDataset data = new XYDatasetMultiTmp(ytitles, xcommon, yss);
        return ChartFactory.createScatterPlot(comboTitle, xaxisTitle,
                yaxisTitle, data,
                PlotOrientation.VERTICAL,
                false, false, false);
    }

    /**
     * @param title
     * @param xaxistitle
     * @param yaxistitle
     * @param y
     * @return
     */
    public static XYPlot lineYHits(final String xaxistitle,
                                   final String yaxistitle,
                                   final String seriesName,
                                   final Vector y) {

        XYDataset data = new XYDatasetProxy2(y, seriesName);

        NumberAxis xAxis = new NumberAxis(xaxistitle);
        xAxis.setAutoRangeIncludesZero(false); // huh
        NumberAxis yAxis = new NumberAxis(yaxistitle);

        yAxis.setTickMarksVisible(false);
        yAxis.setTickLabelsVisible(true);
        //yAxis.setVisible(false);
        StandardXYItemRenderer rend = new StandardXYItemRenderer(StandardXYItemRenderer.DISCONTINUOUS_LINES);

        //StandardXYItemRenderer rend = new MyRend();
        return new XYPlot(data, xAxis, yAxis, rend);
    }

    public static JFreeChart createHistogram(final String title,
                                             final boolean showlegend,
                                             final String categoryAxisLabel,
                                             final String valueAxisLabel,
                                             final Vector v,
                                             final boolean onlyLines,
                                             final int numBins,
                                             final HistogramType htype) {

        return createHistogram(title, showlegend, categoryAxisLabel, valueAxisLabel, new String[]{valueAxisLabel}, new Vector[]{v}, onlyLines, numBins, htype);
    }

    public static JFreeChart createHistogram(final String title,
                                             final boolean showlegend,
                                             final String categoryAxisLabel,
                                             final String valueAxisLabel,
                                             final String[] vnames,
                                             final Vector[] vss,
                                             final boolean onlyLines,
                                             final int numBins,
                                             final HistogramType htype) {

        float min = Float.NaN, max = Float.NaN;
        final HistogramDataset hds = new HistogramDataset();
        boolean tooltips = true;
        boolean urls = true;

        for (int r = 0; r < vnames.length; r++) {
            if (Float.isNaN(min)) {
                hds.addSeries(vnames[r], vss[r].toArrayDouble(), numBins);
            } else {
                hds.addSeries(vnames[r], vss[r].toArrayDouble(), numBins, min, max);
            }
        }


        hds.setType(htype);
        JFreeChart chart;

        if (onlyLines) {
            chart = ChartFactory.createXYLineChart(title, categoryAxisLabel,
                    valueAxisLabel, hds,
                    PlotOrientation.VERTICAL, showlegend, tooltips, urls);

        } else {
            chart = ChartFactory.createHistogram(title,
                    "fooX",
                    "fooY",
                    hds,
                    PlotOrientation.VERTICAL,
                    true,
                    false,
                    false);
        }

        chart.getXYPlot().setForegroundAlpha(0.75f);
        return chart;
    }

    public static final void saveAsSVG(XChart xChart, File toFile, int width, int height, boolean gZip) throws IOException {
        ImageUtil.saveAsSVG(xChart.getFreeChart(), toFile, width, height, gZip);
    }
} // End class XChartFactory
