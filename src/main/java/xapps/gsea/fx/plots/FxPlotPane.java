/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.plots;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.IntConsumer;

import edu.mit.broad.genome.plots.HistogramPlotSpec;
import edu.mit.broad.genome.plots.PlotRasterExporter;
import edu.mit.broad.genome.plots.PlotSpec;
import edu.mit.broad.xbench.core.api.Application;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import xapps.gsea.fx.widgets.FxButtons;
import xapps.gsea.fx.widgets.FxImages;
import xapps.gsea.fx.params.FxFileChooserUtil;

/**
 * FX host for {@link PlotSpec}: rasterizes via {@link PlotRasterExporter}.
 * Simple hist/xy/bubble interactive surfaces use this pane; enrichment mountains use
 * {@code EnplotWebView} (WebView + EnPlot JSON).
 */
public final class FxPlotPane {

    private final BorderPane root = new BorderPane();
    private final ImageView imageView = new ImageView();
    private PlotSpec spec;
    private int chartWidth = 720;
    private int chartHeight = 360;
    private String[] itemNames;
    private IntConsumer itemClickHandler;

    public FxPlotPane(String title) {
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        ScrollPane scroll = new ScrollPane(imageView);
        scroll.setFitToWidth(true);
        javafx.scene.layout.VBox.setVgrow(scroll, Priority.ALWAYS);

        Button save = new Button("Save image...");
        FxButtons.styleToolbar(save);
        save.setOnAction(e -> saveImage());
        HBox tools = FxButtons.row(save);
        FxButtons.padTight(tools);

        root.setMinSize(0, 0);
        root.setTop(tools);
        root.setCenter(scroll);

        imageView.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (itemClickHandler == null || !(spec instanceof HistogramPlotSpec hist) || hist.size() == 0) {
                return;
            }
            double localX = e.getX();
            double imgW = imageView.getBoundsInLocal().getWidth();
            if (imgW <= 0) {
                return;
            }
            // Plot area matches PlotRasterExporter default margins.
            double plotLeft = imgW * PlotRasterExporter.MARGIN_L / Math.max(chartWidth, 1);
            double plotRight = imgW * (1.0 - PlotRasterExporter.MARGIN_R / (double) Math.max(chartWidth, 1));
            if (localX < plotLeft || localX > plotRight) {
                return;
            }
            int idx = (int) ((localX - plotLeft) / (plotRight - plotLeft) * hist.size());
            if (idx < 0) {
                idx = 0;
            }
            if (idx >= hist.size()) {
                idx = hist.size() - 1;
            }
            itemClickHandler.accept(idx);
        });

        Tooltip tip = new Tooltip(title != null ? title : "Plot");
        tip.setShowDelay(javafx.util.Duration.millis(80));
        Tooltip.install(imageView, tip);
        imageView.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            if (itemNames == null || !(spec instanceof HistogramPlotSpec hist) || hist.size() == 0) {
                tip.setText(title != null ? title : "Plot");
                return;
            }
            double localX = e.getX();
            double imgW = imageView.getBoundsInLocal().getWidth();
            double plotLeft = imgW * PlotRasterExporter.MARGIN_L / Math.max(chartWidth, 1);
            double plotRight = imgW * (1.0 - PlotRasterExporter.MARGIN_R / (double) Math.max(chartWidth, 1));
            if (localX < plotLeft || localX > plotRight || imgW <= 0) {
                tip.setText(title != null ? title : "Plot");
                return;
            }
            int idx = (int) ((localX - plotLeft) / (plotRight - plotLeft) * hist.size());
            if (idx >= 0 && idx < itemNames.length) {
                tip.setText(itemNames[idx]);
            }
        });
    }

    public Node getNode() {
        return root;
    }

    public void setSpec(PlotSpec spec, int width, int height) {
        this.spec = spec;
        this.chartWidth = Math.max(80, width);
        this.chartHeight = Math.max(60, height);
        refresh();
    }

    public void setItemNames(String[] names) {
        this.itemNames = names;
    }

    public void setItemClickHandler(IntConsumer handler) {
        this.itemClickHandler = handler;
    }

    public void refresh() {
        if (spec == null) {
            imageView.setImage(null);
            return;
        }
        BufferedImage bi = PlotRasterExporter.render(spec, chartWidth, chartHeight);
        imageView.setImage(FxImages.toFxImage(bi));
        imageView.setFitWidth(chartWidth);
    }

    private void saveImage() {
        if (spec == null) {
            return;
        }
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save plot image");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PNG", "*.png"));
            FxFileChooserUtil.seedInitialDirectory(chooser);
            Window owner = root.getScene() != null ? root.getScene().getWindow() : null;
            File target = chooser.showSaveDialog(owner);
            if (target == null) {
                return;
            }
            if (!target.getName().toLowerCase().endsWith(".png")) {
                target = new File(target.getParentFile(), target.getName() + ".png");
            }
            PlotRasterExporter.savePng(spec, target, chartWidth, chartHeight);
        } catch (Exception ex) {
            Application.getWindowManager().showError("Could not save plot", ex);
        }
    }
}
