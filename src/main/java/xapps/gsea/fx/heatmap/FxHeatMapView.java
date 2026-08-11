/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.heatmap;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import org.genepattern.heatmap.ColorScheme;
import org.genepattern.heatmap.image.DisplaySettings;
import org.genepattern.heatmap.image.HeatMap;
import org.genepattern.io.ImageUtil;

import edu.mit.broad.genome.alg.AlgUtils;
import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.DefaultDataset;
import edu.mit.broad.genome.objects.GPWrappers;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.heatmap.DisplayState;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import xapps.gsea.fx.widgets.FxImages;
import xapps.gsea.fx.params.FxFileChooserUtil;

/**
 * Interactive FX heatmap: live GramImager/{@link HeatMap} re-render with cell size,
 * grid, row/column selection, Save Dataset (slice), Profile, and optional gene-set HTML open.
 */
public class FxHeatMapView {

    private final BorderPane root = new BorderPane();
    private final ImageView imageView = new ImageView();
    private final ListView<String> rowList = new ListView<>();
    private final ListView<String> colList = new ListView<>();
    private final Label tipLabel = new Label();

    private Dataset dataset;
    private ColorScheme colorScheme;
    private HeatMap heatMap;
    private GeneSet[] geneSetsForTooltips;
    private File htmlLookupDir;
    private boolean similarityMode;
    private Consumer<int[]> profileHandler;

    private int cellSize = DisplayState.DEFAULT_CELL_HEIGHT;
    private boolean drawGrid = true;
    private boolean drawRowNames = true;
    private boolean drawColumnNames = true;
    private boolean drawRowDescriptions = false;
    private boolean globalColorScale = false;
    private boolean binaryMembershipMode = false;
    private String featureUIString = "Feature";
    private String sampleUIString = "Sample";
    private boolean showColorSchemeOptions = true;
    private boolean allowChangeColumnNameVisibility = true;
    private boolean allowChangeRowNameVisibility = true;
    private boolean allowChangeRowDescriptionsVisibility = true;
    private boolean showProfileAndLegend = true;
    private Slider sizeSlider;
    private CheckBox toolbarRowNames;
    private CheckBox toolbarColNames;
    private Button legendButton;
    private Button profileButton;
    private Label rowsLabel;
    private Label colsLabel;
    private java.util.function.IntConsumer cellSizeListener;

