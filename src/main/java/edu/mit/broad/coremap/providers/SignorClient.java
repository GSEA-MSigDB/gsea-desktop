/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap.providers;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.coremap.CoreMapTypes.InteractomeEdge;
import edu.mit.broad.coremap.IntegrationOptions;
import edu.mit.broad.coremap.providers.EvidenceQuality.EffectResolved;

/**
 * SIGNOR REST client — CoreMap-parity connect / neighborhood fetching (direct HTTP).
 */
public final class SignorClient {

    private static final Logger klog = LoggerFactory.getLogger(SignorClient.class);
    private static final String SIGNOR_GET_DATA = "https://signor.uniroma2.it/getData.php";
    private static final String SIGNOR_GET_PATHWAY = "https://signor.uniroma2.it/getPathwayData.php";
    private static final int CONNECT_SINGLE_MAX = 120;
    private static final int ID_FETCH_WORKERS = 8;
    private static final int CONNECTOR_FETCH_CAP = 120;

    /** Fixed SIGNOR getData.php column order. */
    private static final String[] SIGNOR_COLUMNS = {
            "ENTITYA", "TYPEA", "IDA", "DATABASEA",
            "ENTITYB", "TYPEB", "IDB", "DATABASEB",
            "EFFECT", "MECHANISM", "RESIDUE", "SEQUENCE", "TAX_ID",
            "CELL_DATA", "TISSUE_DATA", "MODULATOR_COMPLEX", "TARGET_COMPLEX",
            "MODIFICATIONA", "MODASEQ", "MODIFICATIONB", "MODBSEQ",
            "PMID", "DIRECT", "NOTES", "ANNOTATOR", "SENTENCE", "SIGNOR_ID", "SCORE"
    };

    private SignorClient() {
    }

    public static List<InteractomeEdge> fetch(List<String> geneSymbols, IntegrationOptions opts)
            throws Exception {
        return fetch(geneSymbols, opts, null);
    }

