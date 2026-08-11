/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import javafx.scene.control.ComboBox;
import xtools.api.param.Param;

final class BooleanEditor extends AbstractBoundEditor {
    private final ComboBox<Boolean> combo = new ComboBox<>();

    BooleanEditor(Param param) {
        super(param);
        combo.getItems().setAll(Boolean.TRUE, Boolean.FALSE);
        combo.setEditable(false);
        Object v = param.getValue() != null ? param.getValue() : param.getDefault();
        if (v instanceof Boolean) {
            combo.getSelectionModel().select((Boolean) v);
        } else if (v != null) {
            combo.getSelectionModel().select(Boolean.valueOf(String.valueOf(v)));
        } else {
            combo.getSelectionModel().select(Boolean.FALSE);
        }
        combo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> fireChange());
    }

    @Override
    public Object getValue() {
        Boolean sel = combo.getSelectionModel().getSelectedItem();
        return sel != null ? sel : Boolean.FALSE;
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof Boolean) {
            combo.getSelectionModel().select((Boolean) value);
        } else if (value != null) {
            combo.getSelectionModel().select(Boolean.valueOf(String.valueOf(value)));
        }
    }

    @Override
    public Object getView() {
        return combo;
    }
}
