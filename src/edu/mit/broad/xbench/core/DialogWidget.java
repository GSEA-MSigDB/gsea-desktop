/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.core;

import edu.mit.broad.genome.swing.GuiHelper;

import javax.swing.*;
import java.awt.*;

/**
 * A NamingConventions Dialog window. Lays out components.
 * <p/>
 * What is the difference between this class and DialogDescriptor?
 * Mainly that this creates a window (i.e a JInternalFrame) that can
 * be around til user chooses to close, rather than a an option pane
 * based input which is usually modal and needs to be dismissed
 * after the message / inout is complete.
 * Example of use - stack trace displayed upon double-clicking the
 * log console.
 * <p/>
 * Only meant for use with JDesktop panes.
 * <p/>
 * A Widget implemntation that simply dumps data  in a comp
 * Why is this needed? Some methods need to return a Widget. If these
 * methods experience an error, sometimes easier to simply still return
 * a widget (i.e this error widget) with a description of the error
 * rather than poping up a option pane.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class DialogWidget extends JPanel implements Widget {

    private final String fTitle;
    private Icon fIcon = GuiHelper.ICON_OPTIONPANE_INFO16;    // default

    /**
     * Class Constructor.
     *
     * @param title
     * @param icon
     * @param comp
     */
    public DialogWidget(String title, Icon icon, JComponent comp) {

        this.fTitle = title;
        this.fIcon = icon;

        this.setLayout(new BorderLayout());
        this.add(comp, BorderLayout.CENTER);
    }

    public JComponent getWrappedComponent() {
        return this;
    }

    public String getAssociatedTitle() {
        return fTitle;
    }

    public Icon getAssociatedIcon() {
        return fIcon;
    }

    public JMenuBar getJMenuBar() {
        return Widget.EMPTY_MENU_BAR;
    }
}    // End ErrorWidget
