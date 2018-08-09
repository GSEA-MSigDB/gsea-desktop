/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.menu.jfree;

import org.genepattern.menu.PlotAction;
import org.jfree.chart.ChartPanel;

import java.awt.event.ActionEvent;

public class JFreeResetPlotAction extends PlotAction {

    public JFreeResetPlotAction(String name) {
        super(name);
    }

    public void actionPerformed(ActionEvent e) {
        ChartPanel plot = (ChartPanel) getPlot();
        plot.restoreAutoBounds();
    }

}
