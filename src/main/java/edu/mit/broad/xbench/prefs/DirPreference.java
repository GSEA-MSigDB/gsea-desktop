/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.prefs;

import edu.mit.broad.genome.swing.fields.GDirFieldPlusChooser;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;

import java.io.File;

/**
 * @author Aravind Subramanian
 */
public class DirPreference extends AbstractPreference {

    private GDirFieldPlusChooser fChooser;

    /**
     * @param name
     * @param desc
     * @param def
     */
    protected DirPreference(String name, String desc, File def, boolean isDebug, boolean needsRestart) {
        super(name, desc, def, isDebug, needsRestart);
    }

    public Object getValue() {
        String s = kPrefs.get(getName(), ((File) getDefault()).getPath());
        return new File(s);
    }

    public File getDir(boolean makeItIfItDoesntAlreadyExist) {
        File f = (File) getValue();
        if ((f.exists() == false) && (makeItIfItDoesntAlreadyExist)) {
            boolean success = f.mkdir();
            klog.info("Made pref dir: {} status: {}", f, success);
        }
        return (File) getValue();
    }

    public void setValue(Object value) throws Exception {
        kPrefs.put(getName(), value.toString());
    }

    public GFieldPlusChooser getSelectionComponent() {

        if (fChooser == null) {
            fChooser = new GDirFieldPlusChooser();
            fChooser.setValue(getValue());
        }

        fChooser.setValue(getValue());
        return fChooser;

    }

    public void setValueOfPref2SelectionComponentValue() {
        if (fChooser != null) {
            super._setValueOfPref2SelectionComponentValue(fChooser.getValue());
        }
    }
}
