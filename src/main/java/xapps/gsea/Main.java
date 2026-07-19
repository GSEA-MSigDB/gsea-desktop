/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xapps.gsea;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;

import java.util.Properties;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import org.broad.gsea.ui.DesktopIntegration;

import xapps.gsea.fx.GseaFxApplication;

/**
 * Main class of the GSEA desktop application (OpenJFX UI only).
 *
 * @author Aravind Subramanian
 */
public class Main {
    private static final Logger klog = LoggerFactory.getLogger(Main.class);
    private static final Properties buildProps = JarResources.getBuildInfo();

    static {
        // Must be set before AbstractTool's static initializer forces headless=true
        System.setProperty("java.awt.headless", "false");
        // Historical property: UI is OpenJFX only; ignore any request for another toolkit.
        System.setProperty("gsea.ui", "javafx");

        DesktopIntegration.setDockIcon(JarResources.getImage("icon_64x64.png"));
        klog.info("Startup  GSEA Version " + buildProps.getProperty("build.version")
                + " " + buildProps.getProperty("build.timestamp"));
        klog.info("Java " + System.getProperty("java.version")
                + " (build " + System.getProperty("java.vm.version")
                + ") " + System.getProperty("java.version.date", ""));
        klog.info("Java Vendor: " + System.getProperty("java.vendor")
                + " " + System.getProperty("java.vendor.url", ""));
        klog.info("JVM: " + System.getProperty("java.vm.name", "")
                + " " + System.getProperty("java.vendor.version", "")
                + "   " + System.getProperty("java.compiler", ""));
        klog.info("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version")
                + " " + System.getProperty("os.arch"));
        klog.info("GSEA Directory: " + XPreferencesFactory.kAppRuntimeHomeDir.getAbsolutePath());
        klog.info("UI toolkit: OpenJFX");

        if (SystemUtils.IS_OS_MAC_OSX) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }
    }

    /**
     * Launch the GSEA desktop application (JavaFX).
     */
    public static void main(final String[] args) {
        try {
            GseaFxApplication.launchFx(args);
        } catch (Throwable e) {
            e.printStackTrace();
            klog.error(MarkerFactory.getMarker("FATAL"), "Could not create application", e);
        }
    }
}
