/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.swing.windows;

import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.core.api.DialogDescriptor;

import javax.swing.*;

/**
 * Window wrapper for a JTextArea
 *
 * @author Aravind Subramanian
 */
public class GTextAreaWindow {
    private final JTextArea taEntry;

    private static final String TITLE = "Specify values, one per line (spaces NOT allowed)";

    public GTextAreaWindow() {
        this("");
    }

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
}
