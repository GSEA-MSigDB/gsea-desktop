/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xapps.gsea;

import edu.mit.broad.genome.viewers.ReportViewer;
import edu.mit.broad.xbench.actions.WidgetAction;
import edu.mit.broad.xbench.core.Widget;
import edu.mit.broad.xbench.tui.ToolLauncher;

/**
 * @author Aravind Subramanian
 */
public class LeadingEdgeReportAction extends WidgetAction {

    private LeadingEdgeReportViewer fViewer;

    /**
     * Class constructor
     */
    public LeadingEdgeReportAction() {
        super("LeadingEdgeReportAction", ToolLauncher.TITLE, 
                "Run leading edge analysis to find shared signals amongst gene sets", 
                ReportViewer.ICON);
    }


    public Widget getWidget() {

        if (fViewer == null) {
            fViewer = new LeadingEdgeReportViewer();
        }

        return fViewer;
    }
}    // End LeadingEdgeReportAction
