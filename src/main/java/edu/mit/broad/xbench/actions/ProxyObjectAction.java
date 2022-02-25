/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.actions;

import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.xbench.core.Widget;
import edu.mit.broad.xbench.core.api.Application;

import java.io.File;

/**
 * Functions:
 * 1) Acts as a proxy for the real ObjectAction.
 * <p/>
 * 2) If the Widget action is an ObjectAction, and a File were specified,
 * his class parses the File and gives the resulting object to the ObjectAction.
 * Lazy loading -> Doesnt parse the File until the popup option is actually selected.
 * <p/>
 * <p/>
 * Note that typically actions are used via ActionFactory, so client code may not need to explicitly ever use
 * this class
 *
 * @author Aravind Subramanian
 */
public class ProxyObjectAction extends LongWidgetAction {
    private ObjectAction fAction;
    private Object fFileOrObj;

    public ProxyObjectAction(ObjectAction action, Object obj) {
        super(getActionId(action), getActionName(action), 
                getActionDescription(action), getActionIcon(action));

        if (obj == null) {
            throw new IllegalArgumentException("Param obj cannot be null");
        }

        this.fAction = action;
        this.fFileOrObj = obj;
    }

    public Widget getWidget() {

        try {

            Object obj;

            if (fFileOrObj instanceof File) {
                File f = (File) fFileOrObj;
                log.debug("Reading file for action: {}", f);
                obj = ParserFactory.read(f);
            } else {
                obj = fFileOrObj;
            }

            if (obj != null) {
                fAction.setObject(obj);
                return fAction.getWidget();
            } else {
                return null; // parsing cancelled
            }


        } catch (Exception e) {
            Application.getWindowManager().showError("Trouble making Proxy", e);
        }

        return null;
    }
}
