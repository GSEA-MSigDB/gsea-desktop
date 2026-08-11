/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.util.Optional;

import edu.mit.broad.genome.objects.TemplateMode;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Window;
import xtools.api.param.Param;

/**
 * keep {@code fCurrSel} separately from the text field; cancel sets restore state to null while leaving the field text unchanged.
 */
final class TemplateChooserEditor extends AbstractBoundEditor {
    private final TextField field = new TextField();
    private final HBox box = new HBox(6);
    private String currSel;

    TemplateChooserEditor(Param param, TemplateMode mode) {
        super(param);
        String text = ParamEditorSupport.paramDisplayText(param);
        field.setText(text);
        currSel = text.isBlank() ? null : text;
        field.setPromptText(null);
        Button choose = xapps.gsea.fx.widgets.FxEllipsisButton.create("Choose");
        choose.setOnAction(e -> {
            Window owner = FxFileChooserUtil.windowOf(field);
            Optional<String> result = FxTemplateChooserDialog.show(owner, mode, currSel);
            if (result.isPresent()) {
                field.setText(result.get());
                currSel = result.get();
            } else {
                currSel = null;
            }
        });
        HBox.setHgrow(field, Priority.ALWAYS);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().addAll(field, choose);
        ParamEditorSupport.attachPathColorListener(field);
        ParamEditorSupport.attachFileOrPobDrop(field);
        field.textProperty().addListener((obs, o, n) -> fireChange());
    }

    @Override
    public Object getValue() {
        String t = field.getText();
        return (t == null || t.isBlank()) ? null : t;
    }

    @Override
    public void setValue(Object value) {
        String text = ParamEditorSupport.displayText(ParamEditorSupport.unsetIfEmptyArray(value));
        field.setText(text);
        currSel = text.isBlank() ? null : text;
    }

    @Override
    public Object getView() {
        return box;
    }
}
