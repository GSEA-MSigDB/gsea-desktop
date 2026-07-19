/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.heatmap;

import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Jaccard similarity legend matching Swing {@code GradientColorScheme} with
 * {@code setUseDoubleGradient(false)}: WHITE→GREEN bar, min/max labels only (no mid).
 */
public final class FxJaccardLegend {

    private FxJaccardLegend() {
    }

    public static VBox create(double preferredWidth) {
        // Swing GeneSetSimilarityPanel overwrites legend preferred width with cols×cellSize (no floor).
        double w = Math.max(1, preferredWidth);
        // Swing GradientColorScheme.Legend: bar height = 15.
        Canvas canvas = new Canvas(w, 15);
        paint(canvas.getGraphicsContext2D(), w, 15);

        Label min = new Label("0");
        Label max = new Label("1");
        HBox labels = new HBox();
        labels.setMaxWidth(Double.MAX_VALUE);
        labels.getChildren().addAll(min, max);
        HBox.setHgrow(min, Priority.ALWAYS);
        max.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        VBox box = new VBox(2, canvas, labels);
        box.setPadding(new Insets(4, 8, 4, 8));
        // Preferred width tracks cols×cellSize; allow SplitPane parents to shrink below that.
        box.setMinWidth(0);
        return box;
    }

    private static void paint(GraphicsContext g, double w, double h) {
        for (int x = 0; x < (int) w; x++) {
            float v = (float) (x / Math.max(1.0, w - 1));
            g.setFill(colorFor(v));
            g.fillRect(x, 0, 1, h);
        }
    }

    /** Swing single-gradient WHITE→GREEN across [0,1]. */
    static Color colorFor(float v) {
        if (v <= 0f) {
            return Color.WHITE;
        }
        if (v >= 1f) {
            return Color.LIME;
        }
        return blend(Color.WHITE, Color.LIME, v);
    }

    private static Color blend(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                a.getRed() + (b.getRed() - a.getRed()) * t,
                a.getGreen() + (b.getGreen() - a.getGreen()) * t,
                a.getBlue() + (b.getBlue() - a.getBlue()) * t,
                1.0);
    }
}