    public FxHeatMapView() {
        imageView.setPreserveRatio(true);
        rowList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        colList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        rowList.setPrefWidth(180);
        colList.setPrefHeight(90);

        sizeSlider = new Slider(2, 30, cellSize);
        sizeSlider.setShowTickLabels(true);
        sizeSlider.setMajorTickUnit(7);
        sizeSlider.valueProperty().addListener((o, a, b) -> {
            int next = Math.max(2, b.intValue());
            if (next == cellSize) {
                return;
            }
            cellSize = next;
            rebuild();
            fireCellSizeChanged();
        });
        CheckBox grid = new CheckBox("Show Grid");
        grid.setSelected(drawGrid);
        grid.setOnAction(e -> {
            drawGrid = grid.isSelected();
            rebuild();
        });
        toolbarRowNames = new CheckBox(showFeatureNamesLabel());
        toolbarRowNames.setSelected(drawRowNames);
        toolbarRowNames.setOnAction(e -> {
            drawRowNames = toolbarRowNames.isSelected();
            rebuild();
        });
        toolbarColNames = new CheckBox(showSampleNamesLabel());
        toolbarColNames.setSelected(drawColumnNames);
        toolbarColNames.setOnAction(e -> {
            drawColumnNames = toolbarColNames.isSelected();
            rebuild();
        });

        Button displayOpts = new Button("Display Options...");
        displayOpts.setOnAction(e -> showDisplayOptions());
        legendButton = new Button("Legend");
        legendButton.setOnAction(e -> showLegend());
        Button saveImg = new Button("Save image...");
        saveImg.setOnAction(e -> saveImage());
        Button saveDs = new Button("Save Dataset...");
        saveDs.setOnAction(e -> saveDatasetSlice());
        profileButton = new Button("Profile");
        profileButton.setOnAction(e -> runProfile());
        xapps.gsea.fx.widgets.FxButtons.styleToolbar(displayOpts);
        xapps.gsea.fx.widgets.FxButtons.styleToolbar(legendButton);
        xapps.gsea.fx.widgets.FxButtons.styleToolbar(saveImg);
        xapps.gsea.fx.widgets.FxButtons.styleToolbar(saveDs);
        xapps.gsea.fx.widgets.FxButtons.styleToolbar(profileButton);

        HBox tools = xapps.gsea.fx.widgets.FxButtons.row(
                new Label("Grid Size:"), sizeSlider, grid, toolbarRowNames, toolbarColNames, displayOpts, legendButton,
                saveImg, saveDs, profileButton);
        xapps.gsea.fx.widgets.FxButtons.padTight(tools);
        tools.setMinWidth(0);

        ScrollPane imageScroll = new ScrollPane(imageView);
        imageScroll.setFitToWidth(false);
        imageScroll.setFitToHeight(false);
        imageScroll.setMinSize(0, 0);
        VBox.setVgrow(imageScroll, Priority.ALWAYS);

        rowsLabel = new Label("Rows (" + featureUIString.toLowerCase(Locale.ROOT) + "s)");
        colsLabel = new Label("Columns (" + sampleUIString.toLowerCase(Locale.ROOT) + "s)");
        VBox lists = new VBox(4, rowsLabel, rowList, colsLabel, colList, tipLabel);
        VBox.setVgrow(rowList, Priority.ALWAYS);
        lists.setPadding(new Insets(4));
        lists.setPrefWidth(220);
        lists.setMinWidth(0);

        SplitPane split = new SplitPane(imageScroll, lists);
        split.setDividerPositions(0.72);
        split.setMinSize(0, 0);

        root.setMinSize(0, 0);
        root.setTop(tools);
        root.setCenter(split);

        installImageInteractions();
        installRowContextMenu();
    }

    public BorderPane getNode() {
        return root;
    }

    public void setData(Dataset dataset, ColorScheme colorScheme, boolean similarityMode) {
        setData(dataset, colorScheme, similarityMode, false);
    }

    public void setData(Dataset dataset, ColorScheme colorScheme, boolean similarityMode,
            boolean binaryMembership) {
        this.dataset = dataset;
        this.colorScheme = colorScheme;
        this.similarityMode = similarityMode;
        this.binaryMembershipMode = binaryMembership && !similarityMode;
        this.cellSize = similarityMode ? 12 : 8;
        if (sizeSlider != null && (int) sizeSlider.getValue() != cellSize) {
            sizeSlider.setValue(cellSize);
        }
        if (dataset != null) {
            rowList.setItems(FXCollections.observableArrayList(dataset.getRowNames()));
            colList.setItems(FXCollections.observableArrayList(dataset.getColumnNames()));
        } else {
            rowList.getItems().clear();
            colList.getItems().clear();
        }
        rebuild();
    }

    public void setUiNaming(String featureUi, String sampleUi, boolean colorSchemeOptions) {
        if (featureUi != null && !featureUi.isBlank()) {
            this.featureUIString = featureUi;
        }
        if (sampleUi != null && !sampleUi.isBlank()) {
            this.sampleUIString = sampleUi;
        }
        this.showColorSchemeOptions = colorSchemeOptions;
        if (toolbarRowNames != null) {
            toolbarRowNames.setText(showFeatureNamesLabel());
        }
        if (toolbarColNames != null) {
            toolbarColNames.setText(showSampleNamesLabel());
        }
        if (rowsLabel != null) {
            rowsLabel.setText("Rows (" + featureUIString.toLowerCase(Locale.ROOT) + "s)");
        }
        if (colsLabel != null) {
            colsLabel.setText("Columns (" + sampleUIString.toLowerCase(Locale.ROOT) + "s)");
        }
    }

    public int getCellSize() {
        return cellSize;
    }

    public void setOnCellSizeChanged(java.util.function.IntConsumer listener) {
        this.cellSizeListener = listener;
    }

