/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.gsea_msigdb.gsea.ui.api.ViewPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.genome.utils.DateUtils;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.tui.ReportStub;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import xapps.gsea.fx.FxReportOpen;
import xapps.gsea.fx.viewers.report.ReportExplorerSupport;

/**
 * Analysis history browser / Past Analysis:
 * Current Session + History grouped by day; selection opens the report viewer.
 */
public class FxAnalysisHistoryPane implements ViewPage {

    private static final Logger klog = LoggerFactory.getLogger(FxAnalysisHistoryPane.class);

    private final BorderPane root = new BorderPane();
    private final TreeView<HistoryNode> tree = new TreeView<>();
    private final Consumer<ViewPage> openPage;
    private final Set<String> sessionReportNames = new HashSet<>();
    private PropertyChangeListener sessionListener;
    private final BorderPane detailPane = new BorderPane();
    /** Bumped on each selection change so stale async loads cannot overwrite a newer detail. */
    private int detailLoadSeq = 0;

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
                    setGraphic(xapps.gsea.fx.FxFileIcons.reportStubIcon());
                    Label name = new Label(item.displayName != null ? item.displayName : item.label);
                    Label time = new Label(item.timeSuffix != null ? item.timeSuffix : "");
                    time.getStyleClass().add("gsea-muted");
                    setText(null);
                    setGraphic(new javafx.scene.layout.HBox(4,
                            xapps.gsea.fx.FxFileIcons.reportStubIcon(),
                            new javafx.scene.layout.HBox(name, time)));
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
        tree.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> updateDetail());

        detailPane.setCenter(naPlaceholder());
        VBox right = new VBox(detailPane);
        VBox.setVgrow(detailPane, Priority.ALWAYS);

        javafx.scene.control.TitledPane treeFrame = new javafx.scene.control.TitledPane(
                "Analysis history", tree);
        treeFrame.setCollapsible(false);
        treeFrame.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        SplitPane split = new SplitPane(treeFrame, right);
        split.setDividerPositions(0.42);

        root.setCenter(split);

        rebuildTree();
        installSessionListener();
    }

    private static javafx.scene.Node naPlaceholder() {
        Label text = new Label("<No Available Component>");
        javafx.scene.image.ImageView icon = xapps.gsea.fx.FxFileIcons.forResource("NAComponent.gif");
        HBox box = new HBox(8);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        if (icon != null) {
            box.getChildren().add(icon);
        }
        box.getChildren().add(text);
        BorderPane wrap = new BorderPane(box);
        wrap.getStyleClass().add("gsea-na-placeholder");
        return wrap;
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
        session.setExpanded(!expandedLabels.isEmpty() && expandedLabels.contains("Current Session"));
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
        history.setExpanded(!expandedLabels.isEmpty() && expandedLabels.contains("History"));
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

    private void updateDetail() {
        final int seq = ++detailLoadSeq;
        HistoryNode node = selectedNode();
        if (node == null || (node.report == null && node.stub == null)) {
            detailPane.setCenter(naPlaceholder());
            return;
        }
        try {
            if (node.report != null) {
                detailPane.setCenter((Node) FxReportOpen.viewPageFor(node.report, openPage).getContent());
                return;
            }
            // History stubs: read .rpt off the FX thread so large reports don't freeze selection.
            final ReportStub stub = node.stub;
            detailPane.setCenter(ReportExplorerSupport.loadingPlaceholder("Loading report…"));
            Thread t = new Thread(() -> {
                try {
                    Report report = stub.getReport(false);
                    Platform.runLater(() -> {
                        if (seq != detailLoadSeq) {
                            return;
                        }
                        if (report != null) {
                            detailPane.setCenter((Node) FxReportOpen.viewPageFor(report, openPage).getContent());
                        } else {
                            detailPane.setCenter(naPlaceholder());
                        }
                    });
                } catch (Throwable err) {
                    klog.warn("Could not embed report viewer", err);
                    Platform.runLater(() -> {
                        if (seq != detailLoadSeq) {
                            return;
                        }
                        Application.getWindowManager().showError("Bad reports file", err);
                        if (stub.getReportFile() != null) {
                            stub.getReportFile().deleteOnExit();
                        }
                        detailPane.setCenter(naPlaceholder());
                    });
                }
            }, "gsea-history-report");
            t.setDaemon(true);
            t.start();
        } catch (Throwable t) {
            klog.warn("Could not embed report viewer", t);
            Application.getWindowManager().showError("Bad reports file", t);
            if (node.stub != null && node.stub.getReportFile() != null) {
                node.stub.getReportFile().deleteOnExit();
            }
            detailPane.setCenter(naPlaceholder());
        }
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
