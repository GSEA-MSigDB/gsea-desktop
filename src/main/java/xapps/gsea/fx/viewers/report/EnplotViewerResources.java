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

import edu.mit.broad.genome.reports.ModernEnrichmentPlotJson;
import xapps.gsea.fx.FxTheme;

/**
 * Resolves a {@code file:} URL for the bundled EnPlot viewer so JavaFX WebView loads
 * {@code viewer.js} / {@code viewer.css} reliably (classpath {@code jar:} URLs are flaky on Windows).
 */
final class EnplotViewerResources {

    private static final Logger klog = LoggerFactory.getLogger(EnplotViewerResources.class);
    private static final String[] VIEWER_FILES = ModernEnrichmentPlotJson.VIEWER_FILES;

    private static volatile File bundledViewerDir;

    private EnplotViewerResources() {
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
        synchronized (EnplotViewerResources.class) {
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
