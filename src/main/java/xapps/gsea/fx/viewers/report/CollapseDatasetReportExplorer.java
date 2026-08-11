/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import java.io.File;
import java.util.Locale;

import org.gsea_msigdb.gsea.ui.api.FeatureHost;

import edu.mit.broad.genome.reports.api.Report;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

/**
 * Native explorer for CollapseDataset reports: collapsed GCT + etiology table.
 */
public final class CollapseDatasetReportExplorer implements ReportExplorer {

    private record Artifacts(File gct, File etiology) {
    }

    @Override
    public Node create(Report report, FeatureHost host) {
        File dir = ReportExplorerSupport.reportDir(report);
        BorderPane pane = new BorderPane();

        ReportExplorerSupport.loadAsync(pane, "collapse-report-explorer",
                "Loading collapsed dataset…",
                () -> discover(dir),
                arts -> buildUi(arts, report, host),
                "Could not load Collapse Dataset results");
        return pane;
    }

    private static Artifacts discover(File dir) {
        File gct = ReportExplorerSupport.findFirst(dir,
                n -> ReportExplorerSupport.endsWithIgnoreCase(n, ".gct"));
        File etiology = ReportExplorerSupport.findFirst(dir, n -> {
            String lower = n.toLowerCase(Locale.ROOT);
            return lower.endsWith(".tsv") && (lower.contains("etiology") || lower.contains("collapse"));
        });
        if (etiology == null) {
            etiology = ReportExplorerSupport.findFirst(dir,
                    n -> ReportExplorerSupport.endsWithIgnoreCase(n, ".tsv"));
        }
        return new Artifacts(gct, etiology);
    }

    private static Node buildUi(Artifacts arts, Report report, FeatureHost host) {
        if (arts.gct() == null && arts.etiology() == null) {
            return new GenericFilesExplorer().create(report, host);
        }
        if (arts.etiology() == null) {
            return ReportExplorerSupport.tabPane(
                    ReportExplorerSupport.lazyFileTab("Collapsed dataset",
                            arts.gct(), "No .gct file found"));
        }
        if (arts.gct() == null) {
            return ReportExplorerSupport.tabPane(
                    ReportExplorerSupport.fixedTab("Etiology",
                            ReportExplorerSupport.tsvTable(arts.etiology())));
        }
        return ReportExplorerSupport.tabPane(
                ReportExplorerSupport.lazyFileTab("Collapsed dataset",
                        arts.gct(), "No .gct file found"),
                ReportExplorerSupport.fixedTab("Etiology",
                        ReportExplorerSupport.tsvTable(arts.etiology())));
    }
}
