/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.utils;

import java.io.File;

/**
 * Utility methods related to the System class
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class SystemUtils {

    public static final String OS_NAME = System.getProperty("os.name");

    private static Boolean kIsLinuxOrUnix;

    public static boolean isLinuxOrUnix() {
        //TraceUtils.showTrace();
        if (kIsLinuxOrUnix == null) {
            String osName = OS_NAME.toLowerCase();
            if (osName.indexOf("unix") != -1 || osName.indexOf("linux") != -1 || osName.equalsIgnoreCase("OSF1")) {
                kIsLinuxOrUnix = Boolean.TRUE;
            } else {
                kIsLinuxOrUnix = Boolean.FALSE;
            }
        }

        return kIsLinuxOrUnix.booleanValue();
    }

    public static boolean isMac() {
        // TODO: Inline this
        return org.apache.commons.lang3.SystemUtils.IS_OS_MAC_OSX;
    }

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

    public static String getJavaPath() {
        //return getJavaHome() + File.pathSeparator + "bin" + File.pathSeparator + "java";
        return getJavaHome() + File.separator + "bin" + File.separator + "java";
    }

    public static String getJavaHome() {
        return System.getProperty("java.home").trim();
    }

    public static String getClassPath() {
        return System.getProperty("java.class.path").trim();
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
