/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package org.gsea_msigdb.gsea.runtime;

/**
 * Thread-local binding of the active tool {@code runId} for log routing.
 */
public final class RunContext {

    private static final ThreadLocal<String> BOUND_RUN_ID = new ThreadLocal<>();

    private RunContext() {
    }

    public static void bind(String runId) {
        if (runId != null) {
            BOUND_RUN_ID.set(runId);
        }
    }

    public static void unbind() {
        BOUND_RUN_ID.remove();
    }

    public static String currentRunId() {
        return BOUND_RUN_ID.get();
    }
}
