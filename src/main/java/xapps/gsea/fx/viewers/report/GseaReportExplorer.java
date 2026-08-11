/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.gsea_msigdb.gsea.ui.api.FeatureHost;
import org.gsea_msigdb.gsea.runtime.AppServices;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResult;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.alg.gsea.GeneSetScoringTable;
import edu.mit.broad.genome.reports.EnrichmentCharts;
import edu.mit.broad.genome.reports.EnrichmentEsProfiles;
import edu.mit.broad.genome.reports.EnrichmentReports;
import edu.mit.broad.genome.reports.api.Report;
import xapps.gsea.fx.widgets.FxImages;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import xapps.gsea.fx.viewers.FxEnrichmentMapPane;
import xapps.gsea.fx.viewers.FxLeadingEdgePane;
import xapps.gsea.fx.viewers.coremap.CoreMapWorkspace;

/**
 * Native GSEA / GSEAPreranked explorer: phenotype switcher and gene-set detail
 * in a flat Results view. Summary plots live on the report shell Plots tab.
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
    public Node create(Report report, FeatureHost featureHost) {
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
                data -> buildUi(data, featureHost),
                "Could not load enrichment database");
        return host;
    }

    private static Node buildUi(Loaded data, FeatureHost featureHost) {
        EnrichmentDb edb = data.edb();
        PhenotypeLabels pos = PhenotypeLabels.from(edb, true);
        PhenotypeLabels neg = PhenotypeLabels.from(edb, false);
        PhenoStats posStats = PhenoStats.of(edb, true);
        PhenoStats negStats = PhenoStats.of(edb, false);
        int totalSets = edb.getNumResults();

        TextField filter = new TextField();
        filter.setPromptText("Filter gene sets…");
        filter.getStyleClass().add("gsea-report-filter");
        HBox.setHgrow(filter, Priority.ALWAYS);

        File leDir = data.edbDir() != null ? data.edbDir() : data.reportDir();
        Button openLe = new Button("Open in Leading Edge");
        xapps.gsea.fx.widgets.FxButtons.styleSecondary(openLe);
        openLe.setOnAction(e -> openLeadingEdge(leDir, featureHost));

        Button openEm = new Button("Open in Enrichment Map");
        xapps.gsea.fx.widgets.FxButtons.styleSecondary(openEm);
        openEm.setOnAction(e -> openEnrichmentMap(data.reportDir(), featureHost));

        Button openCm = new Button("Open in CoreMap");
        xapps.gsea.fx.widgets.FxButtons.styleSecondary(openCm);
        openCm.setOnAction(e -> openCoreMap(leDir, featureHost));

        HBox toolbar = new HBox(10, filter, openLe, openEm, openCm);
        toolbar.setAlignment(Pos.CENTER_LEFT);
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

        Node geneSetsView = geneSetsView(pos, neg, posStats, negStats, totalSets, posTable, negTable, detail);
        VBox.setVgrow(geneSetsView, Priority.ALWAYS);

        VBox root = new VBox(10, overviewBar(edb, pos, neg, posStats, negStats, data.scoring()),
                toolbar, geneSetsView);
        root.setPadding(new Insets(12));
        root.getStyleClass().add("gsea-enrichment-root");
        return root;
    }

    private static Node geneSetsView(
            PhenotypeLabels pos, PhenotypeLabels neg,
            PhenoStats posStats, PhenoStats negStats, int totalSets,
            EnrichmentResultTable posTable, EnrichmentResultTable negTable,
            BorderPane detail) {
        ToggleGroup phenoGroup = new ToggleGroup();
        ToggleButton posBtn = new ToggleButton(pos.shortName());
        ToggleButton negBtn = new ToggleButton(neg.shortName());
        posBtn.setToggleGroup(phenoGroup);
        negBtn.setToggleGroup(phenoGroup);
        posBtn.getStyleClass().addAll("gsea-section-toggle", "gsea-pheno-toggle", "gsea-pheno-toggle-pos");
        negBtn.getStyleClass().addAll("gsea-section-toggle", "gsea-pheno-toggle", "gsea-pheno-toggle-neg");
        posBtn.setFocusTraversable(false);
        negBtn.setFocusTraversable(false);
        for (ToggleButton btn : List.of(posBtn, negBtn)) {
            btn.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
                if (btn.isSelected()) {
                    e.consume();
                }
            });
        }
        HBox phenoBar = new HBox(0, posBtn, negBtn);
        phenoBar.getStyleClass().add("gsea-section-bar");
        phenoBar.setAlignment(Pos.CENTER_LEFT);

        FlowPane chips = new FlowPane(6, 6);
        BorderPane tableHost = new BorderPane();
        tableHost.setMinSize(0, 0);
        VBox.setVgrow(tableHost, Priority.ALWAYS);

        Runnable showPos = () -> {
            chips.getChildren().setAll(
                    ReportStatChips.chip(posStats.enriched() + " / " + totalSets, "enriched in " + pos.shortName()),
                    ReportStatChips.chip(String.valueOf(posStats.fdr25()), "FDR < 25%"),
                    ReportStatChips.chip(String.valueOf(posStats.fdr5()), "FDR < 5%"),
                    ReportStatChips.chip(String.valueOf(posStats.nom1()), "NOM p < 1%"),
                    ReportStatChips.chip(String.valueOf(posStats.nom5()), "NOM p < 5%"));
            tableHost.setCenter(posTable.getTable());
            BorderPane.setMargin(posTable.getTable(), new Insets(0, 8, 8, 8));
        };
        Runnable showNeg = () -> {
            chips.getChildren().setAll(
                    ReportStatChips.chip(negStats.enriched() + " / " + totalSets, "enriched in " + neg.shortName()),
                    ReportStatChips.chip(String.valueOf(negStats.fdr25()), "FDR < 25%"),
                    ReportStatChips.chip(String.valueOf(negStats.fdr5()), "FDR < 5%"),
                    ReportStatChips.chip(String.valueOf(negStats.nom1()), "NOM p < 1%"),
                    ReportStatChips.chip(String.valueOf(negStats.nom5()), "NOM p < 5%"));
            tableHost.setCenter(negTable.getTable());
            BorderPane.setMargin(negTable.getTable(), new Insets(0, 8, 8, 8));
        };

        Label phenoTitle = new Label(pos.longName());
        phenoTitle.getStyleClass().addAll("gsea-pheno-title", "gsea-pheno-pos");
        phenoTitle.setWrapText(true);

        phenoGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            boolean negSelected = n == negBtn;
            if (negSelected) {
                phenoTitle.setText(neg.longName());
                phenoTitle.getStyleClass().setAll("gsea-pheno-title", "gsea-pheno-neg");
                showNeg.run();
            } else {
                phenoTitle.setText(pos.longName());
                phenoTitle.getStyleClass().setAll("gsea-pheno-title", "gsea-pheno-pos");
                showPos.run();
            }
        });
        phenoGroup.selectToggle(posBtn);
        showPos.run();

        VBox header = new VBox(6, new HBox(10, phenoBar, phenoTitle), chips);
        header.setPadding(new Insets(10, 12, 8, 12));
        HBox.setHgrow(phenoTitle, Priority.ALWAYS);

        BorderPane tablePanel = new BorderPane(tableHost);
        tablePanel.setTop(header);
        tablePanel.getStyleClass().addAll("gsea-pheno-panel", "gsea-pheno-panel-pos");
        phenoGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            tablePanel.getStyleClass().removeAll("gsea-pheno-panel-pos", "gsea-pheno-panel-neg");
            tablePanel.getStyleClass().add(n == negBtn ? "gsea-pheno-panel-neg" : "gsea-pheno-panel-pos");
        });

        SplitPane mainSplit = new SplitPane(tablePanel, detail);
        mainSplit.setOrientation(Orientation.VERTICAL);
        mainSplit.setDividerPositions(0.58);
        return mainSplit;
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
            return new VBox(8, chips, warn);
        }
        return chips;
    }

    private static Node emptyDetail() {
        Label hint = new Label(
                "Select a gene set to view its enrichment plot and members.");
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
            xapps.gsea.fx.widgets.FxButtons.styleSecondary(openHtml);
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

        List<ReportExplorerSupport.Section> sections = new ArrayList<>();
        if (canShowEnplot(result)) {
            File classic = EnplotReportFiles.findClassicEnplot(reportDir, setName, html);
            File v2 = EnplotReportFiles.findEnplotV2(reportDir, setName, html);
            sections.add(new ReportExplorerSupport.Section("EnPlot v2",
                    () -> staticEnplotPane(result, true, v2, classA, classB, scoring, metricName)));
            sections.add(new ReportExplorerSupport.Section("Classic",
                    () -> staticEnplotPane(result, false, classic, classA, classB, scoring, metricName)));
            sections.add(new ReportExplorerSupport.Section("Interactive",
                    () -> EnplotWebView.createInteractivePane(reportDir, result, classA, classB, scoring, metricName),
                    true));
        }
        for (NamedPlot plot : otherGeneSetPlots(reportDir, setName, html)) {
            sections.add(new ReportExplorerSupport.Section(plot.title(),
                    () -> ReportExplorerSupport.singleImage(plot.file())));
        }
        if (tsv != null) {
            sections.add(new ReportExplorerSupport.Section("Members",
                    () -> ReportExplorerSupport.tsvTable(tsv)));
        }
        Node sectionHost;
        if (sections.isEmpty()) {
            sectionHost = ReportExplorerSupport.messagePane("No enrichment plots found for this gene set.");
        } else {
            sectionHost = ReportExplorerSupport.lazySectionHost(sections);
        }

        VBox box = new VBox(8, titleRow, metrics, actions, sectionHost);
        VBox.setVgrow(sectionHost, Priority.ALWAYS);
        box.setPadding(new Insets(8, 10, 10, 10));
        box.setMinSize(0, 0);
        return box;
    }

    private static boolean canShowEnplot(EnrichmentResult result) {
        return result != null && result.getRankedList() != null && result.getScore() != null;
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

        Runnable saveAction = () -> {
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
                AppServices.require().dialogs().showError("Could not save enrichment plot", t);
            }
        };

        if (existingImage != null) {
            host.setCenter(withCompactSave(ReportExplorerSupport.singleImage(existingImage), saveAction));
            return host;
        }

        ReportExplorerSupport.loadAsync(host,
                "gsea-enplot-" + (v2 ? "v2" : "classic"),
                "Generating " + (v2 ? "EnPlot v2" : "classic enplot") + "…",
                () -> Boolean.TRUE,
                ignored -> {
                    EnrichmentCharts charts = EnrichmentReports.createComboChart(
                            result, v2, classA, classB, scoringTable, metricName);
                    return withCompactSave(
                            ReportExplorerSupport.singleImage(plotToFxImage(charts.comboChart, previewW, previewH)),
                            saveAction);
                },
                "Could not generate " + (v2 ? "EnPlot v2" : "classic") + " plot");
        return host;
    }

    /** Save control overlaid on the plot — avoids a dedicated chrome row. */
    private static Node withCompactSave(Node content, Runnable onSave) {
        Button save = new Button("Save…");
        xapps.gsea.fx.widgets.FxButtons.styleSecondary(save);
        xapps.gsea.fx.widgets.FxButtons.sizeToContent(save);
        save.setOnAction(e -> onSave.run());
        StackPane stack = new StackPane();
        stack.setMinSize(0, 0);
        if (content != null) {
            stack.getChildren().add(content);
        }
        stack.getChildren().add(save);
        StackPane.setAlignment(save, Pos.TOP_RIGHT);
        // Clear of typical ScrollPane scrollbar gutter (matches interactive EnPlot).
        StackPane.setMargin(save, new Insets(6, 22, 0, 0));
        return stack;
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
        return plots;
    }

    /**
     * Summary plot gallery for the report shell Plots tab, or {@code null} if none are present.
     */
    public static Node createSummaryPlots(Report report) {
        return plotsGallery(ReportExplorerSupport.reportDir(report));
    }

    private static Node plotsGallery(File reportDir) {
        List<File> plots = new ArrayList<>();
        if (reportDir != null) {
            for (String name : SUMMARY_PLOT_NAMES) {
                File f = new File(reportDir, name);
                if (f.isFile()) {
                    plots.add(f);
                }
            }
        }
        if (plots.isEmpty()) {
            return null;
        }
        Node gallery = ReportExplorerSupport.imageGallery(plots);
        if (gallery instanceof javafx.scene.layout.Region region) {
            region.setMinSize(0, 0);
            region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }
        return gallery;
    }

    private static String fmt(float v) {
        return Float.isNaN(v) ? "NaN" : String.format(Locale.ROOT, "%.4g", v);
    }

    private static void openLeadingEdge(File edbOrReportDir, FeatureHost host) {
        try {
            FxLeadingEdgePane pane = new FxLeadingEdgePane(host);
            host.openPage(pane);
            pane.loadFromDirectory(edbOrReportDir);
        } catch (Throwable t) {
            host.dialogs().showError("Could not open Leading Edge", t);
        }
    }

    private static void openEnrichmentMap(File reportDir, FeatureHost host) {
        try {
            FxEnrichmentMapPane pane = new FxEnrichmentMapPane(host);
            host.openPage(pane);
            if (reportDir != null) {
                pane.loadFromDirectory(reportDir);
            }
        } catch (Throwable t) {
            host.dialogs().showError("Could not open Enrichment Map", t);
        }
    }

    private static void openCoreMap(File edbOrReportDir, FeatureHost host) {
        try {
            CoreMapWorkspace.openFromReport(edbOrReportDir, host);
        } catch (Throwable t) {
            host.dialogs().showError("Could not open CoreMap", t);
        }
    }
}
