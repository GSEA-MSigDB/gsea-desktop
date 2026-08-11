/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.coremap;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Polygon;
import xapps.gsea.fx.widgets.FxButtons;

/**
 * Explore-stage layout: graph canvas, legend strip, insight sidebar, and view/zoom toolbar.
 */
final class CoreMapExploreStage {

    interface Host {
        CoreMapGraphView graphView();

        CoreMapSelectionOverlay selectionOverlay();

        CoreMapInsightSidebar insightSidebar();

        Button adjustSetsBtn();

        Button rescoreBtn();

        Button cancelBtn();

        MenuButton exportPngMenu();

        MenuButton exportJsonMenu();

        Button exportTsvBtn();

        Button saveJobBtn();

        /** View mode or gene-visibility filter changed — reload graph. */
        void onExploreViewChanged();

        /** View mode changed — refresh gene-visibility control enabled state. */
        void onViewModeChanged();
    }

    final ComboBox<String> viewMode = new ComboBox<>(FXCollections.observableArrayList(
            "connectome", "genes", "sets"));
    final ComboBox<String> geneVisibility = new ComboBox<>(FXCollections.observableArrayList(
            "cascade", "mechanism", "all"));
    final Label geneVisibilityLabel = new Label("Genes");
    final Button fitBtn = new Button("Fit");
    final Button zoomInBtn = new Button("+");
    final Button zoomOutBtn = new Button("−");
    final Button reflowBtn = new Button("Reflow");
    final TextField graphSearch = new TextField();

    private FlowPane legendStrip;
    private final Host host;
    private boolean suppressVisibilityListener;

    CoreMapExploreStage(Host host) {
        this.host = host;
        viewMode.getSelectionModel().select("connectome");
        geneVisibility.getSelectionModel().select("cascade");
        graphSearch.setPromptText("Find gene or set…");
        styleSecondaryButtons(FxButtons::styleSecondary);
        wireExploreControls();
    }

    void styleSecondaryButtons(java.util.function.Consumer<Button> styler) {
        styler.accept(fitBtn);
        styler.accept(zoomInBtn);
        styler.accept(zoomOutBtn);
        styler.accept(reflowBtn);
    }

