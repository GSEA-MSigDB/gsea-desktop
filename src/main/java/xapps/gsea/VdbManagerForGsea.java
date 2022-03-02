/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xapps.gsea;

import edu.mit.broad.genome.Conf;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.TraceUtils;
import edu.mit.broad.xbench.core.api.VdbManager;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author Aravind Subramanian
 */
public class VdbManagerForGsea implements VdbManager {

    private String fCacheDate;

    private static final Logger klog = LoggerFactory.getLogger(VdbManagerForGsea.class);

    public VdbManagerForGsea(final String buildDate) {
        this.fCacheDate = buildDate;
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
        return _mkdir(new File(getRuntimeHomeDir(), "reports_cache_" + fCacheDate));
    }

    public File getDefaultOutputDir() {
        File out = _mkdir(XPreferencesFactory.kDefaultReportsOutputDir.getDir(false));
        String dn = NamingConventions.createNiceEnglishDate_for_dirs();
        return _mkdir(new File(out, dn));
    }

    private static File _mkdir(final File dir) {
        if (dir.exists() == false) {
            boolean made = dir.mkdir();
            if (!made) {
                if (Conf.isDebugMode()) {
                    TraceUtils.showTrace();
                }
                klog.error("Could not make dir: {}", dir);
            } else {
                klog.info("Made Vdb dir: {}", dir);
            }
        }

        return dir;
    }
}
