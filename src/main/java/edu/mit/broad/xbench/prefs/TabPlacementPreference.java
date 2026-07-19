/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.xbench.prefs;

/**
 * Legacy tab placement preference (TOP=1 matches historical Swing JTabbedPane.TOP).
 */
public class TabPlacementPreference extends IntPreference {

    public static final int TOP = 1;
    public static final int BOTTOM = 3;
    public static final int LEFT = 2;
    public static final int RIGHT = 4;

    protected TabPlacementPreference(String name, String desc, int def) {
        super(name, desc, def, false, true);
    }
}
