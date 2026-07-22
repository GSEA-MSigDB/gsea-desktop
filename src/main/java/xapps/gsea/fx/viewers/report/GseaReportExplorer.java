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
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResult;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.alg.gsea.GeneSetScoringTable;
import edu.mit.broad.genome.reports.EnrichmentCharts;
import edu.mit.broad.genome.reports.EnrichmentEsProfiles;
import edu.mit.broad.genome.reports.EnrichmentReports;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.xbench.core.api.Application;
import xapps.gsea.fx.FxImages;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import xapps.gsea.fx.jobs.JobRuntime;
import xapps.gsea.fx.viewers.FxEnrichmentMapPane;
import xapps.gsea.fx.viewers.FxLeadingEdgePane;
import xapps.gsea.fx.viewers.coremap.CoreMapWorkspace;
import xapps.gsea.fx.viewers.coremap.FxCoreMapPane;

/**
 * Native GSEA / GSEAPreranked explorer: side-by-side phenotype panels, summary stats,
 * gene-set detail, and summary plots.
 */
public final class GseaReportExplorer implements ReportExplorer {

    private static final String[] SUMMARY_PLOT_NAMES = {
            "gsea_bubble_plot_pos.png",
            "gsea_bubble_plot_neg.png",
            "butterfly_plot.png",
            "pvalues_vs_nes_plot.png",
            "global_es_histogram.png"
    };

    private record Loaded(File reportDir, File edbDir, EnrichmentDb edb,
            EnrichmentEsProfiles.ScoringResolution scoring) {
    }

    private record PhenoStats(int enriched, int fdr25, int fdr5, int nom1, int nom5) {
        static PhenoStats of(EnrichmentDb edb, boolean pos) {
            return new PhenoStats(
                    edb.getNumScores(pos),
                    edb.getNumFDRSig(0.25f, pos),
                    edb.getNumFDRSig(0.05f, pos),
                    edb.getNumNominallySig(0.01f, pos),
                    edb.getNumNominallySig(0.05f, pos));
        }
    }

    private record NamedPlot(String title, File file) {
    }

    @Override
    public Node create(Report report, Consumer<ViewPage> openPage, JobRuntime jobRuntime) {
        Consumer<ViewPage> open = ReportExplorerSupport.safeOpen(openPage);
        File reportDir = ReportExplorerSupport.reportDir(report);
        BorderPane host = new BorderPane();
        host.getStyleClass().add("gsea-report-explorer");

        ReportExplorerSupport.loadAsync(host, "gsea-report-explorer",
                "Loading enrichment results…",
                () -> {
                    File edbDir = ReportExplorerSupport.resolveEdbDir(reportDir);
                    EnrichmentEsProfiles.ScoringResolution scoring = EnrichmentEsProfiles.resolveScoring(
                            report != null ? report.getParametersUsed() : null);
                    return new Loaded(reportDir, edbDir, ParserFactory.readEdb(edbDir, true), scoring);
                },
                data -> buildUi(data, open, jobRuntime),
                "Could not load enrichment database");
        return host;
    }

