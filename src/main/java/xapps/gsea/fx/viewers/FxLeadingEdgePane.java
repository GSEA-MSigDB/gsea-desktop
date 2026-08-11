/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.genepattern.gsea.LeadingEdgeAnalysis;
import org.gsea_msigdb.gsea.ui.api.ViewPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResult;
import edu.mit.broad.genome.parsers.ParserFactory;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import xapps.gsea.GseaWebResources;
import xapps.gsea.fx.heatmap.FxHeatMapView;
import xapps.gsea.fx.heatmap.FxJaccardLegend;
import xapps.gsea.fx.jobs.JobRuntime;
import xapps.gsea.fx.params.FxGseaReportLoadUi;
import xapps.gsea.fx.params.FxReportCacheChooser;
import xapps.gsea.fx.plots.FxPlotPane;
import xapps.gsea.fx.viewers.report.EnrichmentResultRow;
import xapps.gsea.fx.viewers.report.EnrichmentResultTable;
import xapps.gsea.fx.viewers.report.LeadingEdgeOutputs;
import xapps.gsea.fx.viewers.report.PhenotypeLabels;
import xtools.api.Tool;
import edu.mit.broad.genome.plots.PlotBuilders;
import xtools.api.param.ToolParamSet;
import xtools.gsea.LeadingEdgeTool;
import org.gsea_msigdb.gsea.runtime.AppServices;
import org.gsea_msigdb.gsea.ui.api.FeatureHost;

/**
 * Interactive Leading Edge viewer:
 * load EDB, select ≥2 gene sets, run in-memory analysis (2×2 dashboard),
 * or build the HTML report via {@link LeadingEdgeTool}.
 * <p>
 * Completed HTML report reopen (history / Jobs panel) uses
 * {@link xapps.gsea.fx.viewers.FxReportViewer} + {@link xapps.gsea.fx.viewers.report.LeadingEdgeReportExplorer}.
 */
public class FxLeadingEdgePane implements ViewPage {

    private static final Logger klog = LoggerFactory.getLogger(FxLeadingEdgePane.class);

    /** Weak set so closed workspace tabs do not leak via JobRuntime listeners. */
    private static final Set<FxLeadingEdgePane> LIVE =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final JobRuntime.JobListener SHARED_STATUS = job -> {
        if (job == null || !isLeadingEdgeJob(job)) {
            return;
        }
        if (!job.getState().isSuccess() || job.getReportDir() == null || !job.getReportDir().isDirectory()) {
            return;
        }
        File reportDir = job.getReportDir();
        for (FxLeadingEdgePane pane : LIVE) {
            if (pane.root.getScene() != null) {
                pane.showLeadingEdgeOutputs(reportDir);
            }
        }
    };
    /** Runtime the shared listener is currently registered on (null if none). */
    private static volatile JobRuntime listenerRuntime;

    private static boolean isLeadingEdgeJob(xapps.gsea.fx.jobs.JobRecord job) {
        Tool tool = job.getTool();
        return tool != null && tool.getClass().getName().contains("LeadingEdge");
    }

    private final BorderPane root = new BorderPane();
    private final TabPane mainTabs = new TabPane();
    private final FxGseaReportLoadUi loadUi;
    private final JobRuntime jobRuntime;
    private final AppServices svc;
    private int analysisRun = 0;

    public FxLeadingEdgePane(FeatureHost host) {
        this(Objects.requireNonNull(host, "host").services(), host.jobs());
    }

    private FxLeadingEdgePane(AppServices svc, JobRuntime jobRuntime) {
        this.svc = Objects.requireNonNull(svc, "svc");
        this.jobRuntime = Objects.requireNonNull(jobRuntime, "jobRuntime");
        LIVE.add(this);
        ensureSharedListener();
        loadUi = new FxGseaReportLoadUi(FxReportCacheChooser.single(), root, false, false,
                this::loadGseaResults);
        mainTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        root.setTop(loadUi.root);
        root.setCenter(mainTabs);
    }

    /** Append heatmap / HTML Report tabs after {@link LeadingEdgeTool} succeeds. */
    private void showLeadingEdgeOutputs(File reportDir) {
        for (Tab tab : LeadingEdgeOutputs.buildTabs(reportDir, true)) {
            mainTabs.getTabs().add(tab);
            mainTabs.getSelectionModel().select(tab);
        }
    }

