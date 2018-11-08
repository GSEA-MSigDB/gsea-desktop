/*******************************************************************************
 * Copyright (c) 2003-2018 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.plot;

import org.genepattern.data.expr.IExpressionData;
import org.genepattern.menu.jfree.JFreeMenuBar;
import org.genepattern.uiutil.CenteredDialog;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class ProfilePlot {
    private static final void createLegend(JFreeChart jfree) {
        Plot plot = jfree.getPlot();
        LegendTitle legend = new LegendTitle(plot);
        legend.setMargin(new RectangleInsets(1.0, 1.0, 1.0, 1.0));
        Rectangle2D bounds = legend.getBounds();
        legend.setBorder(1.0, 1.0, 1.0, 1.0);
        legend.setBackgroundPaint(Color.white);
        legend.setPosition(RectangleEdge.BOTTOM);
        jfree.clearSubtitles();
        jfree.addSubtitle(legend);
    
    }

    private IExpressionData expressionData;

    private JFreeChart chart;

    private ChartPanel chartPanel;

    private JDialog dialog;

    /**
     * Creates a new instance
     *
     * @param parent         the dialog parent
     * @param expressionData the expression data
     */
    public ProfilePlot(Frame parent, IExpressionData expressionData) {
        this.expressionData = expressionData;
        chart = ChartFactory.createScatterPlot("",
                // title
                "Column",
                // x-axis label
                "Value",
                // y-axis label
                null,
                // data
                PlotOrientation.VERTICAL, false,
                // create legend?
                false,
                // generate tooltips?
                false
                // generate URLs?
        );

        chartPanel = new ChartPanel(chart, false, false, false, false, false);
        chartPanel.setMouseZoomable(true, false);
        XYLineAndShapeRenderer lineRenderer = (XYLineAndShapeRenderer) chart
                .getXYPlot().getRenderer();
        lineRenderer.setDefaultLinesVisible(true);
        String[] columnNames = new String[expressionData.getColumnCount()];
        for (int j = 0, columns = expressionData.getColumnCount(); j < columns; j++) {
            columnNames[j] = expressionData.getColumnName(j);
        }
        SymbolAxis xAxis = new SymbolAxis("Column", columnNames);
        xAxis.setVerticalTickLabels(true);
        chart.getXYPlot().setDomainAxis(xAxis);
        // Set a dialog for display unless we have no parent Frame, as when we are running
        // from the command-line and batch-generating reports
        if (parent != null) {
            dialog = new CenteredDialog(parent);
            dialog.setTitle("Profile");
            dialog.getContentPane().add(chartPanel);
            dialog.setJMenuBar(new JFreeMenuBar(chartPanel, parent));
        }
    }

    public void plot(int[] rowIndices) {
        XYSeriesCollection coll = new XYSeriesCollection();

        int rows = rowIndices != null ? rowIndices.length : expressionData
                .getRowCount();
        for (int i = 0; i < rows; i++) {
            int index = rowIndices != null ? rowIndices[i] : i;
            XYSeries series = new XYSeries(expressionData.getRowName(index));
            for (int j = 0; j < expressionData.getColumnCount(); j++) {
                series.add(j, expressionData.getValue(index, j));
            }
            coll.addSeries(series);
        }

        chart.getXYPlot().setDataset(coll);
        if (rowIndices != null && rowIndices.length <= 5) { 
            // only show legend if showing 5 genes or less
            ProfilePlot.createLegend(chart);
        } else {
            chart.clearSubtitles();
        }

        Thread t = new Thread() {
            public void run() {
                dialog.pack();
                dialog.setVisible(true);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            t.run();
        } else {
            SwingUtilities.invokeLater(t);
        }
    }
}
