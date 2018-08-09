/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.core.api;

import edu.mit.broad.xbench.tui.ReportStub;
import xtools.api.Tool;

/**
 * Class that defines tool related, application wide API's
 */
public interface ToolManager {

    public String getLastToolName();

    public void setLastToolName(Tool tool);

    public ReportStub getLastReportStub(String toolName);

    public ReportStub[] getReportsInCache();

}
