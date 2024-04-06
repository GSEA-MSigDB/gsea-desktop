/*
 * Copyright (c) 2003-2024 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.menu.jfree;

import org.genepattern.menu.PlotAction;
import org.genepattern.menu.ViewMenu;
import org.genepattern.uiutil.CenteredDialog;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.editor.ChartEditor;
import org.jfree.chart.editor.ChartEditorManager;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * @author Joshua Gould
 */
public class JFreeViewMenu extends ViewMenu {

    protected PlotAction[] createPlotActions() {
        return new PlotAction[]{new JFreeResetPlotAction("Reset"),
                new ZoomInAction(), new ZoomOutAction(),
                new PlotOptionsAction()};
    }

    public JFreeViewMenu(JComponent plot, Frame parent) {
        super(plot, parent);
    }

    public static void zoomIn(ChartPanel chartPanel) {
        JFreeChart chart = chartPanel.getChart();
        ValueAxis xAxis = chart.getXYPlot().getDomainAxis();
        ValueAxis yAxis = chart.getXYPlot().getRangeAxis();
        double xmin = xAxis.getLowerBound();
        double xmax = xAxis.getUpperBound();
        double ymin = yAxis.getLowerBound();
        double ymax = yAxis.getUpperBound();
        double deltax = Math.abs(xmax - xmin);
        double deltay = Math.abs(ymax - ymin);
        xAxis.setRange(xmin + deltax * .1, xmax - deltax * .1);
        yAxis.setRange(ymin + deltay * .1, ymax - deltay * .1);
    }

    public static void zoomOut(ChartPanel chartPanel) {
        JFreeChart chart = chartPanel.getChart();
        ValueAxis xAxis = chart.getXYPlot().getDomainAxis();
        ValueAxis yAxis = chart.getXYPlot().getRangeAxis();
        double xmin = xAxis.getLowerBound();
        double xmax = xAxis.getUpperBound();
        double ymin = yAxis.getLowerBound();
        double ymax = yAxis.getUpperBound();
        double deltax = Math.abs(xmax - xmin);
        double deltay = Math.abs(ymax - ymin);
        xAxis.setRange(xmin - deltax * .1, xmax + deltax * .1);
        yAxis.setRange(ymin - deltay * .1, ymax + deltay * .1);
    }

    public static JDialog showPlotOptionsDialog(final ChartPanel chartPanel, Frame parent) {
        JFreeChart chart = chartPanel.getChart();
        final ChartEditor panel = ChartEditorManager.getChartEditor(chart);
        final JDialog dialog = new CenteredDialog(parent);
        dialog.setTitle("Display Options");
        JButton cancelButton = new JButton("Cancel");
        final JButton okButton = new JButton("OK");
        ActionListener l = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == okButton) {
                    panel.updateChart(chartPanel.getChart());
                }
                dialog.dispose();
            }
        };
        cancelButton.addActionListener(l);
        okButton.addActionListener(l);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        dialog.getContentPane().add((Component) panel);
        dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setVisible(true);
        return dialog;
    }

    class PlotOptionsAction extends PlotAction {
        JDialog dialog;

        public PlotOptionsAction() {
            super("Display Options...");
        }

        private void createDialog() {
            final ChartPanel chartPanel = (ChartPanel) getPlot();
            dialog = showPlotOptionsDialog(chartPanel, getFrame());
        }

        public void actionPerformed(java.awt.event.ActionEvent e) {
            if (dialog != null && dialog.isShowing()) {
                dialog.toFront();
            } else {
                createDialog();
            }

        }
    }

    /**
     * @author Joshua Gould
     */
    static class ZoomInAction extends PlotAction {
        public ZoomInAction() {
            super("Zoom In");
            if (System.getProperty(PlotAction.SHOW_ACCELERATORS_PROPERTY,
                    "true").equalsIgnoreCase("true")) {
                KeyStroke ks = KeyStroke.getKeyStroke(
                        java.awt.event.KeyEvent.VK_CLOSE_BRACKET, Toolkit
                        .getDefaultToolkit().getMenuShortcutKeyMaskEx());
                this.putValue(AbstractAction.ACCELERATOR_KEY, ks);
            }
        }

        public void actionPerformed(java.awt.event.ActionEvent e) {
            ChartPanel chartPanel = (ChartPanel) getPlot();
            zoomIn(chartPanel);
        }
    }

    /**
     * @author Joshua Gould
     */
    static class ZoomOutAction extends PlotAction {
        public ZoomOutAction() {
            super("Zoom Out");
            if (System.getProperty(PlotAction.SHOW_ACCELERATORS_PROPERTY,
                    "true").equalsIgnoreCase("true")) {
                KeyStroke ks = KeyStroke.getKeyStroke(
                        java.awt.event.KeyEvent.VK_OPEN_BRACKET, Toolkit
                        .getDefaultToolkit().getMenuShortcutKeyMaskEx());
                this.putValue(AbstractAction.ACCELERATOR_KEY, ks);
            }
        }

        public void actionPerformed(java.awt.event.ActionEvent e) {
            ChartPanel chartPanel = (ChartPanel) getPlot();
            zoomOut(chartPanel);
        }
    }
}
