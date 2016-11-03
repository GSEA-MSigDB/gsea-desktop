/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.uiutil;

import javax.swing.*;

/**
 * @author Joshua Gould
 */
public class OS {

    private OS() {
    }

    public static boolean isMac() {
        return System.getProperty("mrj.version") != null
                && javax.swing.UIManager.getSystemLookAndFeelClassName()
                .equals(
                        javax.swing.UIManager.getLookAndFeel()
                                .getClass().getName());
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase()
                .startsWith("windows");
    }

    public static void setLookAndFeel() {
        if (!OS.isMac()) {
            try {
                UIManager.setLookAndFeel(UIManager
                        .getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
