/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.coremap;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.gsea_msigdb.gsea.ui.api.ViewPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.coremap.CoreMapConstants;
import edu.mit.broad.coremap.CoreMapOrganism;
import edu.mit.broad.coremap.CoreMapJob;
import edu.mit.broad.coremap.CoreMapJobStore;
import edu.mit.broad.coremap.CoreMapJson;
import edu.mit.broad.coremap.CoreMapPipeline;
import edu.mit.broad.coremap.CoreMapPipeline.IntegrateRequest;
import edu.mit.broad.coremap.CoreMapTypes.Bridge;
import edu.mit.broad.coremap.CoreMapTypes.BuildProgress;
import edu.mit.broad.coremap.CoreMapTypes.BuildProgressPhase;
import edu.mit.broad.coremap.CoreMapTypes.EnrichmentParseResult;
import edu.mit.broad.coremap.CoreMapTypes.GeneSetSummary;
import edu.mit.broad.coremap.CoreMapTypes.IntegrationResult;
import edu.mit.broad.coremap.CoreMapTypes.InteractomeSource;
import edu.mit.broad.coremap.CoreMapTypes.LayerHubSummary;
import edu.mit.broad.coremap.CoreMapTypes.NodeElement;
import edu.mit.broad.coremap.CoreMapTypes.SetEnrichmentMetric;
import edu.mit.broad.coremap.CoreMapTypes.SharedDriverSummary;
import edu.mit.broad.coremap.CoreMapTypes.StringMode;
import edu.mit.broad.coremap.EdbEnrichmentParser;
import edu.mit.broad.coremap.EnrichmentMath;
import edu.mit.broad.coremap.IntegrationOptions;
import edu.mit.broad.xbench.core.api.Application;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import xapps.gsea.fx.FxButtons;
import xapps.gsea.fx.params.FxFileChooserUtil;
import xapps.gsea.fx.viewers.coremap.CoreMapGraphViews.GeneVisibility;
import xapps.gsea.fx.viewers.coremap.CoreMapGraphViews.ViewMode;

/**
 * Native CoreMap workspace: load GSEA EDB layers, preview/select sets, integrate,
 * and explore bridges / drivers / hubs with an embedded Cytoscape.js graph.
 */
public class FxCoreMapPane implements ViewPage {

    private static final Logger klog = LoggerFactory.getLogger(FxCoreMapPane.class);

    private final BorderPane root = new BorderPane();
    private final Label statusLabel = new Label("Load one or both GSEA result folders (mechanistic and phenotypic).");

    private final CoreMapLayerLoadUi mechLoad = new CoreMapLayerLoadUi("Mechanistic", true, layerLoadHost());
    private final CoreMapLayerLoadUi phenoLoad = new CoreMapLayerLoadUi("Phenotypic", false, layerLoadHost());

