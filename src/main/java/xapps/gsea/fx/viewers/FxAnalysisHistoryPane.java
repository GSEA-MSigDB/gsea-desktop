/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import org.gsea_msigdb.gsea.ui.api.ViewPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.genome.utils.DateUtils;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.tui.ReportStub;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import xapps.gsea.fx.FxReportOpen;
import xapps.gsea.fx.jobs.JobDisplay;
import xapps.gsea.fx.jobs.JobRuntime;
import xapps.gsea.fx.tui.FxToolRelaunch;
import xapps.gsea.fx.viewers.report.ReportExplorerSupport;
import xapps.gsea.fx.viewers.report.ReportKind;
import xapps.gsea.fx.viewers.report.ReportParamsTable;
import xapps.gsea.fx.viewers.report.ReportStatChips;

/**
 * Analysis history browser / Past Analysis:
 * Current Session + History grouped by day. Selection shows parameter preview and
 * (for GSEA kinds) enrichment summary; double-click / Enter opens the full report.
 */
public class FxAnalysisHistoryPane implements ViewPage {

    private static final Logger klog = LoggerFactory.getLogger(FxAnalysisHistoryPane.class);

    private final BorderPane root = new BorderPane();
    private final TreeView<HistoryNode> tree = new TreeView<>();
    private final BorderPane detailHost = new BorderPane();
    private final Consumer<ViewPage> openPage;
    private final Set<String> sessionReportNames = new HashSet<>();
    private PropertyChangeListener sessionListener;
    /** Bumped on each open request so stale async loads cannot open a superseded report. */
    private int openSeq = 0;
    /** Bumped on each selection so stale preview loads are ignored. */
    private int detailSeq = 0;

