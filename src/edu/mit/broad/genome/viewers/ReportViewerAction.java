/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.viewers;

import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.xbench.actions.ObjectAction;
import edu.mit.broad.xbench.core.Widget;

/**
 * @author Aravind Subramanian
 */
public class ReportViewerAction extends ObjectAction {

    private Report fReport;

    /**
     * Class constructor
     */
    public ReportViewerAction() {
        super("ReportViewerAction", ReportViewer.NAME, "View the Report", ReportViewer.ICON);
    }

    public void setObject(Object rpt) {
        this.fReport = (Report) rpt;
    }

    public Widget getWidget() {
        return new ReportViewer(fReport);

    }
}    // End ReportViewerAction
