/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.gsea_msigdb.gsea.ui.api.ViewPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.cytoscape.CytoscapeCyrest;
import edu.mit.broad.cytoscape.EnrichmentMapParameters;
import edu.mit.broad.xbench.core.api.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import xapps.gsea.fx.params.FxFileChooserUtil;
import xapps.gsea.fx.params.FxReportCacheChooser;
import xapps.gsea.fx.params.FxReportCacheSupport;
import xtools.munge.CollapseDataset;

/**
 * JavaFX Enrichment Map / Cytoscape launcher. Load stage mirrors Swing
 * {@code EnrichmentMapInputPanel} (report cache XOR directory → closable analysis tabs);
 * each analysis tab mirrors {@code EnrichmentMapParameterPanel}.
 */
public class FxEnrichmentMapPane implements ViewPage {

    public static final String LAUNCH_MSG =
            "Please launch Cytoscape 3.3+ with the Enrichment Map plug-in before continuing.";

    private static final Logger klog = LoggerFactory.getLogger(FxEnrichmentMapPane.class);

    private final BorderPane root = new BorderPane();
    private final TabPane analysisTabs = new TabPane();
    private final FxReportCacheChooser cacheChooser = FxReportCacheChooser.multiInterval();
    private final TextField loadDirField = new TextField();
    private int analysisCount = 0;

    public FxEnrichmentMapPane() {
        analysisTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);

