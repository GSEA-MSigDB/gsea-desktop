/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.actions;

import edu.mit.broad.xbench.actions.ext.OsExplorerAction;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;

import java.awt.event.ActionEvent;

/**
 * @author Aravind Subramanian
 */
public class ShowAppRuntimeHomeDirAction extends OsExplorerAction {

    /**
     * Class constructor
     *
     * @param name
     */
    public ShowAppRuntimeHomeDirAction(final String name) {
        super(name, "Show runtime home directory of this application", null, XPreferencesFactory.kAppRuntimeHomeDir);
        setRegisterOnComplete();
    }

    public void actionPerformed(ActionEvent evt) {
        setPath(XPreferencesFactory.kAppRuntimeHomeDir); // might have changed since init
        super.actionPerformed(evt);
    }

}    // End ShowAppRuntimeHomeDirAction