    private void fireCellSizeChanged() {
        if (cellSizeListener != null) {
            cellSizeListener.accept(cellSize);
        }
    }

    private String showFeatureNamesLabel() {
        return "Show " + featureUIString + " Names";
    }

    private String showSampleNamesLabel() {
        return "Show " + sampleUIString + " Names";
    }

    private String showFeatureDescriptionsLabel() {
        return "Show " + featureUIString + " Descriptions";
    }

    public void setOptionsDialogOptions(boolean allowChangeColumnNameVisibility,
            boolean allowChangeRowNameVisibility,
            boolean allowChangeRowDescriptionsVisibility) {
        setOptionsDialogOptions(allowChangeColumnNameVisibility, allowChangeRowNameVisibility,
                allowChangeRowDescriptionsVisibility, true);
    }

    /**
     * @param showProfileAndLegend — when false,
     * Profile/Legend chrome is hidden (LE / similarity panels).
     */
    public void setOptionsDialogOptions(boolean allowChangeColumnNameVisibility,
            boolean allowChangeRowNameVisibility,
            boolean allowChangeRowDescriptionsVisibility,
            boolean showProfileAndLegend) {
        this.allowChangeColumnNameVisibility = allowChangeColumnNameVisibility;
        this.allowChangeRowNameVisibility = allowChangeRowNameVisibility;
        this.allowChangeRowDescriptionsVisibility = allowChangeRowDescriptionsVisibility;
        this.showProfileAndLegend = showProfileAndLegend;
        if (toolbarRowNames != null) {
            toolbarRowNames.setVisible(allowChangeRowNameVisibility);
            toolbarRowNames.setManaged(allowChangeRowNameVisibility);
        }
        if (toolbarColNames != null) {
            toolbarColNames.setVisible(allowChangeColumnNameVisibility);
            toolbarColNames.setManaged(allowChangeColumnNameVisibility);
        }
        applyProfileLegendVisibility();
    }

    private void applyProfileLegendVisibility() {
        if (legendButton != null) {
            legendButton.setVisible(showProfileAndLegend);
            legendButton.setManaged(showProfileAndLegend);
        }
        if (profileButton != null) {
            profileButton.setVisible(showProfileAndLegend);
            profileButton.setManaged(showProfileAndLegend);
        }
    }

