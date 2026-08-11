/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.io.File;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.alg.ComparatorFactory;
import edu.mit.broad.genome.io.MSigDBCatalogClient;
import edu.mit.broad.genome.objects.MSigDBCatalogFile;
import edu.mit.broad.genome.objects.MSigDBCatalogOrdering;
import edu.mit.broad.genome.objects.MSigDBRelease;
import edu.mit.broad.genome.objects.MSigDBSpecies;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import xapps.gsea.GseaWebResources;
import xapps.gsea.fx.widgets.FxProgressMonitorRead;
import org.gsea_msigdb.gsea.runtime.AppServices;

/**
 * Shared helpers for JavaFX gene-set and chip choosers, including deferred MSigDB HTTP/JSON
 * catalog trees.
 */
public final class FxChooserSupport {
    private static final Logger klog = LoggerFactory.getLogger(FxChooserSupport.class);

    public static final String OFFLINE_MESSAGE =
            "Offline mode" + SystemUtils.LINE_SEPARATOR
                    + "Change this in Menu=>Preferences" + SystemUtils.LINE_SEPARATOR
                    + "Use 'Load Data' to access local files." + SystemUtils.LINE_SEPARATOR
                    + "Choose gene sets from other tabs.";

    public static final String DESELECT_INSTRUCTIONS = SystemUtils.IS_OS_MAC
            ? "Use command-click to select/deselect items."
            : "Use control-click to select/deselect items.";

    public static final String IMPORT_LOCAL_FILE = "Import Local File";

    /** Placeholder child under an unexpanded older release until its file catalog is fetched. */
    public static final String LOADING_PLACEHOLDER = "Loading…";

    private FxChooserSupport() {
    }

    /** Read-only wrapped text area used for offline / help messages in chooser tabs. */
    public static TextArea messageArea(String text) {
        TextArea area = new TextArea(text);
        area.setEditable(false);
        area.setWrapText(true);
        return area;
    }

