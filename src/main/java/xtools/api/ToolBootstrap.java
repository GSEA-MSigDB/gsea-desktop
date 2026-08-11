/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xtools.api;

import edu.mit.broad.genome.utils.SystemUtils;
import edu.mit.broad.xbench.core.api.Application;
import xapps.gsea.UpdateChecker;

/**
 * Process-level bootstrap for CLI / headless tool runs.
 * GUI startup must register services before tools run and must not rely on this class
 * to reconfigure the process.
 */
public final class ToolBootstrap {

    private static volatile boolean headlessDefaultsApplied;
    private static volatile boolean updateCheckFromToolsEnabled = true;

    private ToolBootstrap() {
    }

    /**
     * Apply headless defaults and ensure a minimal {@link Application} handler exists.
     * Safe to call multiple times. Does nothing if a handler is already registered (GUI).
     */
    public static synchronized void ensureHeadlessRuntime() {
        if (!headlessDefaultsApplied) {
            if (!SystemUtils.isPropertyDefined("java.awt.headless")) {
                System.setProperty("java.awt.headless", "true");
            }
            headlessDefaultsApplied = true;
        }
        if (!Application.isHandlerSet()) {
            Application.registerHandler(new XToolsApplication());
        }
    }

    /**
     * GUI shell disables per-tool update checks; shell runs the check once at startup.
     */
    public static void setUpdateCheckFromToolsEnabled(boolean enabled) {
        updateCheckFromToolsEnabled = enabled;
    }

    public static boolean isUpdateCheckFromToolsEnabled() {
        return updateCheckFromToolsEnabled;
    }

    /** One-time update check when still owned by the tool path (CLI). */
    public static void maybeCheckForUpdates() {
        if (updateCheckFromToolsEnabled) {
            UpdateChecker.oneTimeGseaUpdateCheck();
        }
    }

    /**
     * CLI entry helper: ensure runtime then run {@link AbstractTool#tool_main}.
     */
    public static void runToolMain(AbstractTool tool) {
        ensureHeadlessRuntime();
        AbstractTool.tool_main(tool);
    }
}
