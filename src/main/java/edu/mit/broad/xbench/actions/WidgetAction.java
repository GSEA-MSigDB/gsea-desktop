/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.actions;

import edu.mit.broad.xbench.core.Widget;
import edu.mit.broad.xbench.core.api.Application;
import foxtrot.Job;
import foxtrot.Worker;

import javax.swing.*;
import javax.swing.event.InternalFrameListener;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Base action class for actions that result in opening up a widget in a new window.
 * <p/>
 * Implementing classes should prefer to extends ObjectAction or FileAction or FilesAction
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
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
        // IMP dont place this in the class init are -- cause the pp to recursive loop

        if (usedefaultifdefaultisbigger) {
            Dimension d = Application.getWindowManager().getExpectedWindowSize();
            if (d != null) {
                if (d.width > w) {
                    w = d.width;
                }

                if (d.height > h) {
                    h = d.height;
                }
            }
        }

        this.fExplicitSize = new Dimension(w, h);

    }

    /**
     * as modified amber to run under foxtrot
     *
     * @param evt
     */

    //sometimes FOXTROT detects null pointer exceptions when this is used??
    // but seem to have got over it mostly -- so use
    public void actionPerformed(final ActionEvent evt) {

        if (fOnlyDc) {
            return;
        }

        //log.debug("actionPerformed: " + evt);
        Worker.post(new Job() {
            public Object run() {
                Runnable r = createTask(evt);
                Thread t = new Thread(r);
                // @note else the action makes the vdbgui hang
                t.setPriority(Thread.MIN_PRIORITY);
                t.start();
                //log.debug("started thread");
                return null;
            }
        });

    }

    /*
     * WITHOUT FOXTROT - orig amber style impl.
     *  DONT DELETE YET
     * public void actionPerformed(ActionEvent evt) {
     *   //log.debug("actionPerformed: " + evt);
     *
     *   Runnable r = createTask(evt);
     *   Thread t = new Thread(r);
     *   t.start();
     *   //log.debug("started thread");
     * }
     */
    public Runnable createTask(final ActionEvent evt) {

        return new Runnable() {

            public void run() {

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
            }
        };
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

        if (fOnlyDc == false) {
            e.consume();

            return;
        }

        if (e.getClickCount() == 2) {

            //log.debug("Doing double click");
            fOnlyDc = false;

            actionPerformed(new ActionEvent(e.getSource(), e.getID(), getActionId(this),
                    e.getModifiers()));

            fOnlyDc = true;
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }
}    // End WidgetAction
