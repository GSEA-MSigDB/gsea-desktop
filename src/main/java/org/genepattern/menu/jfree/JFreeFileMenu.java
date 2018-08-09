/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.menu.jfree;

import org.genepattern.menu.FileMenu;
import org.genepattern.menu.PlotAction;

import javax.swing.*;
import java.awt.*;

/**
 * @author Joshua Gould
 */
public class JFreeFileMenu extends FileMenu {

    protected PlotAction[] createPlotActions() {
        return new PlotAction[]{new JFreeSaveImageAction(parent),
                new JFreePrintAction()};
    }

    public JFreeFileMenu(JComponent plot, Frame parent, boolean showExit) {
        super(plot, parent);
        if (showExit) {
            add(createExitMenuItem());
        }
    }

}
