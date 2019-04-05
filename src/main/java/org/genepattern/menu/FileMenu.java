/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.menu;

import org.apache.commons.lang3.SystemUtils;

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
        if (SystemUtils.IS_OS_MAC_OSX) {
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
