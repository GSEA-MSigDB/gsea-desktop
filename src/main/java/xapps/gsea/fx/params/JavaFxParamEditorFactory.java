/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import org.gsea_msigdb.gsea.ui.api.ParamEditor;
import org.gsea_msigdb.gsea.ui.api.ParamEditorFactory;

import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.objects.TemplateMode;
import edu.mit.broad.genome.parsers.ParserFactory;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import xtools.api.param.BooleanParam;
import xtools.api.param.ChipOptParam;
import xtools.api.param.DatasetReqdParam;
import xtools.api.param.DirParam;
import xtools.api.param.FeatureSpaceReqdParam;
import xtools.api.param.GeneSetMatrixMultiChooserParam;
import xtools.api.param.IntegerParam;
import xtools.api.param.Param;
import xtools.api.param.RandomSeedTypeParam;
import xtools.api.param.RankedListReqdParam;
import xtools.api.param.ReportDirParam;
import xtools.api.param.StringInputParam;
import xtools.api.param.StringMultiInputParam;
import xtools.api.param.TemplateSingleChooserParam;

/**
 * JavaFX editors for GSEA tool params, including specialized gene-set / chip / phenotype choosers.
 */
public class JavaFxParamEditorFactory implements ParamEditorFactory {

    @Override
    public ParamEditor createEditor(Param param) {
        if (param instanceof BooleanParam) {
            return new BooleanEditor(param);
        }
        if (param instanceof RandomSeedTypeParam) {
            return new EditableComboEditor(param, false);
        }
        if (param instanceof IntegerParam) {
            if (param.getHints() != null && param.getHints().length > 0) {
                return new EditableComboEditor(param, true);
            }
            return new IntegerEditor(param);
        }
        if (param instanceof DatasetReqdParam) {
            return new CachedObjectEditor(param, Dataset.class, datasetFilters());
        }
        if (param instanceof RankedListReqdParam) {
            return new CachedObjectEditor(param, RankedList.class, rankedListFilters());
        }
        if (param instanceof DirParam || param instanceof ReportDirParam) {
            return new DirectoryEditor(param);
        }
        if (param instanceof GeneSetMatrixMultiChooserParam) {
            return new SpecializedChooserEditor(param, "Choose…",
                    (owner, cur) -> FxGeneSetChooserDialog.show(owner));
        }
        if (param instanceof ChipOptParam) {
            return new SpecializedChooserEditor(param, "Choose…",
                    (owner, cur) -> FxChipChooserDialog.show(owner));
        }
        if (param instanceof TemplateSingleChooserParam) {
            TemplateMode mode = ((TemplateSingleChooserParam) param).getMode();
            // Swing TemplateSingleChooserParam: restore fCurrSel; cancel wipes it.
            return new TemplateChooserEditor(param, mode);
        }
        if (param instanceof StringMultiInputParam) {
            return new MultiLineTextEditor(param);
        }
        if (param.isFileBased()) {
            return new FilePathEditor(param, null);
        }
        if (param instanceof FeatureSpaceReqdParam) {
            return new FeatureSpaceComboEditor(param);
        }
        if (param.getHints() != null && param.getHints().length > 0
                && !(param instanceof StringInputParam)) {
            return new ComboEditor(param);
        }
        return new TextEditor(param);
    }

    private static List<FileChooser.ExtensionFilter> datasetFilters() {
        List<FileChooser.ExtensionFilter> filters = new ArrayList<>();
        filters.add(new FileChooser.ExtensionFilter(
                "Expression datasets (*.gct, *.res, *.pcl, *.txt)", "*.gct", "*.res", "*.pcl", "*.txt"));
        filters.add(new FileChooser.ExtensionFilter("All files", "*.*"));
        return filters;
    }

    private static List<FileChooser.ExtensionFilter> rankedListFilters() {
        List<FileChooser.ExtensionFilter> filters = new ArrayList<>();
        filters.add(new FileChooser.ExtensionFilter("Ranked list (*.rnk)", "*.rnk"));
        filters.add(new FileChooser.ExtensionFilter("All files", "*.*"));
        return filters;
    }

