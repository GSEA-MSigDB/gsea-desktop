/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.utils;

import java.util.StringTokenizer;

/**
 * Class instantiation, class type checking and java.lang.reflec related utiltities.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ClassUtils {

    /**
     * @return The short name of clazz by stripping off the package name.
     *         For example: the short name of javax.swing.JTable.class
     *         is "JTable"
     */
    public static String shorten(Class clazz) {
        return shorten(clazz.getName());
    }

    public static String shorten(String name) {

        StringTokenizer tok = new StringTokenizer(name, ".");
        int cnt = tok.countTokens();

        for (int i = 0; i < cnt - 1; i++) {
            tok.nextToken();
        }

        return tok.nextToken();
    }

    /**
     * Turns edu.mit.broad.genome.objects.Dataset into Dataset
     * MP if no period (.") found, returns pkgname specified untouched
     *
     * @param pkgname
     * @return
     */
    public static String packageName2ClassName(String pkgname) {

        if (pkgname == null) {
            throw new IllegalArgumentException("Param pkgname cannot be null");
        }


        if (pkgname.indexOf(".") == -1) {
            return pkgname; // untouched
        }

        StringTokenizer tok = new StringTokenizer(pkgname, ".");
        String n = null;
        while (tok.hasMoreTokens()) {
            n = tok.nextToken();
        }

        return n;

    }

}    // End ClassUtils
