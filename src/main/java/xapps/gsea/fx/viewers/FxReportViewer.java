/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers;

import java.io.File;
import java.util.Date;
import java.util.Objects;

import org.gsea_msigdb.gsea.ui.api.FeatureHost;
import org.gsea_msigdb.gsea.ui.api.ViewPage;

import edu.mit.broad.genome.reports.api.Report;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import xapps.gsea.fx.tui.FxToolRelaunch;
import xapps.gsea.fx.viewers.report.GseaReportExplorer;
import xapps.gsea.fx.viewers.report.ReportExplorerRegistry;
import xapps.gsea.fx.viewers.report.ReportKind;
import xapps.gsea.fx.viewers.report.ReportParamsTable;

/**
 * Native report explorer shell: type-aware Results tab, plus Parameters and Files.
 * Keeps Open HTML report / Show in ToolRunner as secondary actions.
 */
public class FxReportViewer implements ViewPage {

    private final Report report;
    private final ReportKind kind;
    private final BorderPane root = new BorderPane();
    private final FeatureHost host;
    private final CheckBox loadDataCheck = new CheckBox("Load data");

    public FxReportViewer(Report report, FeatureHost host) {
        this.report = report;
        this.host = Objects.requireNonNull(host, "host");
        this.kind = ReportKind.from(report);

        root.getStyleClass().add("gsea-report-shell");

        Label title = new Label(report.getName());
        title.getStyleClass().add("gsea-report-title");
        title.setWrapText(true);

        Label kindBadge = new Label(kind.getDisplayName());
        kindBadge.getStyleClass().add("gsea-report-kind");

        Label meta = new Label(new Date(report.getTimestamp()).toString());
        meta.getStyleClass().add("gsea-muted");

        HBox titleRow = new HBox(10, title, kindBadge);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);

        VBox header = new VBox(4, titleRow, meta);
        header.getStyleClass().add("gsea-report-header");
        header.setPadding(new Insets(12, 14, 8, 14));

        loadDataCheck.setSelected(true);
        loadDataCheck.setTooltip(new javafx.scene.control.Tooltip(
                "Follow files specified in params and load their data"));

        Button showInToolRunner = new Button("Show in ToolRunner");
        showInToolRunner.setGraphic(xapps.gsea.fx.widgets.FxFileIcons.forResource("ToolLauncher.gif"));
        xapps.gsea.fx.widgets.FxButtons.stylePrimary(showInToolRunner);
        xapps.gsea.fx.widgets.FxButtons.sizeToContent(showInToolRunner);
        showInToolRunner.setOnAction(e -> FxToolRelaunch.showInToolRunner(
                report, loadDataCheck.isSelected(), host));

        Button openHtml = new Button("Open HTML report");
        openHtml.setTooltip(new javafx.scene.control.Tooltip(
                "Open the classic HTML report index in your system browser"));
        xapps.gsea.fx.widgets.FxButtons.styleSecondary(openHtml);
        xapps.gsea.fx.widgets.FxButtons.sizeToContent(openHtml);
        openHtml.setOnAction(e -> xapps.gsea.fx.viewers.report.FxReportOpen.openInBrowser(report));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(10, loadDataCheck, spacer, showInToolRunner, openHtml);
        toolbar.getStyleClass().add("gsea-report-action-bar");
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(8, 14, 8, 14));

        Node results = ReportExplorerRegistry.forKind(kind).create(report, host);
        if (results instanceof javafx.scene.layout.Region region) {
            region.setMinSize(0, 0);
            region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }

        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("gsea-report-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setMinSize(0, 0);
        Tab resultsTab = new Tab("Results", results);
        resultsTab.setClosable(false);
        tabs.getTabs().add(resultsTab);

        if (kind == ReportKind.GSEA || kind == ReportKind.GSEA_PRERANKED) {
            Node plots = GseaReportExplorer.createSummaryPlots(report);
            if (plots != null) {
                if (plots instanceof javafx.scene.layout.Region region) {
                    region.setMinSize(0, 0);
                    region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                }
                BorderPane plotsPane = new BorderPane(plots);
                plotsPane.setPadding(new Insets(8, 12, 12, 12));
                Tab plotsTab = new Tab("Plots", plotsPane);
                plotsTab.setClosable(false);
                tabs.getTabs().add(plotsTab);
            }
        }

        Tab paramsTab = new Tab("Parameters", buildParamsPane());
        paramsTab.setClosable(false);
        Tab filesTab = new Tab("Files", buildFilesPane());
        filesTab.setClosable(false);
        tabs.getTabs().addAll(paramsTab, filesTab);

        VBox box = new VBox(header, toolbar, tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        root.setCenter(box);
        root.setMinSize(0, 0);
    }

    private Node buildParamsPane() {
        TableView<ReportParamsTable.Row> table = ReportParamsTable.create(report.getParametersUsed());
        BorderPane pane = new BorderPane(table);
        pane.setPadding(new Insets(8, 12, 12, 12));
        return pane;
    }

    private Node buildFilesPane() {
        ListView<File> files = FxReportFilesList.create(report, host::openPage);
        Label hint = new Label("All files in the report folder (double-click to view)");
        VBox box = new VBox(8, hint, files);
        box.setPadding(new Insets(8, 12, 12, 12));
        VBox.setVgrow(files, Priority.ALWAYS);
        return box;
    }

    @Override
    public String getTitle() {
        return report.getName();
    }

    @Override
    public String getIconResourceId() {
        return "past_analysis16.gif";
    }

    @Override
    public Node getContent() {
        return root;
    }
}
