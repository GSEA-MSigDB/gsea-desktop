/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.coremap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import edu.mit.broad.coremap.CoreMapTypes.Bridge;
import edu.mit.broad.coremap.CoreMapTypes.CascadeHop;
import edu.mit.broad.coremap.CoreMapTypes.IntegrationResult;
import edu.mit.broad.coremap.CoreMapTypes.LayerHubSummary;
import edu.mit.broad.coremap.CoreMapTypes.SharedDriverSummary;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Graph-adjacent insight lists (bridges / drivers / hubs) as ranked cards —
 * CoreMap {@code InsightsSidebar} equivalent for JavaFX.
 */
public final class CoreMapInsightSidebar extends VBox {

    public interface SelectionListener {
        void onBridgesSelected(List<Bridge> bridges);

        void onDriversSelected(List<SharedDriverSummary> drivers);

        void onHubsSelected(List<LayerHubSummary> hubs);

        void onClearRequested();
    }

    private final TabPane tabs = new TabPane();
    private final ObservableList<Bridge> bridgeItems = FXCollections.observableArrayList();
    private final ObservableList<SharedDriverSummary> driverItems = FXCollections.observableArrayList();
    private final ObservableList<LayerHubSummary> hubItems = FXCollections.observableArrayList();
    private final FilteredList<Bridge> bridgesFiltered = new FilteredList<>(bridgeItems, b -> true);
    private final FilteredList<SharedDriverSummary> driversFiltered = new FilteredList<>(driverItems, d -> true);
    private final FilteredList<LayerHubSummary> hubsFiltered = new FilteredList<>(hubItems, h -> true);

    private final ListView<Bridge> bridgeList = new ListView<>(bridgesFiltered);
    private final ListView<SharedDriverSummary> driverList = new ListView<>(driversFiltered);
    private final ListView<LayerHubSummary> hubList = new ListView<>(hubsFiltered);

    private final TextField bridgeSearch = new TextField();
    private final TextField driverSearch = new TextField();
    private final TextField hubSearch = new TextField();
    private final ComboBox<String> bridgeSort = new ComboBox<>(FXCollections.observableArrayList(
            "Score", "Mechanistic set", "Phenotypic set"));
    private final ComboBox<String> hubLayer = new ComboBox<>(FXCollections.observableArrayList(
            "All layers", "Mechanistic", "Phenotypic"));
    private final Label bridgeHelp = new Label(
            "Ranked paths from mechanistic to phenotypic sets. "
                    + "Click to focus; Ctrl/Cmd+click to multi-select.");
    private final Label driverHelp = new Label(
            "Genes in both layers’ leading edges, ranked by effect size and sign agreement. "
                    + "Click to focus; Ctrl/Cmd+click to multi-select.");
    private final Label hubHelp = new Label(
            "Highly connected genes within one enrichment layer (works with one or both layers). "
                    + "Click to focus; Ctrl/Cmd+click to multi-select.");
    private final Label bridgeEmpty = new Label();
    private final Label driverEmpty = new Label();
    private final Label hubEmpty = new Label();
    private final Label bridgeFilterCount = new Label();
    private final HBox selectionBar = new HBox(8);
    private final Label selectionCount = new Label();
    private final Button clearSelectionBtn = new Button("Clear");

    private SelectionListener listener;
    private final List<Bridge> selectedBridges = new ArrayList<>();
    private final List<String> selectedGenes = new ArrayList<>();
    private boolean suppressBridgeNotify;
    private boolean suppressGeneNotify;
    private IntegrationResult lastResult;

