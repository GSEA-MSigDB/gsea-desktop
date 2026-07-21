/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.util.ArrayList;
import java.util.List;

import org.gsea_msigdb.gsea.ui.api.ParamEditor;
import org.gsea_msigdb.gsea.ui.api.ParamEditors;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import xtools.api.param.GuiParam;
import xtools.api.param.Param;
import xtools.api.param.ParamSet;

/**
 * JavaFX form for a {@link ParamSet}: Required / Basic / Advanced with Show–Hide.
 * Editors for sections that start collapsed are created when the user first shows them
 * (or when syncing values into the form after Last/Reset).
 */
public class FxParamSetForm {

    private final ParamSet paramSet;
    private final List<ParamEditor> editors = new ArrayList<>();
    /** Same order as {@link #editors}, including only params that got an editor. */
    private final List<Param> orderedParams = new ArrayList<>();
    private final VBox root = new VBox(4);
    private final List<Runnable> changeListeners = new ArrayList<>();
    private final List<LazySection> collapsedSections = new ArrayList<>();
    private boolean changeWired;
    private Runnable changeFanout;

    private boolean showBasic;
    private boolean showAdvanced;

    public FxParamSetForm(ParamSet paramSet) {
        this.paramSet = paramSet;
        root.setPadding(new Insets(8, 12, 8, 12));
        root.getStyleClass().add("gsea-param-form");

        Param[] requiredMerged = mergeRequired(paramSet.getParams(Param.REQUIRED, true),
                paramSet.getParams(Param.PSEUDO_REQUIRED, true));
        Param[] basicOpt = paramSet.getParams(Param.BASIC, true);
        Param[] advanced = paramSet.getParams(Param.ADVANCED, true);

        this.showBasic = basicOpt == null || basicOpt.length <= 6;
        this.showAdvanced = false;

        addSection("Required fields", requiredMerged, true);
        addCollapsibleSection("Basic", basicOpt, true);
        addCollapsibleSection("Advanced", advanced, false);

        if (editors.isEmpty() && collapsedSections.isEmpty()) {
            for (int i = 0; i < paramSet.getNumParams(); i++) {
                addParam(paramSet.getParam(i), root);
            }
        }
    }

    private void addSection(String title, Param[] params, boolean alwaysShowHeader) {
        if (!alwaysShowHeader && (params == null || params.length == 0)) {
            return;
        }
        Label header = new Label(title);
        header.getStyleClass().add("gsea-section-header");
        root.getChildren().add(header);
        root.getChildren().add(new Separator());
        if (params == null || params.length == 0) {
            return;
        }
        for (Param p : params) {
            addParam(p, root);
        }
    }

    private void addCollapsibleSection(String title, Param[] params, boolean isBasic) {
        VBox body = new VBox(4);
        boolean shown = isBasic ? showBasic : showAdvanced;
        Param[] ordered = null;
        if (params == null || params.length == 0) {
            body.getChildren().add(new Label(title + " fields - none available"));
        } else {
            ordered = isBasic ? orderBasic(params) : params;
            if (shown) {
                for (Param p : ordered) {
                    addParam(p, body);
                }
            }
        }
        body.setManaged(shown);
        body.setVisible(shown);

        final LazySection lazy;
        if (ordered != null && !shown) {
            lazy = new LazySection(body, ordered);
            collapsedSections.add(lazy);
        } else {
            lazy = null;
        }

        Button toggle = new Button(shown ? "Hide" : "Show");
        xapps.gsea.fx.FxButtons.styleToolbar(toggle);
        Label header = new Label(title + " fields");
        header.getStyleClass().add("gsea-section-header");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox headerRow = new HBox(8, header, spacer, toggle);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        JavaFxParamEditorFactory.setExactWidth(headerRow, JavaFxParamEditorFactory.SECTION_HEADER_WIDTH);

        toggle.setOnAction(e -> {
            boolean next = isBasic ? !showBasic : !showAdvanced;
            if (isBasic) {
                showBasic = next;
            } else {
                showAdvanced = next;
            }
            if (next && lazy != null) {
                lazy.buildEditors();
            }
            body.setManaged(next);
            body.setVisible(next);
            toggle.setText(next ? "Hide" : "Show");
        });

        root.getChildren().add(headerRow);
        root.getChildren().add(new Separator());
        root.getChildren().add(body);
    }

