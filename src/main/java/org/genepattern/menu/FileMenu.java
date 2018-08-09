/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.menu;

import org.genepattern.uiutil.OS;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Joshua Gould
 */
public abstract class FileMenu extends AbstractPlotMenu {

    public FileMenu(JComponent plot, Frame parent) {
        super("File", plot, parent);
    }

    public static JMenuItem createExitMenuItem() {
        JMenuItem item = null;
        if (OS.isMac()) {
            item = new JMenuItem("Quit");
            KeyStroke ks = KeyStroke.getKeyStroke('Q', Toolkit
                    .getDefaultToolkit().getMenuShortcutKeyMask());
            item.setAccelerator(ks);
        } else {
            item = new JMenuItem("Exit");
        }

        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }

        });
        return item;
    }

}
