/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.actions;

import edu.mit.broad.xbench.core.Widget;
import edu.mit.broad.xbench.core.api.Application;

import javax.swing.*;
import javax.swing.event.InternalFrameListener;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

/**
 * for widget actions that take a while (more than several secs) to launch
 * Basicaly same as the WidgetAction except no cursor or app status stuff
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public abstract class LongWidgetAction extends WidgetAction {

    protected LongWidgetAction(String id, String name, String description, Icon icon) {
        super(id, name, description, icon);
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
        final LongWidgetAction instance = this;

        return new Runnable() {

            public void run() {

                try {
                    //log.debug("Waiting for action");
                    //waitCursor("");
                    //defaultCursor();
                    // this is just to indicate that something hapenned so a dummy timer
                    log.info("Starting: " + getActionName(instance));
                    Widget widget = getWidget();

                    if (widget != null) {
                        if (fExplicitSize != null) {
                            fWindow = Application.getWindowManager().openWindow(widget, fExplicitSize);
                        } else {
                            fWindow = Application.getWindowManager().openWindow(widget);
                        }

                        //window.setIcon(true); // causes the window to launch and then instantly become an icon

                        // comm out after window interface
                        if (fWindow instanceof JInternalFrame) {
                            ((JInternalFrame) fWindow).setJMenuBar(widget.getJMenuBar());
                            InternalFrameListener ifl = getInternalFrameListener();
                            if (ifl != null) {
                                ((JInternalFrame) fWindow).addInternalFrameListener(ifl);
                            }
                        }

                        log.info("Opened widget: " + widget.getAssociatedTitle());

                    } else {
                        log.info("Null widget - no window opened");
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
}    // End LongWidgetAction
