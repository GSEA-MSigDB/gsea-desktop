/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.gsea_msigdb.gsea.ui.api.ViewPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.xbench.core.api.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import xapps.gsea.fx.FxButtons;
import xapps.gsea.fx.FxDesktopUtil;
import xapps.gsea.fx.viewers.FxViewerFactory;

/**
 * Shared helpers for native report explorers.
 * I/O may run off-thread; all Node construction happens on the FX thread.
 */
public final class ReportExplorerSupport {

    private static final Logger klog = LoggerFactory.getLogger(ReportExplorerSupport.class);

    private ReportExplorerSupport() {
    }

    public static File reportDir(Report report) {
        return report != null ? report.getReportDir() : null;
    }

    public static Consumer<ViewPage> safeOpen(Consumer<ViewPage> openPage) {
        return openPage != null ? openPage : page -> { };
    }

    /**
     * Show a loading placeholder, run {@code load} off the FX thread, then build UI on the FX thread.
     */
    public static <T> void loadAsync(BorderPane host, String threadName, String loadingMessage,
            Callable<T> load, Function<T, Node> buildUi, String errorTitle) {
        host.setCenter(loadingPlaceholder(loadingMessage));
        Thread t = new Thread(() -> {
            try {
                T data = load.call();
                Platform.runLater(() -> {
                    try {
                        host.setCenter(buildUi.apply(data));
                    } catch (Throwable uiErr) {
                        klog.error(errorTitle, uiErr);
                        host.setCenter(messagePane(errorTitle + "\n" + safeMessage(uiErr)));
                    }
                });
            } catch (Throwable loadErr) {
                klog.error(errorTitle, loadErr);
                Platform.runLater(() -> host.setCenter(messagePane(
                        errorTitle + "\n" + safeMessage(loadErr)
                                + "\n\nUse the Files tab or Open HTML report.")));
            }
        }, threadName);
        t.setDaemon(true);
        t.start();
    }

