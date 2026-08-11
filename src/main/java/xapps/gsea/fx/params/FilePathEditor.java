/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.io.File;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import xtools.api.param.Param;

final class FilePathEditor extends AbstractBoundEditor {
    private final TextField field = new TextField();
    private final HBox box = new HBox(6);

    FilePathEditor(Param param, List<FileChooser.ExtensionFilter> filters) {
        super(param);
        field.setText(ParamEditorSupport.paramDisplayText(param));
        Button browse = xapps.gsea.fx.widgets.FxEllipsisButton.create("Browse");
        browse.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(param.getNameEnglish() != null ? param.getNameEnglish() : param.getName());
            if (filters != null) {
                chooser.getExtensionFilters().addAll(filters);
            }
            FxFileChooserUtil.seedInitialDirectory(chooser);
            File f = chooser.showOpenDialog(FxFileChooserUtil.windowOf(field));
            if (f != null) {
                FxFileChooserUtil.registerOpened(f);
                field.setText(f.getAbsolutePath());
            }
        });
        HBox.setHgrow(field, Priority.ALWAYS);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().addAll(field, browse);
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
        field.setText(value == null ? "" : ParamEditorSupport.displayText(value));
    }

    @Override
    public Object getView() {
        return box;
    }
}
