/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.actions;

import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import org.apache.log4j.Logger;

/**
 * Base action-pattern class for all xomics actions.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public abstract class XAction extends AbstractAction {
    public static final String ID = "ID";

    protected static final void checkValidAction(XAction action) {
        if (action == null) {
            throw new IllegalArgumentException("Param action cannot be null");
        }
    }

    protected static final String getActionId(XAction action) {
        checkValidAction(action);
        return Objects.toString(action.getValue(ID));
    }

    protected static String getActionName(XAction action) {
        checkValidAction(action);
        return Objects.toString(action.getValue(NAME));
    }

    protected static String getActionDescription(XAction action) {
        checkValidAction(action);
        return Objects.toString(action.getValue(SHORT_DESCRIPTION));
    }

    protected static Icon getActionIcon(XAction action) {
        checkValidAction(action);
        return (Icon)action.getValue(SMALL_ICON);
    }

    protected final Logger log = Logger.getLogger(XAction.class);


    public XAction(String id, String name, String description) {
        this(id, name, description, null);
    }

    public XAction(String id, String name, String description, Icon icon) {
        super();
        super.putValue(ID, id);
        super.putValue(NAME, name);
        if (description != null) super.putValue(SHORT_DESCRIPTION, description);
        if (icon != null) super.putValue(SMALL_ICON, icon);
    }
}    // End XAction
