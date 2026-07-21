/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.xbench.core.api.Application;

import java.io.File;

/**
 * To capture the dir in which to place the file(s) produced by an analysis.
 * Default is the Preferences output folder plus today's dated subfolder
 * ({@link Application#getVdbManager()}.{@code getDefaultOutputDir()}), resolved lazily
 * so it follows Preferences and does not freeze a process working-directory path.
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
        // Empty hints: default comes from {@link #getDefault()} (prefs + dated folder).
        super(OUT, OUT_ENGLISH, OUT_DESC, reqd);
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

    @Override
    public void setValue(File dir) {
        super.setValue(dir != null ? dir.getAbsoluteFile() : null);
    }

    @Override
    public String getValueStringRepresentation(boolean full) {
        Object val = getValue();
        if (val == null) {
            return null;
        }
        File file = val instanceof File ? (File) val : new File(val.toString());
        // Always absolute so FX/CLI never treat a bare dated folder name as relative to cwd.
        return file.getAbsolutePath();
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

}    // End class AnalysisDirParam
