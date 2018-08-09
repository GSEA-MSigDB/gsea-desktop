/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.actions.ext;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.xbench.actions.ExtAction;
import edu.mit.broad.xbench.core.api.Application;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URI;

import javax.swing.Icon;

/**
 * Action to launch native os explorer
 *
 * @author Aravind Subramanian
 */
public class OsExplorerAction extends ExtAction {

    // By default, don't register the file as Recently Opened
    private boolean registerOnComplete = false;
    
    public OsExplorerAction() {
        super("OsExplorerAction", "File Explorer", "Launch FileExplorer in an External Process", 
                JarResources.getIcon("OsExplorer16.gif"), null);
    }

    public OsExplorerAction(String name, String description, Icon icon, File path) {
        super("OsExplorerAction", name, description, icon, path);
    }

    public OsExplorerAction(File file) {
        this();
        setPath(file);
    }

    public void setRegisterOnComplete() {
        registerOnComplete = true;
    }
    
    public void actionPerformed(ActionEvent evt) {

        File file = getPath();
        if (file == null) return;
        try {
            File dir = (file.isDirectory()) ? file : file.getParentFile();

            URI outputDirURI = dir.toURI();
            // we need to add an (empty) authority designator for compatibility with all platforms
            // (mac requires an authority field in file URIs, windows does not)
            // the resulting URI will have the form "file://<absolute path>"
            outputDirURI = new URI(outputDirURI.getScheme(), "", outputDirURI.getPath(), null, null);
            Desktop.getDesktop().browse(outputDirURI);
            
            // We don't register the files as Recently Opened by default, doing it just for a couple of cases.
            // TODO: evaluate if this is necessary at all.
            if (registerOnComplete) Application.getFileManager().registerRecentlyOpenedURL(dir.getPath());
        } catch (Throwable t) {
            Application.getWindowManager().showError("Trouble launching File Explorer on path '"
                    + file.getPath() + "'", t);
        }
    }
}    // End OsExplorerAction
