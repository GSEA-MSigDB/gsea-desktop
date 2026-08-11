/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import xtools.api.param.Param;

/**
 * Editable ComboBox seeded with the param's hints (e.g. {@code RandomSeedTypeParam},
 * or an {@code IntegerParam} that also supplies hints) but allowing free-form typed entry.
 */
final class EditableComboEditor extends AbstractBoundEditor {
    private final ComboBox<String> combo = new ComboBox<>();
    private final boolean integerMode;

    EditableComboEditor(Param param, boolean integerMode) {
        super(param);
        this.integerMode = integerMode;
        combo.setEditable(true);
        combo.setMaxWidth(Double.MAX_VALUE);
        List<String> items = new ArrayList<>();
        Object[] hints = param.getHints();
        if (hints != null) {
            for (Object h : hints) {
                if (h != null) {
                    items.add(String.valueOf(h));
                }
            }
        }
        combo.setItems(FXCollections.observableArrayList(items));
        Object v = ParamEditorSupport.unsetIfEmptyArray(
                param.getValue() != null ? param.getValue() : param.getDefault());
        String text = ParamEditorSupport.displayText(v);
        combo.setValue(text.isEmpty() ? null : text);
        combo.valueProperty().addListener((obs, o, n) -> fireChange());
        combo.getEditor().textProperty().addListener((obs, o, n) -> fireChange());
    }

    @Override
    public Object getValue() {
        String t = combo.getEditor().getText();
        if (t == null || t.isBlank()) {
            t = combo.getValue();
        }
        if (t == null || t.isBlank()) {
            return null;
        }
        t = t.trim();
        if (integerMode) {
            try {
                return Integer.valueOf(t);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException(
                        "Parameter '" + param.getNameEnglish() + "' must be a valid integer. Specified: " + t);
            }
        }
        return t;
    }

    @Override
    public void setValue(Object value) {
        String text = ParamEditorSupport.displayText(ParamEditorSupport.unsetIfEmptyArray(value));
        combo.setValue(text.isEmpty() ? null : text);
    }

    @Override
    public Object getView() {
        return combo;
    }
}
