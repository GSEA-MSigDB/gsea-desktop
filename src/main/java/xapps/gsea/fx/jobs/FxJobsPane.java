/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.jobs;

import java.io.File;
import java.util.Properties;
import java.util.function.Consumer;

import org.gsea_msigdb.gsea.runtime.AppServices;
import org.gsea_msigdb.gsea.ui.api.FeatureHost;

import edu.mit.broad.xbench.tui.JobState;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import xapps.gsea.fx.widgets.FxButtons;
import xapps.gsea.fx.FxDesktopUtil;
import xapps.gsea.fx.viewers.report.FxReportOpen;
import xapps.gsea.fx.tui.FxToolRelaunch;
import xtools.api.Tool;

/**
 * Left-rail Jobs panel: selectable job list, management actions, and per-job log.
 */
public final class FxJobsPane {

    /** Preferred log viewport when expanded (~20 text rows + chrome). */
    private static final int LOG_PREF_ROWS = 20;
    private static final double LOG_ROW_PX = 15;
    private static final double LOG_CHROME_PX = 72;

    private final JobRuntime runtime;
    private final FeatureHost host;
    private final AppServices svc;
    private final BorderPane root = new BorderPane();
    private final ListView<JobRecord> list = new ListView<>();
    private final TextArea logArea = new TextArea();
    private final Label logToggleLabel = new Label();
    private final HBox logToggleBar = new HBox();
    private final VBox listSection = new VBox(4);
    private final BorderPane logPane = new BorderPane();
    private final SplitPane split = new SplitPane();
    private final VBox body = new VBox();
    private final Button openBtn = new Button("Open");
    private final Button cancelBtn = new Button("Cancel");
    private final Button relaunchBtn = new Button("Relaunch");
    private final Button folderBtn = new Button("Folder");
    private final Button clearLogBtn = new Button("Clear log");
    private final Button copyLogBtn = new Button("Copy");
    private LogBuffer attachedLog;
    private String logTitleText = "Log";
    private boolean logExpanded;
    private final Consumer<String> logListener = this::onLogChunk;
    private final javafx.beans.value.ChangeListener<JobState> stateListener =
            (obs, o, n) -> updateActions(list.getSelectionModel().getSelectedItem());
    private final Timeline elapsedTick;

