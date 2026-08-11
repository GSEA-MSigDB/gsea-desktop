/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package org.gsea_msigdb.gsea.runtime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import edu.mit.broad.genome.parsers.ObjectCache;
import edu.mit.broad.genome.parsers.ParserFactory;

public class WorkspaceSessionTest {

    @AfterEach
    public void tearDown() {
        WorkspaceSession cur = WorkspaceSession.currentOrNull();
        if (cur != null) {
            cur.unbindIfCurrent();
        }
    }

    @Test
    public void boundSessionIsUsedByParserFactory() {
        ObjectCache cache = new ObjectCache();
        WorkspaceSession session = new WorkspaceSession(cache);
        session.bindAsCurrent();
        assertSame(cache, ParserFactory.getCache());
        assertSame(session, WorkspaceSession.require());
    }

    @Test
    public void unboundFallsBackToDefaultCache() {
        ObjectCache before = ParserFactory.getCache();
        assertNotNull(before);
        assertTrue(WorkspaceSession.currentOrNull() == null);
    }
}