    /**
     * 's LeadingEdgeWidget. Each loaded EDB owns its
     * selection, filter, summary, and tool parameters so result tabs never
     * overwrite one another.
     */
    private final class ResultsView extends BorderPane {
        private final EnrichmentDb edb;
        private final File gseaResultDir;
        private final EnrichmentResultTable resultTable = new EnrichmentResultTable();
        private final TableView<EnrichmentResultRow> table = resultTable.getTable();
        private final Label positivePhenotypeLabel = new Label();
        private final Label negativePhenotypeLabel = new Label();
        private final Label selectionLabel = new Label("For 0 selected gene sets: ");
        private final Button runAnalysisButton = new Button("Run leading edge analysis");
        private final Button buildHtmlButton = new Button("Build HTML Report");

        ResultsView(EnrichmentDb edb, File gseaResultDir, EnrichmentResult[] results) {
            this.edb = edb;
            this.gseaResultDir = gseaResultDir;
            resultTable.setResults(results);

            runAnalysisButton.setGraphic(xapps.gsea.fx.widgets.FxFileIcons.forResource("Run16.png"));
            runAnalysisButton.setOnAction(e -> runInteractiveAnalysis(this));
            buildHtmlButton.setGraphic(xapps.gsea.fx.widgets.FxFileIcons.forResource("Run16.png"));
            buildHtmlButton.setOnAction(e -> runHtmlReport(this));
            xapps.gsea.fx.widgets.FxButtons.stylePrimary(runAnalysisButton);
            xapps.gsea.fx.widgets.FxButtons.styleSecondary(buildHtmlButton);
            applyPhenotypeLabels(edb, positivePhenotypeLabel, negativePhenotypeLabel);
            HBox phenotypeRow = new HBox(0, positivePhenotypeLabel, negativePhenotypeLabel);

            Button help = helpButton();
            HBox filterRow = xapps.gsea.fx.widgets.FxButtons.row(help, selectionLabel, runAnalysisButton, buildHtmlButton);
            VBox top = new VBox(8, phenotypeRow, filterRow);
            top.setPadding(new Insets(8, 12, 4, 12));

            table.getSelectionModel().getSelectedItems().addListener(
                    (javafx.collections.ListChangeListener<EnrichmentResultRow>) c -> updateSelectionUi());

            setTop(top);
            setCenter(table);
            BorderPane.setMargin(table, new Insets(0, 12, 12, 12));
            updateSelectionUi();
        }

        private void updateSelectionUi() {
            int n = table.getSelectionModel().getSelectedItems().size();
            selectionLabel.setText("For " + n + " selected gene sets: ");
            boolean enabled = n >= 2;
            runAnalysisButton.setDisable(!enabled);
            buildHtmlButton.setDisable(!enabled);
        }
    }

    private void ensureSharedListener() {
        synchronized (FxLeadingEdgePane.class) {
            if (listenerRuntime != null && listenerRuntime != jobRuntime) {
                listenerRuntime.removeListener(SHARED_STATUS);
            }
            listenerRuntime = jobRuntime;
            // Idempotent: re-binds after JobRuntime.dispose() cleared listeners.
            jobRuntime.addListener(SHARED_STATUS);
        }
    }

    private void loadGseaResults() {
        loadUi.ifResolvedSingle(this::loadEdb);
    }

    /** Open an enrichment result folder (object-cache EnrichmentDb / folder browse). */
    public void loadFromDirectory(File dir) {
        if (dir == null) {
            return;
        }
        loadUi.setDirectory(dir);
        loadEdb(dir);
    }

    private void loadEdb(File dir) {
        Thread worker = new Thread(() -> {
            try {
                EnrichmentDb loaded = ParserFactory.readEdb(dir, true);
                EnrichmentResult[] results = LeadingEdgeAnalysis.getAllResultsFromEdb(loaded);
                Platform.runLater(() -> {
                    ResultsView view = new ResultsView(loaded, dir, results);
                    Tab tab = new Tab("GSEA Results", view);
                    tab.setClosable(true);
                    mainTabs.getTabs().add(tab);
                    mainTabs.getSelectionModel().select(tab);
                });
            } catch (Throwable t) {
                klog.error("Failed to load EDB from {}", dir, t);
                Platform.runLater(() -> {
                    svc.dialogs().showError("Trouble loading enrichment database", t);
                });
            }
        }, "gsea-load-edb");
        worker.setDaemon(true);
        worker.start();
    }

