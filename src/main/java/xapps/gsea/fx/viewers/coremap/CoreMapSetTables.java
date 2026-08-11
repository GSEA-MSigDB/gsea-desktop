/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.coremap;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import edu.mit.broad.coremap.CoreMapTypes.GeneSetSummary;
import edu.mit.broad.coremap.CoreMapTypes.IntegrationResult;
import edu.mit.broad.coremap.CoreMapTypes.SetEnrichmentMetric;
import edu.mit.broad.coremap.EnrichmentMath;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Mechanistic and phenotypic gene-set picker tables for the CoreMap setup stage.
 */
final class CoreMapSetTables {

    interface Host {
        /** Threshold triple: min |NES|, max NOM p, max FDR for the layer. */
        double[] thresholdsForLayer(boolean mechanistic);
    }

    private static final class Layer {
        final boolean mechanistic;
        final TableView<CoreMapGeneSetRow> table = new TableView<>();
        final ObservableList<CoreMapGeneSetRow> rows = FXCollections.observableArrayList();
        final FilteredList<CoreMapGeneSetRow> filtered = new FilteredList<>(rows, r -> true);
        final SortedList<CoreMapGeneSetRow> sorted = new SortedList<>(filtered);
        final TextField search = new TextField();
        final CheckBox showFailing = new CheckBox("Show below-threshold");
        final Label status;
        final Button followBtn = new Button("Reapply Filters");
        final Button clearBtn = new Button("Clear");
        List<GeneSetSummary> catalog = List.of();
        boolean followThresholds = true;
        Set<String> pendingSetIds;

        Layer(boolean mechanistic) {
            this.mechanistic = mechanistic;
            status = new Label(mechanistic
                    ? "Load a mechanistic GSEA result."
                    : "Load a phenotypic GSEA result.");
        }
    }

    private final Host host;
    private final Layer mech = new Layer(true);
    private final Layer pheno = new Layer(false);
    private boolean applyingThresholdRefresh;

    CoreMapSetTables(Host host) {
        this.host = host;
        configureSetTable(mech.table, mech.sorted);
        configureSetTable(pheno.table, pheno.sorted);
        wireGeneSetPickerControls();
        mech.showFailing.setSelected(true);
        pheno.showFailing.setSelected(true);
        mech.search.setPromptText("Search gene sets…");
        pheno.search.setPromptText("Search gene sets…");
    }

    void styleSecondaryButtons(java.util.function.Consumer<Button> styler) {
        styler.accept(mech.followBtn);
        styler.accept(mech.clearBtn);
        styler.accept(pheno.followBtn);
        styler.accept(pheno.clearBtn);
    }

    SplitPane buildSplitPane() {
        VBox mechPicker = geneSetPickerPanel("Mechanistic gene sets", mech);
        VBox phenoPicker = geneSetPickerPanel("Phenotypic gene sets", pheno);
        SplitPane setSplit = new SplitPane(mechPicker, phenoPicker);
        setSplit.setDividerPositions(0.5);
        VBox.setVgrow(setSplit, Priority.ALWAYS);
        return setSplit;
    }

    void setDisable(boolean disable) {
        mech.table.setDisable(disable);
        pheno.table.setDisable(disable);
    }

    boolean hasCatalog() {
        return !mech.catalog.isEmpty() || !pheno.catalog.isEmpty();
    }

    void setCatalog(boolean mechanistic, List<GeneSetSummary> catalog) {
        layer(mechanistic).catalog = catalog != null ? catalog : List.of();
    }

    void setPendingSetIds(boolean mechanistic, Set<String> ids) {
        layer(mechanistic).pendingSetIds = ids != null ? new HashSet<>(ids) : null;
    }

    Set<String> consumePendingSetIds(boolean mechanistic) {
        Layer l = layer(mechanistic);
        Set<String> ids = l.pendingSetIds;
        l.pendingSetIds = null;
        return ids;
    }

    void setFollowThresholds(boolean mechanistic, boolean follow) {
        layer(mechanistic).followThresholds = follow;
    }

    void clearLayer(boolean mechanistic) {
        Layer l = layer(mechanistic);
        l.catalog = List.of();
        l.rows.clear();
        l.followThresholds = true;
        l.pendingSetIds = null;
        updateSetStatusLabels();
        updateGeneSetFilters();
    }

    void rebuildFromCatalog(boolean mechanistic) {
        Layer l = layer(mechanistic);
        l.rows.clear();
        for (GeneSetSummary s : l.catalog) {
            CoreMapGeneSetRow r = CoreMapGeneSetRow.from(s);
            r.included.addListener((o, a, b) -> onIncludedToggled(mechanistic));
            l.rows.add(r);
        }
    }

