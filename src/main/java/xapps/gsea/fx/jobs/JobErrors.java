/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.jobs;

import edu.mit.broad.xbench.tui.JobState;
import org.gsea_msigdb.gsea.runtime.AppServices;

/**
 * Central titles and dialogs for job parameter / execution errors.
 */
public final class JobErrors {

    public static final String INVALID_PARAM_TITLE = "One or more parameter(s) were not specified";
    public static final String EXECUTION_ERROR_TITLE = "Tool execution error";

    private JobErrors() {
    }

    public static String titleFor(JobRecord job) {
        if (job == null) {
            return EXECUTION_ERROR_TITLE;
        }
        if (job.getState() == JobState.INVALID_PARAM) {
            return INVALID_PARAM_TITLE;
        }
        return EXECUTION_ERROR_TITLE;
    }

    /** Shows an error dialog for INVALID_PARAM or ERROR; no-op otherwise. */
    public static void showIfNeeded(JobRecord job) {
        if (job == null) {
            return;
        }
        JobState state = job.getState();
        if (state != JobState.INVALID_PARAM && state != JobState.ERROR) {
            return;
        }
        Throwable err = job.getLastError();
        AppServices.require().dialogs().showError(
                titleFor(job),
                err != null ? err : new IllegalStateException(titleFor(job)));
    }

    /** Start-path auto-dialog: INVALID_PARAM only. */
    public static void showParamErrorIfNeeded(JobRecord job) {
        if (job == null || !job.isParamError()) {
            return;
        }
        Throwable err = job.getLastError();
        AppServices.require().dialogs().showError(
                INVALID_PARAM_TITLE,
                err != null ? err : new IllegalStateException("Invalid parameters"));
    }
}
