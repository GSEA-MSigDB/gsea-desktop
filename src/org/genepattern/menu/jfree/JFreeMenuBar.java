/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.menu.jfree;

import org.genepattern.menu.FileMenu;
import org.genepattern.menu.GPMenuBar;
import org.genepattern.menu.ViewMenu;

import javax.swing.*;

public class JFreeMenuBar extends GPMenuBar {

    public JFreeMenuBar(JComponent plot, java.awt.Frame parent) {
        super(plot, parent);
    }

    protected FileMenu createFileMenu(JComponent plot) {
        return new JFreeFileMenu(plot, parent, showExitMenu);
    }

    protected ViewMenu createViewMenu(JComponent plot) {
        return new JFreeViewMenu(plot, parent);
    }
}
