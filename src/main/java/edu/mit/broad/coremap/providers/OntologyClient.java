/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap.providers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.coremap.CoreMapConstants;
import edu.mit.broad.coremap.CoreMapOrganism;
import edu.mit.broad.coremap.CoreMapTypes.InteractomeEdge;

/** HPO / GO co-annotation support edges (used when interactome source is {@code none}). */
public final class OntologyClient {

    private static final Logger klog = LoggerFactory.getLogger(OntologyClient.class);
    private static final String HPO_ANNOTATION = "https://ontology.jax.org/api/network/annotation";
    private static final String QUICKGO_SEARCH = "https://www.ebi.ac.uk/QuickGO/services/annotation/search";

    private OntologyClient() {
    }

    public static int comembershipNeighborCount(int setSize) {
        if (setSize < 2) {
            return 0;
        }
        return (int) CoreMapConstants.clamp(Math.round(Math.log(setSize) / Math.log(2)), 2, 6);
    }

    public static List<InteractomeEdge> fetchHpoEdges(List<String> termIds, Set<String> seedGenes) {
        return fetchTermEdges(termIds, seedGenes, "hpo", true, CoreMapOrganism.taxonId(CoreMapOrganism.HUMAN));
    }

    public static List<InteractomeEdge> fetchGoEdges(List<String> termIds, Set<String> seedGenes) {
        return fetchGoEdges(termIds, seedGenes, CoreMapOrganism.taxonId(CoreMapOrganism.HUMAN));
    }

    public static List<InteractomeEdge> fetchGoEdges(List<String> termIds, Set<String> seedGenes, int taxonId) {
        return fetchTermEdges(termIds, seedGenes, "go", false, taxonId);
    }

    private static List<InteractomeEdge> fetchTermEdges(List<String> termIds, Set<String> seedGenes,
            String provider, boolean hpo, int taxonId) {
        List<String> ids = termIds.stream().filter(t -> t != null && !t.isBlank()).distinct().sorted().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(4, ids.size()));
        try {
            List<Future<List<InteractomeEdge>>> futures = new ArrayList<>();
            for (String termId : ids) {
                futures.add(pool.submit(() -> {
                    Set<String> annotated = hpo ? fetchHpoGenes(termId) : fetchGoGenes(termId, taxonId);
                    return coannotationEdges(termId, annotated, seedGenes, provider,
                            hpo ? "HPO co-annotation" : "GO co-annotation");
                }));
            }
            List<InteractomeEdge> out = new ArrayList<>();
            for (Future<List<InteractomeEdge>> f : futures) {
                try {
                    out.addAll(f.get());
                } catch (Exception ignored) {
                }
            }
            klog.info("{}: {} terms → {} edges (taxon={})", provider.toUpperCase(Locale.ROOT),
                    ids.size(), out.size(), taxonId);
            return out;
        } finally {
            pool.shutdownNow();
        }
    }

    @SuppressWarnings("unchecked")
    static Set<String> fetchHpoGenes(String termId) {
        Set<String> genes = new HashSet<>();
        String tid = termId.trim();
        if (!tid.toUpperCase(Locale.ROOT).startsWith("HP:")) {
            return genes;
        }
        try {
            String body = ProviderHttp.get(HPO_ANNOTATION + "/"
                    + URLEncoder.encode(tid, StandardCharsets.UTF_8), 60_000, 1);
            Object parsed = new JSONParser().parse(body);
            if (!(parsed instanceof JSONObject)) {
                return genes;
            }
            Object arr = ((JSONObject) parsed).get("genes");
            if (arr instanceof JSONArray) {
                for (Object item : (JSONArray) arr) {
                    if (item instanceof JSONObject) {
                        Object name = ((JSONObject) item).get("name");
                        if (name != null) {
                            genes.add(name.toString().toUpperCase(Locale.ROOT));
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return genes;
    }

    @SuppressWarnings("unchecked")
    static Set<String> fetchGoGenes(String termId) {
        return fetchGoGenes(termId, CoreMapOrganism.taxonId(CoreMapOrganism.HUMAN));
    }

    @SuppressWarnings("unchecked")
    static Set<String> fetchGoGenes(String termId, int taxonId) {
        Set<String> genes = new HashSet<>();
        String tid = termId.trim();
        if (!tid.toUpperCase(Locale.ROOT).startsWith("GO:")) {
            return genes;
        }
        try {
            String url = QUICKGO_SEARCH
                    + "?goId=" + URLEncoder.encode(tid, StandardCharsets.UTF_8)
                    + "&taxonId=" + taxonId
                    + "&aspect=" + URLEncoder.encode(
                            "biological_process,molecular_function,cellular_component", StandardCharsets.UTF_8)
                    + "&limit=200";
            String body = ProviderHttp.get(url, 60_000, 1);
            Object parsed = new JSONParser().parse(body);
            if (!(parsed instanceof JSONObject)) {
                return genes;
            }
            Object results = ((JSONObject) parsed).get("results");
            if (!(results instanceof JSONArray)) {
                return genes;
            }
            for (Object rowObj : (JSONArray) results) {
                if (!(rowObj instanceof JSONObject)) {
                    continue;
                }
                JSONObject row = (JSONObject) rowObj;
                for (String key : List.of("symbol", "geneProductName", "name")) {
                    Object value = row.get(key);
                    if (value instanceof String) {
                        String s = (String) value;
                        if (!s.isBlank() && !s.contains(" ") && s.length() <= 15) {
                            genes.add(s.toUpperCase(Locale.ROOT));
                            break;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return genes;
    }

    static List<InteractomeEdge> coannotationEdges(String termId, Set<String> annotatedGenes,
            Set<String> seedGenes, String provider, String mechanism) {
        Set<String> seeds = new HashSet<>();
        for (String s : seedGenes) {
            if (s != null) {
                seeds.add(s.toUpperCase(Locale.ROOT));
            }
        }
        List<String> overlap = new ArrayList<>();
        for (String g : annotatedGenes) {
            String u = g.toUpperCase(Locale.ROOT);
            if (seeds.contains(u)) {
                overlap.add(u);
            }
        }
        overlap = overlap.stream().distinct().sorted().toList();
        List<InteractomeEdge> edges = new ArrayList<>();
        if (overlap.size() < 2) {
            return edges;
        }
        int neighbors = comembershipNeighborCount(overlap.size());
        for (int i = 0; i < overlap.size(); i++) {
            String source = overlap.get(i);
            for (int j = i + 1; j < overlap.size() && j <= i + neighbors; j++) {
                String target = overlap.get(j);
                edges.add(makeEdge(source, target, termId, provider, mechanism));
                edges.add(makeEdge(target, source, termId, provider, mechanism));
            }
        }
        return edges;
    }

    private static InteractomeEdge makeEdge(String source, String target, String termId,
            String provider, String mechanism) {
        InteractomeEdge e = new InteractomeEdge(source, target, 0.45);
        e.directed = false;
        e.effect = "associates";
        e.mechanism = mechanism;
        e.evidence = provider + ":" + termId;
        e.provider = provider;
        e.support = "comembership";
        e.channels = new LinkedHashMap<>();
        e.channels.put("sign", 0.0);
        e.channels.put("comembership", 1.0);
        return e;
    }
}