    public static List<InteractomeEdge> fetch(List<String> geneSymbols, IntegrationOptions opts,
            List<String> softWarnings) throws Exception {
        boolean proteinOnly = opts.signorProteinOnly;
        boolean directOnly = opts.signorDirectOnly;
        int level = Math.max(1, Math.min(3, opts.signorLevel));
        String queryType = opts.signorQueryType != null ? opts.signorQueryType : "connect";
        String organism = normalizeOrganism(opts.signorOrganism);
        Set<String> taxIds = organismTaxIds(organism);
        double minScore = 0;

        Map<String, String> symbolToUniprot = UniProtMapper.mapSymbolsToUniprot(geneSymbols,
                UniProtMapper.parseOrganismId(organism));
        Map<String, String> uniprotToSymbol = UniProtMapper.reverseMap(symbolToUniprot);
        List<String> accessions = symbolToUniprot.values().stream()
                .filter(a -> a != null && !a.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        List<String> pathwayIds = new ArrayList<>();
        if (opts.signorPathways != null) {
            for (String pid : opts.signorPathways) {
                if (pid != null && !pid.isBlank()) {
                    pathwayIds.add(pid.trim());
                }
            }
        }
        if (accessions.isEmpty() && pathwayIds.isEmpty()) {
            klog.warn("SIGNOR: no UniProt accessions for {} gene symbols", geneSymbols.size());
            return List.of();
        }

        List<SignorRelation> relations = new ArrayList<>();
        if (accessions.size() >= 2) {
            if ("connect".equalsIgnoreCase(queryType) && accessions.size() <= CONNECT_SINGLE_MAX) {
                try {
                    relations = fetchConnect(accessions, level, organism);
                } catch (Exception ex) {
                    klog.warn("SIGNOR connect failed, falling back to id= neighborhoods: {}", ex.toString());
                    if (softWarnings != null) {
                        softWarnings.add("SIGNOR connect failed; used neighborhood fetch instead.");
                    }
                    relations = List.of();
                }
            }
            if (relations.isEmpty()) {
                if ("all".equalsIgnoreCase(queryType)) {
                    relations = fetchIncidents(accessions, organism);
                } else {
                    List<SignorRelation> incidents = fetchIncidents(accessions, organism);
                    if (level >= 3) {
                        List<String> connectors = connectorAccessionsOnShortPaths(incidents, accessions, 2);
                        if (connectors.size() > CONNECTOR_FETCH_CAP) {
                            connectors = connectors.subList(0, CONNECTOR_FETCH_CAP);
                        }
                        if (!connectors.isEmpty()) {
                            incidents = new ArrayList<>(incidents);
                            incidents.addAll(fetchIncidents(connectors, organism));
                        }
                    }
                    relations = filterConnectLevelRelations(incidents, accessions, level);
                }
            }
        } else if (accessions.size() == 1) {
            relations = fetchById(accessions.get(0), organism);
        }

        // Optional neighbor expansion for seeds (same gate as STRING / graph extras)
        int expandLimit = Math.max(0, opts.includeExtraNeighbors ? opts.neighborLimit : 0);
        if (expandLimit > 0 && !accessions.isEmpty()) {
            List<String> expandAcc = accessions.subList(0, Math.min(expandLimit, accessions.size()));
            relations = new ArrayList<>(relations);
            relations.addAll(fetchIncidents(expandAcc, organism));
        }

        if (!pathwayIds.isEmpty()) {
            relations = new ArrayList<>(relations);
            relations.addAll(fetchPathwayRelations(pathwayIds));
        }

        List<InteractomeEdge> edges = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        // With pathway overlays, CoreMap also admits proteinfamily under protein_only.
        Set<String> allowed = pathwayIds.isEmpty()
                ? Set.of("protein")
                : Set.of("protein", "proteinfamily");

        for (SignorRelation rel : relations) {
            String typeA = rel.typeA != null ? rel.typeA.toLowerCase(Locale.ROOT) : "";
            String typeB = rel.typeB != null ? rel.typeB.toLowerCase(Locale.ROOT) : "";
            if (proteinOnly && (!allowed.contains(typeA) || !allowed.contains(typeB))) {
                continue;
            }
            if (rel.taxId != null && !rel.taxId.isBlank() && !taxIds.contains(rel.taxId)) {
                continue;
            }
            if (directOnly && !rel.direct) {
                continue;
            }
            if (rel.score < minScore) {
                continue;
            }

            String source = resolveSymbol(rel.idA, rel.entityA, uniprotToSymbol);
            String target = resolveSymbol(rel.idB, rel.entityB, uniprotToSymbol);
            if (source == null || target == null || source.equals(target)) {
                continue;
            }
            String key = source + "\t" + target + "\t" + rel.effect;
            if (!seen.add(key)) {
                continue;
            }

            List<String> evidenceBits = new ArrayList<>();
            if (rel.pathwayId != null && !rel.pathwayId.isBlank()) {
                evidenceBits.add(rel.pathwayId);
            }
            if (rel.pmid != null && !rel.pmid.isBlank()) {
                evidenceBits.add("PMID:" + rel.pmid);
            }
            if (rel.direct) {
                evidenceBits.add("direct");
            }
            if (rel.signorId != null && !rel.signorId.isBlank()) {
                evidenceBits.add(rel.signorId);
            }

            EffectResolved resolved = EvidenceQuality.resolveSignorEffect(rel.effect);
            InteractomeEdge raw = new InteractomeEdge(source, target, rel.score);
            raw.directed = true;
            raw.effect = rel.effect;
            raw.mechanism = mechanismLabel(rel);
            raw.evidence = evidenceBits.isEmpty() ? "SIGNOR" : String.join(";", evidenceBits);
            raw.provider = "signor";
            raw.pmid = rel.pmid;
            raw.direct = rel.direct;
            raw.pathwayId = rel.pathwayId;
            raw.channels = new LinkedHashMap<>();
            raw.channels.put("sign", (double) resolved.sign);
            edges.add(EvidenceQuality.enrichSignorEdge(raw, proteinOnly, typeA, typeB));
        }
        klog.info("SIGNOR: {} relations → {} edges ({} accessions, level={}, type={})",
                relations.size(), edges.size(), accessions.size(), level, queryType);
        return edges;
    }

    private static String resolveSymbol(String acc, String entity, Map<String, String> uniprotToSymbol) {
        if (acc != null && !acc.isBlank()) {
            String s = uniprotToSymbol.get(acc.trim());
            if (s != null) {
                return s;
            }
        }
        if (entity != null && !entity.isBlank()) {
            return entity.trim().toUpperCase(Locale.ROOT);
        }
        return null;
    }

    private static String mechanismLabel(SignorRelation rel) {
        if (rel.mechanism == null || rel.mechanism.isBlank()) {
            return "signor";
        }
        if (rel.residue != null && !rel.residue.isBlank()) {
            return rel.mechanism + " @ " + rel.residue;
        }
        return rel.mechanism;
    }

    static List<SignorRelation> fetchConnect(List<String> accessions, int level, String organism)
            throws IOException {
        String proteins = String.join(",", accessions);
        String url = SIGNOR_GET_DATA
                + "?type=connect"
                + "&proteins=" + URLEncoder.encode(proteins, StandardCharsets.UTF_8)
                + "&level=" + level
                + "&organism=" + URLEncoder.encode(organism, StandardCharsets.UTF_8);
        return parseSignorTsv(ProviderHttp.get(url));
    }

    static List<SignorRelation> fetchById(String accession, String organism) throws IOException {
        String url = SIGNOR_GET_DATA
                + "?organism=" + URLEncoder.encode(organism, StandardCharsets.UTF_8)
                + "&id=" + URLEncoder.encode(accession, StandardCharsets.UTF_8);
        return parseSignorTsv(ProviderHttp.get(url));
    }

    static List<SignorRelation> fetchPathwayRelations(List<String> pathwayIds) {
        if (pathwayIds == null || pathwayIds.isEmpty()) {
            return List.of();
        }
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(3, pathwayIds.size()));
        try {
            List<Future<List<SignorRelation>>> futures = new ArrayList<>();
            for (String pid : pathwayIds) {
                futures.add(pool.submit(() -> {
                    try {
                        return fetchPathwayRelationsOne(pid);
                    } catch (Exception ex) {
                        return List.<SignorRelation>of();
                    }
                }));
            }
            List<SignorRelation> out = new ArrayList<>();
            for (Future<List<SignorRelation>> f : futures) {
                try {
                    out.addAll(f.get());
                } catch (Exception ignored) {
                }
            }
            return out;
        } finally {
            pool.shutdownNow();
        }
    }