    private static void applyPhenotypeLabels(EnrichmentDb loaded, Label positive, Label negative) {
        PhenotypeLabels pos = PhenotypeLabels.from(loaded, true);
        PhenotypeLabels neg = PhenotypeLabels.from(loaded, false);
        positive.setText("positive phenotype: " + pos.shortName() + "   ");
        positive.getStyleClass().setAll("gsea-text-phenotype-positive");
        negative.setText("negative phenotype: " + neg.shortName());
        negative.getStyleClass().setAll("gsea-text-phenotype-negative");
    }

    private List<String> selectedNamesOrWarn(ResultsView view) {
        if (view == null || view.edb == null || view.gseaResultDir == null) {
            svc.dialogs().showMessage("Load a GSEA result folder first.");
            return null;
        }
        List<EnrichmentResultRow> selected = new ArrayList<>(view.table.getSelectionModel().getSelectedItems());
        if (selected.size() < 2) {
            svc.dialogs().showMessage("Select at least two gene sets in the table.");
            return null;
        }
        return selected.stream().map(r -> r.nameProperty().get()).collect(Collectors.toList());
    }

    private void runInteractiveAnalysis(ResultsView view) {
        List<String> names = selectedNamesOrWarn(view);
        if (names == null) {
            return;
        }
        view.runAnalysisButton.setDisable(true);

        EnrichmentDb edbRef = view.edb;
        Thread worker = new Thread(() -> {
            try {
                LeadingEdgeAnalysis.Result result = LeadingEdgeAnalysis.runAnalysis(
                        edbRef, names.toArray(new String[0]));
                Platform.runLater(() -> {
                    showInteractiveResult(result);
                    view.updateSelectionUi();
                });
            } catch (Throwable t) {
                klog.error("Interactive leading edge analysis failed", t);
                Platform.runLater(() -> {
                    svc.dialogs().showError(
                            "An error occurred while running leading edge analysis", t);
                    view.updateSelectionUi();
                });
            }
        }, "gsea-le-interactive");
        worker.setDaemon(true);
        worker.start();
    }

    private Button helpButton() {
        Button help = new Button("Help");
        help.setGraphic(xapps.gsea.fx.widgets.FxFileIcons.forResource("Help16_v2.gif"));
        xapps.gsea.fx.widgets.FxButtons.styleSecondary(help);
        help.setOnAction(e -> {
            String url = GseaWebResources.getGseaHelpURL() + "GSEA/GSEA_User_Guide/Interpret-Leading-Edge";
            try {
                xapps.gsea.fx.FxDesktopUtil.openUrl(url);
            } catch (Exception ex) {
                svc.dialogs().showError("Could not open Leading Edge help", ex);
            }
        });
        return help;
    }

