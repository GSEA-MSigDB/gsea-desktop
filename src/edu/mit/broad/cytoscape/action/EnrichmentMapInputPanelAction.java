/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.cytoscape.action;

import org.apache.log4j.Logger;

import edu.mit.broad.cytoscape.CytoscapeLaunch;
import edu.mit.broad.cytoscape.CytoscapeLocationSingleton.CytoscapeDownloadException;
import edu.mit.broad.cytoscape.view.EnrichmentMapInputPanel;
import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.viewers.ReportViewer;
import edu.mit.broad.xbench.actions.WidgetAction;
import edu.mit.broad.xbench.core.Widget;
import edu.mit.broad.xbench.tui.ToolLauncher;

public class EnrichmentMapInputPanelAction  extends WidgetAction {
    private static final Logger klog = XLogger.getLogger(EnrichmentMapInputPanelAction.class);

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
            try {
                CytoscapeLaunch cyto = new CytoscapeLaunch();
                cyto.launch();
                fViewer = new EnrichmentMapInputPanel();
            }
            catch (CytoscapeDownloadException cdce) {
                // User canceled the Cytoscape download.
                // We'll swallow the exception (logging it) and null the fViewer reference
                fViewer = null;
                klog.info(cdce.getMessage());
            }
            catch (Throwable t) {
                // Something went wrong in setting up the UI (possibly an issue retrieving Cytoscape)
                // We'll swallow the exception (logging it) and null the fViewer reference
                fViewer = null;
                klog.error("Could not create the EnrichmentMapInputPanel UI.", t);
            }
        }

        return fViewer;
    }
}
