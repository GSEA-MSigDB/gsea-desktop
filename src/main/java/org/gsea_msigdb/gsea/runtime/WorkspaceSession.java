/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package org.gsea_msigdb.gsea.runtime;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import edu.mit.broad.genome.parsers.ObjectCache;

/**
 * Explicit GUI session owning the active object cache.
 * When bound, {@link edu.mit.broad.genome.parsers.ParserFactory#getCache()} delegates here.
 */
public final class WorkspaceSession {

    private static final AtomicReference<WorkspaceSession> CURRENT = new AtomicReference<>();

    private final ObjectCache cache;

    public WorkspaceSession() {
        this(new ObjectCache());
    }

    public WorkspaceSession(ObjectCache cache) {
        this.cache = Objects.requireNonNull(cache, "cache");
    }

    public ObjectCache cache() {
        return cache;
    }

    /** Bind as the process-wide GUI session (shell startup). */
    public void bindAsCurrent() {
        CURRENT.set(this);
    }

    public void unbindIfCurrent() {
        CURRENT.compareAndSet(this, null);
    }

    public static WorkspaceSession currentOrNull() {
        return CURRENT.get();
    }

    public static WorkspaceSession require() {
        WorkspaceSession s = CURRENT.get();
        if (s == null) {
            throw new IllegalStateException("WorkspaceSession is not bound");
        }
        return s;
    }
}