    private static Node buildUi(Loaded data, Consumer<ViewPage> openPage, JobRuntime jobRuntime) {
        EnrichmentDb edb = data.edb();
        PhenotypeLabels pos = PhenotypeLabels.from(edb, true);
        PhenotypeLabels neg = PhenotypeLabels.from(edb, false);
        PhenoStats posStats = PhenoStats.of(edb, true);
        PhenoStats negStats = PhenoStats.of(edb, false);
        int totalSets = edb.getNumResults();

        TextField filter = new TextField();
        filter.setPromptText("Filter gene sets in both panels…");
        filter.getStyleClass().add("gsea-report-filter");
        HBox.setHgrow(filter, Priority.ALWAYS);

        File leDir = data.edbDir() != null ? data.edbDir() : data.reportDir();
        Button openLe = new Button("Open in Leading Edge");
        xapps.gsea.fx.FxButtons.styleSecondary(openLe);
        openLe.setOnAction(e -> openLeadingEdge(leDir, openPage, jobRuntime));

        Button openEm = new Button("Open in Enrichment Map");
        xapps.gsea.fx.FxButtons.styleSecondary(openEm);
        openEm.setOnAction(e -> openEnrichmentMap(data.reportDir(), openPage));

        Button openCm = new Button("Open in CoreMap");
        xapps.gsea.fx.FxButtons.styleSecondary(openCm);
        openCm.setOnAction(e -> openCoreMap(leDir, openPage));

        HBox toolbar = new HBox(10, filter, openLe, openEm, openCm);
        toolbar.setPadding(new Insets(0, 0, 8, 0));

        BorderPane detail = new BorderPane();
        detail.getStyleClass().add("gsea-detail-pane");
        detail.setCenter(emptyDetail());

        EnrichmentResultTable posTable = new EnrichmentResultTable(SelectionMode.SINGLE);
        EnrichmentResultTable negTable = new EnrichmentResultTable(SelectionMode.SINGLE);
        posTable.setResults(edb.getResults(true));
        negTable.setResults(edb.getResults(false));
        posTable.bindFilterField(filter);
        negTable.bindFilterField(filter);
        String classA = pos.shortName();
        String classB = neg.shortName();
        String metricName = edb.getMetric() != null ? edb.getMetric().getName() : null;
        try {
            EnrichmentReports.ensureMetricName(edb.getRankedList(), metricName);
        } catch (Throwable ignored) {
            // ranked list optional for some loads
        }
        wireExclusiveSelection(posTable, negTable, pos, neg, data.reportDir(), classA, classB,
                data.scoring(), metricName, detail);

        SplitPane phenotypeSplit = new SplitPane(
                phenotypePanel(pos, posStats, totalSets, posTable),
                phenotypePanel(neg, negStats, totalSets, negTable));
        phenotypeSplit.setOrientation(Orientation.HORIZONTAL);
        phenotypeSplit.setDividerPositions(0.5);

        SplitPane mainSplit = new SplitPane(phenotypeSplit, detail);
        mainSplit.setOrientation(Orientation.VERTICAL);
        mainSplit.setDividerPositions(0.58);
        VBox.setVgrow(mainSplit, Priority.ALWAYS);

        VBox enrichment = new VBox(10, overviewBar(edb, pos, neg, posStats, negStats, data.scoring()),
                toolbar, mainSplit);
        enrichment.setPadding(new Insets(12));
        enrichment.getStyleClass().add("gsea-enrichment-root");

        return ReportExplorerSupport.tabPane(
                ReportExplorerSupport.fixedTab("Enrichment", enrichment),
                ReportExplorerSupport.fixedTab("Plots", plotsTab(data.reportDir())));
    }

    private static void wireExclusiveSelection(
            EnrichmentResultTable primary, EnrichmentResultTable other,
            PhenotypeLabels primaryInfo, PhenotypeLabels otherInfo,
            File reportDir, String classA, String classB,
            EnrichmentEsProfiles.ScoringResolution scoring, String metricName, BorderPane detail) {
        listenSelection(primary, other, primaryInfo, reportDir, classA, classB, scoring, metricName, detail);
        listenSelection(other, primary, otherInfo, reportDir, classA, classB, scoring, metricName, detail);
    }

    private static void listenSelection(
            EnrichmentResultTable source, EnrichmentResultTable peer,
            PhenotypeLabels info, File reportDir, String classA, String classB,
            EnrichmentEsProfiles.ScoringResolution scoring, String metricName, BorderPane detail) {
        source.getTable().getSelectionModel().selectedItemProperty().addListener((obs, o, row) -> {
            if (row != null) {
                peer.clearSelection();
                detail.setCenter(buildDetail(reportDir, row, info, classA, classB, scoring, metricName));
            } else if (peer.getTable().getSelectionModel().getSelectedItem() == null) {
                detail.setCenter(emptyDetail());
            }
        });
    }

    private static Node overviewBar(EnrichmentDb edb, PhenotypeLabels pos, PhenotypeLabels neg,
            PhenoStats posStats, PhenoStats negStats, EnrichmentEsProfiles.ScoringResolution scoring) {
        int genes = 0;
        try {
            if (edb.getRankedList() != null) {
                genes = edb.getRankedList().getSize();
            }
        } catch (Throwable ignored) {
            // ranked list optional for some loads
        }
        FlowPane chips = new FlowPane(8, 8);
        chips.getChildren().addAll(
                ReportStatChips.chip(String.valueOf(edb.getNumResults()), "gene sets"),
                ReportStatChips.chip(String.valueOf(genes), "genes in ranking"),
                ReportStatChips.chip(String.valueOf(edb.getNumPerm()), "permutations"),
                ReportStatChips.chip(String.valueOf(posStats.enriched()), "↑ " + pos.shortName()),
                ReportStatChips.chip(String.valueOf(negStats.enriched()), "↑ " + neg.shortName()));
        if (scoring != null && !scoring.explicitFromReport()) {
            Label warn = new Label("On-demand ES curves use " + scoring.label()
                    + " — scoring_scheme was not found in this report.");
            warn.getStyleClass().add("gsea-muted");
            warn.setWrapText(true);
            VBox box = new VBox(8, chips, warn);
            return box;
        }
        return chips;
    }

