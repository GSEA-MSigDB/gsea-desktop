/*
 * Copyright (c) 2003-2024 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.core.api;

import edu.mit.broad.genome.Errors;
import edu.mit.broad.xbench.core.ApplicationDialog;
import edu.mit.broad.xbench.core.WrappedComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * @author Aravind Subramanian David Eby
 */
public abstract class AbstractWindowManager implements WindowManager {
    private JFrame fRootFrame;

    private static final Logger klog = LoggerFactory.getLogger(AbstractWindowManager.class);

    public AbstractWindowManager(final JFrame rootFrame) {
        if (rootFrame == null) {
            throw new IllegalArgumentException("Param rootFrame cannot be null");
        }
        this.fRootFrame = rootFrame;
    }

    public Dimension getExpectedWindowSize() {
        return new Dimension(10, 10); // @huh??
    }

    // @todo dim
    public edu.mit.broad.xbench.core.Window openWindow(final WrappedComponent wc, final Dimension dim) {
        return openWindow(wc);
    }

    public JFrame getRootFrame() {
        return fRootFrame;
    }

    public void showError(final String msg, final Throwable t) {
        klog.error(msg, t);
        ApplicationDialog.showError(msg, t);
    }

    public void showError(final String msg) {
        ApplicationDialog.showError(msg);
    }

    public void showError(final Errors errors) {
        klog.error(errors.getErrors(false));
        ApplicationDialog.showError(errors);
    }

    public void showError(final Throwable t) {
        showError("Error", t);
    }

    public boolean showConfirm(final String msg) {
        klog.info(msg);
        return ApplicationDialog.showConfirm("Please confirm this action", msg);
    }

    public boolean showConfirm(final String title, final String msg) {
        klog.info(msg);
        return ApplicationDialog.showConfirm(title, msg);
    }

    public void showMessage(final String msg) {
        klog.info(msg);
        ApplicationDialog.showMessage(msg);
    }

    public void showMessage(final String title, final String msg) {
        klog.info(msg);
        ApplicationDialog.showMessage(title, msg);
    }

    public DialogDescriptor createDialogDescriptor(final String title, final Component comp,
            final Action help_action_opt, final Action info_action_opt, boolean showLicenseButton) {
        return new DialogDescriptor(title, comp, help_action_opt, info_action_opt, showLicenseButton);
    }

    public DialogDescriptor createDialogDescriptor(final String title, final Component comp, final Action help_action_opt) {
        return new DialogDescriptor(title, comp, help_action_opt);
    }

    public DialogDescriptor createDialogDescriptor(final String title, final Component comp) {
        return new DialogDescriptor(title, comp, null);
    }
}
