/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap.providers;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolve gene-set IDs to source platforms (Reactome/KEGG/GO/HPO/…).
 * Uses accession/name heuristics always; optionally loads a full MSigDB JSON catalog.
 */
public final class MsigdbResolver {

    private static final Logger klog = LoggerFactory.getLogger(MsigdbResolver.class);

    private static final Pattern REACTOME = Pattern.compile("^R-(HSA|MMU|RNO)-\\d+", Pattern.CASE_INSENSITIVE);
    private static final Pattern GO = Pattern.compile("^GO:\\d+", Pattern.CASE_INSENSITIVE);
    private static final Pattern HPO = Pattern.compile("^HP:\\d+", Pattern.CASE_INSENSITIVE);
    private static final Pattern KEGG = Pattern.compile("^(hsa|mmu|rno)\\d+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern WP = Pattern.compile("^WP\\d+$", Pattern.CASE_INSENSITIVE);

    /** Absolute path → loaded catalog (avoids re-parsing large MSigDB JSON each phase). */
    private static final ConcurrentHashMap<String, MsigdbResolver> CACHE = new ConcurrentHashMap<>();

    private final Map<String, CatalogEntry> byName = new HashMap<>();
    private final Map<String, CatalogEntry> bySource = new HashMap<>();
    private final String path;

    public MsigdbResolver() {
        this.path = "(heuristic)";
    }

    public MsigdbResolver(File jsonFile) throws Exception {
        this.path = jsonFile.getAbsolutePath();
        loadCatalog(jsonFile);
    }

    public static MsigdbResolver loadOptional(String msigdbPath) {
        if (msigdbPath == null || msigdbPath.isBlank()) {
            return new MsigdbResolver();
        }
        File f = new File(msigdbPath.trim());
        if (!f.isFile()) {
            klog.warn("MSigDB JSON not found at {}; using accession heuristics only", msigdbPath);
            return new MsigdbResolver();
        }
        String key = f.getAbsolutePath();
        MsigdbResolver cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            MsigdbResolver loaded = new MsigdbResolver(f);
            MsigdbResolver raced = CACHE.putIfAbsent(key, loaded);
            return raced != null ? raced : loaded;
        } catch (Exception ex) {
            klog.warn("Failed to load MSigDB {}: {}", msigdbPath, ex.toString());
            return new MsigdbResolver();
        }
    }

    public String getPath() {
        return path;
    }

