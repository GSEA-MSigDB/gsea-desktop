/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.tui;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.xbench.core.Widget;

import javax.swing.*;

/**
 * @author Aravind Subramanian
 */
public interface ToolLauncher extends Widget {

    public static final String TITLE = "Tool Launcher";

    public static final Icon ICON = JarResources.getIcon("ToolLauncher.gif"); // prefer this to the other C one

}
