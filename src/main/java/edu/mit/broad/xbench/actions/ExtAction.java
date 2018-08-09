/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.actions;

import java.io.File;

import javax.swing.Icon;

/**
 * Base action for actions that triggers an external process on a File.
 * For example, opening a file in Microsoft Excel.
 *
 * @author Aravind Subramanian
 * @author David Eby
 */
public abstract class ExtAction extends XAction {

    private File fPath;

    protected ExtAction(String id, String name, String description, Icon icon, File path) {
        super(id, name, description, icon);
        fPath = path;
    }
    
    /**
     * @param file The File to run the External Action on
     */
    public void setPath(File path) {
        this.fPath = path;
    }

    public File getPath() {
        return fPath;
    }

}    // End ExtAction
