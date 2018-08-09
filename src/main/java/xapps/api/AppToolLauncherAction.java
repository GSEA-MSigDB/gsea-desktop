/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xapps.api;

import edu.mit.broad.genome.parsers.ParseUtils;
import edu.mit.broad.xbench.actions.WidgetAction;
import edu.mit.broad.xbench.core.Widget;
import edu.mit.broad.xbench.tui.SingleToolLauncher;
import xtools.api.Tool;
import xtools.api.param.ParamSet;

import javax.swing.*;

/**
 * @author Aravind Subramanian
 */
public class AppToolLauncherAction extends WidgetAction {

    private static final String getToolName(final Tool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("Param tool cannot be null");
        }
        return ParseUtils.getLastToken(tool.getName(), ".");
    }
    
    private Tool fTool;
    private ParamSet fPSet;
    private Icon fIcon;

    private String fOptTitle;

    /**
     * Class constructor
     *
     * @param tool
     * @param pset
     * @param icon
     */
    public AppToolLauncherAction(final Tool tool, final ParamSet pset, final String optTitle, final Icon icon) {
        super("AppToolLauncherAction", getToolName(tool), 
              "Set parameters and perform a new task", icon);

        if (pset == null) {
            throw new IllegalArgumentException("Param pset cannot be null");
        }

        this.fTool = tool;
        this.fPSet = pset;
        this.fIcon = icon;
        this.fOptTitle = optTitle;
    }


    private SingleToolLauncher fStl;

    public Widget getWidget() {
        // IMP dont place this in the class init are -- cause the app to recursively loop
        //setSize(ToolLauncherAction.DEFAULT_DIM.width, ToolLauncherAction.DEFAULT_DIM.height, true);

        if (fStl == null) {
            fStl = new SingleToolLauncher(fTool, fPSet, false, false, true, fOptTitle, fIcon);
        }

        fStl.revalidate();
        fStl.repaint();
        return fStl;
    }
}    // End ToolAction


