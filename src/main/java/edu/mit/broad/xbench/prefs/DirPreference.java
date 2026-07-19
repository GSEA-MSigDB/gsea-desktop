/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.xbench.prefs;

import java.io.File;

public class DirPreference extends AbstractPreference {

    protected DirPreference(String name, String desc, File def, boolean isDebug, boolean needsRestart) {
        super(name, desc, def, isDebug, needsRestart);
    }

    public Object getValue() {
        String s = kPrefs.get(getName(), ((File) getDefault()).getPath());
        return new File(s);
    }

    public File getDir(boolean makeItIfItDoesntAlreadyExist) {
        File f = (File) getValue();
        if ((!f.exists()) && makeItIfItDoesntAlreadyExist) {
            boolean success = f.mkdir();
            klog.info("Made pref dir: {} status: {}", f, success);
        }
        return (File) getValue();
    }

    public void setValue(Object value) throws Exception {
        kPrefs.put(getName(), value.toString());
    }
}
