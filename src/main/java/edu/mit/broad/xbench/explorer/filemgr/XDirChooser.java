/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.explorer.filemgr;

import java.io.File;

/**
 * Simplified (both in code and UI) & specialized directory chooser
 * Based on the IntelliJ UI.
 */
public interface XDirChooser {

    public void setCurrentLocation(final String path);

    public boolean show();

    public File getSelectedDir();

    public void setApproveButtonText(String txt);

    public void resetState();

} // End XDirChooser
