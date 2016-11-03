/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.objects.PersistentObject;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.StringTokenizer;

/**
 * AuxFilePath
 * AuxFile
 * <p/>
 * AuxOnly
 * AuxOnlyNoHash
 * <p/>
 * FullNameWithAux
 */
public class AuxUtils {

    /**
     * Privatized class constructor
     * Static methods only
     */
    private AuxUtils() {
    }

    public static String getAuxNameOnlyIncludingHash(final String path) {
        StringTokenizer tok = new StringTokenizer(path, "#");
        tok.nextToken();
        return "#" + tok.nextToken();
    }

    public static String getAuxNameOnlyNoHash(final PersistentObject pob) {
        if (pob == null) {
            throw new IllegalArgumentException("Param pob cannot be null");
        }
        return getAuxNameOnlyNoHash(pob.getName());
    }

    public static String getAuxNameOnlyNoHash(final String fullPathOrNameMaybeWithAux) {
        StringTokenizer tok = new StringTokenizer(fullPathOrNameMaybeWithAux, "#");
        String nonaux = fullPathOrNameMaybeWithAux;

        while (tok.hasMoreTokens()) { // so that we always get the very last token
            nonaux = tok.nextToken();
        }

        return nonaux;
    }

    public static String getBaseNameOnly(String fullPathOrNameMaybeWithAux) {
        StringTokenizer tok = new StringTokenizer(fullPathOrNameMaybeWithAux, "#");
        return tok.nextToken();
    }

    /**
     * either hash or @??
     *
     * @param auxfile
     * @return
     */
    public static File getBaseFileFromFullPath(String fullPathMaybeWithAux) {
        if (fullPathMaybeWithAux == null) {
            throw new IllegalArgumentException("Parameter fullPathMaybeWithAux cannot be null");
        }

        if (fullPathMaybeWithAux.length() == 0) {
            throw new IllegalArgumentException("Parameter fullPathMaybeWithAux cannot be empty");
        }

        StringTokenizer tok = new StringTokenizer(fullPathMaybeWithAux, "#@");

        File f;
        if (tok.countTokens() == 1) {
            f = new File(fullPathMaybeWithAux);
        } else {
            f = new File(tok.nextToken()); // just first token
        }

        //klog.debug("From aux string: " + fullPathMaybeWithAux + " GOT BASE FILE: " + f);
        return f;
    }

    public static File getBaseFileFromAuxFile(File auxfile) {
        return getBaseFileFromFullPath(auxfile.getPath());
    }

    public static String getBasePathFromAuxPath(String fullPathMaybeWithAux) {
        return getBaseFileFromFullPath(fullPathMaybeWithAux).getPath();
    }

    public static String getBaseStringFromAux(String auxname_or_auxpath) {
        StringTokenizer tok = new StringTokenizer(auxname_or_auxpath, "#");
        if (tok.countTokens() == 1) {
            return auxname_or_auxpath;
        } else {
            return tok.nextToken();
        }
    }

    public static boolean isAux(String aux_name_or_path) {
        StringTokenizer tok = new StringTokenizer(aux_name_or_path, "#");

        if (tok.countTokens() == 1) {
            return false;
        } else if (tok.countTokens() >= 2) {
            return true;
        } else {
            throw new RuntimeException("Cannot process files named # tokens found: " + tok.countTokens() + " aux_name_or_path>" + aux_name_or_path + "<");
        }

    }

    public static boolean isAuxFile(File file) {

        if (StringUtils.contains(file.getAbsolutePath(), "#")) {
            return true;
        }

        if (StringUtils.contains(file.getAbsolutePath(), "@")) {
            return true;
        }

        return false;
    }

} // End class AuxUtils
