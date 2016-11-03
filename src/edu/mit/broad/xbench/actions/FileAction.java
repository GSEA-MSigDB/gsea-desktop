/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.actions;

import java.io.File;

import javax.swing.Icon;

/**
 * Tagging class for actions that work off off ONE File
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 * @see FilesAction
 */
public abstract class FileAction extends WidgetAction {

    protected FileAction(String id, String name, String description, Icon icon) {
        super(id, name, description, icon);
    }

    
    public abstract void setFile(File file);
}    // End FileAction
