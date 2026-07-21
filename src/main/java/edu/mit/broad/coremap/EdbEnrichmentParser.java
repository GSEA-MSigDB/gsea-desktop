/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.mit.broad.coremap.CoreMapTypes.EnrichmentParseResult;
import edu.mit.broad.coremap.CoreMapTypes.EnrichmentRow;

/**
 * Parse Broad GSEA {@code results.edb} + companion {@code .rnk} into leading-edge rows
 * using CoreMap / CoreEnrich rules.
 */
public final class EdbEnrichmentParser {

    private static final Pattern DTG = Pattern.compile("<DTG\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTR = Pattern.compile("([A-Za-z_][\\w:-]*)\\s*=\\s*\"([^\"]*)\"");

    private EdbEnrichmentParser() {
    }

    public static EnrichmentParseResult parseReportDirectory(File reportOrEdbDir) throws Exception {
        return parseReportDirectory(reportOrEdbDir, CoreMapConstants.DEFAULT_MIN_NES,
                CoreMapConstants.DEFAULT_MAX_NP, CoreMapConstants.DEFAULT_MAX_FDR, false);
    }

    public static EnrichmentParseResult parseReportDirectory(File reportOrEdbDir,
            double minNes, double maxNp, double maxFdr, boolean applyFilters) throws Exception {
        File edbDir = resolveEdbDir(reportOrEdbDir);
        File edbFile = new File(edbDir, "results.edb");
        if (!edbFile.isFile()) {
            throw new IllegalArgumentException("results.edb not found in: " + edbDir);
        }
        File rnkFile = findRnk(edbDir);
        if (rnkFile == null) {
            throw new IllegalArgumentException("Companion .rnk not found under: " + edbDir);
        }
        String edbText = Files.readString(edbFile.toPath(), StandardCharsets.UTF_8);
        String rnkText = Files.readString(rnkFile.toPath(), StandardCharsets.UTF_8);
        return parseEdbDocuments(edbText, rnkText, minNes, maxNp, maxFdr, applyFilters);
    }

    public static File resolveEdbDir(File reportOrEdbDir) {
        if (reportOrEdbDir == null) {
            throw new IllegalArgumentException("Report directory is null");
        }
        File nested = new File(reportOrEdbDir, "edb");
        if (new File(nested, "results.edb").isFile()) {
            return nested;
        }
        if (new File(reportOrEdbDir, "results.edb").isFile()) {
            return reportOrEdbDir;
        }
        // Parent report dir with nested edb
        if (nested.isDirectory()) {
            return nested;
        }
        return reportOrEdbDir;
    }

