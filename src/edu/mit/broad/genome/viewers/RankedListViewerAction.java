/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.viewers;

import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.xbench.actions.ObjectAction;
import edu.mit.broad.xbench.core.Widget;

/**
 * @author Aravind Subramanian
 */
public class RankedListViewerAction extends ObjectAction {

    private RankedList fRankedList;

    /**
     * Class constructor
     */
    public RankedListViewerAction() {
        super("RankedListViewerAction", RankedListViewer.NAME, "View the Ranked List", 
                RankedListViewer.ICON);
    }


    public void setObject(Object obj) {
        this.fRankedList = (RankedList) obj;
    }

    public Widget getWidget() {
        return new RankedListViewer(fRankedList);

    }
}    // End FSetViewerAction
