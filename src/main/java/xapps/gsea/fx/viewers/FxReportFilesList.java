/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.gsea_msigdb.gsea.ui.api.ViewPage;

import javafx.collections.FXCollections;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import xapps.gsea.fx.FxFileTransferSupport;

/**
 * Report / results file list with : double-click open,
 * full registry-style context menu, and drag-out.
 */
public final class FxReportFilesList {

    private FxReportFilesList() {
    }

    public static ListView<File> create(File[] files, Consumer<ViewPage> openPage) {
        List<File> produced = new ArrayList<>();
        if (files != null) {
            produced.addAll(Arrays.asList(files));
        }
        ListView<File> list = new ListView<>(FXCollections.observableArrayList(produced));
        list.setPlaceholder(new Label(""));
        list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        if (!produced.isEmpty()) {
            list.getSelectionModel().select(0);
        }
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }
                setText(item.getAbsolutePath());
                setGraphic(xapps.gsea.fx.FxFileIcons.forFile(item));
                setTooltip(new javafx.scene.control.Tooltip(item.getAbsolutePath()));
            }
        });
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                File selected = list.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    FxFileActions.runDefaultFileAction(selected, openPage);
                }
            }
        });
        list.setOnContextMenuRequested(e -> {
            File selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            ContextMenu built = FxFileActions.fileContextMenu(selected, openPage, null);
            list.setContextMenu(built);
            built.show(list, e.getScreenX(), e.getScreenY());
        });
        list.setOnDragDetected(e -> {
            List<File> selected = new ArrayList<>();
            for (File f : list.getSelectionModel().getSelectedItems()) {
                if (f != null) {
                    selected.add(f);
                }
            }
            if (!selected.isEmpty()) {
                FxFileTransferSupport.startFileDrag(list, selected, e);
            }
        });
        return list;
    }
}
