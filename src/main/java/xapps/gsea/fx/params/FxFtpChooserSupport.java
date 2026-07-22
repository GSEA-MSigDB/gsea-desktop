/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.io.File;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.SystemUtils;
import org.genepattern.io.FTPFile;
import org.genepattern.io.FTPList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.alg.ComparatorFactory;
import edu.mit.broad.genome.objects.MSigDBSpecies;
import edu.mit.broad.genome.objects.MSigDBVersion;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import xapps.gsea.GseaWebResources;
import xapps.gsea.fx.FxProgressMonitorRead;

/**
 * Non-UI FTP listing helpers shared by JavaFX gene-set and chip choosers.
 */
public final class FxFtpChooserSupport {
    private static final Logger klog = LoggerFactory.getLogger(FxFtpChooserSupport.class);

    public static final String OFFLINE_MESSAGE =
            "Offline mode" + SystemUtils.LINE_SEPARATOR
                    + "Change this in Menu=>Preferences" + SystemUtils.LINE_SEPARATOR
                    + "Use 'Load Data' to access local files." + SystemUtils.LINE_SEPARATOR
                    + "Choose gene sets from other tabs.";

    public static final String DESELECT_INSTRUCTIONS = SystemUtils.IS_OS_MAC
            ? "Use command-click to select/deselect items."
            : "Use control-click to select/deselect items.";

    public static final String IMPORT_LOCAL_FILE = "Import Local File";

    private static final Map<String, List<FTPFile>> LISTING_CACHE = new ConcurrentHashMap<>();

    private FxFtpChooserSupport() {
    }

    /** Read-only wrapped text area used for offline / help messages in chooser tabs. */
    public static javafx.scene.control.TextArea messageArea(String text) {
        javafx.scene.control.TextArea area = new javafx.scene.control.TextArea(text);
        area.setEditable(false);
        area.setWrapText(true);
        return area;
    }

