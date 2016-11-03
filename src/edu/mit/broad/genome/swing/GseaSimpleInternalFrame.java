/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.UIManager;

import com.jgoodies.looks.LookUtils;
import com.jgoodies.uif_lite.panel.SimpleInternalFrame;

public class GseaSimpleInternalFrame extends SimpleInternalFrame {

    public GseaSimpleInternalFrame() {
        super();
    }

    public GseaSimpleInternalFrame(String title) {
        super(title);
    }

    public GseaSimpleInternalFrame(Icon icon, String title) {
        super(icon, title);
    }

    @Override
    protected Color getHeaderBackground() {
        Color c = UIManager.getColor("SimpleInternalFrame.activeTitleBackground");
        if (c != null) {
            return c;
        }
        if (LookUtils.IS_LAF_WINDOWS_XP_ENABLED) {
            c = UIManager.getColor("InternalFrame.activeTitleGradient");
        }

        return c != null ? c : UIManager.getColor("InternalFrame.activeTitleBackground");
    }
}
