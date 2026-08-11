/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import javafx.scene.control.TextField;
import xtools.api.param.Param;

final class IntegerEditor extends AbstractBoundEditor {
    private final TextField field = new TextField();

    IntegerEditor(Param param) {
        super(param);
        Object v = ParamEditorSupport.unsetIfEmptyArray(
                param.getValue() != null ? param.getValue() : param.getDefault());
        if (v != null) {
            field.setText(ParamEditorSupport.displayText(v));
        }
        field.textProperty().addListener((obs, o, n) -> fireChange());
    }

    @Override
    public Object getValue() {
        String t = field.getText();
        if (t == null || t.isBlank()) {
            return null;
        }
        return Integer.valueOf(t.trim());
    }

    @Override
    public void setValue(Object value) {
        field.setText(value == null || ParamEditorSupport.isEmptyArray(value)
                ? "" : ParamEditorSupport.displayText(value));
    }

    @Override
    public Object getView() {
        return field;
    }
}
