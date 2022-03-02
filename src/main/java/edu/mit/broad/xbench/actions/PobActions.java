/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.actions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Aravind Subramanian
 */
public class PobActions {
    private static final Logger klog = LoggerFactory.getLogger(PobActions.class);

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
            klog.error("Invalid default object action index: {} length: {}", defObjectActionIndex, allActions.length);
        }

        if (allActions[defObjectActionIndex] == null) {
            klog.error("Invalid default object action index: {} it is null", defObjectActionIndex);
        }

        if (defFileActionIndex >= allActions.length) {
            klog.error("Invalid default file action index: {} length: ", defObjectActionIndex, allActions.length);
        }

        if (allActions[defFileActionIndex] == null) {
            klog.error("Invalid default file action index: {} it is null", defObjectActionIndex);
        }

        this.defObjectActionIndex = defObjectActionIndex;
        this.defFileActionIndex = defFileActionIndex;
    }
}
