/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.actions.misc_actions;

import edu.mit.broad.genome.swing.GuiHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Class CopyFilesAction
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class CopyFilesAction extends AbstractAction {

    private FilesSelectable fSel;
    private final CopyFilesAction fAction = this;

    /**
     * Class constructor
     *
     * @param selectable
     */
    public CopyFilesAction(FilesSelectable selectable) {

        if (selectable == null) {
            throw new IllegalArgumentException("Param selectable cannot be null");
        }

        this.fSel = selectable;

        this.putValue(Action.NAME, "Copy File(s)");
        this.putValue(Action.SMALL_ICON, GuiHelper.ICON_COPY16);
        this.putValue(Action.SHORT_DESCRIPTION, "Copy File(s)");
    }

    public void actionPerformed(ActionEvent evt) {

        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();

        clip.setContents(fSel.getSelectedFiles(), fSel.getSelectedFiles());
    }


    public MyKeyListener createCtrlCKeyListener() {

        return new MyKeyListener();

    }


    private class MyKeyListener implements KeyListener {
        public void keyTyped(KeyEvent e) {
        }

        public void keyReleased(KeyEvent e) {
        }

        /**
         * Needed - perhaps due to, java doesnt natively support the ctrl-copy thing
         */
        public void keyPressed(KeyEvent e) {

            if ((e.getKeyCode() == KeyEvent.VK_C) && e.isControlDown()) {

                //System.out.println("Ctrl-C DETECTED!");
                fAction.actionPerformed(null);
            }
        }
    }

}

