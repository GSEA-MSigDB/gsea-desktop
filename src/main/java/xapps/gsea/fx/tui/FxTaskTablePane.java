/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.tui;

import java.io.File;
import java.util.Properties;
import java.util.function.Consumer;

import org.gsea_msigdb.gsea.ui.api.ViewPage;

import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.tui.TaskManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import xtools.api.Tool;

/**
 * Process / task table for the JavaFX shell.
 */
public class FxTaskTablePane {

    private static final String STATUS_RUNNING = "gsea-task-status-running";
    private static final String STATUS_SUCCESS = "gsea-task-status-success";
    private static final String STATUS_SUCCESS_WARN = "gsea-task-status-success-warn";
    private static final String STATUS_ERROR = "gsea-task-status-error";
    private static final String STATUS_INVALID_PARAM = "gsea-task-status-invalid-param";
    private static final String STATUS_CANCELED = "gsea-task-status-canceled";
    private static final String STATUS_WAITING = "gsea-task-status-waiting";
    private static final String STATUS_PAUSED = "gsea-task-status-paused";
    private static final String STATUS_KILLED = "gsea-task-status-killed";

    /** Coarse execution-state classification used for status coloring and click behavior. */
    private enum StateKind {
        RUNNING, SUCCESS, SUCCESS_WARN, ERROR, INVALID_PARAM, CANCELED, WAITING, PAUSED, KILLED
    }

    public static final class TaskRow {
        private final String runId;
        private final SimpleStringProperty name = new SimpleStringProperty();
        private final SimpleStringProperty status = new SimpleStringProperty();
        private final SimpleStringProperty report = new SimpleStringProperty();
        private File reportDir;
        private Tool tool;
        private Properties paramSnapshot;

        public TaskRow(String runId, String name, String status, File reportDir) {
            this(runId, name, status, reportDir, null, null);
        }

        public TaskRow(String runId, String name, String status, File reportDir, Tool tool, Properties paramSnapshot) {
            this.runId = runId;
            this.name.set(name);
            this.status.set(status);
            setReportDir(reportDir);
            this.tool = tool;
            this.paramSnapshot = paramSnapshot;
        }

        public String getRunId() {
            return runId;
        }

        void setToolSnapshot(Tool tool, Properties paramSnapshot) {
            if (tool != null) {
                this.tool = tool;
            }
            if (paramSnapshot != null) {
                this.paramSnapshot = paramSnapshot;
            }
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public SimpleStringProperty statusProperty() {
            return status;
        }

        public SimpleStringProperty reportProperty() {
            return report;
        }

        public File getReportDir() {
            return reportDir;
        }

        void setReportDir(File reportDir) {
            this.reportDir = reportDir;
            this.report.set(reportDir != null ? reportDir.getName() : "");
        }

        public Tool getTool() {
            return tool;
        }

        public Properties getParamSnapshot() {
            return paramSnapshot;
        }

        StateKind getStateKind() {
            String s = status.get();
            if (s == null) {
                return StateKind.WAITING;
            }
            if (s.startsWith("Running")) {
                return StateKind.RUNNING;
            }
            if (s.startsWith("Paused")) {
                return StateKind.PAUSED;
            }
            if (s.startsWith("Killed")) {
                return StateKind.KILLED;
            }
            if (s.startsWith("Success (with warnings)")) {
                return StateKind.SUCCESS_WARN;
            }
            if (s.startsWith("Success") || s.equals("0 Result Objects")) {
                return StateKind.SUCCESS;
            }
            if (s.startsWith("Canceled")) {
                return StateKind.CANCELED;
            }
            if (s.startsWith("Waiting")) {
                return StateKind.WAITING;
            }
            if (s.startsWith("Invalid Param")) {
                return StateKind.INVALID_PARAM;
            }
            if (s.startsWith("Error")) {
                return StateKind.ERROR;
            }
            return StateKind.WAITING;
        }
    }

    private static FxTaskTablePane instance;

    private final ObservableList<TaskRow> rows = FXCollections.observableArrayList();
    private final BorderPane root = new BorderPane();
    private final TableView<TaskRow> table = new TableView<>(rows);
    private final Consumer<ViewPage> openPage;

    public FxTaskTablePane() {
        this(null);
    }

