/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.io.File;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import xapps.gsea.fx.widgets.FxButtons;
import xapps.gsea.fx.widgets.FxEllipsisButton;
import org.gsea_msigdb.gsea.runtime.AppServices;

/**
 * Shared Leading Edge / Enrichment Map load chrome: cache chooser XOR folder field.
 * Browse clears the cache selection to avoid sticky XOR conflicts.
 */
public final class FxGseaReportLoadUi {

    public final FxReportCacheChooser cacheChooser;
    public final TextField folderField = new TextField();
    public final VBox root;
    private File rememberedDir;
    private final Node windowOwner;

    public FxGseaReportLoadUi(FxReportCacheChooser cacheChooser, Node windowOwner, boolean verticalLabels,
            boolean includeClear, Runnable onLoad) {
        this.cacheChooser = cacheChooser;
        this.windowOwner = windowOwner;
        folderField.setEditable(true);
        HBox.setHgrow(folderField, Priority.ALWAYS);
        if (!folderField.getStyleClass().contains("gsea-dir-field")) {
            folderField.getStyleClass().add("gsea-dir-field");
        }
        FxPathFieldColors.attach(folderField);

        Button browse = FxEllipsisButton.create(
                "[ OR ] Locate a GSEA report folder from the file system");
        browse.setOnAction(e -> chooseFolder());

        Button load = new Button("Load GSEA Results");
        FxButtons.stylePrimary(load);
        if (onLoad != null) {
            load.setOnAction(e -> onLoad.run());
        }

        HBox.setHgrow(cacheChooser.getNode(), Priority.ALWAYS);
        Button clear = null;
        if (includeClear) {
            clear = new Button("Clear");
            FxButtons.styleSecondary(clear);
            clear.setOnAction(e -> {
                cacheChooser.clearSelection();
                folderField.clear();
                rememberedDir = null;
            });
        }

        if (verticalLabels) {
            HBox folderRow = new HBox(8, folderField, browse);
            HBox actions = clear != null ? FxButtons.row(clear, load) : FxButtons.row(load);
            root = new VBox(12,
                    new Label("Select a GSEA result from the application cache"),
                    cacheChooser.getNode(),
                    new Label("[ OR ] Locate a GSEA result folder from the file system"),
                    folderRow,
                    actions);
            root.setPadding(new Insets(16));
        } else {
            HBox cacheRow = new HBox(8,
                    new Label("Select a GSEA result from the application cache"),
                    cacheChooser.getNode());
            HBox folderRow = clear != null
                    ? FxButtons.row(
                            new Label("[ OR ] Locate a GSEA result folder from the file system"),
                            folderField, browse, clear, load)
                    : FxButtons.row(
                            new Label("[ OR ] Locate a GSEA result folder from the file system"),
                            folderField, browse, load);
            folderRow.setPadding(new Insets(0, 0, 4, 0));
            root = new VBox(8, cacheRow, folderRow);
            root.setPadding(new Insets(12, 12, 4, 12));
        }
    }

    public void chooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        FxFileChooserUtil.seedInitialDirectory(chooser, folderField.getText());
        File selected = chooser.showDialog(FxFileChooserUtil.windowOf(windowOwner));
        if (selected == null) {
            return;
        }
        setDirectory(selected);
    }

    /** Prefill folder and clear cache (avoids sticky XOR). */
    public void setDirectory(File dir) {
        if (dir == null) {
            return;
        }
        rememberedDir = dir;
        folderField.setText(dir.getAbsolutePath());
        cacheChooser.clearSelection();
        FxFileChooserUtil.registerOpenedDir(dir);
    }

    public File getRememberedDir() {
        return rememberedDir;
    }

    public void clear() {
        rememberedDir = null;
        folderField.clear();
        cacheChooser.clearSelection();
    }

    public FxGseaReportXor.SingleResult resolveSingle() {
        return FxGseaReportXor.resolveSingle(cacheChooser, folderField.getText(), rememberedDir);
    }

    public FxGseaReportXor.MultiResult resolveMulti() {
        return FxGseaReportXor.resolveMulti(cacheChooser, folderField.getText());
    }

    public void showXorMessage(FxGseaReportXor.Kind kind) {
        if (kind == FxGseaReportXor.Kind.CONFLICT) {
            AppServices.require().dialogs()
                    .showMessage(FxGseaReportXor.CONFLICT_MESSAGE);
        } else if (kind == FxGseaReportXor.Kind.EMPTY) {
            AppServices.require().dialogs()
                    .showMessage(FxGseaReportXor.EMPTY_MESSAGE);
        }
    }

    public void ifResolvedSingle(Consumer<File> consumer) {
        FxGseaReportXor.SingleResult r = resolveSingle();
        if (r.kind != FxGseaReportXor.Kind.RESOLVED) {
            showXorMessage(r.kind);
            return;
        }
        if (r.dir == null) {
            showXorMessage(FxGseaReportXor.Kind.EMPTY);
            return;
        }
        consumer.accept(r.dir);
    }
}
