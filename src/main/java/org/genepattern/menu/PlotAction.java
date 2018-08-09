/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.menu;

import javax.swing.*;

/**
 * An <tt>AbstractAction</tt> that performs some action on a plot
 *
 * @author Joshua Gould
 */
// TODO: refactor so that no longer inherits from AbstractAction
public abstract class PlotAction extends AbstractAction {
    static public final String SHOW_ACCELERATORS_PROPERTY = "org.genepattern.menu.showAccelerators";

    // TODO: Prob no need for this to be a general JComponent.  Analysis suggests it can be a ChartPlot or JFreeChart.
    // That's up and down the class hierarchy and the related AbstractPlotMenu hierarchy.  Need to confirm but it
    // looks pretty clear.
    private JComponent plot;

    public PlotAction(String name) {
        super(name);
    }

    public PlotAction() {
        super();
    }

    /**
     * Sets the current plot component
     *
     * @param plot the plot
     */
    public void setPlot(JComponent plot) {
        this.plot = plot;
    }

    /**
     * Gets the current plot component
     *
     * @return the plot
     */
    public JComponent getPlot() {
        return plot;
    }

    /**
     * Creates the sub menu items if this action has any or <tt>null</tt> if
     * this action does not contain menu items and hence will be a menu
     *
     * @return the sub menu items
     */
    public JMenuItem[] getSubMenuItems() {
        return null;
    }
}
