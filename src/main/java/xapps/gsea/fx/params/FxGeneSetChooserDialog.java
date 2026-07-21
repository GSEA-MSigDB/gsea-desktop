/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.genepattern.io.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.alg.ComparatorFactory;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.MSigDBSpecies;
import edu.mit.broad.genome.objects.MSigDBVersion;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.objects.Versioned;
import edu.mit.broad.genome.parsers.ParseUtils;
import edu.mit.broad.genome.parsers.ParserFactory;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import xapps.gsea.GseaWebResources;

/**
 * JavaFX gene-set chooser: Human/Mouse MSigDB FTP, local multi-file browse, and text entry.
 */
public final class FxGeneSetChooserDialog {
    private static final Logger klog = LoggerFactory.getLogger(FxGeneSetChooserDialog.class);

    private FxGeneSetChooserDialog() {
    }

    /**
     * @return comma-joined paths, or empty if cancelled
     */
    public static Optional<String> show(Window owner) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Select a gene set");
        dialog.initOwner(owner);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResizable(true);
        FxFtpChooserSupport.addHelpAndInfoButtons(dialog, "#gmx",
                "MSigDB Collections", GseaWebResources.getGseaBaseURL() + "/msigdb/");
        FxFtpChooserSupport.addMsigdbLicenseButton(dialog);
        final Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        ListView<FTPFile> humanList = new ListView<>();
        humanList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        ListView<FTPFile> mouseList = new ListView<>();
        mouseList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        ListView<CachedPathItem> cachedGmx = cachedObjectList(edu.mit.broad.genome.objects.GeneSetMatrix.class);
        ListView<CachedPathItem> cachedGrp = cachedObjectList(GeneSet.class);
        ListView<CachedPathItem> subsets = auxGeneSetList();
        FxFtpChooserSupport.enableDoubleClickToFire(cachedGmx, okButton);
        FxFtpChooserSupport.enableDoubleClickToFire(cachedGrp, okButton);
        FxFtpChooserSupport.enableDoubleClickToFire(subsets, okButton);

        Window dialogWindow = owner;
        // Open on local cache first; MSigDB FTP collections are secondary.
        tabs.getTabs().add(new Tab("Local GMX/GMT",
                FxFtpChooserSupport.wrapLocalTab(
                        xapps.gsea.fx.FxSearchField.wrapList(cachedGmx,
                                i -> i == null ? "" : i.name + " " + i.path),
                        () -> openLocalGeneSetFiles(
                                dialogWindow,
                                cachedGmx,
                                edu.mit.broad.genome.objects.GeneSetMatrix.class,
                                FxFtpChooserSupport.gmxFileFilters(),
                                true))));
        tabs.getTabs().add(new Tab("Local GRP Gene sets",
                FxFtpChooserSupport.wrapLocalTab(
                        xapps.gsea.fx.FxSearchField.wrapList(cachedGrp,
                                i -> i == null ? "" : i.name + " " + i.path),
                        () -> openLocalGeneSetFiles(
                                dialogWindow,
                                cachedGrp,
                                GeneSet.class,
                                FxFtpChooserSupport.grpFileFilters(),
                                true))));
        tabs.getTabs().add(new Tab("Subsets",
                xapps.gsea.fx.FxSearchField.wrapList(subsets, i -> i == null ? "" : i.name + " " + i.path)));

        if (FxFtpChooserSupport.isOnline()) {
            ComparatorFactory.FTPFileByVersionComparator humanCmp =
                    new ComparatorFactory.FTPFileByVersionComparator("h");
            FxFtpChooserSupport.installDeferredFtpTab(
                    tabs,
                    "Human Collection (MSigDB)",
                    humanList,
                    okButton,
                    ".symbols.gmt",
                    MSigDBSpecies.Human,
                    GseaWebResources.getGseaFTPServerGeneSetsDir(MSigDBSpecies.Human),
                    humanCmp,
                    f -> f.getName() + " " + f.getPath());
            ComparatorFactory.FTPFileByVersionComparator mouseCmp =
                    new ComparatorFactory.FTPFileByVersionComparator("mh");
            FxFtpChooserSupport.installDeferredFtpTab(
                    tabs,
                    "Mouse Collection (MSigDB)",
                    mouseList,
                    okButton,
                    ".symbols.gmt",
                    MSigDBSpecies.Mouse,
                    GseaWebResources.getGseaFTPServerGeneSetsDir(MSigDBSpecies.Mouse),
                    mouseCmp,
                    f -> f.getName() + " " + f.getPath());
        } else {
            tabs.getTabs().add(new Tab("Human Collection (MSigDB)", messageArea(FxFtpChooserSupport.OFFLINE_MESSAGE)));
            tabs.getTabs().add(new Tab("Mouse Collection (MSigDB)", messageArea(FxFtpChooserSupport.OFFLINE_MESSAGE)));
        }

