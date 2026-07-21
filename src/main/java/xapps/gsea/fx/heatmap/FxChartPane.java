/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.heatmap;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Locale;
import java.util.Optional;
import java.util.function.IntConsumer;

import org.genepattern.io.ImageUtil;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.Range;
import org.jfree.fx.FXGraphics2D;

import edu.mit.broad.xbench.core.api.Application;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import xapps.gsea.fx.FxTheme;
import xapps.gsea.fx.params.FxFileChooserUtil;

/**
 * Interactive JFreeChart host for FX: zoom (buttons, Ctrl+scroll, drag-rect domain zoom),
 * reset, save PNG/JPEG/SVG, print, bar click. Display uses FXGraphics2D → Canvas.
 */
public class FxChartPane {

    private final BorderPane root = new BorderPane();
    private final Canvas canvas = new Canvas();
    private final javafx.scene.shape.Rectangle dragRect = new javafx.scene.shape.Rectangle();
    private final StackPane chartStack = new StackPane(canvas, dragRect);
    private final ChartRenderingInfo renderInfo = new ChartRenderingInfo();
    private final FXGraphics2D g2;

    private JFreeChart chart;
    private int chartWidth = 720;
    private int chartHeight = 360;
    private Range domainBackup;
    private Range rangeBackup;
    private IntConsumer itemClickHandler;
    private String[] itemNames;

    private boolean dragZooming;
    private double dragStartX;
    private double dragStartY;

