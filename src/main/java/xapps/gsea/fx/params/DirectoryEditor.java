/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.io.File;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import xtools.api.param.DirParam;
import xtools.api.param.Param;
import xtools.api.param.ReportDirParam;

final class DirectoryEditor extends AbstractBoundEditor {
    private final TextField field = new TextField();
    private final HBox box = new HBox(6);

    DirectoryEditor(Param param) {
        super(param);
        // DirParam.getValueStringRepresentation(false) returns only the last segment
        // (e.g. "jul21"); committing that resolves under the process cwd. Always show
        // an absolute path for directory params.
        field.setText(absoluteDirText(param));
        field.setPromptText(null);
        Button browse = xapps.gsea.fx.widgets.FxEllipsisButton.create("Browse");
        browse.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(param.getNameEnglish() != null ? param.getNameEnglish() : param.getName());
            FxFileChooserUtil.seedInitialDirectory(chooser, field.getText());
            File f = chooser.showDialog(FxFileChooserUtil.windowOf(field));
            if (f != null) {
                FxFileChooserUtil.registerOpenedDir(f);
                field.setText(f.getAbsolutePath());
            }
        });
        HBox.setHgrow(field, Priority.ALWAYS);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().addAll(field, browse);
        if (param instanceof ReportDirParam || param instanceof DirParam) {
            ParamEditorSupport.attachPathColorListener(field, true);
        } else {
            ParamEditorSupport.attachPathColorListener(field);
        }
        ParamEditorSupport.attachFileOrPobDrop(field);
        field.textProperty().addListener((obs, o, n) -> fireChange());
    }

    private static String absoluteDirText(Param param) {
        Object v = param.getValue() != null ? param.getValue() : param.getDefault();
        return absoluteDirDisplay(v);
    }

    private static String absoluteDirDisplay(Object value) {
        if (value == null) {
            return "";
        }
        File file = value instanceof File ? (File) value : new File(value.toString().trim());
        String path = file.getPath();
        if (path == null || path.isBlank()) {
            return "";
        }
        return file.getAbsolutePath();
    }

    @Override
    public Object getValue() {
        String t = field.getText();
        if (t == null || t.isBlank()) {
            return new File("");
        }
        return new File(t.trim()).getAbsoluteFile();
    }

    @Override
    public void setValue(Object value) {
        field.setText(absoluteDirDisplay(value));
    }

    @Override
    public Object getView() {
        return box;
    }
}
