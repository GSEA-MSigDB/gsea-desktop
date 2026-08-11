/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import javafx.scene.control.TextField;
import xtools.api.param.Param;
import xtools.api.param.StringInputParam;

final class TextEditor extends AbstractBoundEditor {
    private final TextField field = new TextField();
    private final char[] extraSafe;

    TextEditor(Param param) {
        super(param);
        this.extraSafe = param instanceof StringInputParam
                ? ((StringInputParam) param).getAdditionalSafeChars() : null;
        field.setText(ParamEditorSupport.paramDisplayText(param));
        field.setPromptText(null);
        if (param instanceof StringInputParam) {
            field.addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED, e -> {
                String ch = e.getCharacter();
                if (ch == null || ch.isEmpty()) {
                    return;
                }
                char c = ch.charAt(0);
                String text = field.getText() != null ? field.getText() : "";
                if (!isSafeChar(c, extraSafe, text.length())) {
                    e.consume();
                }
            });
        }
        field.textProperty().addListener((obs, o, n) -> fireChange());
    }

    private static boolean isSafeChar(char c, char[] extra, int currentLength) {
        if (Character.isLetterOrDigit(c) || Character.isISOControl(c)) {
            return true;
        }
        if (c == '-' && currentLength == 0) {
            return true;
        }
        if (c == ',' || c == '_' || c == '<' || c == '>' || c == '.'
                || c == '/' || c == '\\' || c == '=' || c == ' ') {
            return true;
        }
        if (extra != null) {
            for (char x : extra) {
                if (c == x) {
                    return true;
                }
            }
        }
        return false;
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
        return field;
    }
}
