/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

/**
 * Shared button chrome helpers so panes use one sizing / spacing vocabulary.
 */
public final class FxButtons {

    private FxButtons() {
    }

    public static void stylePrimary(Button b) {
        if (b == null) {
            return;
        }
        b.getStyleClass().removeAll("gsea-toolbar-button", "gsea-icon-button", "gsea-rail-button");
        if (!b.getStyleClass().contains("gsea-run-button")) {
            b.getStyleClass().add("gsea-run-button");
        }
    }

    public static void styleSecondary(Button b) {
        if (b == null) {
            return;
        }
        b.getStyleClass().removeAll("gsea-run-button", "gsea-toolbar-button", "gsea-icon-button", "gsea-rail-button");
        if (!b.getStyleClass().contains("gsea-button")) {
            b.getStyleClass().add("gsea-button");
        }
    }

    public static void styleToolbar(Button b) {
        if (b == null) {
            return;
        }
        b.getStyleClass().removeAll("gsea-run-button", "gsea-icon-button", "gsea-rail-button");
        if (!b.getStyleClass().contains("gsea-button")) {
            b.getStyleClass().add("gsea-button");
        }
        if (!b.getStyleClass().contains("gsea-toolbar-button")) {
            b.getStyleClass().add("gsea-toolbar-button");
        }
    }

    public static void styleIcon(Button b) {
        if (b == null) {
            return;
        }
        b.getStyleClass().removeAll("gsea-run-button", "gsea-toolbar-button", "gsea-rail-button");
        if (!b.getStyleClass().contains("gsea-button")) {
            b.getStyleClass().add("gsea-button");
        }
        if (!b.getStyleClass().contains("gsea-icon-button")) {
            b.getStyleClass().add("gsea-icon-button");
        }
    }

    public static void styleRail(Button b) {
        if (b == null) {
            return;
        }
        b.getStyleClass().removeAll("gsea-run-button", "gsea-toolbar-button", "gsea-icon-button", "gsea-button");
        if (!b.getStyleClass().contains("gsea-rail-button")) {
            b.getStyleClass().add("gsea-rail-button");
        }
    }

    /**
     * Let labeled buttons grow to fit text + graphic instead of clipping with {@code ...}.
     * Clears any fixed preferred width/height so CSS min-height still applies.
     */
    public static void sizeToContent(Button... buttons) {
        if (buttons == null) {
            return;
        }
        for (Button b : buttons) {
            if (b == null) {
                continue;
            }
            b.setMinWidth(Region.USE_PREF_SIZE);
            b.setPrefWidth(Region.USE_COMPUTED_SIZE);
            b.setMaxWidth(Region.USE_PREF_SIZE);
            b.setPrefHeight(Region.USE_COMPUTED_SIZE);
            b.setWrapText(false);
            b.setTextOverrun(OverrunStyle.CLIP);
        }
    }

    /** Standard horizontal action strip (8px gaps). */
    public static HBox row(Node... nodes) {
        HBox row = new HBox(nodes);
        row.getStyleClass().add("gsea-button-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** Help left + action cluster (tool launcher / dialog footers). */
    public static HBox actionBar(Node leading, HBox trailing) {
        HBox bar = new HBox(8, leading, trailing);
        bar.getStyleClass().add("gsea-action-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    public static void padTight(HBox row) {
        if (row != null) {
            row.setPadding(new Insets(4, 8, 4, 8));
        }
    }
}
