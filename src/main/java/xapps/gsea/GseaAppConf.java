/*
 * Copyright (c) 2003-2020 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xapps.gsea;

/**
 * Configuration and utils common to all xapplications.
 */
public class GseaAppConf {

    // @maint known extensions
    private static String[] KNOWN_FILE_EXTS = new String[]{"res", "gct", "pcl", "txt", "grp", "gmx", "gmt", "cls", "rnk", "chip"}; // @maint

    public static GseaFileFilter createGseaFileFilter() {

        StringBuffer buf = new StringBuffer("GSEA supported file types [");
        for (int i = 0; i < KNOWN_FILE_EXTS.length; i++) {
            buf.append(KNOWN_FILE_EXTS[i]);
            if (i != KNOWN_FILE_EXTS.length - 1) {
                buf.append(",");
            }
        }

        buf.append(']');

        return new GseaFileFilter(KNOWN_FILE_EXTS, buf.toString());
    }
}