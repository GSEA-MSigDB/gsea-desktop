/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.prefs;

import edu.mit.broad.genome.Conf;
import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;
import edu.mit.broad.genome.utils.SystemUtils;
import edu.mit.broad.xbench.actions.XAction;
import edu.mit.broad.xbench.core.ApplicationDialog;
import edu.mit.broad.xbench.core.api.Application;

import org.apache.log4j.Logger;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.prefs.BackingStoreException;

/**
 * IMP IMP be very careful with using graphics stuff here -> need to always check for headless mode!!
 * <p/>
 * IMP IMP: Dont use RuntimeResources to avoid recursion!!
 */
public class XPreferencesFactory {

    private static final Logger klog = XLogger.getLogger(XPreferencesFactory.class);

    /**
     * runtime home directory (i.e user's home for the application
     * Under ~home.
     */
    // IMP must be declared before the recent files/dirs are made (else they are made in pwd -- remember the jws desktop issue!)
    // IMP This is NOT a preference!
    public static File kAppRuntimeHomeDir;

    private static final String GSEA_HOME = "gsea_home";
    
    /*
     * Variable to store Cytoscape directory.  Default value is null.  Only when it is needed is it set.
     */
    public static final DirPreference kCytoscapeDirectory = new DirPreference("Cytoscape Directory", "Directory where cytoscape application is located", new File("Cytoscape not set yet"),false,false);
    
    /**
     * This has names of files that are to be copied. This list is (re)generated every time
     * BuildHelper is run
     */
    public static String RESOURCES_DATA_FILE_NAME;

    public static final DirPreference kSpecialUserHomeDir = new DirPreference("Special location for " + GSEA_HOME + " folder",
            "Special (non-default) location of " + GSEA_HOME + " folder where the applications reads/stores info",
            SystemUtils.getUserHome(), false, true);

    static {

        kAppRuntimeHomeDir = new File(SystemUtils.getUserHome(), "gsea_home");
        if (kAppRuntimeHomeDir.exists() == false) {
            boolean made = kAppRuntimeHomeDir.mkdir();
            if (!made) {
                klog.fatal("Could not make gsea_home dir at: >" + kAppRuntimeHomeDir + "<");
            }
        }

        RESOURCES_DATA_FILE_NAME = "RdfGseaApp.txt";
        klog.debug("kAppRuntimeHomeDir: " + kAppRuntimeHomeDir + " " + kAppRuntimeHomeDir.exists());
    }

    // -------------------------------------------------------------------------------------------- //

    // @todo Maybe store as a pref later

    public static int getToolTreeWidth() {
        if (Conf.isGseaApp()) {
            return 300;
        } else {
            return 250;
        }
    }

    public static int getToolTreeWidth_min() {
        if (Conf.isGseaApp()) {
            return 150;
        } else {
            return 150;
        }
    }

    public static int getToolTreeDivLocation() {
        return 350;
    }

    /**
     * Privatized class constructor
     */
    private XPreferencesFactory() {
    }

    public static void save() throws BackingStoreException {
        klog.info("Saving preferences to store");
        Preference.kPrefs.flush();
    }

    /**
     * GENERAL
     */
    public static final StringPreference kEmail = new StringPreference("Email Address",
            "Users email address",
            SystemUtils.getUserName() + "@change_this.edu", false, false);

    public static final BooleanPreference kAskBeforeAppShutdown = new BooleanPreference("Prompt before closing application",
            "Display a prompt asking for confirmation before shutting down the application",
            false, false, false);

    public static final BooleanPreference kOnlineMode = new BooleanPreference("Connect over the Internet",
            "You can connect to the GSEA website over the Internet. This ensures you always get the current version of gene sets and chip annotations. ",
            true, false, false);

    public static final DirPreference kDefaultReportsOutputDir = new DirPreference("Default reports output folder",
            "Default location of the output_directory where tool reports are stored",
            new File(kAppRuntimeHomeDir, "output"), false, false);

    public static final BooleanPreference kSplitFileExplorerDisplay = new BooleanPreference("Split file explorer display",
            "Show one or two different windows in the File Explorer", false, false, false);

    public static final TabPlacementPreference kTabPlacement = new TabPlacementPreference("Tab Placement",
            "Display location of the tabbed windows created when viewing data / tools", JTabbedPane.TOP);

    public static final BooleanPreference kToolDisplayComponent = new BooleanPreference("Display tool in table", "Display the tool in a Table container", false, false, true);

    public static final BooleanPreference kToolSelectorComponent = new BooleanPreference("Display tool selector in a tree", "Display the tool selector in a Tree", false, false, true);

    public static final BooleanPreference kMakeGseaUpdateCheck = new BooleanPreference("Check for new GSEA version on startup", "Check for new GSEA version on startup", true, false, true);

