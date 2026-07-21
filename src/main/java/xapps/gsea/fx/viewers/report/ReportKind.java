/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import java.io.File;
import java.util.Locale;

import edu.mit.broad.genome.reports.api.Report;

/**
 * Tool kind for a finished analysis report folder, used to pick a native explorer.
 */
public enum ReportKind {

    GSEA("GSEA"),
    GSEA_PRERANKED("GSEA Preranked"),
    SSGSEA("ssGSEA"),
    LEADING_EDGE("Leading Edge"),
    COREMAP("CoreMap"),
    COLLAPSE_DATASET("Collapse Dataset"),
    CHIP2CHIP("Chip2Chip"),
    UNKNOWN("Report");

    private final String displayName;

    ReportKind(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ReportKind from(Report report) {
        if (report == null) {
            return UNKNOWN;
        }
        Class<?> producer = report.getProducer();
        if (producer != null) {
            ReportKind fromClass = fromProducerName(producer.getName());
            if (fromClass != UNKNOWN) {
                return fromClass;
            }
            // Simple name covers odd ClassLoaders where getName() is not a FQN.
            fromClass = fromProducerName(producer.getSimpleName());
            if (fromClass != UNKNOWN) {
                return fromClass;
            }
        }
        File dir = report.getReportDir();
        if (dir != null) {
            ReportKind fromName = fromFolderName(dir.getName());
            if (fromName != UNKNOWN) {
                return fromName;
            }
            return fromArtifacts(dir);
        }
        if (report.getName() != null) {
            return fromFolderName(report.getName());
        }
        return UNKNOWN;
    }

    static ReportKind fromProducerName(String name) {
        if (name == null || name.isBlank()) {
            return UNKNOWN;
        }
        String n = name.toLowerCase(Locale.ROOT);
        if (n.endsWith(".gseapreranked") || n.equals("gseapreranked")) {
            return GSEA_PRERANKED;
        }
        if (n.endsWith(".gsea") || n.equals("gsea")) {
            return GSEA;
        }
        if (n.endsWith(".ssgsea") || n.equals("ssgsea")) {
            return SSGSEA;
        }
        if (n.endsWith(".leadingedgetool") || n.equals("leadingedgetool")) {
            return LEADING_EDGE;
        }
        if (n.endsWith(".coremaptool") || n.equals("coremaptool")) {
            return COREMAP;
        }
        if (n.endsWith(".collapsedataset") || n.equals("collapsedataset")) {
            return COLLAPSE_DATASET;
        }
        if (n.endsWith(".chip2chip") || n.equals("chip2chip")) {
            return CHIP2CHIP;
        }
        return UNKNOWN;
    }

    static ReportKind fromFolderName(String folderName) {
        if (folderName == null || folderName.isBlank()) {
            return UNKNOWN;
        }
        String n = folderName.toLowerCase(Locale.ROOT);
        if (n.contains(".gseapreranked.")) {
            return GSEA_PRERANKED;
        }
        if (n.contains(".gsea.")) {
            return GSEA;
        }
        if (n.contains(".ssgsea.")) {
            return SSGSEA;
        }
        if (n.contains(".leadingedgetool.")) {
            return LEADING_EDGE;
        }
        if (n.contains(".coremaptool.") || n.contains(".coremap.")) {
            return COREMAP;
        }
        if (n.contains(".collapsedataset.")) {
            return COLLAPSE_DATASET;
        }
        if (n.contains(".chip2chip.")) {
            return CHIP2CHIP;
        }
        return UNKNOWN;
    }

    static ReportKind fromArtifacts(File reportDir) {
        if (reportDir == null || !reportDir.isDirectory()) {
            return UNKNOWN;
        }
        File edb = new File(reportDir, "edb");
        if (new File(edb, "results.edb").isFile() || new File(reportDir, "results.edb").isFile()) {
            File[] cls = edb.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".cls"));
            if (cls != null && cls.length > 0) {
                return GSEA;
            }
            return GSEA_PRERANKED;
        }
        File[] scores = reportDir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith("_ssgsea_scores.gct"));
        if (scores != null && scores.length > 0) {
            return SSGSEA;
        }
        if (LeadingEdgeOutputs.hasHeatMapArtifacts(reportDir)) {
            return LEADING_EDGE;
        }
        if (new File(reportDir, "coremap-job.json").isFile()
                || new File(reportDir, "coremap-result.json").isFile()) {
            return COREMAP;
        }
        File[] mapped = reportDir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).contains("_mapped_to_"));
        if (mapped != null && mapped.length > 0) {
            return CHIP2CHIP;
        }
        return UNKNOWN;
    }
}
