/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.mit.broad.xbench.core.api.Application;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import xapps.gsea.fx.params.FxFileChooserUtil;

/**
 * Save-as dialog for on-demand enrichment plots: width/height (px), DPI scaling, and format.
 */
public final class EnplotExportDialog {

    public enum Format {
        PNG("PNG", "png", "*.png"),
        JPEG("JPEG", "jpg", "*.jpg", "*.jpeg"),
        TIFF("TIFF", "tif", "*.tif", "*.tiff"),
        SVG("SVG", "svg", "*.svg");

        final String label;
        final String extension;
        final String[] glob;

        Format(String label, String extension, String... glob) {
            this.label = label;
            this.extension = extension;
            this.glob = glob;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public record Spec(int widthPx, int heightPx, int dpi, Format format) {
        int renderWidth() {
            return scale(widthPx);
        }

        int renderHeight() {
            return scale(heightPx);
        }

        private int scale(int px) {
            int d = Math.max(1, dpi);
            // 72 DPI → 1× stated pixels; higher DPI enlarges the raster for print quality.
            long scaled = Math.round(px * (d / 72.0));
            return (int) Math.max(1, Math.min(Integer.MAX_VALUE / 4, scaled));
        }
    }

    private EnplotExportDialog() {
    }

    /**
     * Prompt for size/DPI/format and write a raster snapshot (interactive EnPlot canvas).
     * SVG embeds the snapshot as a scaled image (canvas is not a vector source).
     */
    public static Optional<File> saveImage(Window owner,
                                           BufferedImage image,
                                           String suggestedBaseName,
                                           int defaultWidthPx,
                                           int defaultHeightPx) {
        if (image == null) {
            return Optional.empty();
        }
        int w = defaultWidthPx > 0 ? defaultWidthPx : image.getWidth();
        int h = defaultHeightPx > 0 ? defaultHeightPx : image.getHeight();
        Optional<Spec> specOpt = prompt(owner, w, h, Format.PNG);
        if (specOpt.isEmpty()) {
            return Optional.empty();
        }
        Spec spec = specOpt.get();

        Optional<File> targetOpt = chooseTarget(owner, suggestedBaseName, spec.format());
        if (targetOpt.isEmpty()) {
            return Optional.empty();
        }
        File target = targetOpt.get();

        try {
            writeImage(image, target, spec);
            FxFileChooserUtil.registerOpened(target);
            Application.getWindowManager().showMessage("Saved:\n" + target.getAbsolutePath());
            return Optional.of(target);
        } catch (Exception ex) {
            Application.getWindowManager().showError("Could not save enrichment plot", ex);
            return Optional.empty();
        }
    }

    private static Optional<File> chooseTarget(Window owner, String suggestedBaseName, Format format) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save enrichment plot");
        String base = suggestedBaseName == null || suggestedBaseName.isBlank()
                ? "enplot" : suggestedBaseName.replaceAll("[^A-Za-z0-9._-]+", "_");
        chooser.setInitialFileName(base + "." + format.extension);
        chooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter(format.label, format.glob));
        FxFileChooserUtil.seedInitialDirectory(chooser);
        File target = chooser.showSaveDialog(owner);
        if (target == null) {
            return Optional.empty();
        }
        return Optional.of(ensureExtension(target, format));
    }

