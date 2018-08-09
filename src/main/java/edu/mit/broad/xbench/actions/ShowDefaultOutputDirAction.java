/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.actions;

import edu.mit.broad.xbench.actions.ext.OsExplorerAction;
import edu.mit.broad.xbench.core.api.Application;

import java.awt.event.ActionEvent;

/**
 * @author Aravind Subramanian
 */
public class ShowDefaultOutputDirAction extends OsExplorerAction {

    /**
     * Class constructor
     *
     * @param name
     */
    public ShowDefaultOutputDirAction(String name) {
        super(name, "Show output directory of this application", null, Application.getVdbManager().getDefaultOutputDir());
        setRegisterOnComplete();
    }

    
    public void actionPerformed(ActionEvent evt) {
        setPath(Application.getVdbManager().getDefaultOutputDir()); // might have changed since init
        super.actionPerformed(evt);
    }

} // End class ShowAppOutputDirAction
