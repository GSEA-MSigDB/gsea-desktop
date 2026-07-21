/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.gsea_msigdb.gsea.ui.api.ViewPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.core.api.FileManager;
import edu.mit.broad.xbench.core.api.XStore;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import xapps.gsea.GseaWebResources;
import xtools.api.param.ParamSet;

/**
 * Load datasets / phenotypes / gene sets into the application cache, with an object-cache browser.
 */
public class FxLoadDataPane implements ViewPage {

    private static final Logger klog = LoggerFactory.getLogger(FxLoadDataPane.class);

    private final BorderPane root = new BorderPane();
    private final TextArea statusArea = new TextArea();
    private final ProgressBar loadProgress = new ProgressBar(0);
    private final Label loadProgressLabel = new Label("");
    private final Button cancelLoad = new Button("Cancel");
    private final ObservableList<String> recentItems = FXCollections.observableArrayList();
    private final ListView<String> recentList = new ListView<>(recentItems);
    private final List<File> stagedFiles = new ArrayList<>();
    private final ListView<String> stagedList = new ListView<>();
    private final FxObjectCacheTree cacheTree;
    private final Consumer<ViewPage> openPage;
    private volatile boolean loadCancelled;
    private volatile boolean loadInProgress;

    public FxLoadDataPane(Consumer<ViewPage> openPage) {
        this.openPage = openPage != null ? openPage : page -> { };
        this.cacheTree = new FxObjectCacheTree(this.openPage);

        statusArea.setEditable(false);
        statusArea.setWrapText(true);
        statusArea.setPrefRowCount(8);

        loadProgress.setMaxWidth(Double.MAX_VALUE);
        loadProgress.setPrefWidth(280);
        cancelLoad.setDisable(true);
        cancelLoad.setOnAction(e -> {
            loadCancelled = true;
            loadProgressLabel.setText("Cancelling…");
            appendStatus("Cancel requested — aborting current load…");
        });
        HBox progressRow = new HBox(10, loadProgress, loadProgressLabel, cancelLoad);
        HBox.setHgrow(loadProgress, Priority.ALWAYS);
        progressRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        xapps.gsea.fx.FxButtons.styleSecondary(cancelLoad);

        Button browse = new Button("Browse for files ...");
        browse.setGraphic(xapps.gsea.fx.FxFileIcons.forResource("Open16.gif"));
        xapps.gsea.fx.FxButtons.stylePrimary(browse);
        xapps.gsea.fx.FxButtons.sizeToContent(browse);
        browse.setMaxWidth(Double.MAX_VALUE);
        browse.setOnAction(e -> browseAndLoad());
        javafx.scene.control.TitledPane method1 = new javafx.scene.control.TitledPane("Method 1:", browse);
        method1.setCollapsible(false);
        method1.setMaxWidth(Double.MAX_VALUE);

        Button loadLast = new Button("Load last dataset used");
        loadLast.setGraphic(xapps.gsea.fx.FxFileIcons.forResource("History16_v2.gif"));
        xapps.gsea.fx.FxButtons.styleSecondary(loadLast);
        xapps.gsea.fx.FxButtons.sizeToContent(loadLast);
        loadLast.setMaxWidth(Double.MAX_VALUE);
        loadLast.setOnAction(e -> loadLastAnalysisFiles());
        javafx.scene.control.TitledPane method2 = new javafx.scene.control.TitledPane("Method 2:", loadLast);
        method2.setCollapsible(false);
        method2.setMaxWidth(Double.MAX_VALUE);

        Button formatHelp = new Button("File Format Help ...");
        formatHelp.setGraphic(xapps.gsea.fx.FxFileIcons.forResource("Help16_v2.gif"));
        xapps.gsea.fx.FxButtons.styleSecondary(formatHelp);
        xapps.gsea.fx.FxButtons.sizeToContent(formatHelp);
        formatHelp.setOnAction(e -> openUrl(GseaWebResources.getGseaDataFormatsHelpURL()));
        VBox formatsList = supportedFormatsBox();
        VBox formatsContent = new VBox(10, formatsList, formatHelp);
        formatsContent.setPadding(new Insets(6));
        VBox formatsBox = new VBox(formatsContent);
        formatsBox.setPadding(new Insets(4));
        formatsBox.getStyleClass().add("gsea-panel-border");
        Label formatsTitle = new Label("Supported file formats");
        formatsTitle.getStyleClass().add("gsea-section-header");
        formatsBox.getChildren().add(0, formatsTitle);
        formatsBox.setMinWidth(220);
        formatsBox.setPrefWidth(260);

        Label dropHint = new Label("Method 3: drag and drop files here");
        dropHint.setStyle("-fx-font-weight: bold;");
        stagedList.setPrefHeight(80);
        stagedList.setPlaceholder(new Label(""));
        stagedList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                if (empty || path == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                File f = new File(path);
                setText(f.getName());
                setTooltip(new javafx.scene.control.Tooltip(path));
            }
        });

        Button clearStaged = new Button("Clear");
        xapps.gsea.fx.FxButtons.styleSecondary(clearStaged);
        clearStaged.setOnAction(e -> {
            stagedFiles.clear();
            stagedList.getItems().clear();
            appendStatus("Cleared staged drop list.");
        });
        Button loadStaged = new Button("Load these files!");
        loadStaged.setGraphic(xapps.gsea.fx.FxFileIcons.forResource("Dnd2.gif"));
        xapps.gsea.fx.FxButtons.stylePrimary(loadStaged);
        loadStaged.setOnAction(e -> {
            if (stagedFiles.isEmpty()) {
                Application.getWindowManager().showMessage(
                        "No files to import!\nDrag and drop files into the box and try again");
                return;
            }
            loadFiles(new ArrayList<>(stagedFiles), false);
        });
        HBox stagedActions = xapps.gsea.fx.FxButtons.row(clearStaged, loadStaged);
        javafx.scene.layout.VBox method3 = new javafx.scene.layout.VBox(8, dropHint, stagedList, stagedActions);
        method3.setPadding(new Insets(8));
        method3.getStyleClass().add("gsea-panel-border");

        VBox method12 = new VBox(8, method1, method2);
        HBox methods = new HBox(12, method12, method3, formatsBox);
        HBox.setHgrow(method3, Priority.ALWAYS);
        VBox loadPane = new VBox(12, methods, progressRow, statusArea);
        loadPane.setPadding(new Insets(12));
        VBox.setVgrow(statusArea, Priority.ALWAYS);

        Label recentHeader = new Label("Recently used files\n(double click to load, right click for more options)");
        recentHeader.getStyleClass().add("gsea-section-header");
        recentList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        recentList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                if (empty || path == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    setTooltip(null);
                    return;
                }
                File file = new File(path);
                setText(shortRecentPath(file));
                setGraphic(xapps.gsea.fx.FxFileIcons.forFile(file));
                setTooltip(new javafx.scene.control.Tooltip(path));
                setStyle(file.exists() ? "" : "-fx-text-fill: red;");
            }
        });
        recentList.setContextMenu(null);
        recentList.setOnContextMenuRequested(e -> {
            List<String> selected = new ArrayList<>(recentList.getSelectionModel().getSelectedItems());
            if (selected.isEmpty()) {
                return;
            }
            ContextMenu menu = selected.size() > 1
                    ? multiRecentFileMenu(selected)
                    : singleRecentFileMenu(selected.get(0));
            recentList.setContextMenu(menu);
            menu.show(recentList, e.getScreenX(), e.getScreenY());
        });
        recentList.setOnKeyPressed(e -> {
            if (e.isShortcutDown() && e.getCode() == javafx.scene.input.KeyCode.C) {
                copySelectedRecentFiles();
                e.consume();
            }
        });
        recentList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String path = recentList.getSelectionModel().getSelectedItem();
                if (path != null) {
                    File file = new File(path);
                    FxFileActions.runDefaultFileAction(file, openPage);
                    try {
                        Application.getFileManager().getRecentFilesStore().refresh(path);
                        refreshRecentFiles();
                    } catch (Exception ignored) {
                    }
                }
            }
        });
        recentList.setOnDragDetected(e -> {
            List<File> files = new ArrayList<>();
            for (String path : recentList.getSelectionModel().getSelectedItems()) {
                if (path == null) {
                    continue;
                }
                files.add(new File(path));
            }
            if (!files.isEmpty()) {
                xapps.gsea.fx.FxFileTransferSupport.startFileDrag(recentList, files, e);
            }
        });
        VBox recentPane = new VBox(8, recentHeader,
                xapps.gsea.fx.FxSearchField.wrapList(recentList, path -> path != null ? path : ""));
        recentPane.setPadding(new Insets(12));
        VBox.setVgrow(recentList, Priority.ALWAYS);

        SplitPane bottom = new SplitPane(recentPane, cacheTree.getNode());
        bottom.setDividerPositions(0.45);

        SplitPane main = new SplitPane(loadPane, bottom);
        main.setOrientation(Orientation.VERTICAL);
        main.setDividerPositions(0.42);
        root.setCenter(main);

        enableFileDrop(stagedList);

        refreshRecentFiles();
    }

    private void enableFileDrop(Node target) {
        target.setOnDragOver(e -> {
            if (e.getGestureSource() != target
                    && (e.getDragboard().hasFiles()
                    || e.getDragboard().hasString()
                    || xapps.gsea.fx.FxPobTransferSupport.hasPobList(e.getDragboard()))) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });
        target.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;
            List<File> incoming = new ArrayList<>();
            if (xapps.gsea.fx.FxPobTransferSupport.hasPobList(db)) {
                for (edu.mit.broad.genome.objects.PersistentObject pob :
                        xapps.gsea.fx.FxPobTransferSupport.takeDragPobs(e)) {
                    try {
                        File src = ParserFactory.getCache().getSourceFile(pob);
                        if (src != null) {
                            incoming.add(src);
                        }
                    } catch (Throwable ignored) {
                        // skip objects without a source file
                    }
                }
            } else {
                // Prefer path string when it retains missing files stripped from FILES flavor.
                incoming.addAll(xapps.gsea.fx.FxFileTransferSupport.filesFromDragboard(db));
            }
            if (!incoming.isEmpty()) {
                for (File f : incoming) {
                    stagedFiles.add(f);
                    stagedList.getItems().add(f.getAbsolutePath());
                }
                appendStatus("Staged " + incoming.size() + " file(s). Click “Load these files!” to parse.");
                success = true;
            }
            e.setDropCompleted(success);
            e.consume();
        });
    }

    private void forceReloadSelected() {
        List<String> selected = recentList.getSelectionModel().getSelectedItems();
        if (selected == null || selected.isEmpty()) {
            Application.getWindowManager().showMessage("Select a recent file to force-reload.");
            return;
        }
        List<File> files = new ArrayList<>();
        for (String path : selected) {
            files.add(new File(path));
        }
        loadFiles(files, true);
    }

    /** Import N / Purge N / Copy / Purge all. */
    private ContextMenu multiRecentFileMenu(List<String> paths) {
        ContextMenu menu = new ContextMenu();
        List<File> files = new ArrayList<>();
        for (String p : paths) {
            files.add(new File(p));
        }
        MenuItem importSel = new MenuItem("Import data from " + files.size() + " selected files");
        importSel.setOnAction(e -> {
            for (File f : files) {
                try {
                    Application.getFileManager().getRecentFilesStore().refresh(f.getPath());
                } catch (Throwable ignored) {
                }
            }
            refreshRecentFiles();
            loadFiles(new ArrayList<>(files), false);
        });
        MenuItem purgeSel = new MenuItem("Purge " + files.size() + " Selected Files");
        purgeSel.setOnAction(e -> purgeSelectedRecent(files));
        MenuItem copyFiles = new MenuItem("Copy File(s)");
        copyFiles.setGraphic(xapps.gsea.fx.FxFileIcons.forResource("Copy16.gif"));
        copyFiles.setOnAction(e -> copyRecentFiles(files));
        MenuItem purgeAll = new MenuItem("Purge All Files");
        purgeAll.setOnAction(e -> purgeAllRecent());
        menu.getItems().addAll(importSel, new SeparatorMenuItem(), copyFiles,
                new SeparatorMenuItem(), purgeSel, purgeAll);
        return menu;
    }

    private ContextMenu singleRecentFileMenu(String path) {
        File file = new File(path);
        ContextMenu menu = FxFileActions.fileContextMenu(file, openPage, this::refreshRecentFiles);
        menu.getItems().add(new SeparatorMenuItem());
        MenuItem copyFiles = new MenuItem("Copy File(s)");
        copyFiles.setGraphic(xapps.gsea.fx.FxFileIcons.forResource("Copy16.gif"));
        copyFiles.setOnAction(e -> copyRecentFiles(java.util.Collections.singletonList(file)));
        MenuItem purgeSel = new MenuItem("Purge Selected File");
        purgeSel.setOnAction(e -> purgeSelectedRecent(java.util.Collections.singletonList(file)));
        MenuItem purgeAll = new MenuItem("Purge All Files");
        purgeAll.setOnAction(e -> purgeAllRecent());
        menu.getItems().addAll(copyFiles, new SeparatorMenuItem(), purgeSel, purgeAll);
        return menu;
    }

    private void purgeSelectedRecent(List<File> files) {
        boolean proceed = Application.getWindowManager().showConfirm(
                "Delete file history for selected file(s)?",
                "<html><body><b>This will remove these files from this list "
                        + "(but NOT delete the files themselves)</b></body></html>");
        if (!proceed) {
            return;
        }
        try {
            List<String> toRemove = new ArrayList<>();
            for (File f : files) {
                toRemove.add(f.getAbsolutePath());
            }
            Application.getFileManager().getRecentFilesStore().removeAndSave(toRemove);
            refreshRecentFiles();
        } catch (Throwable t) {
            Application.getWindowManager().showError("Could not purge selected files", t);
        }
    }

    private void purgeAllRecent() {
        boolean proceed = Application.getWindowManager().showConfirm(
                "Delete file history?",
                "<html><body><b>This will remove recently used files from this list "
                        + "(but NOT delete the files themselves)</b></body></html>");
        if (!proceed) {
            return;
        }
        try {
            Application.getFileManager().getRecentFilesStore().clearAll();
            refreshRecentFiles();
        } catch (Throwable t) {
            Application.getWindowManager().showError("Could not clear recent files", t);
        }
    }

    private void copySelectedRecentFiles() {
        List<File> files = new ArrayList<>();
        for (String path : recentList.getSelectionModel().getSelectedItems()) {
            if (path == null) {
                continue;
            }
            files.add(new File(path));
        }
        copyRecentFiles(files);
    }

    private static void copyRecentFiles(List<File> files) {
        xapps.gsea.fx.FxFileTransferSupport.copyFilesToClipboard(
                files != null ? files : java.util.Collections.emptyList());
    }

    /** ../parent/file when possible. */
    private static String shortRecentPath(File file) {
        if (file == null) {
            return "";
        }
        File parent = file.getParentFile();
        if (parent != null) {
            return ".." + File.separator + parent.getName() + File.separator + file.getName();
        }
        return file.getName();
    }

    private void loadLastAnalysisFiles() {
        Thread worker = new Thread(() -> {
            try {
                edu.mit.broad.xbench.tui.ReportStub rs = Application.getToolManager()
                        .getLastReportStub(xtools.gsea.Gsea.class.getName());
                if (rs == null) {
                    appendStatus("No analysis history available — nothing loaded.");
                    Platform.runLater(() -> Application.getWindowManager()
                            .showMessage("Load last analysis", "No history available, nothing loaded!"));
                    return;
                }
                edu.mit.broad.genome.reports.api.Report report = rs.getReport(true);
                java.util.Properties params = report.getParametersUsed();
                xtools.api.Tool fillTool;
                Class<?> producer = report.getProducer();
                if (producer != null) {
                    fillTool = edu.mit.broad.xbench.tui.ToolFactory.createTool(producer.getName());
                } else {
                    fillTool = new xtools.gsea.Gsea();
                }
                ParamSet.FoundMissingFile fmf = fillTool.getParamSet().fileCheckingFill(params);
                final File[] missing = fmf.missingFiles;
                if (missing != null && missing.length != 0) {
                    if (!confirmMissingFiles(missing)) {
                        appendStatus("Load last analysis cancelled — missing files not accepted.");
                        return;
                    }
                }
                Application.getWindowManager().showMessage(
                        "Data from the last run of this tool is being automagically loaded. "
                                + "They will soon be available as parameter options");
                List<File> toLoad = new ArrayList<>();
                if (fmf.foundFiles != null) {
                    for (File f : fmf.foundFiles) {
                        if (f != null && f.isFile()) {
                            toLoad.add(f);
                        }
                    }
                }
                if (!toLoad.isEmpty()) {
                    appendStatus("Loading files from last analysis: " + rs.getName());
                    loadFiles(toLoad, false, false);
                } else {
                    appendStatus("Last analysis (" + rs.getName() + ") had no loadable files on disk.");
                }
            } catch (Throwable t) {
                klog.error("Load last analysis failed", t);
                appendStatus("ERROR: " + t.getMessage());
                Platform.runLater(() -> Application.getWindowManager().showError("Load last analysis failed", t));
            }
        }, "gsea-load-last");
        worker.setDaemon(true);
        worker.start();
    }

    /** @return true if the user wants to continue despite missing files */
    private static boolean confirmMissingFiles(File[] files) {
        StringBuilder buf = new StringBuilder(
                "The following file(s) were not found on the local file system:\n\n");
        for (File f : files) {
            buf.append(f).append('\n');
        }
        buf.append("\nContinue loading the files that were found?");
        return Application.getWindowManager().showConfirm("Some Files Missing", buf.toString());
    }

    private void browseAndLoad() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open");
        File lastDir = Application.getFileManager().getLastDirAccesessed();
        if (lastDir != null && lastDir.isDirectory()) {
            chooser.setInitialDirectory(lastDir);
        }
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("GSEA data files",
                        "*.gct", "*.res", "*.pcl", "*.txt", "*.cls",
                        "*.gmx", "*.gmt", "*.grp", "*.rnk", "*.chip", "*.xml"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        List<File> files = chooser.showOpenMultipleDialog(
                root.getScene() != null ? root.getScene().getWindow() : null);
        if (files != null && !files.isEmpty()) {
            for (File file : files) {
                if (!edu.mit.broad.genome.parsers.AuxUtils.isAuxFile(file)) {
                    try {
                        Application.getFileManager().registerRecentlyOpenedFile(file);
                    } catch (Throwable ignored) {
                        // keep loading even if history registration fails
                    }
                }
            }
            refreshRecentFiles();
            loadFiles(files, false);
        }
    }

    private void loadFiles(List<File> files, boolean forceReload) {
        loadFiles(files, forceReload, true);
    }

    private void loadFiles(List<File> files, boolean forceReload, boolean showCompletionDialog) {
        final List<File> unique = new ArrayList<>(new java.util.LinkedHashSet<>(files));
        loadCancelled = false;
        loadInProgress = true;
        int total = unique.size();
        Platform.runLater(() -> {
            cancelLoad.setDisable(false);
            loadProgress.setProgress(0);
            loadProgressLabel.setText("0 / " + total);
        });
        appendStatus("Loading " + unique.size() + " file(s)"
                + (forceReload ? " (force reload)…" : "…"));
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
            FileManager fm = Application.getFileManager();
            int ok = 0;
            int fail = 0;
            int warned = 0;
            int done = 0;
            StringBuilder errors = new StringBuilder();
            StringBuilder loadedNames = new StringBuilder();
            StringBuilder warningNotes = new StringBuilder();
            try {
            for (File file : unique) {
                final int idx = done + 1;
                Platform.runLater(() -> {
                    loadProgress.setProgress(total == 0 ? 1.0 : (idx - 1) / (double) total);
                    loadProgressLabel.setText("Loading " + idx + " / " + total + ": " + file.getName());
                });
                if (loadCancelled) {
                    fail++;
                    errors.append(file.getName()).append(": cancelled\n");
                    appendStatus("Load cancelled at: " + file.getName());
                    loadCancelled = false;
                    done++;
                    continue;
                }
                if (file.isDirectory()) {
                    fail++;
                    errors.append(file.getName()).append(": directories are not supported\n");
                    appendStatus("ERROR: directories are not supported: " + file.getAbsolutePath());
                    done++;
                    continue;
                }
                try {
                    appendStatus("Reading: " + file.getAbsolutePath());
                    Object obj;
                    long fileLen = Math.max(1L, file.length());
                    final int fileIndex = done;
                    // forceReload → useCache=false like ParserFactory.read(file, false).
                    try (java.io.InputStream in = new CancellableFileInputStream(file, bytesRead -> {
                        double frac = (fileIndex + Math.min(1.0, bytesRead / (double) fileLen)) / total;
                        Platform.runLater(() -> {
                            loadProgress.setProgress(frac);
                            loadProgressLabel.setText("Loading " + (fileIndex + 1) + " / " + total
                                    + ": " + file.getName()
                                    + " (" + Math.min(100, (int) (100.0 * bytesRead / fileLen)) + "%)");
                        });
                    })) {
                        obj = ParserFactory.read(file.getPath(), in, !forceReload);
                    }
                    if (loadCancelled || obj == null) {
                        fail++;
                        errors.append(file.getName()).append(": cancelled\n");
                        appendStatus("Load cancelled during: " + file.getName());
                        loadCancelled = false;
                        done++;
                        continue;
                    }
                    if (forceReload) {
                        fm.getRecentFilesStore().refresh(file.getPath());
                    } else if (showCompletionDialog) {
                        fm.registerRecentlyOpenedFile(file);
                    }
                    String type = obj != null ? obj.getClass().getSimpleName() : "unknown";
                    appendStatus("Loaded " + type + ": " + file.getName());
                    loadedNames.append(" • ").append(file.getName()).append(" (").append(type).append(")\n");
                    if (obj instanceof edu.mit.broad.genome.objects.PersistentObject) {
                        List<String> warns = ((edu.mit.broad.genome.objects.PersistentObject) obj).getWarnings();
                        if (warns != null && !warns.isEmpty()) {
                            warned++;
                            for (String w : warns) {
                                warningNotes.append(" • ").append(file.getName()).append(": ").append(w).append('\n');
                                appendStatus("WARNING: " + w);
                            }
                        }
                    }
                    ok++;
                } catch (Throwable t) {
                    if (loadCancelled) {
                        fail++;
                        errors.append(file.getName()).append(": cancelled\n");
                        appendStatus("Load cancelled during: " + file.getName());
                        loadCancelled = false;
                        done++;
                        continue;
                    }
                    klog.error("Failed to load {}", file, t);
                    appendStatus("ERROR loading " + file.getName() + ": " + t.getMessage());
                    errors.append(file.getName()).append(": ").append(t.getMessage()).append('\n');
                    fail++;
                }
                done++;
                final int doneFinal = done;
                Platform.runLater(() -> {
                    loadProgress.setProgress(total == 0 ? 1.0 : doneFinal / (double) total);
                    loadProgressLabel.setText(doneFinal + " / " + total);
                });
            }
            } finally {
                loadInProgress = false;
            }
            final int okFinal = ok;
            final int failFinal = fail;
            final int warnedFinal = warned;
            final int totalFinal = total;
            final boolean cancelledFinal = loadCancelled;
            final String errText = errors.toString();
            final String loadedText = loadedNames.toString();
            final String warnText = warningNotes.toString();
            final boolean showDialog = showCompletionDialog;
            Platform.runLater(() -> {
                cancelLoad.setDisable(true);
                refreshRecentFiles();
                cacheTree.refresh();
                appendStatus((cancelledFinal ? "Load cancelled. " : "Load complete. ")
                        + "Success: " + okFinal + " / " + totalFinal
                        + ", failed: " + failFinal
                        + (warnedFinal > 0 ? ", with warnings: " + warnedFinal : ""));
                loadProgressLabel.setText(okFinal + " / " + totalFinal + " loaded"
                        + (failFinal > 0 ? " (" + failFinal + " failed)" : ""));
                if (!showDialog && !cancelledFinal && failFinal == 0) {
                    return;
                }
                if (cancelledFinal) {
                    Application.getWindowManager().showError(
                            "Load cancelled after " + okFinal + " / " + totalFinal + " files."
                                    + (okFinal > 0 ? "\n\nLoaded before cancel:\n" + loadedText : ""));
                } else if (failFinal > 0) {
                    Application.getWindowManager().showError(
                            "Some files failed to load (" + failFinal + " of " + totalFinal + "):\n" + errText
                                    + (okFinal > 0 ? "\nLoaded OK:\n" + loadedText : ""));
                } else {
                    StringBuilder buf = new StringBuilder("<html>Loading ... ")
                            .append(totalFinal).append(" files<br><br>");
                    buf.append(loadedText.replace(" • ", "").replace("\n", "<br>"));
                    buf.append("<br>Files loaded successfully: ").append(okFinal).append(" / ")
                            .append(totalFinal).append("<br>");
                    buf.append("There were NO errors");
                    if (warnedFinal > 0) {
                        buf.append("<br><br><b>There were warnings. See the [+] console log for details.</b>");
                    }
                    buf.append("</html>");
                    Application.getWindowManager().showMessage(buf.toString());
                }
            });
            return null;
            }
        };
        xapps.gsea.fx.FxWorkers.start(task, "gsea-load-data");
    }

    public void refreshRecentFiles() {
        recentItems.clear();
        try {
            XStore store = Application.getFileManager().getRecentFilesStore();
            for (int i = 0; i < store.getSize(); i++) {
                recentItems.add(store.getElementAt(i));
            }
        } catch (Throwable t) {
            klog.warn("Could not refresh recent files", t);
        }
    }

    private void appendStatus(String line) {
        Platform.runLater(() -> {
            if (statusArea.getText() == null || statusArea.getText().isEmpty()) {
                statusArea.setText(line);
            } else {
                statusArea.appendText("\n" + line);
            }
        });
    }

    private static void openUrl(String url) {
        try {
            xapps.gsea.fx.FxDesktopUtil.openUrl(url);
        } catch (Exception e) {
            Application.getWindowManager().showError("Could not open URL", e);
        }
    }

    /**
     * categories + maroon italic extensions. Built as Label/HBox rows (TextFlow preferred-size is unreliable in an HBox column).
     */
    private static VBox supportedFormatsBox() {
        final String indent = "               ";
        VBox box = new VBox(3);
        box.getChildren().addAll(
                formatRow(plainLabel("Dataset: "), extLabel("res"), plainLabel(" or "),
                        extLabel("gct"), plainLabel(" (Broad/MIT),")),
                formatRow(plainLabel(indent), extLabel("pcl"), plainLabel(" (Stanford)")),
                formatRow(plainLabel(indent), extLabel("txt"), plainLabel(" (tab-delim text)")),
                formatRow(plainLabel("Phenotype labels: "), extLabel("cls")),
                formatRow(plainLabel("Gene sets: "), extLabel("gmx"), plainLabel(" or "),
                        extLabel("gmt"), plainLabel(" or "), extLabel("grp")),
                formatRow(plainLabel("Annotations: "), extLabel("chip")));
        return box;
    }

    private static HBox formatRow(Node... parts) {
        HBox row = new HBox(0, parts);
        row.setAlignment(javafx.geometry.Pos.BASELINE_LEFT);
        return row;
    }

    private static Label plainLabel(String s) {
        Label lab = new Label(s);
        lab.setWrapText(false);
        return lab;
    }

    private static Label extLabel(String s) {
        Label lab = new Label(s);
        lab.setTextFill(Color.web("#800000"));
        lab.setStyle("-fx-font-style: italic; -fx-font-weight: bold; -fx-font-size: 14px;");
        return lab;
    }

    /**
     * Approximate : abort mid-read when cancelled,
     * and report byte progress for the current file.
     */
    private final class CancellableFileInputStream extends java.io.FilterInputStream {
        private final java.util.function.LongConsumer onBytes;
        private long bytesRead;

        CancellableFileInputStream(File file, java.util.function.LongConsumer onBytes)
                throws java.io.IOException {
            super(new java.io.FileInputStream(file));
            this.onBytes = onBytes;
        }

        private void checkCancelled() throws java.io.IOException {
            if (loadCancelled) {
                throw new java.io.InterruptedIOException("Load cancelled");
            }
        }

        private void note(int n) {
            if (n > 0) {
                bytesRead += n;
                if (onBytes != null) {
                    onBytes.accept(bytesRead);
                }
            }
        }

        @Override
        public int read() throws java.io.IOException {
            checkCancelled();
            int b = super.read();
            if (b >= 0) {
                note(1);
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws java.io.IOException {
            checkCancelled();
            int n = super.read(b, off, len);
            note(n);
            return n;
        }
    }

    @Override
    public String getTitle() {
        return "Load data";
    }

    @Override
    public String getIconResourceId() {
        return "LocalFileExplorerWidget_16_v2.jpg";
    }

    @Override
    public Object getContent() {
        return root;
    }
}
