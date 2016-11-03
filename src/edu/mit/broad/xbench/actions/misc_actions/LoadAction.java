/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.actions.misc_actions;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.xbench.actions.FileObjectAction;
import edu.mit.broad.xbench.core.Widget;
import edu.mit.broad.xbench.core.api.Application;

import javax.swing.*;

import java.io.File;

/**
 * force reloading a file, ignoring if it has been cached or not
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */

public class LoadAction extends FileObjectAction {

    private Object fFileOrObject;

    private static final Icon RELOAD_ICON = JarResources.getIcon("Refresh16_2.gif");

    /**
     * Class constructor
     *
     * @param name
     * @param id
     * @param desc
     */
    public LoadAction() {
        super("ForceReloadAction", "Force data reload", 
                "Reload data from source file overwriting the cache", 
                RELOAD_ICON);
    }

    public void setFile(File file) {
        this.fFileOrObject = file;
    }

    public void setObject(Object obj) {
        this.fFileOrObject = obj;
    }

    public Widget getWidget() {

        // the load is non-blocking so better to use a null widget mechanism
        Widget widget = null;

        try {

            if (fFileOrObject != null) {
                File sourceFile;

                if (fFileOrObject instanceof File) {
                    sourceFile = (File) fFileOrObject;
                } else {
                    sourceFile = ParserFactory.getCache().getSourceFile(fFileOrObject);
                }

                //log.debug("loading data from: " + sourceFile);
                // NOTE: this is the sole case where we bypass the cache on a general read.
                // TODO: Review whether there's another route for cache bypass.
                PersistentObject pob = ParserFactory.read(sourceFile, false);
                if (pob != null) {
                    Application.getWindowManager().showMessage("<html><body><b>Successfully reloaded: " + pob.getName() + "</b><br>From file: " + sourceFile + "</body></html>");
                } else {
                    log.info("Cancelled data import!!");
                }

            } else {
                Application.getWindowManager().showMessage("No file or object to load/reload was specified");
            }

        } catch (Throwable t) {
            Application.getWindowManager().showError("Error loading data from file", t);
        }

        return widget;
    }
}    // End ForceReloadAction
