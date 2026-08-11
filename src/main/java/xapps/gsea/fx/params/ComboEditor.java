/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import xtools.api.param.Param;

final class ComboEditor extends AbstractBoundEditor {
    private final ComboBox<Object> combo = new ComboBox<>();

    ComboEditor(Param param) {
        super(param);
        Object[] hints = param.getHints();
        List<Object> items = new ArrayList<>();
        if (hints != null) {
            for (Object h : hints) {
                // Hints can accidentally be a nested empty array (DirParam-style).
                if (h != null && !ParamEditorSupport.isEmptyArray(h)) {
                    items.add(h);
                }
            }
        }
        Object value = ParamEditorSupport.unsetIfEmptyArray(param.getValue());
        if (value != null && !value.getClass().isArray() && !ParamEditorSupport.containsByString(items, value)) {
            items.add(value);
        }
        combo.setItems(FXCollections.observableArrayList(items));
        selectByString(value != null ? value : ParamEditorSupport.unsetIfEmptyArray(param.getDefault()));
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> fireChange());
    }

    private void selectByString(Object sel) {
        sel = ParamEditorSupport.unsetIfEmptyArray(sel);
        if (sel == null) {
            return;
        }
        if (selectMatchingString(sel)) {
            return;
        }
        Object def = ParamEditorSupport.unsetIfEmptyArray(param.getDefault());
        if (def != null && def != sel && selectMatchingString(def)) {
            return;
        }
        if (!combo.getItems().isEmpty()) {
            combo.getSelectionModel().selectFirst();
        }
    }

    private boolean selectMatchingString(Object sel) {
        String sels = ParamEditorSupport.displayText(sel);
        if (sels.isEmpty()) {
            return false;
        }
        for (int i = 0; i < combo.getItems().size(); i++) {
            Object item = combo.getItems().get(i);
            if (item != null && sels.equals(ParamEditorSupport.displayText(item))) {
                combo.getSelectionModel().select(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public Object getValue() {
        return combo.getSelectionModel().getSelectedItem();
    }

    @Override
    public void setValue(Object value) {
        value = ParamEditorSupport.unsetIfEmptyArray(value);
        if (value != null && !value.getClass().isArray()
                && !ParamEditorSupport.containsByString(combo.getItems(), value)) {
            combo.getItems().add(value);
        }
        selectByString(value);
    }

    @Override
    public Object getView() {
        return combo;
    }
}