    private static Node phenotypePanel(PhenotypeLabels pheno, PhenoStats stats, int totalSets,
            EnrichmentResultTable table) {
        Label title = new Label(pheno.longName());
        title.getStyleClass().addAll("gsea-pheno-title",
                pheno.positive() ? "gsea-pheno-pos" : "gsea-pheno-neg");
        title.setWrapText(true);

        FlowPane chips = new FlowPane(6, 6);
        chips.getChildren().addAll(
                ReportStatChips.chip(stats.enriched() + " / " + totalSets, "enriched in " + pheno.shortName()),
                ReportStatChips.chip(String.valueOf(stats.fdr25()), "FDR < 25%"),
                ReportStatChips.chip(String.valueOf(stats.fdr5()), "FDR < 5%"),
                ReportStatChips.chip(String.valueOf(stats.nom1()), "NOM p < 1%"),
                ReportStatChips.chip(String.valueOf(stats.nom5()), "NOM p < 5%"));

        VBox header = new VBox(6, title, chips);
        header.setPadding(new Insets(10, 12, 8, 12));

        BorderPane panel = new BorderPane(table.getTable());
        panel.setTop(header);
        BorderPane.setMargin(table.getTable(), new Insets(0, 8, 8, 8));
        panel.getStyleClass().addAll("gsea-pheno-panel",
                pheno.positive() ? "gsea-pheno-panel-pos" : "gsea-pheno-panel-neg");
        return panel;
    }

    private static Node emptyDetail() {
        Label hint = new Label(
                "Select a gene set in either phenotype panel to view its enrichment plot and members.");
        hint.getStyleClass().add("gsea-muted");
        hint.setWrapText(true);
        hint.setPadding(new Insets(16));
        return hint;
    }

    private static Node buildDetail(File reportDir, EnrichmentResultRow row, PhenotypeLabels pheno,
            String classA, String classB, EnrichmentEsProfiles.ScoringResolution scoring, String metricName) {
        String setName = row.nameProperty().get();
        EnrichmentResult result = row.getResult();
        File tsv = EnplotReportFiles.findGeneSetSibling(reportDir, setName, ".tsv");
        File html = EnplotReportFiles.findGeneSetSibling(reportDir, setName, ".html");

        Label title = new Label(setName);
        title.getStyleClass().add("gsea-section-header");
        title.setWrapText(true);

        Label phenoBadge = new Label(pheno.shortName());
        phenoBadge.getStyleClass().addAll("gsea-pheno-badge",
                pheno.positive() ? "gsea-pheno-pos" : "gsea-pheno-neg");

        HBox titleRow = new HBox(10, title, phenoBadge);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);

        FlowPane metrics = new FlowPane(8, 6);
        metrics.getChildren().addAll(
                ReportStatChips.chip(fmt(row.esProperty().get()), "ES"),
                ReportStatChips.chip(fmt(row.nesProperty().get()), "NES"),
                ReportStatChips.chip(fmt(row.nomPProperty().get()), "NOM p-Val"),
                ReportStatChips.chip(fmt(row.fdrProperty().get()), "FDR"),
                ReportStatChips.chip(fmt(row.fwerProperty().get()), "FWER"),
                ReportStatChips.chip(String.valueOf(row.sizeProperty().get()), "size"),
                ReportStatChips.chip(String.valueOf(row.rankAtMaxProperty().get()), "rank at max"));

        HBox actions = new HBox(8);
        if (html != null) {
            Button openHtml = new Button("Open gene-set HTML");
            xapps.gsea.fx.FxButtons.styleSecondary(openHtml);
            openHtml.setOnAction(e -> ReportExplorerSupport.openFileExternal(html));
            actions.getChildren().add(openHtml);
        }
        String le = row.leadingEdgeProperty().get();
        if (le != null && !le.isBlank()) {
            Label leLabel = new Label(le);
            leLabel.getStyleClass().add("gsea-muted");
            leLabel.setWrapText(true);
            actions.getChildren().add(leLabel);
        }

