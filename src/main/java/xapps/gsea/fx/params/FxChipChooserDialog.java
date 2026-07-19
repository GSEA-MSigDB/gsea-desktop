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
import edu.mit.broad.genome.objects.MSigDBSpecies;
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
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import xapps.gsea.GseaWebResources;

/**
 * JavaFX chip chooser: Human/Mouse MSigDB FTP chips or a single local .chip file.
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
        FxFtpChooserSupport.addHelpAndInfoButtons(dialog, "#chip",
                "MSigDB Chips", GseaWebResources.getGseaChipInfoHelpURL());
        final Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        ListView<FTPFile> humanList = new ListView<>();
        humanList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        ListView<FTPFile> mouseList = new ListView<>();
        mouseList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        ListView<CachedChip> cachedChips = new ListView<>();
        cachedChips.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        // Swing Local Chips: Chip.toString() = name; path not shown in cell text.
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
        FxFtpChooserSupport.enableDoubleClickToFire(cachedChips, okButton);
        tabs.getTabs().add(new Tab("Local Chips",
                xapps.gsea.fx.FxSearchField.wrapList(cachedChips,
                        c -> c == null ? "" : c.name + " " + (c.path != null ? c.path : ""))));

        if (FxFtpChooserSupport.isOnline()) {
            try {
                ComparatorFactory.FTPFileByVersionComparator humanCmp =
                        new ComparatorFactory.FTPFileByVersionComparator();
                List<FTPFile> human = FxFtpChooserSupport.listAndSort(".chip", MSigDBSpecies.Human,
                        GseaWebResources.getGseaFTPServerChipDir(MSigDBSpecies.Human),
                        humanCmp);
                humanList.setItems(FXCollections.observableArrayList(human));
                FxFtpChooserSupport.applyLatestVersionBolding(humanList, humanCmp);
                FxFtpChooserSupport.enableDoubleClickToFire(humanList, okButton);
                tabs.getTabs().add(new Tab("Human Collection Chips (MSigDB)",
                        xapps.gsea.fx.FxSearchField.wrapList(humanList,
                                f -> f == null ? "" : f.getName() + " " + f.getPath())));

                ComparatorFactory.FTPFileByVersionComparator mouseCmp =
                        new ComparatorFactory.FTPFileByVersionComparator("Mouse");
                List<FTPFile> mouse = FxFtpChooserSupport.listAndSort(".chip", MSigDBSpecies.Mouse,
                        GseaWebResources.getGseaFTPServerChipDir(MSigDBSpecies.Mouse),
                        mouseCmp);
                mouseList.setItems(FXCollections.observableArrayList(mouse));
                FxFtpChooserSupport.applyLatestVersionBolding(mouseList, mouseCmp);
                FxFtpChooserSupport.enableDoubleClickToFire(mouseList, okButton);
                tabs.getTabs().add(new Tab("Mouse Collection Chips (MSigDB)",
                        xapps.gsea.fx.FxSearchField.wrapList(mouseList,
                                f -> f == null ? "" : f.getName() + " " + f.getPath())));
            } catch (Exception ex) {
                klog.error(ex.getMessage(), ex);
                tabs.getTabs().add(new Tab("Human Collection Chips (MSigDB)",
                        messageArea(FxFtpChooserSupport.errorListingMessage(ex))));
                tabs.getTabs().add(new Tab("Mouse Collection Chips (MSigDB)",
                        messageArea(FxFtpChooserSupport.errorListingMessage(ex))));
            }
        } else {
            tabs.getTabs().add(new Tab("Human Collection Chips (MSigDB)",
                    messageArea(FxFtpChooserSupport.OFFLINE_MESSAGE)));
            tabs.getTabs().add(new Tab("Mouse Collection Chips (MSigDB)",
                    messageArea(FxFtpChooserSupport.OFFLINE_MESSAGE)));
        }

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

        // Swing: cross-tab multi-selection is preserved until OK validation (below).

        BorderPane root = new BorderPane(tabs);
        root.setPadding(new Insets(8));
        dialog.getDialogPane().setContent(root);
        // Swing WChipChooserWindow → DialogDescriptor.setDisplayWider() → DD_SIZE_WIDER.
        dialog.getDialogPane().setPrefSize(800, 400);
        xapps.gsea.fx.FxTheme.apply(dialog);
        xapps.gsea.fx.FxButtons.stylePrimary(okButton);
        xapps.gsea.fx.FxButtons.styleSecondary(
                (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL));

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            boolean haveHuman = humanList.getSelectionModel().getSelectedItem() != null;
            boolean haveMouse = mouseList.getSelectionModel().getSelectedItem() != null;
            boolean haveCached = cachedChips.getSelectionModel().getSelectedItem() != null;
            int count = (haveHuman ? 1 : 0) + (haveMouse ? 1 : 0) + (haveCached ? 1 : 0);
            if (count > 1) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.initOwner(dialog.getDialogPane().getScene().getWindow());
                alert.setTitle("Multiple CHIPs selected");
                alert.setHeaderText("Multiple CHIP selections are not allowed.");
                alert.setContentText("Is there a selection on another tab?\n" + FxFtpChooserSupport.DESELECT_INSTRUCTIONS);
                xapps.gsea.fx.FxTheme.apply(alert);
                alert.showAndWait();
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == null || btn.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
                return null;
            }
            FTPFile selected = humanList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                return selected.getPath();
            }
            selected = mouseList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                return selected.getPath();
            }
            CachedChip cached = cachedChips.getSelectionModel().getSelectedItem();
            if (cached != null) {
                return cached.path;
            }
            return "";
        });

        return dialog.showAndWait().filter(StringUtils::isNotBlank);
    }

    private static void refreshCachedChips(ListView<CachedChip> cachedChips) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> chips = ParserFactory.getCache().getCachedObjectsL(edu.mit.broad.vdb.chip.Chip.class);
            List<CachedChip> items = new ArrayList<>();
            for (Object o : chips) {
                try {
                    String path = ParserFactory.getCache().getSourcePath(o);
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
            xapps.gsea.fx.FxSearchField.replaceItems(cachedChips, items);
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
            // Swing Chip.toString / default JList: name only (path via tooltip in cell factory).
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
