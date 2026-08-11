/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import xtools.api.param.Param;

final class MultiLineTextEditor extends AbstractBoundEditor {
    private final TextField field = new TextField();
    private final HBox box = new HBox(6);

    MultiLineTextEditor(Param param) {
        super(param);
        field.setText(ParamEditorSupport.paramDisplayText(param));
        field.setPromptText(null);
        Button ellipsis = xapps.gsea.fx.widgets.FxEllipsisButton.create();
        ellipsis.setOnAction(e -> {
            javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle(param.getNameEnglish() != null ? param.getNameEnglish() : param.getName());
            dialog.setHeaderText("Enter one or more values (commas, tabs, or newlines)");
            TextArea area = new TextArea(field.getText() != null ? field.getText().replace(',', '\n') : "");
            area.setPrefRowCount(10);
            area.setPrefColumnCount(40);
            area.setWrapText(true);
            dialog.getDialogPane().setContent(area);
            dialog.getDialogPane().getButtonTypes().addAll(
                    javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
            dialog.setResultConverter(bt -> bt == javafx.scene.control.ButtonType.OK ? area.getText() : null);
            dialog.showAndWait().ifPresent(text -> {
                if (text != null) {
                    field.setText(formatDelimited(text));
                }
            });
        });
        HBox.setHgrow(field, Priority.ALWAYS);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().addAll(field, ellipsis);
        field.textProperty().addListener((obs, o, n) -> fireChange());
    }

    private static String formatDelimited(String text) {
        StringBuilder buf = new StringBuilder();
        java.util.StringTokenizer tok = new java.util.StringTokenizer(text, ",\t\n");
        while (tok.hasMoreTokens()) {
            buf.append(tok.nextToken()).append(',');
        }
        return buf.toString();
    }

    private static String toText(Object v) {
        return ParamEditorSupport.displayText(ParamEditorSupport.unsetIfEmptyArray(v));
    }

    @Override
    public Object getValue() {
        String t = field.getText();
        return (t == null || t.isBlank()) ? null : t;
    }

    @Override
    public void setValue(Object value) {
        field.setText(toText(value));
    }

    @Override
    public Object getView() {
        return box;
    }
}
