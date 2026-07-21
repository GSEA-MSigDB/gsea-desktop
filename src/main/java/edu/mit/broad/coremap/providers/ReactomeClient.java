/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap.providers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.coremap.CoreMapConstants;
import edu.mit.broad.coremap.CoreMapOrganism;
import edu.mit.broad.coremap.CoreMapTypes.InteractomeEdge;

/** Reactome ContentService — reaction-derived mechanism edges among seed genes. */
public final class ReactomeClient {

    private static final Logger klog = LoggerFactory.getLogger(ReactomeClient.class);
    private static final String BASE = "https://reactome.org/ContentService";
    private static final Pattern GENE_TOKEN = Pattern.compile("\\b([A-Z][A-Z0-9]{1,14})\\b");
    private static final int REACTION_FETCH_MAX = 40;
    private static final Set<String> SKIP = Set.of(
            "ATP", "ADP", "GTP", "GDP", "AMP", "DNA", "RNA", "COMPLEX", "SET", "HSA", "MMU", "RNO",
            "R", "AND", "OR", "THE", "WITH", "FROM", "FOR");

    private ReactomeClient() {
    }

    public static List<InteractomeEdge> fetchMechanismEdges(List<String> pathwayIds, Set<String> seedGenes) {
        Set<String> seeds = upper(seedGenes);
        List<String> ids = pathwayIds.stream()
                .filter(id -> id != null && CoreMapOrganism.isReactomeAccession(id))
                .map(id -> id.toUpperCase(Locale.ROOT))
                .distinct().sorted().toList();
        if (ids.isEmpty() || seeds.size() < 2) {
            return List.of();
        }
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(4, ids.size()));
        try {
            List<Future<List<InteractomeEdge>>> futures = new ArrayList<>();
            for (String pid : ids) {
                futures.add(pool.submit(() -> edgesForPathway(pid, seeds, ids.size())));
            }
            List<InteractomeEdge> out = new ArrayList<>();
            for (Future<List<InteractomeEdge>> f : futures) {
                try {
                    out.addAll(f.get());
                } catch (Exception ignored) {
                }
            }
            klog.info("Reactome: {} pathways → {} edges", ids.size(), out.size());
            return out;
        } finally {
            pool.shutdownNow();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<InteractomeEdge> edgesForPathway(String pathwayId, Set<String> seeds, int pathwayCount) {
        try {
            String body = ProviderHttp.get(BASE + "/data/pathway/"
                    + java.net.URLEncoder.encode(pathwayId, java.nio.charset.StandardCharsets.UTF_8)
                    + "/containedEvents", 60_000, 1);
            Object parsed = new JSONParser().parse(body);
            if (!(parsed instanceof JSONArray)) {
                return List.of();
            }
            List<JSONObject> reactions = new ArrayList<>();
            for (Object ev : (JSONArray) parsed) {
                if (!(ev instanceof JSONObject)) {
                    continue;
                }
                JSONObject rec = (JSONObject) ev;
                String schema = String.valueOf(rec.getOrDefault("schemaClass",
                        rec.getOrDefault("className", "")));
                if (schema.equals("Reaction") || schema.equals("BlackBoxEvent")
                        || schema.equals("Polymerisation") || schema.equals("Depolymerisation")
                        || schema.isEmpty() || schema.equals("null")) {
                    reactions.add(rec);
                }
            }
            if (reactions.isEmpty()) {
                for (Object ev : (JSONArray) parsed) {
                    if (ev instanceof JSONObject && ((JSONObject) ev).get("stId") != null) {
                        reactions.add((JSONObject) ev);
                    }
                }
            }
            reactions.sort(Comparator
                    .comparingInt((JSONObject e) -> -seedHitCount(e, seeds))
                    .thenComparing(e -> String.valueOf(e.getOrDefault("stId", ""))));
            int quota = reactomeReactionsPerPathway(seeds.size(), pathwayCount);
            List<JSONObject> selected = reactions.subList(0, Math.min(quota, reactions.size()));
            if (selected.size() <= 1) {
                List<InteractomeEdge> edges = new ArrayList<>();
                for (JSONObject event : selected) {
                    edges.addAll(reactionEdges(event, seeds, pathwayId));
                }
                return edges;
            }
            // Participant GETs dominate latency — overlap them within the pathway.
            ExecutorService reactionPool = Executors.newFixedThreadPool(Math.min(6, selected.size()));
            try {
                List<Future<List<InteractomeEdge>>> futs = new ArrayList<>();
                for (JSONObject event : selected) {
                    futs.add(reactionPool.submit(() -> reactionEdges(event, seeds, pathwayId)));
                }
                List<InteractomeEdge> edges = new ArrayList<>();
                for (Future<List<InteractomeEdge>> fut : futs) {
                    try {
                        edges.addAll(fut.get());
                    } catch (Exception ignored) {
                        // soft-fail individual reactions
                    }
                }
                return edges;
            } finally {
                reactionPool.shutdownNow();
            }
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static int reactomeReactionsPerPathway(int seedCount, int pathwayCount) {
        int totalWork = (int) CoreMapConstants.clamp(Math.round(60 + seedCount * 1.5), 60, 240);
        return (int) CoreMapConstants.clamp(Math.round((double) totalWork / Math.max(1, pathwayCount)),
                8, REACTION_FETCH_MAX);
    }

    private static int seedHitCount(JSONObject event, Set<String> seeds) {
        String text = String.valueOf(event.getOrDefault("displayName", "")).toUpperCase(Locale.ROOT);
        int hits = 0;
        for (String seed : seeds) {
            if (!seed.isEmpty() && text.contains(seed)) {
                hits++;
            }
        }
        return hits;
    }

    @SuppressWarnings("unchecked")
    private static List<InteractomeEdge> reactionEdges(JSONObject event, Set<String> seeds, String pathwayId) {
        String stId = String.valueOf(event.getOrDefault("stId", ""));
        if (stId.isBlank() || "null".equals(stId)) {
            return List.of();
        }
        String display = String.valueOf(event.getOrDefault("displayName", stId));
        String definition = String.valueOf(event.getOrDefault("definition", ""));
        Set<String> genes = genesFromParticipants(stId);
        genes.retainAll(seeds);
        if (genes.size() < 2) {
            genes = genesFromText(display);
            genes.retainAll(seeds);
        }
        if (genes.size() < 2) {
            return List.of();
        }
        Effect effect = effectFromReaction(display, definition);
        List<String> ordered = new ArrayList<>(genes);
        ordered.sort(String::compareTo);
        String hub = ordered.get(0);
        List<InteractomeEdge> out = new ArrayList<>();
        for (int i = 1; i < ordered.size(); i++) {
            String partner = ordered.get(i);
            String source = effect.sign >= 0 ? hub : partner;
            String target = effect.sign >= 0 ? partner : hub;
            InteractomeEdge e = new InteractomeEdge(source, target, 0.72);
            e.directed = true;
            e.effect = effect.effect;
            e.mechanism = (effect.mechanism + ": " + display);
            if (e.mechanism.length() > 180) {
                e.mechanism = e.mechanism.substring(0, 180);
            }
            e.evidence = "Reactome:" + stId;
            e.provider = "reactome";
            e.channels = new LinkedHashMap<>();
            e.channels.put("sign", (double) effect.sign);
            e.pathwayId = pathwayId;
            out.add(e);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> genesFromParticipants(String reactionId) {
        Set<String> genes = new HashSet<>();
        try {
            String body = ProviderHttp.get(BASE + "/data/participants/"
                    + java.net.URLEncoder.encode(reactionId, java.nio.charset.StandardCharsets.UTF_8)
                    + "/participatingPhysicalEntities", 60_000, 1);
            Object parsed = new JSONParser().parse(body);
            if (!(parsed instanceof JSONArray)) {
                return genes;
            }
            for (Object entity : (JSONArray) parsed) {
                if (!(entity instanceof JSONObject)) {
                    continue;
                }
                JSONObject rec = (JSONObject) entity;
                Object names = rec.get("name");
                if (names instanceof JSONArray) {
                    for (Object n : (JSONArray) names) {
                        String text = String.valueOf(n).trim();
                        if (!text.contains(" ") && text.toUpperCase(Locale.ROOT).equals(text)) {
                            genes.add(text.toUpperCase(Locale.ROOT));
                        } else {
                            genes.addAll(genesFromText(text));
                        }
                    }
                }
                genes.addAll(genesFromText(String.valueOf(rec.getOrDefault("displayName", ""))));
            }
        } catch (Exception ignored) {
        }
        return genes;
    }

    private static Set<String> genesFromText(String text) {
        Set<String> genes = new HashSet<>();
        Matcher m = GENE_TOKEN.matcher(text == null ? "" : text);
        while (m.find()) {
            String token = m.group(1);
            if (!SKIP.contains(token) && token.length() >= 2) {
                genes.add(token);
            }
        }
        return genes;
    }

    private static Effect effectFromReaction(String displayName, String definition) {
        String blob = (displayName + " " + definition).toLowerCase(Locale.ROOT);
        if (blob.contains("inhib") || blob.contains("degrad") || blob.contains("repress")
                || blob.contains("negative")) {
            return new Effect("down-regulates", -1, "inhibition");
        }
        if (blob.contains("activ") || blob.contains("phosphoryl") || blob.contains("positive")
                || blob.contains("stimul")) {
            return new Effect("up-regulates", 1, "activation");
        }
        if (blob.contains("transcri")) {
            return new Effect("up-regulates quantity by expression", 1, "transcriptional regulation");
        }
        return new Effect("regulates", 0, "reaction");
    }

    private static Set<String> upper(Set<String> in) {
        Set<String> out = new HashSet<>();
        for (String s : in) {
            if (s != null) {
                out.add(s.toUpperCase(Locale.ROOT));
            }
        }
        return out;
    }

    private static final class Effect {
        final String effect;
        final int sign;
        final String mechanism;

        Effect(String effect, int sign, String mechanism) {
            this.effect = effect;
            this.sign = sign;
            this.mechanism = mechanism;
        }
    }
}
