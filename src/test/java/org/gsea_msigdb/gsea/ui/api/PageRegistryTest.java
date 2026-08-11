/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package org.gsea_msigdb.gsea.ui.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * PageRegistry does not require a live JavaFX toolkit.
 */
public class PageRegistryTest {

    @Test
    public void singletonCreatedOnce() {
        PageRegistry registry = new PageRegistry();
        int[] creates = { 0 };
        registry.registerSingleton(PageId.LOAD_DATA, () -> {
            creates[0]++;
            return new ViewPage() {
                @Override public String getTitle() { return "Load Data"; }
                @Override public String getIconResourceId() { return null; }
                @Override public javafx.scene.Node getContent() { return null; }
            };
        });
        ViewPage a = registry.get(PageId.LOAD_DATA);
        ViewPage b = registry.get(PageId.LOAD_DATA);
        assertSame(a, b);
        assertEquals(1, creates[0]);
        assertSame(a, registry.peekSingleton(PageId.LOAD_DATA));
        assertNull(registry.peekSingleton(PageId.COREMAP));
    }
}
