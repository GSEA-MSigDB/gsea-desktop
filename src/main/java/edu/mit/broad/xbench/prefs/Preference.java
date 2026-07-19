/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.xbench.prefs;

import java.util.prefs.Preferences;

/**
 * Persistent preference value (no UI binding).
 */
public interface Preference {

    String PATHNAME = Preference.class.toString();

    Preferences kPrefs = Preferences.userRoot().node(PATHNAME);

    String getName();

    String getDesc();

    Object getValue();

    void setValue(Object newValue) throws Exception;
}
