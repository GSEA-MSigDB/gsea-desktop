/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;

/**
 * Shared Leading Edge report artifacts: heatmap image tabs + HTML Report tab.
 * Used by the interactive LE workspace after a tool run and by
 * {@link LeadingEdgeReportExplorer} when reopening from history / process table.
 */
public final class LeadingEdgeOutputs {

    private static final Logger klog = LoggerFactory.getLogger(LeadingEdgeOutputs.class);

    private LeadingEdgeOutputs() {
    }

    /**
     * @param closable true on the interactive LE pane; false in the report explorer shell
     */
    public static List<Tab> buildTabs(File reportDir, boolean closable) {
        List<Tab> tabs = new ArrayList<>();
        if (reportDir == null || !reportDir.isDirectory()) {
            return tabs;
        }
        for (File image : findHeatMapImages(reportDir)) {
            try {
                Tab tab = new Tab(shortHeatMapTitle(image.getName()),
                        ReportExplorerSupport.singleImage(image));
                tab.setClosable(closable);
                tabs.add(tab);
            } catch (Throwable t) {
                klog.warn("Could not load heatmap {}", image, t);
            }
        }
        File index = new File(reportDir, "index.html");
        if (index.isFile()) {
            Tab reportTab = webReportTab(index);
            if (reportTab != null) {
                reportTab.setClosable(closable);
                tabs.add(reportTab);
            }
        }
        return tabs;
    }

    /** True if the folder looks like a LeadingEdgeTool report (has LE heatmaps). */
    public static boolean hasHeatMapArtifacts(File reportDir) {
        if (reportDir == null || !reportDir.isDirectory()) {
            return false;
        }
        File[] files = reportDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            return lower.startsWith("leading_edge_heat_map")
                    && (lower.endsWith(".png") || lower.endsWith(".jpg")
                    || lower.endsWith(".jpeg") || lower.endsWith(".svg"));
        });
        return files != null && files.length > 0;
    }

    static List<File> findHeatMapImages(File reportDir) {
        File[] files = reportDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            return lower.startsWith("leading_edge_heat_map")
                    && (lower.endsWith(".png") || lower.endsWith(".jpg")
                    || lower.endsWith(".jpeg") || lower.endsWith(".svg"));
        });
        if (files == null || files.length == 0) {
            return List.of();
        }
        Map<String, File> preferred = new HashMap<>();
        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File f : files) {
            String name = f.getName();
            int dot = name.lastIndexOf('.');
            String stem = dot > 0 ? name.substring(0, dot) : name;
            String ext = dot > 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
            File existing = preferred.get(stem);
            if (existing == null || extRank(ext) < extRank(extensionOf(existing))) {
                preferred.put(stem, f);
            }
        }
        List<File> out = new ArrayList<>(preferred.values());
        out.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    private static Tab webReportTab(File index) {
        try {
            WebView webView = new WebView();
            webView.setMinSize(0, 0);
            webView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            webView.getEngine().load(index.toURI().toString());
            BorderPane host = new BorderPane(webView);
            host.setMinSize(0, 0);
            return new Tab("Report", host);
        } catch (Throwable t) {
            klog.warn("WebView report tab unavailable", t);
            return null;
        }
    }

    private static String shortHeatMapTitle(String fileName) {
        String base = fileName;
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        final String prefix = "leading_edge_heat_map_";
        if (base.startsWith(prefix)) {
            return base.substring(prefix.length());
        }
        return base.startsWith("leading_edge_heat_map") ? "heatmap" : base;
    }

    private static String extensionOf(File f) {
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private static int extRank(String ext) {
        return switch (ext) {
            case "png" -> 0;
            case "jpg", "jpeg" -> 1;
            case "svg" -> 2;
            default -> 9;
        };
    }
}