    private final Spinner<Double> minNes = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 10,
            CoreMapConstants.DEFAULT_MIN_NES, 0.1));
    private final Spinner<Double> maxNp = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1,
            CoreMapConstants.DEFAULT_MAX_NP, 0.01));
    private final Spinner<Double> maxFdr = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1,
            CoreMapConstants.DEFAULT_MAX_FDR, 0.01));
    private final CheckBox separateLayerThresholds = new CheckBox("Separate phenotypic thresholds");
    private final Spinner<Double> phenoMinNes = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 10,
            CoreMapConstants.DEFAULT_MIN_NES, 0.1));
    private final Spinner<Double> phenoMaxNp = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1,
            CoreMapConstants.DEFAULT_MAX_NP, 0.01));
    private final Spinner<Double> phenoMaxFdr = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1,
            CoreMapConstants.DEFAULT_MAX_FDR, 0.01));
    private final HBox phenoThresholdRow = new HBox(8);

    private final TableView<CoreMapGeneSetRow> mechSetTable = new TableView<>();
    private final TableView<CoreMapGeneSetRow> phenoSetTable = new TableView<>();
    private final ObservableList<CoreMapGeneSetRow> mechSetRows = FXCollections.observableArrayList();
    private final ObservableList<CoreMapGeneSetRow> phenoSetRows = FXCollections.observableArrayList();

    private final ComboBox<String> interactomeSource = new ComboBox<>(FXCollections.observableArrayList(
            "fused", "signor", "string", "none"));
    private final ComboBox<String> stringMode = new ComboBox<>(FXCollections.observableArrayList(
            "integrated", "functional", "regulatory", "physical"));
    private final Spinner<Double> minInteraction = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1, 0.4, 0.05));
    private final Spinner<Integer> maxPathLength = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 12));
    private final Spinner<Integer> topKBridges = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 10));
    private final Spinner<Integer> neighborLimit = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 50, 0));
    private final CheckBox includeNeighbors = new CheckBox("Include extra neighbors");
    private final Spinner<Double> pathLengthPenalty = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1, 0.08, 0.01));
    private final Spinner<Double> directionWeight = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1, 0.6, 0.05));
    private final Spinner<Double> sharedDriverDirectionWeight = new Spinner<>(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1, 0.45, 0.05));
    private final Spinner<Integer> signorLevel = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 3, 1));
    private final ComboBox<String> signorOrganism = new ComboBox<>(FXCollections.observableArrayList(
            CoreMapOrganism.displayLabel(CoreMapOrganism.HUMAN),
            CoreMapOrganism.displayLabel(CoreMapOrganism.MOUSE)));
    private final ComboBox<String> signorQueryType = new ComboBox<>(FXCollections.observableArrayList("connect", "all"));
    private final TextField signorPathways = new TextField();
    private final CheckBox signorProteinOnly = new CheckBox("SIGNOR protein-only");
    private final CheckBox signorDirectOnly = new CheckBox("SIGNOR direct-only");
    private final CheckBox enableSourceEnrichment = new CheckBox("Source enrichment (Reactome/KEGG/…)");
    private final TextField msigdbPath = new TextField();
    private final Spinner<Integer> nullPermutations = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 500, 0, 10));

    private final Button integrateBtn = new Button("Integrate");
    private final Button cancelBtn = new Button("Cancel");
    private final Button rescoreBtn = new Button("Rescore");
    private final Button helpBtn = new Button("Help");
    private final Button adjustSetsBtn = new Button("← Adjust sets");
    private final MenuButton exportPngMenu = new MenuButton("Export PNG");
    private final MenuItem exportPngViewItem = new MenuItem("Current view…");
    private final MenuItem exportPngSelectedItem = new MenuItem("Selected…");
    private final MenuButton exportJsonMenu = new MenuButton("Export JSON");
    private final MenuItem exportSelectedJsonItem = new MenuItem("Selected…");
    private final MenuItem exportCurrentJsonItem = new MenuItem("Current view…");
    private final MenuItem exportCompleteJsonItem = new MenuItem("Complete graph…");
    private final Button exportTsvBtn = new Button("Export bridges TSV");
    private final Button saveJobBtn = new Button("Save CoreMap Job");
    private final Button openJobBtn = new Button("Open CoreMap Job…");
    private final ProgressBar progressBar = new ProgressBar(0);
    private TabPane stageTabs;
    private Tab setupTab;
    private Tab exploreTab;
    private Tab guideTab;

    private final CoreMapSelectionOverlay selectionOverlay = new CoreMapSelectionOverlay();
    private final CoreMapInsightSidebar insightSidebar = new CoreMapInsightSidebar();
    private final TextField graphSearch = new TextField();
    private FlowPane legendStrip;
    private final List<String> selectedGraphGenes = new ArrayList<>();

    private final ComboBox<String> viewMode = new ComboBox<>(FXCollections.observableArrayList(
            "connectome", "genes", "sets"));
    private final ComboBox<String> geneVisibility = new ComboBox<>(FXCollections.observableArrayList(
            "cascade", "mechanism", "all"));
    private final Label geneVisibilityLabel = new Label("Genes");
    private boolean suppressVisibilityListener;
    private final Button fitBtn = new Button("Fit");
    private final Button zoomInBtn = new Button("+");
    private final Button zoomOutBtn = new Button("−");
    private final Button reflowBtn = new Button("Reflow");
    private final CoreMapGraphView graphView = new CoreMapGraphView();
    private final List<javafx.scene.Node> optionControls = new ArrayList<>();

    /** Full gene-set catalogs from last preview (threshold-independent). */
    private List<GeneSetSummary> mechCatalog = List.of();
    private List<GeneSetSummary> phenoCatalog = List.of();
    private boolean mechFollowThresholds = true;
    private boolean phenoFollowThresholds = true;
    private boolean applyingThresholdRefresh;
    private final TextField mechSearch = new TextField();
    private final TextField phenoSearch = new TextField();
    private final CheckBox mechShowFailing = new CheckBox("Show below-threshold");
    private final CheckBox phenoShowFailing = new CheckBox("Show below-threshold");
    private final Label mechSetStatus = new Label("Load a mechanistic GSEA result.");
    private final Label phenoSetStatus = new Label("Load a phenotypic GSEA result.");
    private final Button mechFollowBtn = new Button("Reapply Filters");
    private final Button phenoFollowBtn = new Button("Reapply Filters");
    private final Button mechClearBtn = new Button("Clear");
    private final Button phenoClearBtn = new Button("Clear");
    private final FilteredList<CoreMapGeneSetRow> mechFiltered = new FilteredList<>(mechSetRows, r -> true);
    private final FilteredList<CoreMapGeneSetRow> phenoFiltered = new FilteredList<>(phenoSetRows, r -> true);
    private final SortedList<CoreMapGeneSetRow> mechSorted = new SortedList<>(mechFiltered);
    private final SortedList<CoreMapGeneSetRow> phenoSorted = new SortedList<>(phenoFiltered);

    private EnrichmentParseResult mechParsed;
    private EnrichmentParseResult phenoParsed;
    private IntegrationResult lastResult;
    private String sessionId;
    private String sessionProviderKey;
    private Double sessionFetchedMinScore;
    private final AtomicBoolean cancelFlag = new AtomicBoolean(false);
    private boolean liveApiConfirmed;
    private boolean uiBusy;
    private boolean suppressAutoParse;
    private int catalogParseGen;
    private PauseTransition catalogDebounce;
    /** When set, next successful catalog parse reapplies these IDs instead of following thresholds. */
    private Set<String> pendingMechSetIds;
    private Set<String> pendingPhenoSetIds;

    public FxCoreMapPane() {
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
        viewMode.getSelectionModel().select("connectome");
        geneVisibility.getSelectionModel().select("cascade");
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
        configureSetTable(mechSetTable, mechSorted);
        configureSetTable(phenoSetTable, phenoSorted);
        wireGeneSetPickerControls();
        wireThresholdLiveListeners();
        wireInsightListeners();
        graphSearch.setPromptText("Find gene or set…");
        graphSearch.setOnAction(e -> {
            String q = graphSearch.getText() != null ? graphSearch.getText().trim() : "";
            if (q.isEmpty()) {
                return;
            }
            String hit = graphView.findAndFocusId(q);
            if (hit != null && !hit.isBlank()) {
                selectedGraphGenes.clear();
                selectedGraphGenes.add(hit);
                insightSidebar.clearUiSelection();
                showNodeDetail(hit);
                showSelectionOverlay(true);
                return;
            }
            // Not in current view — reveal if possible (CoreMap revealGeneIfNeeded).
            if (!q.contains(" ") && focusNeighborhoodOrReveal(List.of(q))) {
                selectedGraphGenes.clear();
                selectedGraphGenes.add(q);
                insightSidebar.clearUiSelection();
                showNodeDetail(q);
                showSelectionOverlay(true);
                return;
            }
            statusLabel.setText("No graph node matches “" + graphSearch.getText() + "”.");
        });

        FxButtons.stylePrimary(integrateBtn);
        FxButtons.styleSecondary(rescoreBtn);
        FxButtons.styleSecondary(cancelBtn);
        FxButtons.styleSecondary(helpBtn);
        FxButtons.styleSecondary(adjustSetsBtn);
        FxButtons.styleSecondary(fitBtn);
        FxButtons.styleSecondary(zoomInBtn);
        FxButtons.styleSecondary(zoomOutBtn);
        FxButtons.styleSecondary(reflowBtn);
        exportPngMenu.getStyleClass().add("gsea-button");
        exportJsonMenu.getStyleClass().add("gsea-button");
        FxButtons.styleSecondary(exportTsvBtn);
        FxButtons.styleSecondary(saveJobBtn);
        FxButtons.styleSecondary(openJobBtn);
        FxButtons.styleSecondary(mechFollowBtn);
        FxButtons.styleSecondary(phenoFollowBtn);
        FxButtons.styleSecondary(mechClearBtn);
        FxButtons.styleSecondary(phenoClearBtn);
        rescoreBtn.setDisable(true);
        cancelBtn.setDisable(true);
        integrateBtn.setDisable(true);
        exportPngMenu.setDisable(true);
        exportJsonMenu.setDisable(true);
        exportTsvBtn.setDisable(true);
        saveJobBtn.setDisable(true);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefWidth(180);
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        mechShowFailing.setSelected(true);
        phenoShowFailing.setSelected(true);
        mechSearch.setPromptText("Search gene sets…");
        phenoSearch.setPromptText("Search gene sets…");

        exportJsonMenu.getItems().setAll(exportSelectedJsonItem, exportCurrentJsonItem, exportCompleteJsonItem);
        exportPngMenu.getItems().setAll(exportPngViewItem, exportPngSelectedItem);

        integrateBtn.setOnAction(e -> runIntegrate());
        cancelBtn.setOnAction(e -> cancelFlag.set(true));
        rescoreBtn.setOnAction(e -> runRescore());
        helpBtn.setOnAction(e -> showGuideStage());
        adjustSetsBtn.setOnAction(e -> showSetupStage());
        exportPngViewItem.setOnAction(e -> exportPng(false));
        exportPngSelectedItem.setOnAction(e -> exportPng(true));
        exportSelectedJsonItem.setOnAction(e -> exportSelectedJson());
        exportCurrentJsonItem.setOnAction(e -> exportCurrentJson());
        exportCompleteJsonItem.setOnAction(e -> exportCompleteJson());
        exportTsvBtn.setOnAction(e -> exportBridgesTsv());
        saveJobBtn.setOnAction(e -> saveCoreMapJob());
        openJobBtn.setOnAction(e -> openCoreMapJob());
        viewMode.valueProperty().addListener((o, a, b) -> {
            updateGeneVisibilityEnabled();
            refreshLegendStrip();
            refreshGraph();
        });
        geneVisibility.valueProperty().addListener((o, a, b) -> {
            if (!suppressVisibilityListener) {
                refreshGraph();
            }
        });
        fitBtn.setOnAction(e -> graphView.fit());
        zoomInBtn.setOnAction(e -> graphView.zoomBy(1.25));
        zoomOutBtn.setOnAction(e -> graphView.zoomBy(1.0 / 1.25));
        reflowBtn.setOnAction(e -> graphView.reflow());
        interactomeSource.valueProperty().addListener((o, a, b) -> updateRescoreAvailability());
        signorOrganism.valueProperty().addListener((o, a, b) -> updateRescoreAvailability());
        signorQueryType.valueProperty().addListener((o, a, b) -> updateRescoreAvailability());
        signorLevel.valueProperty().addListener((o, a, b) -> updateRescoreAvailability());
        signorPathways.textProperty().addListener((o, a, b) -> updateRescoreAvailability());
        stringMode.valueProperty().addListener((o, a, b) -> updateRescoreAvailability());
        minInteraction.valueProperty().addListener((o, a, b) -> updateRescoreAvailability());

        includeNeighbors.selectedProperty().addListener((o, a, b) -> updateRescoreAvailability());
        neighborLimit.valueProperty().addListener((o, a, b) -> updateRescoreAvailability());
        enableSourceEnrichment.selectedProperty().addListener((o, a, b) -> updateRescoreAvailability());
        msigdbPath.textProperty().addListener((o, a, b) -> updateRescoreAvailability());
        signorProteinOnly.selectedProperty().addListener((o, a, b) -> updateRescoreAvailability());
        signorDirectOnly.selectedProperty().addListener((o, a, b) -> updateRescoreAvailability());

        graphView.setNodeSelectedAdditiveHandler((id, additive) -> Platform.runLater(() -> onGraphNodeSelected(id, additive)));
        graphView.setEdgeSelectedHandler(json -> Platform.runLater(() -> onGraphEdgeSelected(json)));
        graphView.setBackgroundTapHandler(() -> Platform.runLater(this::clearExploreSelection));

        selectionOverlay.setOnDismiss(this::clearExploreSelection);

        setupTab = new Tab("1 · Select sets", buildSetupPane());
        exploreTab = new Tab("2 · Explore map", buildExplorePane());
        guideTab = methodGuideTab();
        stageTabs = new TabPane(setupTab, exploreTab, guideTab);
        stageTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        exploreTab.setDisable(true);
        stageTabs.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b == exploreTab && lastResult == null && !uiBusy) {
                stageTabs.getSelectionModel().select(setupTab);
            }
        });

        root.setCenter(stageTabs);
        root.setBottom(statusBar());
    }

    /** Prefill mechanistic layer from a GSEA report / edb directory. */
    public void loadFromDirectory(File dir) {
        loadFromDirectory(dir, true);
    }

    public void loadFromDirectory(File dir, boolean asMechanistic) {
        if (dir == null) {
            return;
        }
        if (asMechanistic) {
            mechLoad.setDirectory(dir);
        } else {
            phenoLoad.setDirectory(dir);
        }
        runCatalogRefresh();
    }

    /** Whether the mechanistic ({@code true}) or phenotypic ({@code false}) layer has a path set. */
    public boolean isLayerLoaded(boolean mechanistic) {
        return mechanistic ? mechLoad.hasSelection() : phenoLoad.hasSelection();
    }

    private VBox buildSetupPane() {
        GridPane thresholds = new GridPane();
        thresholds.setHgap(8);
        thresholds.setVgap(6);
        thresholds.addRow(0, new Label("Min |NES|"), minNes, new Label("Max NOM pVal"), maxNp, new Label("Max FDR"), maxFdr);
        thresholds.add(separateLayerThresholds, 0, 1, 3, 1);
        thresholds.add(phenoThresholdRow, 0, 2, 6, 1);

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

        optionControls.addAll(List.of(
                interactomeSource, stringMode, minInteraction, maxPathLength, topKBridges, includeNeighbors,
                neighborLimit, pathLengthPenalty, directionWeight, sharedDriverDirectionWeight, signorLevel,
                signorOrganism, signorQueryType, signorProteinOnly, signorDirectOnly, signorPathways,
                enableSourceEnrichment, nullPermutations, msigdbPath, minNes, maxNp, maxFdr,
                separateLayerThresholds, phenoMinNes, phenoMaxNp, phenoMaxFdr));

        TitledPane optionsPane = new TitledPane("Interactome & scoring", opts);
        optionsPane.setExpanded(true);
        optionsPane.setAnimated(true);

        HBox actions = FxButtons.row(integrateBtn, helpBtn, openJobBtn);

        HBox layers = new HBox(12, mechLoad.root, phenoLoad.root);
        HBox.setHgrow(mechLoad.root, Priority.ALWAYS);
        HBox.setHgrow(phenoLoad.root, Priority.ALWAYS);

        VBox mechPicker = geneSetPickerPanel("Mechanistic gene sets", mechSetStatus, mechSearch, mechShowFailing,
                mechFollowBtn, mechClearBtn, mechSetTable);
        VBox phenoPicker = geneSetPickerPanel("Phenotypic gene sets", phenoSetStatus, phenoSearch, phenoShowFailing,
                phenoFollowBtn, phenoClearBtn, phenoSetTable);
        SplitPane setSplit = new SplitPane(mechPicker, phenoPicker);
        setSplit.setDividerPositions(0.5);
        VBox.setVgrow(setSplit, Priority.ALWAYS);

        VBox setup = new VBox(8, layers, thresholds, optionsPane, actions, setSplit);
        setup.setPadding(new Insets(10, 12, 8, 12));
        VBox.setVgrow(setSplit, Priority.ALWAYS);
        return setup;
    }

    private static VBox optionGroup(String title, String help, javafx.scene.Node content) {
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
        interactomeSource.setButtonCell(interactomeSourceCell());
        interactomeSource.setCellFactory(lv -> interactomeSourceCell());
    }

    private static javafx.scene.control.ListCell<String> interactomeSourceCell() {
        return new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(switch (item) {
                    case "fused" -> "Fused (SIGNOR + STRING)";
                    case "signor" -> "SIGNOR (directed causal)";
                    case "string" -> "STRING (functional + regulatory)";
                    case "none" -> "None (set co-membership only)";
                    default -> item;
                });
            }
        };
    }

    private void styleStringModeCombo() {
        stringMode.setPrefWidth(140);
        stringMode.setButtonCell(stringModeCell());
        stringMode.setCellFactory(lv -> stringModeCell());
    }

    private static javafx.scene.control.ListCell<String> stringModeCell() {
        return new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(switch (item) {
                    case "integrated" -> "Integrated";
                    case "regulatory" -> "Regulatory";
                    case "functional" -> "Functional";
                    case "physical" -> "Physical";
                    default -> item;
                });
            }
        };
    }

    private void styleSignorQueryCombo() {
        signorQueryType.setPrefWidth(200);
        signorQueryType.setButtonCell(signorQueryCell());
        signorQueryType.setCellFactory(lv -> signorQueryCell());
    }

    private static javafx.scene.control.ListCell<String> signorQueryCell() {
        return new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(switch (item) {
                    case "connect" -> "Connect — links among enrichment genes";
                    case "all" -> "All — every relation involving enrichment genes";
                    default -> item;
                });
            }
        };
    }

    private static VBox geneSetPickerPanel(String title, Label status, TextField search, CheckBox showFailing,
            Button followBtn, Button clearBtn, TableView<CoreMapGeneSetRow> table) {
        Label heading = new Label(title);
        heading.getStyleClass().add("coremap-card-heading");
        status.getStyleClass().addAll("gsea-muted", "coremap-meta");
        HBox tools = new HBox(8, followBtn, clearBtn, showFailing);
        tools.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(search, Priority.ALWAYS);
        VBox box = new VBox(6, heading, status, search, tools, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    private BorderPane buildExplorePane() {
        StackPane.setAlignment(selectionOverlay, javafx.geometry.Pos.BOTTOM_RIGHT);
        StackPane.setMargin(selectionOverlay, new Insets(12));

        // Match CoreMap: graph fills the stage; legend is a docked strip below (not overlaid).
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
        SplitPane split = new SplitPane(vizColumn, insightSidebar);
        split.setOrientation(Orientation.HORIZONTAL);
        split.setDividerPositions(0.72);

        HBox.setHgrow(graphSearch, Priority.ALWAYS);
        graphSearch.setMaxWidth(220);
        HBox actions = FxButtons.row(adjustSetsBtn, rescoreBtn, cancelBtn,
                new Label("View"), viewMode, geneVisibilityLabel, geneVisibility,
                zoomOutBtn, zoomInBtn, fitBtn, reflowBtn, graphSearch,
                exportPngMenu, exportJsonMenu, exportTsvBtn, saveJobBtn);
        VBox top = new VBox(8, actions);
        top.setPadding(new Insets(10, 12, 0, 12));
        BorderPane explore = new BorderPane();
        explore.setTop(top);
        explore.setCenter(split);
        BorderPane.setMargin(split, new Insets(8, 12, 8, 12));
        return explore;
    }

    /** Rebuild legend contents for the active view mode (CoreMap explore-legend parity). */
    private void refreshLegendStrip() {
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

    private static HBox legendSwatch(String color, String text, double radius) {
        Region swatch = new Region();
        swatch.setMinSize(12, 12);
        swatch.setPrefSize(12, 12);
        swatch.setMaxSize(12, 12);
        swatch.setStyle("-fx-background-color: " + color + "; -fx-background-radius: " + radius
                + "; -fx-border-color: #555555; -fx-border-width: 1; -fx-border-radius: " + radius + ";");
        Label label = legendLabel(text);
        HBox row = new HBox(5, swatch, label);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }

    private static HBox legendGlyph(String glyph, String fill, String text) {
        Label swatch = new Label(glyph);
        swatch.setStyle("-fx-text-fill: " + fill
                + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-min-width: 12;");
        Label label = legendLabel(text);
        HBox row = new HBox(5, swatch, label);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }

    /** Diamond with split mech|pheno fill — matches CoreMap shared-driver swatch. */
    private static HBox legendSharedDriver(String text) {
        Region swatch = new Region();
        swatch.setMinSize(12, 12);
        swatch.setPrefSize(12, 12);
        swatch.setMaxSize(12, 12);
        swatch.setStyle("-fx-background-color: linear-gradient(to right, #D97706 50%, #E11D48 50%);");
        javafx.scene.shape.Polygon diamond = new javafx.scene.shape.Polygon(
                6, 0,
                12, 6,
                6, 12,
                0, 6);
        swatch.setClip(diamond);
        Label label = legendLabel(text);
        HBox row = new HBox(5, swatch, label);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
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
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }

    private static Label legendLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("coremap-legend-label");
        return l;
    }

    private void showSelectionOverlay(boolean show) {
        selectionOverlay.showOverlay(show);
    }

    private void setSelectionContent(javafx.scene.Node content) {
        selectionOverlay.setContent(content);
    }

    private void clearSelectionContent() {
        selectionOverlay.clearContent();
    }

    private CoreMapLayerLoadUi.Host layerLoadHost() {
        return new CoreMapLayerLoadUi.Host() {
            @Override
            public javafx.scene.Node windowOwner() {
                return root;
            }

            @Override
            public void setSuppressAutoParse(boolean suppress) {
                suppressAutoParse = suppress;
            }

            @Override
            public void scheduleCatalogRefresh() {
                FxCoreMapPane.this.scheduleCatalogRefresh();
            }

            @Override
            public void onLayerCleared(boolean mechanistic) {
                invalidateAfterLayerClear(mechanistic);
            }
        };
    }

    private void wireInsightListeners() {
        insightSidebar.setSelectionListener(new CoreMapInsightSidebar.SelectionListener() {
            @Override
            public void onBridgesSelected(List<Bridge> bridges) {
                selectedGraphGenes.clear();
                if (bridges == null || bridges.isEmpty()) {
                    return;
                }
                List<String> genes = new ArrayList<>();
                Set<String> seen = new HashSet<>();
                for (Bridge bridge : bridges) {
                    addBridgeFocusGenes(bridge, genes, seen);
                }
                if (!focusPathOrReveal(genes, CoreMapCascadeUi.collectCascadeEdgePairs(bridges))) {
                    statusLabel.setText("Cascade genes are not in the current graph view.");
                }
                if (bridges.size() == 1) {
                    setSelectionContent(CoreMapSelectionCards.bridgeCard(bridges.get(0),
                            gene -> focusGeneFromCard(gene)));
                } else {
                    setSelectionContent(CoreMapSelectionCards.bridgesCard(bridges));
                }
                showSelectionOverlay(true);
            }

            @Override
            public void onDriversSelected(List<SharedDriverSummary> drivers) {
                selectedGraphGenes.clear();
                List<String> genes = new ArrayList<>();
                for (SharedDriverSummary d : drivers) {
                    if (d != null && d.gene != null && !d.gene.isBlank()) {
                        genes.add(d.gene);
                    }
                }
                showGeneListSelection(genes, drivers.size() == 1
                        ? CoreMapSelectionCards.driverCard(drivers.get(0))
                        : null);
            }

            @Override
            public void onHubsSelected(List<LayerHubSummary> hubs) {
                selectedGraphGenes.clear();
                List<String> genes = new ArrayList<>();
                for (LayerHubSummary h : hubs) {
                    if (h != null && h.gene != null && !h.gene.isBlank()) {
                        genes.add(h.gene);
                    }
                }
                showGeneListSelection(genes, hubs.size() == 1
                        ? CoreMapSelectionCards.hubCard(hubs.get(0))
                        : null);
            }

            @Override
            public void onClearRequested() {
                selectedGraphGenes.clear();
                graphView.clearFocus();
                clearSelectionContent();
                showSelectionOverlay(false);
            }
        });
    }

    private void showGeneListSelection(List<String> genes, javafx.scene.Node singleCard) {
        if (genes.isEmpty()) {
            return;
        }
        // Hubs/drivers: neighborhood highlight (CoreMap selectedIds), not cascade path mode.
        if (!focusNeighborhoodOrReveal(genes)) {
            statusLabel.setText("Selected gene(s) are not in the current graph view.");
        }
        if (genes.size() == 1 && singleCard != null) {
            setSelectionContent(singleCard);
        } else {
            setSelectionContent(CoreMapSelectionCards.genesCard(genes, lastResult, this::focusGeneFromCard));
        }
        showSelectionOverlay(true);
    }

    private void focusGeneFromCard(String gene) {
        focusNeighborhoodOrReveal(List.of(gene));
        showNodeDetail(gene);
        showSelectionOverlay(true);
    }

    private void onGraphNodeSelected(String id, boolean additive) {
        insightSidebar.clearUiSelection();
        if (id == null) {
            return;
        }
        if (!additive) {
            selectedGraphGenes.clear();
            selectedGraphGenes.add(id);
            graphView.focusNeighborhood(List.of(id));
            showNodeDetail(id);
            showSelectionOverlay(true);
            return;
        }
        if (selectedGraphGenes.contains(id)) {
            selectedGraphGenes.remove(id);
        } else {
            selectedGraphGenes.add(id);
        }
        if (selectedGraphGenes.isEmpty()) {
            clearExploreSelection();
            return;
        }
        graphView.focusNeighborhood(List.copyOf(selectedGraphGenes));
        if (selectedGraphGenes.size() == 1) {
            showNodeDetail(selectedGraphGenes.get(0));
        } else {
            setSelectionContent(CoreMapSelectionCards.genesCard(
                    List.copyOf(selectedGraphGenes), lastResult, this::focusGeneFromCard));
        }
        showSelectionOverlay(true);
    }

    private void onGraphEdgeSelected(String edgeDataJson) {
        insightSidebar.clearUiSelection();
        selectedGraphGenes.clear();
        edu.mit.broad.coremap.CoreMapTypes.GraphEdgeData edge =
                edu.mit.broad.coremap.CoreMapJson.parseEdgeDataJson(edgeDataJson);
        if (edge == null) {
            return;
        }
        Bridge setLink = findSetLinkBridge(edge);
        // CoreMap: set_link selection uses cascade path focus, not edge neighborhood.
        if (setLink != null) {
            List<String> genes = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            addBridgeFocusGenes(setLink, genes, seen);
            focusPathOrReveal(genes, CoreMapCascadeUi.collectCascadeEdgePairs(List.of(setLink)));
            setSelectionContent(CoreMapSelectionCards.edgeCard(edge, setLink, this::focusGeneFromCard));
            showSelectionOverlay(true);
            return;
        }
        if (edge.id != null) {
            graphView.focusEdge(edge.id);
        }
        setSelectionContent(CoreMapSelectionCards.edgeCard(edge, null, this::focusGeneFromCard));
        showSelectionOverlay(true);
    }

    private Bridge findSetLinkBridge(edu.mit.broad.coremap.CoreMapTypes.GraphEdgeData edge) {
        if (edge == null || lastResult == null || lastResult.bridges == null) {
            return null;
        }
        if (!"set_link".equals(edge.edgeKind)) {
            return null;
        }
        String mech = null;
        String pheno = null;
        if (edge.id != null && edge.id.startsWith("setlink:")) {
            String rest = edge.id.substring("setlink:".length());
            int arrow = rest.indexOf("->");
            if (arrow > 0) {
                mech = rest.substring(0, arrow);
                pheno = rest.substring(arrow + 2);
            }
        }
        if (mech == null && edge.source != null && edge.source.startsWith("set:mechanistic:")) {
            mech = edge.source.substring("set:mechanistic:".length());
        }
        if (pheno == null && edge.target != null && edge.target.startsWith("set:phenotypic:")) {
            pheno = edge.target.substring("set:phenotypic:".length());
        }
        Bridge best = null;
        for (Bridge b : lastResult.bridges) {
            if (b == null) {
                continue;
            }
            if (mech != null && !mech.equals(b.mechanisticSet)) {
                continue;
            }
            if (pheno != null && !pheno.equals(b.phenotypicSet)) {
                continue;
            }
            if (best == null || b.bridgeScore > best.bridgeScore) {
                best = b;
            }
        }
        return best;
    }

    /**
     * Focus cascade path; if missing under cascade/mechanism visibility, widen to all and retry
     * after the async graph reload completes.
     */
    private boolean focusPathOrReveal(List<String> genes, Set<String> edgePairs) {
        if (genes == null || genes.isEmpty()) {
            return false;
        }
        if (graphView.focusPath(genes, edgePairs)) {
            return true;
        }
        return revealGenesInGraph(genes, edgePairs);
    }

    /** Neighborhood focus for hubs/drivers/single genes (stable camera). */
    private boolean focusNeighborhoodOrReveal(List<String> genes) {
        if (genes == null || genes.isEmpty()) {
            return false;
        }
        if (graphView.focusNeighborhood(genes)) {
            return true;
        }
        return revealGenesInGraph(genes, null);
    }

    private boolean revealGenesInGraph(List<String> genes, Set<String> edgePairs) {
        if ("sets".equals(viewMode.getValue()) || "all".equals(geneVisibility.getValue())) {
            return false;
        }
        suppressVisibilityListener = true;
        try {
            geneVisibility.getSelectionModel().select("all");
        } finally {
            suppressVisibilityListener = false;
        }
        if (lastResult == null) {
            return false;
        }
        ViewMode mode = switch (viewMode.getValue() != null ? viewMode.getValue() : "connectome") {
            case "genes" -> ViewMode.GENES;
            case "sets" -> ViewMode.SETS;
            default -> ViewMode.CONNECTOME;
        };
        graphView.showResult(lastResult, mode, graphInteractomeSource(), GeneVisibility.ALL, genes, edgePairs);
        return true;
    }

    private void clearExploreSelection() {
        selectedGraphGenes.clear();
        insightSidebar.clearUiSelection();
        graphView.clearFocus();
        clearSelectionContent();
        showSelectionOverlay(false);
    }

    private static void addBridgeFocusGenes(Bridge bridge, List<String> genes, Set<String> seen) {
        if (bridge.nodes != null) {
            for (String g : bridge.nodes) {
                if (g != null && seen.add(g)) {
                    genes.add(g);
                }
            }
        }
        if (bridge.supportingPaths != null) {
            for (var support : bridge.supportingPaths) {
                if (support.nodes == null) {
                    continue;
                }
                for (String g : support.nodes) {
                    if (g != null && seen.add(g)) {
                        genes.add(g);
                    }
                }
            }
        }
        if (bridge.mechanisticSet != null) {
            String id = "set:mechanistic:" + bridge.mechanisticSet;
            if (seen.add(id)) {
                genes.add(id);
            }
        }
        if (bridge.phenotypicSet != null) {
            String id = "set:phenotypic:" + bridge.phenotypicSet;
            if (seen.add(id)) {
                genes.add(id);
            }
        }
    }

    private void showSetupStage() {
        if (stageTabs != null) {
            stageTabs.getSelectionModel().select(setupTab);
        }
    }

    private void showExploreStage() {
        if (stageTabs != null && exploreTab != null) {
            exploreTab.setDisable(false);
            stageTabs.getSelectionModel().select(exploreTab);
        }
    }

    private void showGuideStage() {
        if (stageTabs != null && guideTab != null) {
            stageTabs.getSelectionModel().select(guideTab);
        }
    }

    private Tab methodGuideTab() {
        TextArea guide = new TextArea(CoreMapMethodGuideText.text());
        guide.setEditable(false);
        guide.setWrapText(true);
        return new Tab("How it works", guide);
    }

    private Button browseMsigdbButton() {
        Button browse = xapps.gsea.fx.FxEllipsisButton.create("Browse MSigDB JSON");
        browse.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("MSigDB gene set JSON");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
            FxFileChooserUtil.seedInitialDirectory(chooser, msigdbPath.getText());
            File selected = chooser.showOpenDialog(FxFileChooserUtil.windowOf(root));
            if (selected != null) {
                msigdbPath.setText(selected.getAbsolutePath());
                FxFileChooserUtil.registerOpened(selected);
            }
        });
        return browse;
    }

    private HBox statusBar() {
        HBox box = new HBox(10, statusLabel, progressBar);
        HBox.setHgrow(statusLabel, Priority.ALWAYS);
        box.setPadding(new Insets(4, 12, 8, 12));
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return box;
    }

    private void wireGeneSetPickerControls() {
        mechFollowBtn.setOnAction(e -> {
            mechFollowThresholds = true;
            refreshLiveSelection(true);
        });
        phenoFollowBtn.setOnAction(e -> {
            phenoFollowThresholds = true;
            refreshLiveSelection(false);
        });
        mechClearBtn.setOnAction(e -> {
            mechFollowThresholds = false;
            applyingThresholdRefresh = true;
            for (CoreMapGeneSetRow r : mechSetRows) {
                r.included.set(false);
            }
            applyingThresholdRefresh = false;
            updateSetStatusLabels();
            updateGeneSetFilters();
        });
        phenoClearBtn.setOnAction(e -> {
            phenoFollowThresholds = false;
            applyingThresholdRefresh = true;
            for (CoreMapGeneSetRow r : phenoSetRows) {
                r.included.set(false);
            }
            applyingThresholdRefresh = false;
            updateSetStatusLabels();
            updateGeneSetFilters();
        });
        mechSearch.textProperty().addListener((o, a, b) -> updateGeneSetFilters());
        phenoSearch.textProperty().addListener((o, a, b) -> updateGeneSetFilters());
        mechShowFailing.selectedProperty().addListener((o, a, b) -> updateGeneSetFilters());
        phenoShowFailing.selectedProperty().addListener((o, a, b) -> updateGeneSetFilters());
    }

    private void wireThresholdLiveListeners() {
        Runnable refresh = () -> {
            if (!mechCatalog.isEmpty() || !phenoCatalog.isEmpty()) {
                refreshLiveSelection(true);
                refreshLiveSelection(false);
            }
        };
        minNes.valueProperty().addListener((o, a, b) -> refresh.run());
        maxNp.valueProperty().addListener((o, a, b) -> refresh.run());
        maxFdr.valueProperty().addListener((o, a, b) -> refresh.run());
        phenoMinNes.valueProperty().addListener((o, a, b) -> refresh.run());
        phenoMaxNp.valueProperty().addListener((o, a, b) -> refresh.run());
        phenoMaxFdr.valueProperty().addListener((o, a, b) -> refresh.run());
        separateLayerThresholds.selectedProperty().addListener((o, a, b) -> refresh.run());
    }

    private void configureSetTable(TableView<CoreMapGeneSetRow> table, SortedList<CoreMapGeneSetRow> rows) {
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
                    setStyle("");
                } else {
                    setText(item ? "pass" : "fail");
                    setStyle(item ? "-fx-text-fill: #047857;" : "-fx-text-fill: #b45309;");
                }
            }
        });

        TableColumn<CoreMapGeneSetRow, String> id = new TableColumn<>("Gene set");
        id.setCellValueFactory(c -> c.getValue().setId);
        id.setPrefWidth(200);

        TableColumn<CoreMapGeneSetRow, Number> nes = new TableColumn<>("NES");
        nes.setCellValueFactory(c -> c.getValue().nes);
        nes.setPrefWidth(70);

        TableColumn<CoreMapGeneSetRow, Number> np = new TableColumn<>("NOM p");
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

    private void onIncludedToggled(boolean mechanistic) {
        if (applyingThresholdRefresh) {
            return;
        }
        if (mechanistic) {
            mechFollowThresholds = false;
        } else {
            phenoFollowThresholds = false;
        }
        updateSetStatusLabels();
    }

    private void scheduleCatalogRefresh() {
        if (suppressAutoParse) {
            return;
        }
        if (catalogDebounce == null) {
            catalogDebounce = new PauseTransition(Duration.millis(350));
            catalogDebounce.setOnFinished(e -> runCatalogRefresh());
        }
        catalogDebounce.playFromStart();
    }

    /**
     * Parse loaded EDB layers into a full gene-set catalog. Thresholds and include
     * toggles update live afterward — no manual re-preview.
     */
    private void runCatalogRefresh() {
        if (catalogDebounce != null) {
            catalogDebounce.stop();
        }
        File mechDir = mechLoad.hasSelection() ? mechLoad.resolveDirQuiet() : null;
        File phenoDir = phenoLoad.hasSelection() ? phenoLoad.resolveDirQuiet() : null;
        if (mechDir == null && phenoDir == null) {
            if (!mechLoad.hasSelection() && !phenoLoad.hasSelection()) {
                return;
            }
            statusLabel.setText("Waiting for a valid GSEA report folder…");
            return;
        }
        final int gen = ++catalogParseGen;
        setBusy(true, "Parsing enrichment catalog…");
        Thread t = new Thread(() -> {
            try {
                CompletableFuture<EnrichmentParseResult> mechFut = mechDir != null
                        ? CompletableFuture.supplyAsync(() -> {
                            try {
                                return EdbEnrichmentParser.parseReportDirectory(mechDir, 0, 1, 1, false);
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                        })
                        : CompletableFuture.completedFuture(null);
                CompletableFuture<EnrichmentParseResult> phenoFut = phenoDir != null
                        ? CompletableFuture.supplyAsync(() -> {
                            try {
                                return EdbEnrichmentParser.parseReportDirectory(phenoDir, 0, 1, 1, false);
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                        })
                        : CompletableFuture.completedFuture(null);
                EnrichmentParseResult mech;
                EnrichmentParseResult pheno;
                try {
                    mech = mechFut.join();
                    pheno = phenoFut.join();
                } catch (RuntimeException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof Exception e) {
                        throw e;
                    }
                    throw ex;
                }
                List<GeneSetSummary> mechSets = mech != null
                        ? EnrichmentMath.summarizeGenesets(mech.rows, 0, 1, 1)
                        : List.of();
                List<GeneSetSummary> phenoSets = pheno != null
                        ? EnrichmentMath.summarizeGenesets(pheno.rows, 0, 1, 1)
                        : List.of();
                Platform.runLater(() -> {
                    if (gen != catalogParseGen) {
                        return;
                    }
                    Set<String> restoreMech = pendingMechSetIds;
                    Set<String> restorePheno = pendingPhenoSetIds;
                    pendingMechSetIds = null;
                    pendingPhenoSetIds = null;
                    mechParsed = mech;
                    phenoParsed = pheno;
                    mechCatalog = mechSets;
                    phenoCatalog = phenoSets;
                    rebuildSetRowsFromCatalog(true);
                    rebuildSetRowsFromCatalog(false);
                    if (restoreMech != null) {
                        applyInclusion(true, restoreMech);
                    } else {
                        mechFollowThresholds = true;
                        refreshLiveSelection(true);
                    }
                    if (restorePheno != null) {
                        applyInclusion(false, restorePheno);
                    } else {
                        phenoFollowThresholds = true;
                        refreshLiveSelection(false);
                    }
                    integrateBtn.setDisable(mechParsed == null && phenoParsed == null);
                    updateIntegrateButtonLabel();
                    setBusy(false, String.format(Locale.ROOT,
                            "Ready — %d mech sets, %d pheno sets%s.",
                            mechSets.size(), phenoSets.size(),
                            (mechParsed == null) != (phenoParsed == null)
                                    ? " (one layer only)"
                                    : ""));
                });
            } catch (Throwable ex) {
                klog.error("CoreMap catalog parse failed", ex);
                Platform.runLater(() -> {
                    if (gen != catalogParseGen) {
                        return;
                    }
                    setBusy(false, "Could not parse gene sets: " + ex.getMessage());
                    Application.getWindowManager().showError("Could not parse GSEA enrichment", ex);
                });
            }
        }, "coremap-catalog");
        t.setDaemon(true);
        t.start();
    }

    private void runIntegrate() {
        if (mechParsed == null && phenoParsed == null) {
            Application.getWindowManager().showMessage("Load a GSEA result first.");
            return;
        }
        InteractomeSource src = InteractomeSource.fromWire(interactomeSource.getValue());
        if (src != InteractomeSource.NONE && !liveApiConfirmed) {
            if (!Application.getWindowManager().showConfirm(CoreMapWorkspace.LIVE_API_CONFIRM)) {
                return;
            }
            liveApiConfirmed = true;
        }
        cancelFlag.set(false);
        setBusy(true, "Integrating…");
        showExploreStage();
        IntegrateRequest req = buildRequest();
        req.cancelled = cancelFlag;
        Thread t = new Thread(() -> {
            try {
                IntegrationResult result = CoreMapPipeline.integrate(req, this::onProgress);
                Platform.runLater(() -> {
                    applyResult(result);
                    setBusy(false, summarize(result));
                });
            } catch (Throwable ex) {
                boolean cancelled = cancelFlag.get()
                        || (ex.getMessage() != null && ex.getMessage().toLowerCase(Locale.ROOT).contains("cancelled"));
                klog.error(cancelled ? "CoreMap integrate cancelled" : "CoreMap integrate failed", ex);
                Platform.runLater(() -> {
                    setBusy(false, cancelled ? "Integrate cancelled." : "Integrate failed: " + ex.getMessage());
                    if (lastResult == null && exploreTab != null) {
                        exploreTab.setDisable(true);
                        showSetupStage();
                    }
                    if (!cancelled) {
                        Application.getWindowManager().showError("CoreMap integrate failed", ex);
                    }
                });
            }
        }, "coremap-integrate");
        t.setDaemon(true);
        t.start();
    }

    private void runRescore() {
        if (sessionId == null) {
            Application.getWindowManager().showMessage("Integrate first.");
            return;
        }
        if (!canRescoreWithCurrentOptions()) {
            Application.getWindowManager().showMessage(
                    "Interactome source or fetch threshold changed — click Integrate to rebuild.");
            return;
        }
        cancelFlag.set(false);
        setBusy(true, "Rescoring…");
        IntegrationOptions opts = buildOptions();
        String sid = sessionId;
        Thread t = new Thread(() -> {
            try {
                IntegrationResult result = CoreMapPipeline.rescore(sid, opts, this::onProgress, cancelFlag);
                Platform.runLater(() -> {
                    applyResult(result);
                    setBusy(false, "Rescored — " + summarize(result));
                });
            } catch (Throwable ex) {
                boolean cancelled = cancelFlag.get()
                        || (ex.getMessage() != null && ex.getMessage().toLowerCase(Locale.ROOT).contains("cancelled"));
                klog.error(cancelled ? "CoreMap rescore cancelled" : "CoreMap rescore failed", ex);
                Platform.runLater(() -> {
                    setBusy(false, cancelled ? "Rescore cancelled." : "Rescore failed: " + ex.getMessage());
                    if (!cancelled) {
                        Application.getWindowManager().showError("CoreMap rescore failed", ex);
                    }
                });
            }
        }, "coremap-rescore");
        t.setDaemon(true);
        t.start();
    }

    private IntegrateRequest buildRequest() {
        IntegrateRequest req = new IntegrateRequest();
        req.mechanistic = mechParsed;
        req.phenotypic = phenoParsed;
        req.minNes = minNes.getValue();
        req.maxNp = maxNp.getValue();
        req.maxFdr = maxFdr.getValue();
        if (separateLayerThresholds.isSelected()) {
            req.phenoMinNes = phenoMinNes.getValue();
            req.phenoMaxNp = phenoMaxNp.getValue();
            req.phenoMaxFdr = phenoMaxFdr.getValue();
        }
        Set<String> mechIds = includedSetIds(mechSetRows);
        Set<String> phenoIds = includedSetIds(phenoSetRows);
        // Explicit include list (threshold-follow or manual); empty → no sets from that layer
        req.mechanisticSetIds = mechIds;
        req.phenotypicSetIds = phenoIds;
        req.options = buildOptions();
        req.replaceSessionId = sessionId;
        return req;
    }

    private IntegrationOptions buildOptions() {
        IntegrationOptions o = IntegrationOptions.defaults();
        o.interactomeSource = InteractomeSource.fromWire(interactomeSource.getValue());
        o.stringMode = StringMode.fromWire(stringMode.getValue());
        o.minInteractionScore = minInteraction.getValue();
        o.maxPathLength = maxPathLength.getValue();
        o.pathLengthPenalty = pathLengthPenalty.getValue();
        o.directionWeight = directionWeight.getValue();
        o.sharedDriverDirectionWeight = sharedDriverDirectionWeight.getValue();
        o.topKBridges = topKBridges.getValue();
        o.includeExtraNeighbors = includeNeighbors.isSelected();
        o.neighborLimit = neighborLimit.getValue();
        o.signorLevel = signorLevel.getValue();
        o.signorOrganism = CoreMapOrganism.fromDisplayLabel(signorOrganism.getValue());
        o.signorQueryType = signorQueryType.getValue() != null ? signorQueryType.getValue() : "connect";
        o.signorProteinOnly = signorProteinOnly.isSelected();
        o.signorDirectOnly = signorDirectOnly.isSelected();
        o.signorPathways = parseCsv(signorPathways.getText());
        o.enableSourceEnrichment = enableSourceEnrichment.isSelected();
        String msig = msigdbPath.getText();
        o.msigdbPath = msig != null && !msig.isBlank() ? msig.trim() : null;
        o.nullPermutations = nullPermutations.getValue();
        return o;
    }

    private static List<String> parseCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }
        List<String> out = new ArrayList<>();
        for (String part : raw.split("[,;\\s]+")) {
            if (!part.isBlank()) {
                out.add(part.trim());
            }
        }
        return out;
    }

    private void applyResult(IntegrationResult result) {
        applyResult(result, false);
    }

    private void applyResult(IntegrationResult result, boolean fromSavedJob) {
        lastResult = result;
        if (fromSavedJob) {
            if (sessionId != null) {
                CoreMapPipeline.dropSession(sessionId);
            }
            sessionId = null;
            sessionProviderKey = null;
            sessionFetchedMinScore = null;
        } else {
            sessionId = result.sessionId;
            sessionProviderKey = result.stats != null && result.stats.get("provider_key") != null
                    ? String.valueOf(result.stats.get("provider_key"))
                    : CoreMapPipeline.interactomeProviderKey(buildOptions());
            Object fetched = result.stats != null ? result.stats.get("fetched_min_score") : null;
            sessionFetchedMinScore = fetched instanceof Number ? ((Number) fetched).doubleValue() : null;
            if (sessionFetchedMinScore == null && sessionId != null) {
                sessionFetchedMinScore = CoreMapPipeline.sessionFetchedMinScore(sessionId);
            }
        }
        updateRescoreAvailability();
        exportPngMenu.setDisable(false);
        exportJsonMenu.setDisable(false);
        exportTsvBtn.setDisable(result.bridges == null || result.bridges.isEmpty());
        saveJobBtn.setDisable(false);
        insightSidebar.setResult(result);
        // Partial / no-bridge maps: cascade-only filter hides most LE genes (CoreMap explore).
        // Saved jobs already record the user's Genes filter — don't override it.
        if (!fromSavedJob) {
            maybeWidenGeneVisibility(result);
        }
        insightSidebar.selectPreferredInsightTab();
        refreshGraph();
        showExploreStage();
        if (!fromSavedJob) {
            showWarningsIfAny(result);
        }
    }

    /** When cascade is empty or a layer has no genes, default Genes filter to all. */
    private void maybeWidenGeneVisibility(IntegrationResult result) {
        if (result == null) {
            return;
        }
        double mech = numStat(result.stats, "mechanistic_gene_count", "mechanistic_genes");
        double pheno = numStat(result.stats, "phenotypic_gene_count", "phenotypic_genes");
        double cascades = numStat(result.stats, "cascade_count", "bridges");
        if (cascades <= 0 && result.bridges != null) {
            cascades = result.bridges.size();
        }
        if (mech == 0 || pheno == 0 || cascades == 0) {
            suppressVisibilityListener = true;
            try {
                geneVisibility.getSelectionModel().select("all");
            } finally {
                suppressVisibilityListener = false;
            }
        }
    }

    private static double numStat(java.util.Map<String, Object> stats, String primary, String alias) {
        if (stats == null) {
            return 0;
        }
        Object v = stats.get(primary);
        if (v == null && alias != null) {
            v = stats.get(alias);
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        if (v != null) {
            try {
                return Double.parseDouble(v.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private static double numStat(java.util.Map<String, Object> stats, String key) {
        return numStat(stats, key, null);
    }

    /** Reload a saved CoreMap job folder (from History or Open CoreMap Job…). */
    public void loadJob(File jobDir) {
        if (jobDir == null) {
            return;
        }
        setBusy(true, "Loading CoreMap job…");
        Thread t = new Thread(() -> {
            try {
                CoreMapJob job = CoreMapJobStore.load(jobDir);
                Platform.runLater(() -> applyLoadedJob(job));
            } catch (Throwable ex) {
                klog.error("Could not load CoreMap job", ex);
                Platform.runLater(() -> {
                    setBusy(false, "Could not load CoreMap job.");
                    Application.getWindowManager().showError("Could not load CoreMap job", ex);
                });
            }
        }, "coremap-load-job");
        t.setDaemon(true);
        t.start();
    }

    private void applyLoadedJob(CoreMapJob job) {
        try {
            suppressAutoParse = true;
            try {
                if (job.mechanisticPath != null && !job.mechanisticPath.isBlank()) {
                    File f = new File(job.mechanisticPath);
                    if (f.exists()) {
                        mechLoad.setDirectory(f);
                    } else {
                        mechLoad.folderField.setText(job.mechanisticPath);
                    }
                }
                if (job.phenotypicPath != null && !job.phenotypicPath.isBlank()) {
                    File f = new File(job.phenotypicPath);
                    if (f.exists()) {
                        phenoLoad.setDirectory(f);
                    } else {
                        phenoLoad.folderField.setText(job.phenotypicPath);
                    }
                }
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
                applyOptionsToUi(job.options != null ? job.options : IntegrationOptions.defaults());
                if (job.viewMode != null) {
                    viewMode.getSelectionModel().select(job.viewMode);
                }
                if (job.geneVisibility != null && !job.geneVisibility.isBlank()) {
                    suppressVisibilityListener = true;
                    try {
                        geneVisibility.getSelectionModel().select(job.geneVisibility);
                    } finally {
                        suppressVisibilityListener = false;
                    }
                }
                refreshLegendStrip();
                mechParsed = null;
                phenoParsed = null;
                mechCatalog = List.of();
                phenoCatalog = List.of();
                pendingMechSetIds = new HashSet<>(job.mechanisticSetIdSet());
                pendingPhenoSetIds = new HashSet<>(job.phenotypicSetIdSet());
                mechFollowThresholds = false;
                phenoFollowThresholds = false;
                fillSetTableFromJob(mechSetRows, job.result, true, pendingMechSetIds);
                fillSetTableFromJob(phenoSetRows, job.result, false, pendingPhenoSetIds);
                updateGeneSetFilters();
                updateSetStatusLabels();
                applyResult(job.result, true);
            } finally {
                suppressAutoParse = false;
            }
            integrateBtn.setDisable(true);
            statusLabel.setText("Opened saved CoreMap job — parsing layers…");
            if (mechLoad.hasSelection() || phenoLoad.hasSelection()) {
                runCatalogRefresh();
            } else {
                setBusy(false, "Opened saved CoreMap job — set layer paths, then Integrate.");
            }
        } catch (Throwable ex) {
            klog.error("Could not apply CoreMap job", ex);
            setBusy(false, "Could not load CoreMap job.");
            Application.getWindowManager().showError("Could not load CoreMap job", ex);
        }
    }

    private void applyOptionsToUi(IntegrationOptions o) {
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

    private void fillSetTableFromJob(ObservableList<CoreMapGeneSetRow> rows, IntegrationResult result,
            boolean mechanistic, Set<String> selectedIds) {
        rows.clear();
        if (result == null || result.setEnrichments == null) {
            return;
        }
        double minN;
        double maxP;
        double maxF;
        if (mechanistic || !separateLayerThresholds.isSelected()) {
            minN = minNes.getValue();
            maxP = maxNp.getValue();
            maxF = maxFdr.getValue();
        } else {
            minN = phenoMinNes.getValue();
            maxP = phenoMaxNp.getValue();
            maxF = phenoMaxFdr.getValue();
        }
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
            rows.add(r);
        }
    }

    private boolean canRescoreWithCurrentOptions() {
        if (sessionId == null || sessionProviderKey == null) {
            return false;
        }
        IntegrationOptions opts = buildOptions();
        if (!sessionProviderKey.equals(CoreMapPipeline.interactomeProviderKey(opts))) {
            return false;
        }
        if (sessionFetchedMinScore != null
                && (opts.interactomeSource == InteractomeSource.STRING
                || opts.interactomeSource == InteractomeSource.FUSED)
                && opts.minInteractionScore < sessionFetchedMinScore) {
            return false;
        }
        return true;
    }

    private void updateRescoreAvailability() {
        if (uiBusy) {
            return;
        }
        boolean ok = sessionId != null && canRescoreWithCurrentOptions();
        rescoreBtn.setDisable(!ok);
        if (sessionId != null && !ok) {
            rescoreBtn.setText("Re-integrate required");
        } else {
            rescoreBtn.setText("Rescore");
        }
        updateIntegrateButtonLabel();
    }

    /** Dual-layer “Integrate” vs single-layer “Integrate (one layer)”. */
    private void updateIntegrateButtonLabel() {
        boolean both = mechParsed != null && phenoParsed != null;
        boolean neither = mechParsed == null && phenoParsed == null;
        if (neither) {
            integrateBtn.setText("Integrate");
        } else if (both) {
            integrateBtn.setText("Integrate");
        } else {
            integrateBtn.setText("Integrate (one layer)");
        }
    }

    private void showNodeDetail(String id) {
        if (id != null && id.startsWith("set:") && lastResult != null) {
            String[] parts = id.split(":", 3);
            if (parts.length == 3) {
                String layer = parts[1];
                String setId = parts[2];
                List<String> members = collectSetMemberGenes(layer, setId);
                SetEnrichmentMetric metric = findSetMetric(layer, setId);
                if (metric != null) {
                    setSelectionContent(CoreMapSelectionCards.setCard(metric, members, this::focusGeneFromCard));
                } else {
                    SetEnrichmentMetric stub = new SetEnrichmentMetric();
                    stub.setId = setId;
                    stub.setName = setId;
                    stub.layer = "phenotypic".equals(layer)
                            ? edu.mit.broad.coremap.CoreMapTypes.Layer.PHENOTYPIC
                            : edu.mit.broad.coremap.CoreMapTypes.Layer.MECHANISTIC;
                    setSelectionContent(CoreMapSelectionCards.setCard(stub, members, this::focusGeneFromCard));
                }
                graphView.focusNeighborhood(List.of(id));
                return;
            }
            setSelectionContent(CoreMapSelectionCards.plain("Gene set", id));
            graphView.focusNeighborhood(List.of(id));
            return;
        }
        if (lastResult == null || lastResult.elements == null || lastResult.elements.nodes == null) {
            setSelectionContent(CoreMapSelectionCards.plain("Node", id != null ? id : ""));
            return;
        }
        for (NodeElement n : lastResult.elements.nodes) {
            if (n.data != null && id.equals(n.data.id)) {
                setSelectionContent(CoreMapSelectionCards.geneCard(n.data, findSharedDriver(id)));
                graphView.focusNeighborhood(List.of(id));
                return;
            }
        }
        setSelectionContent(CoreMapSelectionCards.plain("Node", id != null ? id : ""));
        graphView.focusNeighborhood(List.of(id));
    }

    private SetEnrichmentMetric findSetMetric(String layer, String setId) {
        if (lastResult == null || lastResult.setEnrichments == null) {
            return null;
        }
        for (SetEnrichmentMetric m : lastResult.setEnrichments) {
            if (m != null && setId.equals(m.setId) && m.layer != null && layer.equals(m.layer.wire())) {
                return m;
            }
        }
        return null;
    }

    /** Genes in the current result that list this set (CoreMap set-card member_genes). */
    private List<String> collectSetMemberGenes(String layer, String setId) {
        List<String> out = new ArrayList<>();
        if (lastResult == null || lastResult.elements == null || lastResult.elements.nodes == null) {
            return out;
        }
        boolean mech = "mechanistic".equals(layer);
        for (NodeElement n : lastResult.elements.nodes) {
            if (n.data == null || n.data.id == null || n.data.id.startsWith("set:")) {
                continue;
            }
            List<String> sets = mech ? n.data.mechanisticSets : n.data.phenotypicSets;
            if (sets != null && sets.contains(setId)) {
                out.add(n.data.id);
            }
        }
        out.sort(String::compareToIgnoreCase);
        return out;
    }

    private SharedDriverSummary findSharedDriver(String gene) {
        if (gene == null || lastResult == null || lastResult.sharedDrivers == null) {
            return null;
        }
        for (SharedDriverSummary d : lastResult.sharedDrivers) {
            if (d != null && gene.equals(d.gene)) {
                return d;
            }
        }
        return null;
    }

    private void exportPng(boolean selectedOnly) {
        if (selectedOnly && !graphView.hasSelection()) {
            Application.getWindowManager().showMessage("Select genes, bridges, or edges on the map first.");
            return;
        }
        String dataUri = selectedOnly ? graphView.exportSelectedPngDataUri() : graphView.exportPngDataUri();
        if (dataUri == null || !dataUri.contains(",")) {
            Application.getWindowManager().showMessage("Graph PNG export is not ready yet.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(selectedOnly ? "Export selected CoreMap PNG" : "Export CoreMap PNG");
        chooser.setInitialFileName(selectedOnly ? "coremap-selected.png" : "coremap.png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        FxFileChooserUtil.seedInitialDirectory(chooser);
        File target = chooser.showSaveDialog(FxFileChooserUtil.windowOf(root));
        if (target == null) {
            return;
        }
        try {
            String b64 = dataUri.substring(dataUri.indexOf(',') + 1);
            Files.write(target.toPath(), Base64.getDecoder().decode(b64));
            FxFileChooserUtil.registerOpened(target);
            statusLabel.setText("Exported PNG → " + target.getName());
        } catch (Throwable ex) {
            klog.error("PNG export failed", ex);
            Application.getWindowManager().showError("Could not export PNG", ex);
        }
    }

    private void exportSelectedJson() {
        if (!graphView.hasSelection()) {
            Application.getWindowManager().showMessage(
                    "Nothing highlighted. Select a bridge, driver, hub, or node first.");
            return;
        }
        String json = graphView.exportSelectedJson();
        saveJsonFile(json, "Export selected CoreMap JSON", "coremap-selected.json");
    }

    private void exportCurrentJson() {
        String json = graphView.exportCurrentJson();
        if (json == null || json.isBlank()) {
            Application.getWindowManager().showMessage("Current graph view is not ready yet.");
            return;
        }
        saveJsonFile(json, "Export current CoreMap JSON", "coremap-current.json");
    }

    private void exportCompleteJson() {
        if (lastResult == null || lastResult.elements == null) {
            Application.getWindowManager().showMessage("Integrate first to export the complete graph.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export complete CoreMap JSON");
        chooser.setInitialFileName("coremap-complete.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        FxFileChooserUtil.seedInitialDirectory(chooser);
        File target = chooser.showSaveDialog(FxFileChooserUtil.windowOf(root));
        if (target == null) {
            return;
        }
        final IntegrationResult result = lastResult;
        Thread t = new Thread(() -> {
            try {
                String json = CoreMapJson.elementsJson(result.elements.nodes, result.elements.edges);
                Files.writeString(target.toPath(), json, StandardCharsets.UTF_8);
                Platform.runLater(() -> {
                    FxFileChooserUtil.registerOpened(target);
                    Application.getWindowManager().showMessage("Saved:\n" + target.getAbsolutePath());
                });
            } catch (Exception ex) {
                Platform.runLater(() -> Application.getWindowManager().showError("Could not export JSON", ex));
            }
        }, "coremap-export-complete-json");
        t.setDaemon(true);
        t.start();
    }

    private void saveJsonFile(String json, String title, String initialName) {
        if (json == null || json.isBlank()) {
            Application.getWindowManager().showMessage("Nothing to export.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.setInitialFileName(initialName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        FxFileChooserUtil.seedInitialDirectory(chooser);
        File target = chooser.showSaveDialog(FxFileChooserUtil.windowOf(root));
        if (target == null) {
            return;
        }
        final String payload = json;
        Thread t = new Thread(() -> {
            try {
                Files.writeString(target.toPath(), payload, StandardCharsets.UTF_8);
                Platform.runLater(() -> {
                    FxFileChooserUtil.registerOpened(target);
                    Application.getWindowManager().showMessage("Saved:\n" + target.getAbsolutePath());
                });
            } catch (Exception ex) {
                Platform.runLater(() -> Application.getWindowManager().showError("Could not export JSON", ex));
            }
        }, "coremap-export-json");
        t.setDaemon(true);
        t.start();
    }

    private void saveCoreMapJob() {
        if (lastResult == null) {
            Application.getWindowManager().showMessage("Integrate first to save a CoreMap job.");
            return;
        }
        File outDir;
        try {
            outDir = Application.getVdbManager().getDefaultOutputDir();
        } catch (Throwable t) {
            Application.getWindowManager().showError("Could not resolve default output folder", t);
            return;
        }
        if (outDir == null) {
            Application.getWindowManager().showMessage(
                    "Set a default output folder in Preferences before saving a CoreMap job.");
            return;
        }
        TextInputDialog labelDialog = new TextInputDialog("CoreMap");
        labelDialog.setTitle("Save CoreMap Job");
        labelDialog.setHeaderText("Saves to the default output folder and Analysis History.");
        labelDialog.setContentText("Analysis name (no spaces):");
        labelDialog.initOwner(FxFileChooserUtil.windowOf(root));
        xapps.gsea.fx.FxTheme.apply(labelDialog);
        var labelOpt = labelDialog.showAndWait();
        if (labelOpt.isEmpty() || labelOpt.get().isBlank()) {
            return;
        }
        String label = labelOpt.get().trim().replace(' ', '_');
        CoreMapJob job = buildJobSnapshot();
        byte[] png = CoreMapJobStore.decodePngDataUri(graphView.exportPngDataUri());
        setBusy(true, "Saving CoreMap job…");
        Thread t = new Thread(() -> {
            try {
                File reportDir = CoreMapJobStore.save(job, label, outDir, png);
                Platform.runLater(() -> {
                    setBusy(false, "Saved CoreMap job.");
                    FxFileChooserUtil.registerOpenedDir(reportDir);
                    Application.getWindowManager().showMessage(
                            "Saved CoreMap job (listed in Analysis History):\n" + reportDir.getAbsolutePath());
                });
            } catch (Throwable ex) {
                klog.error("Could not save CoreMap job", ex);
                Platform.runLater(() -> {
                    setBusy(false, "Could not save CoreMap job.");
                    Application.getWindowManager().showError("Could not save CoreMap job", ex);
                });
            }
        }, "coremap-save-job");
        t.setDaemon(true);
        t.start();
    }

    private void openCoreMapJob() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open CoreMap Job folder");
        FxFileChooserUtil.seedInitialDirectory(chooser);
        File dir = chooser.showDialog(FxFileChooserUtil.windowOf(root));
        if (dir == null) {
            return;
        }
        if (!CoreMapJobStore.looksLikeJobDir(dir)) {
            Application.getWindowManager().showMessage(
                    "Folder does not contain " + CoreMapJob.JOB_FILE + ":\n" + dir.getAbsolutePath());
            return;
        }
        loadJob(dir);
    }

    private CoreMapJob buildJobSnapshot() {
        CoreMapJob job = new CoreMapJob();
        File mechDir = mechLoad.resolveDirQuiet();
        File phenoDir = phenoLoad.resolveDirQuiet();
        job.mechanisticPath = mechDir != null ? mechDir.getAbsolutePath() : null;
        job.phenotypicPath = phenoDir != null ? phenoDir.getAbsolutePath() : null;
        job.mechanisticName = mechDir != null ? mechDir.getName() : null;
        job.phenotypicName = phenoDir != null ? phenoDir.getName() : null;
        job.minNes = minNes.getValue();
        job.maxNp = maxNp.getValue();
        job.maxFdr = maxFdr.getValue();
        job.separateLayerThresholds = separateLayerThresholds.isSelected();
        if (job.separateLayerThresholds) {
            job.phenoMinNes = phenoMinNes.getValue();
            job.phenoMaxNp = phenoMaxNp.getValue();
            job.phenoMaxFdr = phenoMaxFdr.getValue();
        }
        job.mechanisticSetIds = new ArrayList<>(includedSetIds(mechSetRows));
        job.phenotypicSetIds = new ArrayList<>(includedSetIds(phenoSetRows));
        job.options = buildOptions();
        job.viewMode = viewMode.getValue() != null ? viewMode.getValue() : "connectome";
        job.geneVisibility = geneVisibility.getValue() != null ? geneVisibility.getValue() : "cascade";
        job.providerKey = sessionProviderKey;
        job.fetchedMinScore = sessionFetchedMinScore;
        job.result = lastResult;
        return job;
    }

    private void exportBridgesTsv() {
        if (lastResult == null || lastResult.bridges == null || lastResult.bridges.isEmpty()) {
            Application.getWindowManager().showMessage("No bridges to export.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export bridges TSV");
        chooser.setInitialFileName("coremap-bridges.tsv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TSV", "*.tsv", "*.txt"));
        FxFileChooserUtil.seedInitialDirectory(chooser);
        File target = chooser.showSaveDialog(FxFileChooserUtil.windowOf(root));
        if (target == null) {
            return;
        }
        final IntegrationResult result = lastResult;
        Thread t = new Thread(() -> {
            try {
                Files.writeString(target.toPath(), CoreMapJobStore.formatBridgesTsv(result), StandardCharsets.UTF_8);
                Platform.runLater(() -> {
                    FxFileChooserUtil.registerOpened(target);
                    Application.getWindowManager().showMessage("Saved:\n" + target.getAbsolutePath());
                });
            } catch (Exception ex) {
                Platform.runLater(() -> Application.getWindowManager().showError("Could not export TSV", ex));
            }
        }, "coremap-export-tsv");
        t.setDaemon(true);
        t.start();
    }

    private void refreshGraph() {
        clearExploreSelection();
        reloadGraphView();
        updateGeneVisibilityEnabled();
    }

    private void reloadGraphView() {
        if (lastResult == null) {
            return;
        }
        ViewMode mode = switch (viewMode.getValue() != null ? viewMode.getValue() : "connectome") {
            case "genes" -> ViewMode.GENES;
            case "sets" -> ViewMode.SETS;
            default -> ViewMode.CONNECTOME;
        };
        GeneVisibility visibility = switch (geneVisibility.getValue() != null ? geneVisibility.getValue() : "cascade") {
            case "mechanism" -> GeneVisibility.MECHANISM;
            case "all" -> GeneVisibility.ALL;
            default -> GeneVisibility.CASCADE;
        };
        InteractomeSource src = graphInteractomeSource();
        graphView.showResult(lastResult, mode, src, visibility);
    }

    private void updateGeneVisibilityEnabled() {
        boolean sets = "sets".equals(viewMode.getValue());
        geneVisibility.setDisable(sets || uiBusy);
        geneVisibilityLabel.setDisable(sets || uiBusy);
    }

    /** Prefer the source used to build the result so changing the combo doesn't retint the view. */
    private InteractomeSource graphInteractomeSource() {
        if (lastResult != null && lastResult.stats != null) {
            Object wire = lastResult.stats.get("interactome_source");
            if (wire != null) {
                return InteractomeSource.fromWire(String.valueOf(wire));
            }
        }
        return InteractomeSource.fromWire(interactomeSource.getValue());
    }

    private void onProgress(BuildProgress p) {
        Platform.runLater(() -> {
            statusLabel.setText(p.phase.wire() + ": " + p.detail);
            progressBar.setProgress(progressFraction(p.phase));
        });
    }

    private static double progressFraction(BuildProgressPhase phase) {
        return switch (phase) {
            case PARSING -> 0.15;
            case FETCHING_INTERACTOME -> 0.40;
            case RANKING_CASCADES -> 0.75;
            case FINALIZING -> 0.92;
            case READY -> 1.0;
        };
    }

    private void setBusy(boolean busy, String msg) {
        uiBusy = busy;
        integrateBtn.setDisable(busy || (mechParsed == null && phenoParsed == null));
        cancelBtn.setDisable(!busy);
        adjustSetsBtn.setDisable(busy);
        progressBar.setVisible(busy);
        progressBar.setManaged(busy);
        if (busy) {
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        }
        mechLoad.setDisable(busy);
        phenoLoad.setDisable(busy);
        mechSetTable.setDisable(busy);
        phenoSetTable.setDisable(busy);
        for (javafx.scene.Node n : optionControls) {
            n.setDisable(busy);
        }
        if (!busy) {
            updateRescoreAvailability();
        } else {
            rescoreBtn.setDisable(true);
        }
        exportPngMenu.setDisable(busy || lastResult == null);
        exportJsonMenu.setDisable(busy || lastResult == null);
        exportTsvBtn.setDisable(busy || lastResult == null || lastResult.bridges == null
                || lastResult.bridges.isEmpty());
        saveJobBtn.setDisable(busy || lastResult == null);
        openJobBtn.setDisable(busy);
        helpBtn.setDisable(busy);
        viewMode.setDisable(busy);
        updateGeneVisibilityEnabled();
        statusLabel.setText(msg);
    }

    private static String summarize(IntegrationResult r) {
        if (r == null) {
            return "";
        }
        java.util.Map<String, Object> stats = r.stats != null ? r.stats : java.util.Map.of();
        String base = String.format(Locale.ROOT, "Nodes %s, edges %s, bridges %d, hubs %d, shared drivers %d",
                stats.getOrDefault("node_count", stats.getOrDefault("nodes", "?")),
                stats.getOrDefault("edge_count", stats.getOrDefault("edges", "?")),
                r.bridges != null ? r.bridges.size() : 0,
                r.layerHubs != null ? r.layerHubs.size() : 0,
                r.sharedDrivers != null ? r.sharedDrivers.size() : 0);
        StringBuilder extra = new StringBuilder();
        Object message = stats.get("message");
        if (message != null && !String.valueOf(message).isBlank()) {
            extra.append(" | ").append(message);
        }
        Object enrichStatus = stats.get("source_enrichment_status");
        Object enrichErr = stats.get("source_enrichment_error");
        if ("error".equals(String.valueOf(enrichStatus)) && enrichErr != null) {
            extra.append(" | Source enrichment warning");
        }
        Object warnings = stats.get("warnings");
        if (warnings instanceof List<?> list && !list.isEmpty()) {
            extra.append(" | ").append(list.size()).append(" warning(s)");
        }
        return base + extra;
    }

    private void invalidateAfterLayerClear(boolean mechanistic) {
        if (mechanistic) {
            mechParsed = null;
            mechCatalog = List.of();
            mechSetRows.clear();
            mechFollowThresholds = true;
            pendingMechSetIds = null;
        } else {
            phenoParsed = null;
            phenoCatalog = List.of();
            phenoSetRows.clear();
            phenoFollowThresholds = true;
            pendingPhenoSetIds = null;
        }
        updateSetStatusLabels();
        updateGeneSetFilters();
        if (sessionId != null) {
            CoreMapPipeline.dropSession(sessionId);
            sessionId = null;
            sessionProviderKey = null;
            sessionFetchedMinScore = null;
        }
        lastResult = null;
        insightSidebar.setResult(null);
        clearExploreSelection();
        graphView.clear();
        if (exploreTab != null) {
            exploreTab.setDisable(true);
        }
        showSetupStage();
        exportPngMenu.setDisable(true);
        exportJsonMenu.setDisable(true);
        exportTsvBtn.setDisable(true);
        saveJobBtn.setDisable(true);
        integrateBtn.setDisable(mechParsed == null && phenoParsed == null);
        updateIntegrateButtonLabel();
        updateRescoreAvailability();
        statusLabel.setText("Layer cleared — choose a GSEA result to reload sets.");
    }

    private void showWarningsIfAny(IntegrationResult result) {
        if (result == null || result.stats == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        Object enrichErr = result.stats.get("source_enrichment_error");
        if (enrichErr != null) {
            sb.append("Source enrichment: ").append(enrichErr).append('\n');
        }
        Object warnings = result.stats.get("warnings");
        if (warnings instanceof List<?> list) {
            for (Object w : list) {
                sb.append("• ").append(w).append('\n');
            }
        }
        if (sb.length() > 0) {
            statusLabel.setText("Warnings — " + sb.toString().trim().replace('\n', ' '));
        }
    }

    private void rebuildSetRowsFromCatalog(boolean mechanistic) {
        List<GeneSetSummary> catalog = mechanistic ? mechCatalog : phenoCatalog;
        ObservableList<CoreMapGeneSetRow> rows = mechanistic ? mechSetRows : phenoSetRows;
        rows.clear();
        for (GeneSetSummary s : catalog) {
            CoreMapGeneSetRow r = CoreMapGeneSetRow.from(s);
            r.included.addListener((o, a, b) -> onIncludedToggled(mechanistic));
            rows.add(r);
        }
    }

    /** Restore explicit inclusion from a saved job (or other snapshot); leave follow-mode off. */
    private void applyInclusion(boolean mechanistic, Set<String> setIds) {
        if (mechanistic) {
            mechFollowThresholds = false;
        } else {
            phenoFollowThresholds = false;
        }
        Set<String> ids = setIds != null ? setIds : Set.of();
        double minN;
        double maxP;
        double maxF;
        if (mechanistic || !separateLayerThresholds.isSelected()) {
            minN = minNes.getValue();
            maxP = maxNp.getValue();
            maxF = maxFdr.getValue();
        } else {
            minN = phenoMinNes.getValue();
            maxP = phenoMaxNp.getValue();
            maxF = phenoMaxFdr.getValue();
        }
        ObservableList<CoreMapGeneSetRow> rows = mechanistic ? mechSetRows : phenoSetRows;
        applyingThresholdRefresh = true;
        try {
            for (CoreMapGeneSetRow r : rows) {
                Double nes = Double.isNaN(r.nes.get()) ? null : r.nes.get();
                Double np = Double.isNaN(r.pValue.get()) ? null : r.pValue.get();
                Double fdr = Double.isNaN(r.fdr.get()) ? null : r.fdr.get();
                r.passed.set(EnrichmentMath.passesThresholds(nes, np, fdr, minN, maxP, maxF));
                r.included.set(ids.contains(r.setId.get()));
            }
            rows.sort((a, b) -> {
                int byPass = Boolean.compare(b.passed.get(), a.passed.get());
                if (byPass != 0) {
                    return byPass;
                }
                double an = Double.isNaN(a.nes.get()) ? 0 : Math.abs(a.nes.get());
                double bn = Double.isNaN(b.nes.get()) ? 0 : Math.abs(b.nes.get());
                return Double.compare(bn, an);
            });
        } finally {
            applyingThresholdRefresh = false;
        }
        updateGeneSetFilters();
        updateSetStatusLabels();
    }

    private void refreshLiveSelection(boolean mechanistic) {
        double minN;
        double maxP;
        double maxF;
        if (mechanistic || !separateLayerThresholds.isSelected()) {
            minN = minNes.getValue();
            maxP = maxNp.getValue();
            maxF = maxFdr.getValue();
        } else {
            minN = phenoMinNes.getValue();
            maxP = phenoMaxNp.getValue();
            maxF = phenoMaxFdr.getValue();
        }
        boolean follow = mechanistic ? mechFollowThresholds : phenoFollowThresholds;
        ObservableList<CoreMapGeneSetRow> rows = mechanistic ? mechSetRows : phenoSetRows;
        applyingThresholdRefresh = true;
        try {
            for (CoreMapGeneSetRow r : rows) {
                Double nes = Double.isNaN(r.nes.get()) ? null : r.nes.get();
                Double np = Double.isNaN(r.pValue.get()) ? null : r.pValue.get();
                Double fdr = Double.isNaN(r.fdr.get()) ? null : r.fdr.get();
                boolean passes = EnrichmentMath.passesThresholds(nes, np, fdr, minN, maxP, maxF);
                r.passed.set(passes);
                if (follow) {
                    r.included.set(passes);
                }
            }
            rows.sort((a, b) -> {
                int byPass = Boolean.compare(b.passed.get(), a.passed.get());
                if (byPass != 0) {
                    return byPass;
                }
                double an = Double.isNaN(a.nes.get()) ? 0 : Math.abs(a.nes.get());
                double bn = Double.isNaN(b.nes.get()) ? 0 : Math.abs(b.nes.get());
                return Double.compare(bn, an);
            });
        } finally {
            applyingThresholdRefresh = false;
        }
        updateGeneSetFilters();
        updateSetStatusLabels();
    }

    private void updateGeneSetFilters() {
        String mechQ = mechSearch.getText() != null ? mechSearch.getText().trim().toLowerCase(Locale.ROOT) : "";
        String phenoQ = phenoSearch.getText() != null ? phenoSearch.getText().trim().toLowerCase(Locale.ROOT) : "";
        boolean mechShow = mechShowFailing.isSelected();
        boolean phenoShow = phenoShowFailing.isSelected();
        mechFiltered.setPredicate(r -> {
            if (!mechShow && !r.passed.get()) {
                return false;
            }
            if (mechQ.isEmpty()) {
                return true;
            }
            String id = r.setId.get() != null ? r.setId.get().toLowerCase(Locale.ROOT) : "";
            return id.contains(mechQ);
        });
        phenoFiltered.setPredicate(r -> {
            if (!phenoShow && !r.passed.get()) {
                return false;
            }
            if (phenoQ.isEmpty()) {
                return true;
            }
            String id = r.setId.get() != null ? r.setId.get().toLowerCase(Locale.ROOT) : "";
            return id.contains(phenoQ);
        });
    }

    private void updateSetStatusLabels() {
        mechSetStatus.setText(formatSetStatus(mechSetRows, mechFollowThresholds));
        phenoSetStatus.setText(formatSetStatus(phenoSetRows, phenoFollowThresholds));
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

    @Override
    public String getTitle() {
        return "CoreMap";
    }

    @Override
    public String getIconResourceId() {
        return "coremap_logo.png";
    }

    @Override
    public javafx.scene.Node getContent() {
        return root;
    }
}

