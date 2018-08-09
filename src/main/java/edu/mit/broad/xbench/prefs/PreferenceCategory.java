/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.prefs;

/**
 * @author Aravind Subramanian
 */
class PreferenceCategory {

    private Preference[] fPrefs;

    PreferenceCategory(Preference[] prefs) {
        this.fPrefs = new Preference[prefs.length]; // shallow clone
        for (int i = 0; i < prefs.length; i++) {
            this.fPrefs[i] = prefs[i];
        }
    }

}
