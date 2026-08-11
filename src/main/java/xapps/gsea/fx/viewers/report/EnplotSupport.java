/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.alg.gsea.GeneSetScoringTable;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResult;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentScore;
import edu.mit.broad.genome.reports.EnrichmentEsProfiles;
import edu.mit.broad.genome.reports.EnrichmentReports;
import edu.mit.broad.genome.reports.ModernEnrichmentPlotJson;
import xapps.gsea.fx.FxTheme;

/**
 * Interactive EnPlot v2: bundled viewer URL + JSON payload resolve/build.
 */
final class EnplotSupport {

    private static final Logger klog = LoggerFactory.getLogger(EnplotSupport.class);
    private static final String[] VIEWER_FILES = ModernEnrichmentPlotJson.VIEWER_FILES;
    private static volatile File bundledViewerDir;

    private EnplotSupport() {
    }

    /** Extract bundled assets to a temp {@code file:} folder (always matches app version). */
    static String viewerPageUrl() {
        boolean dark = FxTheme.isDarkEffective();
        try {
            File index = new File(bundledViewerDir(), "index.html");
            return pageUrl(index, dark);
        } catch (IOException ioe) {
            klog.error("Could not extract bundled EnPlot viewer", ioe);
            return null;
        }
    }

    static File jsonFile(File reportDir, String geneSetName) {
        return reportDir != null ? ModernEnrichmentPlotJson.jsonFile(reportDir, geneSetName) : null;
    }

    /** Background-safe: read validated JSON from disk only. */
    static String readCachedJson(File jsonFile, int listSize) throws IOException {
        if (jsonFile == null || !jsonFile.isFile()) {
            return null;
        }
        String json = Files.readString(jsonFile.toPath());
        return ModernEnrichmentPlotJson.isCurrentPayload(json, listSize) ? json : null;
    }

    /** FX-thread only: build JSON from enrichment result (may recompute ES curve). */
    static String buildFromResult(EnrichmentResult result, String classA, String classB,
            GeneSetScoringTable scoring, String metricName) {
        if (result == null || result.getGeneSet() == null || result.getRankedList() == null
                || result.getScore() == null) {
            return null;
        }
        String geneSetName = result.getGeneSet().getName(true);
        RankedList rl = result.getRankedList();
        EnrichmentReports.ensureMetricName(rl, metricName);
        EnrichmentScore score = result.getScore();
        return ModernEnrichmentPlotJson.buildPayloadJson(geneSetName, rl, score.getHitIndices(),
                score.getESProfile(), EnrichmentEsProfiles.fullEsProfile(result, scoring),
                classA, classB, score.getES(), score.getNES(), score.getNP(),
                score.getFDR(), score.getFWER(),
                EnrichmentReports.colorBarMarkers(rl));
    }

    private static String pageUrl(File indexHtml, boolean dark) {
        String base = indexHtml.toURI().toASCIIString();
        String url = base + (base.contains("?") ? "&" : "?") + "inject=1";
        if (dark) {
            url += "&theme=dark";
        }
        return url;
    }

    private static File bundledViewerDir() throws IOException {
        File cached = bundledViewerDir;
        if (cached != null && new File(cached, "index.html").isFile()) {
            refreshBundledAssets(cached);
            return cached;
        }
        synchronized (EnplotSupport.class) {
            cached = bundledViewerDir;
            if (cached != null && new File(cached, "index.html").isFile()) {
                refreshBundledAssets(cached);
                return cached;
            }
            File dir = Files.createTempDirectory("gsea-enplot-viewer").toFile();
            dir.deleteOnExit();
            refreshBundledAssets(dir);
            bundledViewerDir = dir;
            return dir;
        }
    }

    private static void refreshBundledAssets(File dir) throws IOException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create viewer dir: " + dir);
        }
        for (String name : VIEWER_FILES) {
            String resource = ModernEnrichmentPlotJson.VIEWER_RESOURCE_DIR + name;
            try (InputStream in = ModernEnrichmentPlotJson.class.getResourceAsStream(resource)) {
                if (in == null) {
                    throw new IOException("Missing viewer resource: " + resource);
                }
                Files.copy(in, new File(dir, name).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
