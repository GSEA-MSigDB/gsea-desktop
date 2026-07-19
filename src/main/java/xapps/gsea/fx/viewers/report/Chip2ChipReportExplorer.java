/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import org.gsea_msigdb.gsea.ui.api.ViewPage;

import edu.mit.broad.genome.reports.api.Report;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;

/**
 * Native explorer for Chip2Chip reports: mapped GMT + etiology summary.
 * Per-set etiology detail files remain available on the Files tab.
 */
public final class Chip2ChipReportExplorer implements ReportExplorer {

    private record Artifacts(File mapped, File summaryTsv, File etiologySummary) {
    }

    @Override
    public Node create(Report report, Consumer<ViewPage> openPage) {
        File dir = ReportExplorerSupport.reportDir(report);
        BorderPane host = new BorderPane();

        ReportExplorerSupport.loadAsync(host, "chip2chip-report-explorer",
                "Loading Chip2Chip results…",
                () -> discover(dir),
                arts -> buildUi(arts, report, openPage),
                "Could not load Chip2Chip results");
        return host;
    }

    private static Artifacts discover(File dir) {
        File mapped = ReportExplorerSupport.findFirst(dir, n -> {
            String lower = n.toLowerCase(Locale.ROOT);
            return (lower.endsWith(".gmt") || lower.endsWith(".gmx")) && lower.contains("mapped");
        });
        if (mapped == null) {
            mapped = ReportExplorerSupport.findFirst(dir,
                    n -> ReportExplorerSupport.endsWithIgnoreCase(n, ".gmt")
                            || ReportExplorerSupport.endsWithIgnoreCase(n, ".gmx"));
        }
        File summaryTsv = ReportExplorerSupport.findFirst(dir, n -> {
            String lower = n.toLowerCase(Locale.ROOT);
            return lower.endsWith(".tsv") && !lower.contains("etiology");
        });
        // Prefer a single summary etiology table/text over dozens of per-set tabs.
        File etiologySummary = ReportExplorerSupport.findFirst(dir, n -> {
            String lower = n.toLowerCase(Locale.ROOT);
            return lower.contains("etiology") && lower.endsWith(".tsv");
        });
        if (etiologySummary == null) {
            etiologySummary = ReportExplorerSupport.findFirst(dir, n -> {
                String lower = n.toLowerCase(Locale.ROOT);
                return lower.contains("etiology") && lower.endsWith(".txt");
            });
        }
        return new Artifacts(mapped, summaryTsv, etiologySummary);
    }

    private static Node buildUi(Artifacts arts, Report report, Consumer<ViewPage> openPage) {
        if (arts.mapped() == null && arts.summaryTsv() == null && arts.etiologySummary() == null) {
            return new GenericFilesExplorer().create(report, openPage);
        }
        List<Tab> tabs = new ArrayList<>();
        tabs.add(ReportExplorerSupport.lazyFileTab("Mapped gene sets",
                arts.mapped(), "No mapped gene-set matrix found"));
        if (arts.summaryTsv() != null) {
            tabs.add(ReportExplorerSupport.fixedTab("Summary",
                    ReportExplorerSupport.tsvTable(arts.summaryTsv())));
        }
        if (arts.etiologySummary() != null) {
            Node content = arts.etiologySummary().getName().toLowerCase(Locale.ROOT).endsWith(".tsv")
                    ? ReportExplorerSupport.tsvTable(arts.etiologySummary())
                    : ReportExplorerSupport.textFile(arts.etiologySummary());
            tabs.add(ReportExplorerSupport.fixedTab("Etiology", content));
        }
        return ReportExplorerSupport.tabPane(tabs.toArray(Tab[]::new));
    }
}
