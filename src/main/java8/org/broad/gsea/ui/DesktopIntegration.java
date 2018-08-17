/*******************************************************************************
 * Copyright (c) 2003-2018 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.broad.gsea.ui;

import java.awt.GraphicsEnvironment;
import java.awt.Image;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import apple.dts.samplecode.osxadapter.OSXAdapter;
import xapps.gsea.GseaFijiTabsApplicationFrame;

/**
 * Java version-specific integration with OS X (macOS)
 * @author eby
 */
public class DesktopIntegration {
    private static Logger log = Logger.getLogger(DesktopIntegration.class);

    public static final void verifyJavaPlatform() {
        String javaVersion = System.getProperty("java.version");
        if (javaVersion == null || !javaVersion.startsWith("1.8")) {
            try {
                System.out.println("Detected an unsupported Java version.  Java 8 is required by this release.");

                if (!GraphicsEnvironment.isHeadless()) {
                    JOptionPane.showMessageDialog(null, "Detected an unsupported Java version.  Java 8 is required by this release.");
                }
            } finally {
                System.exit(1);
            }
        }
    }
    
    public static void setDockIcon(Image image) {
        OSXAdapter.setDockIconImage(image);
    }

    public static void setAboutHandler(GseaFijiTabsApplicationFrame applicationFrame) {
        try {
            OSXAdapter.setAboutHandler(applicationFrame, applicationFrame.getClass().getDeclaredMethod("showAboutDialog", (Class[]) null));
        } catch (Exception e) {
            log.error("Error setting apple-specific about handler", e);
        }
    }
    
    public static void setQuitHandler(GseaFijiTabsApplicationFrame applicationFrame) {
        try {
            OSXAdapter.setQuitHandler(applicationFrame, applicationFrame.getClass().getDeclaredMethod("exitApplication", (Class[]) null));
        } catch (Exception e) {
            log.error("Error setting apple-specific quit handler", e);
        }
    }
}
