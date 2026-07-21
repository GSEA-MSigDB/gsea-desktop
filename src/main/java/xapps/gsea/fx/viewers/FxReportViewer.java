/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import org.gsea_msigdb.gsea.ui.api.ViewPage;

import edu.mit.broad.genome.reports.api.Report;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import xapps.gsea.fx.tui.FxToolRelaunch;
import xapps.gsea.fx.viewers.report.ReportExplorerRegistry;
import xapps.gsea.fx.viewers.report.ReportKind;

/**
 * Native report explorer shell: type-aware Results tab, plus Parameters and Files.
 * Keeps Open HTML report / Show in ToolRunner as secondary actions.
 */
public class FxReportViewer implements ViewPage {

    private final Report report;
    private final ReportKind kind;
    private final BorderPane root = new BorderPane();
    private final Consumer<ViewPage> openPage;
    private final CheckBox loadDataCheck = new CheckBox("Load data");

    public FxReportViewer(Report report, Consumer<ViewPage> openPage) {
        this.report = report;
        this.openPage = openPage != null ? openPage : page -> { };
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
        showInToolRunner.setGraphic(xapps.gsea.fx.FxFileIcons.forResource("ToolLauncher.gif"));
        xapps.gsea.fx.FxButtons.stylePrimary(showInToolRunner);
        xapps.gsea.fx.FxButtons.sizeToContent(showInToolRunner);
        showInToolRunner.setOnAction(e -> FxToolRelaunch.showInToolRunner(
                report, loadDataCheck.isSelected(), this.openPage));

        Button openHtml = new Button("Open HTML report");
        openHtml.setTooltip(new javafx.scene.control.Tooltip(
                "Open the classic HTML report index in your system browser"));
        xapps.gsea.fx.FxButtons.styleSecondary(openHtml);
        xapps.gsea.fx.FxButtons.sizeToContent(openHtml);
        openHtml.setOnAction(e -> xapps.gsea.fx.FxReportOpen.openInBrowser(report));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(10, loadDataCheck, spacer, showInToolRunner, openHtml);
        toolbar.getStyleClass().add("gsea-report-action-bar");
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(8, 14, 8, 14));

        Node results = ReportExplorerRegistry.forKind(kind).create(report, this.openPage);
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
        Tab paramsTab = new Tab("Parameters", buildParamsPane());
        paramsTab.setClosable(false);
        Tab filesTab = new Tab("Files", buildFilesPane());
        filesTab.setClosable(false);
        tabs.getTabs().addAll(resultsTab, paramsTab, filesTab);

        VBox box = new VBox(header, toolbar, tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        root.setCenter(box);
        root.setMinSize(0, 0);
    }

    private VBox buildParamsPane() {
        TableView<ParamRow> table = new TableView<>();
        TableColumn<ParamRow, String> nameCol = new TableColumn<>("Parameter name");
        nameCol.setCellValueFactory(c -> c.getValue().nameProperty());
        nameCol.setPrefWidth(220);
        TableColumn<ParamRow, String> valueCol = new TableColumn<>("Parameter value");
        valueCol.setCellValueFactory(c -> c.getValue().valueProperty());
        valueCol.setPrefWidth(420);
        table.getColumns().addAll(nameCol, valueCol);
        table.setEditable(false);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        Properties params = report.getParametersUsed();
        List<ParamRow> rows = new ArrayList<>();
        if (params != null) {
            for (String key : params.stringPropertyNames()) {
                rows.add(new ParamRow(key, params.getProperty(key)));
            }
        }
        table.setItems(FXCollections.observableArrayList(rows));
        table.setPlaceholder(new Label(""));

        VBox box = new VBox(table);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(8, 12, 12, 12));
        return box;
    }

    private VBox buildFilesPane() {
        ListView<File> filesList = FxReportFilesList.create(report.getFilesProduced(), openPage);
        Label hint = new Label("Files produced as part of this analysis (double-click to view)");
        hint.getStyleClass().add("gsea-muted");
        VBox box = new VBox(8, hint, filesList);
        box.setPadding(new Insets(8, 12, 12, 12));
        VBox.setVgrow(filesList, Priority.ALWAYS);
        return box;
    }

    private static final class ParamRow {
        private final SimpleStringProperty name = new SimpleStringProperty();
        private final SimpleStringProperty value = new SimpleStringProperty();

        ParamRow(String name, String value) {
            this.name.set(name);
            this.value.set(value != null ? value : "");
        }

        SimpleStringProperty nameProperty() {
            return name;
        }

        SimpleStringProperty valueProperty() {
            return value;
        }
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
    public Object getContent() {
        return root;
    }
}
