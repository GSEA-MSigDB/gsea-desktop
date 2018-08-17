/*******************************************************************************
 * Copyright (c) 2003-2018 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.broad.gsea.ui;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Taskbar;

import javax.swing.JOptionPane;

import xapps.gsea.GseaFijiTabsApplicationFrame;

/**
 * Java version-specific integration with the platform Desktop and particularly 
 * for OS X (macOS) specific items.
 * @author eby
 */
public class DesktopIntegration {
    public static final void verifyJavaPlatform() {
        String javaVersion = System.getProperty("java.version");
        if (javaVersion == null || javaVersion.startsWith("1.8")) {
            try {
                System.out.println("Detected an unsupported Java version.  Java 8 is not supported by this release.");

                if (!GraphicsEnvironment.isHeadless()) {
                    JOptionPane.showMessageDialog(null, "Detected an unsupported Java version.  Java 8 is not supported by this release.");
                }
            } finally {
                System.exit(1);
            }
        }
    }
    
    public static void setDockIcon(Image image) {
        Taskbar.getTaskbar().setIconImage(image);
    }

    public static void setAboutHandler(GseaFijiTabsApplicationFrame applicationFrame) {
        Desktop.getDesktop().setAboutHandler(e -> applicationFrame.showAboutDialog());
    }
    
    public static void setQuitHandler(GseaFijiTabsApplicationFrame applicationFrame) {
        Desktop.getDesktop().setQuitHandler((e, response) -> {
            try {
                applicationFrame.exitApplication();
            } finally {
                response.performQuit();
            }
        });
    }
}
