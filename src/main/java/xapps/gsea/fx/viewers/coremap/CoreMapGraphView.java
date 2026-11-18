/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.coremap;

import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.coremap.CoreMapJson;
import edu.mit.broad.coremap.CoreMapTypes.IntegrationResult;
import edu.mit.broad.coremap.CoreMapTypes.InteractomeSource;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.json.simple.JSONObject;
import xapps.gsea.fx.viewers.coremap.CoreMapGraphViews.GeneVisibility;
import xapps.gsea.fx.viewers.coremap.CoreMapGraphViews.ViewElements;
import xapps.gsea.fx.viewers.coremap.CoreMapGraphViews.ViewMode;

/** WebView host for bundled Cytoscape.js CoreMap graph. */
public final class CoreMapGraphView extends BorderPane {

    private static final Logger klog = LoggerFactory.getLogger(CoreMapGraphView.class);

    private final WebView webView = new WebView();
    private final WebEngine engine = webView.getEngine();
    private boolean ready;
    private String pendingElementsJson;
    private String pendingLayoutOpts;
    private final AtomicInteger loadGeneration = new AtomicInteger();
    private List<String> pendingFocusGenes;
    private Set<String> pendingFocusEdgePairs;
    private boolean pendingFocusNeighborhood;
    private int pendingFocusGeneration = -1;
    private Consumer<String> nodeSelectedHandler;
    private BiConsumer<String, Boolean> nodeSelectedAdditiveHandler;
    private Consumer<String> edgeSelectedHandler;
    private Runnable backgroundTapHandler;

    private static final String JAVA_ALERT_PREFIX = "gsea-java:";

