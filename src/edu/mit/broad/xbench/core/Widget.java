/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.core;

import javax.swing.*;

/**
 * Widget cant do dnd -> too complex
 * <p/>
 * Two kinds of GUIs in go:
 * Viewers -> more basic, beans like
 * Widge -> a vdbgui that does something nore ocncrete, more complex vdbgui. (see action-widgert ideom)
 * <p/>
 * Things that a widget does:
 * - provide when the wrapper is closed, a mech to detect any objs made
 * so that they can be persisted.
 * - a control panel that is placed (or not) at a standard place by the wrapper action.
 * or not -> if assembling a superwidget from many smaller widgets, aw wrapper might
 * instead choose to use a single control panel. Thus leaving the decision of whther to
 * use the cp or not to the outermost wrapper is best.
 * aw wraper always places the comps as: cp_returned (right justf) Close Help
 * <p/>
 * <p/>
 * Action -> mechanism to install menu stuff
 * triggers openning of a GUI
 * Manages the widget as  a shared class object.
 * When called, displays the widget.
 * <p/>
 * Widget -> a component (usually but not necc a JPanel)
 * that provides user controls or some chart.
 * <p/>
 * Advantages:
 * - helps actions (and other widgets) combine arbitary widgets into a composite widget. *
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public interface Widget extends WrappedComponent {

    public static final JMenuBar EMPTY_MENU_BAR = new JMenuBar();

    public JMenuBar getJMenuBar();
}    // End Widget
