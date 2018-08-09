/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A convenience event handling class for deciding when to show and when not to show
 * a JPopupMenu based on the type of the MouseEvent.
 * <p/>
 * Classs must implement maybeShowPopup
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public abstract class GPopupChecker extends MouseAdapter {

    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }

    protected abstract void maybeShowPopup(MouseEvent e);

    /* EXAMPLE
    protected void maybeShowPopup(MouseEvent e) {
       if (e.isPopupTrigger()) {
           if (fooo ...) {
               popup.show(e.getComponent(), e.getX(), e.getY());
               }
           }
       }
   }
   */
}    // End GPopupChecker
