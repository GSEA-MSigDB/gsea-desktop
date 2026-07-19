/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.shell;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.Errors;
import edu.mit.broad.genome.StandardException;
import edu.mit.broad.genome.TraceUtils;
import edu.mit.broad.xbench.core.api.WindowManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.web.WebView;
import javafx.stage.Window;
import xapps.gsea.GseaWebResources;

/**
 * {@link WindowManager} backed by the JavaFX {@link GseaFxShell}.
 *
 * <p>Error alerts support an expandable stack-trace "Details" section, a "Copy" button to put
 * those details on the clipboard, and (when the underlying exception is a
 * {@link StandardException}) a "Help" button that opens the matching User Guide anchor.
 * Confirmation dialogs always run on the JavaFX Application Thread and block the calling thread
 * until the user responds. Message bodies that begin with {@code <html>} are rendered as HTML.</p>
 */
public class FxWorkspaceWindowManager implements WindowManager {

    private static final Logger klog = LoggerFactory.getLogger(FxWorkspaceWindowManager.class);
    private final GseaFxShell shell;

    public FxWorkspaceWindowManager(GseaFxShell shell) {
        this.shell = shell;
    }

    public GseaFxShell getShell() {
        return shell;
    }

    @Override
    public void showError(String msg) {
        presentErrorAlert(msg, msg, null, null);
    }

    @Override
    public void showError(Throwable t) {
        String msg = messageOf(t);
        presentErrorAlert(msg, msg, stackTraceOf(t), findStandardException(t));
    }

    @Override
    public void showError(Errors errors) {
        if (errors == null) {
            presentErrorAlert("Error", "Error", null, null);
            return;
        }
        Throwable[] throwables = errors.getErrors();
        String details = throwables.length > 0 ? TraceUtils.getAsString(throwables) : null;
        presentErrorAlert(errors.getName(), errors.getErrors(false), details, findStandardException(throwables));
    }

    @Override
    public void showError(String msg, Throwable t) {
        presentErrorAlert(msg, combineMessage(msg, t), stackTraceOf(t), findStandardException(t));
    }

    @Override
    public boolean showConfirm(String msg) {
        // Swing AbstractWindowManager.showConfirm(msg).
        return showConfirm("Please confirm this action", msg);
    }

    @Override
    public boolean showConfirm(String title, String msg) {
        return callOnFxThreadBlocking(() -> {
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle(title != null ? title : "Confirm");
            alert.setHeaderText(title);
            ButtonType okType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(okType, cancelType);
            applyContent(alert.getDialogPane(), msg);
            styleOwner(alert);
            Optional<ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get() == okType;
        });
    }

    @Override
    public void showMessage(String msg) {
        showMessage("Message", msg);
    }

    @Override
    public void showMessage(String title, String msg) {
        if (isHtml(msg)) {
            runOnFxThreadBlocking(() -> {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle(title != null ? title : "");
                alert.setHeaderText(null);
                alert.getButtonTypes().setAll(ButtonType.OK);
                applyContent(alert.getDialogPane(), msg);
                styleOwner(alert);
                alert.showAndWait();
                return null;
            });
            return;
        }
        xapps.gsea.fx.FxToast.show(title, msg);
    }

    // ------------------------------------------------------------------
    // Error alert construction
    // ------------------------------------------------------------------

