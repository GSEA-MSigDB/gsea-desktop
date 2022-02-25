/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.logging;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;

import edu.mit.broad.xbench.prefs.XPreferencesFactory;

/**
 * JUL FileHandler extension.  Its only purpose is to make sure that the gsea_home directory exists 
 * before any log files are created since apparently JUL can't handle that on its own.
 * 
 * We handle the pattern differently than in the superclass, in that we always want the log file in 
 * the gsea_home and so we make sure that directory exists.
 */
public class LogFileHandler extends FileHandler {
    // Grab a static reference so that the XPreferencesFactory class will be loaded and its
    // static initialization evaluated.  Make a (redundant) check that the dir exists so that
    // the JVM doesn't optimize away this reference.
    private static File GSEA_HOME = XPreferencesFactory.kAppRuntimeHomeDir;
    static { if (!GSEA_HOME.exists()) { GSEA_HOME.mkdirs(); } }
    
    public LogFileHandler() throws IOException, SecurityException { super(); }
    public LogFileHandler(String pattern) throws IOException, SecurityException { super(pattern); }
    public LogFileHandler(String pattern, boolean append) throws IOException, SecurityException { super(pattern, append); }
    public LogFileHandler(String pattern, int limit, int count) throws IOException, SecurityException { super(pattern, limit, count); }
    public LogFileHandler(String pattern, int limit, int count, boolean append) throws IOException, SecurityException { super(pattern, limit, count, append); }
}