    public static final PreferenceCategory kGeneralCategory = new PreferenceCategory(new Preference[]{kEmail,
            kAskBeforeAppShutdown,
            kDefaultReportsOutputDir,
            kTabPlacement,
            kToolDisplayComponent,
            kToolSelectorComponent,
            kMakeGseaUpdateCheck
    });
    
    public static final StringPreference kLastToolName = new StringPreference("Last Tool Run",
            "Dont change me",
            "", true, true);

    private static Dimension screenSize = null;

    static { // watch out for headless mode!!
        try {
            if (!GraphicsEnvironment.isHeadless()) {
                screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            } else {
                screenSize = new Dimension(800, 600); // doesnt matter what
            }
        } catch (Throwable t) {
            klog.fatal("Unexpected trouble", t);
        }
    }

    // NOTE: These are not referenced anywhere else in the Application code, but without them 
    // the app window will be set incorrectly.
    public static final IntPreference kAppWidth = new IntPreference("Last app width",
            "Dont change me",
            screenSize.width - 400, false, true);

    public static final IntPreference kAppHeight = new IntPreference("Last app height",
            "Dont change me",
            screenSize.height - 400, false, true);

    public static final BooleanPreference kAppMaximized = new BooleanPreference("app was maximized", "dummy", false, false, true);

    /*
     * ALGORITHMS RELATED PREFERENCES
     * params:
     * USE_BIASED -> true or false (Boolean objects). Default is FALSE.
     * USE_MEDIAN -> true or false (Boolean objects). Default is TRUE.
     * FIX_LOW    -> true or false (Boolean objects). Default is TRUE
     * Template is required.
     */
    public static final BooleanPreference kMedian = new BooleanPreference("Use median instead of mean for class metrics", "Median or mean for distance metrics (such as s2n)"
            , false, false, false);

    public static final BooleanPreference kFixLowVar = new BooleanPreference("Fix metrics for low variance", "Adjust for low variances"
            , true, false, false);

    public static final BooleanPreference kBiasedVar = new BooleanPreference("Use biased variances", "Use biased mode to calculate variances"
            , false, false, false);

    public static final PreferenceCategory kAlgCategory = new PreferenceCategory(new Preference[]{kMedian, kFixLowVar, kBiasedVar});

    public static JCheckBoxMenuItem createCheckBoxMenuItem(final BooleanPreference pref) {

        final JCheckBoxMenuItem mi = new JCheckBoxMenuItem(pref.getName(), ((Boolean) pref.getValue()).booleanValue());

        mi.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                //System.out.println(">> " + e);
                try {
                    pref.setValue(Boolean.valueOf(mi.getState()));
                    klog.debug("set pref: " + pref.getName() + " to: " + pref.getValue());
                } catch (Throwable t) {
                    Application.getWindowManager().showError("Could not set preference: " + pref.getName(), t);
                }
            }
        });

        return mi;
    }

    // @maint add a new category and this var needs attention
    public static PreferenceCategory[] ALL_CATEGORIES;

    static {
        ALL_CATEGORIES = new PreferenceCategory[]{ kGeneralCategory, kAlgCategory };
    }

    /**
     * @param pref
     * @return
     */
    public static Object showSetPreferenceDialog(final Preference pref) {
        GFieldPlusChooser field = pref.getSelectionComponent();
        String title = "Set preference: " + pref.getName();

        JPanel input = new JPanel(new BorderLayout());

        JLabel label = new JLabel(pref.getName() + ": ");
        label.setFont(GuiHelper.FONT_DEFAULT_BOLD);
        input.add(label, BorderLayout.WEST);
        input.add(field.getComponent(), BorderLayout.CENTER);
        input.add(new JLabel(pref.getDesc()), BorderLayout.SOUTH);

        ApplicationDialog dd = new ApplicationDialog(title, input);
        int res = dd.show();
        if (res == ApplicationDialog.OK_OPTION) {
            try {
                pref.setValue(field.getValue());
            } catch (Throwable t) {
                Application.getWindowManager().showError("Could not set preference: " + pref.getName(), t);
                return null;
            }
        }

        return pref.getValue();
    }

    public static JButton createActionButton(Preference pref) {
        return new JButton(new GenericPrefAction(pref));
    }

    static class GenericPrefAction extends XAction {

        Preference fPref;

        /**
         * Class Constructor.
         */
        GenericPrefAction(Preference pref) {
            this(pref, null);
        }

        GenericPrefAction(Preference pref, Icon customIcon) {
            super("GenericPrefAction", pref.getName(), pref.getDesc(), customIcon);
            this.fPref = pref;
        }

        public void actionPerformed(ActionEvent evt) {
            showSetPreferenceDialog(fPref); // ignore the return value
        }
    }    // End GenericPrefAction

} // End XPreferencesFactory
