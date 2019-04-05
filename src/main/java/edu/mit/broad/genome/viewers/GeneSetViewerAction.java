/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.viewers;

import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.xbench.actions.ObjectAction;
import edu.mit.broad.xbench.core.Widget;

/**
 * @author Aravind Subramanian
 */
public class GeneSetViewerAction extends ObjectAction {

    private GeneSet fGeneSet;

    /**
     * Class constructor
     */
    public GeneSetViewerAction() {
        super("GeneSetViewerAction", GeneSetViewer.NAME, "View the Gene Set", 
                GeneSetViewer.ICON);
    }


    public void setObject(Object obj) {
        this.fGeneSet = (GeneSet) obj;
    }

    public Widget getWidget() {
        return new GeneSetViewer(fGeneSet);

    }
}    // End GeneSetViewerAction