    public FxAnalysisHistoryPane(Consumer<ViewPage> openPage) {
        this.openPage = openPage != null ? openPage : page -> { };

        tree.setShowRoot(true);
        tree.setCellFactory(tv -> new javafx.scene.control.TreeCell<>() {
            @Override
            protected void updateItem(HistoryNode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }
                if (item.report != null || item.stub != null) {
                    Label name = new Label(item.displayName != null ? item.displayName : item.label);
                    Label time = new Label(item.timeSuffix != null ? item.timeSuffix : "");
                    time.getStyleClass().add("gsea-muted");
                    setText(null);
                    setGraphic(new HBox(4,
                            xapps.gsea.fx.FxFileIcons.reportStubIcon(),
                            new HBox(name, time)));
                    if (item.report != null && item.report.getQuickInfo() != null) {
                        setTooltip(new javafx.scene.control.Tooltip(item.report.getQuickInfo()));
                    } else {
                        setTooltip(null);
                    }
                } else {
                    setText(item.label);
                    setGraphic(null);
                    setTooltip(null);
                }
            }
        });
        tree.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                openSelectedReport();
                e.consume();
            }
        });
        tree.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                openSelectedReport();
                e.consume();
            }
        });
        tree.getSelectionModel().selectedItemProperty().addListener((obs, o, n) ->
                showDetail(n != null ? n.getValue() : null));

        Label hint = new Label("Select a report to preview. Double-click or Enter opens the full report.");
        hint.getStyleClass().add("gsea-muted");
        hint.setPadding(new Insets(6, 10, 8, 10));

        detailHost.getStyleClass().add("gsea-history-detail");
        detailHost.setCenter(emptyDetailHint());

        VBox treeSection = new VBox(tree, hint);
        VBox.setVgrow(tree, Priority.ALWAYS);

        SplitPane split = new SplitPane(treeSection, detailHost);
        split.setOrientation(Orientation.HORIZONTAL);
        split.setDividerPositions(0.42);
        root.setCenter(split);
        root.setMinSize(0, 0);

        rebuildTree();
        installSessionListener();
    }

    private void installSessionListener() {
        sessionListener = evt -> {
            Object obj = evt.getNewValue();
            if (obj instanceof Report) {
                Platform.runLater(this::rebuildTree);
            }
        };
        ParserFactory.getCache().addReportAdditionsListener(sessionListener);
    }

    private void rebuildTree() {
        HistoryNode previouslySelected = selectedNode();
        java.util.Set<String> expandedLabels = new java.util.HashSet<>();
        TreeItem<HistoryNode> oldRoot = tree.getRoot();
        if (oldRoot != null) {
            collectExpandedLabels(oldRoot, expandedLabels);
        }

        sessionReportNames.clear();
        TreeItem<HistoryNode> rootItem = new TreeItem<>(new HistoryNode("Reports", null, null));
        rootItem.setExpanded(true);

        TreeItem<HistoryNode> session = new TreeItem<>(new HistoryNode("Current Session", null, null));
        session.setExpanded(expandedLabels.isEmpty() || expandedLabels.contains("Current Session"));
        try {
            @SuppressWarnings("unchecked")
            List<Object> pobs = ParserFactory.getCache().getCachedObjectsL(Report.class);
            List<Report> reports = new ArrayList<>();
            for (Object o : pobs) {
                if (o instanceof Report) {
                    reports.add((Report) o);
                }
            }
            reports.sort(Comparator.comparingLong(Report::getTimestamp).reversed());
            for (Report rpt : reports) {
                sessionReportNames.add(rpt.getName());
                session.getChildren().add(new TreeItem<>(new HistoryNode(
                        rpt.getName(),
                        "[" + DateUtils.formatAsHourMin(rpt.getDate()) + "min]",
                        rpt, null)));
            }
        } catch (Throwable t) {
            klog.warn("Could not load session reports", t);
        }

        TreeItem<HistoryNode> history = new TreeItem<>(new HistoryNode("History", null, null));
        history.setExpanded(expandedLabels.isEmpty() || expandedLabels.contains("History"));
        try {
            ReportStub[] stubs = Application.getToolManager().getReportsInCache();
            Map<String, List<ReportStub>> byDay = new HashMap<>();
            Map<String, Long> dayTs = new HashMap<>();
            for (ReportStub stub : stubs) {
                if (sessionReportNames.contains(stub.getName())) {
                    continue;
                }
                String day = DateUtils.formatAsDayMonthYear(stub.getDate());
                byDay.computeIfAbsent(day, k -> new ArrayList<>()).add(stub);
                dayTs.merge(day, stub.getTimestamp(), Math::max);
            }
            List<String> days = new ArrayList<>(byDay.keySet());
            days.sort((a, b) -> Long.compare(dayTs.getOrDefault(b, 0L), dayTs.getOrDefault(a, 0L)));
            for (String day : days) {
                TreeItem<HistoryNode> dayNode = new TreeItem<>(new HistoryNode(day, null, null));
                dayNode.setExpanded(!expandedLabels.isEmpty() && expandedLabels.contains(day));
                List<ReportStub> list = byDay.get(day);
                list.sort(Comparator.comparingLong(ReportStub::getTimestamp).reversed());
                for (ReportStub stub : list) {
                    dayNode.getChildren().add(new TreeItem<>(new HistoryNode(
                            stub.getName_without_ts(),
                            "[" + DateUtils.formatAsHourMin(stub.getDate()) + "]",
                            null, stub)));
                }
                history.getChildren().add(dayNode);
            }
        } catch (Throwable t) {
            klog.warn("Could not load analysis history", t);
            Application.getWindowManager().showError("Could not load analysis history", t);
        }

        rootItem.getChildren().addAll(session, history);
        tree.setRoot(rootItem);
        if (previouslySelected != null) {
            TreeItem<HistoryNode> match = findHistoryNode(rootItem, previouslySelected);
            if (match != null) {
                tree.getSelectionModel().select(match);
            } else {
                showDetail(null);
            }
        } else {
            showDetail(null);
        }
    }

    private void showDetail(HistoryNode node) {
        final int seq = ++detailSeq;
        if (node == null || (node.report == null && node.stub == null)) {
            detailHost.setCenter(emptyDetailHint());
            return;
        }
        if (node.report != null) {
            detailHost.setCenter(buildDetail(node.report, seq));
            return;
        }
        detailHost.setCenter(loadingPane("Loading report…"));
        final ReportStub stub = node.stub;
        Thread t = new Thread(() -> {
            try {
                Report report = stub.getReport(false);
                Platform.runLater(() -> {
                    if (seq != detailSeq) {
                        return;
                    }
                    if (report != null) {
                        detailHost.setCenter(buildDetail(report, seq));
                    } else {
                        detailHost.setCenter(messagePane("Could not load report"));
                    }
                });
            } catch (Throwable err) {
                klog.warn("Could not load report preview from history", err);
                Platform.runLater(() -> {
                    if (seq != detailSeq) {
                        return;
                    }
                    detailHost.setCenter(messagePane("Could not load report: " + err.getMessage()));
                });
            }
        }, "gsea-history-preview");
        t.setDaemon(true);
        t.start();
    }

    private Node buildDetail(Report report, int seq) {
        ReportKind kind = ReportKind.from(report);

        Label title = new Label(report.getName());
        title.getStyleClass().add("gsea-report-title");
        title.setWrapText(true);

        Label kindBadge = new Label(kind.getDisplayName());
        kindBadge.getStyleClass().add("gsea-report-kind");

        Label meta = new Label(new Date(report.getTimestamp()).toString());
        meta.getStyleClass().add("gsea-muted");

        File reportDir = ReportExplorerSupport.reportDir(report);
        Label path = new Label(reportDir != null ? reportDir.getAbsolutePath() : "");
        path.getStyleClass().add("gsea-muted");
        path.setWrapText(true);

        HBox titleRow = new HBox(10, title, kindBadge);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);

        Button openBtn = new Button("Open report");
        xapps.gsea.fx.FxButtons.stylePrimary(openBtn);
        xapps.gsea.fx.FxButtons.sizeToContent(openBtn);
        openBtn.setOnAction(e -> FxReportOpen.openInApp(report, openPage, JobRuntime.require()));

        Button relaunchBtn = new Button("Show in ToolRunner");
        xapps.gsea.fx.FxButtons.styleSecondary(relaunchBtn);
        xapps.gsea.fx.FxButtons.sizeToContent(relaunchBtn);
        relaunchBtn.setOnAction(e -> FxToolRelaunch.showInToolRunner(
                report, true, openPage, JobRuntime.require()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(10, openBtn, relaunchBtn, spacer);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(4, 0, 8, 0));

        VBox header = new VBox(4, titleRow, meta, path, actions);
        header.setPadding(new Insets(10, 12, 4, 12));

        BorderPane summaryHost = new BorderPane();
        summaryHost.setPadding(new Insets(0, 12, 8, 12));
        if (kind == ReportKind.GSEA || kind == ReportKind.GSEA_PRERANKED) {
            summaryHost.setCenter(loadingPane("Loading enrichment summary…"));
            loadGseaSummaryAsync(report, reportDir, summaryHost, seq);
        }

        Properties params = report.getParametersUsed();
        VBox keyInputs = keyInputsPane(params);
        keyInputs.setPadding(new Insets(0, 12, 8, 12));

        Label paramsHeader = new Label("All parameters");
        paramsHeader.getStyleClass().add("gsea-section-header");
        TableView<ReportParamsTable.Row> paramsTable = ReportParamsTable.create(params);
        paramsTable.setPrefHeight(180);
        paramsTable.setMinHeight(100);
        VBox paramsBox = new VBox(6, paramsHeader, paramsTable);
        paramsBox.setPadding(new Insets(0, 12, 12, 12));
        VBox.setVgrow(paramsTable, Priority.ALWAYS);

        VBox content = new VBox(header, summaryHost, keyInputs, paramsBox);
        VBox.setVgrow(paramsBox, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("gsea-history-detail-scroll");
        return scroll;
    }

    private void loadGseaSummaryAsync(Report report, File reportDir, BorderPane summaryHost, int seq) {
        Thread t = new Thread(() -> {
            try {
                File edbDir = ReportExplorerSupport.resolveEdbDir(reportDir);
                EnrichmentDb edb = ParserFactory.readEdb(edbDir, true);
                Platform.runLater(() -> {
                    if (seq != detailSeq) {
                        return;
                    }
                    Label section = new Label("Enrichment summary");
                    section.getStyleClass().add("gsea-section-header");
                    VBox box = new VBox(8, section, ReportStatChips.gseaSummary(edb));
                    summaryHost.setCenter(box);
                });
            } catch (Throwable err) {
                klog.debug("No enrichment summary for {}", report.getName(), err);
                Platform.runLater(() -> {
                    if (seq != detailSeq) {
                        return;
                    }
                    summaryHost.setCenter(null);
                });
            }
        }, "gsea-history-summary");
        t.setDaemon(true);
        t.start();
    }

    private static VBox keyInputsPane(Properties params) {
        Label header = new Label("Key inputs");
        header.getStyleClass().add("gsea-section-header");
        String hover = JobDisplay.hoverText(params);
        VBox box = new VBox(4, header);
        if (hover == null || hover.isBlank()) {
            Label none = new Label("No primary inputs recorded");
            none.getStyleClass().add("gsea-muted");
            box.getChildren().add(none);
            return box;
        }
        for (String line : hover.split("\n")) {
            Label row = new Label(line);
            row.getStyleClass().add("gsea-muted");
            row.setWrapText(true);
            box.getChildren().add(row);
        }
        return box;
    }

    private static Node emptyDetailHint() {
        Label hint = new Label("Select a report to preview its parameters and summary statistics.");
        hint.getStyleClass().add("gsea-muted");
        hint.setWrapText(true);
        hint.setPadding(new Insets(16));
        return hint;
    }

    private static Node loadingPane(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("gsea-muted");
        label.setPadding(new Insets(12));
        return label;
    }

    private static Node messagePane(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("gsea-muted");
        label.setWrapText(true);
        label.setPadding(new Insets(16));
        return label;
    }

    private void openSelectedReport() {
        HistoryNode node = selectedNode();
        if (node == null || (node.report == null && node.stub == null)) {
            return;
        }
        final int seq = ++openSeq;
        try {
            if (node.report != null) {
                FxReportOpen.openInApp(node.report, openPage, JobRuntime.require());
                return;
            }
            final ReportStub stub = node.stub;
            Thread t = new Thread(() -> {
                try {
                    Report report = stub.getReport(false);
                    Platform.runLater(() -> {
                        if (seq != openSeq) {
                            return;
                        }
                        if (report != null) {
                            FxReportOpen.openInApp(report, openPage, JobRuntime.require());
                        } else {
                            Application.getWindowManager().showMessage("Could not load report");
                        }
                    });
                } catch (Throwable err) {
                    klog.warn("Could not open report from history", err);
                    Platform.runLater(() -> {
                        if (seq != openSeq) {
                            return;
                        }
                        Application.getWindowManager().showError("Bad reports file", err);
                        if (stub.getReportFile() != null) {
                            stub.getReportFile().deleteOnExit();
                        }
                    });
                }
            }, "gsea-history-report");
            t.setDaemon(true);
            t.start();
        } catch (Throwable t) {
            klog.warn("Could not open report from history", t);
            Application.getWindowManager().showError("Bad reports file", t);
            if (node.stub != null && node.stub.getReportFile() != null) {
                node.stub.getReportFile().deleteOnExit();
            }
        }
    }

    private static void collectExpandedLabels(TreeItem<HistoryNode> item, java.util.Set<String> out) {
        if (item == null) {
            return;
        }
        if (item.isExpanded() && item.getValue() != null && item.getValue().label != null) {
            out.add(item.getValue().label);
        }
        for (TreeItem<HistoryNode> child : item.getChildren()) {
            collectExpandedLabels(child, out);
        }
    }

    private static TreeItem<HistoryNode> findHistoryNode(TreeItem<HistoryNode> parent, HistoryNode target) {
        if (parent == null || target == null) {
            return null;
        }
        for (TreeItem<HistoryNode> child : parent.getChildren()) {
            HistoryNode v = child.getValue();
            if (v != null && historyNodeMatches(v, target)) {
                return child;
            }
            TreeItem<HistoryNode> nested = findHistoryNode(child, target);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private static boolean historyNodeMatches(HistoryNode a, HistoryNode b) {
        if (a.report != null && a.report == b.report) {
            return true;
        }
        if (a.stub != null && b.stub != null && a.stub.getName() != null
                && a.stub.getName().equals(b.stub.getName())) {
            return true;
        }
        return a.report == null && a.stub == null && b.report == null && b.stub == null
                && a.label != null && a.label.equals(b.label);
    }

    private HistoryNode selectedNode() {
        TreeItem<HistoryNode> sel = tree.getSelectionModel().getSelectedItem();
        return sel != null ? sel.getValue() : null;
    }

    @Override
    public String getTitle() {
        return "Analysis history";
    }

    @Override
    public String getIconResourceId() {
        return "past_analysis16.gif";
    }

    @Override
    public Object getContent() {
        return root;
    }

    private static final class HistoryNode {
        final String label;
        final String displayName;
        final String timeSuffix;
        final Report report;
        final ReportStub stub;

        HistoryNode(String label, Report report, ReportStub stub) {
            this(label, null, report, stub);
        }

        HistoryNode(String displayName, String timeSuffix, Report report, ReportStub stub) {
            this.displayName = displayName;
            this.timeSuffix = timeSuffix;
            this.label = displayName != null
                    ? displayName + (timeSuffix != null ? timeSuffix : "")
                    : "";
            this.report = report;
            this.stub = stub;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
