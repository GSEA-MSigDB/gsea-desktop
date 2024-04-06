/*
 * Copyright (c) 2003-2024 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Icon;
import javax.swing.JInternalFrame;
import javax.swing.SwingWorker;
import javax.swing.event.InternalFrameListener;

import edu.mit.broad.xbench.core.Widget;
import edu.mit.broad.xbench.core.api.Application;

/**
 * Base action class for actions that result in opening up a widget in a new window.
 * <p/>
 * Implementing classes should prefer to extends ObjectAction or FileAction or FilesAction
 *
 * @author Aravind Subramanian
 */
public abstract class WidgetAction extends XAction implements MouseListener {
    protected WidgetAction(String id, String name, String description, Icon icon) {
        super(id, name, description, icon);
    }

    /**
     * Indicates whether or not to respond only to double clicks. Default false
     */
    protected boolean fOnlyDc;

    /**
     * null by default
     */
    protected Dimension fExplicitSize;
    protected edu.mit.broad.xbench.core.Window fWindow;

    /**
     * Implementing subclasses must specify
     *
     * @return
     */
    public abstract Widget getWidget() throws Exception;

    /**
     * IMP dont call in action constructor
     * call just before you create the widget!!
     *
     * @param w
     * @param h
     */
    protected void setSize(int w, int h, boolean usedefaultifdefaultisbigger) {
        if (usedefaultifdefaultisbigger) {
            Dimension d = Application.getWindowManager().getExpectedWindowSize();
            if (d != null) {
                if (d.width > w) { w = d.width; }
                if (d.height > h) { h = d.height; }
            }
        }
        this.fExplicitSize = new Dimension(w, h);
    }

    public void actionPerformed(final ActionEvent evt) {
        if (fOnlyDc) {
            return;
        }
        createTask();
    }
    
    public void createTask() {
        SwingWorker<Object, Void> worker = new SwingWorker<Object, Void>() {
            @Override
            protected Object doInBackground() throws Exception {
                try {
                    //log.debug("Waiting for action");
                    Widget widget = getWidget();

                    if (widget != null) {
                        if (fExplicitSize != null) {
                            fWindow = Application.getWindowManager().openWindow(widget, fExplicitSize);
                        } else {
                            fWindow = Application.getWindowManager().openWindow(widget);
                        }

                        //window.setIcon(true); // causes the window to launch and then instantly become an icon

                        if (fWindow instanceof JInternalFrame) {
                            ((JInternalFrame) fWindow).setJMenuBar(widget.getJMenuBar());
                            InternalFrameListener ifl = getInternalFrameListener();
                            if (ifl != null) {
                                ((JInternalFrame) fWindow).addInternalFrameListener(ifl);
                            }
                        }

                    } else {
                        log.debug("null widget - no window opended");
                    }

                } catch (Throwable t) {
                    Application.getWindowManager().showError("Trouble making widget", t);
                }
                return null;
            }
        };
        worker.execute();
    }

    /**
     * Subclasses can override to install a real listener
     * Null here
     *
     * @return
     */
    protected InternalFrameListener getInternalFrameListener() {
        return null;
    }

    public void mouseClicked(MouseEvent e) {
        if (!fOnlyDc) {
            e.consume();
            return;
        }

        if (e.getClickCount() == 2) {
            fOnlyDc = false;
            actionPerformed(new ActionEvent(e.getSource(), e.getID(), getActionId(this),
                    e.getModifiersEx()));
            fOnlyDc = true;
        }
    }

    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
    public void mousePressed(MouseEvent e) { }
    public void mouseReleased(MouseEvent e) { }
}