    /** Information alert when more than one tab has a selection. */
    public static void showMultiSelectionInfo(Window owner, String title, String header) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText("Is there a selection on another tab?\n" + DESELECT_INSTRUCTIONS);
        xapps.gsea.fx.FxTheme.apply(alert);
        alert.showAndWait();
    }

    /** @return true if the user chose OK to keep a mixed-version selection */
    public static boolean confirmMixedMsigdbVersions(Window owner) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle("Mixed MSigDB versions detected");
        alert.setHeaderText("Mixed MSigDB versions detected");
        alert.setContentText(
                "Selecting collections from multiple MSigDB versions may result in omitted genes "
                        + "and is not recommended.\n"
                        + "NOTE: another tab may have a selection.\n"
                        + DESELECT_INSTRUCTIONS
                        + "\n\nClick Cancel to change the selection or OK to keep it.");
        xapps.gsea.fx.FxTheme.apply(alert);
        java.util.Optional<ButtonType> choice = alert.showAndWait();
        return choice.isPresent() && choice.get() == ButtonType.OK;
    }

    public static boolean isOnline() {
        return XPreferencesFactory.kOnlineMode.getBoolean();
    }

    /**
     * Installs a deferred MSigDB catalog tab: loads release catalog when first selected,
     * shows TreeView of releases (newest first). Eager-fetches files for newest release;
     * older releases get a "Loading…" placeholder child replaced on first expand.
     *
     * @param fileCatalogUrlExtractor MSigDBRelease::getGeneSetsCatalogUrl or getChipCatalogUrl
     * @param multipleSelection true for gene sets, false for chips
     */
    public static TreeView<Object> installDeferredCatalogTab(
            TabPane tabs,
            String title,
            Button okButton,
            MSigDBSpecies species,
            Function<MSigDBRelease, String> fileCatalogUrlExtractor,
            boolean multipleSelection) {
        TreeView<Object> tree = new TreeView<>();
        tree.getSelectionModel().setSelectionMode(
                multipleSelection ? SelectionMode.MULTIPLE : SelectionMode.SINGLE);
        tree.setShowRoot(false);

        Tab tab = createPendingCatalogTab(title);
        tabs.getTabs().add(tab);

        AtomicBoolean started = new AtomicBoolean(false);
        Runnable load = () -> {
            if (!started.compareAndSet(false, true)) {
                return;
            }
            Task<TreeBuildResult> task = new Task<>() {
                @Override
                protected TreeBuildResult call() throws Exception {
                    List<MSigDBRelease> allReleases = MSigDBCatalogClient.fetchReleaseCatalog(
                            XPreferencesFactory.kMSigDBCatalogURL.getString());
                    List<MSigDBRelease> releases = new ArrayList<>();
                    for (MSigDBRelease release : allReleases) {
                        if (release.getSpecies() == species) {
                            releases.add(release);
                        }
                    }
                    ComparatorFactory.MSigDBReleaseByVersionComparator comp =
                            new ComparatorFactory.MSigDBReleaseByVersionComparator();
                    Collections.sort(releases, comp);

                    TreeItem<Object> root = new TreeItem<>();
                    for (int i = 0; i < releases.size(); i++) {
                        MSigDBRelease release = releases.get(i);
                        TreeItem<Object> releaseItem = new TreeItem<>(release);
                        if (i == 0) {
                            List<MSigDBCatalogFile> files = MSigDBCatalogClient.fetchFileCatalog(
                                    fileCatalogUrlExtractor.apply(release));
                            addFileChildren(releaseItem, files);
                            releaseItem.setExpanded(true);
                        } else {
                            releaseItem.getChildren().add(new TreeItem<>(LOADING_PLACEHOLDER));
                            wireLazyFileFetch(releaseItem, fileCatalogUrlExtractor);
                        }
                        root.getChildren().add(releaseItem);
                    }
                    return new TreeBuildResult(root, comp.getHighestVersionId());
                }
            };
            task.setOnSucceeded(e -> {
                TreeBuildResult result = task.getValue();
                tree.setRoot(result.root);
                applyCatalogCellFactory(tree, result.highestVersionId);
                clearNonLeafSelection(tree);
                enableDoubleClickToFire(tree, okButton);
                tab.setContent(tree);
            });
            task.setOnFailed(e -> {
                Throwable t = task.getException();
                Exception ex = t instanceof Exception exception
                        ? exception
                        : new Exception(t != null ? t.getMessage() : "Catalog listing failed", t);
                failCatalogTab(tab, ex);
            });
            Thread th = new Thread(task, "msigdb-catalog-" + species.name());
            th.setDaemon(true);
            th.start();
        };
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, selected) -> {
            if (selected == tab) {
                load.run();
            }
        });
        return tree;
    }

    /**
     * @return selected leaf {@link MSigDBCatalogFile} values only (release nodes ignored)
     */
    public static List<MSigDBCatalogFile> getSelectedCatalogFiles(TreeView<Object> tree) {
        List<MSigDBCatalogFile> selected = new ArrayList<>();
        if (tree == null) {
            return selected;
        }
        for (TreeItem<Object> item : tree.getSelectionModel().getSelectedItems()) {
            if (item != null && item.getValue() instanceof MSigDBCatalogFile) {
                selected.add((MSigDBCatalogFile) item.getValue());
            }
        }
        return selected;
    }

    private static Tab createPendingCatalogTab(String title) {
        Tab tab = new Tab(title);
        Label status = new Label("Loading MSigDB catalog…");
        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(24, 24);
        VBox pending = new VBox(8, status, progress);
        pending.setPadding(new Insets(12));
        tab.setContent(pending);
        return tab;
    }

    private static void failCatalogTab(Tab tab, Exception ex) {
        klog.error(ex.getMessage(), ex);
        TextArea area = new TextArea(errorListingMessage(ex));
        area.setEditable(false);
        area.setWrapText(true);
        tab.setContent(area);
    }

    private static void addFileChildren(TreeItem<Object> releaseItem, List<MSigDBCatalogFile> files) {
        releaseItem.getChildren().clear();
        if (files == null || files.isEmpty()) {
            releaseItem.getChildren().add(new TreeItem<>("No files available"));
            return;
        }
        for (MSigDBCatalogFile file : MSigDBCatalogOrdering.withHallmarkFirst(files)) {
            releaseItem.getChildren().add(new TreeItem<>(file));
        }
    }

    private static void wireLazyFileFetch(
            TreeItem<Object> releaseItem,
            Function<MSigDBRelease, String> fileCatalogUrlExtractor) {
        AtomicBoolean started = new AtomicBoolean(false);
        releaseItem.expandedProperty().addListener((obs, wasExpanded, expanded) -> {
            if (!expanded) {
                return;
            }
            if (!(releaseItem.getValue() instanceof MSigDBRelease)) {
                return;
            }
            if (releaseItem.getChildren().size() != 1
                    || !LOADING_PLACEHOLDER.equals(releaseItem.getChildren().get(0).getValue())) {
                return;
            }
            if (!started.compareAndSet(false, true)) {
                return;
            }
            MSigDBRelease release = (MSigDBRelease) releaseItem.getValue();
            Task<List<MSigDBCatalogFile>> task = new Task<>() {
                @Override
                protected List<MSigDBCatalogFile> call() throws Exception {
                    return MSigDBCatalogClient.fetchFileCatalog(
                            fileCatalogUrlExtractor.apply(release));
                }
            };
            task.setOnSucceeded(e -> addFileChildren(releaseItem, task.getValue()));
            task.setOnFailed(e -> {
                Throwable t = task.getException();
                String msg = t != null ? t.getMessage() : "Unknown error";
                klog.error(msg, t);
                releaseItem.getChildren().setAll(new TreeItem<>("Error: " + msg));
            });
            Thread th = new Thread(task, "msigdb-file-catalog-" + release.getReleaseName());
            th.setDaemon(true);
            th.start();
        });
    }

    private static void applyCatalogCellFactory(TreeView<Object> tree, String highestVersionId) {
        final String highest = StringUtils.lowerCase(highestVersionId);
        tree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                    setStyle("");
                    return;
                }
                if (item instanceof MSigDBRelease) {
                    MSigDBRelease release = (MSigDBRelease) item;
                    setText(release.getReleaseName());
                    setGraphic(null);
                    setTooltip(tooltipForRelease(release));
                    String versionId = StringUtils.lowerCase(
                            release.getMSigDBVersion().getVersionString());
                    boolean latest = highest != null && highest.equals(versionId);
                    setStyle(latest ? "-fx-font-weight: bold;" : "");
                } else if (item instanceof MSigDBCatalogFile) {
                    MSigDBCatalogFile file = (MSigDBCatalogFile) item;
                    setText(file.getName());
                    setGraphic(xapps.gsea.fx.widgets.FxFileIcons.forResource("FTPFile.gif"));
                    setTooltip(StringUtils.isNotBlank(file.getDescription())
                            ? new Tooltip(file.getDescription()) : null);
                    setStyle("");
                } else if (LOADING_PLACEHOLDER.equals(item)) {
                    setText(LOADING_PLACEHOLDER);
                    setGraphic(null);
                    setTooltip(null);
                    setStyle("-fx-font-style: italic;");
                } else {
                    setText(String.valueOf(item));
                    setGraphic(null);
                    setTooltip(null);
                    setStyle("");
                }
            }
        });
    }

    private static Tooltip tooltipForRelease(MSigDBRelease release) {
        StringBuilder buf = new StringBuilder();
        if (StringUtils.isNotBlank(release.getDescription())) {
            buf.append(release.getDescription());
        }
        if (StringUtils.isNotBlank(release.getReleaseDate())) {
            if (buf.length() > 0) {
                buf.append(" -- ");
            }
            buf.append("released ").append(release.getReleaseDate());
        }
        return buf.length() > 0 ? new Tooltip(buf.toString()) : null;
    }

    private static void clearNonLeafSelection(TreeView<Object> tree) {
        tree.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, selected) -> {
            if (selected == null) {
                return;
            }
            Object value = selected.getValue();
            if (!(value instanceof MSigDBCatalogFile)) {
                Platform.runLater(() -> {
                    int row = tree.getRow(selected);
                    if (row >= 0) {
                        tree.getSelectionModel().clearSelection(row);
                    }
                });
            }
        });
    }

    public static String errorListingMessage(Exception e) {
        return "Error listing MSigDB catalog:" + SystemUtils.LINE_SEPARATOR
                + e.getMessage() + SystemUtils.LINE_SEPARATOR + SystemUtils.LINE_SEPARATOR
                + "This might be due to your network's firewall rules." + SystemUtils.LINE_SEPARATOR
                + "MSigDB files can be manually downloaded from www.gsea-msigdb.org/gsea/downloads.jsp"
                + SystemUtils.LINE_SEPARATOR + SystemUtils.LINE_SEPARATOR
                + "Use 'Load Data' to provide access to local files." + SystemUtils.LINE_SEPARATOR
                + "Choose gene sets from other tabs.";
    }

    /** Opens {@code url} in the platform browser, reporting failures through the window manager. */
    public static void openUrl(String url) {
        try {
            xapps.gsea.fx.FxDesktopUtil.openUrl(url);
        } catch (Throwable t) {
            AppServices.require().dialogs().showError(url + ": unable to launch web browser", t);
        }
    }

    /**
     * Adds non-closing Help (data-format anchor) and Info (arbitrary URL) buttons to a dialog's
     * button bar.
     */
    public static void addHelpAndInfoButtons(Dialog<?> dialog, String dataFormatAnchor,
            String infoLabel, String infoUrl) {
        ButtonType helpType = new ButtonType("Help", ButtonBar.ButtonData.HELP_2);
        ButtonType infoType = new ButtonType(infoLabel, ButtonBar.ButtonData.HELP);
        dialog.getDialogPane().getButtonTypes().addAll(0, List.of(helpType, infoType));
        wireNonClosingButton(dialog, helpType,
                () -> openUrl(GseaWebResources.getGseaDataFormatsHelpURL() + dataFormatAnchor));
        wireNonClosingButton(dialog, infoType, () -> openUrl(infoUrl));
    }

    public static void addMsigdbLicenseButton(Dialog<?> dialog) {
        ButtonType licenseType = new ButtonType("MSigDB License", ButtonBar.ButtonData.HELP);
        dialog.getDialogPane().getButtonTypes().add(0, licenseType);
        wireNonClosingButton(dialog, licenseType,
                () -> openUrl(GseaWebResources.getGseaBaseURL() + "/license_terms_list.jsp"));
    }

    /**
     * Adds a single non-closing Help button (data-format anchor) to a dialog's button bar.
     */
    public static void addHelpButton(Dialog<?> dialog, String dataFormatAnchor) {
        ButtonType helpType = new ButtonType("Help", ButtonBar.ButtonData.HELP_2);
        dialog.getDialogPane().getButtonTypes().add(0, helpType);
        wireNonClosingButton(dialog, helpType,
                () -> openUrl(GseaWebResources.getGseaDataFormatsHelpURL() + dataFormatAnchor));
    }

    /**
     * Adds a single non-closing Help button that opens the User Guide at {@code ugAnchor}
     * (e.g. {@code "#Phenotype-Select-Window").
     */
    public static void addUserGuideHelpButton(Dialog<?> dialog, String ugAnchor) {
        ButtonType helpType = new ButtonType("Help", ButtonBar.ButtonData.HELP_2);
        dialog.getDialogPane().getButtonTypes().add(0, helpType);
        wireNonClosingButton(dialog, helpType,
                () -> openUrl(GseaWebResources.getGseaHelpURL() + "GSEA/GSEA_User_Guide/" + ugAnchor));
    }

    private static void wireNonClosingButton(Dialog<?> dialog, ButtonType buttonType, Runnable action) {
        Button button = (Button) dialog.getDialogPane().lookupButton(buttonType);
        button.addEventFilter(ActionEvent.ACTION, e -> {
            action.run();
            e.consume();
        });
    }

    /** Makes double-clicking a list item act like clicking OK. */
    public static void enableDoubleClickToFire(ListView<?> list, Button okButton) {
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && list.getSelectionModel().getSelectedItem() != null) {
                okButton.fire();
            }
        });
    }

    /** Makes double-clicking a file leaf in a catalog tree act like clicking OK. */
    public static void enableDoubleClickToFire(TreeView<Object> tree, Button okButton) {
        tree.setOnMouseClicked(e -> {
            if (e.getClickCount() != 2) {
                return;
            }
            TreeItem<Object> selected = tree.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getValue() instanceof MSigDBCatalogFile) {
                okButton.fire();
            }
        });
    }

    /**
     * Label plus ellipsis button for importing a file from disk into the local cache.
     */
    public static HBox importLocalFileRow(Runnable openAction) {
        Label label = new Label(IMPORT_LOCAL_FILE);
        Button open = xapps.gsea.fx.widgets.FxEllipsisButton.create(IMPORT_LOCAL_FILE);
        xapps.gsea.fx.widgets.FxButtons.styleSecondary(open);
        open.setOnAction(e -> openAction.run());
        Region spacer = new Region();
        HBox row = new HBox(6, label, spacer, open);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return row;
    }

    /**
     * Local-cache tab content: import row above a search-wrapped list.
     */
    public static VBox wrapLocalTab(VBox searchWrappedList, Runnable openAction) {
        VBox box = new VBox(6, importLocalFileRow(openAction), searchWrappedList);
        box.setFillWidth(true);
        VBox.setVgrow(searchWrappedList, Priority.ALWAYS);
        return box;
    }

    public static void browseLocalFiles(
            Window owner,
            String title,
            List<FileChooser.ExtensionFilter> filters,
            boolean multiple,
            String seedPath,
            Consumer<List<File>> onChosen) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        if (filters != null) {
            chooser.getExtensionFilters().addAll(filters);
        }
        FxFileChooserUtil.seedInitialDirectory(chooser, seedPath);
        List<File> chosen;
        if (multiple) {
            chosen = chooser.showOpenMultipleDialog(owner);
            if (chosen == null) {
                chosen = List.of();
            }
        } else {
            File one = chooser.showOpenDialog(owner);
            chosen = one != null ? List.of(one) : List.of();
        }
        if (!chosen.isEmpty()) {
            onChosen.accept(chosen);
        }
    }

    /**
     * Parse local files on a background thread (with progress) and deliver loaded objects on the FX thread.
     */
    public static void loadLocalFilesAsync(
            List<File> files,
            Class<?> expectedType,
            Consumer<List<PersistentObject>> onLoaded) {
        if (files == null || files.isEmpty()) {
            return;
        }
        for (File f : files) {
            FxFileChooserUtil.registerOpened(f);
        }
        Task<List<PersistentObject>> task = new Task<>() {
            @Override
            protected List<PersistentObject> call() throws Exception {
                List<PersistentObject> loaded = new ArrayList<>();
                for (File file : files) {
                    PersistentObject obj = FxProgressMonitorRead.read(file);
                    if (!expectedType.isInstance(obj)) {
                        String got = obj != null ? obj.getClass().getSimpleName() : "null";
                        throw new IllegalArgumentException(
                                "Expected " + expectedType.getSimpleName() + " from "
                                        + file.getName() + ", got " + got);
                    }
                    loaded.add(obj);
                }
                return loaded;
            }
        };
        task.setOnSucceeded(e -> onLoaded.accept(task.getValue()));
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            if (t != null && !(t instanceof InterruptedIOException)) {
                AppServices.require().dialogs().showError("Could not load file", t);
            }
        });
        Thread th = new Thread(task, "load-local-chooser-file");
        th.setDaemon(true);
        th.start();
    }

    public static List<FileChooser.ExtensionFilter> chipFileFilters() {
        return List.of(
                new FileChooser.ExtensionFilter("Chip (*.chip)", "*.chip"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
    }

    public static List<FileChooser.ExtensionFilter> gmxFileFilters() {
        return List.of(
                new FileChooser.ExtensionFilter("Gene set matrix (*.gmt, *.gmx)", "*.gmt", "*.gmx"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
    }

    public static List<FileChooser.ExtensionFilter> grpFileFilters() {
        return List.of(
                new FileChooser.ExtensionFilter("Gene set (*.grp)", "*.grp"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
    }

    public static List<FileChooser.ExtensionFilter> clsFileFilters() {
        return List.of(
                new FileChooser.ExtensionFilter("Phenotype (*.cls)", "*.cls"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
    }

    private static final class TreeBuildResult {
        final TreeItem<Object> root;
        final String highestVersionId;

        TreeBuildResult(TreeItem<Object> root, String highestVersionId) {
            this.root = root;
            this.highestVersionId = highestVersionId;
        }
    }
}
