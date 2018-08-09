/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.actions;

import edu.mit.broad.genome.viewers.ChipViewer;
import edu.mit.broad.vdb.chip.Chip;
import edu.mit.broad.xbench.core.Widget;

/**
 * @author Aravind Subramanian
 */
public class ChipViewerAction extends ObjectAction {

    private edu.mit.broad.vdb.chip.Chip fChip;

    /**
     * Class Constructor.
     */
    public ChipViewerAction() {
        super("ChipViewerAction", "View Chip Annotation", "View Chip Annotation", 
                ChipViewer.ICON);
    }

    public void setObject(Object chipann) {
        this.fChip = (Chip) chipann;
    }

    public Widget getWidget() {
        return new ChipViewer(fChip);
    }
}    // End ChipViewerAction
