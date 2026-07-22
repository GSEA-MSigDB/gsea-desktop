/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.reports;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import edu.mit.broad.genome.plots.ColorBarSegment;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.math.Vector;
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

    /** Bundled viewer assets copied into reports and FX temp dirs. */
    public static final String[] VIEWER_FILES = {
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
        } catch (ParseException | RuntimeException e) {
            return false;
        }
    }

    private static boolean isDenseEsCurve(JSONObject root, int listSize) {
        JSONArray esCurve = (JSONArray) root.get("esCurve");
        if (esCurve == null || esCurve.isEmpty()) {
            return false;
        }
        int minDense = Math.min(listSize, Math.max(50, listSize / 80));
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
                                    final ColorBarSegment[] colorBarMarkers_opt) throws IOException {
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
                                   final ColorBarSegment[] colorBarMarkers_opt) {
        if (esProfile == null) {
            throw new IllegalArgumentException("esProfile cannot be null");
        }
        if (rl == null) {
            throw new IllegalArgumentException("rl cannot be null");
        }
        EnrichmentMountainData data = EnrichmentMountainData.build(
                esProfile, esProfile_full_opt, hitIndices, rl);

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

        root.put("listSize", data.listSize);
        root.put("peakRank", data.peakRank);
        root.put("peakEs", (double) data.peakEs);
        if (data.zeroCross >= 0) {
            root.put("zeroCross", data.zeroCross);
        }
        root.put("metricName", data.metricName);

        JSONArray hits = new JSONArray();
        for (int i = 0; i < data.hitRanks.length; i++) {
            int rank = data.hitRanks[i];
            JSONObject h = new JSONObject();
            h.put("rank", rank);
            h.put("symbol", rl.getRankName(rank));
            h.put("metric", (double) rl.getScore(rank));
            h.put("runningEs", i < esProfile.getSize() ? (double) esProfile.getElement(i) : 0d);
            h.put("leadingEdge", i < data.leadingEdge.length && data.leadingEdge[i]);
            hits.add(h);
        }
        root.put("hits", hits);
        root.put("esCurve", esCurveArray(data, esProfile_full_opt));
        root.put("metric", metricArray(data));
        if (data.metricY.length > 0) {
            double min = data.metricY[0];
            double max = data.metricY[0];
            for (double v : data.metricY) {
                if (v < min) {
                    min = v;
                }
                if (v > max) {
                    max = v;
                }
            }
            root.put("metricMin", min);
            root.put("metricMax", max);
        } else {
            root.put("metricMin", 0.0d);
            root.put("metricMax", 0.0d);
        }
        root.put("colorBar", colorBarArray(rl, colorBarMarkers_opt));
        return root.toJSONString();
    }

    private static JSONArray colorBarArray(final RankedList rl, final ColorBarSegment[] markersOpt) {
        JSONArray out = new JSONArray();
        ColorBarSegment[] markers = markersOpt != null
                ? markersOpt
                : ColorBarSegment.forRankedList(rl);
        if (markers == null) {
            return out;
        }
        for (ColorBarSegment marker : markers) {
            JSONObject seg = new JSONObject();
            seg.put("start", marker.start);
            seg.put("end", marker.end);
            seg.put("color", marker.toCssRgb());
            out.add(seg);
        }
        return out;
    }

    private static JSONArray esCurveArray(EnrichmentMountainData data, Vector esProfile_full_opt) {
        JSONArray esCurve = new JSONArray();
        if (esProfile_full_opt != null && esProfile_full_opt.getSize() > 0) {
            appendDownsampled(esCurve, esProfile_full_opt, data.peakRank);
            return esCurve;
        }
        // Sparse fallback already includes endpoints in EnrichmentMountainData.
        for (int i = 0; i < data.esY.length; i++) {
            appendPoint(esCurve, (int) Math.round(data.esX[i]), data.esY[i]);
        }
        return esCurve;
    }

    private static JSONArray metricArray(EnrichmentMountainData data) {
        JSONArray metric = new JSONArray();
        int listSize = data.listSize;
        int step = Math.max(1, listSize / MAX_SERIES_POINTS);
        Integer lastX = null;
        for (int i = 0; i < listSize; i += step) {
            appendPoint(metric, i, data.metricY[i]);
            lastX = i;
        }
        if (data.zeroCross >= 0 && data.zeroCross < listSize
                && (lastX == null || data.zeroCross != lastX)) {
            insertPointSorted(metric, data.zeroCross, data.metricY[data.zeroCross]);
        }
        if (listSize > 0 && (listSize - 1) % step != 0) {
            appendPoint(metric, listSize - 1, data.metricY[listSize - 1]);
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
