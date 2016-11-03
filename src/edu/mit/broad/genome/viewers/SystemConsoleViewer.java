/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.viewers;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.swing.SystemConsole;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Widget that builds the local file system view along with a few
 * easy access buttons
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class SystemConsoleViewer extends AbstractViewer {

    public static final String NAME = "SystemConsoleViewer";
    public static final Icon ICON = JarResources.getIcon("Output16.gif");

    /**
     * Class constructor
     *
     * @param ds
     */
    public SystemConsoleViewer() {
        super(NAME, ICON, NAME);
        init();
    }

    private void init() {
        final SystemConsole comp = new SystemConsole();
        comp.setEditable(false);
        this.setLayout(new BorderLayout());
        this.add(new JScrollPane(comp), BorderLayout.CENTER);

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton but = new JButton("Clear All Output");
        panel.add(but);
        but.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                comp.setText("");
            }
        });

        JButton bCopy = new JButton("Copy");
        panel.add(bCopy);
        bCopy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                StringSelection stringSelection = new StringSelection(comp.getText());
                clipboard.setContents(stringSelection, stringSelection);
            }
        });

        this.add(panel, BorderLayout.SOUTH);
    }

    public String getAssociatedTitle() {
        return NAME;
    }


}    // End SystemConsoleViewer
  