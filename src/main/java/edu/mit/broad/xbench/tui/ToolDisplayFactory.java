/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.tui;

import xtools.api.Tool;
import xtools.api.param.ParamSet;

import javax.swing.*;

import edu.mit.broad.genome.viewers.ReportViewer;

import java.awt.event.MouseMotionListener;

/**
 * @author Aravind Subramanian
 */
public class ToolDisplayFactory {

    public static ParamSetDisplay createParamSetDisplayComponent(final Tool tool, final MouseMotionListener ml) {
        return createParamSetDisplayComponent(tool.getTitle(), null, tool.getParamSet(), ml);
    }

    public static ParamSetDisplay createParamSetDisplayComponent(final String title,
                                                                 final Icon icon,
                                                                 final ParamSet pset,
                                                                 final MouseMotionListener ml) {
        // @changed march 2006 always go with the form
        return new ParamSetFormDisplay(title, icon, pset, ml);
    }

    public static ToolSelectorTree createToolSelector(final Tool[] tools,
                                                    final boolean showReportNode,
                                                    final boolean showRootNode) {

        return new ToolSelectorTree(tools, showReportNode, showRootNode);
    }

    public static ToolLauncher createPastAnalysisLauncher() {
        return new ToolLauncherDefaultImpl(new Tool[]{}, true, false, false, false,
                true, ReportViewer.ICON, "Analysis history");
    }

} // End class ParamSetDisplayFactory