    static List<SignorRelation> fetchPathwayRelationsOne(String pathwayId) throws IOException {
        String pid = pathwayId != null ? pathwayId.trim() : "";
        if (pid.isEmpty()) {
            return List.of();
        }
        String url = SIGNOR_GET_PATHWAY
                + "?pathway=" + URLEncoder.encode(pid, StandardCharsets.UTF_8)
                + "&relations=only";
        return parsePathwayRelationsTsv(ProviderHttp.get(url));
    }

    static List<SignorRelation> parsePathwayRelationsTsv(String body) {
        List<SignorRelation> relations = new ArrayList<>();
        if (body == null || body.isBlank() || body.trim().toLowerCase(Locale.ROOT).startsWith("no result")) {
            return relations;
        }
        String[] lines = body.split("\\r?\\n");
        List<String> nonEmpty = new ArrayList<>();
        for (String line : lines) {
            if (!line.isBlank()) {
                nonEmpty.add(line);
            }
        }
        if (nonEmpty.isEmpty()) {
            return relations;
        }
        String[] header = nonEmpty.get(0).split("\t", -1);
        Map<String, Integer> index = new HashMap<>();
        int start = 1;
        if (header.length > 0 && header[0].trim().equalsIgnoreCase("pathway_id")) {
            for (int i = 0; i < header.length; i++) {
                index.put(header[i].trim().toLowerCase(Locale.ROOT), i);
            }
        } else {
            start = 0;
            String[] cols = {
                    "pathway_id", "pathway_name", "entitya", "regulator_location", "typea", "ida", "databasea",
                    "entityb", "target_location", "typeb", "idb", "databaseb", "effect", "mechanism", "residue",
                    "sequence", "tax_id", "cell_data", "tissue_data", "modulator_complex", "target_complex",
                    "modificationa", "modaseq", "modificationb", "modbseq", "pmid", "direct", "notes",
                    "annotator", "sentence", "signor_id", "score"
            };
            for (int i = 0; i < cols.length; i++) {
                index.put(cols[i], i);
            }
        }
        for (int li = start; li < nonEmpty.size(); li++) {
            String[] fields = nonEmpty.get(li).split("\t", -1);
            if (fields.length < 8) {
                continue;
            }
            SignorRelation r = new SignorRelation();
            r.pathwayId = pathwayCol(fields, index, "pathway_id");
            r.entityA = pathwayCol(fields, index, "entitya");
            r.typeA = pathwayCol(fields, index, "typea");
            r.idA = pathwayCol(fields, index, "ida");
            r.databaseA = pathwayCol(fields, index, "databasea");
            r.entityB = pathwayCol(fields, index, "entityb");
            r.typeB = pathwayCol(fields, index, "typeb");
            r.idB = pathwayCol(fields, index, "idb");
            r.databaseB = pathwayCol(fields, index, "databaseb");
            r.effect = pathwayCol(fields, index, "effect");
            r.mechanism = pathwayCol(fields, index, "mechanism");
            r.residue = pathwayCol(fields, index, "residue");
            r.taxId = pathwayCol(fields, index, "tax_id");
            r.pmid = pathwayCol(fields, index, "pmid");
            String direct = pathwayCol(fields, index, "direct").toLowerCase(Locale.ROOT);
            r.direct = direct.equals("t") || direct.equals("true") || direct.equals("1");
            r.signorId = pathwayCol(fields, index, "signor_id");
            r.score = 0.5;
            String scoreRaw = pathwayCol(fields, index, "score");
            if (!scoreRaw.isBlank()) {
                try {
                    r.score = Double.parseDouble(scoreRaw);
                } catch (NumberFormatException ignored) {
                }
            }
            relations.add(r);
        }
        return relations;
    }

