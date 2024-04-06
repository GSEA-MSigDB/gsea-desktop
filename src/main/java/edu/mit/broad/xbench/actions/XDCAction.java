/*
 * Copyright (c) 2003-2024 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.actions;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractAction;
import javax.swing.Icon;

/**
 * Similar functionality as Xaction, except that here it is possible
 * to optionally only respond to double clicks
 *
 * @author Aravind Subramanian
 * @see XAction
 */
public abstract class XDCAction extends AbstractAction implements MouseListener {
    public static final String ID = "ID";

    /**
     * Indicates whether or not to respond only to double clicks. Default false
     */
    protected boolean fOnlyDoubleClick;

    public XDCAction(String id, String name, String description, Icon icon) {
        super();
        super.putValue(ID, id);
        super.putValue(NAME, name);
        super.putValue(SHORT_DESCRIPTION, description);
        super.putValue(SMALL_ICON, icon);
    }

    public void mouseClicked(MouseEvent e) {
        if (!fOnlyDoubleClick) {
            e.consume();
            return;
        }

        if (e.getClickCount() == 2) {
            fOnlyDoubleClick = false;
            actionPerformed(new ActionEvent(e.getSource(), e.getID(), getValue(ID).toString(),
                    e.getModifiersEx()));
            fOnlyDoubleClick = true;
        }
    }

    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
    public void mousePressed(MouseEvent e) { }
    public void mouseReleased(MouseEvent e) { }
}
