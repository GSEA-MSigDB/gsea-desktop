/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.reports;

import java.awt.Color;
import java.awt.Paint;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.jfree.chart.plot.IntervalMarker;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.math.XMath;
import edu.mit.broad.genome.objects.MetricWeightStruc;
import edu.mit.broad.genome.objects.RankedList;

/**
 * Writes {@code enplot2_*} JSON payloads and copies the EnPlot v2 interactive viewer assets into a report directory.
 */
@SuppressWarnings("unchecked")
public final class ModernEnrichmentPlotJson {

    private static final Logger klog = LoggerFactory.getLogger(ModernEnrichmentPlotJson.class);

    /** Bundled viewer folder under classpath resources. */
    public static final String VIEWER_RESOURCE_DIR = "/xapps/gsea/fx/enplot/";
    /** Copied into each report when EnPlot v2 outputs are enabled. */
    public static final String VIEWER_DIR_NAME = "enplot_viewer";

    private static final String[] VIEWER_FILES = {
            "index.html",
            "viewer.js",
            "viewer.css"
    };

    /** Cap series length so large ranked lists stay browser-friendly. */
    private static final int MAX_SERIES_POINTS = 4000;

    private ModernEnrichmentPlotJson() {
    }

    public static String safeBaseName(String gsetName) {
        return NamingConventions.createSafeFileName(EnrichmentReports.ENPLOT2_ + gsetName);
    }

    /** Current interactive payload schema (bump when required fields change). */
    public static final int PAYLOAD_VERSION = 2;

    public static File jsonFile(File reportDir, String gsetName) {
        return new File(reportDir, safeBaseName(gsetName) + ".json");
    }

