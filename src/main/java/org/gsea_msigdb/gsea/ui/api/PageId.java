/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package org.gsea_msigdb.gsea.ui.api;

/**
 * Stable id for a {@link PageRegistry}-backed workspace page (tools and feature viewers).
 * Shell chrome (home, preferences, jobs, console) is not registered here.
 */
public final class PageId {

    public static final PageId LOAD_DATA = of("data.load");
    public static final PageId ANALYSIS_HISTORY = of("analysis.history");
    public static final PageId TOOLS_GSEA = of("tools.gsea");
    public static final PageId TOOLS_GSEA_PRERANKED = of("tools.gsea_preranked");
    public static final PageId TOOLS_SSGSEA = of("tools.ssgsea");
    public static final PageId TOOLS_COLLAPSE = of("tools.collapse");
    public static final PageId TOOLS_CHIP2CHIP = of("tools.chip2chip");
    public static final PageId LEADING_EDGE = of("tools.leading_edge");
    public static final PageId ENRICHMENT_MAP = of("viewer.enrichment_map");
    public static final PageId COREMAP = of("viewer.coremap");

    private final String id;

    private PageId(String id) {
        this.id = id;
    }

    public static PageId of(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("page id required");
        }
        return new PageId(id.trim());
    }

    public String id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PageId other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}
