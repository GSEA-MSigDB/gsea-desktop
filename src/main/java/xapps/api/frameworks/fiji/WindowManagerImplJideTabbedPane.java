/*
 * Copyright (c) 2003-2024 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xapps.api.frameworks.fiji;

import java.awt.Component;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;

import edu.mit.broad.xbench.core.WrappedComponent;
import edu.mit.broad.xbench.core.api.AbstractWindowManager;

/**
 * @author Aravind Subramanian
 */
public class WindowManagerImplJideTabbedPane extends AbstractWindowManager {
    private JTabbedPane fTabbedPane;

    public WindowManagerImplJideTabbedPane(final JFrame frame) {
        super(frame);
        fTabbedPane = new JTabbedPane(JTabbedPane.TOP);
        fTabbedPane.setFont(new Font("Helvetica", Font.PLAIN, 14));
    }

    public JTabbedPane getTabbedPane() {
        return fTabbedPane;
    }

    private int getTabIndex(WrappedComponent wc) {
        for (int i = 0; i < fTabbedPane.getTabCount(); i++) {
            Component comp = fTabbedPane.getComponentAt(i);
            if (comp == wc.getWrappedComponent()) {
                return i;
            }
        }

        return -1;
    }

    public edu.mit.broad.xbench.core.Window openWindow(final WrappedComponent wc) {

        int selIndex = getTabIndex(wc);
        if (selIndex != -1) { // helps the jumpiness a lot
            fTabbedPane.setSelectedIndex(selIndex);
        } else {
            fTabbedPane.addTab(wc.getAssociatedTitle(), wc.getAssociatedIcon(), wc.getWrappedComponent());
            fTabbedPane.setSelectedComponent(wc.getWrappedComponent());
        }

        return new JideWindow(wc.getAssociatedTitle(), wc.getWrappedComponent());
    }

    public JPopupMenu createPopupMenu(final Object obj) {
        return null;
    }

    public boolean runDefaultAction(final Object obj) {
        return false;
    }
}