    public FxTaskTablePane(Consumer<ViewPage> openPage) {
        this.openPage = openPage != null ? openPage : page -> { };
        instance = this;
        TaskManager.getInstance().addStatusListener((runId, name, status, reportDir) ->
                javafx.application.Platform.runLater(() -> updateStatus(runId, name, status, reportDir)));
        Label header = new Label("Processes: click status for results");
        header.getStyleClass().add("gsea-section-header");

        HBox top = new HBox(12, header);
        top.setPadding(new Insets(4, 8, 4, 8));

        Button showFolder = new Button("Show results folder");
        showFolder.setTooltip(new javafx.scene.control.Tooltip(
                "Show output directory of this application"));
        xapps.gsea.fx.FxButtons.styleSecondary(showFolder);
        xapps.gsea.fx.FxButtons.sizeToContent(showFolder);
        showFolder.setOnAction(e -> openResultsFolder());

        Button cancel = new Button("Cancel");
        cancel.setTooltip(new javafx.scene.control.Tooltip(
                "Cancel the selected running job (or the most recent running job)"));
        xapps.gsea.fx.FxButtons.styleSecondary(cancel);
        xapps.gsea.fx.FxButtons.sizeToContent(cancel);
        cancel.setOnAction(e -> cancelRunningJob());

        HBox south = xapps.gsea.fx.FxButtons.row(showFolder, cancel);
        south.setPadding(new Insets(4, 8, 6, 8));

        TableColumn<TaskRow, String> numCol = new TableColumn<>(" ");
        numCol.setCellValueFactory(c -> new SimpleStringProperty(
                String.valueOf(rows.indexOf(c.getValue()) + 1)));
        numCol.setPrefWidth(28);
        numCol.setMaxWidth(36);
        numCol.setMinWidth(24);
        numCol.setSortable(false);

        TableColumn<TaskRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> c.getValue().nameProperty());
        nameCol.setPrefWidth(180);
        nameCol.setCellFactory(col -> createNameCell());

        TableColumn<TaskRow, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> c.getValue().statusProperty());
        statusCol.setPrefWidth(220);
        statusCol.setCellFactory(col -> createStatusCell());

