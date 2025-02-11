/*
 * Copyright (c) 2003-2025 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xapps.gsea;

import com.jgoodies.looks.LookUtils;
import com.jgoodies.looks.Options;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.jgoodies.looks.plastic.theme.ExperienceBlue;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;

import java.util.Properties;

import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import org.broad.gsea.ui.DesktopIntegration;

/**
 * Main class of GSEA application
 * Use this in jar files, command line etc to launch the application.
 *
 * @author Aravind Subramanian
 */
public class Main {
    private static final Logger klog = LoggerFactory.getLogger(Main.class);
    private static final Properties buildProps = JarResources.getBuildInfo();

    static {
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
        
        if (SystemUtils.IS_OS_MAC_OSX) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }
    }

    public Main() {
        setLnF();

        // start up the application
        final GseaFijiTabsApplicationFrame frame = new GseaFijiTabsApplicationFrame();

    	try {
    	    frame.init_bg_while_splashing();
    	} catch (Throwable t) {
    	    System.out.println("Error while initializing .., things may not work");
    	    t.printStackTrace();
    	}

        frame.makeVisible();
    }

    /**
     * Main method to launch the Tools Desktop application
     *
     * @param args Ignored
     */
    public static void main(final String[] args) {
        try {
            new Main();
        } catch (Throwable e) {
            e.printStackTrace();
            klog.error(MarkerFactory.getMarker("FATAL"), "Could not create application", e);
        }
    }

    public void setLnF() {
        try {
            // Set the look and feel early
            // Java Web Start
            // If you use a third party l&f in a network launchable environment such as Java Web Start, you must indicate where to find the l&f classes:
            UIManager.put("ClassLoader", LookUtils.class.getClassLoader());
    
            try {
                if (!SystemUtils.IS_OS_MAC_OSX) {
                    PlasticXPLookAndFeel.setMyCurrentTheme(new ExperienceBlue());
                    UIManager.setLookAndFeel(new com.jgoodies.looks.plastic.PlasticXPLookAndFeel());
                }
    
            } catch (Throwable t) {
                t.printStackTrace();
            }
            UIManager.put("Application.useSystemFontSettings", Boolean.TRUE);
            UIManager.put(Options.USE_SYSTEM_FONTS_APP_KEY, Boolean.TRUE);
    
            // In Microsoft environments that use Tahoma as dialog font, you can
            // tweak the choosen font sizes by setting optional JGoodies font size hints. The global hints can be overriden by look-specific hints:
            Options.setUseSystemFonts(true);
    
            // Dont know if the font stuff actualy did anything!!
    
            //You can choose between two styles for Plastic focus colors: low vs. high contrast;
            // the default is low. You can choose one of:
            //PlasticXPLookAndFeel.setHighContrastFocusColorsEnabled(true);
    
            // lthough ClearLook will typlically improve the appearance of your application,
            // it may lead to incompatible layout, and so, it is switched off by default.
            // You can switch it on, enable a verbose mode, which will log reports about
            // the performed modifications to the console or use the debug mode. In debug mode,
            // ClearLook will mark decorations that it has identified as visual clutter using saturated colors.
    
            //ClearLookManager.setMode(ClearLookMode.DEBUG);
    
            // enable tooltips application wide
            ToolTipManager.sharedInstance().setEnabled(true);
    
            // make tooltips persist for a wee bit longer than the default
            ToolTipManager.sharedInstance().setDismissDelay(15 * 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
