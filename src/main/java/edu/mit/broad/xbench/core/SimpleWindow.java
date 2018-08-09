/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.core;


import edu.mit.broad.xbench.core.api.Application;

import javax.swing.*;

/**
 * WindowComponent wrapper around a DialogWidget / ErrorWidget
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class SimpleWindow implements WrappedComponent {

    private final Widget fWidget;

    /**
     * Class Constructor.
     *
     * @param title
     * @param icon
     * @param comp
     */
    public SimpleWindow(String title, Icon icon, JComponent comp) {
        this.fWidget = new DialogWidget(title, icon, comp);
    }

    /**
     * Open and display the dialog window
     */
    public void open() {
        Application.getWindowManager().openWindow(this);
    }

    public JComponent getWrappedComponent() {
        return fWidget.getWrappedComponent();
    }

    public String getAssociatedTitle() {
        return fWidget.getAssociatedTitle();
    }

    public Icon getAssociatedIcon() {
        return fWidget.getAssociatedIcon();
    }

}    // End DialogWindow
