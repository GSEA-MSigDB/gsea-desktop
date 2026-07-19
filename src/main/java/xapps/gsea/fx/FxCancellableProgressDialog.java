/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx;

import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

/**
 * Shared modal progress + Cancel chrome (Swing {@code ProgressMonitor} stand-in).
 */
public final class FxCancellableProgressDialog {

    private final Dialog<Void> dialog = new Dialog<>();
    private final ProgressBar bar = new ProgressBar(0);
    private final Label note = new Label();
    private final Runnable onCancel;
    private volatile boolean closed;

    public FxCancellableProgressDialog(String title, String initialNote, Window owner, Runnable onCancel) {
        this.onCancel = onCancel;
        // Blank titles break Windows FindWindow dark-frame matching.
        dialog.setTitle(title != null && !title.isBlank() ? title : "Progress");
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        bar.setMaxWidth(Double.MAX_VALUE);
        note.setText(initialNote != null ? initialNote : "");
        note.setWrapText(true);
        VBox box = new VBox(10, note, bar);
        box.setPadding(new Insets(16));
        box.setPrefWidth(420);
        dialog.getDialogPane().setContent(box);
        FxTheme.apply(dialog);

        Button cancel = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancel.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            fireCancel();
            e.consume();
            close();
        });
        dialog.setOnCloseRequest(e -> fireCancel());
    }

    public static FxCancellableProgressDialog withCancelFlag(
            String title, String initialNote, Window owner, AtomicBoolean cancelled) {
        return new FxCancellableProgressDialog(title, initialNote, owner, () -> {
            if (cancelled != null) {
                cancelled.set(true);
            }
        });
    }

    private void fireCancel() {
        if (onCancel != null) {
            onCancel.run();
        }
    }

    public void show() {
        Platform.runLater(() -> {
            if (!closed && !dialog.isShowing()) {
                dialog.show();
            }
        });
    }

    /**
     * @param percent 0–100, or negative to leave the bar unchanged
     */
    public void setProgress(int percent, String noteText) {
        Platform.runLater(() -> {
            if (closed) {
                return;
            }
            if (percent >= 0) {
                bar.setProgress(Math.max(0, Math.min(100, percent)) / 100.0);
            }
            if (noteText != null) {
                note.setText(noteText);
            }
        });
    }

    public void setFraction(double fraction) {
        Platform.runLater(() -> {
            if (!closed) {
                bar.setProgress(Math.max(0, Math.min(1.0, fraction)));
            }
        });
    }

    public void close() {
        closed = true;
        Platform.runLater(() -> {
            if (dialog.isShowing()) {
                dialog.close();
            }
        });
    }

    public static Window firstShowingOwner() {
        return Window.getWindows().stream().filter(Window::isShowing).findFirst().orElse(null);
    }
}