        table.getColumns().add(numCol);
        table.getColumns().add(nameCol);
        table.getColumns().add(statusCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label(""));
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.SINGLE);
        table.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) -> {
            if (n != null && n.intValue() >= 0) {
                Platform.runLater(() -> table.getSelectionModel().clearSelection());
            }
        });
        table.getStyleClass().add("gsea-task-chrome");
        Label title = new Label("GSEA reports");
        title.setStyle("-fx-font-weight: bold; -fx-padding: 2 6 0 6;");
        javafx.scene.layout.VBox north = new javafx.scene.layout.VBox(title, top);
        north.getStyleClass().add("gsea-task-chrome");
        south.getStyleClass().add("gsea-task-chrome");
        root.setTop(north);
        root.setCenter(table);
        root.setBottom(south);
        root.setPrefHeight(350);
        root.setMinHeight(280);
        root.getStyleClass().addAll("gsea-task-table", "gsea-task-chrome");
        Runnable hand = () -> root.setCursor(javafx.scene.Cursor.HAND);
        Runnable def = () -> root.setCursor(javafx.scene.Cursor.DEFAULT);
        root.setOnMouseEntered(e -> hand.run());
        root.setOnMouseExited(e -> def.run());
        table.setOnMouseEntered(e -> table.setCursor(javafx.scene.Cursor.HAND));
        table.setOnMouseExited(e -> table.setCursor(javafx.scene.Cursor.DEFAULT));
    }

    private TableCell<TaskRow, String> createNameCell() {
        TableCell<TaskRow, String> cell = new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(item);
                // Let CSS supply the default text color (dark/light); status column uses semantic colors.
                setTextFill(null);
                setGraphic(xapps.gsea.fx.FxFileIcons.forResource("dirty_ov.gif"));
                setTooltip(new javafx.scene.control.Tooltip(
                        "Set Parameters and Launch Analysis Tools"));
            }
        };
        cell.setOnMouseClicked(e -> {
            if (!cell.isEmpty() && cell.getTableRow() != null) {
                relaunch(cell.getTableRow().getItem());
                e.consume();
            }
        });
        return cell;
    }

    private TableCell<TaskRow, String> createStatusCell() {
        TableCell<TaskRow, String> cell = new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                clearStatusStyleClass(this);
                if (empty || item == null || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                TaskRow row = getTableRow().getItem();
                setText(item);
                getStyleClass().add(styleClassFor(row.getStateKind()));
                if (row.getStateKind() == StateKind.SUCCESS && "Success".equals(item)) {
                    setGraphic(xapps.gsea.fx.FxFileIcons.forResource("Ellipsis.png"));
                } else {
                    setGraphic(null);
                }
            }
        };
        cell.setOnMouseClicked(e -> {
            if (!cell.isEmpty() && cell.getTableRow() != null && cell.getTableRow().getItem() != null) {
                handleStatusClick(cell.getTableRow().getItem());
                e.consume();
            }
        });
        return cell;
    }

    private static String styleClassFor(StateKind kind) {
        switch (kind) {
            case RUNNING:
                return STATUS_RUNNING;
            case PAUSED:
                return STATUS_PAUSED;
            case SUCCESS:
                return STATUS_SUCCESS;
            case SUCCESS_WARN:
                return STATUS_SUCCESS_WARN;
            case KILLED:
                return STATUS_KILLED;
            case ERROR:
                return STATUS_ERROR;
            case INVALID_PARAM:
                return STATUS_INVALID_PARAM;
            case CANCELED:
                return STATUS_CANCELED;
            case WAITING:
            default:
                return STATUS_WAITING;
        }
    }

    private static void clearStatusStyleClass(TableCell<TaskRow, String> cell) {
        cell.getStyleClass().removeAll(
                STATUS_RUNNING,
                STATUS_SUCCESS,
                STATUS_SUCCESS_WARN,
                STATUS_ERROR,
                STATUS_INVALID_PARAM,
                STATUS_CANCELED,
                STATUS_WAITING,
                STATUS_PAUSED,
                STATUS_KILLED);
    }

    public static FxTaskTablePane getInstance() {
        return instance;
    }

    public Node getNode() {
        return root;
    }

    public void attachSnapshot(String runId, Tool tool, Properties paramSnapshot) {
        if (runId == null) {
            return;
        }
        for (TaskRow row : rows) {
            if (runId.equals(row.getRunId())) {
                row.setToolSnapshot(tool, paramSnapshot);
                return;
            }
        }
        String name = tool != null ? tool.getName() : "Tool";
        rows.add(new TaskRow(runId, name, "Waiting", null, tool, paramSnapshot));
    }

    public void updateStatus(String runId, String name, String status, File reportDir) {
        if (runId != null) {
            for (TaskRow row : rows) {
                if (runId.equals(row.getRunId())) {
                    row.statusProperty().set(status);
                    if (name != null) {
                        row.nameProperty().set(name);
                    }
                    if (reportDir != null) {
                        row.setReportDir(reportDir);
                    }
                    return;
                }
            }
            rows.add(new TaskRow(runId, name != null ? name : "Tool", status, reportDir));
            return;
        }
        // Fallback without runId (should not happen with current TaskManager)
        rows.add(new TaskRow(java.util.UUID.randomUUID().toString(),
                name != null ? name : "Tool", status, reportDir));
    }

    /** "Show results folder". */
    private static void openResultsFolder() {
        File dir = Application.getVdbManager().getDefaultOutputDir();
        if (dir == null) {
            return;
        }
        try {
            xapps.gsea.fx.FxDesktopUtil.openInOsExplorer(dir);
        } catch (Exception e) {
            Application.getWindowManager().showError(
                    "Trouble launching File Explorer on path '" + dir.getPath() + "'", e);
        }
    }

    /** Cancel the most recent running/paused/waiting job. */
    private void cancelRunningJob() {
        TaskRow target = null;
        for (int i = rows.size() - 1; i >= 0; i--) {
            TaskRow row = rows.get(i);
            StateKind kind = row.getStateKind();
            if (kind == StateKind.RUNNING || kind == StateKind.PAUSED || kind == StateKind.WAITING) {
                target = row;
                break;
            }
        }
        if (target == null) {
            Application.getWindowManager().showMessage("No running job to cancel");
            return;
        }
        if (Application.getWindowManager().showConfirm(
                "Cancel job",
                "Cancel running job \"" + target.nameProperty().get() + "\"?")) {
            TaskManager.getInstance().cancel(target.getRunId());
        }
    }

    /** Name-column click: reopen the tool pre-filled with this run's saved parameters. */
    private void relaunch(TaskRow row) {
        if (row == null) {
            return;
        }
        Tool tool = row.getTool();
        Properties paramSnapshot = row.getParamSnapshot();
        if (tool == null || paramSnapshot == null) {
            Application.getWindowManager().showMessage("No saved parameters",
                    "No saved parameters are available to reopen " + row.nameProperty().get() + ".");
            return;
        }
        FxToolRelaunch.showInToolRunner(tool, paramSnapshot, openPage);
    }

    /** Status-column click: cancel if running; otherwise open report / show error. */
    private void handleStatusClick(TaskRow row) {
        String name = row.nameProperty().get();
        switch (row.getStateKind()) {
            case RUNNING:
            case PAUSED: {
                if (Application.getWindowManager().showConfirm(
                        "Cancel job",
                        "Cancel running job \"" + name + "\"?")) {
                    TaskManager.getInstance().cancel(row.getRunId());
                }
                break;
            }
            case WAITING: {
                Tool tool = row.getTool();
                String waitFor = tool != null ? tool.getClass().getName() : name;
                Application.getWindowManager().showMessage("Waiting for: " + waitFor);
                break;
            }
            case CANCELED:
                Application.getWindowManager().showMessage("This job was canceled by the user");
                break;
            case KILLED:
                Application.getWindowManager().showMessage("This job was force-stopped");
                break;
            case ERROR: {
                Throwable t = TaskManager.getInstance().getLastError(row.getRunId());
                Application.getWindowManager().showError("Tool execution error", t);
                break;
            }
            case INVALID_PARAM: {
                Throwable t = TaskManager.getInstance().getLastError(row.getRunId());
                Application.getWindowManager().showError(
                        "One or more parameter(s) were not specified", t);
                break;
            }
            case SUCCESS_WARN:
            case SUCCESS:
                xapps.gsea.fx.FxReportOpen.openFromRun(row.getRunId(), row.getReportDir(), openPage);
                break;
            default:
                break;
        }
    }
}
