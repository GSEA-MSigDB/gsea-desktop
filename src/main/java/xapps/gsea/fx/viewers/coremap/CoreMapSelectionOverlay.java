/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.coremap;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import xapps.gsea.fx.FxButtons;

/**
 * Resizable, draggable selection hoverbox (CoreMap {@code SelectionHoverbox}).
 * Anchored bottom-right; resize grip is top-left (NW) so growth expands into the canvas.
 */
public final class CoreMapSelectionOverlay extends StackPane {

    private static final double WIDTH_MIN = 240;
    private static final double WIDTH_MAX = 720;
    private static final double HEIGHT_MIN = 140;
    private static final double HEIGHT_MAX = 720;

    private final VBox selectionContent = new VBox(6);
    private final ScrollPane selectionScroll = new ScrollPane(selectionContent);
    private final VBox panel = new VBox(6);
    private final Region resizeHandle = new Region();
    private Runnable onDismiss;

    private double dragStartX;
    private double dragStartY;
    private double startW;
    private double startH;
    private double moveStartX;
    private double moveStartY;
    private double startTranslateX;
    private double startTranslateY;
    private boolean resizing;

    public CoreMapSelectionOverlay() {
        Button dismiss = new Button("×");
        FxButtons.styleSecondary(dismiss);
        dismiss.setOnAction(e -> {
            if (onDismiss != null) {
                onDismiss.run();
            }
        });
        Label title = new Label("Selection");
        title.getStyleClass().add("coremap-selection-title");
        HBox header = new HBox(8, title, dismiss);
        HBox.setHgrow(title, Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setCursor(Cursor.MOVE);
        header.setOnMousePressed(this::onMovePressed);
        header.setOnMouseDragged(this::onMoveDragged);

        selectionScroll.setFitToWidth(true);
        selectionScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        selectionScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(selectionScroll, Priority.ALWAYS);

        panel.getChildren().setAll(header, selectionScroll);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(340);
        panel.setPrefHeight(280);
        panel.setMinWidth(WIDTH_MIN);
        panel.setMinHeight(HEIGHT_MIN);
        panel.setMaxWidth(WIDTH_MAX);
        panel.setMaxHeight(HEIGHT_MAX);
        panel.getStyleClass().add("coremap-selection-panel");

        resizeHandle.setPrefSize(16, 16);
        resizeHandle.setMinSize(16, 16);
        resizeHandle.setMaxSize(16, 16);
        resizeHandle.getStyleClass().add("coremap-resize-handle");
        resizeHandle.setCursor(Cursor.NW_RESIZE);
        resizeHandle.setFocusTraversable(false);
        resizeHandle.setOnMousePressed(this::onResizePressed);
        resizeHandle.setOnMouseDragged(this::onResizeDragged);
        resizeHandle.setOnMouseReleased(e -> {
            resizing = false;
            e.consume();
        });

        StackPane.setAlignment(panel, Pos.TOP_LEFT);
        StackPane.setAlignment(resizeHandle, Pos.TOP_LEFT);
        StackPane.setMargin(resizeHandle, new Insets(2, 0, 0, 2));
        getChildren().setAll(panel, resizeHandle);
        setVisible(false);
        setManaged(false);
        setPrefSize(340, 280);
        setMinSize(WIDTH_MIN, HEIGHT_MIN);
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        pickOnBoundsProperty().set(true);
    }

    public void setOnDismiss(Runnable onDismiss) {
        this.onDismiss = onDismiss;
    }

    public void setContent(javafx.scene.Node content) {
        selectionContent.getChildren().setAll(content);
    }

    public void clearContent() {
        selectionContent.getChildren().clear();
    }

    public void showOverlay(boolean show) {
        setVisible(show);
        setManaged(show);
        if (show) {
            toFront();
        }
    }

    private void onMovePressed(MouseEvent e) {
        if (resizing) {
            return;
        }
        moveStartX = e.getScreenX();
        moveStartY = e.getScreenY();
        startTranslateX = getTranslateX();
        startTranslateY = getTranslateY();
        e.consume();
    }

    private void onMoveDragged(MouseEvent e) {
        if (resizing) {
            return;
        }
        setTranslateX(startTranslateX + (e.getScreenX() - moveStartX));
        setTranslateY(startTranslateY + (e.getScreenY() - moveStartY));
        e.consume();
    }

    private void onResizePressed(MouseEvent e) {
        resizing = true;
        dragStartX = e.getScreenX();
        dragStartY = e.getScreenY();
        startW = panel.getWidth() > 0 ? panel.getWidth() : panel.getPrefWidth();
        startH = panel.getHeight() > 0 ? panel.getHeight() : panel.getPrefHeight();
        e.consume();
    }

    private void onResizeDragged(MouseEvent e) {
        // NW grip on a bottom-right-anchored panel: drag left/up grows (CoreMap parity).
        double nextW = clamp(startW + (dragStartX - e.getScreenX()), WIDTH_MIN, maxWidthNow());
        double nextH = clamp(startH + (dragStartY - e.getScreenY()), HEIGHT_MIN, maxHeightNow());
        applySize(nextW, nextH);
        e.consume();
    }

    private double maxWidthNow() {
        if (getParent() instanceof Region parent && parent.getWidth() > 0) {
            return Math.min(WIDTH_MAX, Math.max(WIDTH_MIN, parent.getWidth() - 24));
        }
        return WIDTH_MAX;
    }

    private double maxHeightNow() {
        if (getParent() instanceof Region parent && parent.getHeight() > 0) {
            return Math.min(HEIGHT_MAX, Math.max(HEIGHT_MIN, parent.getHeight() - 24));
        }
        return HEIGHT_MAX;
    }

    private void applySize(double w, double h) {
        panel.setPrefWidth(w);
        panel.setPrefHeight(h);
        setPrefWidth(w);
        setPrefHeight(h);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
