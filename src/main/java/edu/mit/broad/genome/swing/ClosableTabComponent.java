/*
 * Copyright (c) 2003-2025 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Closable Tab component
 * 
 * Somewhat inspired by https://docs.oracle.com/javase/tutorial/uiswing/components/tabbedpane.html
 * but heavily modified for our own purposes.
 */ 
public class ClosableTabComponent extends JPanel {
    private final JTabbedPane pane;

    public ClosableTabComponent(final JTabbedPane pane, String tabLabel, Icon icon) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        if (pane == null) {
            throw new NullPointerException("TabbedPane is null");
        }
        this.pane = pane;
        setOpaque(false);
        JLabel label = new JLabel(tabLabel);
        if (icon != null) { label.setIcon(icon); }
        add(label);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        add(new CloseTabButton());
        setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
    }

    private class CloseTabButton extends JButton implements ActionListener {
        public CloseTabButton() {
            setToolTipText("Close");
            setPreferredSize(new Dimension(18, 18));
            setUI(new BasicButtonUI());
            setContentAreaFilled(false);
            setFocusable(false);
            setBorder(BorderFactory.createEtchedBorder());
            setBorderPainted(false);
            setRolloverEnabled(true);
            addMouseListener(ButtonBorderMouseListener);
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            int i = pane.indexOfTabComponent(ClosableTabComponent.this);
            if (i != -1) { pane.remove(i); }
        }

        // Don't update
        public void updateUI() { }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            // Use a slight offset if the button is being pressed
            if (getModel().isPressed()) { g2.translate(1, 1); }
            // Draw the X in black, unless the cursor is over it
            if (! getModel().isRollover()) {
                g2.setColor(Color.BLACK);
            } else {
                g2.setColor(Color.MAGENTA);
            }
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(6, 6, getWidth() - 5, getHeight() - 5);
            g2.drawLine(getWidth() - 5, 6, 6, getHeight() - 5);
            g2.dispose();
        }
    }

    // Draw (or Remove) a border on the button when the mouse enters (or exits) its bounds.
    private final static MouseListener ButtonBorderMouseListener = new MouseAdapter() {
        public void mouseEntered(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(true);
            }
        }

        public void mouseExited(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(false);
            }
        }
    };
}
