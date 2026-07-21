/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import xapps.gsea.fx.FxEllipsisButton;

/**
 * FX
 * {@code ReportCacheChooserParam}: path/text field + ellipsis → list dialog.
 */
public final class FxReportCacheChooser {
    private final TextField field = new TextField();
    private final HBox root = new HBox(6);
    private final boolean multiInterval;
    private List<FxReportCacheSupport.CachedReport> selected = new ArrayList<>();

    private FxReportCacheChooser(boolean multiInterval) {
        this.multiInterval = multiInterval;
        field.setEditable(true);
        field.textProperty().addListener((obs, o, n) -> {
            if (n == null || n.isBlank()) {
                selected = new ArrayList<>();
                field.setTooltip(null);
                return;
            }
            String joined = selected.stream()
                    .filter(s -> s.reportDir != null)
                    .map(s -> s.reportDir.getPath())
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
            if (!n.equals(joined)) {
                selected = new ArrayList<>();
            }
        });
        HBox.setHgrow(field, Priority.ALWAYS);
        Button ellipsis = FxEllipsisButton.create("Select from report cache");
        ellipsis.setOnAction(e -> openChooser());
        root.getChildren().addAll(field, ellipsis);
        root.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    }

    public static FxReportCacheChooser single() {
        return new FxReportCacheChooser(false);
    }

    public static FxReportCacheChooser multiInterval() {
        return new FxReportCacheChooser(true);
    }

    public HBox getNode() {
        return root;
    }

    public TextField getTextField() {
        return field;
    }

    public List<FxReportCacheSupport.CachedReport> getSelected() {
        if (!selected.isEmpty()) {
            return new ArrayList<>(selected);
        }
        // Typed paths: match against current cache when possible.
        List<File> dirs = getReportDirs();
        if (dirs.isEmpty()) {
            return List.of();
        }
        List<FxReportCacheSupport.CachedReport> reports = FxReportCacheSupport.listGseaReportsWithEdb();
        List<FxReportCacheSupport.CachedReport> matched = new ArrayList<>();
        for (File dir : dirs) {
            String abs = dir.getAbsolutePath();
            for (FxReportCacheSupport.CachedReport r : reports) {
                if (r.reportDir != null && abs.equals(r.reportDir.getAbsolutePath())) {
                    matched.add(r);
                    break;
                }
            }
        }
        return matched;
    }

    public FxReportCacheSupport.CachedReport getSelectedOne() {
        List<FxReportCacheSupport.CachedReport> sels = getSelected();
        return sels.isEmpty() ? null : sels.get(0);
    }

    public void clearSelection() {
        selected = new ArrayList<>();
        field.clear();
        field.setTooltip(null);
    }

    public boolean isSpecified() {
        String t = field.getText();
        return t != null && !t.isBlank();
    }

    public List<File> getReportDirs() {
        String t = field.getText();
        if (t == null || t.isBlank()) {
            return List.of();
        }
        List<File> out = new ArrayList<>();
        for (String part : t.split(",")) {
            String p = part.trim();
            if (!p.isEmpty()) {
                out.add(new File(p));
            }
        }
        return out;
    }

    private void openChooser() {
        Window owner = field.getScene() != null ? field.getScene().getWindow() : null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Select an option");
        dialog.initOwner(owner);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ListView<FxReportCacheSupport.CachedReport> list = new ListView<>();
        list.setPrefSize(550, 400);
        list.setPlaceholder(new Label(""));
        list.getSelectionModel().setSelectionMode(
                multiInterval ? SelectionMode.MULTIPLE : SelectionMode.SINGLE);
        if (multiInterval) {
            final boolean[] enforcing = { false };
            list.getSelectionModel().getSelectedIndices().addListener(
                    (javafx.collections.ListChangeListener<Integer>) c -> {
                        if (enforcing[0]) {
                            return;
                        }
                        while (c.next()) {
                            if (!c.wasAdded() && !c.wasRemoved()) {
                                continue;
                            }
                            var idxs = list.getSelectionModel().getSelectedIndices();
                            if (idxs.size() <= 1) {
                                return;
                            }
                            int min = Integer.MAX_VALUE;
                            int max = Integer.MIN_VALUE;
                            for (Integer i : idxs) {
                                if (i != null) {
                                    min = Math.min(min, i);
                                    max = Math.max(max, i);
                                }
                            }
                            if (max - min + 1 != idxs.size()) {
                                // Keep newly added contiguous interval only.
                                int keepMin = Integer.MAX_VALUE;
                                int keepMax = Integer.MIN_VALUE;
                                for (Integer i : c.getAddedSubList()) {
                                    if (i != null) {
                                        keepMin = Math.min(keepMin, i);
                                        keepMax = Math.max(keepMax, i);
                                    }
                                }
                                if (keepMin <= keepMax) {
                                    enforcing[0] = true;
                                    try {
                                        list.getSelectionModel().clearSelection();
                                        list.getSelectionModel().selectRange(keepMin, keepMax + 1);
                                    } finally {
                                        enforcing[0] = false;
                                    }
                                }
                            }
                            return;
                        }
                    });
        }
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(FxReportCacheSupport.CachedReport item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                setText(item.displayName);
                setTooltip(item.reportDir != null ? new Tooltip(item.reportDir.getPath()) : null);
            }
        });
        List<FxReportCacheSupport.CachedReport> reports = FxReportCacheSupport.listGseaReportsWithEdb();
        list.setItems(FXCollections.observableArrayList(reports));
        // Restore prior selection when possible.
        for (FxReportCacheSupport.CachedReport prev : selected) {
            for (int i = 0; i < reports.size(); i++) {
                FxReportCacheSupport.CachedReport r = reports.get(i);
                if (prev.reportDir != null && r.reportDir != null
                        && prev.reportDir.getAbsolutePath().equals(r.reportDir.getAbsolutePath())) {
                    list.getSelectionModel().select(i);
                }
            }
        }
        list.setPlaceholder(new Label(""));
        VBox body = new VBox(8, list);
        body.setPadding(new Insets(12));
        VBox.setVgrow(list, Priority.ALWAYS);
        dialog.getDialogPane().setContent(body);
        xapps.gsea.fx.FxTheme.apply(dialog);
        xapps.gsea.fx.FxButtons.stylePrimary(
                (Button) dialog.getDialogPane().lookupButton(ButtonType.OK));
        xapps.gsea.fx.FxButtons.styleSecondary(
                (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL));

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                List<FxReportCacheSupport.CachedReport> sels =
                        new ArrayList<>(list.getSelectionModel().getSelectedItems());
                if (sels.isEmpty()) {
                    return;
                }
                selected = sels;
                List<String> paths = new ArrayList<>();
                for (FxReportCacheSupport.CachedReport s : selected) {
                    if (s.reportDir != null) {
                        paths.add(s.reportDir.getPath());
                    }
                }
                field.setText(String.join(",", paths));
                field.setTooltip(paths.isEmpty() ? null : new Tooltip(String.join("\n", paths)));
            }
        });
    }
}
