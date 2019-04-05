/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.utils;

import java.io.File;

/**
 * Utility methods related to the System class
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class SystemUtils {

    public static String getProperty(String key, boolean caseSensitive) {
        if (caseSensitive) {
            return System.getProperty(key);
        }

        String s = System.getProperty(key);
        if (s == null || s.length() == 0) {
            s = System.getProperty(key.toLowerCase());
        }

        if (s == null || s.length() == 0) {
            s = System.getProperty(key.toUpperCase());
        }

        return s;
    }

    public static String getUserName() {
        return System.getProperty("user.name");
    }

    public static File getUserHome() {
        return new File(System.getProperty("user.home"));
    }

    public static File getPwd() {
        return new File(System.getProperty("user.dir"));
    }

    public static File getTmpDir() {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    // @note that the -D commands are case sensitive
    // luckilly the boolean parsing isnt
    public static boolean isHeadless() {
        return isPropertyTrue("java.awt.headless");
    }

    public static boolean isPropertyTrue(String prpName) {
        String p = getProperty(prpName, false);
        return Boolean.valueOf(p).booleanValue();
    }

    public static boolean isPropertyDefined(String prpName) {
        String p = getProperty(prpName, false);
        return p != null && p.trim().length() != 0;
    }

}    // End SystemUtils
