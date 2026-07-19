/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gsea_msigdb.gsea.ui.api.ViewPage;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import edu.mit.broad.genome.parsers.AuxUtils;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.xbench.core.api.Application;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import xapps.gsea.fx.viewers.FxEnrichmentMapPane;
import xapps.gsea.fx.viewers.FxLeadingEdgePane;

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

    private static final Pattern IMG_SRC = Pattern.compile(
            "(?i)<img\\b[^>]*\\bsrc\\s*=\\s*['\"]([^'\"]+)['\"]");

    private record Loaded(File reportDir, File edbDir, EnrichmentDb edb) {
    }

    private record PhenotypeInfo(String shortName, String longName, boolean positive) {
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
    public Node create(Report report, Consumer<ViewPage> openPage) {
        Consumer<ViewPage> open = ReportExplorerSupport.safeOpen(openPage);
        File reportDir = ReportExplorerSupport.reportDir(report);
        BorderPane host = new BorderPane();
        host.getStyleClass().add("gsea-report-explorer");

        ReportExplorerSupport.loadAsync(host, "gsea-report-explorer",
                "Loading enrichment results…",
                () -> {
                    File edbDir = ReportExplorerSupport.resolveEdbDir(reportDir);
                    return new Loaded(reportDir, edbDir, ParserFactory.readEdb(edbDir, true));
                },
                data -> buildUi(data, open),
                "Could not load enrichment database");
        return host;
    }

    private static Node buildUi(Loaded data, Consumer<ViewPage> openPage) {
        EnrichmentDb edb = data.edb();
        PhenotypeInfo pos = phenotypeInfo(edb, true);
        PhenotypeInfo neg = phenotypeInfo(edb, false);
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
        openLe.setOnAction(e -> openLeadingEdge(leDir, openPage));

        Button openEm = new Button("Open in Enrichment Map");
        xapps.gsea.fx.FxButtons.styleSecondary(openEm);
        openEm.setOnAction(e -> openEnrichmentMap(data.reportDir(), openPage));

        HBox toolbar = new HBox(10, filter, openLe, openEm);
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
        wireExclusiveSelection(posTable, negTable, pos, neg, data.reportDir(), detail);

        SplitPane phenotypeSplit = new SplitPane(
                phenotypePanel(pos, posStats, totalSets, posTable),
                phenotypePanel(neg, negStats, totalSets, negTable));
        phenotypeSplit.setOrientation(Orientation.HORIZONTAL);
        phenotypeSplit.setDividerPositions(0.5);

        SplitPane mainSplit = new SplitPane(phenotypeSplit, detail);
        mainSplit.setOrientation(Orientation.VERTICAL);
        mainSplit.setDividerPositions(0.58);
        VBox.setVgrow(mainSplit, Priority.ALWAYS);

        VBox enrichment = new VBox(10, overviewBar(edb, pos, neg, posStats, negStats), toolbar, mainSplit);
        enrichment.setPadding(new Insets(12));
        enrichment.getStyleClass().add("gsea-enrichment-root");

        return ReportExplorerSupport.tabPane(
                ReportExplorerSupport.fixedTab("Enrichment", enrichment),
                ReportExplorerSupport.fixedTab("Plots", plotsTab(data.reportDir())));
    }

    private static void wireExclusiveSelection(
            EnrichmentResultTable primary, EnrichmentResultTable other,
            PhenotypeInfo primaryInfo, PhenotypeInfo otherInfo,
            File reportDir, BorderPane detail) {
        listenSelection(primary, other, primaryInfo, reportDir, detail);
        listenSelection(other, primary, otherInfo, reportDir, detail);
    }

    private static void listenSelection(
            EnrichmentResultTable source, EnrichmentResultTable peer,
            PhenotypeInfo info, File reportDir, BorderPane detail) {
        source.getTable().getSelectionModel().selectedItemProperty().addListener((obs, o, row) -> {
            if (row != null) {
                peer.clearSelection();
                detail.setCenter(buildDetail(reportDir, row, info));
            } else if (peer.getTable().getSelectionModel().getSelectedItem() == null) {
                detail.setCenter(emptyDetail());
            }
        });
    }

    private static Node overviewBar(EnrichmentDb edb, PhenotypeInfo pos, PhenotypeInfo neg,
            PhenoStats posStats, PhenoStats negStats) {
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
                statChip(String.valueOf(edb.getNumResults()), "gene sets"),
                statChip(String.valueOf(genes), "genes in ranking"),
                statChip(String.valueOf(edb.getNumPerm()), "permutations"),
                statChip(String.valueOf(posStats.enriched()), "↑ " + pos.shortName()),
                statChip(String.valueOf(negStats.enriched()), "↑ " + neg.shortName()));
        return chips;
    }

    private static Node phenotypePanel(PhenotypeInfo pheno, PhenoStats stats, int totalSets,
            EnrichmentResultTable table) {
        Label title = new Label(pheno.longName());
        title.getStyleClass().addAll("gsea-pheno-title",
                pheno.positive() ? "gsea-pheno-pos" : "gsea-pheno-neg");
        title.setWrapText(true);

        FlowPane chips = new FlowPane(6, 6);
        chips.getChildren().addAll(
                statChip(stats.enriched() + " / " + totalSets, "enriched in " + pheno.shortName()),
                statChip(String.valueOf(stats.fdr25()), "FDR < 25%"),
                statChip(String.valueOf(stats.fdr5()), "FDR < 5%"),
                statChip(String.valueOf(stats.nom1()), "NOM p < 1%"),
                statChip(String.valueOf(stats.nom5()), "NOM p < 5%"));

        VBox header = new VBox(6, title, chips);
        header.setPadding(new Insets(10, 12, 8, 12));

        BorderPane panel = new BorderPane(table.getTable());
        panel.setTop(header);
        BorderPane.setMargin(table.getTable(), new Insets(0, 8, 8, 8));
        panel.getStyleClass().addAll("gsea-pheno-panel",
                pheno.positive() ? "gsea-pheno-panel-pos" : "gsea-pheno-panel-neg");
        return panel;
    }

    private static Node statChip(String value, String label) {
        Label valueLbl = new Label(value);
        valueLbl.getStyleClass().add("gsea-stat-value");
        Label labelLbl = new Label(label);
        labelLbl.getStyleClass().add("gsea-stat-label");
        VBox chip = new VBox(1, valueLbl, labelLbl);
        chip.getStyleClass().add("gsea-stat-chip");
        return chip;
    }

    private static Node emptyDetail() {
        Label hint = new Label(
                "Select a gene set in either phenotype panel to view its enrichment plot and members.");
        hint.getStyleClass().add("gsea-muted");
        hint.setWrapText(true);
        hint.setPadding(new Insets(16));
        return hint;
    }

    private static Node buildDetail(File reportDir, EnrichmentResultRow row, PhenotypeInfo pheno) {
        String setName = row.nameProperty().get();
        File tsv = findGeneSetSibling(reportDir, setName, ".tsv");
        File html = findGeneSetSibling(reportDir, setName, ".html");
        List<NamedPlot> plots = findGeneSetPlots(reportDir, setName, html);

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
                statChip(fmt(row.nesProperty().get()), "NES"),
                statChip(fmt(row.esProperty().get()), "ES"),
                statChip(fmt(row.fdrProperty().get()), "FDR q"),
                statChip(fmt(row.nomPProperty().get()), "NOM p"),
                statChip(fmt(row.fwerProperty().get()), "FWER"),
                statChip(String.valueOf(row.sizeProperty().get()), "size"),
                statChip(String.valueOf(row.rankAtMaxProperty().get()), "rank at max"));

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
        if (plots.isEmpty()) {
            tabs.add(ReportExplorerSupport.fixedTab("Plots",
                    ReportExplorerSupport.messagePane("No enrichment plots found for this gene set.")));
        } else {
            for (NamedPlot plot : plots) {
                tabs.add(ReportExplorerSupport.fixedTab(plot.title(),
                        ReportExplorerSupport.singleImage(plot.file())));
            }
        }
        if (tsv != null) {
            tabs.add(ReportExplorerSupport.fixedTab("Members", ReportExplorerSupport.tsvTable(tsv)));
        }
        TabPane detailTabs = ReportExplorerSupport.tabPane(tabs.toArray(Tab[]::new));

        VBox box = new VBox(10, titleRow, metrics, actions, detailTabs);
        VBox.setVgrow(detailTabs, Priority.ALWAYS);
        box.setPadding(new Insets(12));
        return box;
    }

    /**
     * Per-set HTML typically embeds: enrichment plot, optional Blue-Pink heatmap,
     * and null ES distribution. Prefer {@code <img src>} from the gene-set HTML.
     */
    private static List<NamedPlot> findGeneSetPlots(File reportDir, String geneSetName, File html) {
        List<NamedPlot> fromHtml = plotsFromHtml(reportDir, geneSetName, html);
        return fromHtml.isEmpty() ? plotsFromFiles(reportDir, geneSetName) : fromHtml;
    }

    private static List<NamedPlot> plotsFromHtml(File reportDir, String geneSetName, File html) {
        List<NamedPlot> plots = new ArrayList<>();
        if (html == null || !html.isFile()) {
            return plots;
        }
        for (String src : extractImgSrcs(html)) {
            File image = resolveReportImage(reportDir, src);
            if (image != null) {
                plots.add(new NamedPlot(plotTitle(fileName(src), geneSetName), image));
            }
        }
        return plots;
    }

    private static List<NamedPlot> plotsFromFiles(File reportDir, String geneSetName) {
        List<NamedPlot> plots = new ArrayList<>();
        if (reportDir == null || geneSetName == null) {
            return plots;
        }
        String lowerName = geneSetName.toLowerCase(Locale.ROOT);
        File enplot = ReportExplorerSupport.findFirst(reportDir, n -> {
            String lower = n.toLowerCase(Locale.ROOT);
            return lower.startsWith("enplot_" + lowerName) && ReportExplorerSupport.isImageName(n);
        });
        if (enplot != null) {
            plots.add(new NamedPlot("Enrichment plot", enplot));
        }
        File heatmap = ReportExplorerSupport.findFirst(reportDir, n -> {
            String lower = n.toLowerCase(Locale.ROOT);
            return lower.startsWith(lowerName + "_") && ReportExplorerSupport.isImageName(n)
                    && !lower.startsWith("enplot_");
        });
        if (heatmap != null) {
            plots.add(new NamedPlot("Heatmap", heatmap));
        }
        return plots;
    }

    private static List<String> extractImgSrcs(File html) {
        List<String> srcs = new ArrayList<>();
        try {
            Matcher m = IMG_SRC.matcher(Files.readString(html.toPath()));
            while (m.find()) {
                String src = m.group(1).trim();
                if (!src.isEmpty() && !srcs.contains(src)) {
                    srcs.add(src);
                }
            }
        } catch (Throwable ignored) {
            // caller falls back to filesystem patterns
        }
        return srcs;
    }

    private static File resolveReportImage(File reportDir, String src) {
        String name = fileName(src);
        if (!ReportExplorerSupport.isImageName(name)) {
            return null;
        }
        if (reportDir != null) {
            File inReport = new File(reportDir, name);
            if (inReport.isFile()) {
                return inReport;
            }
        }
        File asPath = new File(src);
        return asPath.isFile() ? asPath : null;
    }

    private static String plotTitle(String fileName, String geneSetName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.startsWith("enplot_")) {
            return "Enrichment plot";
        }
        if (lower.startsWith("gset_rnd_es_dist")) {
            return "Null ES distribution";
        }
        if (geneSetName != null && lower.startsWith(geneSetName.toLowerCase(Locale.ROOT) + "_")) {
            return "Heatmap";
        }
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static String fileName(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static File findGeneSetSibling(File reportDir, String geneSetName, String ext) {
        if (reportDir == null || geneSetName == null) {
            return null;
        }
        File exact = new File(reportDir, geneSetName + ext);
        return exact.isFile() ? exact : null;
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

    /** Naming mirrors EnrichmentReports classA/classB labels. */
    private static PhenotypeInfo phenotypeInfo(EnrichmentDb edb, boolean positive) {
        Template template = null;
        try {
            template = edb.getTemplate();
        } catch (Throwable ignored) {
            // preranked / missing template
        }
        if (template == null) {
            String shortName = Constants.NA + (positive ? "_pos" : "_neg");
            return new PhenotypeInfo(shortName,
                    positive ? "positive correlation with profile" : "negative correlation with profile",
                    positive);
        }
        if (template.isContinuous()) {
            String nn = AuxUtils.getAuxNameOnlyNoHash(template);
            return new PhenotypeInfo(nn + (positive ? "_pos" : "_neg"),
                    positive ? "positive correlation with profile" : "negative correlation with profile",
                    positive);
        }
        try {
            int idx = positive ? 0 : 1;
            String shortName = template.getClassName(idx);
            return new PhenotypeInfo(shortName,
                    shortName + " (" + template.getClass(idx).getSize() + " samples)",
                    positive);
        } catch (Throwable t) {
            String shortName = Constants.NA + (positive ? "_pos" : "_neg");
            return new PhenotypeInfo(shortName, shortName, positive);
        }
    }

    private static String fmt(float v) {
        return Float.isNaN(v) ? "NaN" : String.format(Locale.ROOT, "%.4g", v);
    }

    private static void openLeadingEdge(File edbOrReportDir, Consumer<ViewPage> openPage) {
        try {
            FxLeadingEdgePane pane = new FxLeadingEdgePane();
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
}