    /**
     * Display text for path-ish param values.
     * Matches Swing {@code ParamSetDisplay.reset}: {@code Object[]} is joined with commas
     * (empty array → blank). Never use {@code Object.toString()} on arrays
     * ({@code [L…;@hash]}).
     */
    private static String displayText(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof File) {
            return ((File) v).getPath();
        }
        if (v instanceof Object[]) {
            Object[] arr = (Object[]) v;
            if (arr.length == 0) {
                return "";
            }
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] == null) {
                    continue;
                }
                if (buf.length() > 0) {
                    buf.append(',');
                }
                buf.append(elementDisplayText(arr[i]));
            }
            return buf.toString();
        }
        return String.valueOf(v);
    }

    private static String elementDisplayText(Object el) {
        if (el instanceof File) {
            return ((File) el).getPath();
        }
        if (el instanceof PersistentObject) {
            try {
                String path = ParserFactory.getCache().getSourcePath(el);
                if (path != null && !path.isBlank()) {
                    return path;
                }
            } catch (Throwable ignored) {
            }
            return ((PersistentObject) el).getName();
        }
        return el.toString().trim();
    }

    /**
     * Prefer each Param's own string form (e.g. {@code GeneSetMatrixMultiChooserParam} path list);
     * fall back to {@link #displayText}.
     */
    private static String paramDisplayText(Param param) {
        try {
            String rep = param.getValueStringRepresentation(false);
            if (rep != null) {
                return rep;
            }
        } catch (Throwable ignored) {
        }
        Object v = param.getValue();
        if (v == null) {
            v = param.getDefault();
        }
        return displayText(v);
    }

    /** Empty arrays are common Param defaults; treat like unset for combo/text seeding. */
    private static boolean isEmptyArray(Object v) {
        return v != null && v.getClass().isArray() && java.lang.reflect.Array.getLength(v) == 0;
    }

    /** Null-out empty-array defaults so they are not stringified as {@code [L…;@hash]}. */
    private static Object unsetIfEmptyArray(Object v) {
        return isEmptyArray(v) ? null : v;
    }

    /**
     * Path coloring matching Swing {@code GFieldUtils}: blue = good, red = bad;
     * URL schemes and comma-separated multi-paths supported.
     * Dir/ReportDir fields also get {@code gsea-dir-field} (light green).
     */
    private static void attachPathColorListener(TextField field) {
        attachPathColorListener(field, false);
    }

    private static void attachPathColorListener(TextField field, boolean dirField) {
        if (dirField) {
            if (!field.getStyleClass().contains("gsea-dir-field")) {
                field.getStyleClass().add("gsea-dir-field");
            }
        }
        FxPathFieldColors.attach(field);
    }

    /**
     * Swing param fields accept {@code PobTransferable} / file-list drops → fill path text.
     */
    private static void attachFileOrPobDrop(TextField field) {
        field.setOnDragOver(e -> {
            if (e.getGestureSource() != field
                    && (e.getDragboard().hasFiles()
                    || xapps.gsea.fx.FxPobTransferSupport.hasPobList(e.getDragboard()))) {
                e.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            e.consume();
        });
        field.setOnDragDropped(e -> {
            javafx.scene.input.Dragboard db = e.getDragboard();
            String path = null;
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                path = db.getFiles().get(0).getAbsolutePath();
            } else if (xapps.gsea.fx.FxPobTransferSupport.hasPobList(db)) {
                List<PersistentObject> pobs = xapps.gsea.fx.FxPobTransferSupport.takeDragPobs(e);
                if (!pobs.isEmpty()) {
                    try {
                        File src = ParserFactory.getCache().getSourceFile(pobs.get(0));
                        if (src != null) {
                            path = src.getAbsolutePath();
                        }
                    } catch (Throwable ignored) {
                        // leave path null
                    }
                }
            }
            boolean ok = path != null;
            if (ok) {
                field.setText(path);
            }
            e.setDropCompleted(ok);
            e.consume();
        });
    }

    private abstract static class AbstractBoundEditor implements ParamEditor {
        protected final Param param;
        private Runnable changeListener;

        protected AbstractBoundEditor(Param param) {
            this.param = param;
        }

        @Override
        public void commitToParam() {
            param.setValue(getValue());
        }

        @Override
        public void onChange(Runnable listener) {
            this.changeListener = listener;
        }

        protected void fireChange() {
            if (changeListener != null) {
                changeListener.run();
            }
        }
    }

    private static final class TextEditor extends AbstractBoundEditor {
        private final TextField field = new TextField();
        private final char[] extraSafe;

        private TextEditor(Param param) {
            super(param);
            this.extraSafe = param instanceof StringInputParam
                    ? ((StringInputParam) param).getAdditionalSafeChars() : null;
            field.setText(paramDisplayText(param));
            // Swing GTextField: empty until typed (label is beside the editor).
            field.setPromptText(null);
            // Swing GSafeCharsField for StringInputParam (and report-label path uses separate checks).
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
            // Swing GSafeCharsField: '-' only when document length is 0.
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
            field.setText(value == null ? "" : displayText(value));
        }

        @Override
        public Object getView() {
            return field;
        }
    }

    private static final class IntegerEditor extends AbstractBoundEditor {
        private final TextField field = new TextField();

        private IntegerEditor(Param param) {
            super(param);
            Object v = unsetIfEmptyArray(param.getValue() != null ? param.getValue() : param.getDefault());
            if (v != null) {
                field.setText(displayText(v));
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
            field.setText(value == null || isEmptyArray(value) ? "" : displayText(value));
        }

        @Override
        public Object getView() {
            return field;
        }
    }

    private static final class BooleanEditor extends AbstractBoundEditor {
        private final ComboBox<Boolean> combo = new ComboBox<>();

        private BooleanEditor(Param param) {
            super(param);
            // Swing BooleanParam: non-editable TRUE/FALSE GComboBoxField.
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

    private static final class ComboEditor extends AbstractBoundEditor {
        private final ComboBox<Object> combo = new ComboBox<>();

        private ComboEditor(Param param) {
            super(param);
            Object[] hints = param.getHints();
            List<Object> items = new ArrayList<>();
            if (hints != null) {
                for (Object h : hints) {
                    // Hints can accidentally be a nested empty array (DirParam-style).
                    if (h != null && !isEmptyArray(h)) {
                        items.add(h);
                    }
                }
            }
            Object value = unsetIfEmptyArray(param.getValue());
            // Swing createActionListenerBoundHintsComboBox: keep orphan value not in hints.
            if (value != null && !value.getClass().isArray() && !containsByString(items, value)) {
                items.add(value);
            }
            combo.setItems(FXCollections.observableArrayList(items));
            selectByString(value != null ? value : unsetIfEmptyArray(param.getDefault()));
            combo.setMaxWidth(Double.MAX_VALUE);
            combo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> fireChange());
        }

        private void selectByString(Object sel) {
            // Swing ParamHelper.safeSelectValueDefaultByString after orphan may already be in items.
            sel = unsetIfEmptyArray(sel);
            if (sel == null) {
                return;
            }
            if (selectMatchingString(sel)) {
                return;
            }
            Object def = unsetIfEmptyArray(param.getDefault());
            if (def != null && def != sel && selectMatchingString(def)) {
                return;
            }
            if (!combo.getItems().isEmpty()) {
                combo.getSelectionModel().selectFirst();
            }
        }

        private boolean selectMatchingString(Object sel) {
            String sels = displayText(sel);
            if (sels.isEmpty()) {
                return false;
            }
            for (int i = 0; i < combo.getItems().size(); i++) {
                Object item = combo.getItems().get(i);
                if (item != null && sels.equals(displayText(item))) {
                    combo.getSelectionModel().select(i);
                    return true;
                }
            }
            return false;
        }

        private static boolean containsByString(List<Object> items, Object value) {
            String vs = displayText(value);
            if (vs.isEmpty()) {
                return false;
            }
            for (Object item : items) {
                if (item != null && vs.equals(displayText(item))) {
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
            value = unsetIfEmptyArray(value);
            if (value != null && !value.getClass().isArray() && !containsByString(combo.getItems(), value)) {
                combo.getItems().add(value);
            }
            selectByString(value);
        }

        @Override
        public Object getView() {
            return combo;
        }
    }

    /**
     * Swing {@code FeatureSpaceReqdParam.MyListRenderer}: descriptive suffixes in the list.
     * Stored value remains the MODES token ({@code Remap_Only}/{@code Collapse}/{@code No_Collapse}).
     */
    private static final class FeatureSpaceComboEditor extends AbstractBoundEditor {
        private static final String[] LABELS = {
                "Remap_Only (remap dataset with 'chip' without mathematical collapse)",
                "Collapse (use 'chip' to collapse dataset to symbols before analysis)",
                "No_Collapse (use dataset 'as is' in the original format)"
        };
        private final ComboBox<Object> combo = new ComboBox<>();

        private FeatureSpaceComboEditor(Param param) {
            super(param);
            Object[] hints = param.getHints();
            List<Object> items = new ArrayList<>();
            if (hints != null) {
                for (Object h : hints) {
                    if (h != null && !isEmptyArray(h)) {
                        items.add(h);
                    }
                }
            }
            Object value = unsetIfEmptyArray(param.getValue());
            if (value != null && !value.getClass().isArray() && !containsByString(items, value)) {
                items.add(value);
            }
            combo.setItems(FXCollections.observableArrayList(items));
            // Swing MyListRenderer: long labels only when index >= 0 (dropdown); closed = raw token.
            combo.setCellFactory(lv -> featureSpaceCell(true));
            combo.setButtonCell(featureSpaceCell(false));
            selectByString(value != null ? value : unsetIfEmptyArray(param.getDefault()));
            combo.setMaxWidth(Double.MAX_VALUE);
            combo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> fireChange());
        }

        private static boolean containsByString(List<Object> items, Object value) {
            String vs = displayText(value);
            if (vs.isEmpty()) {
                return false;
            }
            for (Object item : items) {
                if (item != null && vs.equals(displayText(item))) {
                    return true;
                }
            }
            return false;
        }

        private static ListCell<Object> featureSpaceCell(boolean descriptive) {
            return new ListCell<>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        return;
                    }
                    String token = item.toString();
                    if (descriptive) {
                        for (int i = 0; i < LABELS.length; i++) {
                            if (LABELS[i].startsWith(token + " ")) {
                                setText(LABELS[i]);
                                return;
                            }
                        }
                    }
                    setText(token);
                }
            };
        }

        private void selectByString(Object sel) {
            sel = unsetIfEmptyArray(sel);
            if (sel == null) {
                return;
            }
            if ("true".equals(sel)) {
                sel = "Collapse";
            } else if ("false".equals(sel)) {
                sel = "No_Collapse";
            }
            if (selectMatchingString(sel)) {
                return;
            }
            Object def = unsetIfEmptyArray(param.getDefault());
            if (def != null && def != sel && selectMatchingString(def)) {
                return;
            }
            if (!combo.getItems().isEmpty()) {
                combo.getSelectionModel().selectFirst();
            }
        }

        private boolean selectMatchingString(Object sel) {
            String sels = displayText(sel);
            if (sels.isEmpty()) {
                return false;
            }
            for (int i = 0; i < combo.getItems().size(); i++) {
                Object item = combo.getItems().get(i);
                if (item != null && sels.equals(displayText(item))) {
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
            value = unsetIfEmptyArray(value);
            if (value != null && !value.getClass().isArray() && !containsByString(combo.getItems(), value)) {
                combo.getItems().add(value);
            }
            selectByString(value);
        }

        @Override
        public Object getView() {
            return combo;
        }
    }

    /**
     * Editable ComboBox seeded with the param's hints (e.g. {@code RandomSeedTypeParam},
     * or an {@code IntegerParam} that also supplies hints) but allowing free-form typed entry.
     */
    private static final class EditableComboEditor extends AbstractBoundEditor {
        private final ComboBox<String> combo = new ComboBox<>();
        private final boolean integerMode;

        private EditableComboEditor(Param param, boolean integerMode) {
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
            Object v = unsetIfEmptyArray(param.getValue() != null ? param.getValue() : param.getDefault());
            String text = displayText(v);
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
            String text = displayText(unsetIfEmptyArray(value));
            combo.setValue(text.isEmpty() ? null : text);
        }

        @Override
        public Object getView() {
            return combo;
        }
    }

    private static final class SpecializedChooserEditor extends AbstractBoundEditor {
        private final TextField field = new TextField();
        private final HBox box = new HBox(6);

        private SpecializedChooserEditor(Param param, String buttonLabel,
                BiFunction<Window, String, Optional<String>> chooser) {
            super(param);
            field.setText(paramDisplayText(param));
            Button choose = xapps.gsea.fx.FxEllipsisButton.create(buttonLabel);
            choose.setOnAction(e -> {
                Window owner = FxFileChooserUtil.windowOf(field);
                Optional<String> result = chooser.apply(owner, field.getText());
                result.ifPresent(field::setText);
            });
            HBox.setHgrow(field, Priority.ALWAYS);
            box.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().addAll(field, choose);
            attachPathColorListener(field);
            attachFileOrPobDrop(field);
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
            field.setText(displayText(unsetIfEmptyArray(value)));
        }

        @Override
        public Object getView() {
            return box;
        }
    }

    private static final class FilePathEditor extends AbstractBoundEditor {
        private final TextField field = new TextField();
        private final HBox box = new HBox(6);

        private FilePathEditor(Param param, List<FileChooser.ExtensionFilter> filters) {
            super(param);
            field.setText(paramDisplayText(param));
            Button browse = xapps.gsea.fx.FxEllipsisButton.create("Browse");
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
            attachPathColorListener(field);
            attachFileOrPobDrop(field);
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
            field.setText(value == null ? "" : displayText(value));
        }

        @Override
        public Object getView() {
            return box;
        }
    }

    private static final class MultiLineTextEditor extends AbstractBoundEditor {
        private final TextField field = new TextField();
        private final HBox box = new HBox(6);

        private MultiLineTextEditor(Param param) {
            super(param);
            field.setText(paramDisplayText(param));
            field.setPromptText(null);
            // Swing multi-line chooser field starts empty (no invent prompt).
            Button ellipsis = xapps.gsea.fx.FxEllipsisButton.create();
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
            return displayText(unsetIfEmptyArray(v));
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

    /**
     * HBox with a ComboBox listing cached objects of the requested Pob class (Dataset,
     * RankedList, ...) — Swing {@code PobParam} has no browse control.
     */
    private static final class CachedObjectEditor extends AbstractBoundEditor {
        private final ComboBox<Object> combo = new ComboBox<>();
        private final HBox box = new HBox(6);
        private final Class<?> pobClass;
        private Object fallbackValue;

        private CachedObjectEditor(Param param, Class<?> pobClass, List<FileChooser.ExtensionFilter> filters) {
            super(param);
            this.pobClass = pobClass;
            // filters unused: Swing PobParam has no FileChooser on the editor.

            combo.setMaxWidth(Double.MAX_VALUE);
            combo.setCellFactory(lv -> xapps.gsea.fx.FxPobListCells.pobNameQuickInfoCell());
            combo.setButtonCell(xapps.gsea.fx.FxPobListCells.pobNameQuickInfoCell());
            refreshItems();

            // Swing ParamHelper.safeSelectPobValueDefaultOrFirst: value → default → first.
            applyInitialValue(unsetIfEmptyArray(param.getValue()), true);

            // Swing PobParam: combo only (load files via Load Data / object cache).
            HBox.setHgrow(combo, Priority.ALWAYS);
            box.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().add(combo);
            combo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> fireChange());
            // Live cache binding: refresh when the popup opens (Swing ObjectBindery parity).
            combo.setOnShowing(e -> refreshItems());
            try {
                ParserFactory.getCache().addPathAdditionsListener(evt ->
                        javafx.application.Platform.runLater(this::refreshItems));
            } catch (Throwable ignored) {
            }
        }

        private void refreshItems() {
            List<Object> items = new ArrayList<>();
            try {
                @SuppressWarnings("unchecked")
                List<Object> cached = ParserFactory.getCache().getCachedObjectsL(pobClass);
                items.addAll(cached);
            } catch (Exception ignore) {
                // cache not available yet
            }
            Object selected = combo.getSelectionModel().getSelectedItem();
            combo.setPromptText(null);
            combo.setItems(FXCollections.observableArrayList(items));
            if (selected != null && items.contains(selected)) {
                combo.getSelectionModel().select(selected);
            } else if (fallbackValue != null) {
                applyInitialValue(fallbackValue, false);
            }
        }

        /**
         * @param tryDefaultAfterMiss when true, after value miss try {@code param.getDefault()}
         *                            before selecting first (Swing safeSelectPobValueDefaultOrFirst).
         */
        private void applyInitialValue(Object v, boolean tryDefaultAfterMiss) {
            if (trySelect(v)) {
                return;
            }
            if (tryDefaultAfterMiss) {
                Object def = unsetIfEmptyArray(param.getDefault());
                if (def != null && def != v && trySelect(def)) {
                    return;
                }
            }
            // Swing safeSelectPobValueDefaultOrFirst: select first if present; otherwise leave blank
            // (do not use ComboBox promptText as a fake value — it shows as gray filler).
            combo.setPromptText(null);
            if (!combo.getItems().isEmpty()) {
                combo.getSelectionModel().select(0);
                fallbackValue = null;
                return;
            }
            combo.getSelectionModel().clearSelection();
            if (v != null && !v.getClass().isArray()) {
                fallbackValue = v;
            }
        }

        private boolean trySelect(Object v) {
            if (v == null || v.getClass().isArray()) {
                // Empty Dataset[] / RankedList[] defaults must not become combo text via String.valueOf.
                return false;
            }
            if (pobClass.isInstance(v)) {
                if (!combo.getItems().contains(v)) {
                    combo.getItems().add(0, v);
                }
                combo.getSelectionModel().select(v);
                combo.setPromptText(null);
                fallbackValue = null;
                return true;
            }
            String path = v instanceof File ? ((File) v).getPath() : displayText(v);
            if (path.isEmpty()) {
                return false;
            }
            int idx = findByPobPathIndex(path);
            if (idx >= 0) {
                combo.getSelectionModel().select(idx);
                combo.setPromptText(null);
                fallbackValue = null;
                return true;
            }
            return false;
        }

        private int findByPobPathIndex(String path) {
            if (path == null || path.isBlank()) {
                return -1;
            }
            for (int i = 0; i < combo.getItems().size(); i++) {
                Object obj = combo.getItems().get(i);
                try {
                    File f = ParserFactory.getCache().getSourceFile(obj);
                    if (f != null && f.getPath().equals(path)) {
                        return i;
                    }
                } catch (Throwable ignored) {
                }
            }
            return -1;
        }

        @Override
        public Object getValue() {
            Object sel = combo.getSelectionModel().getSelectedItem();
            if (sel != null) {
                return sel;
            }
            return fallbackValue;
        }

        @Override
        public void setValue(Object value) {
            fallbackValue = null;
            combo.setPromptText(null);
            applyInitialValue(value, true);
        }

        @Override
        public Object getView() {
            return box;
        }
    }

    /**
     * Swing {@code TemplateSingleChooserParam}: keep {@code fCurrSel} separately from the text
     * field; cancel sets restore state to null while leaving the field text unchanged.
     */
    private static final class TemplateChooserEditor extends AbstractBoundEditor {
        private final TextField field = new TextField();
        private final HBox box = new HBox(6);
        /** Swing {@code fCurrSel} — wiped on cancel. */
        private String currSel;

        private TemplateChooserEditor(Param param, TemplateMode mode) {
            super(param);
            String text = paramDisplayText(param);
            field.setText(text);
            currSel = text.isBlank() ? null : text;
            field.setPromptText(null);
            Button choose = xapps.gsea.fx.FxEllipsisButton.create("Choose");
            choose.setOnAction(e -> {
                Window owner = FxFileChooserUtil.windowOf(field);
                Optional<String> result = FxTemplateChooserDialog.show(owner, mode, currSel);
                if (result.isPresent()) {
                    field.setText(result.get());
                    currSel = result.get();
                } else {
                    // Swing: fCurrSel = bag when bag is null (cancel).
                    currSel = null;
                }
            });
            HBox.setHgrow(field, Priority.ALWAYS);
            box.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().addAll(field, choose);
            attachPathColorListener(field);
            attachFileOrPobDrop(field);
            field.textProperty().addListener((obs, o, n) -> fireChange());
        }

        @Override
        public Object getValue() {
            String t = field.getText();
            return (t == null || t.isBlank()) ? null : t;
        }

        @Override
        public void setValue(Object value) {
            String text = displayText(unsetIfEmptyArray(value));
            field.setText(text);
            currSel = text.isBlank() ? null : text;
        }

        @Override
        public Object getView() {
            return box;
        }
    }

    private static final class DirectoryEditor extends AbstractBoundEditor {
        private final TextField field = new TextField();
        private final HBox box = new HBox(6);

        private DirectoryEditor(Param param) {
            super(param);
            field.setText(paramDisplayText(param));
            field.setPromptText(null);
            Button browse = xapps.gsea.fx.FxEllipsisButton.create("Browse");
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
                // Swing GDirFieldPlusChooser text field background #EAFFEA (kept under path coloring).
                attachPathColorListener(field, true);
            } else {
                attachPathColorListener(field);
            }
            attachFileOrPobDrop(field);
            field.textProperty().addListener((obs, o, n) -> fireChange());
        }

        @Override
        public Object getValue() {
            // Swing GDirFieldPlusChooser: blank text → new File("") (not null / not default).
            String t = field.getText();
            return new File(t != null ? t : "");
        }

        @Override
        public void setValue(Object value) {
            field.setText(value == null ? "" : displayText(value));
        }

        @Override
        public Object getView() {
            return box;
        }
    }

    /**
     * Fixed columns approximating Swing FormLayout {@code 130dlu / 225dlu}.
     * Keeps labels and ellipsis buttons aligned instead of stretching with the window.
     */
    public static final double LABEL_COL_WIDTH = 200;
    public static final double FIELD_COL_WIDTH = 350;
    public static final double LABEL_FIELD_GAP = 6;
    /** Label + field + room for a Show/Hide toggle. */
    public static final double SECTION_HEADER_WIDTH =
            LABEL_COL_WIDTH + LABEL_FIELD_GAP + FIELD_COL_WIDTH + 50;

    /** Build a labeled row for a param editor (used by forms). */
    public static HBox labeledRow(Param param, ParamEditor editor) {
        String name = param.getNameEnglish() != null ? param.getNameEnglish() : param.getName();
        Label label = new Label(name != null ? name : "");
        label.setStyle("-fx-font-weight: bold;");
        setExactWidth(label, LABEL_COL_WIDTH);
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER_LEFT);
        if (param.getDesc() != null && !param.getDesc().isBlank()) {
            label.setTooltip(new Tooltip(param.getDesc()));
        }
        Node editorNode = (Node) editor.getView();
        if (param instanceof xtools.api.param.ReportLabelParam
                && !editorNode.getStyleClass().contains("gsea-report-label-field")) {
            editorNode.getStyleClass().add("gsea-report-label-field");
        }
        if (editorNode instanceof Region region) {
            setExactWidth(region, FIELD_COL_WIDTH);
        }
        HBox row = new HBox(LABEL_FIELD_GAP, label, editorNode);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    static void setExactWidth(Region region, double width) {
        region.setMinWidth(width);
        region.setPrefWidth(width);
        region.setMaxWidth(width);
    }
}
