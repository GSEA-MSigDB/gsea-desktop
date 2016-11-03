/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.actions;

import edu.mit.broad.genome.XLogger;
import org.apache.log4j.Logger;

/**
 * @author Aravind Subramanian
 */
public class PobActions {

    private static final Logger klog = XLogger.getLogger(PobActions.class);

    public XAction[] allActions;
    public int defObjectActionIndex;
    public int defFileActionIndex;

    public PobActions() {
    }

    public PobActions(final XAction[] allActions) {
        this(allActions, 0, 0);
    }

    public PobActions(final XAction[] allActions,
                      final int defObjectActionIndex,
                      final int defFileActionIndex) {

        this.allActions = allActions;

        // dont exception, it is used in initialization
        if (defObjectActionIndex >= allActions.length) {
            klog.fatal("Invalid default object action index: " + defObjectActionIndex + " length: " + allActions.length);
        }

        if (allActions[defObjectActionIndex] == null) {
            klog.fatal("Invalid default object action index: " + defObjectActionIndex + " it is null");
        }

        if (defFileActionIndex >= allActions.length) {
            klog.fatal("Invalid default file action index: " + defObjectActionIndex + " length: " + allActions.length);
        }

        if (allActions[defFileActionIndex] == null) {
            klog.fatal("Invalid default file action index: " + defObjectActionIndex + " it is null");
        }

        this.defObjectActionIndex = defObjectActionIndex;
        this.defFileActionIndex = defFileActionIndex;
    }
} // End inner class DXAction
