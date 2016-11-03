/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.actions;

import javax.swing.Icon;

/**
 * Tagging class for actions that work off one Object
 * <p/>
 * General notes on actions
 * - should not be reused, make one each time to launch
 * <p/>
 * <p/>
 * ObjectActions must follow the following design pattern:
 * <p/>
 * 1) Constructors:
 * FooObjectAction()
 * FooObjectAction(Object ob)
 * setObject(obj)
 * getWidget() -> should create a new widget
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public abstract class ObjectAction extends WidgetAction {

    protected ObjectAction(String id, String name, String description, Icon icon) {
        super(id, name, description, icon);
    }

    public abstract void setObject(Object obj);

}    // End ObjectAction
