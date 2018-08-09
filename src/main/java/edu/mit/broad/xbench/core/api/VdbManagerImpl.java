/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.core.api;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.utils.SystemUtils;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;
import org.apache.log4j.Logger;

import java.io.File;

/**
 * @author Aravind Subramanian
 */
public class VdbManagerImpl implements VdbManager {

    private String fBuildDate;

    private static final Logger klog = XLogger.getLogger(VdbManagerImpl.class);

    /**
     * Class constructor
     */
    public VdbManagerImpl(final String buildDate) {
        //System.out.println("##### init VdbManagerImpl: " + buildDate);
        this.fBuildDate = buildDate;
    }

    // @note this is the key preference - everything else is relative to this
    // Except for the vdb tweak
    public File getRuntimeHomeDir() {
        return XPreferencesFactory.kAppRuntimeHomeDir;
    }

    public File getTmpDir() {
        return _mkdir(new File(getRuntimeHomeDir(), "tmp"));
    }

    public File getReportsCacheDir() { // @note that the reports cache dir is specific to the version.
        return _mkdir(new File(getRuntimeHomeDir(), "reports_cache_" + fBuildDate));
    }

    public File getDefaultOutputDir() {
        File pwd = SystemUtils.getPwd();
        String dn = NamingConventions.createNiceEnglishDate_for_dirs();
        return _mkdir(new File(pwd, dn));
    }

    private static File _mkdir(File dir) {
        if (dir.exists() == false) {
            boolean made = dir.mkdir();
            if (!made) {
                klog.fatal("Could not make dir: " + dir);
            } else {
                klog.info("Made Vdb dir JIT: " + dir);
            }
        }

        return dir;
    }

} // End class VdbManagerImpl

