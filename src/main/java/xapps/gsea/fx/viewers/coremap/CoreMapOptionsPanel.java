/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.coremap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import edu.mit.broad.coremap.CoreMapConstants;
import edu.mit.broad.coremap.CoreMapJob;
import edu.mit.broad.coremap.CoreMapOrganism;
import edu.mit.broad.coremap.IntegrationOptions;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import xapps.gsea.fx.params.FxFileChooserUtil;
import xapps.gsea.fx.widgets.FxEllipsisButton;

/**
 * Threshold spinners and interactome / scoring option widgets for the CoreMap setup stage.
 */
final class CoreMapOptionsPanel {

    interface Host {
        Node windowOwner();

        /** Threshold or separate-layer toggle changed — refresh gene-set pass/include state. */
        void onThresholdsChanged();

        /** Integration option changed — update rescore / re-integrate availability. */
        void onIntegrationOptionsChanged();
    }

    final Spinner<Double> minNes = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 10,
            CoreMapConstants.DEFAULT_MIN_NES, 0.1));
    final Spinner<Double> maxNp = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1,
            CoreMapConstants.DEFAULT_MAX_NP, 0.01));
    final Spinner<Double> maxFdr = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1,
            CoreMapConstants.DEFAULT_MAX_FDR, 0.01));
    final CheckBox separateLayerThresholds = new CheckBox("Separate phenotypic thresholds");
    final Spinner<Double> phenoMinNes = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 10,
            CoreMapConstants.DEFAULT_MIN_NES, 0.1));
    final Spinner<Double> phenoMaxNp = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1,
            CoreMapConstants.DEFAULT_MAX_NP, 0.01));
    final Spinner<Double> phenoMaxFdr = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1,
            CoreMapConstants.DEFAULT_MAX_FDR, 0.01));

    final ComboBox<String> interactomeSource = new ComboBox<>(FXCollections.observableArrayList(
            "fused", "signor", "string", "none"));
    final ComboBox<String> stringMode = new ComboBox<>(FXCollections.observableArrayList(
            "integrated", "functional", "regulatory", "physical"));
    final Spinner<Double> minInteraction = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1, 0.4, 0.05));
    final Spinner<Integer> maxPathLength = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 12));
    final Spinner<Integer> topKBridges = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 10));
    final Spinner<Integer> neighborLimit = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 50, 0));
    final CheckBox includeNeighbors = new CheckBox("Include extra neighbors");
    final Spinner<Double> pathLengthPenalty = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1, 0.08, 0.01));
    final Spinner<Double> directionWeight = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1, 0.6, 0.05));
    final Spinner<Double> sharedDriverDirectionWeight = new Spinner<>(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1, 0.45, 0.05));
    final Spinner<Integer> signorLevel = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 3, 1));
    final ComboBox<String> signorOrganism = new ComboBox<>(FXCollections.observableArrayList(
            CoreMapOrganism.displayLabel(CoreMapOrganism.HUMAN),
            CoreMapOrganism.displayLabel(CoreMapOrganism.MOUSE)));
    final ComboBox<String> signorQueryType = new ComboBox<>(FXCollections.observableArrayList("connect", "all"));
    final TextField signorPathways = new TextField();
    final CheckBox signorProteinOnly = new CheckBox("SIGNOR protein-only");
    final CheckBox signorDirectOnly = new CheckBox("SIGNOR direct-only");
    final CheckBox enableSourceEnrichment = new CheckBox("Source enrichment (Reactome/KEGG/…)");
    final TextField msigdbPath = new TextField();
    final Spinner<Integer> nullPermutations = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 500, 0, 10));

    private final HBox phenoThresholdRow = new HBox(8);
    private final List<Node> optionControls = new ArrayList<>();
    private final Host host;

    CoreMapOptionsPanel(Host host) {
        this.host = host;
        initWidgets();
        wireThresholdListeners();
        wireIntegrationOptionListeners();
        collectOptionControls();
    }

    List<Node> optionControls() {
        return optionControls;
    }

    CoreMapIntegrateController.IntegrationOptionsSnapshot optionsSnapshot() {
        return CoreMapIntegrateController.snapshotFrom(
                interactomeSource, stringMode, minInteraction, maxPathLength, pathLengthPenalty,
                directionWeight, sharedDriverDirectionWeight, topKBridges, includeNeighbors, neighborLimit,
                signorLevel, signorOrganism, signorQueryType, signorProteinOnly, signorDirectOnly,
                signorPathways, enableSourceEnrichment, msigdbPath, nullPermutations);
    }

    GridPane buildThresholdsGrid() {
        GridPane thresholds = new GridPane();
        thresholds.setHgap(8);
        thresholds.setVgap(6);
        thresholds.addRow(0, new Label("Min |NES|"), minNes, new Label("Max NOM pVal"), maxNp, new Label("Max FDR"), maxFdr);
        thresholds.add(separateLayerThresholds, 0, 1, 3, 1);
        thresholds.add(phenoThresholdRow, 0, 2, 6, 1);
        return thresholds;
    }

    TitledPane buildOptionsPane() {
        Label sourceHint = new Label();
        sourceHint.getStyleClass().addAll("gsea-muted", "coremap-hint");
        sourceHint.setWrapText(true);
        sourceHint.setMaxWidth(Double.MAX_VALUE);

        GridPane sourceRow = new GridPane();
        sourceRow.setHgap(8);
        sourceRow.setVgap(6);
        sourceRow.addRow(0, new Label("Source"), interactomeSource);

        GridPane sharedGrid = new GridPane();
        sharedGrid.setHgap(8);
        sharedGrid.setVgap(6);
        sharedGrid.addRow(0, new Label("Organism"), signorOrganism,
                new Label("Min interaction"), minInteraction);
        sharedGrid.addRow(1, new Label("Max path length"), maxPathLength,
                new Label("Top bridges"), topKBridges);
        sharedGrid.addRow(2, includeNeighbors, neighborLimit,
                new Label("Path penalty"), pathLengthPenalty);
        sharedGrid.addRow(3, new Label("Direction weight"), directionWeight,
                new Label("Shared-driver dir. wt"), sharedDriverDirectionWeight);
        sharedGrid.addRow(4, new Label("Null permutations"), nullPermutations);
        VBox sharedBox = optionGroup("Shared scoring",
                "Used for path search and ranking, including organism for UniProt mapping.",
                sharedGrid);

        GridPane signorGrid = new GridPane();
        signorGrid.setHgap(8);
        signorGrid.setVgap(6);
        signorGrid.addRow(0, new Label("Query type"), signorQueryType,
                new Label("Connect level"), signorLevel);
        signorGrid.addRow(1, signorProteinOnly, signorDirectOnly);
        signorGrid.add(new Label("Pathway IDs"), 0, 2);
        signorGrid.add(signorPathways, 1, 2, 3, 1);
        VBox signorBox = optionGroup("SIGNOR",
                "Directed causal edges. Applies when Source is SIGNOR or Fused.",
                signorGrid);

        GridPane stringGrid = new GridPane();
        stringGrid.setHgap(8);
        stringGrid.setVgap(6);
        stringGrid.addRow(0, new Label("STRING mode"), stringMode);
        VBox stringBox = optionGroup("STRING",
                "Functional, regulatory, or physical networks. Applies when Source is STRING or Fused.",
                stringGrid);

        GridPane enrichGrid = new GridPane();
        enrichGrid.setHgap(8);
        enrichGrid.setVgap(6);
        enrichGrid.add(enableSourceEnrichment, 0, 0, 4, 1);
        enrichGrid.add(new Label("MSigDB JSON"), 0, 1);
        HBox msigRow = new HBox(8, msigdbPath, browseMsigdbButton());
        HBox.setHgrow(msigdbPath, Priority.ALWAYS);
        enrichGrid.add(msigRow, 1, 1, 3, 1);
        VBox enrichBox = optionGroup("Source enrichment",
                "Optional Reactome, KEGG, HPO, or GO annotation via MSigDB.",
                enrichGrid);

        VBox opts = new VBox(10, sourceRow, sourceHint, sharedBox, signorBox, stringBox, enrichBox);
        Runnable refreshInteractomeSections = () -> {
            String src = interactomeSource.getValue() != null ? interactomeSource.getValue() : "fused";
            boolean showSignor = "signor".equals(src) || "fused".equals(src);
            boolean showString = "string".equals(src) || "fused".equals(src);
            signorBox.setVisible(showSignor);
            signorBox.setManaged(showSignor);
            stringBox.setVisible(showString);
            stringBox.setManaged(showString);
            sourceHint.setText(interactomeSourceHint(src));
        };
        interactomeSource.valueProperty().addListener((o, a, b) -> refreshInteractomeSections.run());
        refreshInteractomeSections.run();

        TitledPane optionsPane = new TitledPane("Interactome & scoring", opts);
        optionsPane.setExpanded(true);
        optionsPane.setAnimated(true);
        return optionsPane;
    }

    void applyIntegrationOptions(IntegrationOptions o) {
        interactomeSource.getSelectionModel().select(o.interactomeSource != null ? o.interactomeSource.wire() : "fused");
        stringMode.getSelectionModel().select(o.stringMode != null ? o.stringMode.wire() : "integrated");
        minInteraction.getValueFactory().setValue(o.minInteractionScore);
        maxPathLength.getValueFactory().setValue(o.maxPathLength);
        pathLengthPenalty.getValueFactory().setValue(o.pathLengthPenalty);
        topKBridges.getValueFactory().setValue(o.topKBridges);
        includeNeighbors.setSelected(o.includeExtraNeighbors);
        neighborLimit.getValueFactory().setValue(o.neighborLimit);
        directionWeight.getValueFactory().setValue(o.directionWeight);
        sharedDriverDirectionWeight.getValueFactory().setValue(o.sharedDriverDirectionWeight);
        signorLevel.getValueFactory().setValue(o.signorLevel);
        if (o.signorOrganism != null) {
            signorOrganism.getSelectionModel().select(CoreMapOrganism.displayLabel(o.signorOrganism));
        }
        if (o.signorQueryType != null) {
            signorQueryType.getSelectionModel().select(o.signorQueryType);
        }
        signorProteinOnly.setSelected(o.signorProteinOnly);
        signorDirectOnly.setSelected(o.signorDirectOnly);
        signorPathways.setText(o.signorPathways != null ? String.join(", ", o.signorPathways) : "");
        enableSourceEnrichment.setSelected(o.enableSourceEnrichment);
        msigdbPath.setText(o.msigdbPath != null ? o.msigdbPath : "");
        nullPermutations.getValueFactory().setValue(o.nullPermutations);
    }

    void applyJobThresholds(CoreMapJob job) {
        minNes.getValueFactory().setValue(job.minNes);
        maxNp.getValueFactory().setValue(job.maxNp);
        maxFdr.getValueFactory().setValue(job.maxFdr);
        separateLayerThresholds.setSelected(job.separateLayerThresholds);
        if (job.phenoMinNes != null) {
            phenoMinNes.getValueFactory().setValue(job.phenoMinNes);
        }
        if (job.phenoMaxNp != null) {
            phenoMaxNp.getValueFactory().setValue(job.phenoMaxNp);
        }
        if (job.phenoMaxFdr != null) {
            phenoMaxFdr.getValueFactory().setValue(job.phenoMaxFdr);
        }
    }

    /** Thresholds for the given layer (mechanistic uses shared row unless separate pheno thresholds). */
    double[] thresholdsForLayer(boolean mechanistic) {
        if (mechanistic || !separateLayerThresholds.isSelected()) {
            return new double[] { minNes.getValue(), maxNp.getValue(), maxFdr.getValue() };
        }
        return new double[] { phenoMinNes.getValue(), phenoMaxNp.getValue(), phenoMaxFdr.getValue() };
    }

    private void initWidgets() {
        minNes.setEditable(true);
        maxNp.setEditable(true);
        maxFdr.setEditable(true);
        phenoMinNes.setEditable(true);
        phenoMaxNp.setEditable(true);
        phenoMaxFdr.setEditable(true);
        phenoThresholdRow.getChildren().setAll(
                new Label("Pheno min |NES|"), phenoMinNes,
                new Label("Max NOM pVal"), phenoMaxNp,
                new Label("Max FDR"), phenoMaxFdr);
        phenoThresholdRow.setVisible(false);
        phenoThresholdRow.setManaged(false);
        separateLayerThresholds.selectedProperty().addListener((o, a, on) -> {
            phenoThresholdRow.setVisible(on);
            phenoThresholdRow.setManaged(on);
            if (on) {
                phenoMinNes.getValueFactory().setValue(minNes.getValue());
                phenoMaxNp.getValueFactory().setValue(maxNp.getValue());
                phenoMaxFdr.getValueFactory().setValue(maxFdr.getValue());
            }
        });
        minInteraction.setEditable(true);
        maxPathLength.setEditable(true);
        topKBridges.setEditable(true);
        neighborLimit.setEditable(true);
        pathLengthPenalty.setEditable(true);
        directionWeight.setEditable(true);
        sharedDriverDirectionWeight.setEditable(true);
        nullPermutations.setEditable(true);
        interactomeSource.getSelectionModel().select("fused");
        stringMode.getSelectionModel().select("integrated");
        signorOrganism.getSelectionModel().select(CoreMapOrganism.displayLabel(CoreMapOrganism.HUMAN));
        signorQueryType.getSelectionModel().select("connect");
        signorProteinOnly.setSelected(true);
        signorProteinOnly.setText("Protein–protein only");
        signorDirectOnly.setSelected(false);
        signorDirectOnly.setText("Direct evidence only");
        enableSourceEnrichment.setSelected(true);
        signorLevel.setEditable(true);
        signorPathways.setPromptText("SIGNOR-MM, SIGNOR-P1 (optional)");
        msigdbPath.setPromptText("Optional MSigDB JSON for set ID resolution");
        styleInteractomeSourceCombo();
        styleStringModeCombo();
        styleSignorQueryCombo();
    }

    private void wireThresholdListeners() {
        Runnable refresh = host::onThresholdsChanged;
        minNes.valueProperty().addListener((o, a, b) -> refresh.run());
        maxNp.valueProperty().addListener((o, a, b) -> refresh.run());
        maxFdr.valueProperty().addListener((o, a, b) -> refresh.run());
        phenoMinNes.valueProperty().addListener((o, a, b) -> refresh.run());
        phenoMaxNp.valueProperty().addListener((o, a, b) -> refresh.run());
        phenoMaxFdr.valueProperty().addListener((o, a, b) -> refresh.run());
        separateLayerThresholds.selectedProperty().addListener((o, a, b) -> refresh.run());
    }

    private void wireIntegrationOptionListeners() {
        Runnable refresh = host::onIntegrationOptionsChanged;
        interactomeSource.valueProperty().addListener((o, a, b) -> refresh.run());
        signorOrganism.valueProperty().addListener((o, a, b) -> refresh.run());
        signorQueryType.valueProperty().addListener((o, a, b) -> refresh.run());
        signorLevel.valueProperty().addListener((o, a, b) -> refresh.run());
        signorPathways.textProperty().addListener((o, a, b) -> refresh.run());
        stringMode.valueProperty().addListener((o, a, b) -> refresh.run());
        minInteraction.valueProperty().addListener((o, a, b) -> refresh.run());
        includeNeighbors.selectedProperty().addListener((o, a, b) -> refresh.run());
        neighborLimit.valueProperty().addListener((o, a, b) -> refresh.run());
        enableSourceEnrichment.selectedProperty().addListener((o, a, b) -> refresh.run());
        msigdbPath.textProperty().addListener((o, a, b) -> refresh.run());
        signorProteinOnly.selectedProperty().addListener((o, a, b) -> refresh.run());
        signorDirectOnly.selectedProperty().addListener((o, a, b) -> refresh.run());
    }

    private void collectOptionControls() {
        optionControls.addAll(List.of(
                interactomeSource, stringMode, minInteraction, maxPathLength, topKBridges, includeNeighbors,
                neighborLimit, pathLengthPenalty, directionWeight, sharedDriverDirectionWeight, signorLevel,
                signorOrganism, signorQueryType, signorProteinOnly, signorDirectOnly, signorPathways,
                enableSourceEnrichment, nullPermutations, msigdbPath, minNes, maxNp, maxFdr,
                separateLayerThresholds, phenoMinNes, phenoMaxNp, phenoMaxFdr));
    }

    private Button browseMsigdbButton() {
        Button browse = FxEllipsisButton.create("Browse MSigDB JSON");
        browse.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("MSigDB gene set JSON");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
            FxFileChooserUtil.seedInitialDirectory(chooser, msigdbPath.getText());
            File selected = chooser.showOpenDialog(FxFileChooserUtil.windowOf(host.windowOwner()));
            if (selected != null) {
                msigdbPath.setText(selected.getAbsolutePath());
                FxFileChooserUtil.registerOpened(selected);
            }
        });
        return browse;
    }

    private static VBox optionGroup(String title, String help, Node content) {
        Label heading = new Label(title);
        heading.getStyleClass().add("coremap-option-group-title");
        Label hint = new Label(help);
        hint.getStyleClass().addAll("gsea-muted", "coremap-hint");
        hint.setWrapText(true);
        hint.setMaxWidth(Double.MAX_VALUE);
        VBox box = new VBox(6, heading, hint, content);
        box.getStyleClass().add("coremap-option-group");
        return box;
    }

    private static String interactomeSourceHint(String src) {
        return switch (src != null ? src : "") {
            case "signor" -> "Directed causal edges from SIGNOR. Connect links enrichment genes to each other; "
                    + "All includes every curated relation involving those genes.";
            case "string" -> "STRING network edges. Integrated mixes functional and regulatory; "
                    + "physical uses binding-oriented edges.";
            case "fused" -> "Merge SIGNOR and STRING. When both report an edge, SIGNOR sets the sign and effect; "
                    + "STRING fills in extra edges. Options for both sources apply below.";
            case "none" -> "Skip SIGNOR/STRING. Gene–gene links come from shared leading-edge set membership "
                    + "(and GO/HPO if source enrichment is on).";
            default -> "Select an interactome source.";
        };
    }

    private void styleInteractomeSourceCombo() {
        interactomeSource.setPrefWidth(260);
        styleLabeledCombo(interactomeSource, item -> switch (item) {
            case "fused" -> "Fused (SIGNOR + STRING)";
            case "signor" -> "SIGNOR (directed causal)";
            case "string" -> "STRING (functional + regulatory)";
            case "none" -> "None (set co-membership only)";
            default -> item;
        });
    }

    private void styleStringModeCombo() {
        stringMode.setPrefWidth(140);
        styleLabeledCombo(stringMode, item -> switch (item) {
            case "integrated" -> "Integrated";
            case "regulatory" -> "Regulatory";
            case "functional" -> "Functional";
            case "physical" -> "Physical";
            default -> item;
        });
    }

    private void styleSignorQueryCombo() {
        signorQueryType.setPrefWidth(200);
        styleLabeledCombo(signorQueryType, item -> switch (item) {
            case "connect" -> "Connect — links among enrichment genes";
            case "all" -> "All — every relation involving enrichment genes";
            default -> item;
        });
    }

    private static void styleLabeledCombo(ComboBox<String> combo, Function<String, String> labelOf) {
        Callback<ListView<String>, ListCell<String>> factory = lv -> labeledCell(labelOf);
        combo.setButtonCell(labeledCell(labelOf));
        combo.setCellFactory(factory);
    }

    private static ListCell<String> labeledCell(Function<String, String> labelOf) {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(labelOf.apply(item));
            }
        };
    }
}
