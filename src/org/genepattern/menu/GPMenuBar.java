/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.menu;

import javax.swing.*;
import java.awt.*;

public abstract class GPMenuBar extends JMenuBar {
    protected FileMenu fileMenu;

    protected ViewMenu viewMenu;

    protected Frame parent;

    protected abstract FileMenu createFileMenu(JComponent plot);

    protected abstract ViewMenu createViewMenu(JComponent plot);

    protected boolean showExitMenu = false;

    public GPMenuBar(JComponent plot, Frame parent) {
        if (parent == null) {
            throw new NullPointerException("Null parent not allowed");
        }
        this.parent = parent;
        fileMenu = createFileMenu(plot);
        viewMenu = createViewMenu(plot);
        add(fileMenu);
        add(viewMenu);
    }
}