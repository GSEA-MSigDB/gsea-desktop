/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.core.api;

import edu.mit.broad.genome.Errors;
import edu.mit.broad.xbench.core.WrappedComponent;

import javax.swing.*;
import java.awt.*;

/**
 * Class that defines windowing API's.
 * Most will trow a headless exception.
 */
public interface WindowManager {

    public JFrame getRootFrame() throws HeadlessException;

    public Dimension getExpectedWindowSize() throws HeadlessException;

    public edu.mit.broad.xbench.core.Window openWindow(final WrappedComponent wc) throws HeadlessException;

    public edu.mit.broad.xbench.core.Window openWindow(final WrappedComponent wc, final Dimension dim) throws HeadlessException;

    // -------------------------------------------------------------------------------------------- //
    // ----------------------------- DISPLAYS A MESSAGE INFO, WARNING ETC ------------------------- //
    // -------------------------------------------------------------------------------------------- //
    public void showError(final String msg) throws HeadlessException;

    public void showError(final Throwable t) throws HeadlessException;

    public void showError(final Errors errors) throws HeadlessException;

    public void showError(final String msg, final Throwable t) throws HeadlessException;

    public void showError(final String msg,
                          final Throwable t,
                          final JButton[] customRemedyOptions) throws HeadlessException;

    

    // -------------------------------------------------------------------------------------------- //


    public boolean showConfirm(final String msg) throws HeadlessException;

    public boolean showConfirm(final String title, final String msg) throws HeadlessException;

    public void showMessage(final String msg) throws HeadlessException;

    public void showMessage(final String title, final String msg) throws HeadlessException;

    public DialogDescriptor createDialogDescriptor(final String title, final Component comp, final Action helpAction_opt);

    public DialogDescriptor createDialogDescriptor(final String title, final Component comp, final Action helpAction_opt, boolean showLicenseButton);

    public DialogDescriptor createDialogDescriptor(final String title, final Component comp);

    // -------------------------------------------------------------------------------------------- //
    // ----------------------------------ACTION RELATED------------------------------------ //
    // -------------------------------------------------------------------------------------------- //

    public JPopupMenu createPopupMenu(final Object obj) throws HeadlessException;

    public boolean runDefaultAction(final Object obj);

} // End interface WindowManager
