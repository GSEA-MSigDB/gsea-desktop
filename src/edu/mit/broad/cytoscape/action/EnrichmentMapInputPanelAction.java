/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.cytoscape.action;

import edu.mit.broad.cytoscape.view.EnrichmentMapInputPanel;
import edu.mit.broad.genome.viewers.ReportViewer;
import edu.mit.broad.xbench.actions.WidgetAction;
import edu.mit.broad.xbench.core.Widget;
import edu.mit.broad.xbench.tui.ToolLauncher;

public class EnrichmentMapInputPanelAction  extends WidgetAction {
	private EnrichmentMapInputPanel fViewer;

    /**
     * Class constructor
     */
    public EnrichmentMapInputPanelAction() {
        super("EnrichmentMapVisualizeAction", ToolLauncher.TITLE, 
                "Visualize GSEA enrichment results as an enrichment map in cytoscape", 
                ReportViewer.ICON);
    }


    public Widget getWidget() {

        if (fViewer == null) {
            fViewer = new EnrichmentMapInputPanel();
        }

        return fViewer;
    }
}
