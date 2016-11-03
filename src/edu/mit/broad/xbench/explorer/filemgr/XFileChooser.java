/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.explorer.filemgr;

import java.io.File;

/**
 * 1) custom icons
 * 2) keep ext stuff confinded here
 * <p/>
 * ONLY for files -- dont use for directories
 *
 * @see DirChooser for directories
 */
public interface XFileChooser {

    public File getSelectedFile();

    public File[] getSelectedFiles();

    public boolean showOpenDialog();

} // End XFileChooser
