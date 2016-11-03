/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.prefs;

import edu.mit.broad.genome.swing.choosers.GFileFieldPlusChooser;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;

import java.io.File;

/**
 * @author Aravind Subramanian
 */
public class FilePreference extends AbstractPreference {

    private GFileFieldPlusChooser fChooser;

    /**
     * @param name
     * @param desc
     * @param def
     */
    protected FilePreference(final String name,
                             final String desc,
                             final File def,
                             final boolean isDebug,
                             final boolean needsRestart) {

        super(name, desc, def, isDebug, needsRestart);
    }

    public Object getValue() {
        String s = kPrefs.get(getName(), ((File) getDefault()).getPath());
        return new File(s);
    }

    public File getFile() {
        return (File) getValue();
    }

    public void setValue(Object value) throws Exception {
        kPrefs.put(getName(), value.toString());
    }

    public GFieldPlusChooser getSelectionComponent() {
        if (fChooser == null) {
            fChooser = new GFileFieldPlusChooser();
        }

        //klog.debug("Getting component");
        fChooser.setValue(getValue());
        return fChooser;
    }

    public void setValueOfPref2SelectionComponentValue() {
        if (fChooser != null) {
            super._setValueOfPref2SelectionComponentValue(fChooser.getValue());
        }
    }

} // End FilePreference