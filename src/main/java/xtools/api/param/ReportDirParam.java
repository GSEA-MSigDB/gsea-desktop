/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.swing.fields.GDirFieldPlusChooser;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;
import edu.mit.broad.xbench.core.api.Application;

import java.io.File;

/**
 * To capture the dir in which to place the file(s) produced by an analysis
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ReportDirParam extends DirParam {

    /**
     * Class constructor
     *
     * @param def
     * @param reqd
     */
    public ReportDirParam(final boolean reqd) {
        super(OUT, OUT_ENGLISH, OUT_DESC, Application.getVdbManager().getDefaultOutputDir(), reqd);
    }

    public boolean isFileBased() {
        return true;
    }

    // DONT rename this getName. Duh!!
    public File getAnalysisDir() {

        Object val = super.getValue();

        if (val == null) {
            throw new NullPointerException("Null param value. Always check isSpecified() before calling");
        }

        return (File) getValue();
    }

    /**
     * Override so that we can add a make this my default button
     *
     * @return
     */
    public GFieldPlusChooser getSelectionComponent() {

        if (fChooser == null) {
            fChooser = new GDirFieldPlusChooser();
            if (getValue() != null) {
                // set the value to specified
                fChooser.setValue(getValue());

            } else { // cancelled
                fChooser.setValue(getDefault());
            }

            ParamHelper.addDocumentListener(fChooser.getTextField(), this);

        }

        return fChooser;
    }

}    // End class AnalysisDirParam