        tabs.getSelectionModel().selectedItemProperty().addListener((obs, o, tab) -> {
            if (tab == null) {
                return;
            }
            String t = tab.getText();
            if ("Local GMX/GMT".equals(t)) {
                refreshCachedObjectList(cachedGmx, edu.mit.broad.genome.objects.GeneSetMatrix.class);
            } else if ("Local GRP Gene sets".equals(t)) {
                refreshCachedObjectList(cachedGrp, GeneSet.class);
            } else if ("Subsets".equals(t)) {
                refreshAuxGeneSetList(subsets);
            }
        });

        javafx.animation.Timeline cacheRefresh = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), ev -> {
                    Tab sel = tabs.getSelectionModel().getSelectedItem();
                    if (sel == null) {
                        return;
                    }
                    String t = sel.getText();
                    if ("Local GMX/GMT".equals(t)) {
                        refreshCachedObjectList(cachedGmx, edu.mit.broad.genome.objects.GeneSetMatrix.class);
                    } else if ("Local GRP Gene sets".equals(t)) {
                        refreshCachedObjectList(cachedGrp, GeneSet.class);
                    } else if ("Subsets".equals(t)) {
                        refreshAuxGeneSetList(subsets);
                    }
                }));
        cacheRefresh.setCycleCount(javafx.animation.Animation.INDEFINITE);
        dialog.setOnShown(e -> cacheRefresh.play());
        dialog.setOnHidden(e -> cacheRefresh.stop());

        TextArea taGenes = new TextArea();
        TitledPane textEntry = new TitledPane(
                "Make an 'on-the-fly' gene set: Enter features below, one per line", taGenes);
        textEntry.setCollapsible(false);
        tabs.getTabs().add(new Tab("Text entry", textEntry));

        tabs.getSelectionModel().select(0);

        BorderPane root = new BorderPane(tabs);
        root.setPadding(new Insets(8));
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setPrefSize(800, 400);
        xapps.gsea.fx.FxTheme.apply(dialog);
        xapps.gsea.fx.FxButtons.stylePrimary(okButton);
        xapps.gsea.fx.FxButtons.styleSecondary(
                (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL));

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            List<Versioned> selected = collectSelectedVersioned(
                    humanList, mouseList, cachedGmx, cachedGrp, subsets);
            if (!speciesOk(selected)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.initOwner(dialog.getDialogPane().getScene().getWindow());
                alert.setTitle("Multiple species selected");
                alert.setHeaderText("Multiple species selections are not allowed.");
                alert.setContentText("Is there a selection on another tab?\n" + FxFtpChooserSupport.DESELECT_INSTRUCTIONS);
                xapps.gsea.fx.FxTheme.apply(alert);
                alert.showAndWait();
                event.consume();
                return;
            }
            boolean hasOnTheFly = StringUtils.isNotBlank(taGenes.getText());
            if (!versionsOk(selected, hasOnTheFly)) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.initOwner(dialog.getDialogPane().getScene().getWindow());
                alert.setTitle("Mixed MSigDB versions detected");
                alert.setHeaderText("Mixed MSigDB versions detected");
                alert.setContentText(
                        "Selecting collections from multiple MSigDB versions may result in omitted genes "
                                + "and is not recommended.\n"
                                + "NOTE: another tab may have a selection.\n"
                                + FxFtpChooserSupport.DESELECT_INSTRUCTIONS
                                + "\n\nClick Cancel to change the selection or OK to keep it.");
                xapps.gsea.fx.FxTheme.apply(alert);
                Optional<ButtonType> choice = alert.showAndWait();
                if (choice.isEmpty() || choice.get() != ButtonType.OK) {
                    event.consume();
                }
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == null || btn.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
                return null;
            }
            List<String> all = new ArrayList<>();
            for (FTPFile f : humanList.getSelectionModel().getSelectedItems()) {
                all.add(f.getPath());
            }
            for (FTPFile f : mouseList.getSelectionModel().getSelectedItems()) {
                all.add(f.getPath());
            }
            for (CachedPathItem item : cachedGmx.getSelectionModel().getSelectedItems()) {
                all.add(item.path);
            }
            for (CachedPathItem item : cachedGrp.getSelectionModel().getSelectedItems()) {
                all.add(item.path);
            }
            for (CachedPathItem item : subsets.getSelectionModel().getSelectedItems()) {
                all.add(item.path);
            }

            String onTheFlyText = taGenes.getText();
            if (StringUtils.isNotBlank(onTheFlyText)) {
                String[] genes = ParseUtils.string2strings(onTheFlyText, "\t\n");
                if (genes.length != 0) {
                    GeneSet gset = new GeneSet("from_text_entry_", genes);
                    try {
                        File tmp = File.createTempFile(gset.getName(), ".grp");
                        ParserFactory.save(gset, tmp);
                        all.add(ParserFactory.getCache().getSourcePath(gset));
                        FxFileChooserUtil.registerOpened(tmp);
                    } catch (Throwable t) {
                        klog.error(t.getMessage(), t);
                    }
                }
            }
            if (all.isEmpty()) {
                return "";
            }
            return String.join(",", all);
        });

        return dialog.showAndWait().filter(StringUtils::isNotBlank);
    }

    private static void openLocalGeneSetFiles(
            Window owner,
            ListView<CachedPathItem> list,
            Class<?> type,
            List<FileChooser.ExtensionFilter> filters,
            boolean multiple) {
        FxFtpChooserSupport.browseLocalFiles(
                owner,
                "Open gene set file",
                filters,
                multiple,
                selectedCachedPath(list),
                files -> FxFtpChooserSupport.loadLocalFilesAsync(
                        files,
                        type,
                        loaded -> {
                            refreshCachedObjectList(list, type);
                            List<String> paths = new ArrayList<>();
                            for (PersistentObject obj : loaded) {
                                try {
                                    paths.add(ParserFactory.getCache().getSourcePath(obj));
                                } catch (Exception ignore) {
                                }
                            }
                            selectCachedPaths(list, paths);
                        }));
    }

    private static String selectedCachedPath(ListView<CachedPathItem> list) {
        CachedPathItem sel = list.getSelectionModel().getSelectedItem();
        return sel != null ? sel.path : null;
    }

    private static void selectCachedPaths(ListView<CachedPathItem> list, List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return;
        }
        for (CachedPathItem item : list.getItems()) {
            if (item != null && paths.contains(item.path)) {
                list.getSelectionModel().select(item);
            }
        }
    }

    private static List<Versioned> collectSelectedVersioned(
            ListView<FTPFile> humanList,
            ListView<FTPFile> mouseList,
            ListView<CachedPathItem> cachedGmx,
            ListView<CachedPathItem> cachedGrp,
            ListView<CachedPathItem> subsets) {
        List<Versioned> selected = new ArrayList<>();
        selected.addAll(humanList.getSelectionModel().getSelectedItems());
        selected.addAll(mouseList.getSelectionModel().getSelectedItems());
        for (CachedPathItem item : cachedGmx.getSelectionModel().getSelectedItems()) {
            if (item.source instanceof Versioned) {
                selected.add((Versioned) item.source);
            }
        }
        for (CachedPathItem item : cachedGrp.getSelectionModel().getSelectedItems()) {
            if (item.source instanceof Versioned) {
                selected.add((Versioned) item.source);
            }
        }
        for (CachedPathItem item : subsets.getSelectionModel().getSelectedItems()) {
            if (item.source instanceof Versioned) {
                selected.add((Versioned) item.source);
            }
        }
        return selected;
    }

    private static boolean speciesOk(List<Versioned> selectedItems) {
        List<Versioned> known = new ArrayList<>();
        for (Versioned item : selectedItems) {
            if (!item.getMSigDBVersion().isUnknownVersion()) {
                known.add(item);
            }
        }
        if (known.isEmpty()) {
            return true;
        }
        MSigDBVersion first = known.get(0).getMSigDBVersion();
        for (int i = 1; i < known.size(); i++) {
            if (first.getMsigDBSpecies() != known.get(i).getMSigDBVersion().getMsigDBSpecies()) {
                return false;
            }
        }
        return true;
    }

    /**
     * warn when known MSigDB versions disagree (unknowns and on-the-fly text soften "all known").
     */
    private static boolean versionsOk(List<Versioned> selectedItems, boolean hasOnTheFlyText) {
        if (selectedItems.isEmpty()) {
            return true;
        }
        List<Versioned> copy = new ArrayList<>(selectedItems);
        MSigDBVersion first = copy.remove(0).getMSigDBVersion();
        boolean allUnknown = first.isUnknownVersion();
        boolean allKnown = !first.isUnknownVersion();
        allKnown &= !hasOnTheFlyText;

        for (Versioned item : copy) {
            MSigDBVersion currVer = item.getMSigDBVersion();
            if (!currVer.isUnknownVersion()) {
                if (!first.equals(currVer)) {
                    return false;
                }
                allUnknown = false;
                allKnown &= true;
            } else {
                allUnknown &= true;
                allKnown = false;
            }
        }
        return allUnknown || allKnown;
    }

    private static ListView<CachedPathItem> cachedObjectList(Class<?> type) {
        ListView<CachedPathItem> list = new ListView<>();
        list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CachedPathItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }
                if (item.source instanceof PersistentObject pob) {
                    xapps.gsea.fx.FxPobListCells.applyPob(this, pob);
                } else {
                    setText(item.name);
                    setGraphic(null);
                    setTooltip(item.path != null ? new Tooltip(item.path) : null);
                }
            }
        });
        refreshCachedObjectList(list, type);
        return list;
    }

    private static void refreshCachedObjectList(ListView<CachedPathItem> list, Class<?> type) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> objs = ParserFactory.getCache().getCachedObjectsL(type);
            List<CachedPathItem> items = new ArrayList<>();
            for (Object o : objs) {
                try {
                    String path = ParserFactory.getCache().getSourcePath(o);
                    String name = o instanceof edu.mit.broad.genome.objects.PersistentObject
                            ? ((edu.mit.broad.genome.objects.PersistentObject) o).getName()
                            : String.valueOf(o);
                    items.add(new CachedPathItem(name, path, o));
                } catch (Exception ignore) {
                    // skip uncached
                }
            }
            // Preserve selection by path so periodic refresh does not clear local picks
            List<String> selectedPaths = new ArrayList<>();
            for (CachedPathItem sel : list.getSelectionModel().getSelectedItems()) {
                if (sel != null && sel.path != null) {
                    selectedPaths.add(sel.path);
                }
            }
            xapps.gsea.fx.FxSearchField.replaceItems(list, items);
            list.getSelectionModel().clearSelection();
            for (CachedPathItem item : list.getItems()) {
                if (item != null && selectedPaths.contains(item.path)) {
                    list.getSelectionModel().select(item);
                }
            }
        } catch (Exception e) {
            klog.debug("Could not list cached {}", type.getSimpleName(), e);
        }
    }

    private static ListView<CachedPathItem> auxGeneSetList() {
        ListView<CachedPathItem> list = new ListView<>();
        list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CachedPathItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }
                if (item.source instanceof PersistentObject pob) {
                    xapps.gsea.fx.FxPobListCells.applyPob(this, pob);
                } else {
                    setText(item.name);
                    setGraphic(null);
                    setTooltip(item.path != null ? new Tooltip(item.path) : null);
                }
            }
        });
        refreshAuxGeneSetList(list);
        return list;
    }

    private static void refreshAuxGeneSetList(ListView<CachedPathItem> list) {
        try {
            List<GeneSet> aux = ParserFactory.getCache().getAuxGeneSets();
            List<CachedPathItem> items = new ArrayList<>();
            for (GeneSet gset : aux) {
                try {
                    String path;
                    try {
                        path = ParserFactory.getCache().getSourcePath(gset);
                    } catch (IllegalArgumentException notCached) {
                        File tmp = File.createTempFile(gset.getName() + "_", ".grp");
                        ParserFactory.save(gset, tmp);
                        path = ParserFactory.getCache().getSourcePath(gset);
                    }
                    items.add(new CachedPathItem(gset.getName(true), path, gset));
                } catch (Exception ex) {
                    klog.debug("Skip aux gene set {}: {}", gset, ex.toString());
                }
            }
            List<String> selectedPaths = new ArrayList<>();
            for (CachedPathItem sel : list.getSelectionModel().getSelectedItems()) {
                if (sel != null && sel.path != null) {
                    selectedPaths.add(sel.path);
                }
            }
            xapps.gsea.fx.FxSearchField.replaceItems(list, items);
            list.getSelectionModel().clearSelection();
            for (CachedPathItem item : list.getItems()) {
                if (item != null && selectedPaths.contains(item.path)) {
                    list.getSelectionModel().select(item);
                }
            }
            if (items.isEmpty()) {
                list.setPlaceholder(new javafx.scene.control.Label(""));
            }
        } catch (Exception e) {
            klog.debug("Could not list aux gene sets", e);
        }
    }

    private static final class CachedPathItem {
        final String name;
        final String path;
        final Object source;

        CachedPathItem(String name, String path, Object source) {
            this.name = name;
            this.path = path;
            this.source = source;
        }

        @Override
        public String toString() {
            // Display handled by NonFTPGeneSetsRenderer-style cell factory.
            return name;
        }
    }

    private static TextArea messageArea(String text) {
        TextArea area = new TextArea(text);
        area.setEditable(false);
        area.setWrapText(true);
        return area;
    }
}