    static Optional<Spec> prompt(Window owner, int defaultWidthPx, int defaultHeightPx, Format defaultFormat) {
        Dialog<Spec> dialog = new Dialog<>();
        dialog.setTitle("Save enrichment plot");
        dialog.setHeaderText("Choose output size, resolution, and format");
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Spinner<Integer> width = intSpinner(defaultWidthPx, 50, 20000);
        Spinner<Integer> height = intSpinner(defaultHeightPx, 50, 20000);
        Spinner<Integer> dpi = intSpinner(150, 36, 1200);
        ComboBox<Format> format = new ComboBox<>();
        for (Format f : Format.values()) {
            if (f == Format.TIFF && !hasImageWriter("TIFF") && !hasImageWriter("tif")) {
                continue;
            }
            format.getItems().add(f);
        }
        Format initial = defaultFormat != null && format.getItems().contains(defaultFormat)
                ? defaultFormat : Format.PNG;
        format.getSelectionModel().select(initial);

        Label hint = new Label("Raster pixels = size × (DPI / 72). SVG uses width/height as canvas size; DPI is ignored.");
        hint.setWrapText(true);
        hint.getStyleClass().add("gsea-muted");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        grid.add(new Label("Width (px)"), 0, 0);
        grid.add(width, 1, 0);
        grid.add(new Label("Height (px)"), 0, 1);
        grid.add(height, 1, 1);
        grid.add(new Label("DPI"), 0, 2);
        grid.add(dpi, 1, 2);
        grid.add(new Label("Format"), 0, 3);
        grid.add(format, 1, 3);
        grid.add(hint, 0, 4, 2, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(420);

        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        xapps.gsea.fx.FxButtons.stylePrimary(ok);
        Button cancel = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        xapps.gsea.fx.FxButtons.styleSecondary(cancel);

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) {
                return null;
            }
            commitSpinner(width);
            commitSpinner(height);
            commitSpinner(dpi);
            return new Spec(width.getValue(), height.getValue(), dpi.getValue(),
                    format.getSelectionModel().getSelectedItem());
        });

        return dialog.showAndWait();
    }

    private static void commitSpinner(Spinner<Integer> spinner) {
        try {
            String text = spinner.getEditor().getText();
            if (text != null && !text.isBlank()) {
                spinner.getValueFactory().setValue(Integer.parseInt(text.trim()));
            }
        } catch (NumberFormatException ignored) {
            // keep current spinner value
        }
    }

    private static boolean hasImageWriter(String formatName) {
        return ImageIO.getImageWritersByFormatName(formatName).hasNext();
    }

    static void writeImage(BufferedImage source, File target, Spec spec) throws IOException {
        Format format = spec.format();
        int w = format == Format.SVG ? Math.max(1, spec.widthPx()) : spec.renderWidth();
        int h = format == Format.SVG ? Math.max(1, spec.heightPx()) : spec.renderHeight();
        BufferedImage image = scaleImage(source, w, h);
        if (format == Format.SVG) {
            writeSvgFromImage(image, target, w, h);
            return;
        }
        if (format == Format.JPEG) {
            image = toRgb(image);
        }
        writeRaster(image, target, format, spec.dpi());
    }

    private static BufferedImage scaleImage(BufferedImage source, int width, int height) {
        if (source.getWidth() == width && source.getHeight() == height) {
            return source;
        }
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return scaled;
    }

    private static void writeSvgFromImage(BufferedImage image, File target, int width, int height)
            throws IOException {
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
        Document document = domImpl.createDocument("http://www.w3.org/2000/svg", "svg", null);
        SVGGraphics2D svg = new SVGGraphics2D(document);
        svg.setSVGCanvasSize(new Dimension(width, height));
        svg.drawImage(image, 0, 0, width, height, null);
        try (Writer out = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(target), StandardCharsets.UTF_8))) {
            svg.stream(out, true);
        } finally {
            svg.dispose();
        }
    }

    private static void writeRaster(BufferedImage image, File target, Format format, int dpi)
            throws IOException {
        String formatName = switch (format) {
            case JPEG -> "jpg";
            case TIFF -> "TIFF";
            default -> "png";
        };
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);
        if (!writers.hasNext() && format == Format.TIFF) {
            writers = ImageIO.getImageWritersByFormatName("tif");
        }
        if (!writers.hasNext()) {
            throw new IOException("No ImageIO writer for format " + format.label
                    + ". Try PNG, JPEG, or SVG.");
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(target)) {
            if (ios == null) {
                throw new IOException("Could not open output stream for " + target);
            }
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(image);
            IIOMetadata metadata = writer.getDefaultImageMetadata(type, param);
            applyDpi(metadata, format, dpi);
            writer.write(null, new IIOImage(image, null, metadata), param);
        } finally {
            writer.dispose();
        }
    }

    private static void applyDpi(IIOMetadata metadata, Format format, int dpi) {
        if (metadata == null || dpi <= 0) {
            return;
        }
        try {
            if (format == Format.PNG) {
                applyPngDpi(metadata, dpi);
            } else if (format == Format.JPEG) {
                applyJpegDpi(metadata, dpi);
            }
            // TIFF DPI via standard metadata varies by plugin; pixel size already reflects DPI scaling.
        } catch (Throwable ignored) {
            // Metadata is best-effort; the scaled pixel dimensions still reflect the chosen DPI.
        }
    }

    private static void applyPngDpi(IIOMetadata metadata, int dpi) throws Exception {
        double dotsPerMeter = dpi / 0.0254;
        String metaFormat = "javax_imageio_png_1.0";
        if (!metadata.isStandardMetadataFormatSupported()
                && !arrayContains(metadata.getMetadataFormatNames(), metaFormat)) {
            return;
        }
        IIOMetadataNode root;
        try {
            root = (IIOMetadataNode) metadata.getAsTree(metaFormat);
        } catch (IllegalArgumentException ex) {
            root = new IIOMetadataNode(metaFormat);
        }
        IIOMetadataNode phys = getOrCreateChild(root, "pHYs");
        phys.setAttribute("pixelsPerUnitXAxis", String.valueOf(Math.round(dotsPerMeter)));
        phys.setAttribute("pixelsPerUnitYAxis", String.valueOf(Math.round(dotsPerMeter)));
        phys.setAttribute("unitSpecifier", "meter");
        metadata.mergeTree(metaFormat, root);
    }

    private static void applyJpegDpi(IIOMetadata metadata, int dpi) throws Exception {
        String metaFormat = "javax_imageio_jpeg_image_1.0";
        if (!arrayContains(metadata.getMetadataFormatNames(), metaFormat)) {
            return;
        }
        Element root = (Element) metadata.getAsTree(metaFormat);
        NodeList app0List = root.getElementsByTagName("app0JFIF");
        Element app0;
        if (app0List.getLength() > 0) {
            app0 = (Element) app0List.item(0);
        } else {
            app0 = root.getOwnerDocument().createElement("app0JFIF");
            root.appendChild(app0);
        }
        app0.setAttribute("majorVersion", "1");
        app0.setAttribute("minorVersion", "2");
        app0.setAttribute("resUnits", "1"); // dots per inch
        app0.setAttribute("Xdensity", String.valueOf(dpi));
        app0.setAttribute("Ydensity", String.valueOf(dpi));
        app0.setAttribute("thumbWidth", "0");
        app0.setAttribute("thumbHeight", "0");
        metadata.mergeTree(metaFormat, root);
    }

    private static IIOMetadataNode getOrCreateChild(IIOMetadataNode parent, String name) {
        NodeList list = parent.getElementsByTagName(name);
        if (list.getLength() > 0) {
            return (IIOMetadataNode) list.item(0);
        }
        IIOMetadataNode child = new IIOMetadataNode(name);
        parent.appendChild(child);
        return child;
    }

    private static boolean arrayContains(String[] arr, String value) {
        if (arr == null) {
            return false;
        }
        for (String s : arr) {
            if (value.equals(s)) {
                return true;
            }
        }
        return false;
    }

    private static BufferedImage toRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            return src;
        }
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            g.drawImage(src, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    private static Spinner<Integer> intSpinner(int initial, int min, int max) {
        Spinner<Integer> spinner = new Spinner<>();
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initial));
        spinner.setEditable(true);
        spinner.setPrefWidth(120);
        return spinner;
    }

    private static File ensureExtension(File target, Format format) {
        String lower = target.getName().toLowerCase(Locale.ROOT);
        for (String g : format.glob) {
            String ext = g.startsWith("*.") ? g.substring(1) : g;
            if (lower.endsWith(ext.toLowerCase(Locale.ROOT))) {
                return target;
            }
        }
        return new File(target.getParentFile(), target.getName() + "." + format.extension);
    }
}
