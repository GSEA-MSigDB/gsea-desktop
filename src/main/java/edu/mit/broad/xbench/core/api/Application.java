/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.xbench.core.api;

/**
 * Application services facade.
 */
public class Application {

    private static Handler kAppHandler;

    private Application() {
    }

    public static boolean isHandlerSet() {
        return kAppHandler != null;
    }

    public static void registerHandler(final Handler appHandler) {
        if (appHandler == null) {
            throw new IllegalArgumentException("Param appHandler cannot be null");
        }
        kAppHandler = appHandler;
    }

    private static void _check() {
        if (kAppHandler == null) {
            throw new IllegalStateException("No Application handler set yet");
        }
    }

    public static ToolManager getToolManager() {
        _check();
        return kAppHandler.getToolManager();
    }

    public static FileManager getFileManager() {
        _check();
        return kAppHandler.getFileManager();
    }

    public static VdbManager getVdbManager() {
        _check();
        return kAppHandler.getVdbManager();
    }

    public static WindowManager getWindowManager() {
        _check();
        return kAppHandler.getWindowManager();
    }

    public interface Handler {
        ToolManager getToolManager();

        FileManager getFileManager();

        VdbManager getVdbManager();

        WindowManager getWindowManager();
    }
}
