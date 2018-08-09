/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.menu;

import javax.swing.*;
import java.awt.*;

/**
 * @author Joshua Gould
 */
public abstract class ViewMenu extends AbstractPlotMenu {

    public ViewMenu(JComponent plot, Frame parent) {
        super("View", plot, parent);
    }

}
