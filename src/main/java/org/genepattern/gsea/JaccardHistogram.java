/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.gsea;

import gnu.trove.TFloatIntHashMap;
import gnu.trove.TFloatIntIterator;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIntIterator;
import org.genepattern.uiutil.UIUtil;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import edu.mit.broad.genome.reports.EnrichmentReports;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Joshua Gould
 */
public class JaccardHistogram extends JPanel {
    private double binWidth = 0.02;

    private JFreeChart jaccardChart;

    private TFloatIntHashMap jaccardToOccurrencesMap;

    private ChartPanel chartPanel;

    public JaccardHistogram() {
        setLayout(new BorderLayout());

        jaccardChart = ChartFactory.createHistogram("", "Jaccard",
                "Number of Occurences", null, PlotOrientation.VERTICAL, false,
                true, false);

        chartPanel = new ChartPanel(jaccardChart, false, false, false, false,
                false);

        XYPlot plot = jaccardChart.getXYPlot();
        plot.getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        plot.getRangeAxis().setAxisLinePaint(Color.GRAY);
        plot.getRangeAxis().setAxisLineStroke(new BasicStroke(1.0f,BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        plot.getRangeAxis().setTickMarkPaint(Color.GRAY);
        plot.getRangeAxis().setTickMarkStroke(new BasicStroke(1.0f));
        plot.getDomainAxis().setAxisLinePaint(Color.GRAY);
        plot.getDomainAxis().setAxisLineStroke(new BasicStroke(1.0f,BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        plot.getDomainAxis().setTickMarkPaint(Color.GRAY);
        plot.getDomainAxis().setTickMarkStroke(new BasicStroke(1.0f));
        plot.setAxisOffset(new RectangleInsets(0,0,0,0));
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        ((NumberAxis) plot.getDomainAxis()).setAutoRangeIncludesZero(true);
        XYBarRenderer renderer = (XYBarRenderer)plot.getRenderer();
        renderer.setGradientPaintTransformer(null);
        renderer.setBarPainter(new StandardXYBarPainter());
        renderer.setDrawBarOutline(true);
        renderer.setSeriesOutlinePaint(0, Color.GRAY);

        chartPanel.setMouseZoomable(true, false);
        chartPanel.getChart().setBackgroundPaint(EnrichmentReports.CHART_FRAME_COLOR);
        this.add(chartPanel);

        JLabel binWidthLabel = new JLabel("Bin Width:");
        final JTextField binWidthTextField = new JTextField("" + binWidth, 4);
        JButton btn = new JButton("Update");
        btn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                double d;
                try {
                    d = Double.parseDouble(binWidthTextField.getText().trim());
                    if (d < 0 || d > 1) {
                        UIUtil.showMessageDialog(chartPanel
                                .getTopLevelAncestor(),
                                "Bin width must be between zero and one.");
                        return;
                    }
                    binWidth = d;
                } catch (NumberFormatException e1) {
                    UIUtil.showMessageDialog(chartPanel.getTopLevelAncestor(),
                            "Bin width is not a number.");
                    return;
                }
                updateJaccardHistogram();
            }

        });
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(binWidthLabel);
        bottomPanel.add(binWidthTextField);
        bottomPanel.add(btn);
        this.add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * bins jaccard values
     */
    void updateJaccardHistogram() {
        // the jaccard is the intersection/union
        if (binWidth == 0) {
        } else {
            TIntIntHashMap binNumberToOccurencesMap = new TIntIntHashMap();
            XYSeries series = new XYSeries("");
            XYSeriesCollection coll = new HistogramDataset(binWidth);
            coll.setIntervalWidth(0);
            coll.addSeries(series);
            for (TFloatIntIterator it = jaccardToOccurrencesMap.iterator(); it
                    .hasNext();) {
                it.advance();
                float value = it.key();
                // if(value!=0) System.out.println(value);
                int occurences = it.value();
                int bin = (int) (value / binWidth);
                int priorOccurences = binNumberToOccurencesMap.get(bin);
                binNumberToOccurencesMap.put(bin, occurences + priorOccurences);
            }

            for (TIntIntIterator it = binNumberToOccurencesMap.iterator(); it
                    .hasNext();) {
                it.advance();
                int binNumber = it.key();
                int occurences = it.value();
                double valueForBin = binNumber * binWidth;
                series.add(valueForBin, occurences);
            }
            jaccardChart.getXYPlot().setDataset(coll);
        }
    }

    public void setJaccardToOccurrencesMap(
            TFloatIntHashMap jaccardToOccurrencesMap) {
        this.jaccardToOccurrencesMap = jaccardToOccurrencesMap;
        updateJaccardHistogram();
    }

    public ChartPanel getChartPanel() {
        return chartPanel;
    }

}
