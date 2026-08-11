/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.util.ArrayList;
import java.util.List;

import org.gsea_msigdb.gsea.ui.api.ParamEditor;
import org.gsea_msigdb.gsea.ui.api.ParamEditorFactory;

import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.objects.TemplateMode;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
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
 * Dispatch is registry-based ({@link #registerDefaults()}); editor implementations live in
 * package-private classes in this package.
 */
public class JavaFxParamEditorFactory implements ParamEditorFactory {

    private static final List<java.util.Map.Entry<Class<? extends Param>, EditorFactory>> TYPE_EDITORS =
            new ArrayList<>();
    private static final List<EditorFactory> PREDICATE_EDITORS = new ArrayList<>();

    static {
        registerDefaults();
    }

    private static void registerDefaults() {
        registerType(BooleanParam.class, BooleanEditor::new);
        registerType(RandomSeedTypeParam.class, p -> new EditableComboEditor(p, false));
        registerType(IntegerParam.class, p -> {
            if (p.getHints() != null && p.getHints().length > 0) {
                return new EditableComboEditor(p, true);
            }
            return new IntegerEditor(p);
        });
        registerType(DatasetReqdParam.class,
                p -> new CachedObjectEditor(p, Dataset.class, datasetFilters()));
        registerType(RankedListReqdParam.class,
                p -> new CachedObjectEditor(p, RankedList.class, rankedListFilters()));
        registerType(DirParam.class, DirectoryEditor::new);
        registerType(ReportDirParam.class, DirectoryEditor::new);
        registerType(GeneSetMatrixMultiChooserParam.class, p -> new SpecializedChooserEditor(p, "Choose…",
                (owner, cur) -> FxGeneSetChooserDialog.show(owner)));
        registerType(ChipOptParam.class, p -> new SpecializedChooserEditor(p, "Choose…",
                (owner, cur) -> FxChipChooserDialog.show(owner)));
        registerType(TemplateSingleChooserParam.class, p -> {
            TemplateMode mode = ((TemplateSingleChooserParam) p).getMode();
            return new TemplateChooserEditor(p, mode);
        });
        registerType(StringMultiInputParam.class, MultiLineTextEditor::new);
        registerType(FeatureSpaceReqdParam.class, FeatureSpaceComboEditor::new);

        PREDICATE_EDITORS.add(p -> p.isFileBased() ? new FilePathEditor(p, null) : null);
        PREDICATE_EDITORS.add(p -> {
            if (p.getHints() != null && p.getHints().length > 0 && !(p instanceof StringInputParam)) {
                return new ComboEditor(p);
            }
            return null;
        });
    }

    private static void registerType(Class<? extends Param> type, EditorFactory factory) {
        TYPE_EDITORS.add(java.util.Map.entry(type, factory));
    }

    @Override
    public ParamEditor createEditor(Param param) {
        for (var entry : TYPE_EDITORS) {
            if (entry.getKey().isInstance(param)) {
                return entry.getValue().create(param);
            }
        }
        for (EditorFactory factory : PREDICATE_EDITORS) {
            ParamEditor editor = factory.create(param);
            if (editor != null) {
                return editor;
            }
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
     * Fixed columns approximating {@code 130dlu / 225dlu}.
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
