/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.heatmap;

import java.text.NumberFormat;
import java.util.Locale;

import org.genepattern.heatmap.RowColorScheme;

import edu.mit.broad.genome.math.ColorSchemes;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * FX legends, binary LE membership,
 * and {@code LegendPanel} (RowColorScheme Relative/Absolute).
 */
public final class FxHeatMapLegend {

    private FxHeatMapLegend() {
    }

    public static Node broadCancer() {
        ColorSchemes.BroadCancer scheme = new ColorSchemes.BroadCancer();
        java.awt.Color[] awt = new java.awt.Color[scheme.getNumColors()];
        String[] labels = new String[scheme.getNumColors()];
        for (int i = 0; i < scheme.getNumColors(); i++) {
            awt[i] = scheme.getColor(i);
            labels[i] = scheme.getValue(i);
        }
        return discrete(awt, labels, null);
    }

    /** White / yellow membership legend. */
    public static Node binaryMembership() {
        return discrete(
                new java.awt.Color[] { java.awt.Color.WHITE, java.awt.Color.YELLOW },
                new String[] { "Absent", "Present" },
                "Membership");
    }

    public static Node rowColorScheme(RowColorScheme scheme, boolean globalScale) {
        if (scheme == null) {
            return new Label("No legend available.");
        }
        java.awt.Color[] colors = scheme.getColorMap();
        String[] labels;
        String title;
        NumberFormat fmt = NumberFormat.getInstance(Locale.ROOT);
        if (!globalScale) {
            title = "Normalized Expression";
            fmt.setMaximumFractionDigits(1);
            fmt.setMinimumFractionDigits(1);
            double[] values = new double[scheme.getColorCount()];
            scheme.calculateSlots(-3, 3, 0, values);
            labels = new String[values.length + 1];
            labels[0] = fmt.format(-3);
            for (int i = 0; i < values.length; i++) {
                labels[i + 1] = fmt.format(values[i]);
            }
        } else {
            title = "";
            fmt.setMaximumFractionDigits(0);
            fmt.setMinimumFractionDigits(0);
            double[] slots = scheme.getSlots();
            labels = new String[slots.length];
            for (int i = 0; i < slots.length; i++) {
                labels[i] = fmt.format(slots[i]);
            }
        }
        return discrete(colors, labels, title);
    }

    public static Node discrete(java.awt.Color[] colors, String[] labels, String title) {
        if (colors == null || colors.length == 0) {
            return new Label("No legend available.");
        }
        HBox swatches = new HBox(0);
        swatches.setAlignment(Pos.CENTER_LEFT);
        int n = colors.length;
        for (int i = 0; i < n; i++) {
            VBox cell = new VBox(2);
            cell.setAlignment(Pos.TOP_CENTER);
            Rectangle rect = new Rectangle(Math.max(28, 560.0 / n), 18);
            rect.setFill(toFx(colors[i]));
            rect.setStroke(Color.BLACK);
            rect.setStrokeWidth(1);
            String tip = (labels != null && i < labels.length) ? labels[i] : "";
            if (!tip.isBlank()) {
                Tooltip.install(rect, new Tooltip(tip));
            }
            Label lab = new Label(tip);
            lab.setStyle("-fx-font-size: 10px;");
            lab.setMaxWidth(rect.getWidth());
            lab.setAlignment(Pos.CENTER);
            cell.getChildren().addAll(rect, lab);
            HBox.setHgrow(cell, Priority.ALWAYS);
            swatches.getChildren().add(cell);
        }
        if (labels != null && labels.length > n) {
            Label extra = new Label(labels[labels.length - 1]);
            extra.setStyle("-fx-font-size: 10px;");
            extra.setPadding(new Insets(20, 0, 0, 4));
            swatches.getChildren().add(extra);
        }
        VBox box = new VBox(6);
        box.setPadding(new Insets(12));
        box.getStyleClass().add("gsea-legend-box");
        if (title != null && !title.isBlank()) {
            Label t = new Label(title);
            t.getStyleClass().add("gsea-section-header");
            box.getChildren().add(t);
        }
        box.getChildren().add(swatches);
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        return box;
    }

    private static Color toFx(java.awt.Color c) {
        if (c == null) {
            return Color.GRAY;
        }
        return Color.rgb(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() / 255.0);
    }
}
