/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing.windows;

import edu.mit.broad.genome.XLogger;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.core.api.DialogDescriptor;

import javax.swing.*;

import org.apache.log4j.Logger;

/**
 * Window wrapper for a JTextArea
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class GTextAreaWindow {
    protected static final Logger klog = XLogger.getLogger(GTextAreaWindow.class);

    private final JTextArea taEntry;

    private static final String TITLE = "Specify values, one per line (spaces NOT allowed)";

    /**
     * Class Constructor.
     */
    public GTextAreaWindow() {
        this("");
    }

    /**
     * Class Constructor.
     *
     * @param options
     */
    public GTextAreaWindow(String curr) {
        taEntry = new JTextArea(curr);
    }

    /**
     * @return value selected or null is user cancelled
     */
    public String show() {

        DialogDescriptor desc = Application.getWindowManager().createDialogDescriptor(TITLE, new JScrollPane(taEntry));

        int res = desc.show();

        if (res == DialogDescriptor.CANCEL_OPTION) {
            return null;
        } else {
            return taEntry.getText();
        }
    }

}        // End GTextAreaWindow
