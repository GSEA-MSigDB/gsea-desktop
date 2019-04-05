/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xapps.api;

import edu.mit.broad.genome.viewers.ReportViewer;
import edu.mit.broad.xbench.actions.WidgetAction;
import edu.mit.broad.xbench.core.Widget;
import edu.mit.broad.xbench.tui.ToolLauncher;
import edu.mit.broad.xbench.tui.ToolLauncherDefaultImpl;

import java.awt.*;

/**
 * @author Aravind Subramanian
 */
public class PastAnalysisAction extends WidgetAction {

    public final static Dimension DEFAULT_DIM = new Dimension(550, 380);
    private static ToolLauncher kToolLauncher = null;

    /**
     * Class constructor
     */
    public PastAnalysisAction() {
        super("PastAnalysisAction", ToolLauncher.TITLE, "Record of past analyses",
                ReportViewer.ICON);
    }

    public Widget getWidget() {
        // IMP dont place this in the class init are -- cause the app to recursively loop
        //setSize(DEFAULT_DIM.width, DEFAULT_DIM.height, true);

        if (kToolLauncher == null) {
            kToolLauncher = new ToolLauncherDefaultImpl();
        }

        return kToolLauncher;
    }
}    // End GeneSetUtilitiesLauncherAction


