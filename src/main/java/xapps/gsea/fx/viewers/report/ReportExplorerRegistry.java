/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import java.util.EnumMap;
import java.util.Map;

/**
 * Maps {@link ReportKind} to a {@link ReportExplorer}.
 */
public final class ReportExplorerRegistry {

    private static final Map<ReportKind, ReportExplorer> EXPLORERS = new EnumMap<>(ReportKind.class);
    private static final ReportExplorer FALLBACK = new GenericFilesExplorer();

    static {
        GseaReportExplorer gsea = new GseaReportExplorer();
        EXPLORERS.put(ReportKind.GSEA, gsea);
        EXPLORERS.put(ReportKind.GSEA_PRERANKED, gsea);
        EXPLORERS.put(ReportKind.SSGSEA, new SsGseaReportExplorer());
        EXPLORERS.put(ReportKind.LEADING_EDGE, new LeadingEdgeReportExplorer());
        EXPLORERS.put(ReportKind.COLLAPSE_DATASET, new CollapseDatasetReportExplorer());
        EXPLORERS.put(ReportKind.CHIP2CHIP, new Chip2ChipReportExplorer());
        EXPLORERS.put(ReportKind.UNKNOWN, FALLBACK);
    }

    private ReportExplorerRegistry() {
    }

    public static ReportExplorer forKind(ReportKind kind) {
        return EXPLORERS.getOrDefault(kind != null ? kind : ReportKind.UNKNOWN, FALLBACK);
    }
}
