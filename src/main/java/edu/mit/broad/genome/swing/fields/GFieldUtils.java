/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing.fields;

import edu.mit.broad.genome.parsers.AuxUtils;

import java.awt.*;
import java.io.File;
import java.util.StringTokenizer;

/**
 * @author Aravind Subramanian
 */
public class GFieldUtils {

    public static final Color GOOD_COLOR = Color.blue;
    public static final Color ERROR_COLOR = Color.red;

    /**
     * Needs to accoint for that sometimes the field contains multiple paths
     * separated by commas.
     *
     * @param text
     * @return
     */
    public static Color getFileFieldColor(String pathOrPaths) {
        if (pathOrPaths == null) {
            return ERROR_COLOR;
        }

        if (pathOrPaths.startsWith("http") || pathOrPaths.startsWith("ftp") || pathOrPaths.startsWith("gseaftp")) {
            return GOOD_COLOR;
        }

        pathOrPaths = pathOrPaths.trim();

        if (pathOrPaths.indexOf(',') == -1) {
            return _getColor(pathOrPaths);
        }

        StringTokenizer tok = new StringTokenizer(pathOrPaths, ",");
        int cnt = tok.countTokens();
        for (int i = 0; i < cnt; i++) {
            if (isGoodPath(tok.nextToken()) == false) {
                return ERROR_COLOR;
            }
        }

        return GOOD_COLOR;
    }

    public static Color _getColor(String path) {
        if (isGoodPath(path)) {
            return GOOD_COLOR;
        } else {
            return ERROR_COLOR;
        }
    }

    public static boolean isGoodPath(String pathMaybeWithAux) {
        if (pathMaybeWithAux == null || pathMaybeWithAux.length() == 0) {
            return false;
        }

        if (pathMaybeWithAux.toLowerCase().startsWith("http") ||
                pathMaybeWithAux.toLowerCase().startsWith("www") ||
                pathMaybeWithAux.toLowerCase().startsWith("ftp") ||
                pathMaybeWithAux.toLowerCase().startsWith("gseaftp")) {
            return true;
        }

        File file = AuxUtils.getBaseFileFromFullPath(pathMaybeWithAux);
        if (file.exists()) {
            return true;
        } else {
            return false;
        }
    }

} // End GFieldUtils
