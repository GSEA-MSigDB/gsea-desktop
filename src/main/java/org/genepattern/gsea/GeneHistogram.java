/*******************************************************************************
 * Copyright (c) 2003-2018 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.gsea;

import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.reports.EnrichmentReports;

import org.jfree.chart.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

/**
 * @author Joshua Gould
 */
public class GeneHistogram extends JPanel {
    private JFreeChart geneHistChart;

    private RankedList featureFrequency;

    private int selectedGeneIndex = -1;

    private ChartPanel geneHistPanel;

    private RankedList scores;

    public GeneHistogram() {
        geneHistChart = ChartFactory.createHistogram("", "Gene",
                "Number Of Gene Sets", null, PlotOrientation.VERTICAL, false,
                false, false);

        XYPlot plot = geneHistChart.getXYPlot();
        plot.getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        plot.setAxisOffset(new RectangleInsets(0,0,0,0));
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeZeroBaselineVisible(false);
        plot.setDomainZeroBaselineVisible(false);

        XYBarRenderer renderer = new XYBarRenderer() {
            public Paint getItemPaint(int series, int item) {
                if (item == selectedGeneIndex) {
                    return Color.YELLOW;
                }

                if (scores != null) {
                    float value = scores.getScore(featureFrequency
                            .getRankName(item));
                    return value > 0 ? Color.RED : Color.BLUE;
                }
                return super.getItemPaint(series, item);
            }
        };
        renderer.setGradientPaintTransformer(null);
        renderer.setBarPainter(new StandardXYBarPainter());
        renderer.setShadowVisible(false);

        plot.setRenderer(renderer);
        plot.getRangeAxis().setAxisLinePaint(Color.GRAY);
        plot.getRangeAxis().setAxisLineStroke(new BasicStroke(1.0f,BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        plot.getRangeAxis().setTickMarkPaint(Color.GRAY);
        plot.getRangeAxis().setTickMarkStroke(new BasicStroke(1.0f));

        geneHistPanel = new ChartPanel(geneHistChart, false, false, false,
                false, false) {
            public String getToolTipText(MouseEvent e) {
                int index = getXIndex(e.getX(), e.getY());
                return index != -1 ? featureFrequency.getRankName(index) : null;
            }
        };
        ToolTipManager.sharedInstance().registerComponent(geneHistPanel);
        geneHistChart.setBackgroundPaint(EnrichmentReports.CHART_FRAME_COLOR);

        ChartMouseListener listener = new ChartMouseListener() {

            public void chartMouseClicked(ChartMouseEvent event) {
                int mouseX = event.getTrigger().getX();
                int mouseY = event.getTrigger().getY();
                selectedGeneIndex = getXIndex(mouseX, mouseY);
            }

            public void chartMouseMoved(ChartMouseEvent event) {
            }

        };

        geneHistPanel.addChartMouseListener(listener);
        geneHistPanel.setMouseZoomable(true, false);
        setLayout(new BorderLayout());
        add(geneHistPanel, BorderLayout.CENTER);
    }

    public ChartPanel getChartPanel() {
        return geneHistPanel;
    }

    public String getGeneName(int index) {
        return featureFrequency.getRankName(index);
    }

    public int getXIndex(int mouseX, int mouseY) {
        Point p1 = new Point(mouseX, mouseY);
        XYPlot plot = (XYPlot) geneHistChart.getPlot();
        Rectangle2D plotArea = geneHistPanel.getScreenDataArea();
        ValueAxis domainAxis = plot.getDomainAxis();
        RectangleEdge domainAxisEdge = plot.getDomainAxisEdge();
        ValueAxis rangeAxis = plot.getRangeAxis();
        RectangleEdge rangeAxisEdge = plot.getRangeAxisEdge();
        double chartX = domainAxis.java2DToValue(p1.getX(), plotArea,
                domainAxisEdge);
        double chartY = rangeAxis.java2DToValue(p1.getY(), plotArea,
                rangeAxisEdge);
        int index = (int) Math.round(chartX);
        if (index < 0) {
            index = 0;
        } else if (index >= featureFrequency.getSize()) {
            index = featureFrequency.getSize() - 1;
        }
        if (chartY <= featureFrequency.getScore(index)) {
            return index;
        }
        return -1;
    }

    void updateGeneHist() {
        XYSeries series = new XYSeries("Histogram");
        XYSeriesCollection coll = new HistogramDataset(0);
        coll.addSeries(series);
        for (int i = 0; i < featureFrequency.getSize(); i++) {
            double score = featureFrequency.getScore(i);
            series.add(i, score);
        }
        SymbolAxis xAxis = new SymbolAxis("Gene", featureFrequency
                .getRankedNamesArray());

        xAxis.setVerticalTickLabels(true);
        xAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 9));
        xAxis.setTickMarkPaint(Color.GRAY);
        xAxis.setTickMarkStroke(new BasicStroke(1.0f));
        xAxis.setAxisLinePaint(Color.GRAY);
        xAxis.setAxisLineStroke(new BasicStroke(1.0f,BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        geneHistChart.getXYPlot().setDomainAxis(xAxis);
        geneHistChart.getXYPlot().setDataset(coll);
    }

    public void setFeatureFrequency(RankedList featureFrequency,
                                    RankedList scores) {
        this.featureFrequency = featureFrequency;
        this.scores = scores;
        updateGeneHist();
    }
}
