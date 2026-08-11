/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.alg.gsea.GeneSetScoringTable;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResult;
import edu.mit.broad.genome.reports.EnrichmentEsProfiles;
import edu.mit.broad.xbench.core.api.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Window;
import netscape.javascript.JSObject;

/**
 * JavaFX host for the bundled interactive EnPlot v2 viewer ({@code xapps/gsea/fx/enplot/}).
 * <p>
 * Loads {@code index.html?inject=1} from a {@code file:} URL, then delivers plot JSON through
 * {@code window.__enplotHostInject}. Lifecycle matches {@link xapps.gsea.fx.viewers.coremap.CoreMapGraphView}.
 */
public final class EnplotWebView extends BorderPane {

    private static final Logger klog = LoggerFactory.getLogger(EnplotWebView.class);
    private static final int INJECT_CHUNK = 120_000;
    private static final int MIN_VIEWER_HEIGHT = 480;

    private final WebView webView = new WebView();
    private final WebEngine engine = webView.getEngine();
    private boolean pageReady;
    private String pendingPayload;
    private String viewerUrl;

    EnplotWebView() {
        webView.setMinSize(0, 0);
        webView.setPrefHeight(MIN_VIEWER_HEIGHT);
        webView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        webView.setContextMenuEnabled(false);
        setCenter(webView);
        setMinSize(0, 0);
        setPrefHeight(MIN_VIEWER_HEIGHT);

        engine.setJavaScriptEnabled(true);
        engine.setOnError(event -> klog.warn("EnPlot WebView error: {}", event.getMessage()));
        engine.getLoadWorker().exceptionProperty().addListener((obs, old, ex) -> {
            if (ex != null) {
                klog.warn("EnPlot WebView load failed", ex);
            }
        });
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                pageReady = true;
                injectPending();
            }
        });

        viewerUrl = EnplotSupport.viewerPageUrl();
        if (viewerUrl == null) {
            klog.error("EnPlot viewer page URL could not be resolved");
            return;
        }
        engine.load(viewerUrl);
    }

    /** Queue or inject plot JSON (call from FX thread). */
    public void loadPayload(String jsonText) {
        if (jsonText == null || jsonText.isBlank()) {
            return;
        }
        pendingPayload = jsonText;
        injectPending();
    }

    /**
     * Build a pane with loading placeholder, async disk read, FX-thread EDB build, and save bar.
     */
    public static BorderPane createInteractivePane(File reportDir, EnrichmentResult result,
            String classA, String classB, EnrichmentEsProfiles.ScoringResolution scoring) {
        return createInteractivePane(reportDir, result, classA, classB, scoring, null);
    }

    public static BorderPane createInteractivePane(File reportDir, EnrichmentResult result,
            String classA, String classB, EnrichmentEsProfiles.ScoringResolution scoring,
            String metricName) {
        if (result == null || result.getRankedList() == null || result.getScore() == null) {
            BorderPane err = new BorderPane();
            err.setCenter(ReportExplorerSupport.messagePane("Interactive EnPlot v2 unavailable."));
            return err;
        }
        String geneSetName = result.getGeneSet().getName(true);
        GeneSetScoringTable scoringTable = scoring != null ? scoring.table() : null;
        int listSize = result.getRankedList().getSize();
        File jsonFile = EnplotSupport.jsonFile(reportDir, geneSetName);

        BorderPane host = new BorderPane();
        host.setMinSize(0, 0);
        host.setPrefHeight(MIN_VIEWER_HEIGHT);
        ReportExplorerSupport.loadAsync(host,
                "gsea-enplot-interactive",
                "Loading interactive EnPlot v2…",
                () -> EnplotSupport.readCachedJson(jsonFile, listSize),
                cachedJson -> {
                    String json = cachedJson;
                    if (json == null) {
                        json = EnplotSupport.buildFromResult(result, classA, classB, scoringTable, metricName);
                    }
                    if (json == null || json.isBlank()) {
                        return ReportExplorerSupport.messagePane("Interactive EnPlot v2 unavailable.");
                    }
                    EnplotWebView view = new EnplotWebView();
                    Node pane = withSaveBar(view, geneSetName);
                    view.loadPayload(json);
                    return pane;
                },
                "Interactive EnPlot v2 unavailable");
        return host;
    }

    /** Save control overlaid on the plot — same placement as static EnPlot views. */
    private static Node withSaveBar(EnplotWebView view, String geneSetName) {
        Button save = new Button("Save…");
        xapps.gsea.fx.widgets.FxButtons.styleSecondary(save);
        xapps.gsea.fx.widgets.FxButtons.sizeToContent(save);
        save.setOnAction(e -> view.saveSnapshot(geneSetName));

        StackPane stack = new StackPane(view, save);
        stack.setMinSize(0, 0);
        stack.setPrefHeight(MIN_VIEWER_HEIGHT);
        StackPane.setAlignment(save, Pos.TOP_RIGHT);
        // Clear of typical ScrollPane / WebView scrollbar gutter.
        StackPane.setMargin(save, new Insets(6, 22, 0, 0));
        return stack;
    }

    public void saveSnapshot(String geneSetName) {
        try {
            Object result = engine.executeScript(
                    "(function(){"
                            + "if(typeof window.__enplotCapturePng==='function'){"
                            + "  return window.__enplotCapturePng();"
                            + "}"
                            + "var c=document.getElementById('enplot-canvas');"
                            + "if(!c) return null;"
                            + "return {dataUrl:c.toDataURL('image/png'),"
                            + "width:c.clientWidth||c.width,height:c.clientHeight||c.height};"
                            + "})()");
            Capture capture = parseCapture(result);
            if (capture == null) {
                Application.getWindowManager().showMessage("Could not capture the interactive plot. "
                        + "Wait for it to finish loading, then try again.");
                return;
            }
            byte[] bytes = Base64.getDecoder().decode(capture.base64());
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                Application.getWindowManager().showMessage("Could not decode the interactive plot image.");
                return;
            }
            Window owner = getScene() != null ? getScene().getWindow() : null;
            String safe = geneSetName == null ? "enplot_v2"
                    : geneSetName.replaceAll("[^A-Za-z0-9._-]+", "_");
            int defW = capture.width() > 1 ? capture.width() : image.getWidth();
            int defH = capture.height() > 1 ? capture.height() : image.getHeight();
            EnplotExportDialog.saveImage(owner, image, safe + "_enplot_v2", defW, defH);
        } catch (Throwable t) {
            Application.getWindowManager().showError("Save plot failed", t);
        }
    }

    private record Capture(String base64, int width, int height) {
    }

    private static Capture parseCapture(Object result) {
        if (result == null) {
            return null;
        }
        String dataUrl = null;
        int width = 0;
        int height = 0;
        if (result instanceof String s) {
            dataUrl = s;
        } else if (result instanceof JSObject js) {
            Object urlObj = js.getMember("dataUrl");
            if (urlObj instanceof String s) {
                dataUrl = s;
            }
            width = toInt(js.getMember("width"));
            height = toInt(js.getMember("height"));
        }
        if (dataUrl == null || !dataUrl.startsWith("data:image/png;base64,")) {
            return null;
        }
        String b64 = dataUrl.substring("data:image/png;base64,".length());
        return new Capture(b64, Math.max(0, width), Math.max(0, height));
    }

    private static int toInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private void injectPending() {
        if (!pageReady || pendingPayload == null || pendingPayload.isBlank()) {
            return;
        }
        scheduleInject();
    }

    private void scheduleInject() {
        final String json = pendingPayload;
        Runnable attempt = new Runnable() {
            int tries;

            @Override
            public void run() {
                if (tries++ > 60) {
                    klog.warn("EnPlot host inject gave up after {} attempts (url={})",
                            tries - 1, engine.getLocation());
                    showInjectError("Timed out loading the interactive plot viewer.");
                    return;
                }
                try {
                    Object hostReady = engine.executeScript(
                            "typeof window.__enplotHostInject==='function'");
                    if (!Boolean.TRUE.equals(hostReady)) {
                        Platform.runLater(this);
                        return;
                    }
                    injectPayload(json);
                    Object ok = engine.executeScript(
                            "!!document.getElementById('enplot-canvas')");
                    if (Boolean.TRUE.equals(ok)) {
                        return;
                    }
                } catch (Throwable t) {
                    klog.debug("EnPlot host inject attempt {} pending", tries, t);
                }
                Platform.runLater(this);
            }
        };
        attempt.run();
    }

    private void showInjectError(String message) {
        try {
            engine.executeScript(
                    "var a=document.getElementById('app');"
                            + "if(a){a.innerHTML='<div class=\"enplot-error\">"
                            + message.replace("'", "\\'")
                            + "</div>';}");
        } catch (Throwable ignored) {
            // best effort
        }
    }

    private void injectPayload(String json) {
        if (json.length() <= INJECT_CHUNK) {
            engine.executeScript(
                    "window.__enplotHostInject(" + JSONValue.toJSONString(json) + ");");
            return;
        }
        engine.executeScript("window.__enplotPayload = '';");
        for (int i = 0; i < json.length(); i += INJECT_CHUNK) {
            String part = json.substring(i, Math.min(json.length(), i + INJECT_CHUNK));
            engine.executeScript("window.__enplotPayload += " + JSONValue.toJSONString(part) + ";");
        }
        engine.executeScript(
                "window.__enplotHostInject(window.__enplotPayload);"
                        + "window.__enplotPayload=null;");
    }
}
