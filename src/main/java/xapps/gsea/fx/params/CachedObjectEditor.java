/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.gsea_msigdb.gsea.runtime.AppServices;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import xtools.api.param.Param;

/**
 * HBox with a ComboBox listing cached objects of the requested Pob class (Dataset,
 * RankedList..).
 */
final class CachedObjectEditor extends AbstractBoundEditor {
    private final ComboBox<Object> combo = new ComboBox<>();
    private final HBox box = new HBox(6);
    private final Class<?> pobClass;
    private final List<FileChooser.ExtensionFilter> filters;
    private Object fallbackValue;

    CachedObjectEditor(Param param, Class<?> pobClass, List<FileChooser.ExtensionFilter> filters) {
        super(param);
        this.pobClass = pobClass;
        this.filters = filters;

        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setCellFactory(lv -> xapps.gsea.fx.widgets.FxPobListCells.pobNameQuickInfoCell());
        combo.setButtonCell(xapps.gsea.fx.widgets.FxPobListCells.pobNameQuickInfoCell());
        refreshItems();

        applyInitialValue(ParamEditorSupport.unsetIfEmptyArray(param.getValue()), true);

        HBox.setHgrow(combo, Priority.ALWAYS);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(combo);
        if (filters != null && !filters.isEmpty()) {
            Button browse = xapps.gsea.fx.widgets.FxEllipsisButton.create("Browse");
            browse.setOnAction(e -> browseForFile());
            box.getChildren().add(browse);
        }
        combo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> fireChange());
        combo.setOnShowing(e -> refreshItems());
        try {
            AppServices.require().cache().addPathAdditionsListener(evt ->
                    javafx.application.Platform.runLater(this::refreshItems));
        } catch (Throwable ignored) {
        }
    }

    private void browseForFile() {
        String seed = null;
        Object sel = combo.getSelectionModel().getSelectedItem();
        if (sel != null) {
            try {
                File src = AppServices.require().cache().getSourceFile(sel);
                if (src != null) {
                    seed = src.getPath();
                }
            } catch (Throwable ignored) {
            }
        }
        if (seed == null && fallbackValue instanceof File) {
            seed = ((File) fallbackValue).getPath();
        }
        FxChooserSupport.browseLocalFiles(
                FxFileChooserUtil.windowOf(combo),
                param.getNameEnglish() != null ? param.getNameEnglish() : param.getName(),
                filters,
                false,
                seed,
                files -> FxChooserSupport.loadLocalFilesAsync(
                        files,
                        pobClass,
                        loaded -> {
                            refreshItems();
                            combo.getSelectionModel().select(loaded.get(0));
                            fallbackValue = null;
                            fireChange();
                        }));
    }

    private void refreshItems() {
        List<Object> items = new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            List<Object> cached = AppServices.require().cache().getCachedObjectsL(pobClass);
            items.addAll(cached);
        } catch (Exception ignore) {
            // cache not available yet
        }
        Object selected = combo.getSelectionModel().getSelectedItem();
        combo.setPromptText(null);
        combo.setItems(FXCollections.observableArrayList(items));
        if (selected != null && items.contains(selected)) {
            combo.getSelectionModel().select(selected);
        } else if (fallbackValue != null) {
            applyInitialValue(fallbackValue, false);
        }
    }

    /**
     * @param tryDefaultAfterMiss when true, after value miss try {@code param.getDefault}
     * before selecting first.
     */
    private void applyInitialValue(Object v, boolean tryDefaultAfterMiss) {
        if (trySelect(v)) {
            return;
        }
        if (tryDefaultAfterMiss) {
            Object def = ParamEditorSupport.unsetIfEmptyArray(param.getDefault());
            if (def != null && def != v && trySelect(def)) {
                return;
            }
        }
        combo.setPromptText(null);
        if (!combo.getItems().isEmpty()) {
            combo.getSelectionModel().select(0);
            fallbackValue = null;
            return;
        }
        combo.getSelectionModel().clearSelection();
        if (v != null && !v.getClass().isArray()) {
            fallbackValue = v;
        }
    }

    private boolean trySelect(Object v) {
        if (v == null || v.getClass().isArray()) {
            // Empty Dataset[] / RankedList[] defaults must not become combo text via String.valueOf.
            return false;
        }
        if (pobClass.isInstance(v)) {
            if (!combo.getItems().contains(v)) {
                combo.getItems().add(0, v);
            }
            combo.getSelectionModel().select(v);
            combo.setPromptText(null);
            fallbackValue = null;
            return true;
        }
        String path = v instanceof File ? ((File) v).getPath() : ParamEditorSupport.displayText(v);
        if (path.isEmpty()) {
            return false;
        }
        int idx = findByPobPathIndex(path);
        if (idx >= 0) {
            combo.getSelectionModel().select(idx);
            combo.setPromptText(null);
            fallbackValue = null;
            return true;
        }
        return false;
    }

    private int findByPobPathIndex(String path) {
        if (path == null || path.isBlank()) {
            return -1;
        }
        for (int i = 0; i < combo.getItems().size(); i++) {
            Object obj = combo.getItems().get(i);
            try {
                File f = AppServices.require().cache().getSourceFile(obj);
                if (f != null && f.getPath().equals(path)) {
                    return i;
                }
            } catch (Throwable ignored) {
            }
        }
        return -1;
    }

    @Override
    public Object getValue() {
        Object sel = combo.getSelectionModel().getSelectedItem();
        if (sel != null) {
            return sel;
        }
        return fallbackValue;
    }

    @Override
    public void setValue(Object value) {
        fallbackValue = null;
        combo.setPromptText(null);
        applyInitialValue(value, true);
    }

    @Override
    public Object getView() {
        return box;
    }
}