    private void showDisplayOptions() {
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Options");
        dialog.setHeaderText(null);
        CheckBox optGrid = new CheckBox("Show Grid");
        optGrid.setSelected(drawGrid);
        CheckBox optRows = new CheckBox(showFeatureNamesLabel());
        optRows.setSelected(drawRowNames);
        CheckBox optCols = new CheckBox(showSampleNamesLabel());
        optCols.setSelected(drawColumnNames);
        CheckBox optDesc = new CheckBox(showFeatureDescriptionsLabel());
        optDesc.setSelected(drawRowDescriptions);
        javafx.scene.control.RadioButton relative = new javafx.scene.control.RadioButton("Relative");
        javafx.scene.control.RadioButton global = new javafx.scene.control.RadioButton("Global");
        javafx.scene.control.ToggleGroup schemeGroup = new javafx.scene.control.ToggleGroup();
        relative.setToggleGroup(schemeGroup);
        global.setToggleGroup(schemeGroup);
        if (globalColorScale) {
            global.setSelected(true);
        } else {
            relative.setSelected(true);
        }
        boolean schemeDisabled = !showColorSchemeOptions || similarityMode || binaryMembershipMode;
        relative.setDisable(schemeDisabled);
        global.setDisable(schemeDisabled);
        Slider optSize = new Slider(2, 30, cellSize);
        optSize.setShowTickLabels(true);
        optSize.setMajorTickUnit(7);
        javafx.scene.control.TextField sizeField = new javafx.scene.control.TextField(String.valueOf(cellSize));
        sizeField.setPrefWidth(48);

        Runnable applySize = () -> {
            int v = Math.max(2, Math.min(30, (int) optSize.getValue()));
            try {
                v = Math.max(2, Math.min(30, Integer.parseInt(sizeField.getText().trim())));
                optSize.setValue(v);
            } catch (NumberFormatException ignored) {
                sizeField.setText(String.valueOf(v));
            }
            if (v != cellSize) {
                cellSize = v;
                if (sizeSlider != null) {
                    sizeSlider.setValue(cellSize);
                }
                rebuild();
                fireCellSizeChanged();
            }
        };
        optSize.valueProperty().addListener((o, a, b) -> {
            int v = Math.max(2, Math.min(30, b.intValue()));
            if (!sizeField.getText().equals(String.valueOf(v))) {
                sizeField.setText(String.valueOf(v));
            }
            if (v != cellSize) {
                cellSize = v;
                if (sizeSlider != null) {
                    sizeSlider.setValue(cellSize);
                }
                rebuild();
                fireCellSizeChanged();
            }
        });
        sizeField.setOnAction(e -> applySize.run());
        sizeField.focusedProperty().addListener((o, was, is) -> {
            if (!is) {
                applySize.run();
            }
        });
        optGrid.setOnAction(e -> {
            drawGrid = optGrid.isSelected();
            rebuild();
        });
        optRows.setOnAction(e -> {
            drawRowNames = optRows.isSelected();
            if (toolbarRowNames != null) {
                toolbarRowNames.setSelected(drawRowNames);
            }
            rebuild();
        });
        optCols.setOnAction(e -> {
            drawColumnNames = optCols.isSelected();
            if (toolbarColNames != null) {
                toolbarColNames.setSelected(drawColumnNames);
            }
            rebuild();
        });
        optDesc.setOnAction(e -> {
            drawRowDescriptions = optDesc.isSelected();
            rebuild();
        });
        schemeGroup.selectedToggleProperty().addListener((o, a, b) -> {
            if (schemeDisabled || b == null) {
                return;
            }
            boolean nextGlobal = global.isSelected();
            if (nextGlobal != globalColorScale) {
                globalColorScale = nextGlobal;
                applyColorScaleMode(globalColorScale);
                rebuild();
            }
        });

        Label schemeLabel = new Label("Color Scheme: ");
        HBox schemeRow = new HBox(8, schemeLabel, relative, global);
        VBox content = new VBox(10);
        if (showColorSchemeOptions) {
            content.getChildren().add(schemeRow);
        }
        content.getChildren().add(optGrid);
        if (allowChangeRowNameVisibility) {
            content.getChildren().add(optRows);
        }
        if (allowChangeColumnNameVisibility) {
            content.getChildren().add(optCols);
        }
        if (allowChangeRowDescriptionsVisibility) {
            content.getChildren().add(optDesc);
        }
        content.getChildren().add(new HBox(8, new Label("Grid Size:"), optSize, sizeField));
        content.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void applyColorScaleMode(boolean global) {
        if (dataset == null || similarityMode || binaryMembershipMode) {
            return;
        }
        if (colorScheme instanceof org.genepattern.heatmap.RowColorScheme rowScheme) {
            rowScheme.setGlobalScale(global);
            return;
        }
        // LE score maps from GPWrappers / ColorDatasetImpl: rebuild Relative vs Absolute.
        final edu.mit.broad.genome.math.ScaleMode mode = global
                ? edu.mit.broad.genome.math.ScaleMode.ABSOLUTE
                : edu.mit.broad.genome.math.ScaleMode.REL_MEAN_ZERO_OMITTED;
        final Dataset ds = dataset;
        colorScheme = new ColorScheme() {
            private final edu.mit.broad.genome.objects.ColorDataset cds =
                    new edu.mit.broad.genome.objects.ColorDatasetImpl(
                            ds, mode, new edu.mit.broad.genome.math.ColorSchemes.BroadCancer());

            @Override
            public java.awt.Color getColor(int row, int column) {
                return cds.getColor(row, column);
            }

            @Override
            public void setDataset(org.genepattern.data.expr.IExpressionData d) {
            }
        };
    }

    private void showLegend() {
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Legend");
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        if (similarityMode) {
            dialog.getDialogPane().setContent(FxJaccardLegend.create(280));
        } else if (binaryMembershipMode) {
            dialog.getDialogPane().setContent(FxHeatMapLegend.binaryMembership());
        } else if (colorScheme instanceof org.genepattern.heatmap.RowColorScheme rowScheme) {
            dialog.getDialogPane().setContent(
                    FxHeatMapLegend.rowColorScheme(rowScheme, globalColorScale));
        } else {
            // LE score maps use BroadCancer via GPWrappers / ColorDatasetImpl.
            dialog.getDialogPane().setContent(FxHeatMapLegend.broadCancer());
        }
        dialog.showAndWait();
    }

    public void setGeneSetsForTooltips(GeneSet[] geneSets) {
        this.geneSetsForTooltips = geneSets;
    }

    public void setHtmlLookupDir(File dir) {
        this.htmlLookupDir = dir;
    }

    public void setProfileHandler(Consumer<int[]> handler) {
        this.profileHandler = handler;
    }

    /** Select a gene column by name (gene-histogram click). */
    public void selectColumnByName(String geneName) {
        if (geneName == null || dataset == null) {
            return;
        }
        try {
            int idx = dataset.getColumnIndex(geneName);
            if (idx >= 0) {
                colList.getSelectionModel().clearAndSelect(idx);
                colList.scrollTo(idx);
                tipLabel.setText("Selected column: " + geneName + " (index " + (idx + 1) + ")");
            }
        } catch (Throwable ignored) {
        }
    }

    public int[] getSelectedRowIndices() {
        return rowList.getSelectionModel().getSelectedIndices().stream().mapToInt(Integer::intValue).toArray();
    }

    public int[] getSelectedColumnIndices() {
        return colList.getSelectionModel().getSelectedIndices().stream().mapToInt(Integer::intValue).toArray();
    }

    public HeatMap getHeatMap() {
        return heatMap;
    }

    public Dataset getDataset() {
        return dataset;
    }

    private void rebuild() {
        if (dataset == null) {
            imageView.setImage(null);
            heatMap = null;
            return;
        }
        DisplaySettings settings = new DisplaySettings();
        settings.rowSize = cellSize;
        settings.columnSize = cellSize;
        settings.drawGrid = drawGrid;
        settings.showFeatureGridLines = drawGrid;
        settings.showSampleGridLines = drawGrid;
        settings.drawRowNames = drawRowNames;
        settings.drawColumnNames = drawColumnNames;
        settings.drawRowDescriptions = drawRowDescriptions;
        settings.upperTriangular = similarityMode;
        if (colorScheme != null) {
            settings.colorConverter = colorScheme;
        }
        heatMap = HeatMap.createHeatMap(
                GPWrappers.createIExpressionData(dataset),
                settings,
                GPWrappers.createFeatureAnnotator(dataset),
                GPWrappers.createSampleAnnotator(dataset, null));
        imageView.setImage(FxImages.toFxImage(heatMap.snapshot()));
    }

    private void installImageInteractions() {
        Tooltip tip = new Tooltip();
        tip.setShowDelay(javafx.util.Duration.millis(100));
        Tooltip.install(imageView, tip);
        // install() parks the popup at first-show coords; keep it on the cursor while moving.
        imageView.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            int[] cell = hitCell(e);
            if (cell == null || dataset == null) {
                tip.setText(similarityMode ? "Gene Set Similarity" : "Leading Edge Matrix");
            } else {
                tip.setText(tooltipFor(cell[0], cell[1]));
            }
            if (tip.isShowing()) {
                tip.setX(e.getScreenX() + 14);
                tip.setY(e.getScreenY() + 18);
            }
        });
        imageView.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() != MouseButton.PRIMARY) {
                return;
            }
            int[] cell = hitCell(e);
            if (cell == null) {
                return;
            }
            rowList.getSelectionModel().clearAndSelect(cell[0]);
            rowList.scrollTo(cell[0]);
            colList.getSelectionModel().clearAndSelect(cell[1]);
            colList.scrollTo(cell[1]);
            if (e.getClickCount() == 2 && !similarityMode) {
                openGeneSetHtml(dataset.getRowName(cell[0]));
            }
        });
    }

    private void installRowContextMenu() {
        MenuItem profile = new MenuItem("Profile");
        profile.setOnAction(e -> runProfile());
        rowList.setContextMenu(new ContextMenu(profile));
        rowList.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY || similarityMode || e.getClickCount() != 1) {
                return;
            }
            String name = rowList.getSelectionModel().getSelectedItem();
            if (name != null) {
                openGeneSetHtml(name);
            }
        });
    }

    private int[] hitCell(MouseEvent e) {
        if (heatMap == null || imageView.getImage() == null) {
            return null;
        }
        double scaleX = imageView.getImage().getWidth() / Math.max(1.0, imageView.getBoundsInLocal().getWidth());
        double scaleY = imageView.getImage().getHeight() / Math.max(1.0, imageView.getBoundsInLocal().getHeight());
        int x = (int) (e.getX() * scaleX);
        int y = (int) (e.getY() * scaleY);
        return heatMap.cellAtSnapshotPoint(x, y);
    }

    private String tooltipFor(int row, int col) {
        try {
            float value = dataset.getElement(row, col);
            String rowName = dataset.getRowName(row);
            String colName = dataset.getColumnName(col);
            if (similarityMode && geneSetsForTooltips != null
                    && row < geneSetsForTooltips.length && col < geneSetsForTooltips.length) {
                GeneSet a = geneSetsForTooltips[row];
                GeneSet b = geneSetsForTooltips[col];
                int intersection = AlgUtils.intersectSize(a, b);
                int union = AlgUtils.unionAllCount(new GeneSet[]{a, b});
                return String.format(Locale.ROOT,
                        "%.4f (intersection=%d, union=%d)%n%s (size=%d)%n%s (size=%d)",
                        value, intersection, union, rowName, a.getNumMembers(), colName, b.getNumMembers());
            }
            return String.format(Locale.ROOT, "%s / %s%nvalue=%.4f", rowName, colName, value);
        } catch (Throwable t) {
            return "Heatmap";
        }
    }

    private void openGeneSetHtml(String gsetName) {
        if (htmlLookupDir == null || gsetName == null) {
            Application.getWindowManager().showMessage("No report directory available for gene-set HTML.");
            return;
        }
        String lookup = gsetName.replaceAll("_signal", "");
        File html = new File(htmlLookupDir, lookup + ".html");
        if (!html.exists()) {
            File[] files = htmlLookupDir.listFiles((dir, name) ->
                    name.toLowerCase(Locale.ROOT).endsWith(".html")
                            && (name.startsWith(lookup) || name.startsWith(gsetName)));
            if (files != null && files.length > 0) {
                html = files[0];
            }
        }
        if (!html.exists()) {
            Application.getWindowManager().showMessage("No HTML report found for:\n" + gsetName);
            return;
        }
        try {
            xapps.gsea.fx.FxDesktopUtil.openUri(html.toURI());
        } catch (Exception ex) {
            Application.getWindowManager().showError("Could not open gene-set HTML", ex);
        }
    }

    private void runProfile() {
        int[] rows = getSelectedRowIndices();
        if (rows.length == 0 && dataset != null) {
            String noun = featureUIString == null || featureUIString.isBlank() ? "feature" : featureUIString;
            String lower = Character.toLowerCase(noun.charAt(0)) + noun.substring(1) + "s";
            Application.getWindowManager().showMessage("Please select " + lower + " to view.");
            return;
        }
        if (profileHandler != null) {
            profileHandler.accept(rows);
        } else {
            FxProfileDialog.show(dataset, rows);
        }
    }

    private void saveImage() {
        if (heatMap == null) {
            Application.getWindowManager().showMessage("No heatmap to save.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save heatmap image");
        chooser.setInitialFileName((similarityMode ? "similarity" : "leading_edge") + ".png");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG", "*.png"),
                new FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("SVG", "*.svg"));
        FxFileChooserUtil.seedInitialDirectory(chooser);
        File target = chooser.showSaveDialog(root.getScene() != null ? root.getScene().getWindow() : null);
        if (target == null) {
            return;
        }
        try {
            String name = target.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".svg")) {
                ImageUtil.savePlotImage(heatMap, target, "svg");
            } else {
                String format = name.endsWith(".jpg") || name.endsWith(".jpeg") ? "jpg" : "png";
                if (!name.endsWith(".png") && !name.endsWith(".jpg") && !name.endsWith(".jpeg")) {
                    target = new File(target.getParentFile(), target.getName() + ".png");
                    format = "png";
                }
                javax.imageio.ImageIO.write(heatMap.snapshot(), format, target);
            }
            FxFileChooserUtil.registerOpened(target);
            Application.getWindowManager().showMessage("Saved:\n" + target.getAbsolutePath());
        } catch (Exception ex) {
            Application.getWindowManager().showError("Could not save image", ex);
        }
    }

    private void saveDatasetSlice() {
        if (dataset == null) {
            return;
        }
        Window owner = root.getScene() != null ? root.getScene().getWindow() : null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Save Dataset");
        dialog.initOwner(owner);
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Save", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        Label instructions = new Label(
                "The selected columns and rows will be included in the dataset.");
        TextField pathField = new TextField();
        pathField.setPrefColumnCount(30);
        Button browse = new Button("Browse...");
        xapps.gsea.fx.widgets.FxButtons.styleSecondary(browse);
        browse.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Dataset");
            chooser.setInitialFileName(dataset.getName() + ".gct");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("GCT (*.gct)", "*.gct"),
                    new FileChooser.ExtensionFilter("RES (*.res)", "*.res"),
                    new FileChooser.ExtensionFilter("TXT (*.txt)", "*.txt"));
            FxFileChooserUtil.seedInitialDirectory(chooser, pathField.getText());
            File picked = chooser.showSaveDialog(owner);
            if (picked != null) {
                pathField.setText(picked.getAbsolutePath());
            }
        });
        HBox fileRow = new HBox(8, new Label("Output File:"), pathField, browse);
        HBox.setHgrow(pathField, Priority.ALWAYS);
        VBox body = new VBox(12, instructions, fileRow);
        body.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(body);
        xapps.gsea.fx.FxTheme.apply(dialog);

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(
                dialog.getDialogPane().getButtonTypes().get(0));
        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            String pathname = pathField.getText() != null ? pathField.getText().trim() : "";
            if (pathname.isEmpty()) {
                Application.getWindowManager().showError("Please enter an output file.");
                e.consume();
                return;
            }
            try {
                int[] rows = getSelectedRowIndices();
                int[] cols = getSelectedColumnIndices();
                Dataset toSave = dataset;
                if (rows.length > 0 || cols.length > 0) {
                    int[] useRows = rows.length > 0 ? rows : allIndices(dataset.getNumRow());
                    int[] useCols = cols.length > 0 ? cols : allIndices(dataset.getNumCol());
                    toSave = sliceDataset(dataset, useRows, useCols);
                }
                File target = new File(pathname);
                String lower = target.getName().toLowerCase(Locale.ROOT);
                if (!lower.endsWith(".gct") && !lower.endsWith(".res") && !lower.endsWith(".txt")) {
                    target = new File(target.getParentFile(), target.getName() + ".gct");
                }
                ParserFactory.save(toSave, target);
                FxFileChooserUtil.registerOpened(target);
                Application.getWindowManager().showMessage("Saved dataset:\n" + target.getAbsolutePath());
            } catch (Exception ex) {
                Application.getWindowManager().showError("Could not save dataset", ex);
                e.consume();
            }
        });
        dialog.showAndWait();
    }

    private static int[] allIndices(int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = i;
        }
        return a;
    }

    private static Dataset sliceDataset(Dataset src, int[] rows, int[] cols) {
        Matrix m = new Matrix(rows.length, cols.length);
        List<String> rowNames = new ArrayList<>();
        List<String> colNames = new ArrayList<>();
        for (int r = 0; r < rows.length; r++) {
            rowNames.add(src.getRowName(rows[r]));
            for (int c = 0; c < cols.length; c++) {
                if (r == 0) {
                    colNames.add(src.getColumnName(cols[c]));
                }
                m.setElement(r, c, src.getElement(rows[r], cols[c]));
            }
        }
        return new DefaultDataset(src.getName() + "_slice", m, rowNames, colNames, src.getAnnot());
    }
}
