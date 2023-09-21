/*
 * Copyright (c) 2003-2023 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xapps.api.frameworks;

import edu.mit.broad.genome.swing.image.IconCustomSized;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

/**
 * A JToolBar with nice big icons appropriate for a workspaces toolbar
 */
public class WorkspaceToolBar extends JToolBar {

    public static final int DEFAULT_BUTTON_WIDTH = 70;
    public static final int DEFAULT_BUTTON_HEIGHT = 40;

    //private final Font bFont = new Font("Arial", Font.BOLD, 10);
    public WorkspaceToolBar() {
        super();
        this.setRequestFocusEnabled(false);

        this.setBorderPainted(true);
        this.setFloatable(false);

        this.setBorderPainted(true);
        this.setRollover(true);
    }

    public WorkspaceToolBar(final int orientation) {
        super(orientation);

        this.setBorderPainted(true);
        this.setFloatable(false);

        this.setBorderPainted(true);
        this.setRollover(true);
    }

    public void add(URL iconURL, String name, final Action action, int width, int height, Font font, boolean alignFontToTheRight) {
        Icon icon = (iconURL != null) ? new IconCustomSized(iconURL, 32, 32) : null;
        JButton but = new JButton(name, icon);
        but.setRolloverEnabled(true);

        if (alignFontToTheRight) {
            but.setHorizontalTextPosition(SwingConstants.RIGHT);
            but.setVerticalTextPosition(SwingConstants.CENTER);
            but.setAlignmentX(JToolBar.LEFT_ALIGNMENT);
        }

        if (font != null) { but.setFont(font); }

        Dimension d = new Dimension(width, height);
        but.setSize(d);
        but.setPreferredSize(d);

        but.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                action.actionPerformed(e);
            }
        });

        super.add(but);
    }
}
