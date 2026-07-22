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
import xapps.gsea.fx.jobs.JobRuntime;

/**
 * Native explorer for ssGSEA reports: scores GCT, optional ROC/MCC tables and plot galleries.
 */
public final class SsGseaReportExplorer implements ReportExplorer {

    private record Artifacts(
            File scores,
            List<File> bubbles,
            List<File> resultTsvs,
            List<File> rocPlots,
            List<File> mccSnaps) {
    }

    @Override
    public Node create(Report report, Consumer<ViewPage> openPage, JobRuntime jobRuntime) {
        File dir = ReportExplorerSupport.reportDir(report);
        BorderPane host = new BorderPane();

        ReportExplorerSupport.loadAsync(host, "ssgsea-report-explorer",
                "Loading ssGSEA results…",
                () -> discover(dir),
                arts -> buildUi(arts, report, openPage, jobRuntime),
                "Could not load ssGSEA results");
        return host;
    }

    private static Artifacts discover(File dir) {
        File scores = ReportExplorerSupport.findFirst(dir,
                n -> n.toLowerCase(Locale.ROOT).endsWith("_ssgsea_scores.gct"));
        List<File> bubbles = ReportExplorerSupport.findImages(
                dir != null ? new File(dir, "ssgsea_bubble_plots") : null, null);
        List<File> resultTsvs = ReportExplorerSupport.findAll(dir, n -> {
            String lower = n.toLowerCase(Locale.ROOT);
            return lower.endsWith(".tsv") && lower.contains(".results");
        });
        List<File> rocPlots = ReportExplorerSupport.findImages(
                dir != null ? new File(dir, "ssgsea_roc_plots") : null, null);
        List<File> mccSnaps = ReportExplorerSupport.findImages(dir, n -> {
            String lower = n.toLowerCase(Locale.ROOT);
            return lower.contains("mccpos") || lower.contains("mccneg")
                    || lower.contains("ssgsea_roc_mcc");
        });
        return new Artifacts(scores, bubbles, resultTsvs, rocPlots, mccSnaps);
    }

    private static Node buildUi(Artifacts arts, Report report, Consumer<ViewPage> openPage,
            JobRuntime jobRuntime) {
        List<Tab> tabs = new ArrayList<>();
        tabs.add(ReportExplorerSupport.lazyFileTab("Scores",
                arts.scores(), "No *_ssgsea_scores.gct found"));

        if (!arts.bubbles().isEmpty()) {
            tabs.add(ReportExplorerSupport.fixedTab("Bubble plots",
                    ReportExplorerSupport.imageGallery(arts.bubbles())));
        }
        for (File tsv : arts.resultTsvs()) {
            tabs.add(ReportExplorerSupport.fixedTab(
                    ReportExplorerSupport.shortTitle(tsv.getName(), 40),
                    ReportExplorerSupport.tsvTable(tsv)));
        }
        if (!arts.rocPlots().isEmpty()) {
            tabs.add(ReportExplorerSupport.fixedTab("ROC plots",
                    ReportExplorerSupport.imageGallery(arts.rocPlots())));
        }
        if (!arts.mccSnaps().isEmpty()) {
            tabs.add(ReportExplorerSupport.fixedTab("MCC snapshots",
                    ReportExplorerSupport.imageGallery(arts.mccSnaps())));
        }

        if (tabs.size() == 1 && arts.scores() == null) {
            return new GenericFilesExplorer().create(report, openPage, jobRuntime);
        }
        return ReportExplorerSupport.tabPane(tabs.toArray(Tab[]::new));
    }
}