    public CoreMapInsightSidebar() {
        getStyleClass().add("coremap-insight");
        setSpacing(0);
        setPadding(Insets.EMPTY);
        setPrefWidth(340);
        setMinWidth(260);
        bridgeSort.getSelectionModel().select("Score");
        hubLayer.getSelectionModel().select("All layers");
        bridgeSearch.setPromptText("Set, gene, mechanism…");
        driverSearch.setPromptText("Gene or set ID…");
        hubSearch.setPromptText("Search hubs…");
        styleHelp(bridgeHelp);
        styleHelp(driverHelp);
        styleHelp(hubHelp);
        styleMuted(bridgeEmpty);
        styleMuted(driverEmpty);
        styleMuted(hubEmpty);
        styleMuted(bridgeFilterCount);
        bridgeEmpty.setManaged(false);
        bridgeEmpty.setVisible(false);
        driverEmpty.setManaged(false);
        driverEmpty.setVisible(false);
        hubEmpty.setManaged(false);
        hubEmpty.setVisible(false);
        bridgeFilterCount.setManaged(false);
        bridgeFilterCount.setVisible(false);

        selectionCount.getStyleClass().add("coremap-meta");
        clearSelectionBtn.getStyleClass().add("coremap-meta");
        selectionBar.getChildren().setAll(selectionCount, clearSelectionBtn);
        selectionBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        selectionBar.setPadding(new Insets(0, 0, 4, 0));
        selectionBar.setManaged(false);
        selectionBar.setVisible(false);
        clearSelectionBtn.setOnAction(e -> {
            clearUiSelection();
            if (listener != null) {
                listener.onClearRequested();
            }
        });

        bridgeList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        driverList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        hubList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        bridgeList.setCellFactory(lv -> {
            BridgeCardCell cell = new BridgeCardCell();
            // Constrain cell width so Label wrapText / TextFlow wrap actually engage.
            cell.prefWidthProperty().bind(lv.widthProperty().subtract(18));
            return cell;
        });
        driverList.setCellFactory(lv -> {
            DriverCardCell cell = new DriverCardCell();
            cell.prefWidthProperty().bind(lv.widthProperty().subtract(18));
            return cell;
        });
        hubList.setCellFactory(lv -> {
            HubCardCell cell = new HubCardCell();
            cell.prefWidthProperty().bind(lv.widthProperty().subtract(18));
            return cell;
        });

        bridgeList.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Bridge>) c -> {
            if (suppressBridgeNotify) {
                return;
            }
            selectedBridges.clear();
            selectedBridges.addAll(bridgeList.getSelectionModel().getSelectedItems());
            selectedGenes.clear();
            clearPeerGeneLists();
            updateSelectionBar();
            refreshLists();
            if (listener == null) {
                return;
            }
            if (selectedBridges.isEmpty()) {
                listener.onClearRequested();
            } else {
                listener.onBridgesSelected(List.copyOf(selectedBridges));
            }
        });
        driverList.getSelectionModel().getSelectedItems().addListener((ListChangeListener<SharedDriverSummary>) c -> {
            if (suppressGeneNotify) {
                return;
            }
            List<SharedDriverSummary> selected = List.copyOf(driverList.getSelectionModel().getSelectedItems());
            if (selected.isEmpty()) {
                return;
            }
            withBridgeSuppress(() -> bridgeList.getSelectionModel().clearSelection());
            withGeneSuppress(() -> hubList.getSelectionModel().clearSelection());
            selectedBridges.clear();
            selectedGenes.clear();
            for (SharedDriverSummary d : selected) {
                if (d.gene != null) {
                    selectedGenes.add(d.gene);
                }
            }
            updateSelectionBar();
            refreshLists();
            if (listener != null) {
                listener.onDriversSelected(selected);
            }
        });
        hubList.getSelectionModel().getSelectedItems().addListener((ListChangeListener<LayerHubSummary>) c -> {
            if (suppressGeneNotify) {
                return;
            }
            List<LayerHubSummary> selected = List.copyOf(hubList.getSelectionModel().getSelectedItems());
            if (selected.isEmpty()) {
                return;
            }
            withBridgeSuppress(() -> bridgeList.getSelectionModel().clearSelection());
            withGeneSuppress(() -> driverList.getSelectionModel().clearSelection());
            selectedBridges.clear();
            selectedGenes.clear();
            for (LayerHubSummary h : selected) {
                if (h.gene != null) {
                    selectedGenes.add(h.gene);
                }
            }
            updateSelectionBar();
            refreshLists();
            if (listener != null) {
                listener.onHubsSelected(selected);
            }
        });

        bridgeSearch.textProperty().addListener((o, a, b) -> updateBridgeFilter());
        driverSearch.textProperty().addListener((o, a, b) -> updateDriverFilter());
        hubSearch.textProperty().addListener((o, a, b) -> updateHubFilter());
        bridgeSort.valueProperty().addListener((o, a, b) -> sortBridges());
        hubLayer.valueProperty().addListener((o, a, b) -> updateHubFilter());

        VBox bridgesPane = panel(bridgeHelp, toolbar(bridgeSearch, bridgeSort),
                selectionBar, bridgeFilterCount, bridgeEmpty, bridgeList);
        VBox driversPane = panel(driverHelp, toolbar(driverSearch, null), driverEmpty, driverList);
        VBox hubsPane = panel(hubHelp, toolbar(hubSearch, hubLayer), hubEmpty, hubList);

        tabs.getTabs().setAll(
                new Tab("Bridges", bridgesPane),
                new Tab("Drivers", driversPane),
                new Tab("Hubs", hubsPane));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        getChildren().add(tabs);
    }

    public void setSelectionListener(SelectionListener listener) {
        this.listener = listener;
    }

    public void setResult(IntegrationResult result) {
        lastResult = result;
        bridgeItems.clear();
        driverItems.clear();
        hubItems.clear();
        selectedBridges.clear();
        selectedGenes.clear();
        if (result == null) {
            updateEmptyStates();
            updateSelectionBar();
            return;
        }
        if (result.bridges != null) {
            bridgeItems.addAll(result.bridges);
        }
        if (result.sharedDrivers != null) {
            driverItems.addAll(result.sharedDrivers);
        }
        if (result.layerHubs != null) {
            hubItems.addAll(result.layerHubs);
        }
        sortBridges();
        updateHubLayerOptions();
        updateBridgeFilter();
        updateDriverFilter();
        updateHubFilter();
        suppressBridgeNotify = true;
        try {
            bridgeList.getSelectionModel().clearSelection();
            driverList.getSelectionModel().clearSelection();
            hubList.getSelectionModel().clearSelection();
        } finally {
            suppressBridgeNotify = false;
        }
        updateEmptyStates();
        updateSelectionBar();
    }

    public void clearUiSelection() {
        selectedBridges.clear();
        selectedGenes.clear();
        withBridgeSuppress(() -> bridgeList.getSelectionModel().clearSelection());
        withGeneSuppress(() -> {
            driverList.getSelectionModel().clearSelection();
            hubList.getSelectionModel().clearSelection();
        });
        updateSelectionBar();
        refreshLists();
    }

    private void withBridgeSuppress(Runnable action) {
        suppressBridgeNotify = true;
        try {
            action.run();
        } finally {
            suppressBridgeNotify = false;
        }
    }

    private void withGeneSuppress(Runnable action) {
        suppressGeneNotify = true;
        try {
            action.run();
        } finally {
            suppressGeneNotify = false;
        }
    }

    private void clearPeerGeneLists() {
        withGeneSuppress(() -> {
            driverList.getSelectionModel().clearSelection();
            hubList.getSelectionModel().clearSelection();
        });
    }

    private void refreshLists() {
        bridgeList.refresh();
        driverList.refresh();
        hubList.refresh();
    }

    private void updateSelectionBar() {
        int n = selectedBridges.size() + selectedGenes.size();
        boolean show = n > 0;
        selectionBar.setManaged(show);
        selectionBar.setVisible(show);
        if (!selectedBridges.isEmpty()) {
            selectionCount.setText(selectedBridges.size() + " selected");
        } else {
            selectionCount.setText(selectedGenes.size() + " selected");
        }
    }

    private void sortBridges() {
        List<String> keepKeys = new ArrayList<>();
        for (Bridge b : selectedBridges) {
            keepKeys.add(CoreMapCascadeUi.bridgeKey(b));
        }
        String sort = bridgeSort.getValue() != null ? bridgeSort.getValue() : "Score";
        Comparator<Bridge> cmp = switch (sort) {
            case "Mechanistic set" -> Comparator.comparing(CoreMapCascadeUi::mechLabel,
                    String.CASE_INSENSITIVE_ORDER);
            case "Phenotypic set" -> Comparator.comparing(CoreMapCascadeUi::phenoLabel,
                    String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparingDouble((Bridge b) -> b.bridgeScore).reversed();
        };
        List<Bridge> copy = new ArrayList<>(bridgeItems);
        copy.sort(cmp);
        withBridgeSuppress(() -> {
            bridgeItems.setAll(copy);
            bridgeList.getSelectionModel().clearSelection();
            for (Bridge b : bridgesFiltered) {
                if (keepKeys.contains(CoreMapCascadeUi.bridgeKey(b))) {
                    bridgeList.getSelectionModel().select(b);
                }
            }
        });
        selectedBridges.clear();
        selectedBridges.addAll(bridgeList.getSelectionModel().getSelectedItems());
        updateSelectionBar();
        refreshLists();
        if (listener == null) {
            return;
        }
        if (selectedBridges.isEmpty()) {
            if (!keepKeys.isEmpty()) {
                listener.onClearRequested();
            }
        } else {
            listener.onBridgesSelected(List.copyOf(selectedBridges));
        }
    }

    private void updateBridgeFilter() {
        String q = bridgeSearch.getText() != null ? bridgeSearch.getText().trim().toLowerCase(Locale.ROOT) : "";
        bridgesFiltered.setPredicate(b -> q.isEmpty() || bridgeHaystack(b).contains(q));
        updateEmptyStates();
        String raw = bridgeSearch.getText() != null ? bridgeSearch.getText().trim() : "";
        boolean filterActive = !raw.isEmpty() && !bridgeItems.isEmpty() && !bridgesFiltered.isEmpty();
        bridgeFilterCount.setManaged(filterActive);
        bridgeFilterCount.setVisible(filterActive);
        if (filterActive) {
            bridgeFilterCount.setText("Showing " + bridgesFiltered.size() + " of " + bridgeItems.size());
        }
    }

    private void updateDriverFilter() {
        String q = driverSearch.getText() != null ? driverSearch.getText().trim().toLowerCase(Locale.ROOT) : "";
        driversFiltered.setPredicate(d -> {
            if (q.isEmpty()) {
                return true;
            }
            StringBuilder sb = new StringBuilder(d.gene != null ? d.gene : "");
            if (d.mechanisticSets != null) {
                sb.append(' ').append(String.join(" ", d.mechanisticSets));
            }
            if (d.phenotypicSets != null) {
                sb.append(' ').append(String.join(" ", d.phenotypicSets));
            }
            return sb.toString().toLowerCase(Locale.ROOT).contains(q);
        });
        updateEmptyStates();
    }

    private void updateHubFilter() {
        String q = hubSearch.getText() != null ? hubSearch.getText().trim().toLowerCase(Locale.ROOT) : "";
        String layer = hubLayer.getValue() != null ? hubLayer.getValue() : "All layers";
        hubsFiltered.setPredicate(h -> {
            if (!"All layers".equals(layer) && h.layer != null) {
                if ("Mechanistic".equals(layer) && !h.layer.wire().equals("mechanistic")) {
                    return false;
                }
                if ("Phenotypic".equals(layer) && !h.layer.wire().equals("phenotypic")) {
                    return false;
                }
            }
            if (q.isEmpty()) {
                return true;
            }
            String hay = (h.gene != null ? h.gene : "") + " "
                    + (h.layer != null ? h.layer.wire() : "") + " "
                    + (h.sets != null ? String.join(" ", h.sets) : "");
            return hay.toLowerCase(Locale.ROOT).contains(q);
        });
        updateEmptyStates();
    }

    /** Offer only layers that have hubs (CoreMap InsightsSidebar). */
    private void updateHubLayerOptions() {
        boolean hasMech = false;
        boolean hasPheno = false;
        for (LayerHubSummary h : hubItems) {
            if (h == null || h.layer == null) {
                continue;
            }
            if ("mechanistic".equals(h.layer.wire())) {
                hasMech = true;
            } else if ("phenotypic".equals(h.layer.wire())) {
                hasPheno = true;
            }
        }
        String prev = hubLayer.getValue();
        java.util.List<String> items = new java.util.ArrayList<>();
        items.add("All layers");
        if (hasMech) {
            items.add("Mechanistic");
        }
        if (hasPheno) {
            items.add("Phenotypic");
        }
        hubLayer.getItems().setAll(items);
        if (prev != null && items.contains(prev)) {
            hubLayer.getSelectionModel().select(prev);
        } else {
            hubLayer.getSelectionModel().select("All layers");
        }
    }

    private void updateEmptyStates() {
        setEmpty(bridgeEmpty, bridgeItems.isEmpty(),
                partialLayerMessage(
                        "Bridges need both mechanistic and phenotypic layers. Only one layer is loaded.",
                        "No bridges."),
                !bridgeItems.isEmpty() && bridgesFiltered.isEmpty(),
                "No bridges match “" + safeQuery(bridgeSearch) + "”.");
        setEmpty(driverEmpty, driverItems.isEmpty(),
                partialLayerMessage("Shared drivers need genes in both layers.", "No shared drivers."),
                !driverItems.isEmpty() && driversFiltered.isEmpty(),
                "No drivers match “" + safeQuery(driverSearch) + "”.");
        setEmpty(hubEmpty, hubItems.isEmpty(),
                partialLayerMessage(
                        "No hubs yet. Need at least two leading-edge genes from the same layer connected in the interactome.",
                        "No hubs."),
                !hubItems.isEmpty() && hubsFiltered.isEmpty(),
                "No hubs match the current filter.");
    }

    private String partialLayerMessage(String partial, String empty) {
        if (isPartialSingleLayer(lastResult)) {
            return partial;
        }
        return empty;
    }

    /** True when exactly one enrichment layer contributed genes. */
    public static boolean isPartialSingleLayer(IntegrationResult result) {
        if (result == null) {
            return false;
        }
        double mechN = geneCount(result, true);
        double phenoN = geneCount(result, false);
        return (mechN == 0) != (phenoN == 0);
    }

    private static double geneCount(IntegrationResult result, boolean mechanistic) {
        if (result.stats != null) {
            String primary = mechanistic ? "mechanistic_gene_count" : "phenotypic_gene_count";
            String alias = mechanistic ? "mechanistic_genes" : "phenotypic_genes";
            Object v = result.stats.get(primary);
            if (v == null) {
                v = result.stats.get(alias);
            }
            if (v instanceof Number n) {
                return n.doubleValue();
            }
        }
        // Fallback: scan set enrichments / elements if stats missing (saved jobs).
        if (result.setEnrichments != null) {
            String layer = mechanistic ? "mechanistic" : "phenotypic";
            for (var m : result.setEnrichments) {
                if (m != null && m.layer != null && layer.equals(m.layer.wire())) {
                    return 1;
                }
            }
        }
        return 0;
    }

    /** Prefer Hubs after a partial single-layer build (CoreMap explore default). */
    public void selectPreferredInsightTab() {
        if (isPartialSingleLayer(lastResult)) {
            tabs.getSelectionModel().select(2); // Hubs
        } else {
            tabs.getSelectionModel().select(0); // Bridges
        }
    }

    private static String safeQuery(TextField field) {
        return field.getText() != null ? field.getText().trim() : "";
    }

    private static void setEmpty(Label empty, boolean noItems, String noItemsMsg,
            boolean noMatch, String noMatchMsg) {
        if (noItems) {
            empty.setText(noItemsMsg);
            empty.setManaged(true);
            empty.setVisible(true);
        } else if (noMatch) {
            empty.setText(noMatchMsg);
            empty.setManaged(true);
            empty.setVisible(true);
        } else {
            empty.setManaged(false);
            empty.setVisible(false);
        }
    }

    private static String bridgeHaystack(Bridge b) {
        StringBuilder sb = new StringBuilder();
        sb.append(CoreMapCascadeUi.mechLabel(b)).append(' ')
                .append(CoreMapCascadeUi.phenoLabel(b)).append(' ');
        if (b.nodes != null) {
            sb.append(String.join(" ", b.nodes)).append(' ');
        }
        if (b.sharedDrivers != null) {
            sb.append(String.join(" ", b.sharedDrivers)).append(' ');
        }
        if (b.hops != null) {
            for (CascadeHop hop : b.hops) {
                if (hop.effect != null) {
                    sb.append(hop.effect).append(' ');
                }
                if (hop.mechanism != null) {
                    sb.append(hop.mechanism).append(' ');
                }
            }
        }
        if (b.supportingPaths != null) {
            for (var support : b.supportingPaths) {
                if (support.nodes != null) {
                    sb.append(String.join(" ", support.nodes)).append(' ');
                }
            }
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    private static void styleHelp(Label help) {
        help.getStyleClass().addAll("gsea-muted", "coremap-help");
        help.setWrapText(true);
        help.setMaxWidth(Double.MAX_VALUE);
    }

    private static void styleMuted(Label label) {
        label.getStyleClass().addAll("gsea-muted", "coremap-meta");
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
    }

    /** Bind wrap lengths so long titles/meta reflow inside the sidebar ListView. */
    private static void bindCardWrap(ListCell<?> cell, VBox card, Label title, javafx.scene.Node cascade) {
        card.setMaxWidth(Double.MAX_VALUE);
        title.getStyleClass().add("coremap-card-title");
        title.setWrapText(true);
        applyInsightWrap(card, Math.max(120, cell.getWidth() - 24));
    }

    private static void applyInsightWrap(javafx.scene.Node node, double wrap) {
        if (node == null || wrap < 40) {
            return;
        }
        if (node instanceof Label l) {
            l.setMaxWidth(wrap);
            l.setWrapText(true);
        } else if (node instanceof javafx.scene.text.TextFlow tf) {
            // TextFlow wraps to its laid-out width (no prefWrapLength API).
            tf.setPrefWidth(wrap);
            tf.setMaxWidth(wrap);
        } else if (node instanceof javafx.scene.Parent p) {
            for (javafx.scene.Node child : p.getChildrenUnmodifiable()) {
                applyInsightWrap(child, wrap);
            }
        }
    }

    private static void installWrapListener(ListCell<?> cell) {
        cell.widthProperty().addListener((o, a, b) -> {
            if (cell.getGraphic() != null) {
                applyInsightWrap(cell.getGraphic(), Math.max(120, b.doubleValue() - 24));
            }
        });
    }

    private static VBox panel(Label help, HBox toolbar, javafx.scene.Node... rest) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(8));
        box.getChildren().addAll(help, toolbar);
        for (javafx.scene.Node n : rest) {
            box.getChildren().add(n);
            if (n instanceof ListView<?> lv) {
                VBox.setVgrow(lv, Priority.ALWAYS);
            }
        }
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private static HBox toolbar(TextField search, ComboBox<String> extra) {
        HBox.setHgrow(search, Priority.ALWAYS);
        HBox row = new HBox(8, search);
        if (extra != null) {
            extra.setMaxWidth(140);
            row.getChildren().add(extra);
        }
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }

    private static Label meta(String text) {
        return CoreMapInsightStyles.meta(text);
    }

    private static Label chip(String text, boolean associative) {
        return CoreMapInsightStyles.chip(text, associative);
    }

    private boolean isBridgeActive(Bridge b) {
        if (b == null || selectedBridges.isEmpty()) {
            return false;
        }
        String key = CoreMapCascadeUi.bridgeKey(b);
        for (Bridge sel : selectedBridges) {
            if (key.equals(CoreMapCascadeUi.bridgeKey(sel))) {
                return true;
            }
        }
        return false;
    }

    private boolean isGeneActive(String gene) {
        return gene != null && selectedGenes.contains(gene);
    }

    private final class BridgeCardCell extends ListCell<Bridge> {
        BridgeCardCell() {
            getStyleClass().add("coremap-list-cell");
            installWrapListener(this);
        }

        @Override
        protected void updateItem(Bridge b, boolean empty) {
            super.updateItem(b, empty);
            getStyleClass().remove("coremap-card-selected");
            if (empty || b == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label title = new Label(CoreMapCascadeUi.mechLabel(b) + " → " + CoreMapCascadeUi.phenoLabel(b));

            HBox chips = new HBox(6);
            String kind = CoreMapCascadeUi.evidenceKindLabel(b);
            if (kind != null) {
                chips.getChildren().add(chip(kind, kind.startsWith("association")));
            }

            int pathCount = b.pathCount != null ? b.pathCount : 1;
            boolean multiSupport = pathCount > 1
                    && b.supportingPaths != null
                    && b.supportingPaths.size() > 1;
            javafx.scene.Node cascade = multiSupport
                    ? CoreMapCascadeUi.cascadeBranchDiagram(b, true)
                    : CoreMapCascadeUi.cascadeChainFlow(b.nodes, b.hops);

            StringBuilder metaSb = new StringBuilder(String.format(Locale.ROOT, "%.3f", b.bridgeScore));
            if (pathCount > 1) {
                metaSb.append(" · ").append(pathCount).append(" cascades");
            }
            metaSb.append(" · ").append(CoreMapCascadeUi.alignmentLabel(b.directionAlignment));
            String emp = CoreMapCascadeUi.empiricalPLabel(b);
            if (emp != null) {
                metaSb.append(" · ").append(emp);
            }

            VBox card = new VBox(4, title);
            if (!chips.getChildren().isEmpty()) {
                card.getChildren().add(chips);
            }
            card.getChildren().add(cascade);
            card.getChildren().add(meta(metaSb.toString()));
            String related = CoreMapCascadeUi.relatedSetsText(b);
            if (related != null) {
                card.getChildren().add(meta("Related: " + related));
            }
            card.setPadding(new Insets(8, 6, 8, 6));
            if (isBridgeActive(b)) {
                card.getStyleClass().add("coremap-card-selected");
                getStyleClass().add("coremap-card-selected");
            }
            bindCardWrap(this, card, title, cascade);
            setGraphic(card);
            setText(null);
        }
    }

    private final class DriverCardCell extends ListCell<SharedDriverSummary> {
        DriverCardCell() {
            getStyleClass().add("coremap-list-cell");
            installWrapListener(this);
        }

        @Override
        protected void updateItem(SharedDriverSummary d, boolean empty) {
            super.updateItem(d, empty);
            getStyleClass().remove("coremap-card-selected");
            if (empty || d == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label title = new Label(d.gene);
            String metaText = String.format(Locale.ROOT, "score %.3f · concordance %.2f",
                    d.sharedDriverScore, d.directionConcordance);
            VBox card = new VBox(4, title, meta(metaText));
            if (d.mechanisticSets != null && !d.mechanisticSets.isEmpty()) {
                card.getChildren().add(meta("Mech: " + String.join(", ", d.mechanisticSets)));
            }
            if (d.phenotypicSets != null && !d.phenotypicSets.isEmpty()) {
                card.getChildren().add(meta("Pheno: " + String.join(", ", d.phenotypicSets)));
            }
            card.setPadding(new Insets(8, 6, 8, 6));
            if (isGeneActive(d.gene)) {
                card.getStyleClass().add("coremap-card-selected");
                getStyleClass().add("coremap-card-selected");
            }
            bindCardWrap(this, card, title, null);
            setGraphic(card);
            setText(null);
        }
    }

    private final class HubCardCell extends ListCell<LayerHubSummary> {
        HubCardCell() {
            getStyleClass().add("coremap-list-cell");
            installWrapListener(this);
        }

        @Override
        protected void updateItem(LayerHubSummary h, boolean empty) {
            super.updateItem(h, empty);
            getStyleClass().remove("coremap-card-selected");
            if (empty || h == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label title = new Label(h.gene);
            String layer = h.layer != null ? CoreMapInsightStyles.layerLabel(h.layer.wire()) : "";
            String metaText = String.format(Locale.ROOT, "%s · hub %.3f · degree %d",
                    layer, h.hubScore, h.degree);
            VBox card = new VBox(4, title, meta(metaText));
            if (h.sets != null && !h.sets.isEmpty()) {
                card.getChildren().add(meta(String.join(", ", h.sets)));
            }
            card.setPadding(new Insets(8, 6, 8, 6));
            if (isGeneActive(h.gene)) {
                card.getStyleClass().add("coremap-card-selected");
                getStyleClass().add("coremap-card-selected");
            }
            bindCardWrap(this, card, title, null);
            setGraphic(card);
            setText(null);
        }
    }
}