    void refreshLiveSelection(boolean mechanistic) {
        Layer l = layer(mechanistic);
        double[] thresholds = host.thresholdsForLayer(mechanistic);
        double minN = thresholds[0];
        double maxP = thresholds[1];
        double maxF = thresholds[2];
        applyingThresholdRefresh = true;
        try {
            for (CoreMapGeneSetRow r : l.rows) {
                Double nes = Double.isNaN(r.nes.get()) ? null : r.nes.get();
                Double np = Double.isNaN(r.pValue.get()) ? null : r.pValue.get();
                Double fdr = Double.isNaN(r.fdr.get()) ? null : r.fdr.get();
                boolean passes = EnrichmentMath.passesThresholds(nes, np, fdr, minN, maxP, maxF);
                r.passed.set(passes);
                if (l.followThresholds) {
                    r.included.set(passes);
                }
            }
            sortRows(l.rows);
        } finally {
            applyingThresholdRefresh = false;
        }
        updateGeneSetFilters();
        updateSetStatusLabels();
    }

    void applyInclusion(boolean mechanistic, Set<String> setIds) {
        Layer l = layer(mechanistic);
        l.followThresholds = false;
        Set<String> ids = setIds != null ? setIds : Set.of();
        double[] thresholds = host.thresholdsForLayer(mechanistic);
        double minN = thresholds[0];
        double maxP = thresholds[1];
        double maxF = thresholds[2];
        applyingThresholdRefresh = true;
        try {
            for (CoreMapGeneSetRow r : l.rows) {
                Double nes = Double.isNaN(r.nes.get()) ? null : r.nes.get();
                Double np = Double.isNaN(r.pValue.get()) ? null : r.pValue.get();
                Double fdr = Double.isNaN(r.fdr.get()) ? null : r.fdr.get();
                r.passed.set(EnrichmentMath.passesThresholds(nes, np, fdr, minN, maxP, maxF));
                r.included.set(ids.contains(r.setId.get()));
            }
            sortRows(l.rows);
        } finally {
            applyingThresholdRefresh = false;
        }
        updateGeneSetFilters();
        updateSetStatusLabels();
    }

    void fillFromJob(IntegrationResult result, boolean mechanistic, Set<String> selectedIds) {
        Layer l = layer(mechanistic);
        l.rows.clear();
        if (result == null || result.setEnrichments == null) {
            return;
        }
        double[] thresholds = host.thresholdsForLayer(mechanistic);
        double minN = thresholds[0];
        double maxP = thresholds[1];
        double maxF = thresholds[2];
        for (SetEnrichmentMetric m : result.setEnrichments) {
            if (m.layer == null) {
                continue;
            }
            boolean mech = m.layer.wire().equals("mechanistic");
            if (mech != mechanistic) {
                continue;
            }
            CoreMapGeneSetRow r = new CoreMapGeneSetRow();
            r.setId.set(m.setId);
            r.nes.set(m.nes != null ? m.nes : Double.NaN);
            r.pValue.set(m.pValue != null ? m.pValue : Double.NaN);
            r.fdr.set(m.fdr != null ? m.fdr : Double.NaN);
            r.leCount.set(0);
            r.passed.set(EnrichmentMath.passesThresholds(m.nes, m.pValue, m.fdr, minN, maxP, maxF));
            r.included.set(selectedIds != null && selectedIds.contains(m.setId));
            r.included.addListener((o, a, b) -> onIncludedToggled(mechanistic));
            l.rows.add(r);
        }
    }

    void updateFiltersAndStatus() {
        updateGeneSetFilters();
        updateSetStatusLabels();
    }

    Set<String> mechanisticSetIds() {
        return includedSetIds(mech.rows);
    }

    Set<String> phenotypicSetIds() {
        return includedSetIds(pheno.rows);
    }

    private Layer layer(boolean mechanistic) {
        return mechanistic ? mech : pheno;
    }

    private void wireGeneSetPickerControls() {
        mech.followBtn.setOnAction(e -> {
            mech.followThresholds = true;
            refreshLiveSelection(true);
        });
        pheno.followBtn.setOnAction(e -> {
            pheno.followThresholds = true;
            refreshLiveSelection(false);
        });
        mech.clearBtn.setOnAction(e -> clearManualSelection(true));
        pheno.clearBtn.setOnAction(e -> clearManualSelection(false));
        mech.search.textProperty().addListener((o, a, b) -> updateGeneSetFilters());
        pheno.search.textProperty().addListener((o, a, b) -> updateGeneSetFilters());
        mech.showFailing.selectedProperty().addListener((o, a, b) -> updateGeneSetFilters());
        pheno.showFailing.selectedProperty().addListener((o, a, b) -> updateGeneSetFilters());
    }

    private void clearManualSelection(boolean mechanistic) {
        Layer l = layer(mechanistic);
        l.followThresholds = false;
        applyingThresholdRefresh = true;
        try {
            for (CoreMapGeneSetRow r : l.rows) {
                r.included.set(false);
            }
        } finally {
            applyingThresholdRefresh = false;
        }
        updateSetStatusLabels();
        updateGeneSetFilters();
    }

    private void onIncludedToggled(boolean mechanistic) {
        if (applyingThresholdRefresh) {
            return;
        }
        layer(mechanistic).followThresholds = false;
        updateSetStatusLabels();
    }