    private void addParam(Param p, VBox parent) {
        if (p == null || isHidden(p)) {
            return;
        }
        ParamEditor editor = ParamEditors.createEditor(p);
        editors.add(editor);
        orderedParams.add(p);
        if (changeWired && changeFanout != null) {
            editor.onChange(changeFanout);
        }
        Node row = JavaFxParamEditorFactory.labeledRow(p, editor);
        parent.getChildren().add(row);
    }

    /** ReportLabelParam first among Basic fields. */
    private static Param[] orderBasic(Param[] params) {
        if (params == null || params.length < 2) {
            return params;
        }
        List<Param> ordered = new ArrayList<>();
        for (Param p : params) {
            if (p instanceof xtools.api.param.ReportLabelParam) {
                ordered.add(0, p);
            } else {
                ordered.add(p);
            }
        }
        return ordered.toArray(new Param[0]);
    }

    /** ReportDirParam last among required / pseudo-required. */
    private static Param[] orderRequired(Param[] params) {
        if (params == null || params.length < 2) {
            return params;
        }
        List<Param> ordered = new ArrayList<>();
        Param reportDir = null;
        for (Param p : params) {
            if (p instanceof xtools.api.param.ReportDirParam) {
                reportDir = p;
            } else {
                ordered.add(p);
            }
        }
        if (reportDir != null) {
            ordered.add(reportDir);
        }
        return ordered.toArray(new Param[0]);
    }

    private static Param[] mergeRequired(Param[] required, Param[] pseudo) {
        List<Param> all = new ArrayList<>();
        if (required != null) {
            for (Param p : required) {
                all.add(p);
            }
        }
        if (pseudo != null) {
            for (Param p : pseudo) {
                all.add(p);
            }
        }
        return orderRequired(all.toArray(new Param[0]));
    }

    /** Skip GuiParam chrome-only entries. */
    private static boolean isHidden(Param p) {
        return p instanceof GuiParam;
    }

    /**
     * Notify {@code listener} whenever any parameter editor fires a change. Editors already expose {@link ParamEditor#onChange(Runnable}}.
     */
    public void addChangeListener(Runnable listener) {
        if (listener == null) {
            return;
        }
        changeListeners.add(listener);
        listener.run();
        if (!changeWired) {
            changeWired = true;
            changeFanout = () -> {
                for (Runnable r : new ArrayList<>(changeListeners)) {
                    try {
                        r.run();
                    } catch (RuntimeException ignored) {
                        // keep other listeners running
                    }
                }
            };
            for (ParamEditor editor : editors) {
                editor.onChange(changeFanout);
            }
        }
    }

    public void commitAll() {
        // Unbuilt sections still hold values on Param objects (defaults / Last fill); only commit widgets.
        for (ParamEditor editor : editors) {
            editor.commitToParam();
        }
    }

    /** Refresh editor widgets from current Param values (e.g. after filling from history). */
    public void syncEditorsFromParams() {
        // Last/Reset may set Advanced values before those editors exist.
        for (LazySection section : collapsedSections) {
            section.buildEditors();
        }
        for (int i = 0; i < editors.size(); i++) {
            Param p = orderedParams.get(i);
            Object v = p.getValue() != null ? p.getValue() : p.getDefault();
            // Empty-array defaults (GeneSetMatrix[], Dataset[], …) must not become [L…;@hash] text.
            if (v != null && v.getClass().isArray() && java.lang.reflect.Array.getLength(v) == 0) {
                v = null;
            }
            editors.get(i).setValue(v);
        }
    }

    public ParamSet getParamSet() {
        return paramSet;
    }

    public ScrollPane getScrollPane() {
        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        return sp;
    }

    public VBox getRoot() {
        return root;
    }

    /** Collapsed section whose editors are created on first Show (or sync). */
    private final class LazySection {
        private final VBox body;
        private final Param[] params;
        private boolean built;

        LazySection(VBox body, Param[] params) {
            this.body = body;
            this.params = params;
        }

        void buildEditors() {
            if (built) {
                return;
            }
            built = true;
            for (Param p : params) {
                addParam(p, body);
            }
        }
    }
}