    /** Information alert when more than one tab has a selection. */
    public static void showMultiSelectionInfo(javafx.stage.Window owner, String title, String header) {
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
    public static boolean confirmMixedMsigdbVersions(javafx.stage.Window owner) {
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
        java.util.Optional<javafx.scene.control.ButtonType> choice = alert.showAndWait();
        return choice.isPresent() && choice.get() == javafx.scene.control.ButtonType.OK;
    }

    public static boolean isOnline() {
        return XPreferencesFactory.kOnlineMode.getBoolean();
    }

    /**
     * List FTP files ending with {@code suffix}, attaching an {@link MSigDBVersion} parsed from each name.
     */
    public static List<FTPFile> listFtpFiles(String suffix, MSigDBSpecies species, String ftpDir)
            throws Exception {
        FTPList ftpList = null;
        try {
            ftpList = new FTPList(
                    GseaWebResources.getGseaFTPServer(),
                    GseaWebResources.getGseaFTPServerUserName(),
                    GseaWebResources.getGseaFTPServerPassword());
            String[] ftpFileNames = ftpList.getDirectoryListing(ftpDir, null);
            List<FTPFile> files = new ArrayList<>();
            if (ftpFileNames == null) {
                return files;
            }
            for (String ftpFileName : ftpFileNames) {
                String versionId = NamingConventions.extractVersionFromFileName(ftpFileName, suffix);
                files.add(new FTPFile(ftpList.host, ftpDir, ftpFileName, new MSigDBVersion(species, versionId)));
            }
            return files;
        } finally {
            if (ftpList != null) {
                try {
                    ftpList.quit();
                } catch (Exception e) {
                    klog.debug("FTP quit: {}", e.toString());
                }
            }
        }
    }

    public static List<FTPFile> listAndSort(String suffix, MSigDBSpecies species, String ftpDir,
            ComparatorFactory.FTPFileByVersionComparator comparator) throws Exception {
        List<FTPFile> files = listFtpFiles(suffix, species, ftpDir);
        FTPFile[] arr = files.toArray(new FTPFile[0]);
        Arrays.parallelSort(arr, comparator);
        return Arrays.asList(arr);
    }

    private static String listingKey(String suffix, MSigDBSpecies species, String ftpDir) {
        return species.name() + '|' + suffix + '|' + ftpDir;
    }

    /**
     * Loads an MSigDB FTP directory listing on a background thread. Results are cached for the
     * session so reopening a chooser is instant.
     */
    public static void loadFtpListingAsync(
            String suffix,
            MSigDBSpecies species,
            String ftpDir,
            ComparatorFactory.FTPFileByVersionComparator comparator,
            Consumer<List<FTPFile>> onSuccess,
            Consumer<Exception> onFailure) {
        String key = listingKey(suffix, species, ftpDir);
        List<FTPFile> cached = LISTING_CACHE.get(key);
        if (cached != null) {
            Platform.runLater(() -> onSuccess.accept(cached));
            return;
        }
        Task<List<FTPFile>> task = new Task<>() {
            @Override
            protected List<FTPFile> call() throws Exception {
                List<FTPFile> files = listAndSort(suffix, species, ftpDir, comparator);
                LISTING_CACHE.put(key, files);
                return files;
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            Exception ex = t instanceof Exception exception
                    ? exception : new Exception(t != null ? t.getMessage() : "FTP listing failed", t);
            onFailure.accept(ex);
        });
        Thread th = new Thread(task, "ftp-listing-" + species.name());
        th.setDaemon(true);
        th.start();
    }

    public static Tab createPendingFtpTab(String title) {
        Tab tab = new Tab(title);
        Label status = new Label("Loading MSigDB files from server…");
        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(24, 24);
        VBox pending = new VBox(8, status, progress);
        pending.setPadding(new Insets(12));
        tab.setContent(pending);
        return tab;
    }

    public static void finishFtpTab(
            Tab tab,
            ListView<FTPFile> listView,
            Button okButton,
            List<FTPFile> files,
            ComparatorFactory.FTPFileByVersionComparator comparator,
            Function<FTPFile, String> searchText) {
        listView.setItems(FXCollections.observableArrayList(files));
        applyLatestVersionBolding(listView, comparator);
        enableDoubleClickToFire(listView, okButton);
        tab.setContent(xapps.gsea.fx.FxSearchField.wrapList(listView,
                f -> f == null ? "" : searchText.apply(f)));
    }

    public static void failFtpTab(Tab tab, Exception ex) {
        klog.error(ex.getMessage(), ex);
        TextArea area = new TextArea(errorListingMessage(ex));
        area.setEditable(false);
        area.setWrapText(true);
        tab.setContent(area);
    }

    /**
     * Adds an MSigDB FTP tab that loads only when first selected (never blocks dialog open).
     */
    public static void installDeferredFtpTab(
            TabPane tabs,
            String title,
            ListView<FTPFile> listView,
            Button okButton,
            String suffix,
            MSigDBSpecies species,
            String ftpDir,
            ComparatorFactory.FTPFileByVersionComparator comparator,
            Function<FTPFile, String> searchText) {
        Tab tab = createPendingFtpTab(title);
        tabs.getTabs().add(tab);
        AtomicBoolean started = new AtomicBoolean(false);
        Runnable load = () -> {
            if (!started.compareAndSet(false, true)) {
                return;
            }
            loadFtpListingAsync(
                    suffix,
                    species,
                    ftpDir,
                    comparator,
                    files -> finishFtpTab(tab, listView, okButton, files, comparator, searchText),
                    ex -> failFtpTab(tab, ex));
        };
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, selected) -> {
            if (selected == tab) {
                load.run();
            }
        });
    }

    /** Style FTP list cells: bold rows whose name contains the highest MSigDB version id. */
    public static void applyLatestVersionBolding(javafx.scene.control.ListView<FTPFile> list,
            ComparatorFactory.FTPFileByVersionComparator comparator) {
        final String highest = comparator != null ? comparator.getHighestVersionId() : null;
        list.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(FTPFile item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }
                setText(item.getName());
                setGraphic(xapps.gsea.fx.FxFileIcons.forResource("FTPFile.gif"));
                boolean latest = highest != null && item.getName() != null
                        && item.getName().toLowerCase(java.util.Locale.ROOT)
                        .contains(highest.toLowerCase(java.util.Locale.ROOT));
                setStyle(latest ? "-fx-font-weight: bold;" : "");
            }
        });
    }

    public static String errorListingMessage(Exception e) {
        return "Error listing MSigDB files:" + SystemUtils.LINE_SEPARATOR
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
            Application.getWindowManager().showError(url + ": unable to launch web browser", t);
        }
    }

    /**
     * Adds non-closing Help (data-format anchor) and Info (arbitrary URL) buttons to a dialog's
     * button bar {@code JarResources.createDataFormatAction}
     * with a {@code BrowserAction} info link.
     */
    public static void addHelpAndInfoButtons(Dialog<?> dialog, String dataFormatAnchor,
            String infoLabel, String infoUrl) {
        ButtonType helpType = new ButtonType("Help", ButtonBar.ButtonData.HELP_2);
        ButtonType infoType = new ButtonType(infoLabel, ButtonBar.ButtonData.HELP);
        dialog.getDialogPane().getButtonTypes().addAll(0, List.of(helpType, infoType));
        wireNonClosingButton(dialog, helpType, () -> openUrl(GseaWebResources.getGseaDataFormatsHelpURL() + dataFormatAnchor));
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
        wireNonClosingButton(dialog, helpType, () -> openUrl(GseaWebResources.getGseaDataFormatsHelpURL() + dataFormatAnchor));
    }

    /**
     * Adds a single non-closing Help button that opens the User Guide at {@code ugAnchor}
     * (e.g. {@code "#Phenotype-Select-Window")
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

    /** Makes double-clicking a list item act like clicking OK */
    public static void enableDoubleClickToFire(ListView<?> list, Button okButton) {
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && list.getSelectionModel().getSelectedItem() != null) {
                okButton.fire();
            }
        });
    }

    /**
     * Label plus ellipsis button for importing a file from disk into the local cache.
     */
    public static HBox importLocalFileRow(Runnable openAction) {
        Label label = new Label(IMPORT_LOCAL_FILE);
        Button open = xapps.gsea.fx.FxEllipsisButton.create(IMPORT_LOCAL_FILE);
        xapps.gsea.fx.FxButtons.styleSecondary(open);
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
                Application.getWindowManager().showError("Could not load file", t);
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
}