        List<Tab> tabs = new ArrayList<>();
        Tab enplotSection = enrichmentPlotSection(reportDir, result, classA, classB, scoring, metricName, html);
        if (enplotSection != null) {
            tabs.add(enplotSection);
        }
        for (NamedPlot plot : otherGeneSetPlots(reportDir, setName, html)) {
            tabs.add(ReportExplorerSupport.fixedTab(plot.title(),
                    ReportExplorerSupport.singleImage(plot.file())));
        }
        if (tabs.isEmpty()) {
            tabs.add(ReportExplorerSupport.fixedTab("Plots",
                    ReportExplorerSupport.messagePane("No enrichment plots found for this gene set.")));
        }
        if (tsv != null) {
            tabs.add(ReportExplorerSupport.fixedTab("Members", ReportExplorerSupport.tsvTable(tsv)));
        }
        TabPane detailTabs = ReportExplorerSupport.tabPane(tabs.toArray(Tab[]::new));
        detailTabs.getStyleClass().add("gsea-report-tabs");

        VBox box = new VBox(10, titleRow, metrics, actions, detailTabs);
        VBox.setVgrow(detailTabs, Priority.ALWAYS);
        box.setPadding(new Insets(12));
        return box;
    }

    /**
     * Nested Enrichment Plot section with left tabs: EnPlot v2, EnPlot Classic, Interactive.
     * Static classic/v2 images are used when present; otherwise charts are generated on demand
     * from the loaded enrichment database (including gene sets outside {@code plot_top_x}).
     */
    private static Tab enrichmentPlotSection(File reportDir, EnrichmentResult result,
            String classA, String classB, EnrichmentEsProfiles.ScoringResolution scoring,
            String metricName, File html) {
        if (result == null || result.getRankedList() == null || result.getScore() == null) {
            return null;
        }
        String geneSetName = result.getGeneSet().getName(true);
        File classic = EnplotReportFiles.findClassicEnplot(reportDir, geneSetName, html);
        File v2 = EnplotReportFiles.findEnplotV2(reportDir, geneSetName, html);

        List<Tab> inner = new ArrayList<>();
        inner.add(ReportExplorerSupport.lazyNodeTab("EnPlot v2",
                () -> staticEnplotPane(result, true, v2, classA, classB, scoring, metricName)));
        inner.add(ReportExplorerSupport.lazyNodeTab("EnPlot Classic",
                () -> staticEnplotPane(result, false, classic, classA, classB, scoring, metricName)));
        inner.add(ReportExplorerSupport.lazyNodeTabOnClick("Interactive",
                () -> EnplotWebView.createInteractivePane(reportDir, result, classA, classB, scoring, metricName)));

        TabPane nested = ReportExplorerSupport.tabPane(inner.toArray(Tab[]::new));
        nested.setSide(javafx.geometry.Side.LEFT);
        nested.getStyleClass().add("gsea-enplot-tabs");
        return ReportExplorerSupport.fixedTab("Enrichment Plot", nested);
    }

    private static Node staticEnplotPane(EnrichmentResult result, boolean v2,
            File existingImage, String classA, String classB,
            EnrichmentEsProfiles.ScoringResolution scoring, String metricName) {
        int previewW = v2 ? 900 : 500;
        int previewH = v2 ? 960 : 500;
        String setName = result.getGeneSet().getName(true);
        GeneSetScoringTable scoringTable = scoring != null ? scoring.table() : null;

        BorderPane host = new BorderPane();
        host.setMinSize(0, 0);

        Button save = new Button("Save plot…");
        xapps.gsea.fx.FxButtons.styleSecondary(save);
        Label source = new Label(existingImage != null
                ? "From report · Save exports a fresh chart"
                : "Generated on demand");
        source.getStyleClass().add("gsea-muted");
        HBox bar = new HBox(8, save, source);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 8, 6, 8));
        bar.getStyleClass().add("gsea-report-action-bar");
        host.setTop(bar);

        save.setOnAction(e -> {
            try {
                EnrichmentCharts charts = EnrichmentReports.createComboChart(
                        result, v2, classA, classB, scoringTable, metricName);
                Window owner = host.getScene() != null ? host.getScene().getWindow() : null;
                String base = (v2 ? EnrichmentReports.ENPLOT2_ : EnrichmentReports.ENPLOT_) + setName;
                java.awt.image.BufferedImage bi;
                if (charts.comboChart instanceof edu.mit.broad.genome.plots.PlotChart pc) {
                    bi = edu.mit.broad.genome.plots.PlotRasterExporter.render(pc.getSpec(), previewW, previewH);
                } else {
                    java.io.File tmp = java.io.File.createTempFile("enplot-", ".png");
                    try {
                        charts.comboChart.saveAsPNG(tmp, previewW, previewH);
                        bi = javax.imageio.ImageIO.read(tmp);
                    } finally {
                        tmp.delete();
                    }
                }
                EnplotExportDialog.saveImage(owner, bi, base, previewW, previewH);
            } catch (Throwable t) {
                Application.getWindowManager().showError("Could not save enrichment plot", t);
            }
        });

        if (existingImage != null) {
            host.setCenter(ReportExplorerSupport.singleImage(existingImage));
            return host;
        }

        ReportExplorerSupport.loadAsync(host,
                "gsea-enplot-" + (v2 ? "v2" : "classic"),
                "Generating " + (v2 ? "EnPlot v2" : "classic enplot") + "…",
                () -> Boolean.TRUE,
                ignored -> {
                    EnrichmentCharts charts = EnrichmentReports.createComboChart(
                            result, v2, classA, classB, scoringTable, metricName);
                    return ReportExplorerSupport.singleImage(plotToFxImage(charts.comboChart, previewW, previewH));
                },
                "Could not generate " + (v2 ? "EnPlot v2" : "classic") + " plot");
        return host;
    }

    private static Image plotToFxImage(edu.mit.broad.genome.charts.XChart chart, int width, int height) {
        if (chart instanceof edu.mit.broad.genome.plots.PlotChart pc) {
            return FxImages.toFxImage(
                    edu.mit.broad.genome.plots.PlotRasterExporter.render(pc.getSpec(), width, height));
        }
        try {
            java.io.File tmp = java.io.File.createTempFile("gsea-plot-", ".png");
            try {
                chart.saveAsPNG(tmp, width, height);
                return new Image(tmp.toURI().toString());
            } finally {
                tmp.delete();
            }
        } catch (Exception e) {
            return null;
        }
    }

    /** Heatmap / null distribution and any other non-enplot images. */
    private static List<NamedPlot> otherGeneSetPlots(File reportDir, String geneSetName, File html) {
        List<NamedPlot> plots = new ArrayList<>();
        if (html != null && html.isFile()) {
            for (String src : EnplotReportFiles.extractImgSrcs(html)) {
                String name = EnplotReportFiles.fileName(src);
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.startsWith("enplot")) {
                    continue;
                }
                File image = EnplotReportFiles.resolveReportImage(reportDir, src);
                if (image != null) {
                    plots.add(new NamedPlot(EnplotReportFiles.plotTitle(name, geneSetName), image));
                }
            }
            if (!plots.isEmpty()) {
                return plots;
            }
        }
        if (reportDir == null || geneSetName == null) {
            return plots;
        }
        String lowerName = geneSetName.toLowerCase(Locale.ROOT);
        File heatmap = ReportExplorerSupport.findFirst(reportDir, n -> {
            String lower = n.toLowerCase(Locale.ROOT);
            return lower.startsWith(lowerName + "_") && ReportExplorerSupport.isImageName(n)
                    && !lower.startsWith("enplot");
        });
        if (heatmap != null) {
            plots.add(new NamedPlot("Heatmap", heatmap));
        }
        // Null ES distribution uses a fixed filename in HTML reports; only show when embedded in this set's page.
        return plots;
    }

    private static Node plotsTab(File reportDir) {
        List<File> plots = new ArrayList<>();
        if (reportDir != null) {
            for (String name : SUMMARY_PLOT_NAMES) {
                File f = new File(reportDir, name);
                if (f.isFile()) {
                    plots.add(f);
                }
            }
        }
        return ReportExplorerSupport.imageGallery(plots);
    }

    private static String fmt(float v) {
        return Float.isNaN(v) ? "NaN" : String.format(Locale.ROOT, "%.4g", v);
    }

    private static void openLeadingEdge(File edbOrReportDir, Consumer<ViewPage> openPage,
            JobRuntime jobRuntime) {
        try {
            FxLeadingEdgePane pane = new FxLeadingEdgePane(jobRuntime);
            openPage.accept(pane);
            pane.loadFromDirectory(edbOrReportDir);
        } catch (Throwable t) {
            Application.getWindowManager().showError("Could not open Leading Edge", t);
        }
    }

    private static void openEnrichmentMap(File reportDir, Consumer<ViewPage> openPage) {
        try {
            FxEnrichmentMapPane pane = new FxEnrichmentMapPane();
            openPage.accept(pane);
            if (reportDir != null) {
                pane.loadFromDirectory(reportDir);
            }
        } catch (Throwable t) {
            Application.getWindowManager().showError("Could not open Enrichment Map", t);
        }
    }

    private static void openCoreMap(File edbOrReportDir, Consumer<ViewPage> openPage) {
        try {
            CoreMapWorkspace.openFromReport(edbOrReportDir, FxCoreMapPane::new, openPage);
        } catch (Throwable t) {
            Application.getWindowManager().showError("Could not open CoreMap", t);
        }
    }
}
