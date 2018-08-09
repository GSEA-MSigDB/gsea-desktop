/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.core.api;

import java.io.File;

/**
 * Class that defines databases that are widely available
 */
public interface VdbManager {

    public File getRuntimeHomeDir();

    public File getTmpDir(); // must not be lazily made -- too many things depend on it

    public File getReportsCacheDir();

    public File getDefaultOutputDir();

} // End interface VdbManager
