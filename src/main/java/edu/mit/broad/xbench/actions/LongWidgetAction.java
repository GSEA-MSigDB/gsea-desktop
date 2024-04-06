/*
 * Copyright (c) 2003-2024 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.actions;

import edu.mit.broad.xbench.core.Widget;
import edu.mit.broad.xbench.core.api.Application;

import javax.swing.Icon;
import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameListener;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

/**
 * for widget actions that take a while (more than several secs) to launch
 * Basically same as the WidgetAction except no cursor or app status stuff
 *
 * @author Aravind Subramanian
 */
public abstract class LongWidgetAction extends WidgetAction {
    protected LongWidgetAction(String id, String name, String description, Icon icon) {
        super(id, name, description, icon);
    }

    public Runnable createTask(final ActionEvent evt) {
        final LongWidgetAction instance = this;
        return new Runnable() {
            public void run() {
                try {
                    // this is just to indicate that something happened so a dummy timer
                    log.info("Starting: {}", getActionName(instance));
                    Widget widget = getWidget();

                    if (widget != null) {
                        if (fExplicitSize != null) {
                            fWindow = Application.getWindowManager().openWindow(widget, fExplicitSize);
                        } else {
                            fWindow = Application.getWindowManager().openWindow(widget);
                        }

                        if (fWindow instanceof JInternalFrame) {
                            ((JInternalFrame) fWindow).setJMenuBar(widget.getJMenuBar());
                            InternalFrameListener ifl = getInternalFrameListener();
                            if (ifl != null) {
                                ((JInternalFrame) fWindow).addInternalFrameListener(ifl);
                            }
                        }

                        log.info("Opened widget: {}", widget.getAssociatedTitle());
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
