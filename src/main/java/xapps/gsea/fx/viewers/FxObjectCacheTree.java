/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.gsea_msigdb.gsea.ui.api.ViewPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.GeneSetMatrix;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.objects.SampleAnnot;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import edu.mit.broad.genome.parsers.ObjectCache;
import edu.mit.broad.genome.parsers.ParseUtils;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.vdb.chip.Chip;
import javafx.geometry.Insets;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.gsea_msigdb.gsea.runtime.AppServices;
import org.gsea_msigdb.gsea.ui.api.FeatureHost;

/**
 * Tree of objects currently held in the workspace object cache, organized by type.
 */
public class FxObjectCacheTree {

    private static final Logger klog = LoggerFactory.getLogger(FxObjectCacheTree.class);

    private final BorderPane root = new BorderPane();
    private final TreeView<CacheNode> treeView = new TreeView<>();
    private final FeatureHost host;
    private final Consumer<ViewPage> openPage;
    private final AppServices svc;
    private String filterQuery = "";

    public FxObjectCacheTree(FeatureHost host) {
        this.host = java.util.Objects.requireNonNull(host, "host");
        this.openPage = host::openPage;
        this.svc = host.services();

        Label header = new Label("Object cache\n(objects already loaded & ready for use)");
        header.getStyleClass().add("gsea-section-header");
        header.setPadding(new Insets(4, 0, 4, 0));

        javafx.scene.control.TextField filterField = xapps.gsea.fx.widgets.FxSearchField.bind(q -> {
            filterQuery = q;
            refresh();
        });
        filterField.setPromptText("Filter objects…");

        treeView.setShowRoot(true);
        treeView.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(CacheNode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }
                javafx.scene.image.ImageView icon = null;
                if (item.payload instanceof PersistentObject) {
                    icon = xapps.gsea.fx.widgets.FxFileIcons.forObject(item.payload);
                    File src = sourceFileOf(item.payload);
                    setTooltip(new Tooltip(src != null ? src.getAbsolutePath() : "Unknown path for object"));
                } else if (item.payload == null && item.label != null
                        && !item.label.startsWith("Objects in memory")) {
                    // Do not invent Open16.gif; text-only matches "renderer did not set icon".
                    icon = null;
                    setTooltip(null);
                } else {
                    setTooltip(null);
                }
                if (item.quickInfo != null && !item.quickInfo.isBlank()) {
                    Label name = new Label(item.label);
                    Label qi = new Label(" [" + item.quickInfo + "]");
                    qi.getStyleClass().add("gsea-muted");
                    HBox row = new HBox(0, name, qi);
                    if (icon != null) {
                        setGraphic(new HBox(4, icon, row));
                    } else {
                        setGraphic(row);
                    }
                    setText(null);
                } else {
                    setText(item.label);
                    setGraphic(icon);
                }
            }
        });

        treeView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.isShiftDown()) {
                TreeItem<CacheNode> sel = treeView.getSelectionModel().getSelectedItem();
                if (sel != null && sel == treeView.getRoot()) {
                    expandAll();
                    e.consume();
                    return;
                }
            }
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                openSelected();
                e.consume();
            }
        });

        try {
            svc.cache().addPathAdditionsListener(evt ->
                    javafx.application.Platform.runLater(this::refresh));
        } catch (Throwable t) {
            klog.debug("Could not attach cache path listener", t);
        }

        treeView.setOnContextMenuRequested(e -> {
            Object sel = selectedObject();
            if (!(sel instanceof PersistentObject persistent)) {
                treeView.setContextMenu(null);
                return;
            }
            ContextMenu built = FxFileActions.objectContextMenu(persistent, openPage, this::refresh);
            treeView.setContextMenu(built);
            built.show(treeView, e.getScreenX(), e.getScreenY());
            e.consume();
        });
        treeView.setContextMenu(null);

        treeView.setOnDragDetected(e -> {
            List<PersistentObject> pobs = selectedPobs();
            if (!pobs.isEmpty()) {
                xapps.gsea.fx.widgets.FxPobTransferSupport.startPobDrag(treeView, pobs, e);
            }
        });
        treeView.setOnDragDone(e -> xapps.gsea.fx.widgets.FxPobTransferSupport.clear());
        treeView.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == javafx.scene.input.KeyCode.C) {
                copySelectedFiles();
                e.consume();
            }
        });

        VBox.setVgrow(treeView, Priority.ALWAYS);
        treeView.getStyleClass().add("gsea-panel-border");
        Label borderHint = new Label("Double-click to view · right-click for more options");
        borderHint.setMaxWidth(Double.MAX_VALUE);
        borderHint.setAlignment(javafx.geometry.Pos.CENTER);
        borderHint.getStyleClass().add("gsea-muted");
        borderHint.setStyle("-fx-font-size: 10px; -fx-font-family: Helvetica;");
        VBox framed = new VBox(0, borderHint, treeView);
        framed.getStyleClass().add("gsea-panel-border");
        VBox.setVgrow(treeView, Priority.ALWAYS);
        VBox box = new VBox(4, header, filterField, framed);
        VBox.setVgrow(framed, Priority.ALWAYS);
        root.setCenter(box);
        root.setPadding(new Insets(4, 8, 8, 8));

        refresh();
    }

    public BorderPane getNode() {
        return root;
    }

    public void refresh() {
        List<Object> previouslySelected = new ArrayList<>();
        for (TreeItem<CacheNode> item : new ArrayList<>(treeView.getSelectionModel().getSelectedItems())) {
            if (item != null && item.getValue() != null && item.getValue().payload != null) {
                previouslySelected.add(item.getValue().payload);
            }
        }
        java.util.Set<String> expandedCategories = new java.util.HashSet<>();
        TreeItem<CacheNode> oldRoot = treeView.getRoot();
        if (oldRoot != null) {
            for (TreeItem<CacheNode> cat : oldRoot.getChildren()) {
                if (cat.isExpanded() && cat.getValue() != null && cat.getValue().label != null) {
                    expandedCategories.add(cat.getValue().label);
                }
            }
        }

        TreeItem<CacheNode> rootItem = new TreeItem<>(new CacheNode(
                "Objects in memory [shift-click to expand all]", null));
        rootItem.setExpanded(true);

        ObjectCache cache = svc.cache();
        for (Class<?> cl : cache.getCachedRepresentationClasses()) {
            @SuppressWarnings("unchecked")
            List<Object> objs = cache.getCachedObjectsL(cl);
            if (objs == null || objs.isEmpty()) {
                continue;
            }
            addCategory(rootItem, categoryLabelFor(cl), cl, cache, expandedCategories);
        }

        treeView.setRoot(rootItem);
        if (filterQuery != null && !filterQuery.isEmpty()) {
            expandAll();
        }
        if (!previouslySelected.isEmpty()) {
            treeView.getSelectionModel().clearSelection();
            for (Object payload : previouslySelected) {
                TreeItem<CacheNode> match = findPayload(rootItem, payload);
                if (match != null) {
                    treeView.getSelectionModel().select(match);
                }
            }
        }
    }

    private static TreeItem<CacheNode> findPayload(TreeItem<CacheNode> parent, Object payload) {
        if (parent == null || payload == null) {
            return null;
        }
        for (TreeItem<CacheNode> child : parent.getChildren()) {
            if (child.getValue() != null && payload.equals(child.getValue().payload)) {
                return child;
            }
            TreeItem<CacheNode> nested = findPayload(child, payload);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private static String categoryLabelFor(Class<?> cl) {
        return friendlyCategoryLabel(cl);
    }

    private static String friendlyCategoryLabel(Class<?> cl) {
        String txt = cl.getSimpleName();
        if ("FSet".equals(txt)) {
            return "GeneSet"; // gray [grp] attached via CacheNode.quickInfo in addCategory
        }
        if ("GeneSet".equalsIgnoreCase(txt)) {
            return "Gene sets";
        }
        if ("ErrorPob".equalsIgnoreCase(txt)) {
            return "Errors";
        }
        if ("GeneSetMatrix".equalsIgnoreCase(txt)) {
            return "Gene set databases";
        }
        if ("SampleAnnot".equalsIgnoreCase(txt)) {
            return "Sample annotations";
        }
        if ("GenesOfInterest".equalsIgnoreCase(txt)) {
            return "Genes of interest";
        }
        if ("Dataset".equalsIgnoreCase(txt)) {
            return "Datasets";
        }
        if ("EnrichmentDb".equalsIgnoreCase(txt)) {
            return "Enrichment results";
        }
        if ("Template".equalsIgnoreCase(txt)) {
            return "Phenotypes";
        }
        if ("RankedList".equalsIgnoreCase(txt)) {
            return "RankedGeneList";
        }
        return txt;
    }

    private void expandAll() {
        TreeItem<CacheNode> rootItem = treeView.getRoot();
        if (rootItem == null) {
            return;
        }
        expandRecursive(rootItem);
    }

    private static void expandRecursive(TreeItem<CacheNode> item) {
        item.setExpanded(true);
        for (TreeItem<CacheNode> child : item.getChildren()) {
            expandRecursive(child);
        }
    }

    private void addCategory(TreeItem<CacheNode> parent, String label, Class<?> type,
            ObjectCache cache, java.util.Set<String> expandedCategories) {
        @SuppressWarnings("unchecked")
        List<Object> objs = cache.getCachedObjectsL(type);
        String quickInfo = "FSet".equals(type.getSimpleName()) ? "grp" : null;
        TreeItem<CacheNode> cat = new TreeItem<>(new CacheNode(label, null, quickInfo));
        // Preserve prior expansion when known; otherwise expand nonempty categories (first paint).
        if (expandedCategories != null) {
            cat.setExpanded(expandedCategories.contains(label));
        } else {
            cat.setExpanded(false);
        }
        boolean categoryLabelMatch = xapps.gsea.fx.widgets.FxSearchField.matches(label, filterQuery);
        for (Object obj : objs) {
            CacheNode leaf = leafNode(obj);
            if (filterQuery != null && !filterQuery.isEmpty() && !categoryLabelMatch) {
                String hay = leaf.label;
                if (leaf.quickInfo != null) {
                    hay = hay + " " + leaf.quickInfo;
                }
                File src = sourceFileOf(obj);
                if (src != null) {
                    hay = hay + " " + src.getAbsolutePath();
                }
                if (!xapps.gsea.fx.widgets.FxSearchField.matches(hay, filterQuery)) {
                    continue;
                }
            }
            cat.getChildren().add(new TreeItem<>(leaf));
        }
        if (cat.getChildren().isEmpty() && filterQuery != null && !filterQuery.isEmpty()
                && !categoryLabelMatch) {
            return;
        }
        parent.getChildren().add(cat);
    }

    private static CacheNode leafNode(Object obj) {
        if (!(obj instanceof PersistentObject pob)) {
            return new CacheNode(String.valueOf(obj), obj, null);
        }
        String name = pob.getName() != null ? pob.getName() : String.valueOf(obj);
        String qi = pob.getQuickInfo();
        if (qi != null && qi.isBlank()) {
            qi = null;
        }
        return new CacheNode(name, obj, qi);
    }

    private Object selectedObject() {
        TreeItem<CacheNode> sel = treeView.getSelectionModel().getSelectedItem();
        return sel != null && sel.getValue() != null ? sel.getValue().payload : null;
    }

    private void openSelected() {
        Object pob = selectedObject();
        if (pob == null) {
            return;
        }
        try {
            if (pob instanceof EnrichmentDb edb) {
                File dir = edb.getEdbDir();
                FxLeadingEdgePane pane = new FxLeadingEdgePane(host);
                if (dir != null && dir.isDirectory()) {
                    pane.loadFromDirectory(dir);
                }
                openPage.accept(pane);
                return;
            }
            if (pob instanceof SampleAnnot) {
                svc.dialogs().showMessage(
                        "Sample annotations",
                        "No dedicated viewer for sample annotations.\n"
                                + ((PersistentObject) pob).getName());
                return;
            }
            if (pob instanceof PersistentObject persistent) {
                FxFileActions.viewObject(persistent, host);
            }
        } catch (Exception ex) {
            klog.error("Could not open viewer for {}", pob, ex);
            svc.dialogs().showError("Could not open viewer", ex);
        }
    }

    /** Re-parse the object's source file, bypassing the object cache. */
    private void forceReloadSelected() {
        Object pob = selectedObject();
        File src = sourceFileOf(pob);
        if (src == null || !src.isFile()) {
            svc.dialogs().showError("No source file available to force-reload.");
            return;
        }
        final File file = src;
        Thread worker = new Thread(() -> {
            try {
                ParserFactory.read(file, false);
                svc.files().getRecentFilesStore().refresh(file.getPath());
                javafx.application.Platform.runLater(() -> {
                    refresh();
                    svc.dialogs().showMessage(
                            "<html><body><b>Successfully reloaded: "
                                    + (pob instanceof PersistentObject
                                    ? ((PersistentObject) pob).getName()
                                    : file.getName())
                                    + "</b><br>From file: " + file + "</body></html>");
                });
            } catch (Throwable t) {
                klog.error("Force reload failed for {}", file, t);
                javafx.application.Platform.runLater(() ->
                        svc.dialogs().showError("Could not force-reload file", t));
            }
        }, "gsea-cache-force-reload");
        worker.setDaemon(true);
        worker.start();
    }

    private List<PersistentObject> selectedPobs() {
        List<PersistentObject> pobs = new ArrayList<>();
        for (TreeItem<CacheNode> item : treeView.getSelectionModel().getSelectedItems()) {
            if (item == null || item.getValue() == null) {
                continue;
            }
            if (item.getValue().payload instanceof PersistentObject pob) {
                pobs.add(pob);
            }
        }
        return pobs;
    }

    private List<File> selectedSourceFiles() {
        List<File> files = new ArrayList<>();
        for (TreeItem<CacheNode> item : treeView.getSelectionModel().getSelectedItems()) {
            if (item == null || item.getValue() == null) {
                continue;
            }
            File src = sourceFileOf(item.getValue().payload);
            if (src != null) {
                files.add(src);
            }
        }
        return files;
    }

    private void copySelectedFiles() {
        List<File> files = selectedSourceFiles();
        xapps.gsea.fx.widgets.FxFileTransferSupport.copyFilesToClipboard(files);
    }

    private void revealSelected() {
        Object pob = selectedObject();
        if (pob == null) {
            return;
        }
        try {
            ObjectCache cache = svc.cache();
            File src = cache.getSourceFile(pob);
            if (src == null) {
                svc.dialogs().showError("No source path for this object");
                return;
            }
            File parent = src.isDirectory() ? src : src.getParentFile();
            if (parent != null && parent.exists()) {
                xapps.gsea.fx.FxDesktopUtil.openFile(parent);
            } else if (src.exists()) {
                xapps.gsea.fx.FxDesktopUtil.openFile(src);
            } else {
                svc.dialogs().showError("Path does not exist:\n" + src.getAbsolutePath());
            }
        } catch (Exception ex) {
            klog.error("Reveal failed", ex);
            svc.dialogs().showError("Could not reveal in OS", ex);
        }
    }

    private void copySelectedPath() {
        Object pob = selectedObject();
        if (pob == null) {
            return;
        }
        try {
            String path = svc.cache().getSourcePath(pob);
            ClipboardContent content = new ClipboardContent();
            content.putString(path);
            Clipboard.getSystemClipboard().setContent(content);
        } catch (Exception ex) {
            svc.dialogs().showError("Could not copy path", ex);
        }
    }

    private File sourceFileOf(Object pob) {
        try {
            return svc.cache().getSourceFile(pob);
        } catch (Exception ex) {
            return null;
        }
    }

    private void removeGeneSetDuplicates() {
        Object pob = selectedObject();
        if (!(pob instanceof GeneSet)) {
            return;
        }
        File src = sourceFileOf(pob);
        if (src == null || !src.getName().toLowerCase().endsWith(".grp")) {
            svc.dialogs().showError(
                    "Only .grp files allowed - cannot perform this action on: "
                            + (src != null ? src.getName() : "(no source path)"));
            return;
        }
        try {
            int before = ParseUtils.countLines(src, true);
            GeneSet gset = (GeneSet) ParserFactory.read(src);
            ParserFactory.save(gset, src);
            refresh();
            svc.dialogs().showMessage(
                    "Successfully removed duplicates from the GeneSet. Before: "
                            + before + " after: " + gset.getNumMembers());
        } catch (Throwable t) {
            klog.error("Remove duplicates failed", t);
            svc.dialogs().showError("Error removing duplicates from GeneSet", t);
        }
    }

    private void extractGeneSetsFromMatrix() {
        Object pob = selectedObject();
        if (!(pob instanceof GeneSetMatrix)) {
            return;
        }
        try {
            GeneSetMatrix gm = (GeneSetMatrix) pob;
            ParserFactory.extractGeneSets(gm);
            refresh();
            svc.dialogs().showMessage(
                    "Successfully created " + gm.getNumGeneSets()
                            + " GeneSets from the GeneSetMatrix " + gm.getName());
        } catch (Throwable t) {
            klog.error("Extract GeneSets failed", t);
            svc.dialogs().showError("Error creating GeneSets from GeneSetMatrix", t);
        }
    }

    private void convertMatrixToGeneSet() {
        Object pob = selectedObject();
        if (!(pob instanceof GeneSetMatrix)) {
            return;
        }
        try {
            GeneSetMatrix gm = (GeneSetMatrix) pob;
            GeneSet gset = ParserFactory.combineIntoOne(gm);
            File tmp = new File(svc.vdb().getTmpDir(), gset.getName(true));
            ParserFactory.save(gset, tmp);
            refresh();
            svc.dialogs().showMessage(
                    "Successfully created a GeneSet from the GeneSetMatrix " + gm.getName()
                            + " into: " + tmp.getPath());
        } catch (Throwable t) {
            klog.error("Convert to GeneSet failed", t);
            svc.dialogs().showError("Error creating a GeneSet from GeneSetMatrix", t);
        }
    }

    private static final class CacheNode {
        final String label;
        final Object payload;
        final String quickInfo;

        CacheNode(String label, Object payload) {
            this(label, payload, null);
        }

        CacheNode(String label, Object payload, String quickInfo) {
            this.label = label;
            this.payload = payload;
            this.quickInfo = quickInfo;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