        Tab loadTab = new Tab("Load GSEA Results", buildLoadPane());
        loadTab.setClosable(false);
        analysisTabs.getTabs().add(loadTab);
        root.setCenter(analysisTabs);
    }

    /** Prefill the report directory and load an Enrichment Map analysis tab. */
    public void loadFromDirectory(File dir) {
        if (dir == null) {
            return;
        }
        cacheChooser.clearSelection();
        loadDirField.setText(dir.getAbsolutePath());
        FxFileChooserUtil.registerOpenedDir(dir);
        loadGseaResults();
    }

    private VBox buildLoadPane() {
        // Swing EnrichmentMapInputPanel has no section title on the load form.

        // Swing EnrichmentMapInputPanel DirParam: label beside field; no field prompt.
        HBox.setHgrow(loadDirField, Priority.ALWAYS);
        // Swing DirParam → GDirFieldPlusChooser: #EAFFEA + path coloring + Ellipsis.png.
        if (!loadDirField.getStyleClass().contains("gsea-dir-field")) {
            loadDirField.getStyleClass().add("gsea-dir-field");
        }
        xapps.gsea.fx.params.FxPathFieldColors.attach(loadDirField);
        Button browseDir = xapps.gsea.fx.FxEllipsisButton.create(
                "[ OR ] Locate a GSEA report folder from the file system");
        browseDir.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            // Swing GDirFieldPlusChooser → chooseDirByDialog (platform default title).
            FxFileChooserUtil.seedInitialDirectory(chooser, loadDirField.getText());
            File selected = chooser.showDialog(FxFileChooserUtil.windowOf(root));
            if (selected != null) {
                // Swing: browse only sets DirParam — do not clear cache (XOR checked on Load).
                loadDirField.setText(selected.getAbsolutePath());
                FxFileChooserUtil.registerOpenedDir(selected);
            }
        });

        // Swing EnrichmentMapInputPanel: Clear + Load (no Refresh chrome).
        Button clear = new Button("Clear");
        xapps.gsea.fx.FxButtons.styleSecondary(clear);
        clear.setOnAction(e -> {
            cacheChooser.clearSelection();
            loadDirField.clear();
        });
        Button load = new Button("Load GSEA Results");
        xapps.gsea.fx.FxButtons.stylePrimary(load);
        load.setOnAction(e -> loadGseaResults());

        HBox.setHgrow(cacheChooser.getNode(), Priority.ALWAYS);
        VBox box = new VBox(12,
                new Label("Select a GSEA result from the application cache"),
                cacheChooser.getNode(),
                new Label("[ OR ] Locate a GSEA result folder from the file system"),
                new HBox(8, loadDirField, browseDir),
                xapps.gsea.fx.FxButtons.row(clear, load));
        box.setPadding(new Insets(16));
        return box;
    }

    private void loadGseaResults() {
        // Swing: cache XOR dir via isSpecified() on params (field text).
        boolean hasCache = cacheChooser.isSpecified();
        String dirText = loadDirField.getText();
        boolean hasDir = dirText != null && !dirText.isBlank();
        if (hasCache && hasDir) {
            Application.getWindowManager().showMessage(
                    "Both cache and a brows'ed directory were specified. Only 1 can be specified. Delete one and try again");
            return;
        }
        if (!hasCache && !hasDir) {
            Application.getWindowManager().showMessage(
                    "No GSEA result folder was specified. Specify one and try again");
            return;
        }

        String[] datasets;
        if (hasCache) {
            List<FxReportCacheSupport.CachedReport> selected = cacheChooser.getSelected();
            if (!selected.isEmpty()) {
                datasets = selected.stream()
                        .map(c -> c.reportDir.getAbsolutePath())
                        .toArray(String[]::new);
            } else {
                // Typed comma-separated reportDir paths.
                datasets = cacheChooser.getReportDirs().stream()
                        .map(File::getAbsolutePath)
                        .toArray(String[]::new);
            }
        } else {
            datasets = dirText.trim().split(",");
        }

        try {
            AnalysisForm form = new AnalysisForm(datasets);
            analysisCount++;
            Tab tab = new Tab("EM Analysis");
            tab.setClosable(true);
            // Swing EnrichmentMapInputPanel wraps each analysis form in a JScrollPane.
            javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(form.root);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(true);
            tab.setContent(scroll);
            analysisTabs.getTabs().add(tab);
            analysisTabs.getSelectionModel().select(tab);
        } catch (Throwable t) {
            klog.error("Unable to initialize Enrichment Map interface", t);
            Application.getWindowManager().showError("Trouble loading enrichment database", t);
        }
    }

    /** One closable analysis tab (parameter form + launch). */
    private final class AnalysisForm {
        private final BorderPane root = new BorderPane();
        private final TextField resultDirField = new TextField();
        private final TextField resultDir2Field = new TextField();
        private final TextField expressionField = new TextField();
        private final TextField expression2Field = new TextField();
        private final TextField pvalueField = new TextField("0.005");
        private final TextField qvalueField = new TextField("0.1");
        private final TextField similarityField = new TextField("0.5");
        private final TextField combinedConstantField = new TextField("0.5");
        private double lastValidPvalue = 0.005;
        private double lastValidQvalue = 0.1;
        private double lastValidSimilarity = 0.5;
        private double lastValidCombined = 0.5;
        private final RadioButton overlapRadio = new RadioButton("Overlap Coefficient");
        private final RadioButton jaccardRadio = new RadioButton("Jaccard Coefficient");
        private final RadioButton combinedRadio = new RadioButton("Jaccard+Overlap Combined");
        /** Swing: format tip on GCT labels only (not on the path fields). */
        private Tooltip expressionFormatTip;
        /** When false, metric switches rewrite the similarity cutoff to Swing defaults. */
        private boolean similarityCutOffChanged = false;

        AnalysisForm(String[] rawDatasets) {
            // Matches Swing EnrichmentMapParameterPanel#createDatasetPanel: sort alphabetically
            // before populating the dataset combos.
            String[] datasets = rawDatasets == null ? null : rawDatasets.clone();
            if (datasets != null) {
                Arrays.sort(datasets);
            }
            ComboBox<String> ds1Combo = new ComboBox<>();
            ComboBox<String> ds2Combo = new ComboBox<>();
            if (datasets != null && datasets.length > 0) {
                ds1Combo.getItems().addAll(datasets);
                ds1Combo.getSelectionModel().select(0);
                resultDirField.setText(datasets[0].trim());
                String expr = findExpressionFile(datasets[0].trim());
                if (expr != null && !expr.startsWith("Unable")) {
                    expressionField.setText(expr);
                }
                if (datasets.length > 1) {
                    ds2Combo.getItems().addAll(datasets);
                    ds2Combo.getSelectionModel().select(1);
                    resultDir2Field.setText(datasets[1].trim());
                    String expr2 = findExpressionFile(datasets[1].trim());
                    if (expr2 != null && !expr2.startsWith("Unable")) {
                        expression2Field.setText(expr2);
                    }
                }
                ds1Combo.setOnAction(e -> {
                    String v = ds1Combo.getSelectionModel().getSelectedItem();
                    if (v != null) {
                        resultDirField.setText(v.trim());
                        String ex = findExpressionFile(v.trim());
                        if (ex != null && !ex.startsWith("Unable")) {
                            expressionField.setText(ex);
                        }
                    }
                });
                ds2Combo.setOnAction(e -> {
                    String v = ds2Combo.getSelectionModel().getSelectedItem();
                    if (v != null) {
                        resultDir2Field.setText(v.trim());
                        String ex = findExpressionFile(v.trim());
                        if (ex != null && !ex.startsWith("Unable")) {
                            expression2Field.setText(ex);
                        }
                    }
                });
            }

            HBox.setHgrow(expressionField, Priority.ALWAYS);
            HBox.setHgrow(expression2Field, Priority.ALWAYS);

            // Swing selectGCTFileButton: "..."
            Button browseExpr = xapps.gsea.fx.FxEllipsisButton.create();
            browseExpr.setOnAction(e -> chooseExpressionFile(expressionField, resultDirField));
            Button browseExpr2 = xapps.gsea.fx.FxEllipsisButton.create();
            browseExpr2.setOnAction(e -> chooseExpressionFile(expression2Field, resultDir2Field));

            ToggleGroup metricGroup = new ToggleGroup();
            jaccardRadio.setToggleGroup(metricGroup);
            overlapRadio.setToggleGroup(metricGroup);
            combinedRadio.setToggleGroup(metricGroup);
            // Swing EnrichmentMapParameters ctor: SM_OVERLAP @ 0.5
            overlapRadio.setSelected(true);
            overlapRadio.setTooltip(new Tooltip(
                    "Overlap Coefficient = [size of (A intersect B)] / [size of (minimum( A , B))]"));
            jaccardRadio.setTooltip(new Tooltip(
                    "Jaccard Coefficient = [size of (A intersect B)] / [size of (A union B)]"));
            combinedRadio.setTooltip(new Tooltip(
                    "Combined Constant = k; Combined Coefficient = (k * Overlap) + ((1-k) * Jaccard)"));
            // Swing EnrichmentMapParameterPanel tip strings (verbatim).
            String pTip = "Sets the p-value cutoff \nonly genesets with a p-value less than \nthe cutoff will be included.";
            String qTip = "Sets the FDR q-value cutoff \n"
                    + "only genesets with a FDR q-value less than \n"
                    + "the cutoff will be included.";
            String simTip = "Sets the Jaccard or Overlap coefficient cutoff \n"
                    + "only edges with a Jaccard or Overlap coefficient less than \n"
                    + "the cutoff will be added.";
            String exprTip = "File with gene expression values.\n"
                    + "Format: gene <tab> description <tab> expression value <tab> ...";
            pvalueField.setTooltip(new Tooltip(pTip));
            qvalueField.setTooltip(new Tooltip(qTip));
            similarityField.setTooltip(new Tooltip(simTip));
            // Format tip lives on labels only; field tip becomes absolute path after browse.
            expressionField.setTooltip(null);
            expression2Field.setTooltip(null);
            expressionFormatTip = new Tooltip(exprTip);
            overlapRadio.setOnAction(e -> onMetricChanged(EnrichmentMapParameters.SM_OVERLAP));
            jaccardRadio.setOnAction(e -> onMetricChanged(EnrichmentMapParameters.SM_JACCARD));
            combinedRadio.setOnAction(e -> onMetricChanged(EnrichmentMapParameters.SM_COMBINED));
            onMetricChanged(EnrichmentMapParameters.SM_OVERLAP);
            combinedConstantField.setDisable(true);
            similarityField.textProperty().addListener((obs, o, n) -> {
                // Ignore programmatic metric-default writes; only user edits set the flag.
                if (similarityField.isFocused()) {
                    similarityCutOffChanged = true;
                }
            });
            combinedConstantField.textProperty().addListener((obs, o, n) -> {
                if (!combinedConstantField.isFocused() || similarityCutOffChanged) {
                    return;
                }
                if (!combinedRadio.isSelected()) {
                    return;
                }
                try {
                    double k = Double.parseDouble(combinedConstantField.getText().trim());
                    if (k >= 0.0 && k <= 1.0) {
                        double cutoff = (0.5 * k) + ((1.0 - k) * 0.25);
                        similarityField.setText(formatCutoff(cutoff));
                    }
                } catch (NumberFormatException ignored) {
                    // leave as-is until launch validation
                }
            });

            GridPane form = new GridPane();
            form.setHgap(8);
            form.setVgap(8);
            int row = 0;
            // Swing createDatasetPanel: Dataset 1 always; Dataset 2 only when length > 1.
            // Result folders are chosen via combos (no free-text browse on the analysis form).
            boolean dual = datasets != null && datasets.length > 1;
            Label ds1Sep = new Label("Dataset 1");
            ds1Sep.setStyle("-fx-font-weight: bold;");
            form.add(ds1Sep, 0, row++, 2, 1);
            form.add(new Label("Dataset 1:"), 0, row);
            form.add(ds1Combo, 1, row++);
            Label exprLabel = new Label("*Expression (Dataset 1):");
            exprLabel.setTooltip(expressionFormatTip);
            form.add(exprLabel, 0, row);
            form.add(new HBox(6, expressionField, browseExpr), 1, row++);
            if (dual) {
                Label ds2Sep = new Label("Dataset 2");
                ds2Sep.setStyle("-fx-font-weight: bold;");
                form.add(ds2Sep, 0, row++, 2, 1);
                form.add(new Label("Dataset 2:"), 0, row);
                form.add(ds2Combo, 1, row++);
                Label expr2Label = new Label("*Expression (Dataset 2):");
                expr2Label.setTooltip(expressionFormatTip);
                form.add(expr2Label, 0, row);
                form.add(new HBox(6, expression2Field, browseExpr2), 1, row++);
            }
            Label pLabel = new Label("P-value Cutoff");
            pLabel.setTooltip(pvalueField.getTooltip());
            form.add(pLabel, 0, row);
            form.add(pvalueField, 1, row++);
            Label qLabel = new Label("FDR Q-value Cutoff");
            qLabel.setTooltip(qvalueField.getTooltip());
            form.add(qLabel, 0, row);
            form.add(qvalueField, 1, row++);
            wireCutoffValidation();

            // Swing CollapsiblePanel("Advanced Options") — collapsed by default.
            // Radio order: Jaccard → Overlap → Combined (EnrichmentMapParameterPanel).
            VBox advancedContent = new VBox(8,
                    new Label("Similarity Cutoff:"),
                    similarityField,
                    new VBox(4, jaccardRadio, overlapRadio, combinedRadio, similarityLogo()),
                    new Label("Combined Constant"),
                    combinedConstantField);
            TitledPane advanced = new TitledPane("Advanced Options", advancedContent);
            advanced.setExpanded(false);
            advanced.setAnimated(true);
            form.add(advanced, 0, row++, 2, 1);

            Label prerankedNote = new Label(
                    "*If you are using GSEAPreranked and you would like to see the expression values "
                            + "in the heat map instead of the ranks change this default setting.");
            prerankedNote.setWrapText(true);
            prerankedNote.setStyle("-fx-font-style: italic;");

            // Swing createBottomPanel: Build Enrichment Map only (no Clear / Refresh port / Status).
            Button launch = new Button("Build Enrichment Map");
            xapps.gsea.fx.FxButtons.stylePrimary(launch);
            launch.setOnAction(e -> launchEnrichmentMap());

            VBox box = new VBox(12, form, prerankedNote,
                    xapps.gsea.fx.FxButtons.row(launch));
            box.setPadding(new Insets(16));
            // Swing EnrichmentMapParameterPanel: TitledBorder("Enrichment Map Parameters").
            TitledPane titled = new TitledPane("Enrichment Map Parameters", box);
            titled.setCollapsible(false);
            titled.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            root.setCenter(titled);
        }

        private static javafx.scene.image.ImageView similarityLogo() {
            javafx.scene.image.ImageView view = new javafx.scene.image.ImageView();
            try {
                var url = FxEnrichmentMapPane.class.getResource(
                        "/edu/mit/broad/genome/resources/GSEA_similarityLogo.png");
                if (url != null) {
                    javafx.scene.image.Image img = new javafx.scene.image.Image(url.toExternalForm());
                    view.setImage(img);
                    view.setPreserveRatio(true);
                    view.setFitWidth(320);
                }
            } catch (Throwable ignored) {
                // logo is decorative
            }
            return view;
        }

        private void onMetricChanged(String metric) {
            boolean combined = EnrichmentMapParameters.SM_COMBINED.equals(metric);
            combinedConstantField.setDisable(!combined);
            if (similarityCutOffChanged) {
                return;
            }
            if (EnrichmentMapParameters.SM_JACCARD.equals(metric)) {
                similarityField.setText("0.25");
            } else if (EnrichmentMapParameters.SM_OVERLAP.equals(metric)) {
                similarityField.setText("0.5");
            } else if (combined) {
                try {
                    double k = Double.parseDouble(combinedConstantField.getText().trim());
                    double cutoff = (0.5 * k) + ((1.0 - k) * 0.25);
                    similarityField.setText(formatCutoff(cutoff));
                } catch (NumberFormatException nfe) {
                    similarityField.setText("0.375");
                }
            }
        }

        private static String formatCutoff(double cutoff) {
            String s = Double.toString(cutoff);
            if (s.endsWith(".0")) {
                return s.substring(0, s.length() - 2);
            }
            return s;
        }

        private void chooseExpressionFile(TextField target, TextField resultDirField) {
            FileChooser chooser = new FileChooser();
            // Swing FileDialog uses platform default title (no custom string).
            // Swing EnrichmentMapParameterPanel: FileDialog.setDirectory(params.getEdbdir())
            // which is resultDir/edb after setEdbdir.
            String seed = null;
            String rd = resultDirField != null ? resultDirField.getText() : null;
            if (rd != null && !rd.isBlank()) {
                seed = new File(rd.trim(), "edb").getAbsolutePath();
            }
            if (seed == null || seed.isBlank()) {
                seed = target.getText();
            }
            FxFileChooserUtil.seedInitialDirectory(chooser, seed);
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Expression / rank", "*.gct", "*.res", "*.rnk", "*.txt"),
                    new FileChooser.ExtensionFilter("All files", "*.*"));
            File selected = chooser.showOpenDialog(FxFileChooserUtil.windowOf(root));
            if (selected != null) {
                target.setText(selected.getAbsolutePath());
                // Swing: GCTFileName*TextField.setToolTipText(file.getAbsolutePath())
                target.setTooltip(new Tooltip(selected.getAbsolutePath()));
                FxFileChooserUtil.registerOpened(selected);
            }
        }

        private void wireCutoffValidation() {
            pvalueField.focusedProperty().addListener((o, was, is) -> {
                if (!is) {
                    validatePvalueField();
                }
            });
            qvalueField.focusedProperty().addListener((o, was, is) -> {
                if (!is) {
                    validateQvalueField();
                }
            });
            similarityField.focusedProperty().addListener((o, was, is) -> {
                if (!is) {
                    validateSimilarityField();
                }
            });
            combinedConstantField.focusedProperty().addListener((o, was, is) -> {
                if (!is) {
                    validateCombinedField();
                }
            });
        }

        private boolean validatePvalueField() {
            try {
                double v = Double.parseDouble(pvalueField.getText().trim());
                if (v > 0.0 && v <= 1.0) {
                    lastValidPvalue = v;
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
            pvalueField.setText(formatCutoff(lastValidPvalue));
            return false;
        }

        private boolean validateQvalueField() {
            try {
                double v = Double.parseDouble(qvalueField.getText().trim());
                if (v >= 0.0 && v <= 100.0) {
                    lastValidQvalue = v;
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
            qvalueField.setText(formatCutoff(lastValidQvalue));
            return false;
        }

        private boolean validateSimilarityField() {
            try {
                double v = Double.parseDouble(similarityField.getText().trim());
                if (v >= 0.0 && v <= 1.0) {
                    lastValidSimilarity = v;
                    similarityCutOffChanged = true;
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
            similarityField.setText(formatCutoff(lastValidSimilarity));
            return false;
        }

        private boolean validateCombinedField() {
            try {
                double v = Double.parseDouble(combinedConstantField.getText().trim());
                if (v >= 0.0 && v <= 1.0) {
                    lastValidCombined = v;
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
            combinedConstantField.setText(formatCutoff(lastValidCombined));
            return false;
        }

        private void launchEnrichmentMap() {
            String resultDir = resultDirField.getText();
            if (resultDir == null || resultDir.isBlank()) {
                Application.getWindowManager().showMessage("Specify a GSEA result folder first.");
                return;
            }

            if (!validatePvalueField() || !validateQvalueField()
                    || !validateSimilarityField() || !validateCombinedField()) {
                // Swing FormattedTextFieldAction: silently reset invalid values (dialog commented out).
                // Continue launch with last-valid cutoffs — do not abort.
            }

            double pvalue = lastValidPvalue;
            double qvalue = lastValidQvalue;
            double similarity = lastValidSimilarity;
            double combinedConstant = lastValidCombined;

            String expr = nullToEmpty(expressionField.getText());
            EnrichmentMapParameters params = new EnrichmentMapParameters(resultDir.trim(), expr);
            params.setPvalue(pvalue);
            params.setQvalue(qvalue);
            params.setSimilarityCutOff(similarity);
            params.setCombinedConstant(combinedConstant);
            params.setSimilarityMetric(selectedMetric());
            params.setEdbdir(resultDir.trim());

            String result2 = resultDir2Field.getText();
            if (result2 != null && !result2.isBlank()) {
                params.setEdbdir2(result2.trim());
                params.setExpression2FilePath(nullToEmpty(expression2Field.getText()));
            }

            Thread worker = new Thread(() -> {
                CytoscapeCyrest cyto = new CytoscapeCyrest(params);
                try {
                    // Swing BuildActionPerformed: confirm if inactive, then re-check before create.
                    if (!cyto.CytoscapeRestActive()) {
                        klog.info(LAUNCH_MSG);
                        if (!Application.getWindowManager().showConfirm(LAUNCH_MSG)) {
                            return;
                        }
                    }
                    if (cyto.CytoscapeRestActive() && cyto.CytoscapeRestCommandEM()) {
                        if (cyto.createEM_get()) {
                            Platform.runLater(() -> Application.getWindowManager().showMessage(
                                    "An Enrichment map was successfully loaded and created in cytoscape.  "
                                            + "Please navigate to cytoscape to view results"));
                        }
                        // createEM_get false → Swing silent no-op
                    }
                    // REST up but !EM commands → Swing silent no-op
                } catch (java.io.IOException e) {
                    klog.error("Unable to communicate with cytoscape: {}", e.getMessage());
                    klog.error(LAUNCH_MSG);
                    Application.getWindowManager().showMessage(LAUNCH_MSG);
                } catch (java.net.URISyntaxException e) {
                    klog.error("Issue with cytoscape rest command: {}", e.getMessage());
                }
            }, "gsea-enrichment-map");
            worker.setDaemon(true);
            worker.start();
        }

        private String selectedMetric() {
            if (jaccardRadio.isSelected()) {
                return EnrichmentMapParameters.SM_JACCARD;
            }
            if (combinedRadio.isSelected()) {
                return EnrichmentMapParameters.SM_COMBINED;
            }
            return EnrichmentMapParameters.SM_OVERLAP;
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Port of Swing {@code EnrichmentMapParameterPanel#getExpressionFile}: resolve the expression
     * or rank file from the GSEA result folder's {@code .rpt}, collapsing when needed, else fall
     * back to the {@code edb/*.rnk} file.
     */
    static String findExpressionFile(String resultDirPath) {
        try {
            File edbdir = new File(resultDirPath);
            if (!edbdir.getName().equalsIgnoreCase("edb")) {
                File nested = new File(edbdir, "edb");
                if (nested.isDirectory()) {
                    edbdir = nested;
                }
            }
            if (!edbdir.exists() || !edbdir.isDirectory()) {
                return "Unable to find edb directory";
            }
            File parentDir = edbdir.getParentFile();
            if (parentDir == null || !parentDir.isDirectory()) {
                return "Unable to find rpt file";
            }
            File[] rptFiles = parentDir.listFiles((FilenameFilter) (dir, name) ->
                    name.toLowerCase().endsWith(".rpt"));
            if (rptFiles == null || rptFiles.length != 1) {
                return "Unable to find rpt file";
            }
            File rpt = rptFiles[0];
            Map<String, String> params = new HashMap<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(rpt)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.split("\t");
                    if (tokens.length == 2) {
                        params.put(tokens[0], tokens[1]);
                    } else if (tokens.length >= 3) {
                        params.put(tokens[0] + " " + tokens[1], tokens[2]);
                    }
                }
            }

            String collapse = params.getOrDefault("param collapse", "");
            if (params.containsKey("param rnk")) {
                return params.get("param rnk");
            }
            if (!params.containsKey("param res")) {
                return "Unable to get expresion File";
            }

            String res = params.get("param res");
            if (res != null && !new File(res).exists()) {
                Application.getWindowManager().showMessage("Unable to find expression file: " + res);
            }
            if ("false".equalsIgnoreCase(collapse) || "No_Collapse".equalsIgnoreCase(collapse)) {
                return res;
            }

            // Collapsed analysis: try CollapseDataset, else edb/*.rnk
            Properties props = new Properties();
            props.put("res", res);
            String rptLabel = params.getOrDefault("param rpt_label", "");
            props.put("rpt_label", rptLabel);
            String mode = params.getOrDefault("param mode", "");
            props.put("mode", mode);
            String chip = params.getOrDefault("param chip", "");
            props.put("chip", chip);
            String include = params.getOrDefault("param include_only_symbols", "");
            props.put("include_only_symbols", include);
            String out = params.getOrDefault("param out", "");
            props.put("out", out);
            props.put("gui", "false");

            if (!rptLabel.isEmpty() && !mode.isEmpty() && !chip.isEmpty()
                    && !include.isEmpty() && !out.isEmpty()) {
                try {
                    CollapseDataset tool = new CollapseDataset(props, "");
                    tool.execute();
                    File reportDir = tool.getReport().getReportDir();
                    String tempFile = new File(res).getName();
                    String extendedName = "Remap_only".equals(mode)
                            ? "_remapped_to_symbols.gct"
                            : "_collapsed_to_symbols.gct";
                    String simplename = tempFile.replace(".gct", extendedName);
                    return new File(reportDir, simplename).getAbsolutePath();
                } catch (Throwable t) {
                    klog.warn("CollapseDataset during EM expression detect failed: {}", t.getMessage());
                }
            }

            File[] edbRnk = edbdir.listFiles((FilenameFilter) (dir, name) ->
                    name.toLowerCase().endsWith(".rnk"));
            // Swing EnrichmentMapParameterPanel bug-compatible: checks rpt `files.length`
            // (not edbRnk) and returns the .rpt path when length==1.
            if (edbRnk != null && rptFiles.length == 1) {
                return rptFiles[0].getAbsolutePath();
            }
            return "Unable to get expresion File";
        } catch (Exception e) {
            return "Unable to auto-detect expression file: " + e.getMessage();
        }
    }

    @Override
    public String getTitle() {
        return "Enrichment Map Visualization";
    }

    @Override
    public String getIconResourceId() {
        return "enrichmentmap_logo.png";
    }

    @Override
    public Object getContent() {
        return root;
    }
}
