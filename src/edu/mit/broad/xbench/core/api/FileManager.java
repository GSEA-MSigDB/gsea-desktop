/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.core.api;

import edu.mit.broad.xbench.explorer.filemgr.XDirChooser;
import edu.mit.broad.xbench.explorer.filemgr.XFileChooser;

import java.awt.*;
import java.io.File;

/**
 * Class that defines File related, application wide API's
 * File choosers, dir choosers & history mechanisms.
 */
public interface FileManager {

    // we need these in addition to the automatic file chooser based mechanism because
    // files can be loaded in in other ways - for example double click of a jlist
    public void registerRecentlyOpenedDir(final File dir);

    public void registerRecentlyOpenedFile(final File addFile);

    public void registerRecentlyOpenedURL(final String url);

    public File[] getRecentDirs();

    public XStore getRecentFilesStore();

    public XFileChooser getFileChooser() throws HeadlessException;

    public XFileChooser getFileChooser(final javax.swing.filechooser.FileFilter[] filts) throws HeadlessException;

    public XDirChooser getDirChooser(final String approveButtonTxt) throws HeadlessException;

} // End class FileManager