    public CoreMapGraphView() {
        setCenter(webView);
        webView.setContextMenuEnabled(false);
        engine.setOnAlert(event -> dispatchJavaAlert(event.getData()));
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                ready = true;
                if (pendingElementsJson != null) {
                    loadElementsJson(pendingElementsJson,
                            pendingLayoutOpts != null ? pendingLayoutOpts : "{}");
                    pendingElementsJson = null;
                    pendingLayoutOpts = null;
                }
            }
        });
        URL url = CoreMapGraphView.class.getResource("/xapps/gsea/fx/coremap/graph/index.html");
        if (url != null) {
            engine.load(url.toExternalForm());
        } else {
            klog.error("CoreMap graph index.html not found on classpath");
        }
    }

    private void dispatchJavaAlert(String data) {
        if (data == null || !data.startsWith(JAVA_ALERT_PREFIX)) {
            return;
        }
        Object parsed = JSONValue.parse(data.substring(JAVA_ALERT_PREFIX.length()));
        if (!(parsed instanceof JSONObject message)) {
            return;
        }
        String method = String.valueOf(message.get("method"));
        JSONArray args = message.get("args") instanceof JSONArray array ? array : new JSONArray();
        JavaBridge bridge = new JavaBridge();
        switch (method) {
            case "onNodeSelected" -> bridge.onNodeSelected(arg(args, 0), arg(args, 1));
            case "onEdgeSelected" -> bridge.onEdgeSelected(arg(args, 0));
            case "onBackgroundTap" -> bridge.onBackgroundTap();
            case "onLayoutComplete" -> bridge.onLayoutComplete();
            default -> klog.debug("Ignored CoreMap page message {}", method);
        }
    }

    private static String arg(JSONArray args, int index) {
        if (index >= args.size() || args.get(index) == null) {
            return "";
        }
        return String.valueOf(args.get(index));
    }

    public void setNodeSelectedHandler(Consumer<String> handler) {
        this.nodeSelectedHandler = handler;
    }

    /** Prefer this when Ctrl/Cmd multi-select is supported. */
    public void setNodeSelectedAdditiveHandler(BiConsumer<String, Boolean> handler) {
        this.nodeSelectedAdditiveHandler = handler;
    }

    public void setEdgeSelectedHandler(Consumer<String> edgeDataJsonHandler) {
        this.edgeSelectedHandler = edgeDataJsonHandler;
    }

    public void setBackgroundTapHandler(Runnable handler) {
        this.backgroundTapHandler = handler;
    }

    public void showResult(IntegrationResult result, ViewMode mode, InteractomeSource source,
            GeneVisibility geneVisibility) {
        showResult(result, mode, source, geneVisibility, null, null);
    }

    /**
     * Build/inject the graph; if {@code focusGenes} is non-null, focus after load completes
     * (avoids racing async inject).
     */
    public void showResult(IntegrationResult result, ViewMode mode, InteractomeSource source,
            GeneVisibility geneVisibility, List<String> focusGenes, Set<String> focusEdgePairs) {
        final int gen = loadGeneration.incrementAndGet();
        if (focusGenes != null && !focusGenes.isEmpty()) {
            pendingFocusGenes = List.copyOf(focusGenes);
            pendingFocusEdgePairs = focusEdgePairs != null ? Set.copyOf(focusEdgePairs) : null;
            // null edgePairs ⇒ neighborhood (hubs/drivers); non-null ⇒ cascade path mode
            pendingFocusNeighborhood = focusEdgePairs == null;
            pendingFocusGeneration = gen;
        } else {
            pendingFocusGenes = null;
            pendingFocusEdgePairs = null;
            pendingFocusNeighborhood = false;
            pendingFocusGeneration = -1;
        }
        Thread t = new Thread(() -> {
            try {
                ViewElements view = CoreMapGraphViews.buildViewElements(result, mode, source, geneVisibility);
                // Slim paint-only JSON for WebView (full export JSON stays separate).
                String json = CoreMapJson.webViewElementsJson(view.nodes, view.edges);
                String layoutOpts = layoutOptsJson(mode);
                Platform.runLater(() -> {
                    if (gen != loadGeneration.get()) {
                        return;
                    }
                    if (!ready) {
                        pendingElementsJson = json;
                        pendingLayoutOpts = layoutOpts;
                        return;
                    }
                    loadElementsJson(json, layoutOpts);
                });
            } catch (Throwable ex) {
                klog.error("Failed to build CoreMap graph JSON", ex);
            }
        }, "coremap-graph-json");
        t.setDaemon(true);
        t.start();
    }

    /** Clear the graph display (e.g. after layer Clear). */
    public void clear() {
        loadGeneration.incrementAndGet();
        pendingElementsJson = null;
        pendingLayoutOpts = null;
        pendingFocusGenes = null;
        pendingFocusEdgePairs = null;
        pendingFocusNeighborhood = false;
        pendingFocusGeneration = -1;
        if (!ready) {
            return;
        }
        loadElementsJson("{\"nodes\":[],\"edges\":[]}", "{}");
    }

    private void loadElementsJson(String json, String layoutOpts) {
        try {
            String opts = layoutOpts != null ? layoutOpts : "{}";
            // Small executeScript batches. One script that both carries a multi-MB payload
            // and runs layout is what WebKit drops.
            if (!loadGraphBatched(json, opts)) {
                injectJsonPayload(json);
                engine.executeScript(
                        "window.coreMapBridge.loadGraph(window.__coreMapPayload, "
                                + JSONValue.toJSONString(opts) + ");"
                                + "window.__coreMapPayload = null;");
            }
        } catch (Throwable t) {
            klog.error("Failed to load CoreMap graph", t);
        }
    }

    /**
     * Add nodes and edges in small scripts, then finish. Layout stays deferred in JS.
     * @return false if the payload could not be parsed for batching
     */
    private boolean loadGraphBatched(String json, String opts) {
        try {
            Object parsed = new org.json.simple.parser.JSONParser().parse(json);
            if (!(parsed instanceof org.json.simple.JSONObject root)) {
                return false;
            }
            org.json.simple.JSONArray nodes = root.get("nodes") instanceof org.json.simple.JSONArray a
                    ? a : new org.json.simple.JSONArray();
            org.json.simple.JSONArray edges = root.get("edges") instanceof org.json.simple.JSONArray a
                    ? a : new org.json.simple.JSONArray();
            engine.executeScript("window.coreMapBridge.beginGraphLoad()");
            final int batch = 40;
            for (int i = 0; i < nodes.size(); i += batch) {
                engine.executeScript("window.coreMapBridge.addGraphElements("
                        + JSONValue.toJSONString(elementChunk(nodes, i, batch, true)) + ")");
            }
            for (int i = 0; i < edges.size(); i += batch) {
                engine.executeScript("window.coreMapBridge.addGraphElements("
                        + JSONValue.toJSONString(elementChunk(edges, i, batch, false)) + ")");
            }
            engine.executeScript("window.coreMapBridge.finishGraphLoad(" + JSONValue.toJSONString(opts) + ")");
            return true;
        } catch (Throwable t) {
            klog.warn("Batched graph inject failed", t);
            return false;
        }
    }

    private static String elementChunk(org.json.simple.JSONArray items, int start, int batch, boolean nodes) {
        JSONObject chunk = new JSONObject();
        org.json.simple.JSONArray slice = new org.json.simple.JSONArray();
        for (int j = start; j < Math.min(items.size(), start + batch); j++) {
            slice.add(items.get(j));
        }
        chunk.put("nodes", nodes ? slice : new org.json.simple.JSONArray());
        chunk.put("edges", nodes ? new org.json.simple.JSONArray() : slice);
        return chunk.toJSONString();
    }

    /** Build a JS string in chunks so large graph JSON never sits in one executeScript source. */
    private void injectJsonPayload(String json) {
        engine.executeScript("window.__coreMapPayload = '';");
        final int chunk = 120_000;
        for (int i = 0; i < json.length(); i += chunk) {
            String part = json.substring(i, Math.min(json.length(), i + chunk));
            engine.executeScript("window.__coreMapPayload += " + JSONValue.toJSONString(part) + ";");
        }
    }

    private void applyPendingFocus(int gen) {
        if (pendingFocusGeneration != gen || pendingFocusGenes == null || pendingFocusGenes.isEmpty()) {
            return;
        }
        List<String> genes = pendingFocusGenes;
        Set<String> pairs = pendingFocusEdgePairs;
        boolean neighborhood = pendingFocusNeighborhood;
        pendingFocusGenes = null;
        pendingFocusEdgePairs = null;
        pendingFocusNeighborhood = false;
        pendingFocusGeneration = -1;
        if (neighborhood) {
            focusNeighborhood(genes);
        } else {
            focusPath(genes, pairs);
        }
    }

    private static String layoutOptsJson(ViewMode mode) {
        // Match CoreMap networkGraphCy ideal lengths / repulsion by mode.
        return switch (mode) {
            case SETS -> "{\"idealEdgeLength\":140,\"nodeRepulsion\":8000,\"randomize\":true}";
            case GENES -> "{\"idealEdgeLength\":95,\"nodeRepulsion\":6500,\"randomize\":true}";
            default -> "{\"idealEdgeLength\":110,\"nodeRepulsion\":7500,\"randomize\":true}";
        };
    }

    public boolean focusGenes(List<String> geneIds) {
        return focusNeighborhood(geneIds);
    }

    /**
     * Highlight genes and their closed neighborhood (sidebar hubs/drivers/search).
     * Does not change zoom/pan.
     */
    public boolean focusNeighborhood(List<String> geneIds) {
        if (!ready || geneIds == null || geneIds.isEmpty()) {
            return false;
        }
        try {
            Object r = engine.executeScript("window.coreMapBridge.focusNeighborhood("
                    + JSONValue.toJSONString(toJsonArray(geneIds)) + ")");
            return asBoolean(r);
        } catch (Throwable t) {
            klog.warn("focusNeighborhood failed", t);
            return false;
        }
    }

    /** @return true if at least one requested node was present in the current graph */
    public boolean focusPath(List<String> geneIds, Set<String> edgePairs) {
        if (!ready || geneIds == null || geneIds.isEmpty()) {
            return false;
        }
        try {
            Object r = engine.executeScript("window.coreMapBridge.focusPath("
                    + JSONValue.toJSONString(toJsonArray(geneIds)) + ", "
                    + JSONValue.toJSONString(toJsonArray(edgePairs)) + ")");
            return asBoolean(r);
        } catch (Throwable t) {
            klog.warn("focusPath failed", t);
            return false;
        }
    }

    public boolean findAndFocus(String query) {
        return findAndFocusId(query) != null;
    }

    /** Ranked find; returns matched node id or null. */
    public String findAndFocusId(String query) {
        if (!ready || query == null || query.isBlank()) {
            return null;
        }
        try {
            Object r = engine.executeScript("window.coreMapBridge.findAndFocus("
                    + JSONValue.toJSONString(query.trim()) + ")");
            if (r == null || r instanceof Boolean b && !b) {
                return null;
            }
            String id = r.toString();
            return id.isBlank() || "false".equalsIgnoreCase(id) ? null : id;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String toJsString(String s) {
        return JSONValue.toJSONString(s != null ? s : "");
    }

    public boolean focusEdge(String edgeId) {
        if (!ready || edgeId == null || edgeId.isBlank()) {
            return false;
        }
        try {
            Object r = engine.executeScript(
                    "window.coreMapBridge.focusEdge(" + toJsString(edgeId) + ")");
            return asBoolean(r);
        } catch (Throwable t) {
            return false;
        }
    }

    public void fit() {
        if (!ready) {
            return;
        }
        try {
            engine.executeScript("window.coreMapBridge.fit()");
        } catch (Throwable ignored) {
        }
    }

    public void zoomBy(double factor) {
        if (!ready) {
            return;
        }
        try {
            engine.executeScript("window.coreMapBridge.zoomBy(" + factor + ")");
        } catch (Throwable ignored) {
        }
    }

    public void reflow() {
        if (!ready) {
            return;
        }
        try {
            engine.executeScript("window.coreMapBridge.reflow()");
        } catch (Throwable ignored) {
        }
    }

    public void clearFocus() {
        if (!ready) {
            return;
        }
        try {
            engine.executeScript("window.coreMapBridge.clearFocus()");
        } catch (Throwable ignored) {
        }
    }

    public String exportPngDataUri() {
        return exportPngDataUri("view");
    }

    /** PNG of current view ("view") or highlighted selection ("focus"). */
    public String exportPngDataUri(String scope) {
        if (!ready) {
            return null;
        }
        try {
            String s = scope != null ? scope : "view";
            Object r = engine.executeScript(
                    "window.coreMapBridge.exportPng(" + toJsString(s) + ")");
            return r != null ? r.toString() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public String exportSelectedPngDataUri() {
        return exportPngDataUri("focus");
    }

    /** Current Cytoscape view JSON (full canvas for the active view mode). */
    public String exportCurrentJson() {
        return callBridgeString("exportCurrentJson");
    }

    /** Highlighted selection subgraph JSON; may be empty nodes/edges. */
    public String exportSelectedJson() {
        return callBridgeString("exportSelectedJson");
    }

    public boolean hasSelection() {
        if (!ready) {
            return false;
        }
        try {
            Object r = engine.executeScript("window.coreMapBridge.hasSelection()");
            return r instanceof Boolean && (Boolean) r
                    || (r != null && Boolean.parseBoolean(r.toString()));
        } catch (Throwable t) {
            return false;
        }
    }

    private String callBridgeString(String method) {
        if (!ready) {
            return null;
        }
        try {
            Object r = engine.executeScript("window.coreMapBridge." + method + "()");
            return r != null ? r.toString() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String toJsonArray(List<String> ids) {
        JSONArray arr = new JSONArray();
        if (ids != null) {
            for (String id : ids) {
                if (id != null) {
                    arr.add(id);
                }
            }
        }
        return arr.toJSONString();
    }

    private static String toJsonArray(Set<String> ids) {
        JSONArray arr = new JSONArray();
        if (ids != null) {
            for (String id : ids) {
                if (id != null) {
                    arr.add(id);
                }
            }
        }
        return arr.toJSONString();
    }

    private static boolean asBoolean(Object r) {
        return r instanceof Boolean b ? b : r != null && Boolean.parseBoolean(r.toString());
    }

    public final class JavaBridge {
        /**
         * WebKit LiveConnect is unreliable with boolean overloads — JS always passes
         * additive as the string {@code "true"} / {@code "false"}.
         */
        public void onNodeSelected(String id, String additiveFlag) {
            boolean additive = additiveFlag != null
                    && ("true".equalsIgnoreCase(additiveFlag) || "1".equals(additiveFlag));
            if (nodeSelectedAdditiveHandler != null) {
                nodeSelectedAdditiveHandler.accept(id, additive);
            } else if (nodeSelectedHandler != null) {
                nodeSelectedHandler.accept(id);
            }
        }

        public void onEdgeSelected(String edgeDataJson) {
            if (edgeSelectedHandler != null) {
                edgeSelectedHandler.accept(edgeDataJson);
            }
        }

        public void onBackgroundTap() {
            if (backgroundTapHandler != null) {
                backgroundTapHandler.run();
            }
        }

        /** Fired from bridge.js after layout stop (or ensureLaidOut with nothing to do). */
        public void onLayoutComplete() {
            final int gen = loadGeneration.get();
            Platform.runLater(() -> {
                if (gen != loadGeneration.get()) {
                    return;
                }
                // Layout already fitted in JS; only apply pending hub/driver/bridge focus.
                applyPendingFocus(gen);
            });
        }
    }
}
