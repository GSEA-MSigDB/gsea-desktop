/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.xbench.prefs;

import edu.mit.broad.genome.utils.SystemUtils;

import xapps.gsea.GseaWebResources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import java.io.File;
import java.util.prefs.BackingStoreException;

/**
 * Application preferences (values only; no UI toolkit dependency).
 */
public class XPreferencesFactory {
    private static final Logger klog = LoggerFactory.getLogger(XPreferencesFactory.class);

    public static File kAppRuntimeHomeDir;

    public static final IntPreference kCytoscapeRESTPort = new IntPreference("Cytoscape REST port",
            "Localhost network port for Cytoscape cyREST API", 1234, false, false);

    static {
        kAppRuntimeHomeDir = new File(SystemUtils.getUserHome(), "gsea_home");
        boolean exists = kAppRuntimeHomeDir.exists();
        if (!exists) {
            boolean made = kAppRuntimeHomeDir.mkdir();
            if (!made) {
                klog.error(MarkerFactory.getMarker("FATAL"), "Could not make gsea_home dir at: >{}<",
                        kAppRuntimeHomeDir);
            }
        }
        klog.debug("kAppRuntimeHomeDir: {} exists: {}", kAppRuntimeHomeDir, exists);
    }

    private XPreferencesFactory() {
    }

    public static void save() throws BackingStoreException {
        klog.info("Saving preferences to store");
        Preference.kPrefs.flush();
    }

    public static final StringPreference kEmail = new StringPreference("Email Address", "Users email address",
            SystemUtils.getUserName() + "@change_this.edu", false, false);

    public static final BooleanPreference kAskBeforeAppShutdown = new BooleanPreference(
            "Prompt before closing application",
            "Display a prompt asking for confirmation before shutting down the application", false, false, false);

    public static final BooleanPreference kOnlineMode = new BooleanPreference("Connect over the Internet",
            "You can connect to the GSEA website over the Internet. This ensures you always get the current version of gene sets and chip annotations. ",
            true, false, false);

    public static final StringPreference kMSigDBCatalogURL = new StringPreference("MSigDB Catalog URL",
            "URL of the JSON catalog listing available MSigDB releases (Human and Mouse gene sets and chip annotations).",
            GseaWebResources.DEFAULT_MSIGDB_CATALOG_URL, false, false);

    public static final DirPreference kDefaultReportsOutputDir = new DirPreference("Default reports output folder",
            "Default location of the output_directory where tool reports are stored",
            new File(kAppRuntimeHomeDir, "output"), false, false);

    public static final BooleanPreference kMakeGseaUpdateCheck = new BooleanPreference(
            "Check for new GSEA version on startup", "Check for new GSEA version on startup", true, false, true);

    /** UI theme: System (follow OS), Light, or Dark. Applied immediately (no restart). */
    public static final StringPreference kUiAppearance = new StringPreference(
            "Appearance",
            "Color theme for the application UI: System, Light, or Dark",
            "System",
            false,
            false);

    public static final PreferenceCategory kGeneralCategory = new PreferenceCategory(new Preference[] { kEmail,
            kAskBeforeAppShutdown, kDefaultReportsOutputDir, kMakeGseaUpdateCheck, kOnlineMode,
            kMSigDBCatalogURL, kUiAppearance });

    public static final StringPreference kLastToolName = new StringPreference("Last Tool Run", "Dont change me", "",
            true, true);

    /** Fallback defaults when no prior window size is stored (no AWT/Toolkit dependency). */
    private static final int DEFAULT_SCREEN_WIDTH = 1440;
    private static final int DEFAULT_SCREEN_HEIGHT = 900;

    public static final IntPreference kAppWidth = new IntPreference("Last app width", "Dont change me",
            DEFAULT_SCREEN_WIDTH - 400, false, true);

    public static final IntPreference kAppHeight = new IntPreference("Last app height", "Dont change me",
            DEFAULT_SCREEN_HEIGHT - 400, false, true);

    public static final IntPreference kAppXPosition = new IntPreference("Last app x position", "Dont change me", 50,
            false, true);

    public static final IntPreference kAppYPosition = new IntPreference("Last app Y position", "Dont change me", 50,
            false, true);

    public static final BooleanPreference kAppMaximized = new BooleanPreference("app was maximized", "dummy", false,
            false, true);

    /**
     * Main shell: left tools column width in pixels.
     * Default must stay in sync with {@code FxToolsRail.DEFAULT_TOOLS_COLUMN_WIDTH_PX} (320).
     */
    public static final IntPreference kShellToolsPanelWidth = new IntPreference(
            "Shell tools panel width px", "Dont change me", 320, false, true);

    /** Main shell: application-messages panel height in pixels. */
    public static final IntPreference kShellMessagesPanelHeight = new IntPreference(
            "Shell messages panel height px", "Dont change me", 180, false, true);

    /** Main shell: tools vs. jobs within the left column (0–100 percent). */
    public static final IntPreference kShellLeftVerticalDivider = new IntPreference(
            "Shell left vertical divider percent", "Dont change me", 55, false, true);

    /**
     * Main shell: tools-rail section and item order.
     * Format: {@code sectionId:itemId,itemId;sectionId:...}
     */
    public static final StringPreference kShellToolsRailOrder = new StringPreference(
            "Shell tools rail order", "Dont change me", "", false, true);

    public static final BooleanPreference kMedian = new BooleanPreference(
            "Use median instead of mean for class metrics",
            "Median or mean for distance metrics (such as s2n)", false, false, false);

    public static final BooleanPreference kFixLowVar = new BooleanPreference("Fix metrics for low variance",
            "Adjust for low variances", true, false, false);

    public static final BooleanPreference kBiasedVar = new BooleanPreference("Use biased variances",
            "Use biased mode to calculate variances", false, false, false);

    public static final PreferenceCategory kAlgCategory = new PreferenceCategory(
            new Preference[] { kMedian, kFixLowVar, kBiasedVar });

    public static PreferenceCategory[] ALL_CATEGORIES;

    static {
        ALL_CATEGORIES = new PreferenceCategory[] { kGeneralCategory, kAlgCategory };
    }
}
