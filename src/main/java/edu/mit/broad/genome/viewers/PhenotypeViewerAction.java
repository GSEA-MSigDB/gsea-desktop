/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.viewers;

import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.xbench.actions.ObjectAction;
import edu.mit.broad.xbench.core.Widget;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class PhenotypeViewerAction extends ObjectAction {

    private Template fTemplate;

    /**
     * Class Constructor
     */
    public PhenotypeViewerAction() {
        super("PhenotypeViewerAction", "Phenotype viewer", "View details about this phenotype", 
                PhenotypeViewer.ICON);
    }


    public void setObject(final Object template) {
        this.fTemplate = (Template) template;
    }

    public Widget getWidget() {
        return new PhenotypeViewer(fTemplate);
    }
}    // End TemplateViewerAction
