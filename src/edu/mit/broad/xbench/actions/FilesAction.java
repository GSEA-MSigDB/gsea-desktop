/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.actions;

import java.io.File;

import javax.swing.Icon;

/**
 * Tagging class for actions that work off off one or more Files
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 * @see FileAction
 */
public abstract class FilesAction extends FileAction {

    protected FilesAction(String id, String name, String description, Icon icon) {
        super(id, name, description, icon);
    }
    public abstract void setFiles(File[] files);
}    // End FilesAction
