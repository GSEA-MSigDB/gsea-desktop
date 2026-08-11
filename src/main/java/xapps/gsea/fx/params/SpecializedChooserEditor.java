/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.util.Optional;
import java.util.function.BiFunction;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Window;
import xtools.api.param.Param;

final class SpecializedChooserEditor extends AbstractBoundEditor {
    private final TextField field = new TextField();
    private final HBox box = new HBox(6);

    SpecializedChooserEditor(Param param, String buttonLabel,
            BiFunction<Window, String, Optional<String>> chooser) {
        super(param);
        field.setText(ParamEditorSupport.paramDisplayText(param));
        Button choose = xapps.gsea.fx.widgets.FxEllipsisButton.create(buttonLabel);
        choose.setOnAction(e -> {
            Window owner = FxFileChooserUtil.windowOf(field);
            Optional<String> result = chooser.apply(owner, field.getText());
            result.ifPresent(field::setText);
        });
        HBox.setHgrow(field, Priority.ALWAYS);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().addAll(field, choose);
        ParamEditorSupport.attachPathColorListener(field);
        ParamEditorSupport.attachFileOrPobDrop(field);
        field.setPromptText(null);
        field.textProperty().addListener((obs, o, n) -> fireChange());
    }

    @Override
    public Object getValue() {
        String t = field.getText();
        return (t == null || t.isBlank()) ? null : t;
    }

    @Override
    public void setValue(Object value) {
        field.setText(ParamEditorSupport.displayText(ParamEditorSupport.unsetIfEmptyArray(value)));
    }

    @Override
    public Object getView() {
        return box;
    }
}
