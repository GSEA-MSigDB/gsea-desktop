/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.widgets;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/** Non-blocking toast overlay for informational messages. */
public final class FxToast {

    private static final Duration SHOW_FOR = Duration.seconds(3.5);
    private static volatile StackPane host;

    private FxToast() {
    }

    public static void setHost(StackPane overlayHost) {
        host = overlayHost;
    }

    public static void show(String message) {
        show(null, message);
    }

    public static void show(String title, String message) {
        String text = compose(title, message);
        if (text.isEmpty()) {
            return;
        }
        if (Platform.isFxApplicationThread()) {
            present(text);
        } else {
            Platform.runLater(() -> present(text));
        }
    }

    private static String compose(String title, String message) {
        String body = message != null ? message.trim() : "";
        String t = title != null ? title.trim() : "";
        if (t.isEmpty() || "Message".equalsIgnoreCase(t) || body.regionMatches(true, 0, t, 0, t.length())) {
            return body;
        }
        return body.isEmpty() ? t : t + "\n" + body;
    }

    private static void present(String text) {
        StackPane overlay = host;
        if (overlay == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }
        overlay.getChildren().removeIf(n -> n.getStyleClass().contains("gsea-toast"));

        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(420);
        label.getStyleClass().addAll("gsea-toast", "gsea-toast-label");
        label.setPadding(new Insets(12, 16, 12, 16));
        // StackPane stretches children to fill unless max size is capped to pref.
        label.setMaxSize(460, javafx.scene.layout.Region.USE_PREF_SIZE);
        label.setMouseTransparent(true);
        label.setOpacity(0);
        StackPane.setAlignment(label, Pos.BOTTOM_CENTER);
        StackPane.setMargin(label, new Insets(0, 16, 48, 16));
        overlay.getChildren().add(label);

        FadeTransition in = new FadeTransition(Duration.millis(150), label);
        in.setToValue(1);
        in.play();

        PauseTransition hold = new PauseTransition(SHOW_FOR);
        hold.setOnFinished(e -> {
            FadeTransition out = new FadeTransition(Duration.millis(180), label);
            out.setToValue(0);
            out.setOnFinished(ev -> overlay.getChildren().remove(label));
            out.play();
        });
        hold.play();
    }
}