    BorderPane buildPane() {
        CoreMapSelectionOverlay selectionOverlay = host.selectionOverlay();
        StackPane.setAlignment(selectionOverlay, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(selectionOverlay, new Insets(12));

        CoreMapGraphView graphView = host.graphView();
        StackPane canvas = new StackPane(graphView, selectionOverlay);
        canvas.setMinHeight(0);
        graphView.setMinHeight(0);
        VBox.setVgrow(canvas, Priority.ALWAYS);

        legendStrip = new FlowPane(14, 6);
        legendStrip.setPadding(new Insets(6, 12, 8, 12));
        legendStrip.getStyleClass().add("coremap-legend");
        legendStrip.setMaxWidth(Double.MAX_VALUE);
        refreshLegendStrip();

        VBox vizColumn = new VBox(canvas, legendStrip);
        SplitPane split = new SplitPane(vizColumn, host.insightSidebar());
        split.setOrientation(Orientation.HORIZONTAL);
        split.setDividerPositions(0.72);

        HBox.setHgrow(graphSearch, Priority.ALWAYS);
        graphSearch.setMaxWidth(220);
        HBox actions = FxButtons.row(host.adjustSetsBtn(), host.rescoreBtn(), host.cancelBtn(),
                new Label("View"), viewMode, geneVisibilityLabel, geneVisibility,
                zoomOutBtn, zoomInBtn, fitBtn, reflowBtn, graphSearch,
                host.exportPngMenu(), host.exportJsonMenu(), host.exportTsvBtn(), host.saveJobBtn());
        VBox top = new VBox(8, actions);
        top.setPadding(new Insets(10, 12, 0, 12));
        BorderPane explore = new BorderPane();
        explore.setTop(top);
        explore.setCenter(split);
        BorderPane.setMargin(split, new Insets(8, 12, 8, 12));
        return explore;
    }

    /** Rebuild legend contents for the active view mode (CoreMap explore-legend parity). */
    void refreshLegendStrip() {
        if (legendStrip == null) {
            return;
        }
        String mode = viewMode.getValue() != null ? viewMode.getValue() : "connectome";
        legendStrip.getChildren().clear();
        if (!"genes".equals(mode)) {
            legendStrip.getChildren().addAll(
                    legendSwatch("#D97706", "Mechanistic set", 3),
                    legendSwatch("#E11D48", "Phenotypic set", 8));
        }
        if (!"sets".equals(mode)) {
            legendStrip.getChildren().addAll(
                    legendSharedDriver("Shared driver"),
                    legendGlyph("▲", "#D97706", "Mechanistic gene"),
                    legendGlyph("⬡", "#E11D48", "Phenotypic gene"),
                    legendGlyph("●", "#9AA7B8", "Intermediate"));
        }
        legendStrip.getChildren().addAll(
                legendSwatch("#D97706", "Mech hot (+)", 2),
                legendSwatch("#0E7490", "Mech cold (−)", 2),
                legendSwatch("#E11D48", "Pheno hot (+)", 2),
                legendSwatch("#4338CA", "Pheno cold (−)", 2),
                legendSwatch("#9AA7B8", "Unsigned", 2),
                legendEdgeStyle("solid", "Solid = curated causal"),
                legendEdgeStyle("dotted", "Dotted = STRING"),
                legendEdgeStyle("dashed", "Dashed = membership"));
    }

    void selectGeneVisibility(String value, boolean suppressListener) {
        suppressVisibilityListener = suppressListener;
        try {
            geneVisibility.getSelectionModel().select(value);
        } finally {
            if (suppressListener) {
                suppressVisibilityListener = false;
            }
        }
    }

    void setViewModeDisabled(boolean disabled) {
        viewMode.setDisable(disabled);
    }

    void updateGeneVisibilityEnabled(boolean uiBusy) {
        boolean sets = "sets".equals(viewMode.getValue());
        geneVisibility.setDisable(sets || uiBusy);
        geneVisibilityLabel.setDisable(sets || uiBusy);
    }

    private void wireExploreControls() {
        viewMode.valueProperty().addListener((o, a, b) -> {
            host.onViewModeChanged();
            refreshLegendStrip();
            host.onExploreViewChanged();
        });
        geneVisibility.valueProperty().addListener((o, a, b) -> {
            if (!suppressVisibilityListener) {
                host.onExploreViewChanged();
            }
        });
        fitBtn.setOnAction(e -> host.graphView().fit());
        zoomInBtn.setOnAction(e -> host.graphView().zoomBy(1.25));
        zoomOutBtn.setOnAction(e -> host.graphView().zoomBy(1.0 / 1.25));
        reflowBtn.setOnAction(e -> host.graphView().reflow());
    }

    private static HBox legendSwatch(String color, String text, double radius) {
        Region swatch = new Region();
        swatch.setMinSize(12, 12);
        swatch.setPrefSize(12, 12);
        swatch.setMaxSize(12, 12);
        swatch.setStyle("-fx-background-color: " + color + "; -fx-background-radius: " + radius
                + "; -fx-border-color: #555555; -fx-border-width: 1; -fx-border-radius: " + radius + ";");
        Label label = legendLabel(text);
        HBox row = new HBox(5, swatch, label);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static HBox legendGlyph(String glyph, String fill, String text) {
        Label swatch = new Label(glyph);
        swatch.setStyle("-fx-text-fill: " + fill
                + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-min-width: 12;");
        Label label = legendLabel(text);
        HBox row = new HBox(5, swatch, label);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** Diamond with split mech|pheno fill — matches CoreMap shared-driver swatch. */
    private static HBox legendSharedDriver(String text) {
        Region swatch = new Region();
        swatch.setMinSize(12, 12);
        swatch.setPrefSize(12, 12);
        swatch.setMaxSize(12, 12);
        swatch.setStyle("-fx-background-color: linear-gradient(to right, #D97706 50%, #E11D48 50%);");
        Polygon diamond = new Polygon(
                6, 0,
                12, 6,
                6, 12,
                0, 6);
        swatch.setClip(diamond);
        Label label = legendLabel(text);
        HBox row = new HBox(5, swatch, label);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static HBox legendEdgeStyle(String style, String text) {
        Region line = new Region();
        line.setMinSize(18, 0);
        line.setPrefSize(18, 0);
        line.setMaxSize(18, 0);
        String borderStyle = switch (style) {
            case "dotted" -> "dotted";
            case "dashed" -> "dashed";
            default -> "solid";
        };
        String color = "dashed".equals(style) ? "#6B6560" : "#64748b";
        line.setStyle("-fx-border-color: " + color + " transparent transparent transparent;"
                + "-fx-border-width: 2.5 0 0 0; -fx-border-style: " + borderStyle + ";");
        Label label = legendLabel(text);
        HBox row = new HBox(5, line, label);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static Label legendLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("coremap-legend-label");
        return l;
    }
}