    public FxJobsPane(FeatureHost host) {
        this.host = java.util.Objects.requireNonNull(host, "host");
        this.runtime = host.jobs();
        this.svc = host.services();

        list.setItems(runtime.getJobs());
        list.setPlaceholder(new Label("No jobs yet"));
        list.setCellFactory(lv -> new JobListCell());
        list.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (runtime.getSelectedJob() != n) {
                runtime.setSelectedJob(n);
            }
            bindLog(n);
            updateActions(n);
            if (o != null) {
                o.stateProperty().removeListener(stateListener);
            }
            if (n != null) {
                n.stateProperty().addListener(stateListener);
            }
        });
        runtime.selectedJobProperty().addListener((obs, o, n) -> {
            JobRecord current = list.getSelectionModel().getSelectedItem();
            if (n == current) {
                return;
            }
            if (n == null) {
                list.getSelectionModel().clearSelection();
            } else {
                list.getSelectionModel().select(n);
            }
        });
        list.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY || e.getClickCount() != 2) {
                return;
            }
            JobRecord job = list.getSelectionModel().getSelectedItem();
            if (job == null) {
                return;
            }
            JobState state = job.getState();
            boolean openResults = state != null && state.isSuccess()
                    && (job.getReportDir() != null || job.getReportIndex() != null);
            if (openResults) {
                openSelected();
            } else if (!logExpanded) {
                setLogExpanded(true);
            }
        });

        FxButtons.styleSecondary(openBtn);
        FxButtons.styleSecondary(cancelBtn);
        FxButtons.styleSecondary(relaunchBtn);
        FxButtons.styleSecondary(folderBtn);
        FxButtons.sizeToContent(openBtn);
        FxButtons.sizeToContent(cancelBtn);
        FxButtons.sizeToContent(relaunchBtn);
        FxButtons.sizeToContent(folderBtn);
        openBtn.setTooltip(new Tooltip("Open analysis results"));
        cancelBtn.setTooltip(new Tooltip("Cancel the selected running job"));
        relaunchBtn.setTooltip(new Tooltip("Reopen this job with its saved parameters"));
        folderBtn.setTooltip(new Tooltip("Show the job results folder (or default output directory)"));

        openBtn.setOnAction(e -> openSelected());
        cancelBtn.setOnAction(e -> cancelSelected());
        relaunchBtn.setOnAction(e -> relaunchSelected());
        folderBtn.setOnAction(e -> openFolder());

        HBox actions = FxButtons.row(openBtn, cancelBtn, relaunchBtn, folderBtn);
        actions.setPadding(new Insets(4, 8, 4, 8));
        actions.setAlignment(Pos.CENTER_LEFT);

        listSection.getChildren().setAll(list, actions);
        listSection.setPadding(new Insets(4, 0, 0, 0));
        VBox.setVgrow(list, Priority.ALWAYS);
        listSection.getStyleClass().add("gsea-jobs-pane");

        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefRowCount(LOG_PREF_ROWS);
        logArea.getStyleClass().add("gsea-job-log");

        FxButtons.styleSecondary(clearLogBtn);
        FxButtons.styleSecondary(copyLogBtn);
        FxButtons.sizeToContent(clearLogBtn);
        FxButtons.sizeToContent(copyLogBtn);
        clearLogBtn.setOnAction(e -> clearSelectedLog());
        copyLogBtn.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(logArea.getText());
            Clipboard.getSystemClipboard().setContent(content);
        });
        HBox logActions = FxButtons.row(clearLogBtn, copyLogBtn);
        logActions.setAlignment(Pos.CENTER_RIGHT);
        logActions.setPadding(new Insets(4, 8, 6, 8));

        logToggleLabel.getStyleClass().add("gsea-job-log-toggle");
        logToggleBar.getChildren().setAll(logToggleLabel);
        logToggleBar.setAlignment(Pos.CENTER_LEFT);
        logToggleBar.setPadding(new Insets(6, 8, 6, 8));
        logToggleBar.getStyleClass().add("gsea-job-log-toggle-bar");
        logToggleBar.setCursor(javafx.scene.Cursor.HAND);
        logToggleBar.setOnMouseClicked(e -> setLogExpanded(!logExpanded));
        Tooltip.install(logToggleBar, new Tooltip("Show or hide the job log"));

        logPane.setCenter(logArea);
        logPane.setBottom(logActions);
        logPane.getStyleClass().add("gsea-jobs-pane");
        logPane.setMinHeight(LOG_CHROME_PX + LOG_ROW_PX * 4);

        split.setOrientation(Orientation.VERTICAL);
        VBox.setVgrow(split, Priority.ALWAYS);
        VBox.setVgrow(listSection, Priority.ALWAYS);

        body.getStyleClass().add("gsea-jobs-pane");
        root.setCenter(body);
        root.getStyleClass().addAll("gsea-jobs-pane", "gsea-jobs-root");
        root.setPrefHeight(280);
        root.setMinHeight(200);

        setLogExpanded(false);
        updateActions(null);

        // Update elapsed labels only — do not list.refresh() (that recreates
        // tooltips and restyles status every second → flicker + dead hover).
        elapsedTick = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickElapsed()));
        elapsedTick.setCycleCount(Animation.INDEFINITE);
        elapsedTick.play();
    }

    public Node getNode() {
        return root;
    }

    /** Stop the elapsed-time refresh (e.g. on application quit). */
    public void dispose() {
        elapsedTick.stop();
        JobRecord selected = list.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.stateProperty().removeListener(stateListener);
        }
        if (attachedLog != null) {
            attachedLog.removeListener(logListener);
            attachedLog = null;
        }
    }

    private void setLogExpanded(boolean expanded) {
        logExpanded = expanded;
        refreshLogToggleLabel();
        if (expanded) {
            VBox logColumn = new VBox(logToggleBar, logPane);
            VBox.setVgrow(logPane, Priority.ALWAYS);
            split.getItems().setAll(listSection, logColumn);
            body.getChildren().setAll(split);
            root.setPrefHeight(520);
            Platform.runLater(this::sizeLogForPrefRows);
        } else {
            split.getItems().clear();
            body.getChildren().setAll(listSection, logToggleBar);
            root.setPrefHeight(280);
        }
    }

    private void sizeLogForPrefRows() {
        double total = split.getHeight();
        if (total <= 1) {
            return;
        }
        double logHeight = 28 + LOG_CHROME_PX + LOG_PREF_ROWS * LOG_ROW_PX;
        double pos = 1.0 - (logHeight / total);
        pos = Math.max(0.2, Math.min(0.8, pos));
        split.setDividerPositions(pos);
    }

    private void refreshLogToggleLabel() {
        logToggleLabel.setText((logExpanded ? "▼  " : "▶  ") + logTitleText);
    }

    private void tickElapsed() {
        for (Node node : list.lookupAll(".list-cell")) {
            if (node instanceof JobListCell) {
                ((JobListCell) node).refreshElapsed();
            }
        }
    }

    private void bindLog(JobRecord job) {
        if (attachedLog != null) {
            attachedLog.removeListener(logListener);
            attachedLog = null;
        }
        if (job == null) {
            logTitleText = "Log";
            refreshLogToggleLabel();
            logArea.clear();
            return;
        }
        logTitleText = "Log — " + job.getName();
        refreshLogToggleLabel();
        attachedLog = job.getLog();
        logArea.setText(attachedLog.getText());
        logArea.positionCaret(logArea.getLength());
        attachedLog.addListener(logListener);
    }

    private void onLogChunk(String chunk) {
        if (chunk == null) {
            JobRecord job = list.getSelectionModel().getSelectedItem();
            logArea.setText(job != null ? job.getLog().getText() : "");
            return;
        }
        logArea.appendText(chunk);
        logArea.positionCaret(logArea.getLength());
    }

    private void updateActions(JobRecord job) {
        boolean has = job != null;
        JobState state = has ? job.getState() : null;
        boolean canOpen = has && state != null && (
                (state.isSuccess() && (job.getReportDir() != null || job.getReportIndex() != null))
                        || state == JobState.ERROR
                        || state == JobState.INVALID_PARAM);
        openBtn.setDisable(!canOpen);
        cancelBtn.setDisable(!has || state == null || !state.isActive() || state == JobState.CANCELING);
        relaunchBtn.setDisable(!has || job.getTool() == null || job.getParamSnapshot() == null);
        folderBtn.setDisable(!has);
        clearLogBtn.setDisable(!has);
        copyLogBtn.setDisable(!has);
        openBtn.setText(state == JobState.ERROR || state == JobState.INVALID_PARAM ? "Error" : "Open");
    }

    private void openSelected() {
        JobRecord job = list.getSelectionModel().getSelectedItem();
        if (job == null) {
            return;
        }
        JobState state = job.getState();
        if (state == JobState.ERROR || state == JobState.INVALID_PARAM) {
            JobErrors.showIfNeeded(job);
            return;
        }
        if (state != null && state.isSuccess()) {
            FxReportOpen.openFromJob(job, host);
        }
    }

    private void cancelSelected() {
        JobRecord job = list.getSelectionModel().getSelectedItem();
        if (job == null || !job.getState().isActive()) {
            svc.dialogs().showMessage("No running job to cancel");
            return;
        }
        if (svc.dialogs().showConfirm(
                "Cancel job",
                "Cancel running job \"" + job.getName() + "\"?")) {
            runtime.cancel(job);
            updateActions(job);
        }
    }

    private void relaunchSelected() {
        JobRecord job = list.getSelectionModel().getSelectedItem();
        if (job == null) {
            return;
        }
        Tool tool = job.getTool();
        Properties paramSnapshot = job.getParamSnapshot();
        if (tool == null || paramSnapshot == null) {
            svc.dialogs().showMessage("No saved parameters",
                    "No saved parameters are available to reopen " + job.getName() + ".");
            return;
        }
        FxToolRelaunch.showInToolRunner(tool, paramSnapshot, host);
    }

    private void openFolder() {
        JobRecord job = list.getSelectionModel().getSelectedItem();
        File dir = null;
        if (job != null && job.getReportDir() != null) {
            dir = job.getReportDir();
        }
        if (dir == null) {
            dir = svc.vdb().getDefaultOutputDir();
        }
        if (dir == null) {
            return;
        }
        try {
            FxDesktopUtil.openInOsExplorer(dir);
        } catch (Exception e) {
            svc.dialogs().showError(
                    "Trouble launching File Explorer on path '" + dir.getPath() + "'", e);
        }
    }

    private void clearSelectedLog() {
        JobRecord job = list.getSelectionModel().getSelectedItem();
        if (job != null) {
            job.getLog().clear();
        }
    }

    private static final class JobListCell extends ListCell<JobRecord> {
        private final Label name = new Label();
        private final Label status = new Label();
        private final Label elapsed = new Label();
        private final VBox box = new VBox(2);
        private final Tooltip hoverTip = new Tooltip();
        private String boundRunId;
        private JobRecord boundJob;
        private final javafx.beans.value.ChangeListener<JobState> cellStateListener =
                (obs, o, n) -> applyStatusStyle(n);

        JobListCell() {
            name.getStyleClass().add("gsea-job-name");
            status.getStyleClass().add("gsea-job-status");
            elapsed.getStyleClass().add("gsea-muted");
            HBox meta = new HBox(8, status, elapsed);
            meta.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().addAll(name, meta);
            box.setPadding(new Insets(4, 6, 4, 6));
            hoverTip.setWrapText(true);
            hoverTip.setMaxWidth(420);
            hoverTip.setShowDelay(Duration.millis(400));
        }

        void refreshElapsed() {
            JobRecord item = boundJob;
            if (item == null || isEmpty()) {
                return;
            }
            JobState state = item.getState();
            if (state != null && state.isActive()) {
                elapsed.setText(item.formatElapsed());
            }
        }

        @Override
        protected void updateItem(JobRecord item, boolean empty) {
            super.updateItem(item, empty);
            name.textProperty().unbind();
            status.textProperty().unbind();
            if (boundJob != null) {
                boundJob.stateProperty().removeListener(cellStateListener);
                boundJob = null;
            }
            if (empty || item == null) {
                boundRunId = null;
                setText(null);
                setGraphic(null);
                setTooltip(null);
                return;
            }
            boolean sameJob = item.getRunId().equals(boundRunId);
            boundRunId = item.getRunId();
            boundJob = item;
            item.stateProperty().addListener(cellStateListener);
            name.textProperty().bind(item.nameProperty());
            status.textProperty().bind(item.statusLabelProperty());
            elapsed.setText(item.formatElapsed());
            applyStatusStyle(item.getState());
            if (!sameJob) {
                String hover = item.getHoverText();
                if (hover != null) {
                    hoverTip.setText(hover);
                    setTooltip(hoverTip);
                } else {
                    setTooltip(null);
                }
            }
            setGraphic(box);
            setText(null);
        }

        private void applyStatusStyle(JobState state) {
            clearStatusStyle();
            if (state != null) {
                status.getStyleClass().add(styleClassFor(state));
            }
        }

        private void clearStatusStyle() {
            status.getStyleClass().removeAll(
                    "gsea-job-status-waiting",
                    "gsea-job-status-running",
                    "gsea-job-status-canceling",
                    "gsea-job-status-canceled",
                    "gsea-job-status-success",
                    "gsea-job-status-success-warn",
                    "gsea-job-status-error",
                    "gsea-job-status-invalid-param");
        }

        private static String styleClassFor(JobState state) {
            switch (state) {
                case RUNNING:
                    return "gsea-job-status-running";
                case CANCELING:
                    return "gsea-job-status-canceling";
                case CANCELED:
                    return "gsea-job-status-canceled";
                case SUCCESS:
                    return "gsea-job-status-success";
                case SUCCESS_WARN:
                    return "gsea-job-status-success-warn";
                case ERROR:
                    return "gsea-job-status-error";
                case INVALID_PARAM:
                    return "gsea-job-status-invalid-param";
                case WAITING:
                default:
                    return "gsea-job-status-waiting";
            }
        }
    }
}
