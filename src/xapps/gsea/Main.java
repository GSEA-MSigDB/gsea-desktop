/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xapps.gsea;

import com.jgoodies.looks.LookUtils;
import com.jgoodies.looks.Options;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.jgoodies.looks.plastic.theme.ExperienceBlue;
import com.jidesoft.comparator.ObjectComparatorManager;
import com.jidesoft.swing.SplashScreen;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.utils.SystemUtils;
import edu.mit.broad.xbench.ComparatorFactory2;

import org.apache.log4j.Logger;

import javax.swing.*;

/**
 * Main class of GSEA application
 * Use this in jar files, command line etc to launch the application.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class Main {

    /**
     * Note that it is necessary to provide a JIDE Software license key in order to use JIDE Components, Dock and Grids.
     * The key included here was generously provided to the GSEA development team for use with the GSEA project.
     * Other developers and commercial users should contact http://www.jidesoft.com to determine what type of license
     * is needed.
     */
    static {
        // Tell Jide we are valid
        com.jidesoft.utils.Lm.verifyLicense("Broad Institute of MIT and Harvard",
                "Gene set  enrichment analysis java desktop application",
                "YSjBO6OJfF9WbavzI73Jt1HgDI4x9L21");

        if (SystemUtils.isMac()) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            
            // TODO: examine whether to reinstate this code.
            // Skipping for now to keep Apple JAR dependencies out of the build.
            // Disable the 'About' until we build full support.
            // This is not the preferred way to do it - it will suffice for now.
            //com.apple.eawt.Application.getApplication().setEnabledAboutMenu(false);
        }
    }

    private static final Logger klog = XLogger.getLogger(Main.class);

    /**
     * Class Constructor.
     */
    public Main() {

        super();

        setLnF();

        ImageIcon splash = JarResources.getImageIcon("gsea_splash.png");
        SplashScreen.create(splash);
        SplashScreen.show();

        // start up the application

        final GseaFijiTabsApplicationFrame frame = new GseaFijiTabsApplicationFrame();

	try {
	    frame.init_bg_while_splashing();
	} catch (Throwable t) {
	    System.out.println("Error while initializing .., things may not work");
	    t.printStackTrace();
	}

	SplashScreen.hide();

        frame.makeVisible(true);

        // A global object that can register comparator with a type and a ComparatorContext.
        ObjectComparatorManager.registerComparator(Integer.class, new ComparatorFactory2.IntegerComparator());
        ObjectComparatorManager.registerComparator(Float.class, new ComparatorFactory2.FloatComparator());

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
            klog.fatal("Could not create application", e);
        }
    }

    public void setLnF() {
    
        try {
            // Set the look and feel early
            // Java Web Start
            // If you use a third party l&f in a network launchable environment such as Java Web Start, you must indicate where to find the l&f classes:
            UIManager.put("ClassLoader", LookUtils.class.getClassLoader());
    
            try {
                if (!SystemUtils.isMac()) {
                    PlasticXPLookAndFeel.setMyCurrentTheme(new ExperienceBlue());
                    UIManager.setLookAndFeel(new com.jgoodies.looks.plastic.PlasticXPLookAndFeel());
                    //UIManager.setLookAndFeel(new com.jgoodies.looks.));
                    //PlasticLookAndFeel.setMyCurrentTheme(new Silver());
                    //UIManager.setLookAndFeel(new com.jgoodies.looks.plastic.PlasticLookAndFeel());
    
                }
    
            } catch (Throwable t) {
                t.printStackTrace();
            }
            //UIManager.setLookAndFeel("com.jgoodies.looks.plastic.PlasticXPLookAndFeel");
            //UIManager.setLookAndFeel("com.jgoodies.looks.plastic.Plastic3DLookAndFeel");
            //UIManager.setLookAndFeel("com.jgoodies.looks.plastic.ExtWindowsLookAndFeel");
            // PlasticXPLookAndFeel.setMyCurrentTheme(new DarkStar());
            //PlasticXPLookAndFeel.setMyCurrentTheme(new DesertBlue());
    
            // Some UI tweaks thanks to jgoodies suggestions
            // You can force the JGoodies looks and the 1.4 versions of the
            // // Sun looks to use system fonts using a String key, or JGoodies constant:
            UIManager.put("Application.useSystemFontSettings", Boolean.TRUE);
            UIManager.put(Options.USE_SYSTEM_FONTS_APP_KEY, Boolean.TRUE);
    
            // In Microsoft environments that use Tahoma as dialog font, you can
            // tweak the choosen font sizes by setting optional JGoodies font size hints. The global hints can be overriden by look-specific hints:
            Options.setUseSystemFonts(true);
            // @note api deprecated jgoodies 2.0 feb 2006
            //setGlobalFontSizeHints(FontSizeHints.MIXED);
    
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
}    // End Main
