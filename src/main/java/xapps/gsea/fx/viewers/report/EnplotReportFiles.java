/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolve enrichment-plot and related image files from a GSEA report directory / HTML page.
 */
public final class EnplotReportFiles {

    private static final Pattern IMG_SRC = Pattern.compile(
            "(?i)<img\\b[^>]*\\bsrc\\s*=\\s*[\"']([^\"']+)[\"']");

    private EnplotReportFiles() {
    }

    public static File findClassicEnplot(File reportDir, String geneSetName, File html) {
        File fromHtml = findEnplotFromHtml(reportDir, html, false);
        if (fromHtml != null) {
            return fromHtml;
        }
        if (reportDir == null || geneSetName == null) {
            return null;
        }
        String lowerName = geneSetName.toLowerCase(Locale.ROOT);
        return ReportExplorerSupport.findFirst(reportDir, n -> {
            String lower = n.toLowerCase(Locale.ROOT);
            return lower.startsWith("enplot_" + lowerName)
                    && !lower.startsWith("enplot2_")
                    && ReportExplorerSupport.isImageName(n);
        });
    }

    public static File findEnplotV2(File reportDir, String geneSetName, File html) {
        File fromHtml = findEnplotFromHtml(reportDir, html, true);
        if (fromHtml != null) {
            return fromHtml;
        }
        if (reportDir == null || geneSetName == null) {
            return null;
        }
        String lowerName = geneSetName.toLowerCase(Locale.ROOT);
        return ReportExplorerSupport.findFirst(reportDir, n -> {
            String lower = n.toLowerCase(Locale.ROOT);
            return lower.startsWith("enplot2_" + lowerName) && ReportExplorerSupport.isImageName(n);
        });
    }

    public static File findEnplotFromHtml(File reportDir, File html, boolean v2) {
        if (html == null || !html.isFile()) {
            return null;
        }
        for (String src : extractImgSrcs(html)) {
            String lower = fileName(src).toLowerCase(Locale.ROOT);
            boolean match = v2 ? lower.startsWith("enplot2_")
                    : (lower.startsWith("enplot_") && !lower.startsWith("enplot2_"));
            if (match) {
                File image = resolveReportImage(reportDir, src);
                if (image != null) {
                    return image;
                }
            }
        }
        return null;
    }

    public static List<String> extractImgSrcs(File html) {
        List<String> srcs = new ArrayList<>();
        try {
            Matcher m = IMG_SRC.matcher(Files.readString(html.toPath()));
            while (m.find()) {
                String src = m.group(1).trim();
                if (!src.isEmpty() && !srcs.contains(src)) {
                    srcs.add(src);
                }
            }
        } catch (Throwable ignored) {
            // caller falls back to filesystem patterns
        }
        return srcs;
    }

    public static File resolveReportImage(File reportDir, String src) {
        String name = fileName(src);
        if (!ReportExplorerSupport.isImageName(name)) {
            return null;
        }
        if (reportDir != null) {
            File inReport = new File(reportDir, name);
            if (inReport.isFile()) {
                return inReport;
            }
        }
        File asPath = new File(src);
        return asPath.isFile() ? asPath : null;
    }

    public static String plotTitle(String fileName, String geneSetName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.startsWith("enplot2_")) {
            return "EnPlot v2";
        }
        if (lower.startsWith("enplot_")) {
            return "EnPlot Classic";
        }
        if (lower.startsWith("gset_rnd_es_dist")) {
            return "Null ES distribution";
        }
        if (geneSetName != null && lower.startsWith(geneSetName.toLowerCase(Locale.ROOT) + "_")) {
            return "Heatmap";
        }
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    public static String fileName(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    public static File findGeneSetSibling(File reportDir, String geneSetName, String ext) {
        if (reportDir == null || geneSetName == null) {
            return null;
        }
        File exact = new File(reportDir, geneSetName + ext);
        return exact.isFile() ? exact : null;
    }
}