    private void presentErrorAlert(String headerRaw, String bodyMessage, String detailsText, StandardException se) {
        runOnFxThreadBlocking(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(shortTitle(headerRaw));
            applyContent(alert.getDialogPane(), bodyMessage);

            List<ButtonType> buttonTypes = new ArrayList<>();
            buttonTypes.add(ButtonType.OK);

            String details = (detailsText != null && !detailsText.isEmpty())
                    ? detailsText
                    : (bodyMessage != null ? bodyMessage : "");
            TextArea detailArea = new TextArea(details.isEmpty() ? "No stack trace available." : details);
            detailArea.setEditable(false);
            detailArea.setWrapText(false);
            detailArea.setPrefSize(560, 280);
            alert.getDialogPane().setExpandableContent(detailArea);
            alert.getDialogPane().setExpanded(false);

            ButtonType copyType = new ButtonType("Copy", ButtonBar.ButtonData.LEFT);
            buttonTypes.add(copyType);

            ButtonType helpType = null;
            if (se != null) {
                helpType = new ButtonType("Help", ButtonBar.ButtonData.HELP_2);
                buttonTypes.add(helpType);
            }

            alert.getButtonTypes().setAll(buttonTypes);
            styleOwner(alert);

            Button copyButton = (Button) alert.getDialogPane().lookupButton(copyType);
            String toCopy = details.isEmpty() ? (bodyMessage != null ? bodyMessage : "") : details;
            copyButton.addEventFilter(ActionEvent.ACTION, e -> {
                ClipboardContent content = new ClipboardContent();
                content.putString(toCopy);
                Clipboard.getSystemClipboard().setContent(content);
                e.consume();
            });
            if (helpType != null) {
                Button helpButton = (Button) alert.getDialogPane().lookupButton(helpType);
                StandardException fse = se;
                helpButton.addEventFilter(ActionEvent.ACTION, e -> {
                    openHelpFor(fse);
                    e.consume();
                });
            }

            alert.showAndWait();
            return null;
        });
    }

    private void openHelpFor(StandardException se) {
        String url = GseaWebResources.getGseaHelpURL() + "GSEA/GSEA_User_Guide/#error-" + se.getErrorCode();
        try {
            xapps.gsea.fx.FxDesktopUtil.openUrl(url);
        } catch (Exception ex) {
            klog.warn("Could not open help url: {}", url, ex);
        }
    }

    private void styleOwner(Alert alert) {
        Window owner = shell != null ? shell.getStage() : null;
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setResizable(true);
        xapps.gsea.fx.FxTheme.apply(alert);
    }

    // ------------------------------------------------------------------
    // Content rendering (plain text vs. <html> bodies)
    // ------------------------------------------------------------------

    private static void applyContent(DialogPane pane, String message) {
        if (isHtml(message)) {
            WebView view = new WebView();
            view.getEngine().loadContent(message);
            view.setPrefSize(520, 280);
            view.setMaxHeight(320);
            pane.setContent(view);
            return;
        }
        String text = message == null ? "" : message;
        long lines = text.chars().filter(c -> c == '\n').count() + 1;
        if (text.length() > 360 || lines > 6) {
            // TextArea scrolls itself — do not wrap in another ScrollPane.
            TextArea area = new TextArea(text);
            area.setEditable(false);
            area.setWrapText(true);
            area.setPrefWidth(480);
            area.setPrefRowCount((int) Math.min(14, Math.max(6, lines)));
            area.setMaxHeight(280);
            pane.setContent(area);
        } else {
            Label label = new Label(text);
            label.setWrapText(true);
            label.setMaxWidth(480);
            pane.setContent(label);
        }
    }

    private static boolean isHtml(String s) {
        return s != null && s.trim().regionMatches(true, 0, "<html>", 0, 6);
    }

    // ------------------------------------------------------------------
    // Threading helper — showConfirm must always run on the FX thread and block for the result.
    // ------------------------------------------------------------------

    private static boolean callOnFxThreadBlocking(Callable<Boolean> callable) {
        try {
            Boolean result = runOnFxThreadBlocking(callable);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            klog.warn("Confirmation dialog failed", e);
            return false;
        }
    }

    private static <T> T runOnFxThreadBlocking(Callable<T> callable) {
        if (Platform.isFxApplicationThread()) {
            try {
                return callable.call();
            } catch (Exception e) {
                klog.warn("FX dialog failed", e);
                return null;
            }
        }
        FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);
        try {
            return task.get();
        } catch (Exception e) {
            klog.warn("FX dialog failed", e);
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Message / StandardException helpers
    // ------------------------------------------------------------------

    private static String messageOf(Throwable t) {
        if (t == null) {
            return "Error";
        }
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }

    private static String combineMessage(String msg, Throwable t) {
        if (isHtml(msg)) {
            return msg;
        }
        if (t == null || t.getMessage() == null) {
            return msg;
        }
        if (msg == null || msg.isEmpty()) {
            return t.getMessage();
        }
        if (msg.equals(t.getMessage())) {
            return msg;
        }
        return msg + "\n" + t.getMessage();
    }

    private static String stackTraceOf(Throwable t) {
        return t == null ? null : TraceUtils.getAsString(t);
    }

    private static String shortTitle(String raw) {
        if (raw == null || raw.isEmpty() || isHtml(raw)) {
            return "Error";
        }
        return raw.length() > 80 ? raw.substring(0, 80) + "…" : raw;
    }

    private static StandardException findStandardException(Throwable t) {
        Throwable cur = t;
        int guard = 0;
        while (cur != null && guard++ < 20) {
            if (cur instanceof StandardException) {
                return (StandardException) cur;
            }
            cur = cur.getCause();
        }
        return null;
    }

    private static StandardException findStandardException(Throwable[] throwables) {
        if (throwables == null) {
            return null;
        }
        for (Throwable t : throwables) {
            StandardException se = findStandardException(t);
            if (se != null) {
                return se;
            }
        }
        return null;
    }
}
