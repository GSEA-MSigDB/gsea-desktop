/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.coremap;

import java.io.File;
import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import xapps.gsea.fx.FxButtons;
import xapps.gsea.fx.params.FxFileChooserUtil;
import xapps.gsea.fx.params.FxReportCacheChooser;
import xapps.gsea.fx.params.FxReportCacheSupport;

/** Mechanistic / phenotypic GSEA layer folder + cache picker. */
public final class CoreMapLayerLoadUi {

    public interface Host {
        javafx.scene.Node windowOwner();

        void setSuppressAutoParse(boolean suppress);

        void scheduleCatalogRefresh();

        void onLayerCleared(boolean mechanistic);
    }

    public final VBox root = new VBox(6);
    public final TextField folderField = new TextField();
    public final FxReportCacheChooser cacheChooser = FxReportCacheChooser.single();
    public final Button clearBtn = new Button("Clear");
    public final boolean mechanistic;
    private final Host host;
    private File dir;

    public CoreMapLayerLoadUi(String title, boolean mechanistic, Host host) {
        this.mechanistic = mechanistic;
        this.host = host;
        folderField.setEditable(true);
        if (!folderField.getStyleClass().contains("gsea-dir-field")) {
            folderField.getStyleClass().add("gsea-dir-field");
        }
        xapps.gsea.fx.params.FxPathFieldColors.attach(folderField);
        Button browse = xapps.gsea.fx.FxEllipsisButton.create("Browse " + title + " GSEA folder");
        browse.setOnAction(e -> chooseFolder());
        FxButtons.styleSecondary(clearBtn);
        clearBtn.setOnAction(e -> {
            host.setSuppressAutoParse(true);
            clear();
            host.setSuppressAutoParse(false);
            host.onLayerCleared(this.mechanistic);
        });
        folderField.textProperty().addListener((o, a, b) -> host.scheduleCatalogRefresh());
        cacheChooser.getTextField().textProperty().addListener((o, a, b) -> host.scheduleCatalogRefresh());
        HBox.setHgrow(folderField, Priority.ALWAYS);
        HBox.setHgrow(cacheChooser.getNode(), Priority.ALWAYS);
        HBox cacheRow = new HBox(8, new Label(title + " cache"), cacheChooser.getNode(), clearBtn);
        HBox dirRow = new HBox(8, new Label("or folder"), folderField, browse);
        root.getChildren().addAll(new Label(title + " layer"), cacheRow, dirRow);
        root.setPadding(new Insets(4));
        root.getStyleClass().add("coremap-layer-frame");
    }

    public void setDirectory(File d) {
        this.dir = d;
        folderField.setText(d.getAbsolutePath());
        cacheChooser.clearSelection();
        FxFileChooserUtil.registerOpenedDir(d);
    }

    public void clear() {
        dir = null;
        folderField.clear();
        cacheChooser.clearSelection();
    }

    public boolean hasSelection() {
        return cacheChooser.isSpecified()
                || (folderField.getText() != null && !folderField.getText().isBlank());
    }

    public void setDisable(boolean disable) {
        root.setDisable(disable);
    }

    public void chooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        FxFileChooserUtil.seedInitialDirectory(chooser, folderField.getText());
        File selected = chooser.showDialog(FxFileChooserUtil.windowOf(host.windowOwner()));
        if (selected != null) {
            setDirectory(selected);
        }
    }

    /** Resolve without showing conflict dialogs (for auto-parse / job snapshots). */
    public File resolveDirQuiet() {
        boolean cacheSpecified = cacheChooser.isSpecified();
        boolean dirSpecified = folderField.getText() != null && !folderField.getText().isBlank();
        if (cacheSpecified && dirSpecified) {
            return null;
        }
        if (!cacheSpecified && !dirSpecified) {
            return null;
        }
        if (cacheSpecified) {
            FxReportCacheSupport.CachedReport sel = cacheChooser.getSelectedOne();
            if (sel != null) {
                return sel.edbDir;
            }
            List<File> dirs = cacheChooser.getReportDirs();
            if (dirs.isEmpty()) {
                return null;
            }
            File d = dirs.get(0);
            File nested = new File(d, "edb");
            return nested.isDirectory() ? nested : d;
        }
        String path = folderField.getText().trim();
        return dir != null && dir.getAbsolutePath().equals(path) ? dir : new File(path);
    }
}
