/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.core;

import edu.mit.broad.genome.Errors;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.core.api.DialogDescriptor;
import edu.mit.broad.xbench.core.api.DialogDescriptorJide;

import javax.swing.*;
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
 * @author Aravind Subramanian
 * @version %I%, %G%
 * @note Importance of setting the parent component
 * Having no parent component (or the JOptionPane.getRootFrame() one) causes
 * a modal issue wherein a GUI with the model window showing if minimized
 * appears to hang (the CTRL-DELETE-OPTION / GeneCluster bug that Keith noticed)
 * @see DialogDescriptor
 */
public class ApplicationDialog extends DialogDescriptorJide {

    /**
     * Class Constructor.
     *
     * @param comp
     * @param title
     */
    public ApplicationDialog(final String title, final Component comp) {
        super(title, comp, null);
    }

    

    // -------------------------------------------------------------------------------------------- //
    // ------------------------------------------ ERROR MESSAGES --------------------------------- //
    // -------------------------------------------------------------------------------------------- //

    private static void foo2(final String title,
                             final Throwable t_opt,
                             final String error_msg_opt) {

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
                /*
                if (t instanceof StandardException) {
                    new ApplicationDialog("Error", shortErrorDesc, (StandardException) t).show();
                } else {
                    new ApplicationDialog("Error", shortErrorDesc, t).show();
                }
                */
                /*
                ErrorWidgetJide ed = new ErrorWidgetJide(Application.getWindowManager().getRootFrame(), "Error", shortErrorDesc, t);
                ed.pack();
                ed.setLocationRelativeTo(Application.getWindowManager().getRootFrame());
                ed.setVisible(true);
                */

            }
        });
    }

    // -------------------------------------------------------------------------------------------- //
    // -------------------------------------------------------------------------------------------- //
    // -------------------------------------------------------------------------------------------- //

    public static void showMessage(final String msg) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(Application.getWindowManager().getRootFrame(), msg);
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

}    // End ApplicationDialog

/**
 * Show the error in a modal window
 *
 * @param title
 * @param messagePrefix
 * @param t
 */
/*
private ApplicationDialog(final String title, final String messagePrefix, final Throwable t) {
    Component comp = new ErrorWidget(title, messagePrefix, t);
    jbInit(title, comp);
    super.getCancelButton().setText("Close");
}

private ApplicationDialog(final String title, final String messagePrefix, final StandardException t) {
    super(title, new ErrorWidget(title, messagePrefix, t), JarResources.createHelpAction(t));
    super.getCancelButton().setText("Close");
    super.removeOKButton();
}
*/

//  private static void foo(final String title, final Throwable t_opt, final String error_msg_opt) {
/*
JideOptionPane optionPane = new JideOptionPane("Click \"Details\" button to see more information ... ",
        JOptionPane.ERROR_MESSAGE, JideOptionPane.CLOSE_OPTION);
optionPane.setTitle(title);

String details; // @todo error code for std exception
if (t_opt != null) {
    details = TraceUtils.getAsString(t_opt);
} else {
    details = error_msg_opt;
}
optionPane.setDetails(details);
JDialog dialog = optionPane.createDialog(Application.getWindowManager().getRootFrame(), "Error");
dialog.setResizable(true);
dialog.pack();
dialog.setVisible(true);
*/

/*

    JideOptionPane optionPane = new JideOptionPane("Click \"Details\" button to see more information ... ", JOptionPane.ERROR_MESSAGE, JideOptionPane.CLOSE_OPTION);
    optionPane.setTitle("An exception happened during file transfers - if the title is very long, it will wrap automatically.");

    String details = ("java.lang.Exception: Stack trace\n" +
            "\tat java.awt.Component.processMouseEvent(Component.java:5957)\n" +
            "\tat javax.swing.JComponent.processMouseEvent(JComponent.java:3284)\n" +
            "\tat java.awt.Component.processEvent(Component.java:5722)\n" +
            "\tat java.awt.Container.processEvent(Container.java:1966)\n" +
            "\tat java.awt.Component.dispatchEventImpl(Component.java:4365)\n" +
            "\tat java.awt.Container.dispatchEventImpl(Container.java:2024)\n" +
            "\tat java.awt.Component.dispatchEvent(Component.java:4195)\n" +
            "\tat java.awt.LightweightDispatcher.retargetMouseEvent(Container.java:4228)\n" +
            "\tat java.awt.LightweightDispatcher.processMouseEvent(Container.java:3892)\n" +
            "\tat java.awt.LightweightDispatcher.dispatchEvent(Container.java:3822)\n" +
            "\tat java.awt.Container.dispatchEventImpl(Container.java:2010)\n" +
            "\tat java.awt.Window.dispatchEventImpl(Window.java:2299)\n" +
            "\tat java.awt.Component.dispatchEvent(Component.java:4195)\n" +
            "\tat java.awt.EventQueue.dispatchEvent(EventQueue.java:599)\n" +
            "\tat java.awt.EventDispatchThread.pumpOneEventForFilters(EventDispatchThread.java:273)\n" +
            "\tat java.awt.EventDispatchThread.pumpEventsForFilter(EventDispatchThread.java:183)\n" +
            "\tat java.awt.EventDispatchThread.pumpEventsForHierarchy(EventDispatchThread.java:173)\n" +
            "\tat java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:168)\n" +
            "\tat java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:160)\n" +
            "\tat java.awt.EventDispatchThread.run(EventDispatchThread.java:121)");


    optionPane.setDetails(details);
    JDialog dialog = optionPane.createDialog(Application.getWindowManager().getRootFrame(), "Warning");
    dialog.setResizable(true);
    dialog.pack();
    dialog.setVisible(true);
}
*/