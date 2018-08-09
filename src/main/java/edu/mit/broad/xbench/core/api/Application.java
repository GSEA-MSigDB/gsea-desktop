/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.core.api;

import edu.mit.broad.genome.TraceUtils;
import edu.mit.broad.genome.XLogger;
import edu.mit.broad.xbench.core.StatusBar;

import java.awt.*;

/**
 * Factory method for an application
 * <p/>
 * This should be like a POB because only 1 version of it exists (for junit)
 */
public class Application {

    private static Handler kAppHandler;

    public static Dimension DEFAULT_MODAL_WIDGET_RUNNER_DIALOG = new Dimension(575, 500);

    private Application() {
    }

    public static boolean isHandlerSet() {
        return kAppHandler != null;
    }

    public static void registerHandler(final Handler appHandler) {
        //TraceUtils.showTrace();

        if (appHandler == null) {
            throw new IllegalArgumentException("Param appHandler cannot be null");
        }

        //klog.info("Setting Application Handler: " + appHandler.getClass());
        kAppHandler = appHandler;

        //TraceUtils.showTrace();

        if (appHandler.getStatusBar() != null) {
            XLogger.addAppender(appHandler.getStatusBar());
        }

    }

    private static void _check() {
        if (kAppHandler == null) {
            // Dont because that could trigger a recursive call
            //Application.getWindowManager().showError("No Application handler set");
            TraceUtils.showTrace();
            // @todo return a default handler
            throw new IllegalStateException("No Application handler set yet: " + kAppHandler);
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

    public static WindowManager getWindowManager() throws HeadlessException {
        _check();
        return kAppHandler.getWindowManager();
    }

    public interface Handler {

        public StatusBar getStatusBar() throws HeadlessException;

        public ToolManager getToolManager();

        public FileManager getFileManager();

        public VdbManager getVdbManager();

        public WindowManager getWindowManager() throws HeadlessException;

    } // End interface Handler


} // End class Application