    public FxChartPane(String title) {
        g2 = new FXGraphics2D(canvas.getGraphicsContext2D());
        dragRect.setFill(Color.rgb(30, 144, 255, 0.25));
        dragRect.setStroke(Color.DODGERBLUE);
        dragRect.setVisible(false);
        dragRect.setManaged(false);
        StackPane.setAlignment(dragRect, javafx.geometry.Pos.TOP_LEFT);

        ScrollPane scroll = new ScrollPane(chartStack);
        scroll.setFitToWidth(true);
        javafx.scene.layout.VBox.setVgrow(scroll, Priority.ALWAYS);

        Button reset = new Button("Reset");
        reset.setOnAction(e -> resetZoom());
        Button zoomIn = new Button("Zoom In");
        zoomIn.setOnAction(e -> zoom(0.8));
        zoomIn.setTooltip(new Tooltip("Zoom In (Ctrl/Cmd + ])"));
        Button zoomOut = new Button("Zoom Out");
        zoomOut.setOnAction(e -> zoom(1.1));
        zoomOut.setTooltip(new Tooltip("Zoom Out (Ctrl/Cmd + [)"));
        Button displayOpts = new Button("Display Options...");
        displayOpts.setOnAction(e -> showDisplayOptions());
        Button save = new Button("Save image...");
        save.setOnAction(e -> saveImage());
        Button print = new Button("Print...");
        print.setOnAction(e -> printImage());
        xapps.gsea.fx.FxButtons.styleToolbar(reset);
        xapps.gsea.fx.FxButtons.styleToolbar(zoomIn);
        xapps.gsea.fx.FxButtons.styleToolbar(zoomOut);
        xapps.gsea.fx.FxButtons.styleToolbar(displayOpts);
        xapps.gsea.fx.FxButtons.styleToolbar(save);
        xapps.gsea.fx.FxButtons.styleToolbar(print);

        HBox tools = xapps.gsea.fx.FxButtons.row(reset, zoomIn, zoomOut, displayOpts, save, print);
        xapps.gsea.fx.FxButtons.padTight(tools);
        tools.setMinWidth(0);

        root.setMinSize(0, 0);
        root.setTop(tools);
        root.setCenter(scroll);

        root.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (!e.isShortcutDown()) {
                return;
            }
            if (e.getCode() == javafx.scene.input.KeyCode.CLOSE_BRACKET) {
                zoom(0.9);
                e.consume();
            } else if (e.getCode() == javafx.scene.input.KeyCode.OPEN_BRACKET) {
                zoom(1.1);
                e.consume();
            }
        });
        root.setFocusTraversable(true);

        canvas.addEventHandler(ScrollEvent.SCROLL, e -> {
            if (e.isControlDown()) {
                zoom(e.getDeltaY() > 0 ? 0.9 : 1.1);
                e.consume();
            }
        });
        Tooltip tip = new Tooltip();
        tip.setShowDelay(javafx.util.Duration.millis(80));
        Tooltip.install(canvas, tip);
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            Integer item = hitItem(e);
            if (item != null && itemNames != null && item >= 0 && item < itemNames.length) {
                tip.setText(itemNames[item]);
            } else if (item != null) {
                tip.setText("Item " + item);
            } else {
                tip.setText(title != null ? title : "Chart");
            }
            if (tip.isShowing()) {
                tip.setX(e.getScreenX() + 14);
                tip.setY(e.getScreenY() + 18);
            }
        });
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.isPrimaryButtonDown()) {
                dragZooming = true;
                dragStartX = e.getX();
                dragStartY = e.getY();
                dragRect.setX(dragStartX);
                dragRect.setY(0);
                dragRect.setWidth(0);
                dragRect.setHeight(Math.max(1, canvas.getHeight()));
                dragRect.setVisible(true);
            }
        });
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!dragZooming) {
                return;
            }
            double x = Math.min(dragStartX, e.getX());
            double w = Math.abs(e.getX() - dragStartX);
            dragRect.setX(x);
            dragRect.setWidth(w);
            dragRect.setHeight(Math.max(1, canvas.getHeight()));
        });
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (!dragZooming) {
                return;
            }
            dragZooming = false;
            dragRect.setVisible(false);
            double w = Math.abs(e.getX() - dragStartX);
            if (w >= 8) {
                applyDomainDragZoom(Math.min(dragStartX, e.getX()), Math.max(dragStartX, e.getX()));
            } else {
                Integer item = hitItem(e);
                if (item != null && itemClickHandler != null) {
                    itemClickHandler.accept(item);
                }
            }
        });
    }

    public BorderPane getNode() {
        return root;
    }

    public void setChart(JFreeChart chart, int width, int height) {
        this.chart = chart;
        this.chartWidth = Math.max(200, width);
        this.chartHeight = Math.max(120, height);
        backupRanges();
        refresh();
    }

    public void setItemNames(String[] names) {
        this.itemNames = names;
    }

    public void setItemClickHandler(IntConsumer handler) {
        this.itemClickHandler = handler;
    }

    public JFreeChart getChart() {
        return chart;
    }

    /** Re-render the current chart (e.g. after mutating a selection-aware renderer). */
    public void refresh() {
        if (chart == null) {
            GraphicsContext ctx = canvas.getGraphicsContext2D();
            ctx.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            canvas.setWidth(0);
            canvas.setHeight(0);
            return;
        }
        canvas.setWidth(chartWidth);
        canvas.setHeight(chartHeight);
        GraphicsContext ctx = canvas.getGraphicsContext2D();
        ctx.clearRect(0, 0, chartWidth, chartHeight);
        renderInfo.clear();
        chart.draw(g2, new Rectangle(chartWidth, chartHeight), null, renderInfo);
    }

    private void backupRanges() {
        if (chart == null || !(chart.getPlot() instanceof XYPlot)) {
            return;
        }
        XYPlot plot = (XYPlot) chart.getPlot();
        domainBackup = plot.getDomainAxis().getRange();
        rangeBackup = plot.getRangeAxis().getRange();
    }

    /** Domain-only rubber-band zoom. */
    private void applyDomainDragZoom(double viewX0, double viewX1) {
        if (chart == null || !(chart.getPlot() instanceof XYPlot)) {
            return;
        }
        XYPlot plot = (XYPlot) chart.getPlot();
        PlotRenderingInfo plotInfo = renderInfo.getPlotInfo();
        if (plotInfo == null) {
            return;
        }
        Rectangle2D dataArea = plotInfo.getDataArea();
        if (dataArea == null || dataArea.getWidth() <= 0) {
            return;
        }
        // Canvas coords match chart draw coords 1:1 (no ImageView scale).
        double java0 = Math.max(dataArea.getMinX(), Math.min(dataArea.getMaxX(), viewX0));
        double java1 = Math.max(dataArea.getMinX(), Math.min(dataArea.getMaxX(), viewX1));
        if (Math.abs(java1 - java0) < 2) {
            return;
        }
        double v0 = plot.getDomainAxis().java2DToValue(java0, dataArea, plot.getDomainAxisEdge());
        double v1 = plot.getDomainAxis().java2DToValue(java1, dataArea, plot.getDomainAxisEdge());
        plot.getDomainAxis().setRange(new Range(Math.min(v0, v1), Math.max(v0, v1)));
        refresh();
    }

    private Integer hitItem(MouseEvent e) {
        if (chart == null) {
            return null;
        }
        int x = (int) e.getX();
        int y = (int) e.getY();
        ChartEntity entity = renderInfo.getEntityCollection() != null
                ? renderInfo.getEntityCollection().getEntity(x, y)
                : null;
        if (entity instanceof XYItemEntity) {
            return ((XYItemEntity) entity).getItem();
        }
        return null;
    }

    private void zoom(double factor) {
        if (chart == null || !(chart.getPlot() instanceof XYPlot)) {
            return;
        }
        XYPlot plot = (XYPlot) chart.getPlot();
        Range d = plot.getDomainAxis().getRange();
        Range r = plot.getRangeAxis().getRange();
        plot.getDomainAxis().setRange(scaleRange(d, factor));
        plot.getRangeAxis().setRange(scaleRange(r, factor));
        refresh();
    }

    private static Range scaleRange(Range range, double factor) {
        double mid = range.getCentralValue();
        double half = range.getLength() * factor / 2.0;
        return new Range(mid - half, mid + half);
    }

    private void resetZoom() {
        if (chart == null || !(chart.getPlot() instanceof XYPlot)) {
            return;
        }
        XYPlot plot = (XYPlot) chart.getPlot();
        if (domainBackup != null) {
            plot.getDomainAxis().setRange(domainBackup);
        }
        if (rangeBackup != null) {
            plot.getRangeAxis().setRange(rangeBackup);
        }
        refresh();
    }

    /** Display Options: title, legend, axis labels/ranges. */
    private void showDisplayOptions() {
        if (chart == null) {
            return;
        }
        XYPlot plot = chart.getPlot() instanceof XYPlot ? (XYPlot) chart.getPlot() : null;
        LegendTitle legend = chart.getLegend();

        TextField titleField = new TextField(chart.getTitle() != null ? chart.getTitle().getText() : "");
        CheckBox showLegend = new CheckBox("Show legend");
        showLegend.setSelected(legend != null && legend.isVisible());
        showLegend.setDisable(legend == null);

        TextField xLabel = new TextField();
        TextField xMin = new TextField();
        TextField xMax = new TextField();
        CheckBox xAuto = new CheckBox("Auto-range");
        TextField yLabel = new TextField();
        TextField yMin = new TextField();
        TextField yMax = new TextField();
        CheckBox yAuto = new CheckBox("Auto-range");
        if (plot != null) {
            bindAxisFields(plot.getDomainAxis(), xLabel, xMin, xMax, xAuto);
            bindAxisFields(plot.getRangeAxis(), yLabel, yMin, yMax, yAuto);
        }

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        ColumnConstraints grow = new ColumnConstraints();
        grow.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(new ColumnConstraints(), grow, grow, new ColumnConstraints());

        int row = 0;
        grid.addRow(row++, new Label("Title:"), titleField);
        GridPane.setColumnSpan(titleField, 3);
        grid.add(showLegend, 1, row++, 3, 1);
        if (plot != null) {
            grid.addRow(row++, new Label("X axis:"), xLabel);
            GridPane.setColumnSpan(xLabel, 3);
            grid.add(new Label("X range:"), 0, row);
            grid.add(xMin, 1, row);
            grid.add(xMax, 2, row);
            grid.add(xAuto, 3, row++);
            grid.addRow(row++, new Label("Y axis:"), yLabel);
            GridPane.setColumnSpan(yLabel, 3);
            grid.add(new Label("Y range:"), 0, row);
            grid.add(yMin, 1, row);
            grid.add(yMax, 2, row);
            grid.add(yAuto, 3, row);
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Display Options");
        dialog.setHeaderText(null);
        if (root.getScene() != null && root.getScene().getWindow() != null) {
            dialog.initOwner(root.getScene().getWindow());
        }
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        FxTheme.apply(dialog);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        String titleText = titleField.getText() != null ? titleField.getText().trim() : "";
        if (chart.getTitle() != null) {
            chart.getTitle().setText(titleText);
        } else if (!titleText.isEmpty()) {
            chart.setTitle(titleText);
        }
        if (legend != null) {
            legend.setVisible(showLegend.isSelected());
        }
        if (plot != null) {
            applyAxisFields(plot.getDomainAxis(), xLabel, xMin, xMax, xAuto);
            applyAxisFields(plot.getRangeAxis(), yLabel, yMin, yMax, yAuto);
            backupRanges();
        }
        refresh();
    }

    private static void bindAxisFields(ValueAxis axis, TextField label, TextField min, TextField max,
            CheckBox auto) {
        label.setText(axis.getLabel() != null ? axis.getLabel() : "");
        Range range = axis.getRange();
        min.setText(formatBound(range.getLowerBound()));
        max.setText(formatBound(range.getUpperBound()));
        auto.setSelected(axis.isAutoRange());
        Runnable sync = () -> {
            min.setDisable(auto.isSelected());
            max.setDisable(auto.isSelected());
        };
        auto.setOnAction(e -> sync.run());
        sync.run();
    }

    private static void applyAxisFields(ValueAxis axis, TextField label, TextField min, TextField max,
            CheckBox auto) {
        axis.setLabel(label.getText());
        axis.setAutoRange(auto.isSelected());
        if (!auto.isSelected()) {
            Double lo = parseBound(min.getText());
            Double hi = parseBound(max.getText());
            if (lo != null && hi != null && lo < hi) {
                axis.setRange(new Range(lo, hi));
            }
        }
    }

    private static String formatBound(double v) {
        return Double.isFinite(v) ? String.format(Locale.ROOT, "%.6g", v) : "";
    }

    private static Double parseBound(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void saveImage() {
        if (chart == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save chart");
        chooser.setInitialFileName("chart.png");
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
                ImageUtil.saveAsSVG(chart, target, chartWidth, chartHeight, false);
            } else {
                String format = name.endsWith(".jpg") || name.endsWith(".jpeg") ? "jpg" : "png";
                if (!name.endsWith(".png") && !name.endsWith(".jpg") && !name.endsWith(".jpeg")) {
                    target = new File(target.getParentFile(), target.getName() + ".png");
                    format = "png";
                }
                javax.imageio.ImageIO.write(
                        chart.createBufferedImage(chartWidth, chartHeight), format, target);
            }
            FxFileChooserUtil.registerOpened(target);
            Application.getWindowManager().showMessage("Saved:\n" + target.getAbsolutePath());
        } catch (Exception ex) {
            Application.getWindowManager().showError("Could not save chart", ex);
        }
    }

    private void printImage() {
        if (chart == null) {
            return;
        }
        BufferedImage image = chart.createBufferedImage(chartWidth, chartHeight);
        try {
            java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
            job.setJobName("GSEA chart");
            job.setPrintable((graphics, pageFormat, pageIndex) -> {
                if (pageIndex > 0) {
                    return java.awt.print.Printable.NO_SUCH_PAGE;
                }
                java.awt.Graphics2D g2d = (java.awt.Graphics2D) graphics;
                double x = pageFormat.getImageableX();
                double y = pageFormat.getImageableY();
                double w = pageFormat.getImageableWidth();
                double h = pageFormat.getImageableHeight();
                double scale = Math.min(w / image.getWidth(), h / image.getHeight());
                g2d.drawImage(image, (int) x, (int) y,
                        (int) (image.getWidth() * scale), (int) (image.getHeight() * scale), null);
                return java.awt.print.Printable.PAGE_EXISTS;
            });
            if (job.printDialog()) {
                job.print();
            }
        } catch (Exception ex) {
            Application.getWindowManager().showError("Could not print chart", ex);
        }
    }
}
