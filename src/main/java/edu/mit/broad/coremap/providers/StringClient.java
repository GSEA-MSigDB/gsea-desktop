/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap.providers;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.coremap.CoreMapOrganism;
import edu.mit.broad.coremap.CoreMapTypes.InteractomeEdge;
import edu.mit.broad.coremap.CoreMapTypes.StringMode;
import edu.mit.broad.coremap.IntegrationOptions;
import edu.mit.broad.coremap.providers.EvidenceQuality.StringEffect;

/** STRING REST client — CoreMap-parity network fetch (direct HTTP). */
public final class StringClient {

    private static final Logger klog = LoggerFactory.getLogger(StringClient.class);
    private static final String STRING_API = "https://string-db.org/api/tsv/network";
    private static final int POST_THRESHOLD = 80;
    private static final String[] REGULATION_CANDIDATES = {
            "regulation", "regulation_type", "regulatory_effect", "effect", "sign", "mode", "action", "relationship"
    };

    private StringClient() {
    }

    public static List<InteractomeEdge> fetch(List<String> geneSymbols, IntegrationOptions opts)
            throws IOException {
        if (geneSymbols == null || geneSymbols.isEmpty()) {
            return List.of();
        }
        List<String> query = new ArrayList<>();
        for (String s : geneSymbols) {
            if (s != null && !s.isBlank()) {
                query.add(s.toUpperCase(Locale.ROOT));
            }
        }
        query.sort(String::compareTo);
        int requiredScore = Math.max(150, (int) Math.round(opts.minInteractionScore * 1000));
        int addNodes = Math.max(0, opts.includeExtraNeighbors ? opts.neighborLimit : 0);
        StringMode mode = opts.stringMode != null ? opts.stringMode : StringMode.INTEGRATED;

        List<String> networkTypes = new ArrayList<>();
        if (mode == StringMode.INTEGRATED) {
            networkTypes.add("functional");
            networkTypes.add("regulatory");
        } else if (mode == StringMode.PHYSICAL) {
            networkTypes.add("physical");
        } else {
            networkTypes.add(mode.wire());
        }

        List<InteractomeEdge> all = new ArrayList<>();
        int species = parseSpecies(opts);
        if (networkTypes.size() == 1) {
            all.addAll(fetchMode(query, networkTypes.get(0), requiredScore, addNodes, species));
        } else {
            // INTEGRATED: functional + regulatory are independent STRING calls.
            List<CompletableFuture<List<InteractomeEdge>>> futs = new ArrayList<>();
            for (String networkType : networkTypes) {
                String nt = networkType;
                futs.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return fetchMode(query, nt, requiredScore, addNodes, species);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }));
            }
            for (CompletableFuture<List<InteractomeEdge>> fut : futs) {
                try {
                    all.addAll(fut.join());
                } catch (RuntimeException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof IOException ioe) {
                        throw ioe;
                    }
                    throw ex;
                }
            }
        }
        klog.info("STRING: {} edges for {} genes (mode={}, species={}, required_score={})",
                all.size(), query.size(), mode.wire(), species, requiredScore);
        return all;
    }

    private static int parseSpecies(IntegrationOptions opts) {
        return CoreMapOrganism.taxonId(opts != null ? opts.signorOrganism : null);
    }

    private static List<InteractomeEdge> fetchMode(List<String> querySymbols, String networkType,
            int requiredScore, int addNodes, int species) throws IOException {
        String identifiers = String.join("\r", querySymbols);
        String form = "identifiers=" + URLEncoder.encode(identifiers, StandardCharsets.UTF_8)
                + "&species=" + species
                + "&required_score=" + requiredScore
                + "&network_type=" + URLEncoder.encode(networkType, StandardCharsets.UTF_8)
                + "&caller_identity=gsea-desktop-coremap";
        if (addNodes > 0) {
            form += "&add_nodes=" + addNodes;
        }
        String body = querySymbols.size() >= POST_THRESHOLD
                ? ProviderHttp.postForm(STRING_API, form)
                : ProviderHttp.get(STRING_API + "?" + form, 90_000, 2);
        return parseNetworkTsv(body, networkType);
    }

    static List<InteractomeEdge> parseNetworkTsv(String body, String networkType) {
        List<InteractomeEdge> edges = new ArrayList<>();
        if (body == null || body.isBlank()) {
            return edges;
        }
        String[] lines = body.split("\\r?\\n");
        if (lines.length < 2) {
            return edges;
        }
        String[] header = lines[0].split("\t");
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            idx.put(header[i], i);
        }
        String regulationCol = pickRegulationColumn(idx);
        for (int li = 1; li < lines.length; li++) {
            String[] row = lines[li].split("\t", -1);
            String source = cell(row, idx, "preferredName_A").toUpperCase(Locale.ROOT).trim();
            String target = cell(row, idx, "preferredName_B").toUpperCase(Locale.ROOT).trim();
            if (source.isEmpty() || target.isEmpty() || source.equals(target)) {
                continue;
            }
            double score;
            try {
                score = Double.parseDouble(cell(row, idx, "score"));
            } catch (NumberFormatException ex) {
                continue;
            }
            if (score > 1) {
                score = score / 1000.0;
            }
            Map<String, Double> channels = new LinkedHashMap<>();
            for (String key : List.of("nscore", "fscore", "pscore", "ascore", "escore", "dscore", "tscore")) {
                try {
                    double n = Double.parseDouble(cell(row, idx, key));
                    channels.put(key, Double.isFinite(n) ? n : 0.0);
                } catch (Exception ignored) {
                    channels.put(key, 0.0);
                }
            }
            String regulationRaw = regulationCol != null ? cell(row, idx, regulationCol) : "";
            StringEffect resolved = EvidenceQuality.resolveStringEffect(networkType, regulationRaw);
            channels.put("sign", (double) resolved.sign);

            InteractomeEdge raw = new InteractomeEdge(source, target, score);
            raw.directed = resolved.directed;
            raw.effect = resolved.effect;
            raw.mechanism = networkType;
            raw.evidence = "STRING";
            raw.provider = "string";
            raw.channels = channels;
            edges.add(EvidenceQuality.enrichStringEdge(raw, networkType));
        }
        return edges;
    }

    private static String pickRegulationColumn(Map<String, Integer> idx) {
        for (String cand : REGULATION_CANDIDATES) {
            for (String key : idx.keySet()) {
                if (key.equalsIgnoreCase(cand)) {
                    return key;
                }
            }
        }
        for (String key : idx.keySet()) {
            String low = key.toLowerCase(Locale.ROOT);
            if (low.contains("regulat") || low.contains("sign") || low.contains("effect")) {
                if (List.of("score", "nscore", "fscore", "pscore", "ascore", "escore", "dscore", "tscore")
                        .contains(low)) {
                    continue;
                }
                return key;
            }
        }
        return null;
    }

    private static String cell(String[] row, Map<String, Integer> idx, String key) {
        Integer i = idx.get(key);
        if (i == null) {
            for (Map.Entry<String, Integer> e : idx.entrySet()) {
                if (e.getKey().equalsIgnoreCase(key)) {
                    i = e.getValue();
                    break;
                }
            }
        }
        if (i == null || i >= row.length) {
            return "";
        }
        return row[i];
    }
}