    public ResolvedSource resolve(String setId, String setName) {
        for (String raw : new String[] { setId, setName }) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            CatalogEntry hit = lookup(raw.trim());
            if (hit != null) {
                String accession = hit.exactSource != null && !hit.exactSource.isBlank()
                        ? hit.exactSource : raw.trim();
                return new ResolvedSource(setId, setName != null ? setName : hit.name,
                        platformOf(hit), accession, hit.collection, hit.name,
                        hit.externalUrl, hit.msigdbUrl);
            }
        }
        for (String raw : new String[] { setId, setName }) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String text = raw.trim();
            Platform platform = platformFromAccession(text);
            if (platform == Platform.UNKNOWN) {
                platform = platformFromName(text);
            }
            if (platform != Platform.UNKNOWN) {
                String accession = extractAccession(text, platform);
                return new ResolvedSource(setId, setName, platform, accession, "",
                        null, defaultUrl(platform, accession), "");
            }
        }
        return null;
    }

    private CatalogEntry lookup(String text) {
        String key = text.toUpperCase(Locale.ROOT);
        CatalogEntry hit = byName.get(key);
        if (hit != null) {
            return hit;
        }
        hit = bySource.get(key);
        if (hit != null) {
            return hit;
        }
        String alt = key.replace(':', '_');
        hit = byName.get(alt);
        if (hit != null) {
            return hit;
        }
        return bySource.get(alt);
    }

    @SuppressWarnings("unchecked")
    private void loadCatalog(File jsonFile) throws Exception {
        Object parsed = new JSONParser().parse(new FileReader(jsonFile, StandardCharsets.UTF_8));
        if (!(parsed instanceof JSONObject)) {
            throw new IllegalArgumentException("MSigDB JSON must be an object keyed by gene-set name");
        }
        JSONObject root = (JSONObject) parsed;
        for (Object nameObj : root.keySet()) {
            Object raw = root.get(nameObj);
            if (!(raw instanceof JSONObject)) {
                continue;
            }
            JSONObject rec = (JSONObject) raw;
            CatalogEntry meta = new CatalogEntry();
            meta.name = String.valueOf(nameObj);
            meta.collection = String.valueOf(rec.getOrDefault("collection", ""));
            meta.systematicName = String.valueOf(rec.getOrDefault("systematicName", ""));
            meta.exactSource = String.valueOf(rec.getOrDefault("exactSource", "")).trim();
            meta.externalUrl = String.valueOf(rec.getOrDefault("externalDetailsURL", ""));
            meta.msigdbUrl = String.valueOf(rec.getOrDefault("msigdbURL", ""));
            byName.put(meta.name.toUpperCase(Locale.ROOT), meta);
            if (!meta.systematicName.isBlank()) {
                byName.put(meta.systematicName.toUpperCase(Locale.ROOT), meta);
            }
            if (!meta.exactSource.isBlank()) {
                bySource.put(meta.exactSource.toUpperCase(Locale.ROOT), meta);
                bySource.put(meta.exactSource.toUpperCase(Locale.ROOT).replace(':', '_'), meta);
            }
        }
        klog.info("Loaded MSigDB catalog {} ({} sets)", path, byName.size());
    }

    public static Platform platformFromAccession(String accession) {
        String text = accession == null ? "" : accession.trim();
        if (text.isEmpty()) {
            return Platform.UNKNOWN;
        }
        if (REACTOME.matcher(text).find()) {
            return Platform.REACTOME;
        }
        if (GO.matcher(text).find()) {
            return Platform.GO;
        }
        if (HPO.matcher(text).find()) {
            return Platform.HPO;
        }
        if (KEGG.matcher(text).find()) {
            return Platform.KEGG;
        }
        if (WP.matcher(text).find()) {
            return Platform.WIKIPATHWAYS;
        }
        return platformFromName(text);
    }

    public static Platform platformFromName(String text) {
        String upper = text.toUpperCase(Locale.ROOT);
        if (upper.startsWith("REACTOME_") || upper.contains("R-HSA-") || upper.contains("R-MMU-")
                || upper.contains("R-RNO-")) {
            return Platform.REACTOME;
        }
        if (upper.startsWith("GOBP_") || upper.startsWith("GOMF_") || upper.startsWith("GOCC_")
                || upper.startsWith("GO_") || upper.contains("GO:")) {
            return Platform.GO;
        }
        if (upper.startsWith("HP_") || upper.contains("HP:")) {
            return Platform.HPO;
        }
        if (upper.startsWith("KEGG_") || upper.contains("HSA") || upper.contains("MMU")
                || Pattern.compile("\\b(hsa|mmu|rno)\\d+", Pattern.CASE_INSENSITIVE).matcher(text).find()) {
            return Platform.KEGG;
        }
        if (upper.startsWith("WP_")) {
            return Platform.WIKIPATHWAYS;
        }
        return Platform.UNKNOWN;
    }

    static String extractAccession(String text, Platform platform) {
        if (platform == Platform.REACTOME) {
            java.util.regex.Matcher m = Pattern.compile("(R-(?:HSA|MMU|RNO)-\\d+)", Pattern.CASE_INSENSITIVE)
                    .matcher(text);
            if (m.find()) {
                return m.group(1).toUpperCase(Locale.ROOT);
            }
        }
        if (platform == Platform.GO) {
            java.util.regex.Matcher m = Pattern.compile("(GO:\\d+)", Pattern.CASE_INSENSITIVE).matcher(text);
            if (m.find()) {
                return m.group(1).toUpperCase(Locale.ROOT);
            }
        }
        if (platform == Platform.HPO) {
            java.util.regex.Matcher m = Pattern.compile("(HP:\\d+)", Pattern.CASE_INSENSITIVE).matcher(text);
            if (m.find()) {
                return m.group(1).toUpperCase(Locale.ROOT);
            }
        }
        if (platform == Platform.KEGG) {
            java.util.regex.Matcher m = Pattern.compile("((?:hsa|mmu|rno)\\d+)", Pattern.CASE_INSENSITIVE)
                    .matcher(text);
            if (m.find()) {
                return m.group(1).toLowerCase(Locale.ROOT);
            }
        }
        return text.trim();
    }

    private static Platform platformOf(CatalogEntry meta) {
        Map<String, Platform> collections = new LinkedHashMap<>();
        collections.put("C2:CP:REACTOME", Platform.REACTOME);
        collections.put("C2:CP:KEGG_LEGACY", Platform.KEGG);
        collections.put("C2:CP:KEGG_MEDICUS", Platform.KEGG);
        collections.put("C2:CP:WIKIPATHWAYS", Platform.WIKIPATHWAYS);
        collections.put("C5:GO:BP", Platform.GO);
        collections.put("C5:GO:MF", Platform.GO);
        collections.put("C5:GO:CC", Platform.GO);
        collections.put("C5:HPO", Platform.HPO);
        if (collections.containsKey(meta.collection)) {
            return collections.get(meta.collection);
        }
        return platformFromAccession(meta.exactSource);
    }

    private static String defaultUrl(Platform platform, String accession) {
        return switch (platform) {
            case REACTOME -> "https://reactome.org/content/detail/" + accession;
            case GO -> "https://amigo.geneontology.org/amigo/term/" + accession;
            case HPO -> "https://hpo.jax.org/app/browse/term/" + accession;
            case KEGG -> "https://www.kegg.jp/pathway/" + accession;
            case WIKIPATHWAYS -> "https://www.wikipathways.org/instance/" + accession;
            default -> "";
        };
    }

    public enum Platform {
        REACTOME, KEGG, GO, HPO, WIKIPATHWAYS, BIOCARTA, PID, UNKNOWN
    }

    public static final class ResolvedSource {
        public final String setId;
        public final String setName;
        public final Platform platform;
        public final String accession;
        public final String collection;
        public final String msigdbName;
        public final String externalUrl;
        public final String msigdbUrl;

        public ResolvedSource(String setId, String setName, Platform platform, String accession,
                String collection, String msigdbName, String externalUrl, String msigdbUrl) {
            this.setId = setId;
            this.setName = setName;
            this.platform = platform;
            this.accession = accession;
            this.collection = collection;
            this.msigdbName = msigdbName;
            this.externalUrl = externalUrl;
            this.msigdbUrl = msigdbUrl;
        }
    }

    private static final class CatalogEntry {
        String name;
        String collection = "";
        String systematicName = "";
        String exactSource = "";
        String externalUrl = "";
        String msigdbUrl = "";
    }
}
