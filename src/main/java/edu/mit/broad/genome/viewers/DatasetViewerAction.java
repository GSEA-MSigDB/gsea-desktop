/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.viewers;

import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.xbench.actions.ObjectAction;
import edu.mit.broad.xbench.core.Widget;

/**
 * @author Aravind Subramanian
 */
public class DatasetViewerAction extends ObjectAction {

    private Dataset fDataset;

    /**
     * Class Constructor.
     */
    public DatasetViewerAction() {
        super("DatasetViewerAction", DatasetViewer.NAME, "View Dataset", 
                DatasetViewer.ICON);
    }

    public void setObject(Object ds) {
        this.fDataset = (Dataset) ds;
    }

    public Widget getWidget() {
        return new DatasetViewer(fDataset);
    }
}    // End DatasetViewerAction