    public static Node loadingPlaceholder(String message) {
        ProgressIndicator spin = new ProgressIndicator();
        spin.setMaxSize(48, 48);
        Label label = new Label(message != null ? message : "Loading…");
        VBox box = new VBox(12, spin, label);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(24));
        return new BorderPane(box);
    }

    public static Node messagePane(String message) {
        Label label = new Label(message);
        label.setWrapText(true);
        label.setPadding(new Insets(16));
        return label;
    }

    private static String safeMessage(Throwable t) {
        if (t == null) {
            return "";
        }
        String msg = t.getMessage();
        return msg != null && !msg.isBlank() ? msg : t.getClass().getSimpleName();
    }

    public static Tab fixedTab(String title, Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    public static TabPane tabPane(Tab... tabs) {
        TabPane pane = new TabPane(tabs);
        pane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        pane.setMinSize(0, 0);
        pane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return pane;
    }

    public static String shortTitle(String name, int max) {
        if (name == null) {
            return "";
        }
        return name.length() <= max ? name : name.substring(0, Math.max(0, max - 1)) + "…";
    }

    public static File findFirst(File dir, Predicate<String> nameMatch) {
        List<File> all = findAll(dir, nameMatch);
        return all.isEmpty() ? null : all.get(0);
    }

    public static List<File> findAll(File dir, Predicate<String> nameMatch) {
        List<File> out = new ArrayList<>();
        if (dir == null || !dir.isDirectory() || nameMatch == null) {
            return out;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return out;
        }
        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File f : files) {
            if (f.isFile() && nameMatch.test(f.getName())) {
                out.add(f);
            }
        }
        return out;
    }

    public static List<File> findImages(File dir, Predicate<String> nameMatch) {
        return findAll(dir, name -> isImageName(name) && (nameMatch == null || nameMatch.test(name)));
    }

    public static boolean isImageName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }

    public static boolean endsWithIgnoreCase(String name, String suffix) {
        return name != null && suffix != null
                && name.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT));
    }

    public static Node imageGallery(List<File> images) {
        if (images == null || images.isEmpty()) {
            return messagePane("No plots found");
        }
        FlowPane flow = new FlowPane(12, 12);
        flow.setPadding(new Insets(12));
        for (File image : images) {
            try {
                ImageView view = new ImageView(new Image(image.toURI().toString(), 360, 0, true, true, true));
                view.setPreserveRatio(true);
                Label caption = new Label(image.getName());
                caption.setWrapText(true);
                caption.setMaxWidth(360);
                VBox card = new VBox(4, view, caption);
                card.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2) {
                        openFileExternal(image);
                    }
                });
                flow.getChildren().add(card);
            } catch (Throwable t) {
                klog.warn("Could not load image {}", image, t);
            }
        }
        ScrollPane scroll = new ScrollPane(flow);
        scroll.setFitToWidth(true);
        return scroll;
    }

    public static Node singleImage(File image) {
        if (image == null || !image.isFile()) {
            return messagePane("No image available");
        }
        try {
            return zoomableImage(new Image(image.toURI().toString(), true));
        } catch (Throwable t) {
            klog.warn("Could not load image {}", image, t);
            return messagePane("Could not load image: " + image.getName());
        }
    }

    /** Zoomable viewer for an in-memory JavaFX image (e.g. on-demand chart render). */
    public static Node singleImage(Image image) {
        if (image == null) {
            return messagePane("No image available");
        }
        return zoomableImage(image);
    }

    /**
     * Report heatmap / plot viewer: starts fitted to the viewport; Zoom In/Out, Fit, 100%,
     * Ctrl/Cmd+scroll to zoom, drag-pan when larger than the viewport.
     */
    private static Node zoomableImage(Image image) {
        return new ZoomableImagePane(image);
    }

    /**
     * Interactive image host for report explorers (Leading Edge heatmaps, GSEA plots, …).
     */
    private static final class ZoomableImagePane extends BorderPane {
        private static final double ZOOM_STEP = 1.25;
        private static final double MIN_ZOOM = 0.05;
        private static final double MAX_ZOOM = 32.0;

        private final Image image;
        private final ImageView view = new ImageView();
        private final ScrollPane scroll = new ScrollPane();
        private final Label zoomLabel = new Label();
        /** Absolute scale vs native pixels; {@link Double#NaN} means fit-to-viewport. */
        private double zoom = Double.NaN;

        ZoomableImagePane(Image image) {
            this.image = image;
            view.setImage(image);
            view.setPreserveRatio(true);
            view.setSmooth(true);
            view.setCache(true);

            scroll.setContent(view);
            scroll.setPannable(true);
            scroll.setFitToWidth(false);
            scroll.setFitToHeight(false);
            scroll.setMinSize(0, 0);
            VBox.setVgrow(scroll, Priority.ALWAYS);

            setTop(buildToolbar());
            setCenter(scroll);
            setMinSize(0, 0);
            setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            scroll.viewportBoundsProperty().addListener((obs, o, n) -> {
                if (Double.isNaN(zoom)) {
                    applyZoom();
                }
            });
            image.progressProperty().addListener((obs, o, n) -> {
                if (n != null && n.doubleValue() >= 1.0) {
                    Platform.runLater(this::applyZoom);
                }
            });
            addEventFilter(ScrollEvent.SCROLL, e -> {
                if (e.isControlDown() || e.isMetaDown()) {
                    e.consume();
                    zoomBy(e.getDeltaY() > 0 ? ZOOM_STEP : 1.0 / ZOOM_STEP);
                }
            });
            applyZoom();
        }

        private HBox buildToolbar() {
            Button fit = new Button("Fit");
            fit.setTooltip(new Tooltip("Fit image to the viewport"));
            fit.setOnAction(e -> {
                zoom = Double.NaN;
                applyZoom();
            });
            Button actual = new Button("100%");
            actual.setTooltip(new Tooltip("Show at native pixel size"));
            actual.setOnAction(e -> {
                zoom = 1.0;
                applyZoom();
            });
            Button zoomIn = new Button("Zoom In");
            zoomIn.setTooltip(new Tooltip("Zoom In (Ctrl/Cmd + scroll)"));
            zoomIn.setOnAction(e -> zoomBy(ZOOM_STEP));
            Button zoomOut = new Button("Zoom Out");
            zoomOut.setTooltip(new Tooltip("Zoom Out (Ctrl/Cmd + scroll)"));
            zoomOut.setOnAction(e -> zoomBy(1.0 / ZOOM_STEP));
            FxButtons.styleToolbar(fit);
            FxButtons.styleToolbar(actual);
            FxButtons.styleToolbar(zoomIn);
            FxButtons.styleToolbar(zoomOut);
            zoomLabel.getStyleClass().add("gsea-muted");
            HBox tools = FxButtons.row(fit, actual, zoomIn, zoomOut, zoomLabel);
            tools.setPadding(new Insets(4, 8, 4, 8));
            tools.setMinWidth(0);
            return tools;
        }

        private void zoomBy(double factor) {
            zoom = clamp(currentAbsoluteZoom() * factor);
            applyZoom();
        }

        private double currentAbsoluteZoom() {
            if (!Double.isNaN(zoom)) {
                return zoom;
            }
            double iw = image.getWidth();
            double fitted = view.getFitWidth();
            if (iw > 0 && fitted > 0) {
                return fitted / iw;
            }
            return fitScale();
        }

        private double fitScale() {
            var bounds = scroll.getViewportBounds();
            double iw = image.getWidth();
            double ih = image.getHeight();
            if (iw <= 0 || ih <= 0 || bounds.getWidth() <= 1 || bounds.getHeight() <= 1) {
                return 1.0;
            }
            return Math.min(bounds.getWidth() / iw, bounds.getHeight() / ih);
        }

        private void applyZoom() {
            double iw = image.getWidth();
            if (iw <= 0) {
                return;
            }
            double scale = Double.isNaN(zoom) ? fitScale() : clamp(zoom);
            view.setFitWidth(iw * scale);
            zoomLabel.setText(Math.round(scale * 100) + "%");
        }

        private static double clamp(double z) {
            return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, z));
        }
    }

    public static Node tsvTable(File tsv) {
        if (tsv == null || !tsv.isFile()) {
            return messagePane("No table file");
        }
        try {
            List<String[]> rows = new ArrayList<>();
            try (BufferedReader br = Files.newBufferedReader(tsv.toPath(), StandardCharsets.UTF_8)) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.isBlank() || line.startsWith("#")) {
                        continue;
                    }
                    rows.add(line.split("\t", -1));
                }
            }
            if (rows.isEmpty()) {
                return messagePane("Empty table: " + tsv.getName());
            }
            String[] header = rows.get(0);
            TableView<String[]> table = new TableView<>(
                    FXCollections.observableArrayList(rows.subList(1, rows.size())));
            table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            table.setPlaceholder(new Label(""));
            for (int c = 0; c < header.length; c++) {
                final int col = c;
                String title = header[c].isBlank() ? "Col " + (c + 1) : header[c];
                TableColumn<String[], String> column = new TableColumn<>(title);
                column.setCellValueFactory(cdf -> {
                    String[] row = cdf.getValue();
                    return new SimpleStringProperty(
                            row != null && col < row.length ? row[col] : "");
                });
                column.setPrefWidth(Math.min(220, Math.max(80, title.length() * 9)));
                table.getColumns().add(column);
            }
            return table;
        } catch (Throwable t) {
            klog.warn("Could not read TSV {}", tsv, t);
            return messagePane("Could not read " + tsv.getName());
        }
    }

    public static Node textFile(File file) {
        if (file == null || !file.isFile()) {
            return messagePane("No file");
        }
        try {
            TextArea area = new TextArea(Files.readString(file.toPath()));
            area.setEditable(false);
            area.setWrapText(false);
            return area;
        } catch (Throwable t) {
            return messagePane("Could not read " + file.getName());
        }
    }

    public static void openFileExternal(File file) {
        if (file == null) {
            return;
        }
        try {
            FxDesktopUtil.openUri(file.toURI());
        } catch (Exception e) {
            Application.getWindowManager().showError("Could not open file", e);
        }
    }

    /**
     * Tab whose content is built only when first selected (or immediately if already selected).
     */
    public static Tab lazyNodeTab(String title, java.util.function.Supplier<Node> content) {
        BorderPane host = new BorderPane();
        host.setCenter(messagePane("Select this tab to load…"));
        Tab tab = fixedTab(title, host);
        java.util.concurrent.atomic.AtomicBoolean started =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        Runnable start = () -> {
            if (!started.compareAndSet(false, true)) {
                return;
            }
            try {
                host.setCenter(content.get());
            } catch (Throwable t) {
                host.setCenter(messagePane("Could not load tab: "
                        + (t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName())));
            }
        };
        tab.selectedProperty().addListener((obs, was, isNow) -> {
            if (Boolean.TRUE.equals(isNow)) {
                start.run();
            }
        });
        Platform.runLater(() -> {
            if (tab.isSelected()) {
                start.run();
            }
        });
        return tab;
    }

    /**
     * Lazy tab that loads only when the user selects it (never on initial layout).
     * Use for heavy content nested under an already-selected parent tab.
     */
    public static Tab lazyNodeTabOnClick(String title, java.util.function.Supplier<Node> content) {
        BorderPane host = new BorderPane();
        host.setCenter(messagePane("Select this tab to load…"));
        Tab tab = fixedTab(title, host);
        java.util.concurrent.atomic.AtomicBoolean started =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        tab.selectedProperty().addListener((obs, was, isNow) -> {
            if (!Boolean.TRUE.equals(isNow) || !started.compareAndSet(false, true)) {
                return;
            }
            try {
                host.setCenter(content.get());
            } catch (Throwable t) {
                host.setCenter(messagePane("Could not load tab: "
                        + (t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName())));
            }
        });
        return tab;
    }

    /**
     * Tab whose file content is parsed only when first selected (or immediately if already
     * selected). Avoids blocking the UI when a Results tab hosts a large GCT/GMX.
     */
    public static Tab lazyFileTab(String title, File file, String missingMessage) {
        if (file == null || !file.isFile()) {
            return fixedTab(title, messagePane(
                    missingMessage != null ? missingMessage : "File not found"));
        }
        BorderPane host = new BorderPane();
        host.setCenter(messagePane("Select this tab to load " + file.getName()));
        Tab tab = fixedTab(title, host);
        java.util.concurrent.atomic.AtomicBoolean started =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        Runnable start = () -> {
            if (!started.compareAndSet(false, true)) {
                return;
            }
            startFileViewerLoad(host, file);
        };
        tab.selectedProperty().addListener((obs, was, isNow) -> {
            if (Boolean.TRUE.equals(isNow)) {
                start.run();
            }
        });
        // When this becomes the TabPane's selected tab on add, selected may already be true
        // before the listener above is useful — start once the tab is attached.
        tab.tabPaneProperty().addListener((obs, was, pane) -> {
            if (pane != null && tab.isSelected()) {
                start.run();
            }
        });
        return tab;
    }

    private static void startFileViewerLoad(BorderPane host, File file) {
        loadAsync(host, "gsea-file-viewer", "Loading " + file.getName() + "…",
                () -> ParserFactory.read(file, true),
                obj -> viewerForObject(obj, file.getName()),
                "Could not load " + file.getName());
    }

    private static Node viewerForObject(Object obj, String name) {
        if (obj == null) {
            return messagePane("Could not load " + name + ": empty result");
        }
        try {
            Object content = FxViewerFactory.open(obj).getContent();
            return content instanceof Node n ? n : messagePane(String.valueOf(content));
        } catch (Throwable t) {
            klog.warn("Could not open viewer for {}", name, t);
            return messagePane("Could not open " + name + ": " + safeMessage(t));
        }
    }

    public static File resolveEdbDir(File reportDir) {
        if (reportDir == null) {
            return null;
        }
        File nested = new File(reportDir, "edb");
        if (new File(nested, "results.edb").isFile()) {
            return nested;
        }
        if (new File(reportDir, "results.edb").isFile()) {
            return reportDir;
        }
        return nested.isDirectory() ? nested : reportDir;
    }
}
