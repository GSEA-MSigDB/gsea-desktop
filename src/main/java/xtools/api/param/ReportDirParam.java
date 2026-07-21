/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

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
     * @param reqd
     */
    public ReportDirParam(final boolean reqd) {
        // Default is resolved lazily from VdbManager so CLI/FX both honor Preferences
        // and never freeze a one-time path from the process working directory.
        super(OUT, OUT_ENGLISH, OUT_DESC, new File("."), reqd);
    }

    public boolean isFileBased() {
        return true;
    }

    @Override
    public Object getDefault() {
        return Application.getVdbManager().getDefaultOutputDir();
    }

    @Override
    public Object getValue() {
        if (super.getValueRaw() != null) {
            return super.getValue();
        }
        return getDefault();
    }

    // DONT rename this getName. Duh!!
    public File getAnalysisDir() {
        Object val = getValue();
        if (val == null) {
            throw new NullPointerException("Null param value. Always check isSpecified() before calling");
        }
        File dir = val instanceof File ? (File) val : new File(val.toString());
        return dir.getAbsoluteFile();
    }

    /**
     * Override so that we can add a make this my default button
     *
     * @return
     */

}    // End class AnalysisDirParam
