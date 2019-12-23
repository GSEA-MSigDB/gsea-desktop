/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.actions.ext;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.xbench.actions.ExtAction;
import edu.mit.broad.xbench.core.api.Application;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URI;

import org.apache.commons.io.FilenameUtils;

/**
 * Action to launch a file in the Desktop Browser
 *
 * @author David Eby
 */
public class FileBrowserAction extends ExtAction {
    
    public FileBrowserAction() {
        super("FileBrowserAction", "Launch File", "Launch a File in an External Process", null, null);
    }

    public FileBrowserAction(File path) {
        this();
        setPath(path);
    }

    @Override
    public void setPath(File path) {
        super.setPath(path);
        if (path == null) return;
        
        // Set the action name & icon for certain well-known file types
        String ext = FilenameUtils.getExtension(path.getName());
        if (ext.equalsIgnoreCase(Constants.HTML)) {
            super.putValue(NAME, "Web Browser");
            super.putValue(SMALL_ICON, JarResources.getIcon("Htm.gif"));
        }
        else if (ext.equalsIgnoreCase(Constants.CSV) || ext.equalsIgnoreCase(Constants.TSV) || ext.equalsIgnoreCase(Constants.XLS)) {
            // Note: keeping XLS here for historical jobs produce by earlier versions.  Might drop this later.
            super.putValue(NAME, "Launch in Excel");
            super.putValue(SMALL_ICON, JarResources.getIcon("Xls.gif"));
        }
    }

    public void actionPerformed(ActionEvent evt) {
        File file = getPath();
        if (file == null) return;
        try {
            URI outputDirURI = file.toURI();
            // we need to add an (empty) authority designator for compatibility with all platforms
            // (mac requires an authority field in file URIs, windows does not)
            // the resulting URI will have the form "file://<absolute path>"
            outputDirURI = new URI(outputDirURI.getScheme(), "", outputDirURI.getPath(), null, null);
            Desktop.getDesktop().browse(outputDirURI);
        } catch (Throwable t) {
            Application.getWindowManager().showError("Trouble launching File on path '"
                    + file.getPath() + "'", t);
        }
    }
}
