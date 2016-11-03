/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xapps.api.vtools;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.FormLayout;
import xtools.api.param.Param;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Displays a few params in a jgoodies form
 * No fluff - meant to be displayed in a dilaog window
 * NO basic - advanced concept -- all are shown.
 */
public class ParamSetFormForAFew {

    public static final Color LIGHT_GREEN = Color.decode("#EAFFEA");

    public static final int DEFAULT_INITIAL_DELAY = ToolTipManager.sharedInstance().getInitialDelay();

    public static void enableToolTips(final JLabel label, final Param param) {

        label.setToolTipText(param.getDesc());
        label.addMouseListener(new MouseAdapter() {

            public void mouseEntered(MouseEvent e) {
                // Show tool tips immediately
                ToolTipManager.sharedInstance().setInitialDelay(0);
            }

            public void mouseExited(MouseEvent e) {
                ToolTipManager.sharedInstance().setInitialDelay(DEFAULT_INITIAL_DELAY);
            }

            public void mouseClicked(MouseEvent e) {
                // Show tool tips immediately
                ToolTipManager.sharedInstance().setInitialDelay(0);
            }
        });
    }

    public static PanelBuilder createPanelBuilder(StringBuffer colStr, StringBuffer rowStr) {
        FormLayout layout = new FormLayout(colStr.toString(), rowStr.toString());
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();
        return builder;
    }

} // End class ParamSetForm