    /**
     * Returns true when saved JSON matches {@link #PAYLOAD_VERSION} and includes a dense ES curve
     * plus fields required by the current viewer.
     */
    @SuppressWarnings("unchecked")
    public static boolean isCurrentPayload(String jsonText, int listSize) {
        if (jsonText == null || jsonText.isBlank() || listSize <= 0) {
            return false;
        }
        try {
            JSONObject root = (JSONObject) new JSONParser().parse(jsonText);
            Object ver = root.get("version");
            int version = ver instanceof Number n ? n.intValue()
                    : ver != null ? Integer.parseInt(String.valueOf(ver)) : 0;
            if (version < PAYLOAD_VERSION) {
                return false;
            }
            if (!isDenseEsCurve(root, listSize)) {
                return false;
            }
            if (!(root.get("colorBar") instanceof JSONArray)) {
                return false;
            }
            if (!root.containsKey("metricName")) {
                return false;
            }
            return root.containsKey("metricMin") && root.containsKey("metricMax");
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isDenseEsCurve(JSONObject root, int listSize) {
        JSONArray esCurve = (JSONArray) root.get("esCurve");
        if (esCurve == null || esCurve.isEmpty()) {
            return false;
        }
        int minDense = Math.max(50, listSize / 80);
        return esCurve.size() >= minDense;
    }

    /**
     * Relative href from a gene-set HTML page in the report root to the shared viewer,
     * loading a {@code .js} boot script (works under {@code file://}).
     */
    public static String sharedViewerHref(String bootJsFileName) {
        return VIEWER_DIR_NAME + "/index.html?data=" + encodeQuery("../" + bootJsFileName);
    }

    public static File writePayload(final File saveInDir,
                                    final String gsetName,
                                    final RankedList rl,
                                    final int[] hitIndices,
                                    final Vector esProfile,
                                    final Vector esProfile_full_opt,
                                    final String classAName_opt,
                                    final String classBName_opt,
                                    final float es,
                                    final float nes,
                                    final float np,
                                    final float fdr,
                                    final float fwer,
                                    final IntervalMarker[] colorBarMarkers_opt) throws IOException {
        if (saveInDir == null) {
            throw new IllegalArgumentException("saveInDir cannot be null");
        }
        if (!saveInDir.exists()) {
            saveInDir.mkdirs();
        }

        String json = buildPayloadJson(gsetName, rl, hitIndices, esProfile, esProfile_full_opt,
                classAName_opt, classBName_opt, es, nes, np, fdr, fwer, colorBarMarkers_opt);
        File jsonFile = new File(saveInDir, safeBaseName(gsetName) + ".json");
        try (FileWriter fw = new FileWriter(jsonFile, StandardCharsets.UTF_8)) {
            fw.write(json);
        }
        return jsonFile;
    }

    /**
     * Writes a {@code .js} boot file that calls {@code window.__enplotReady(...)} so the shared
     * viewer can load plot data under {@code file://} (where {@code fetch} of JSON is blocked).
     */
    public static File writeBootScript(final File saveInDir, final String gsetName, final File jsonFile)
            throws IOException {
        String json = Files.readString(jsonFile.toPath(), StandardCharsets.UTF_8);
        File jsFile = new File(saveInDir, safeBaseName(gsetName) + ".js");
        try (FileWriter fw = new FileWriter(jsFile, StandardCharsets.UTF_8)) {
            fw.write("window.__enplotReady(");
            fw.write(json);
            fw.write(");\n");
        }
        return jsFile;
    }

    public static String buildPayloadJson(final String gsetName,
                                   final RankedList rl,
                                   final int[] hitIndices,
                                   final Vector esProfile,
                                   final Vector esProfile_full_opt,
                                   final String classAName_opt,
                                   final String classBName_opt,
                                   final float es,
                                   final float nes,
                                   final float np,
                                   final float fdr,
                                   final float fwer,
                                   final IntervalMarker[] colorBarMarkers_opt) {
        JSONObject root = new JSONObject();
        root.put("version", PAYLOAD_VERSION);
        root.put("geneSet", gsetName);
        root.put("classA", classAName_opt != null ? classAName_opt : "");
        root.put("classB", classBName_opt != null ? classBName_opt : "");

        JSONObject stats = new JSONObject();
        stats.put("es", (double) es);
        stats.put("nes", (double) nes);
        stats.put("np", (double) np);
        stats.put("fdr", (double) fdr);
        stats.put("fwer", (double) fwer);
        root.put("stats", stats);

        int listSize = rl.getSize();
        root.put("listSize", listSize);

        int peakHit = esProfile.maxDevFrom0Index();
        float peakEs = esProfile.maxDevFrom0();
        boolean pos = XMath.isPositive(peakEs);
        int peakRank = hitIndices[peakHit];
        root.put("peakRank", peakRank);
        root.put("peakEs", (double) peakEs);

        MetricWeightStruc mws = rl.getMetricWeightStruc();
        if (mws != null) {
            root.put("zeroCross", mws.getTotalPosLength());
        }
        root.put("metricName", mws != null && mws.getMetricName() != null ? mws.getMetricName() : "");

        JSONArray hits = new JSONArray();
        for (int i = 0; i < hitIndices.length; i++) {
            int rank = hitIndices[i];
            JSONObject h = new JSONObject();
            h.put("rank", rank);
            h.put("symbol", rl.getRankName(rank));
            h.put("metric", (double) rl.getScore(rank));
            h.put("runningEs", (double) esProfile.getElement(i));
            boolean leading = (pos && rank <= peakRank) || (!pos && rank >= peakRank);
            h.put("leadingEdge", leading);
            hits.add(h);
        }
        root.put("hits", hits);
        root.put("esCurve", esCurveArray(hitIndices, esProfile, esProfile_full_opt, listSize, peakRank));
        root.put("metric", metricArray(rl, listSize));
        Vector metricScores = Vector.infinityAdjustRankedScoreVector(rl.getScoresV(false));
        if (metricScores.getSize() > 0) {
            root.put("metricMin", (double) metricScores.min());
            root.put("metricMax", (double) metricScores.max());
        } else {
            root.put("metricMin", 0.0d);
            root.put("metricMax", 0.0d);
        }
        root.put("colorBar", colorBarArray(rl, colorBarMarkers_opt));
        return root.toJSONString();
    }

    private static JSONArray colorBarArray(final RankedList rl, final IntervalMarker[] markersOpt) {
        JSONArray out = new JSONArray();
        IntervalMarker[] markers = markersOpt;
        if (markers == null) {
            int numRanges = (rl.getSize() < 100) ? rl.getSize() : 100;
            markers = RankedListCharts.createIntervalMarkers(numRanges, rl);
        }
        if (markers == null) {
            return out;
        }
        for (IntervalMarker marker : markers) {
            JSONObject seg = new JSONObject();
            seg.put("start", marker.getStartValue());
            seg.put("end", marker.getEndValue());
            seg.put("color", paintToCss(marker.getPaint()));
            out.add(seg);
        }
        return out;
    }

    private static String paintToCss(Paint paint) {
        if (paint instanceof Color c) {
            return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
        }
        return "#cccccc";
    }

    private static JSONArray esCurveArray(final int[] hitIndices,
                                          final Vector esProfile,
                                          final Vector esProfile_full_opt,
                                          final int listSize,
                                          final int peakRank) {
        JSONArray esCurve = new JSONArray();
        if (esProfile_full_opt != null && esProfile_full_opt.getSize() > 0) {
            appendDownsampled(esCurve, esProfile_full_opt, peakRank);
            return esCurve;
        }
        // Sparse fallback: hits only, with endpoints at 0
        appendPoint(esCurve, 0, 0.0d);
        for (int i = 0; i < hitIndices.length; i++) {
            appendPoint(esCurve, hitIndices[i], esProfile.getElement(i));
        }
        appendPoint(esCurve, listSize - 1, 0.0d);
        return esCurve;
    }

    private static JSONArray metricArray(final RankedList rl, final int listSize) {
        JSONArray metric = new JSONArray();
        Vector scores = Vector.infinityAdjustRankedScoreVector(rl.getScoresV(false));
        int zeroCross = -1;
        MetricWeightStruc mws = rl.getMetricWeightStruc();
        if (mws != null) {
            zeroCross = mws.getTotalPosLength();
        }
        int step = Math.max(1, listSize / MAX_SERIES_POINTS);
        Integer lastX = null;
        for (int i = 0; i < listSize; i += step) {
            appendPoint(metric, i, scores.getElement(i));
            lastX = i;
        }
        if (zeroCross >= 0 && zeroCross < listSize && (lastX == null || zeroCross != lastX)) {
            insertPointSorted(metric, zeroCross, scores.getElement(zeroCross));
        }
        if (listSize > 0 && (listSize - 1) % step != 0) {
            appendPoint(metric, listSize - 1, scores.getElement(listSize - 1));
        }
        return metric;
    }

    private static void appendDownsampled(JSONArray out, Vector series, int mustIncludeX) {
        int size = series.getSize();
        int step = Math.max(1, size / MAX_SERIES_POINTS);
        Integer lastX = null;
        for (int i = 0; i < size; i += step) {
            appendPoint(out, i, series.getElement(i));
            lastX = i;
        }
        if (mustIncludeX >= 0 && mustIncludeX < size
                && (lastX == null || mustIncludeX != lastX)) {
            insertPointSorted(out, mustIncludeX, series.getElement(mustIncludeX));
        }
        if (size > 0 && (size - 1) % step != 0) {
            appendPoint(out, size - 1, series.getElement(size - 1));
        }
    }

    /** Insert {@code [x,y]} keeping ascending x order (for sparse forced peak/zero-cross points). */
    private static void insertPointSorted(JSONArray out, int x, double y) {
        for (int i = 0; i < out.size(); i++) {
            Object el = out.get(i);
            if (!(el instanceof JSONArray pt) || pt.isEmpty()) {
                continue;
            }
            Object xo = pt.get(0);
            int px = xo instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(xo));
            if (px == x) {
                return;
            }
            if (px > x) {
                JSONArray np = new JSONArray();
                np.add(x);
                np.add(y);
                out.add(i, np);
                return;
            }
        }
        appendPoint(out, x, y);
    }

    private static void appendPoint(JSONArray out, int x, double y) {
        JSONArray pt = new JSONArray();
        pt.add(x);
        pt.add(y);
        out.add(pt);
    }

    private static String encodeQuery(String s) {
        try {
            return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception e) {
            return s;
        }
    }

    /**
     * Ensures {@code enplot_viewer/} exists under the report dir.
     *
     * @param forceOverwrite when true (report generation), refresh assets from the classpath;
     *                       when false (FX browse), only copy files that are missing.
     */
    public static void ensureViewerAssets(File reportDir, boolean forceOverwrite) {
        if (reportDir == null) {
            return;
        }
        File viewerDir = new File(reportDir, VIEWER_DIR_NAME);
        if (!viewerDir.exists() && !viewerDir.mkdirs()) {
            klog.warn("Could not create enplot viewer dir: {}", viewerDir);
            return;
        }
        for (String name : VIEWER_FILES) {
            File dest = new File(viewerDir, name);
            if (!forceOverwrite && dest.isFile()) {
                continue;
            }
            String resource = VIEWER_RESOURCE_DIR + name;
            try (InputStream in = ModernEnrichmentPlotJson.class.getResourceAsStream(resource)) {
                if (in == null) {
                    klog.warn("Missing enplot viewer resource: {}", resource);
                    continue;
                }
                Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ioe) {
                klog.warn("Failed copying enplot viewer asset {}: {}", name, ioe.getMessage());
            }
        }
    }

    /** Report-generation convenience: always refresh viewer assets. */
    public static void ensureViewerAssets(File reportDir) {
        ensureViewerAssets(reportDir, true);
    }
}
