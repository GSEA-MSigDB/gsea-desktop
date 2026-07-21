/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap.providers;

import edu.mit.broad.coremap.CoreMapOrganism;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gene symbol → UniProt accession: bundled cache plus live UniProt REST for misses.
 */
public final class UniProtMapper {

    private static final Logger klog = LoggerFactory.getLogger(UniProtMapper.class);
    private static final String UNIPROT_SEARCH = "https://rest.uniprot.org/uniprotkb/search";

    private static final Map<String, String> BUNDLE = loadBundle();
    private static final Map<String, String> RUNTIME = Collections.synchronizedMap(new HashMap<>());

    private UniProtMapper() {
    }

    public static Map<String, String> mapSymbolsToUniprot(Iterable<String> symbols) throws Exception {
        return mapSymbolsToUniprot(symbols, 9606);
    }

    public static int parseOrganismId(String organism) {
        return CoreMapOrganism.taxonId(organism);
    }

    public static Map<String, String> mapSymbolsToUniprot(Iterable<String> symbols, int organismId)
            throws Exception {
        Set<String> cleaned = new HashSet<>();
        for (String s : symbols) {
            if (s != null && !s.isBlank()) {
                cleaned.add(s.trim().toUpperCase(Locale.ROOT));
            }
        }
        List<String> ordered = new ArrayList<>(cleaned);
        Collections.sort(ordered);
        if (ordered.isEmpty()) {
            return Map.of();
        }

        Map<String, String> mapping = new LinkedHashMap<>();
        List<String> pending = new ArrayList<>();
        for (String symbol : ordered) {
            String hit = lookupLocal(symbol);
            if (hit != null && !hit.isBlank()) {
                mapping.put(symbol, hit);
            } else if (hit == null) {
                // not known as missing
                pending.add(symbol);
            }
            // empty string in RUNTIME means previously looked up with no hit — skip
        }
        if (!pending.isEmpty()) {
            batchLookup(pending, organismId, mapping);
        }
        return mapping;
    }

    public static Map<String, String> reverseMap(Map<String, String> symbolToAcc) {
        Map<String, String> rev = new HashMap<>();
        for (Map.Entry<String, String> e : symbolToAcc.entrySet()) {
            if (e.getValue() != null && !e.getValue().isBlank()) {
                rev.putIfAbsent(e.getValue(), e.getKey());
            }
        }
        return rev;
    }

    private static String lookupLocal(String symbol) {
        synchronized (RUNTIME) {
            if (RUNTIME.containsKey(symbol)) {
                String v = RUNTIME.get(symbol);
                return v == null || v.isBlank() ? "" : v;
            }
        }
        String bundled = BUNDLE.get(symbol);
        return bundled;
    }

    @SuppressWarnings("unchecked")
    private static void batchLookup(List<String> pending, int organismId, Map<String, String> mapping)
            throws Exception {
        final int batchSize = 40;
        // Cap concurrency to stay polite to UniProt while overlapping batch latency.
        final int workers = Math.min(4, Math.max(1, (pending.size() + batchSize - 1) / batchSize));
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        try {
            Map<String, String> concurrent = new ConcurrentHashMap<>();
            List<CompletableFuture<Void>> futs = new ArrayList<>();
            for (int i = 0; i < pending.size(); i += batchSize) {
                List<String> chunk = List.copyOf(pending.subList(i, Math.min(i + batchSize, pending.size())));
                futs.add(CompletableFuture.runAsync(() -> {
                    try {
                        lookupChunk(chunk, organismId, concurrent);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }, pool));
            }
            for (CompletableFuture<Void> fut : futs) {
                try {
                    fut.join();
                } catch (RuntimeException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof Exception e) {
                        throw e;
                    }
                    throw ex;
                }
            }
            mapping.putAll(concurrent);
        } finally {
            pool.shutdownNow();
        }
    }

    @SuppressWarnings("unchecked")
    private static void lookupChunk(List<String> chunk, int organismId, Map<String, String> mapping)
            throws Exception {
        StringBuilder geneClause = new StringBuilder();
        for (int j = 0; j < chunk.size(); j++) {
            if (j > 0) {
                geneClause.append(" OR ");
            }
            geneClause.append("(gene_exact:").append(chunk.get(j)).append(')');
        }
        String query = "(" + geneClause + ") AND (organism_id:" + organismId + ") AND (reviewed:true)";
        String url = UNIPROT_SEARCH
                + "?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&fields=accession,gene_names"
                + "&format=json"
                + "&size=" + Math.max(chunk.size() * 3, 50);
        String body;
        try {
            body = ProviderHttp.get(url, 30_000, 2);
        } catch (Exception ex) {
            klog.warn("UniProt lookup failed for batch of {}: {}", chunk.size(), ex.toString());
            for (String symbol : chunk) {
                RUNTIME.put(symbol, "");
            }
            return;
        }
        Set<String> wanted = new HashSet<>(chunk);
        Set<String> found = new HashSet<>();
        Object parsed = new JSONParser().parse(body);
        if (parsed instanceof JSONObject) {
            Object results = ((JSONObject) parsed).get("results");
            if (results instanceof JSONArray) {
                for (Object entryObj : (JSONArray) results) {
                    if (!(entryObj instanceof JSONObject)) {
                        continue;
                    }
                    JSONObject entry = (JSONObject) entryObj;
                    Object accObj = entry.get("primaryAccession");
                    if (accObj == null) {
                        continue;
                    }
                    String accession = accObj.toString();
                    Object genesObj = entry.get("genes");
                    if (!(genesObj instanceof JSONArray)) {
                        continue;
                    }
                    for (Object gObj : (JSONArray) genesObj) {
                        if (!(gObj instanceof JSONObject)) {
                            continue;
                        }
                        Object geneName = ((JSONObject) gObj).get("geneName");
                        if (!(geneName instanceof JSONObject)) {
                            continue;
                        }
                        Object nameVal = ((JSONObject) geneName).get("value");
                        if (nameVal == null) {
                            continue;
                        }
                        String symbol = nameVal.toString().toUpperCase(Locale.ROOT);
                        if (wanted.contains(symbol) && mapping.putIfAbsent(symbol, accession) == null) {
                            RUNTIME.put(symbol, accession);
                            found.add(symbol);
                        }
                    }
                }
            }
        }
        for (String symbol : chunk) {
            if (!found.contains(symbol) && !mapping.containsKey(symbol)) {
                RUNTIME.put(symbol, "");
            }
        }
    }

    private static Map<String, String> loadBundle() {
        Map<String, String> map = new HashMap<>();
        try (InputStream in = UniProtMapper.class.getResourceAsStream(
                "/edu/mit/broad/coremap/data/uniprot_symbol_cache.json")) {
            if (in == null) {
                return map;
            }
            Object parsed = new JSONParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8));
            if (parsed instanceof JSONObject) {
                JSONObject obj = (JSONObject) parsed;
                for (Object key : obj.keySet()) {
                    Object val = obj.get(key);
                    if (key != null && val != null && !val.toString().isBlank()) {
                        map.put(key.toString().toUpperCase(Locale.ROOT), val.toString());
                    }
                }
            }
        } catch (Exception ex) {
            klog.warn("Could not load UniProt symbol cache: {}", ex.toString());
        }
        return Collections.unmodifiableMap(map);
    }
}