    private void showInteractiveResult(LeadingEdgeAnalysis.Result result) {
        analysisRun++;

        FxHeatMapView leHeat = new FxHeatMapView();
        boolean binaryMembership = result.getGeneScores() == null;
        leHeat.setData(result.getClusteredMorphed(), result.getLeadingEdgeColorScheme(), false,
                binaryMembership);
        leHeat.setUiNaming("Gene Set", "Gene", false);
        leHeat.setOptionsDialogOptions(false, false, false, false);
        leHeat.setHtmlLookupDir(result.getResultDirectory());

        FxHeatMapView simHeat = new FxHeatMapView();
        simHeat.setData(result.getSimilarityDataset(), result.getSimilarityColorScheme(), true);
        simHeat.setUiNaming("Gene Set", "Gene Set", false);
        simHeat.setOptionsDialogOptions(false, false, false, false);
        simHeat.setGeneSetsForTooltips(result.getReorderedGeneSets());
        final int numSimCols = result.getSimilarityDataset() != null
                ? result.getSimilarityDataset().getNumCol() : 15;
        BorderPane simPane = new BorderPane(simHeat.getNode());
        Runnable refreshLegend = () -> {
            int legendWidth = Math.max(1, numSimCols * simHeat.getCellSize());
            simPane.setTop(FxJaccardLegend.create(legendWidth));
        };
        refreshLegend.run();
        simHeat.setOnCellSizeChanged(sz -> refreshLegend.run());

        BorderPane lePane = leHeat.getNode();
        lePane.setMinSize(0, 0);
        simPane.setMinSize(0, 0);
        SplitPane top = new SplitPane(lePane, simPane);
        top.setOrientation(Orientation.HORIZONTAL);
        top.setDividerPositions(0.5);

        java.util.concurrent.atomic.AtomicInteger selectedGene = new java.util.concurrent.atomic.AtomicInteger(-1);
        FxPlotPane geneChart = new FxPlotPane("Gene Histogram");
        geneChart.setSpec(
                PlotBuilders.geneHistogram(result.getFeatureFrequency(), result.getGeneScores(), selectedGene.get()),
                720, 360);
        if (result.getFeatureFrequency() != null) {
            geneChart.setItemNames(result.getFeatureFrequency().getRankedNamesArray());
            geneChart.setItemClickHandler(item -> {
                selectedGene.set(item);
                geneChart.setSpec(
                        PlotBuilders.geneHistogram(result.getFeatureFrequency(), result.getGeneScores(), item),
                        720, 360);
                String gene = result.getFeatureFrequency().getRankName(item);
                leHeat.selectColumnByName(gene);
            });
        }

        VBox jaccardBox = new VBox(8);
        FxPlotPane jaccardChart = new FxPlotPane("Jaccard Histogram of Gene Sets");
        jaccardChart.setSpec(
                PlotBuilders.jaccardHistogram(result.getJaccardDistrib(), 0.02),
                720, 360);
        TextField binField = new TextField("0.02");
        binField.setPrefWidth(80);
        Button updateBin = new Button("Update");
        xapps.gsea.fx.widgets.FxButtons.styleToolbar(updateBin);
        updateBin.setOnAction(e -> {
            try {
                double bw = Double.parseDouble(binField.getText().trim());
                if (bw < 0 || bw > 1) {
                    svc.dialogs().showMessage("Bin width must be between zero and one.");
                    return;
                }
                if (bw == 0) {
                    return;
                }
                jaccardChart.setSpec(
                        PlotBuilders.jaccardHistogram(result.getJaccardDistrib(), bw),
                        720, 360);
            } catch (NumberFormatException nfe) {
                svc.dialogs().showMessage("Bin width is not a number.");
            }
        });
        jaccardBox.getChildren().addAll(
                new HBox(8, new Label("Bin Width:"), binField, updateBin),
                jaccardChart.getNode());
        VBox.setVgrow(jaccardChart.getNode(), Priority.ALWAYS);

        BorderPane geneNode = new BorderPane(geneChart.getNode());
        geneNode.setMinSize(0, 0);
        jaccardBox.setMinSize(0, 0);
        SplitPane bottom = new SplitPane(geneNode, jaccardBox);
        bottom.setOrientation(Orientation.HORIZONTAL);
        bottom.setDividerPositions(0.55);
        bottom.setMinSize(0, 0);
        top.setMinSize(0, 0);

        SplitPane grid = new SplitPane(top, bottom);
        grid.setOrientation(Orientation.VERTICAL);
        grid.setDividerPositions(0.55);
        grid.setPadding(new Insets(8));

        Tab tab = new Tab("Leading Edge Analysis-" + analysisRun, grid);
        tab.setClosable(true);
        mainTabs.getTabs().add(tab);
        mainTabs.getSelectionModel().select(tab);
    }


    private void runHtmlReport(ResultsView view) {
        List<String> names = selectedNamesOrWarn(view);
        if (names == null) {
            return;
        }

        view.buildHtmlButton.setDisable(true);

        Thread worker = new Thread(() -> {
            try {
                LeadingEdgeTool tool = new LeadingEdgeTool();
                ToolParamSet paramSet = (ToolParamSet) tool.getParamSet();
                paramSet.getParam("altDelim").setValue(";");
                paramSet.getParam("gsets").setValue(StringUtils.join(names, ";"));
                paramSet.getParam("dir").setValue(view.edb.getEdbDir());

                String runId = jobRuntime.start(tool, paramSet, Thread.MIN_PRIORITY);
                Platform.runLater(() -> {
                    jobRuntime.showParamErrorIfNeeded(runId);
                    view.updateSelectionUi();
                });
            } catch (Throwable t) {
                klog.error("Leading edge HTML report failed to start", t);
                Platform.runLater(() -> {
                    svc.dialogs().showError(
                            "An error occurred while building the HTML report", t);
                    view.updateSelectionUi();
                });
            }
        }, "gsea-leading-edge-html");
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    public String getTitle() {
        return "Leading edge analysis";
    }

    @Override
    public String getIconResourceId() {
        return "Lev16_b.gif";
    }

    @Override
    public javafx.scene.Node getContent() {
        return root;
    }
}
