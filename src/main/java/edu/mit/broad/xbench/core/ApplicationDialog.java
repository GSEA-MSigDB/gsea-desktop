/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.core;

import edu.mit.broad.genome.Errors;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.core.api.DialogDescriptor;
import edu.mit.broad.xbench.core.api.DialogDescriptorJide;

import javax.swing.*;

import org.apache.commons.lang3.StringUtils;

import java.awt.*;

/**
 * Usually modal, application wide dialogs. They block GUI usage
 * until user chooses an option.
 * <p/>
 * NO internal dialogs stuff!!
 * <p/>
 * Auot sets the application as the parent component
 * <p/>
 * Uses JOptionPane internally, but adds some xomics candy.
 * plus has some magic to make jlists double clickable
 * <p/>
 * IMP: much of the code is borrowed from DialogDescriptor
 * <p/>
 * Advantage in parcelling this off into a class is that we redecue client codes
 * usage of Application.
 *
 * @author Aravind Subramanian, David Eby
 * @note Importance of setting the parent component
 * Having no parent component (or the JOptionPane.getRootFrame() one) causes
 * a modal issue wherein a GUI with the model window showing if minimized
 * appears to hang (the CTRL-DELETE-OPTION / GeneCluster bug that Keith noticed)
 * @see DialogDescriptor
 */
public class ApplicationDialog extends DialogDescriptorJide {
    public ApplicationDialog(final String title, final Component comp) {
        super(title, comp, null);
    }
    
    private static void foo2(final String title, final Throwable t_opt, final String error_msg_opt) {
        _show(new ErrorWidgetJide2(Application.getWindowManager().getRootFrame(), title, t_opt, error_msg_opt));
    }

    private static void foo2(final Errors errors) {
        _show(new ErrorWidgetJide2(Application.getWindowManager().getRootFrame(), errors));
    }

    private static void foo2(final String msg) {
        _show(new ErrorWidgetJide2(Application.getWindowManager().getRootFrame(), msg));
    }

    private static void _show(ErrorWidgetJide2 ew) {
        ew.pack();
        ew.setLocationRelativeTo(Application.getWindowManager().getRootFrame());
        ew.setVisible(true);
    }

    public static void showError(final Errors errors) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                foo2(errors);
            }
        });
    }

    public static void showError(final String msg) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                foo2(msg);
            }
        });
    }

    public static void showError(final String shortErrorDesc, final Throwable t) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                foo2(shortErrorDesc, t, null);
            }
        });
    }

    public static void showFormattedMessage(final String htmlMsg) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JLabel label = new JLabel(htmlMsg);
                JOptionPane.showMessageDialog(Application.getWindowManager().getRootFrame(), label);
            }
        });
    }

    public static void showMessage(final String msg) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Object dialogMsg = (StringUtils.startsWithIgnoreCase(msg, "<html>")) ? new JLabel(msg) : msg;
                JOptionPane.showMessageDialog(Application.getWindowManager().getRootFrame(), dialogMsg);
            }
        });
    }

    public static void showMessage(final String title, final String msg) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(Application.getWindowManager().getRootFrame(), msg, title, JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    public static boolean showConfirm(final String title, String msg) {
        int res = JOptionPane.showConfirmDialog(Application.getWindowManager().getRootFrame(), msg, title, JOptionPane.OK_CANCEL_OPTION);
        return res == ApplicationDialog.OK_OPTION;
    }
}
