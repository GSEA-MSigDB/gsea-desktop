/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.broad.gsea.ui;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Taskbar;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.SystemUtils;

import xapps.gsea.GseaFijiTabsApplicationFrame;

/**
 * Java version-specific integration with the platform Desktop and particularly 
 * for OS X (macOS) specific items.
 * @author eby
 */
public class DesktopIntegration {
    public static void setDockIcon(Image image) {
        if (SystemUtils.IS_OS_MAC_OSX) {
            Taskbar.getTaskbar().setIconImage(image);
        }
    }

    public static void setAboutHandler(GseaFijiTabsApplicationFrame applicationFrame) {
        Desktop.getDesktop().setAboutHandler(e -> applicationFrame.showAboutDialog());
    }
    
    public static void setQuitHandler(GseaFijiTabsApplicationFrame applicationFrame) {
        Desktop.getDesktop().setQuitHandler((e, response) -> {
            if (applicationFrame.exitApplication()) {
            	response.performQuit();
            } else {
            	response.cancelQuit();
            }
        });
    }
}
