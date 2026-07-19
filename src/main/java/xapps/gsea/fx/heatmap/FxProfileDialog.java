/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.heatmap;

import org.genepattern.data.expr.IExpressionData;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.GPWrappers;
import edu.mit.broad.xbench.core.api.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * FX Profile plot dialog (Swing {@code ProfilePlot} chart model).
 */
public final class FxProfileDialog {

    private FxProfileDialog() {
    }

    public static void show(Dataset dataset, int[] rowIndices) {
        if (dataset == null) {
            Application.getWindowManager().showMessage("No dataset for Profile.");
            return;
        }
        IExpressionData data = GPWrappers.createIExpressionData(dataset);
        JFreeChart chart = ChartFactory.createScatterPlot("", "Column", "Value", null,
                PlotOrientation.VERTICAL, false, false, false);
        XYLineAndShapeRenderer lineRenderer = (XYLineAndShapeRenderer) chart.getXYPlot().getRenderer();
        lineRenderer.setDefaultLinesVisible(true);
        String[] columnNames = new String[data.getColumnCount()];
        for (int j = 0; j < data.getColumnCount(); j++) {
            columnNames[j] = data.getColumnName(j);
        }
        SymbolAxis xAxis = new SymbolAxis("Column", columnNames);
        xAxis.setVerticalTickLabels(true);
        chart.getXYPlot().setDomainAxis(xAxis);

        XYSeriesCollection coll = new XYSeriesCollection();
        int rows = rowIndices != null && rowIndices.length > 0 ? rowIndices.length : data.getRowCount();
        for (int i = 0; i < rows; i++) {
            int index = rowIndices != null && rowIndices.length > 0 ? rowIndices[i] : i;
            XYSeries series = new XYSeries(data.getRowName(index));
            for (int j = 0; j < data.getColumnCount(); j++) {
                series.add(j, data.getValue(index, j));
            }
            coll.addSeries(series);
        }
        chart.getXYPlot().setDataset(coll);
        if (rowIndices != null && rowIndices.length > 0 && rowIndices.length <= 5) {
            LegendTitle legend = new LegendTitle(chart.getPlot());
            legend.setMargin(new RectangleInsets(1.0, 1.0, 1.0, 1.0));
            legend.setBorder(1.0, 1.0, 1.0, 1.0);
            legend.setBackgroundPaint(java.awt.Color.white);
            legend.setPosition(RectangleEdge.BOTTOM);
            chart.clearSubtitles();
            chart.addSubtitle(legend);
        }

        FxChartPane chartPane = new FxChartPane("Profile");
        chartPane.setChart(chart, 800, 480);

        Stage stage = new Stage();
        stage.initModality(Modality.NONE);
        stage.setTitle("Profile");
        Button close = new Button("Close");
        xapps.gsea.fx.FxButtons.styleSecondary(close);
        close.setOnAction(e -> stage.close());
        BorderPane root = new BorderPane(chartPane.getNode());
        HBox bottom = xapps.gsea.fx.FxButtons.row(close);
        bottom.setPadding(new Insets(8));
        root.setBottom(bottom);
        BorderPane.setMargin(root.getBottom(), new Insets(8));
        Scene scene = new Scene(root, 860, 560);
        xapps.gsea.fx.FxTheme.apply(scene);
        stage.setScene(scene);
        stage.show();
    }
}
