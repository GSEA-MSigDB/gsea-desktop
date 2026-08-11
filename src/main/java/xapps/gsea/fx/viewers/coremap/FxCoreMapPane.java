/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.coremap;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.gsea_msigdb.gsea.ui.api.ViewPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.coremap.CoreMapJob;
import edu.mit.broad.coremap.CoreMapJobStore;
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
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import xapps.gsea.fx.widgets.FxButtons;
import xapps.gsea.fx.viewers.coremap.CoreMapGraphViews.GeneVisibility;
import xapps.gsea.fx.viewers.coremap.CoreMapGraphViews.ViewMode;
import org.gsea_msigdb.gsea.runtime.AppServices;
import org.gsea_msigdb.gsea.ui.api.FeatureHost;

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
    private final List<String> selectedGraphGenes = new ArrayList<>();

    private final CoreMapGraphView graphView = new CoreMapGraphView();
    private final AppServices svc;
    private final CoreMapExportController exportIo;
    private final CoreMapOptionsPanel optionsPanel;
    private final CoreMapSetTables setTables;
    private final CoreMapExploreStage exploreStage;

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

    private final CoreMapIntegrateController integrateController = new CoreMapIntegrateController(new CoreMapIntegrateController.Host() {
        @Override public EnrichmentParseResult mechanistic() { return mechParsed; }
        @Override public EnrichmentParseResult phenotypic() { return phenoParsed; }
        @Override public Set<String> mechanisticSetIds() { return setTables.mechanisticSetIds(); }
        @Override public Set<String> phenotypicSetIds() { return setTables.phenotypicSetIds(); }
        @Override public Double minNes() { return optionsPanel.minNes.getValue(); }
        @Override public Double maxNp() { return optionsPanel.maxNp.getValue(); }
        @Override public Double maxFdr() { return optionsPanel.maxFdr.getValue(); }
        @Override public boolean separateLayerThresholds() { return optionsPanel.separateLayerThresholds.isSelected(); }
        @Override public Double phenoMinNes() { return optionsPanel.phenoMinNes.getValue(); }
        @Override public Double phenoMaxNp() { return optionsPanel.phenoMaxNp.getValue(); }
        @Override public Double phenoMaxFdr() { return optionsPanel.phenoMaxFdr.getValue(); }
        @Override public String sessionId() { return sessionId; }
        @Override public AtomicBoolean cancelFlag() { return cancelFlag; }
        @Override public edu.mit.broad.xbench.core.api.WindowManager dialogs() {
            return svc.dialogs();
        }
        @Override public void setBusy(boolean busy, String message) { FxCoreMapPane.this.setBusy(busy, message); }
        @Override public void showExploreStage() { FxCoreMapPane.this.showExploreStage(); }
        @Override public void showSetupStage() { FxCoreMapPane.this.showSetupStage(); }
        @Override public void onProgress(BuildProgress progress) { FxCoreMapPane.this.onProgress(progress); }
        @Override public void applyResult(IntegrationResult result) { FxCoreMapPane.this.applyResult(result); }
        @Override public boolean liveApiConfirmed() { return liveApiConfirmed; }
        @Override public void setLiveApiConfirmed(boolean confirmed) { liveApiConfirmed = confirmed; }
        @Override public boolean canRescoreWithCurrentOptions() { return FxCoreMapPane.this.canRescoreWithCurrentOptions(); }
        @Override public IntegrationResult lastResult() { return lastResult; }
        @Override public void disableExploreIfEmpty() {
            if (exploreTab != null) {
                exploreTab.setDisable(true);
            }
        }
        @Override public CoreMapIntegrateController.IntegrationOptionsSnapshot optionsSnapshot() {
            return optionsPanel.optionsSnapshot();
        }
    });

    public FxCoreMapPane(FeatureHost host) {
        this(Objects.requireNonNull(host, "host").services());
    }

    private FxCoreMapPane(AppServices svc) {
        this.svc = Objects.requireNonNull(svc, "svc");
        this.exportIo = new CoreMapExportController(new CoreMapExportController.Host() {
            @Override public javafx.scene.Node owner() { return root; }
            @Override public AppServices services() { return FxCoreMapPane.this.svc; }
            @Override public CoreMapGraphView graphView() { return graphView; }
            @Override public IntegrationResult lastResult() { return lastResult; }
            @Override public CoreMapJob buildJobSnapshot() { return FxCoreMapPane.this.buildJobSnapshot(); }
            @Override public void loadJob(java.io.File dir) { FxCoreMapPane.this.loadJob(dir); }
            @Override public void setBusy(boolean busy, String message) { FxCoreMapPane.this.setBusy(busy, message); }
            @Override public void setStatus(String message) { statusLabel.setText(message); }
        });
        this.optionsPanel = new CoreMapOptionsPanel(new CoreMapOptionsPanel.Host() {
            @Override public javafx.scene.Node windowOwner() { return root; }

            @Override public void onThresholdsChanged() {
                if (setTables.hasCatalog()) {
                    setTables.refreshLiveSelection(true);
                    setTables.refreshLiveSelection(false);
                }
            }

            @Override public void onIntegrationOptionsChanged() {
                updateRescoreAvailability();
            }
        });
        this.setTables = new CoreMapSetTables(new CoreMapSetTables.Host() {
            @Override public double[] thresholdsForLayer(boolean mechanistic) {
                return optionsPanel.thresholdsForLayer(mechanistic);
            }
        });
        this.exploreStage = new CoreMapExploreStage(new CoreMapExploreStage.Host() {
            @Override public CoreMapGraphView graphView() { return graphView; }
            @Override public CoreMapSelectionOverlay selectionOverlay() { return selectionOverlay; }
            @Override public CoreMapInsightSidebar insightSidebar() { return insightSidebar; }
            @Override public Button adjustSetsBtn() { return adjustSetsBtn; }
            @Override public Button rescoreBtn() { return rescoreBtn; }
            @Override public Button cancelBtn() { return cancelBtn; }
            @Override public MenuButton exportPngMenu() { return exportPngMenu; }
            @Override public MenuButton exportJsonMenu() { return exportJsonMenu; }
            @Override public Button exportTsvBtn() { return exportTsvBtn; }
            @Override public Button saveJobBtn() { return saveJobBtn; }
            @Override public void onExploreViewChanged() { refreshGraph(); }
            @Override public void onViewModeChanged() { updateGeneVisibilityEnabled(); }
        });
        wireInsightListeners();
        exploreStage.graphSearch.setOnAction(e -> {
            String q = exploreStage.graphSearch.getText() != null ? exploreStage.graphSearch.getText().trim() : "";
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
            statusLabel.setText("No graph node matches “" + exploreStage.graphSearch.getText() + "”.");
        });

        FxButtons.stylePrimary(integrateBtn);
        FxButtons.styleSecondary(rescoreBtn);
        FxButtons.styleSecondary(cancelBtn);
        FxButtons.styleSecondary(helpBtn);
        FxButtons.styleSecondary(adjustSetsBtn);
        exportPngMenu.getStyleClass().add("gsea-button");
        exportJsonMenu.getStyleClass().add("gsea-button");
        FxButtons.styleSecondary(exportTsvBtn);
        FxButtons.styleSecondary(saveJobBtn);
        FxButtons.styleSecondary(openJobBtn);
        setTables.styleSecondaryButtons(FxButtons::styleSecondary);
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

        exportJsonMenu.getItems().setAll(exportSelectedJsonItem, exportCurrentJsonItem, exportCompleteJsonItem);
        exportPngMenu.getItems().setAll(exportPngViewItem, exportPngSelectedItem);

        integrateBtn.setOnAction(e -> runIntegrate());
        cancelBtn.setOnAction(e -> cancelFlag.set(true));
        rescoreBtn.setOnAction(e -> runRescore());
        helpBtn.setOnAction(e -> showGuideStage());
        adjustSetsBtn.setOnAction(e -> showSetupStage());
        exportPngViewItem.setOnAction(e -> exportIo.exportPng(false));
        exportPngSelectedItem.setOnAction(e -> exportIo.exportPng(true));
        exportSelectedJsonItem.setOnAction(e -> exportIo.exportSelectedJson());
        exportCurrentJsonItem.setOnAction(e -> exportIo.exportCurrentJson());
        exportCompleteJsonItem.setOnAction(e -> exportIo.exportCompleteJson());
        exportTsvBtn.setOnAction(e -> exportIo.exportBridgesTsv());
        saveJobBtn.setOnAction(e -> exportIo.saveCoreMapJob());
        openJobBtn.setOnAction(e -> exportIo.openCoreMapJob());

        graphView.setNodeSelectedAdditiveHandler((id, additive) -> Platform.runLater(() -> onGraphNodeSelected(id, additive)));
        graphView.setEdgeSelectedHandler(json -> Platform.runLater(() -> onGraphEdgeSelected(json)));
        graphView.setBackgroundTapHandler(() -> Platform.runLater(this::clearExploreSelection));

        selectionOverlay.setOnDismiss(this::clearExploreSelection);

        setupTab = new Tab("1 · Select sets", buildSetupPane());
        exploreTab = new Tab("2 · Explore map", exploreStage.buildPane());
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
        HBox actions = FxButtons.row(integrateBtn, helpBtn, openJobBtn);

        HBox layers = new HBox(12, mechLoad.root, phenoLoad.root);
        HBox.setHgrow(mechLoad.root, Priority.ALWAYS);
        HBox.setHgrow(phenoLoad.root, Priority.ALWAYS);

        SplitPane setSplit = setTables.buildSplitPane();
        VBox.setVgrow(setSplit, Priority.ALWAYS);

        VBox setup = new VBox(8, layers, optionsPanel.buildThresholdsGrid(), optionsPanel.buildOptionsPane(),
                actions, setSplit);
        setup.setPadding(new Insets(10, 12, 8, 12));
        VBox.setVgrow(setSplit, Priority.ALWAYS);
        return setup;
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
        if ("sets".equals(exploreStage.viewMode.getValue()) || "all".equals(exploreStage.geneVisibility.getValue())) {
            return false;
        }
        exploreStage.selectGeneVisibility("all", true);
        if (lastResult == null) {
            return false;
        }
        ViewMode mode = toViewMode(exploreStage.viewMode.getValue());
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
        TextArea guide = new TextArea(methodGuideText());
        guide.setEditable(false);
        guide.setWrapText(true);
        return new Tab("How it works", guide);
    }

    private HBox statusBar() {
        HBox box = new HBox(10, statusLabel, progressBar);
        HBox.setHgrow(statusLabel, Priority.ALWAYS);
        box.setPadding(new Insets(4, 12, 8, 12));
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return box;
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
                    Set<String> restoreMech = setTables.consumePendingSetIds(true);
                    Set<String> restorePheno = setTables.consumePendingSetIds(false);
                    mechParsed = mech;
                    phenoParsed = pheno;
                    setTables.setCatalog(true, mechSets);
                    setTables.setCatalog(false, phenoSets);
                    setTables.rebuildFromCatalog(true);
                    setTables.rebuildFromCatalog(false);
                    if (restoreMech != null) {
                        setTables.applyInclusion(true, restoreMech);
                    } else {
                        setTables.setFollowThresholds(true, true);
                        setTables.refreshLiveSelection(true);
                    }
                    if (restorePheno != null) {
                        setTables.applyInclusion(false, restorePheno);
                    } else {
                        setTables.setFollowThresholds(false, true);
                        setTables.refreshLiveSelection(false);
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
                    svc.dialogs().showError("Could not parse GSEA enrichment", ex);
                });
            }
        }, "coremap-catalog");
        t.setDaemon(true);
        t.start();
    }

    private void runIntegrate() {
        integrateController.runIntegrate();
    }

    private void runRescore() {
        integrateController.runRescore();
    }

    private IntegrateRequest buildRequest() {
        return integrateController.buildRequest();
    }

    private IntegrationOptions buildOptions() {
        return CoreMapIntegrateController.buildOptions(optionsPanel.optionsSnapshot());
    }

    private static List<String> parseCsv(String raw) {
        return CoreMapIntegrateController.parseCsv(raw);
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
            exploreStage.selectGeneVisibility("all", true);
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
                    svc.dialogs().showError("Could not load CoreMap job", ex);
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
                optionsPanel.applyJobThresholds(job);
                optionsPanel.applyIntegrationOptions(job.options != null ? job.options : IntegrationOptions.defaults());
                if (job.viewMode != null) {
                    exploreStage.viewMode.getSelectionModel().select(job.viewMode);
                }
                if (job.geneVisibility != null && !job.geneVisibility.isBlank()) {
                    exploreStage.selectGeneVisibility(job.geneVisibility, true);
                }
                exploreStage.refreshLegendStrip();
                mechParsed = null;
                phenoParsed = null;
                setTables.setCatalog(true, List.of());
                setTables.setCatalog(false, List.of());
                setTables.setPendingSetIds(true, new HashSet<>(job.mechanisticSetIdSet()));
                setTables.setPendingSetIds(false, new HashSet<>(job.phenotypicSetIdSet()));
                setTables.setFollowThresholds(true, false);
                setTables.setFollowThresholds(false, false);
                setTables.fillFromJob(job.result, true, new HashSet<>(job.mechanisticSetIdSet()));
                setTables.fillFromJob(job.result, false, new HashSet<>(job.phenotypicSetIdSet()));
                setTables.updateFiltersAndStatus();
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
            svc.dialogs().showError("Could not load CoreMap job", ex);
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

    private CoreMapJob buildJobSnapshot() {
        CoreMapJob job = new CoreMapJob();
        File mechDir = mechLoad.resolveDirQuiet();
        File phenoDir = phenoLoad.resolveDirQuiet();
        job.mechanisticPath = mechDir != null ? mechDir.getAbsolutePath() : null;
        job.phenotypicPath = phenoDir != null ? phenoDir.getAbsolutePath() : null;
        job.mechanisticName = mechDir != null ? mechDir.getName() : null;
        job.phenotypicName = phenoDir != null ? phenoDir.getName() : null;
        job.minNes = optionsPanel.minNes.getValue();
        job.maxNp = optionsPanel.maxNp.getValue();
        job.maxFdr = optionsPanel.maxFdr.getValue();
        job.separateLayerThresholds = optionsPanel.separateLayerThresholds.isSelected();
        if (job.separateLayerThresholds) {
            job.phenoMinNes = optionsPanel.phenoMinNes.getValue();
            job.phenoMaxNp = optionsPanel.phenoMaxNp.getValue();
            job.phenoMaxFdr = optionsPanel.phenoMaxFdr.getValue();
        }
        job.mechanisticSetIds = new ArrayList<>(setTables.mechanisticSetIds());
        job.phenotypicSetIds = new ArrayList<>(setTables.phenotypicSetIds());
        job.options = buildOptions();
        job.viewMode = exploreStage.viewMode.getValue() != null ? exploreStage.viewMode.getValue() : "connectome";
        job.geneVisibility = exploreStage.geneVisibility.getValue() != null
                ? exploreStage.geneVisibility.getValue() : "cascade";
        job.providerKey = sessionProviderKey;
        job.fetchedMinScore = sessionFetchedMinScore;
        job.result = lastResult;
        return job;
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
        ViewMode mode = toViewMode(exploreStage.viewMode.getValue());
        GeneVisibility visibility = switch (exploreStage.geneVisibility.getValue() != null
                ? exploreStage.geneVisibility.getValue() : "cascade") {
            case "mechanism" -> GeneVisibility.MECHANISM;
            case "all" -> GeneVisibility.ALL;
            default -> GeneVisibility.CASCADE;
        };
        InteractomeSource src = graphInteractomeSource();
        graphView.showResult(lastResult, mode, src, visibility);
    }

    private static ViewMode toViewMode(String wire) {
        return switch (wire != null ? wire : "connectome") {
            case "genes" -> ViewMode.GENES;
            case "sets" -> ViewMode.SETS;
            default -> ViewMode.CONNECTOME;
        };
    }

    private void updateGeneVisibilityEnabled() {
        exploreStage.updateGeneVisibilityEnabled(uiBusy);
    }

    /** Prefer the source used to build the result so changing the combo doesn't retint the view. */
    private InteractomeSource graphInteractomeSource() {
        if (lastResult != null && lastResult.stats != null) {
            Object wire = lastResult.stats.get("interactome_source");
            if (wire != null) {
                return InteractomeSource.fromWire(String.valueOf(wire));
            }
        }
        return InteractomeSource.fromWire(optionsPanel.interactomeSource.getValue());
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
        setTables.setDisable(busy);
        for (javafx.scene.Node n : optionsPanel.optionControls()) {
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
        exploreStage.setViewModeDisabled(busy);
        updateGeneVisibilityEnabled();
        statusLabel.setText(msg);
    }

    private static String summarize(IntegrationResult r) {
        return CoreMapIntegrateController.summarize(r);
    }

    private void invalidateAfterLayerClear(boolean mechanistic) {
        if (mechanistic) {
            mechParsed = null;
        } else {
            phenoParsed = null;
        }
        setTables.clearLayer(mechanistic);
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

    private static String methodGuideText() {
        return """
                How it works

                CoreMap links two precomputed GSEA results — a mechanistic layer and a phenotypic layer —
                through a gene/set interactome. It ranks short paths (Bridges) from mechanistic sets to
                phenotypic sets and finds shared drivers (genes in both leading edges). It does not run GSEA.

                Scope
                • Does: parse Broad GSEA EDB outputs, fetch a neighborhood interactome, score edges, search
                  mech-set → pheno-set paths, and draw one Cytoscape.js map.
                • Does not: run GSEA, prove causality, or apply tissue-specific expression filters. Ranked
                  Bridges are network candidates given the chosen interactome and search settings.

                Desktop notes (vs. the browser app)
                • Calls SIGNOR / STRING / fused APIs directly (no CORS proxy).
                • Inputs are GSEA report folders or the report cache (not ZIP uploads).
                • No demo interactome — use none (co-membership), SIGNOR, STRING, or fused.
                • Set selection and Rescore stay on the Select-sets tab; Bridges / Drivers / Hubs and the
                  graph are on the map stage. Zoom (+/−/Fit/Reflow) is in the explore toolbar.
                • Graph layout animation is off in WebView (same fCoSE spacing; use Fit after layout).
                • Selection does not zoom the camera; green highlight matches the browser CoreMap.

                Workflow
                1. Load mechanistic and/or phenotypic GSEA result folders (or cache picks).
                   One layer builds hubs and a within-layer network; both layers add Bridges and
                   shared drivers.
                2. Preview gene sets; adjust |NES| / NP / FDR; select sets to include.
                3. Choose interactome (default SIGNOR), scoring options, and optional source enrichment.
                4. Integrate → explore Bridges, shared drivers, layer hubs, and the graph.
                5. Rescore reuses the cached interactome when the provider settings are unchanged.

                Source enrichment
                When enabled, CoreMap maps enrichment set IDs to Reactome / KEGG (and GO/HPO when the
                interactome is “none”) and adds those edges before bridge search. Accession patterns work
                without a catalog; optional MSigDB JSON improves ID resolution.

                Bridge search & nulls
                Bridges are short paths between leading-edge genes of mechanistic and phenotypic sets.
                Optional null permutations estimate empirical_p by shuffling labels within degree buckets
                (statistic: path_jaccard).

                Shared drivers & hubs
                Shared drivers are genes in both layers’ leading edges, scored with direction agreement.
                Layer hubs are high-degree genes within one layer’s search neighborhood.

                Parameters (defaults match the CoreMap engine)
                • Min interaction score, max path length, path-length penalty, direction weight
                • Top-K bridges, extra neighbors, organism (Human HSA/9606 or Mouse MMU/10090),
                  SIGNOR level / query type / filters
                • STRING mode (integrated / functional / regulatory / physical)
                • Source enrichment on/off, null permutations

                Limits
                STRING/fused queries are capped for dense association graphs. Search uses a beam width and
                node budget. Layout (fCoSE) affects display only — not ranking.
                """;
    }
}

