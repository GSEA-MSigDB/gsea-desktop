/*
 * Copyright (c) 2003-2024 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.menu.jfree;

import org.genepattern.menu.PlotAction;
import org.genepattern.uiutil.UIUtil;
import org.jfree.chart.ChartPanel;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

public class JFreePrintAction extends PlotAction {

    public JFreePrintAction() {
        super("Print...");
        if (System.getProperty(PlotAction.SHOW_ACCELERATORS_PROPERTY, "true")
                .equalsIgnoreCase("true")) {
            KeyStroke ks = KeyStroke.getKeyStroke('P', Toolkit
                    .getDefaultToolkit().getMenuShortcutKeyMaskEx());
            this.putValue(AbstractAction.ACCELERATOR_KEY, ks);
        }
    }

    public void actionPerformed(ActionEvent e) {
        PrinterJob printerJob = PrinterJob.getPrinterJob();
        final ChartPanel plot = (ChartPanel) getPlot();
        printerJob.setPrintable(plot);
        if (printerJob.printDialog()) {
            try {
                printerJob.print();
            } catch (PrinterException pe) {
                UIUtil.showErrorDialog(plot.getTopLevelAncestor(),
                        "A printing error occurred.");
            }
        }
    }
}
