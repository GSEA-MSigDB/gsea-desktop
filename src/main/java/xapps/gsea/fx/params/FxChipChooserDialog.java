/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.objects.MSigDBCatalogFile;
import edu.mit.broad.genome.objects.MSigDBRelease;
import edu.mit.broad.genome.objects.MSigDBSpecies;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import xapps.gsea.GseaWebResources;
import org.gsea_msigdb.gsea.runtime.AppServices;

/**
 * JavaFX chip chooser: Human/Mouse MSigDB catalog chips or a single local .chip file.
 */
public final class FxChipChooserDialog {
    private static final Logger klog = LoggerFactory.getLogger(FxChipChooserDialog.class);

    private FxChipChooserDialog() {
    }

    /**
     * @return selected chip path, or empty if cancelled
     */
    public static Optional<String> show(Window owner) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Select a chip");
        dialog.initOwner(owner);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResizable(true);
        FxChooserSupport.addHelpAndInfoButtons(dialog, "#chip",
                "MSigDB Chips", GseaWebResources.getGseaChipInfoHelpURL());
        final Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        TreeView<Object> humanTree = new TreeView<>();
        TreeView<Object> mouseTree = new TreeView<>();

        ListView<CachedChip> cachedChips = new ListView<>();
        cachedChips.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        cachedChips.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CachedChip item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                setText(item.name);
                setTooltip(item.path != null && !item.path.isBlank()
                        ? new Tooltip(item.path) : null);
            }
        });
        refreshCachedChips(cachedChips);
        FxChooserSupport.enableDoubleClickToFire(cachedChips, okButton);
        tabs.getTabs().add(new Tab("Local Chips",
                FxChooserSupport.wrapLocalTab(
                        xapps.gsea.fx.widgets.FxSearchField.wrapList(cachedChips,
                                c -> c == null ? "" : c.name + " " + (c.path != null ? c.path : "")),
                        () -> openLocalChipFile(owner, cachedChips))));

        if (FxChooserSupport.isOnline()) {
            humanTree = FxChooserSupport.installDeferredCatalogTab(
                    tabs,
                    "Human Collection Chips (MSigDB)",
                    okButton,
                    MSigDBSpecies.Human,
                    MSigDBRelease::getChipCatalogUrl,
                    false);
            mouseTree = FxChooserSupport.installDeferredCatalogTab(
                    tabs,
                    "Mouse Collection Chips (MSigDB)",
                    okButton,
                    MSigDBSpecies.Mouse,
                    MSigDBRelease::getChipCatalogUrl,
                    false);
        } else {
            tabs.getTabs().add(new Tab("Human Collection Chips (MSigDB)",
                    FxChooserSupport.messageArea(FxChooserSupport.OFFLINE_MESSAGE)));
            tabs.getTabs().add(new Tab("Mouse Collection Chips (MSigDB)",
                    FxChooserSupport.messageArea(FxChooserSupport.OFFLINE_MESSAGE)));
        }

        final TreeView<Object> humanTreeFinal = humanTree;
        final TreeView<Object> mouseTreeFinal = mouseTree;

        tabs.getSelectionModel().selectedItemProperty().addListener((obs, o, tab) -> {
            if (tab != null && "Local Chips".equals(tab.getText())) {
                refreshCachedChips(cachedChips);
            }
        });
        javafx.animation.Timeline chipRefresh = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), ev -> {
                    Tab sel = tabs.getSelectionModel().getSelectedItem();
                    if (sel != null && "Local Chips".equals(sel.getText())) {
                        refreshCachedChips(cachedChips);
                    }
                }));
        chipRefresh.setCycleCount(javafx.animation.Animation.INDEFINITE);
        dialog.setOnShown(e -> chipRefresh.play());
        dialog.setOnHidden(e -> chipRefresh.stop());

        tabs.getSelectionModel().select(0);

        BorderPane root = new BorderPane(tabs);
        root.setPadding(new Insets(8));
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setPrefSize(800, 400);
        xapps.gsea.fx.FxTheme.apply(dialog);
        xapps.gsea.fx.widgets.FxButtons.stylePrimary(okButton);
        xapps.gsea.fx.widgets.FxButtons.styleSecondary(
                (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL));

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            boolean haveHuman = !FxChooserSupport.getSelectedCatalogFiles(humanTreeFinal).isEmpty();
            boolean haveMouse = !FxChooserSupport.getSelectedCatalogFiles(mouseTreeFinal).isEmpty();
            boolean haveCached = cachedChips.getSelectionModel().getSelectedItem() != null;
            int count = (haveHuman ? 1 : 0) + (haveMouse ? 1 : 0) + (haveCached ? 1 : 0);
            if (count > 1) {
                FxChooserSupport.showMultiSelectionInfo(
                        dialog.getDialogPane().getScene().getWindow(),
                        "Multiple CHIPs selected",
                        "Multiple CHIP selections are not allowed.");
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == null || btn.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
                return null;
            }
            List<MSigDBCatalogFile> humanSel = FxChooserSupport.getSelectedCatalogFiles(humanTreeFinal);
            if (!humanSel.isEmpty()) {
                return humanSel.get(0).getPath();
            }
            List<MSigDBCatalogFile> mouseSel = FxChooserSupport.getSelectedCatalogFiles(mouseTreeFinal);
            if (!mouseSel.isEmpty()) {
                return mouseSel.get(0).getPath();
            }
            CachedChip cached = cachedChips.getSelectionModel().getSelectedItem();
            if (cached != null) {
                return cached.path;
            }
            return "";
        });

        return dialog.showAndWait().filter(StringUtils::isNotBlank);
    }

    private static void openLocalChipFile(Window owner, ListView<CachedChip> cachedChips) {
        FxChooserSupport.browseLocalFiles(
                owner,
                "Open chip file",
                FxChooserSupport.chipFileFilters(),
                false,
                selectedChipPath(cachedChips),
                files -> FxChooserSupport.loadLocalFilesAsync(
                        files,
                        edu.mit.broad.vdb.chip.Chip.class,
                        loaded -> {
                            refreshCachedChips(cachedChips);
                            try {
                                selectCachedChipByPath(cachedChips,
                                        AppServices.require().cache().getSourcePath(loaded.get(0)));
                            } catch (Exception ignore) {
                            }
                        }));
    }

    private static String selectedChipPath(ListView<CachedChip> cachedChips) {
        CachedChip sel = cachedChips.getSelectionModel().getSelectedItem();
        return sel != null ? sel.path : null;
    }

    private static void selectCachedChipByPath(ListView<CachedChip> cachedChips, String path) {
        if (path == null) {
            return;
        }
        for (CachedChip item : cachedChips.getItems()) {
            if (item != null && path.equals(item.path)) {
                cachedChips.getSelectionModel().select(item);
                return;
            }
        }
    }

    private static void refreshCachedChips(ListView<CachedChip> cachedChips) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> chips = AppServices.require().cache().getCachedObjectsL(edu.mit.broad.vdb.chip.Chip.class);
            List<CachedChip> items = new ArrayList<>();
            for (Object o : chips) {
                try {
                    String path = AppServices.require().cache().getSourcePath(o);
                    String name = o instanceof edu.mit.broad.genome.objects.PersistentObject
                            ? ((edu.mit.broad.genome.objects.PersistentObject) o).getName()
                            : String.valueOf(o);
                    items.add(new CachedChip(name, path));
                } catch (Exception ignore) {
                }
            }
            String selectedPath = null;
            CachedChip sel = cachedChips.getSelectionModel().getSelectedItem();
            if (sel != null) {
                selectedPath = sel.path;
            }
            xapps.gsea.fx.widgets.FxSearchField.replaceItems(cachedChips, items);
            cachedChips.getSelectionModel().clearSelection();
            if (selectedPath != null) {
                for (CachedChip item : cachedChips.getItems()) {
                    if (item != null && selectedPath.equals(item.path)) {
                        cachedChips.getSelectionModel().select(item);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            klog.debug("Could not list cached chips", e);
        }
    }

    private static final class CachedChip {
        final String name;
        final String path;

        CachedChip(String name, String path) {
            this.name = name;
            this.path = path;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
