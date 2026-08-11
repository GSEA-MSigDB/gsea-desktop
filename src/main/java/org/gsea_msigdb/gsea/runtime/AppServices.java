/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package org.gsea_msigdb.gsea.runtime;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import edu.mit.broad.genome.parsers.ObjectCache;
import edu.mit.broad.xbench.core.api.FileManager;
import edu.mit.broad.xbench.core.api.ToolManager;
import edu.mit.broad.xbench.core.api.VdbManager;
import edu.mit.broad.xbench.core.api.WindowManager;
import xapps.gsea.fx.jobs.JobRuntime;

/**
 * Injectible application services for the FX shell (and tests).
 * Presentation code uses {@link #require()} (bound by the shell); CLI/tools keep
 * {@link edu.mit.broad.xbench.core.api.Application} as the headless bridge.
 */
public final class AppServices {

    private static final AtomicReference<AppServices> CURRENT = new AtomicReference<>();

    private final WindowManager dialogs;
    private final FileManager files;
    private final VdbManager vdb;
    private final ToolManager tools;
    private final JobRuntime jobs;
    private final WorkspaceSession session;

    public AppServices(WindowManager dialogs, FileManager files, VdbManager vdb, ToolManager tools,
            JobRuntime jobs, WorkspaceSession session) {
        this.dialogs = Objects.requireNonNull(dialogs, "dialogs");
        this.files = Objects.requireNonNull(files, "files");
        this.vdb = Objects.requireNonNull(vdb, "vdb");
        this.tools = Objects.requireNonNull(tools, "tools");
        this.jobs = Objects.requireNonNull(jobs, "jobs");
        this.session = Objects.requireNonNull(session, "session");
    }

    public WindowManager dialogs() {
        return dialogs;
    }

    public FileManager files() {
        return files;
    }

    public VdbManager vdb() {
        return vdb;
    }

    public ToolManager tools() {
        return tools;
    }

    public JobRuntime jobs() {
        return jobs;
    }

    public WorkspaceSession session() {
        return session;
    }

    /** Active object cache for the bound GUI session. */
    public ObjectCache cache() {
        return session.cache();
    }

    /** Bind as the GUI process services (shell startup). */
    public void bindAsCurrent() {
        CURRENT.set(this);
    }

    public void unbindIfCurrent() {
        CURRENT.compareAndSet(this, null);
    }

    public static AppServices current() {
        return CURRENT.get();
    }

    public static AppServices require() {
        AppServices s = CURRENT.get();
        if (s == null) {
            throw new IllegalStateException("AppServices is not bound (GUI shell not started)");
        }
        return s;
    }
}
