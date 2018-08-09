/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.actions;

import edu.mit.broad.xbench.core.Widget;

import java.io.File;

/**
 * proxy for real action
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ProxyFileObjectAction extends LongWidgetAction {
    private FileObjectAction fAction;
    private Object fFileOrObj;

    /**
     * Class constructor
     *
     * @param action
     * @param obj
     */
    public ProxyFileObjectAction(FileObjectAction action, Object obj) {
        super(getActionId(action), getActionName(action), 
                getActionDescription(action), getActionIcon(action));

        if (obj == null) {
            throw new IllegalArgumentException("Param obj cannot be null");
        }

        this.fAction = action;
        this.fFileOrObj = obj;
    }

    public Widget getWidget() throws Exception {

        if (fFileOrObj instanceof File) {
            fAction.setFile((File) fFileOrObj);
        } else {
            fAction.setObject(fFileOrObj);
        }

        return fAction.getWidget();
    }
}    // End ProxyFileObjectAction
