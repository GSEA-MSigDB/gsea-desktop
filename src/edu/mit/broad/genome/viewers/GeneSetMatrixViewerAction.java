/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.viewers;

import edu.mit.broad.genome.objects.GeneSetMatrix;
import edu.mit.broad.xbench.actions.ObjectAction;
import edu.mit.broad.xbench.core.Widget;

/**
 * @author Aravind Subramanian
 */
public class GeneSetMatrixViewerAction extends ObjectAction {

    private GeneSetMatrix fGeneSetMatrix;

    /**
     * Class Constructor.
     */
    public GeneSetMatrixViewerAction() {
        super("GeneSetMatrixViewerAction", GeneSetMatrixViewer2.NAME, "View GeneSetMatrix", 
                GeneSetMatrixViewer2.ICON);
    }

    public void setObject(Object ds) {
        this.fGeneSetMatrix = (GeneSetMatrix) ds;
    }

    public Widget getWidget() {
        return new GeneSetMatrixViewer2(fGeneSetMatrix);
    }
}    // End GeneSetMatrixViewerAction