    private static void configureSetTable(TableView<CoreMapGeneSetRow> table, SortedList<CoreMapGeneSetRow> rows) {
        table.setItems(rows);
        rows.comparatorProperty().bind(table.comparatorProperty());
        table.setEditable(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        TableColumn<CoreMapGeneSetRow, Boolean> include = new TableColumn<>("In");
        include.setCellValueFactory(c -> c.getValue().included);
        include.setCellFactory(CheckBoxTableCell.forTableColumn(include));
        include.setEditable(true);
        include.setPrefWidth(40);

        TableColumn<CoreMapGeneSetRow, Boolean> pass = new TableColumn<>("Pass");
        pass.setCellValueFactory(c -> c.getValue().passed);
        pass.setPrefWidth(50);
        pass.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    getStyleClass().removeAll("gsea-text-pass", "gsea-text-fail");
                } else {
                    setText(item ? "pass" : "fail");
                    getStyleClass().removeAll("gsea-text-pass", "gsea-text-fail");
                    getStyleClass().add(item ? "gsea-text-pass" : "gsea-text-fail");
                }
            }
        });

        TableColumn<CoreMapGeneSetRow, String> id = new TableColumn<>("Gene set");
        id.setCellValueFactory(c -> c.getValue().setId);
        id.setPrefWidth(200);

        TableColumn<CoreMapGeneSetRow, Number> nes = new TableColumn<>("NES");
        nes.setCellValueFactory(c -> c.getValue().nes);
        nes.setPrefWidth(70);

        TableColumn<CoreMapGeneSetRow, Number> np = new TableColumn<>("NOM p-Val");
        np.setCellValueFactory(c -> c.getValue().pValue);
        np.setPrefWidth(70);

        TableColumn<CoreMapGeneSetRow, Number> fdr = new TableColumn<>("FDR");
        fdr.setCellValueFactory(c -> c.getValue().fdr);
        fdr.setPrefWidth(70);

        TableColumn<CoreMapGeneSetRow, Number> n = new TableColumn<>("LE");
        n.setCellValueFactory(c -> c.getValue().leCount);
        n.setPrefWidth(50);

        table.getColumns().setAll(include, pass, id, nes, np, fdr, n);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private static VBox geneSetPickerPanel(String title, Layer layer) {
        Label heading = new Label(title);
        heading.getStyleClass().add("coremap-card-heading");
        layer.status.getStyleClass().addAll("gsea-muted", "coremap-meta");
        HBox tools = new HBox(8, layer.followBtn, layer.clearBtn, layer.showFailing);
        tools.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(layer.search, Priority.ALWAYS);
        VBox box = new VBox(6, heading, layer.status, layer.search, tools, layer.table);
        VBox.setVgrow(layer.table, Priority.ALWAYS);
        return box;
    }

    private void updateGeneSetFilters() {
        String mechQ = mech.search.getText() != null ? mech.search.getText().trim().toLowerCase(Locale.ROOT) : "";
        String phenoQ = pheno.search.getText() != null ? pheno.search.getText().trim().toLowerCase(Locale.ROOT) : "";
        boolean mechShow = mech.showFailing.isSelected();
        boolean phenoShow = pheno.showFailing.isSelected();
        mech.filtered.setPredicate(r -> rowMatchesFilter(r, mechQ, mechShow));
        pheno.filtered.setPredicate(r -> rowMatchesFilter(r, phenoQ, phenoShow));
    }

    private static boolean rowMatchesFilter(CoreMapGeneSetRow r, String query, boolean showFailing) {
        if (!showFailing && !r.passed.get()) {
            return false;
        }
        if (query.isEmpty()) {
            return true;
        }
        String id = r.setId.get() != null ? r.setId.get().toLowerCase(Locale.ROOT) : "";
        return id.contains(query);
    }

    private void updateSetStatusLabels() {
        mech.status.setText(formatSetStatus(mech.rows, mech.followThresholds));
        pheno.status.setText(formatSetStatus(pheno.rows, pheno.followThresholds));
    }

    private static String formatSetStatus(ObservableList<CoreMapGeneSetRow> rows, boolean follow) {
        if (rows.isEmpty()) {
            return "Load a GSEA result.";
        }
        int included = 0;
        int passing = 0;
        for (CoreMapGeneSetRow r : rows) {
            if (r.included.get()) {
                included++;
            }
            if (r.passed.get()) {
                passing++;
            }
        }
        String mode = follow ? "following thresholds" : "manual selection";
        return String.format(Locale.ROOT, "%d selected · %d/%d pass · %s",
                included, passing, rows.size(), mode);
    }

    private static Set<String> includedSetIds(ObservableList<CoreMapGeneSetRow> rows) {
        Set<String> ids = new HashSet<>();
        for (CoreMapGeneSetRow r : rows) {
            if (r.included.get()) {
                ids.add(r.setId.get());
            }
        }
        return ids;
    }

    private static void sortRows(ObservableList<CoreMapGeneSetRow> rows) {
        rows.sort((a, b) -> {
            int byPass = Boolean.compare(b.passed.get(), a.passed.get());
            if (byPass != 0) {
                return byPass;
            }
            double an = Double.isNaN(a.nes.get()) ? 0 : Math.abs(a.nes.get());
            double bn = Double.isNaN(b.nes.get()) ? 0 : Math.abs(b.nes.get());
            return Double.compare(bn, an);
        });
    }
}
