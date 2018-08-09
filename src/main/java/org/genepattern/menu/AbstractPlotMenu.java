/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.menu;

import javax.swing.*;
import java.awt.*;

public abstract class AbstractPlotMenu extends JMenu {
    protected JComponent plot;

    protected PlotAction[] plotActions;

    protected Frame parent;

    protected abstract PlotAction[] createPlotActions();

    public AbstractPlotMenu(String name, JComponent plot, Frame parent) {
        super(name);
        this.parent = parent;
        this.plotActions = createPlotActions();
        setPlot(plot);
        for (int i = 0, length = plotActions.length; i < length; i++) {
            PlotAction a = plotActions[i];
            JMenuItem[] menuItems = a.getSubMenuItems();
            if (menuItems == null) {
                add(a);
            } else {
                JMenu menu = new JMenu(a);
                for (int j = 0; j < menuItems.length; j++) {
                    menu.add(menuItems[j]);
                }
                add(menu);
            }
        }
    }

    public Frame getFrame() {
        return parent;
    }

    public void setPlot(JComponent plot) {
        this.plot = plot;
        for (int i = 0, length = plotActions.length; i < length; i++) {
            plotActions[i].setPlot(plot);
            plotActions[i].setEnabled(plot != null);
        }
    }

}