    private static File findRnk(File edbDir) {
        File[] files = edbDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".rnk"));
        if (files == null || files.length == 0) {
            return null;
        }
        return files[0];
    }

    public static EnrichmentParseResult parseEdbDocuments(String edbText, String rnkText,
            double minNes, double maxNp, double maxFdr, boolean applyFilters) {
        ParsedRnk rnk = parseRnk(rnkText);
        Map<String, Double> rankMap = geneRankMapFromRnk(rnk.genes, rnk.ranks);
        List<EnrichmentRow> rows = new ArrayList<>();

        Matcher m = DTG.matcher(edbText);
        while (m.find()) {
            Map<String, String> dtg = parseDtgAttributes(m.group());
            String genesetRaw = dtg.getOrDefault("GENESET", "");
            String setId = genesetRaw.contains("#")
                    ? genesetRaw.substring(genesetRaw.lastIndexOf('#') + 1).trim()
                    : genesetRaw.trim();
            if (setId.isEmpty()) {
                continue;
            }
            double nes;
            double npVal;
            double fdr;
            int rankAtEs;
            double rankScoreAtEs;
            try {
                nes = Double.parseDouble(dtg.getOrDefault("NES", "NaN"));
                npVal = Double.parseDouble(dtg.getOrDefault("NP", "NaN"));
                fdr = Double.parseDouble(dtg.getOrDefault("FDR", "NaN"));
                rankAtEs = (int) Math.floor(Double.parseDouble(dtg.getOrDefault("RANK_AT_ES", "NaN")));
                rankScoreAtEs = Double.parseDouble(dtg.getOrDefault("RANK_SCORE_AT_ES", "NaN"));
                if (!Double.isFinite(nes) || !Double.isFinite(npVal) || !Double.isFinite(fdr)
                        || !Double.isFinite(rankAtEs) || !Double.isFinite(rankScoreAtEs)) {
                    throw new NumberFormatException("nan");
                }
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid numeric fields on DTG for " + setId);
            }
            if (applyFilters && !EnrichmentMath.passesThresholds(nes, npVal, fdr, minNes, maxNp, maxFdr)) {
                continue;
            }
            String hitRaw = dtg.getOrDefault("HIT_INDICES", "").trim();
            if (hitRaw.isEmpty()) {
                continue;
            }
            List<Integer> hitIndices = new ArrayList<>();
            for (String tok : hitRaw.split("\\s+")) {
                if (!tok.isEmpty()) {
                    hitIndices.add((int) Double.parseDouble(tok));
                }
            }
            List<Integer> leading = leadingEdgeIndices(hitIndices, rankAtEs, rankScoreAtEs);
            for (int index : leading) {
                if (index < 0 || index >= rnk.genes.size()) {
                    continue;
                }
                String gene = rnk.genes.get(index);
                if (gene == null || gene.equalsIgnoreCase("nan")) {
                    continue;
                }
                EnrichmentRow row = new EnrichmentRow();
                row.geneSymbol = gene.toUpperCase(Locale.ROOT);
                row.setId = setId;
                row.setName = setId;
                row.nes = nes;
                row.rnkScore = rnk.ranks.get(index);
                row.pValue = npVal;
                row.fdr = fdr;
                row.leadingEdge = true;
                rows.add(row);
            }
        }
        if (rows.isEmpty()) {
            if (applyFilters) {
                throw new IllegalArgumentException(String.format(Locale.ROOT,
                        "No leading-edge genes passed EDB filters (min |NES|=%s, max NOM pVal=%s, max FDR=%s).",
                        minNes, maxNp, maxFdr));
            }
            throw new IllegalArgumentException("No leading-edge genes found in EDB results.");
        }
        return new EnrichmentParseResult(rows, rankMap);
    }

    static List<Integer> leadingEdgeIndices(List<Integer> hitIndices, int rankAtEs, double rankScoreAtEs) {
        List<Integer> out = new ArrayList<>();
        if (rankScoreAtEs >= 0) {
            for (int index : hitIndices) {
                if (index <= rankAtEs) {
                    out.add(index);
                }
            }
            return out;
        }
        Integer closest = null;
        for (int i : hitIndices) {
            if (i < rankAtEs) {
                if (closest == null || i > closest) {
                    closest = i;
                }
            }
        }
        for (int index : hitIndices) {
            if ((closest != null && index == closest) || index >= rankAtEs) {
                out.add(index);
            }
        }
        return out;
    }

    private static Map<String, String> parseDtgAttributes(String tag) {
        Map<String, String> attrs = new HashMap<>();
        Matcher m = ATTR.matcher(tag);
        while (m.find()) {
            attrs.put(m.group(1), m.group(2));
        }
        return attrs;
    }

    private static ParsedRnk parseRnk(String rnkText) {
        List<String> genes = new ArrayList<>();
        List<Double> ranks = new ArrayList<>();
        String text = rnkText.startsWith("\uFEFF") ? rnkText.substring(1) : rnkText;
        for (String line : text.split("\\r?\\n")) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.split("\t");
            if (parts.length < 2) {
                continue;
            }
            String gene = parts[0].trim();
            try {
                double rank = Double.parseDouble(parts[1].trim());
                if (gene.isEmpty() || !Double.isFinite(rank)) {
                    continue;
                }
                genes.add(gene);
                ranks.add(rank);
            } catch (NumberFormatException ignored) {
                // skip header / bad lines
            }
        }
        return new ParsedRnk(genes, ranks);
    }

    private static Map<String, Double> geneRankMapFromRnk(List<String> genes, List<Double> ranks) {
        Map<String, Double> map = new HashMap<>();
        for (int i = 0; i < genes.size(); i++) {
            String gene = genes.get(i).toUpperCase(Locale.ROOT);
            if (gene.isEmpty() || gene.equals("NAN")) {
                continue;
            }
            double rank = ranks.get(i);
            if (!Double.isFinite(rank)) {
                continue;
            }
            Double cur = map.get(gene);
            if (cur == null || Math.abs(rank) >= Math.abs(cur)) {
                map.put(gene, rank);
            }
        }
        return map;
    }

    private static final class ParsedRnk {
        final List<String> genes;
        final List<Double> ranks;

        ParsedRnk(List<String> genes, List<Double> ranks) {
            this.genes = genes;
            this.ranks = ranks;
        }
    }
}
