/*
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xtools.api;

import edu.mit.broad.genome.NotImplementedException;
import edu.mit.broad.xbench.core.api.*;

/**
 * Minimal Application.Handler for headless / CLI tool runs.
 */
public class XToolsApplication implements Application.Handler {

    private static final VdbManager fVdbmanager = new VdbManagerImpl("foo");

    public XToolsApplication() {
    }

    public ToolManager getToolManager() {
        throw new NotImplementedException();
    }

    public FileManager getFileManager() {
        throw new NotImplementedException();
    }

    public VdbManager getVdbManager() {
        return fVdbmanager;
    }

    public WindowManager getWindowManager() {
        return new WindowManager() {
            @Override
            public void showError(String msg) {
                System.err.println(msg);
            }

            @Override
            public void showError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void showError(edu.mit.broad.genome.Errors errors) {
                System.err.println(errors.getErrors(false));
            }

            @Override
            public void showError(String msg, Throwable t) {
                System.err.println(msg);
                t.printStackTrace();
            }

            @Override
            public boolean showConfirm(String msg) {
                return true;
            }

            @Override
            public boolean showConfirm(String title, String msg) {
                return true;
            }

            @Override
            public void showMessage(String msg) {
                System.out.println(msg);
            }

            @Override
            public void showMessage(String title, String msg) {
                System.out.println(title + ": " + msg);
            }
        };
    }
}