    private static String pathwayCol(String[] fields, Map<String, Integer> index, String name) {
        Integer i = index.get(name);
        if (i == null || i >= fields.length) {
            return "";
        }
        return fields[i].trim();
    }

    private static List<SignorRelation> fetchIncidents(List<String> ids, String organism) {
        if (ids.isEmpty()) {
            return List.of();
        }
        if (ids.size() == 1) {
            try {
                return fetchById(ids.get(0), organism);
            } catch (Exception ex) {
                return List.of();
            }
        }
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(ID_FETCH_WORKERS, ids.size()));
        try {
            List<Future<List<SignorRelation>>> futures = new ArrayList<>();
            for (String id : ids) {
                futures.add(pool.submit(() -> {
                    try {
                        return fetchById(id, organism);
                    } catch (Exception ex) {
                        return List.<SignorRelation>of();
                    }
                }));
            }
            List<SignorRelation> out = new ArrayList<>();
            for (Future<List<SignorRelation>> f : futures) {
                try {
                    out.addAll(f.get());
                } catch (Exception ignored) {
                }
            }
            return out;
        } finally {
            pool.shutdownNow();
        }
    }

    static List<SignorRelation> parseSignorTsv(String body) {
        List<SignorRelation> relations = new ArrayList<>();
        if (body == null || body.isBlank() || body.trim().toLowerCase(Locale.ROOT).startsWith("no result")) {
            return relations;
        }
        for (String line : body.split("\\r?\\n")) {
            if (line.isBlank() || line.startsWith("ENTITYA")) {
                continue;
            }
            String[] fields = line.split("\t", -1);
            if (fields.length < SIGNOR_COLUMNS.length) {
                continue;
            }
            SignorRelation r = new SignorRelation();
            r.entityA = col(fields, "ENTITYA");
            r.typeA = col(fields, "TYPEA");
            r.idA = col(fields, "IDA");
            r.databaseA = col(fields, "DATABASEA");
            r.entityB = col(fields, "ENTITYB");
            r.typeB = col(fields, "TYPEB");
            r.idB = col(fields, "IDB");
            r.databaseB = col(fields, "DATABASEB");
            r.effect = col(fields, "EFFECT");
            r.mechanism = col(fields, "MECHANISM");
            r.residue = col(fields, "RESIDUE");
            r.taxId = col(fields, "TAX_ID");
            r.pmid = col(fields, "PMID");
            String direct = col(fields, "DIRECT").toLowerCase(Locale.ROOT);
            r.direct = direct.equals("t") || direct.equals("true") || direct.equals("1");
            r.signorId = col(fields, "SIGNOR_ID");
            String scoreRaw = col(fields, "SCORE");
            r.score = 0.5;
            if (!scoreRaw.isBlank()) {
                try {
                    r.score = Double.parseDouble(scoreRaw);
                } catch (NumberFormatException ignored) {
                }
            }
            relations.add(r);
        }
        return relations;
    }

    private static String col(String[] fields, String name) {
        for (int i = 0; i < SIGNOR_COLUMNS.length; i++) {
            if (SIGNOR_COLUMNS[i].equals(name)) {
                return i < fields.length ? fields[i].trim() : "";
            }
        }
        return "";
    }

    static List<SignorRelation> filterConnectLevelRelations(List<SignorRelation> relations,
            List<String> seedAccessions, int level) {
        Set<String> seeds = new LinkedHashSet<>();
        for (String id : seedAccessions) {
            if (id != null && !id.isBlank()) {
                seeds.add(id.trim());
            }
        }
        if (seeds.isEmpty() || relations.isEmpty()) {
            return List.of();
        }
        int hops = Math.max(1, Math.min(3, level));
        if (hops == 1) {
            List<SignorRelation> out = new ArrayList<>();
            for (SignorRelation rel : relations) {
                String a = trim(rel.idA);
                String b = trim(rel.idB);
                if (!a.isEmpty() && !b.isEmpty() && !a.equals(b) && seeds.contains(a) && seeds.contains(b)) {
                    out.add(rel);
                }
            }
            return out;
        }
        Adj adj = buildAdj(relations);
        Map<String, Integer> forwardDist = multiSourceDistances(seeds, adj.forward, hops);
        Map<String, Integer> reverseDist = multiSourceDistances(seeds, adj.reverse, hops);
        List<SignorRelation> out = new ArrayList<>();
        for (SignorRelation rel : relations) {
            String a = trim(rel.idA);
            String b = trim(rel.idB);
            if (a.isEmpty() || b.isEmpty() || a.equals(b)) {
                continue;
            }
            Integer da = forwardDist.get(a);
            Integer db = reverseDist.get(b);
            if (da == null || db == null) {
                continue;
            }
            if (da + 1 + db <= hops) {
                out.add(rel);
            }
        }
        return out;
    }

    static List<String> connectorAccessionsOnShortPaths(List<SignorRelation> relations,
            List<String> seedAccessions, int maxHops) {
        Set<String> seeds = new LinkedHashSet<>();
        for (String id : seedAccessions) {
            if (id != null && !id.isBlank()) {
                seeds.add(id.trim());
            }
        }
        int hops = Math.max(1, Math.min(3, maxHops));
        Adj adj = buildAdj(relations);
        Map<String, Integer> forwardDist = multiSourceDistances(seeds, adj.forward, hops);
        Map<String, Integer> reverseDist = multiSourceDistances(seeds, adj.reverse, hops);
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : forwardDist.entrySet()) {
            if (seeds.contains(e.getKey())) {
                continue;
            }
            Integer dRev = reverseDist.get(e.getKey());
            if (dRev == null) {
                continue;
            }
            if (e.getValue() + dRev <= hops) {
                out.add(e.getKey());
            }
        }
        Collections.sort(out);
        return out;
    }

    private static Adj buildAdj(List<SignorRelation> relations) {
        Map<String, Set<String>> forward = new HashMap<>();
        Map<String, Set<String>> reverse = new HashMap<>();
        for (SignorRelation rel : relations) {
            String a = trim(rel.idA);
            String b = trim(rel.idB);
            if (a.isEmpty() || b.isEmpty() || a.equals(b)) {
                continue;
            }
            forward.computeIfAbsent(a, k -> new HashSet<>()).add(b);
            reverse.computeIfAbsent(b, k -> new HashSet<>()).add(a);
        }
        return new Adj(forward, reverse);
    }

    private static Map<String, Integer> multiSourceDistances(Set<String> starts,
            Map<String, Set<String>> adj, int maxDist) {
        Map<String, Integer> dist = new HashMap<>();
        Queue<String> queue = new ArrayDeque<>();
        for (String s : starts) {
            if (s == null || s.isBlank() || dist.containsKey(s)) {
                continue;
            }
            dist.put(s, 0);
            queue.add(s);
        }
        while (!queue.isEmpty()) {
            String u = queue.poll();
            int d = dist.getOrDefault(u, 0);
            if (d >= maxDist) {
                continue;
            }
            for (String v : adj.getOrDefault(u, Set.of())) {
                if (dist.containsKey(v)) {
                    continue;
                }
                dist.put(v, d + 1);
                queue.add(v);
            }
        }
        return dist;
    }

    public static String normalizeOrganism(String organism) {
        return edu.mit.broad.coremap.CoreMapOrganism.normalize(organism);
    }

    private static Set<String> organismTaxIds(String organism) {
        String tax = normalizeOrganism(organism);
        Set<String> ids = new HashSet<>();
        ids.add(tax);
        if ("9606".equals(tax)) {
            ids.add("Homo sapiens");
        } else if ("10090".equals(tax)) {
            ids.add("Mus musculus");
        } else if ("10116".equals(tax)) {
            ids.add("Rattus norvegicus");
        }
        return ids;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static final class Adj {
        final Map<String, Set<String>> forward;
        final Map<String, Set<String>> reverse;

        Adj(Map<String, Set<String>> forward, Map<String, Set<String>> reverse) {
            this.forward = forward;
            this.reverse = reverse;
        }
    }

    static final class SignorRelation {
        String entityA;
        String typeA;
        String idA;
        String databaseA;
        String entityB;
        String typeB;
        String idB;
        String databaseB;
        String effect;
        String mechanism;
        String residue;
        String taxId;
        String pmid;
        boolean direct;
        String signorId;
        double score;
        String pathwayId;
    }
}
