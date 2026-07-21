/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import edu.mit.broad.coremap.CoreMapTypes.BuildProgress;
import edu.mit.broad.coremap.CoreMapTypes.BuildProgressPhase;
import edu.mit.broad.coremap.CoreMapTypes.EnrichmentParseResult;
import edu.mit.broad.coremap.CoreMapTypes.EnrichmentRow;
import edu.mit.broad.coremap.CoreMapTypes.IntegrationResult;
import edu.mit.broad.coremap.CoreMapTypes.InteractomeEdge;
import edu.mit.broad.coremap.CoreMapTypes.InteractomeSource;
import edu.mit.broad.coremap.CoreMapTypes.SharedDriverSummary;
import edu.mit.broad.coremap.providers.SignorClient;
import edu.mit.broad.coremap.providers.SourceEnrichment;
import edu.mit.broad.coremap.providers.StringClient;
import edu.mit.broad.coremap.providers.MsigdbResolver.ResolvedSource;

/**
 * Integrate / rescore orchestration for CoreMap.
 */
public final class CoreMapPipeline {

    private static final Map<String, IntegrateSession> SESSIONS = new ConcurrentHashMap<>();

    private CoreMapPipeline() {
    }

    public static final class IntegrateRequest {
        public EnrichmentParseResult mechanistic;
        public EnrichmentParseResult phenotypic;
        public double minNes = CoreMapConstants.DEFAULT_MIN_NES;
        public double maxNp = CoreMapConstants.DEFAULT_MAX_NP;
        public double maxFdr = CoreMapConstants.DEFAULT_MAX_FDR;
        /** When set, phenotypic layer uses these instead of shared minNes/maxNp/maxFdr. */
        public Double phenoMinNes;
        public Double phenoMaxNp;
        public Double phenoMaxFdr;
        /**
         * When non-null, only these set IDs are used (explicit UI selection).
         * An empty set means no sets from that layer. Null falls back to threshold filters.
         */
        public Set<String> mechanisticSetIds;
        public Set<String> phenotypicSetIds;
        public IntegrationOptions options = IntegrationOptions.defaults();
        /** Cooperative cancel — checked between pipeline phases. */
        public AtomicBoolean cancelled;
        /** When set, drop this session id after a successful integrate (re-integrate). */
        public String replaceSessionId;
    }

    public static IntegrationResult integrate(IntegrateRequest req, Consumer<BuildProgress> onProgress)
            throws Exception {
        checkCancelled(req);
        progress(onProgress, BuildProgressPhase.PARSING, "Filtering gene sets…");
        double mechMinNes = req.minNes;
        double mechMaxNp = req.maxNp;
        double mechMaxFdr = req.maxFdr;
        double phenoMinNes = req.phenoMinNes != null ? req.phenoMinNes : req.minNes;
        double phenoMaxNp = req.phenoMaxNp != null ? req.phenoMaxNp : req.maxNp;
        double phenoMaxFdr = req.phenoMaxFdr != null ? req.phenoMaxFdr : req.maxFdr;
        List<EnrichmentRow> mechRows = filterLayer(req.mechanistic, req.mechanisticSetIds,
                mechMinNes, mechMaxNp, mechMaxFdr);
        List<EnrichmentRow> phenoRows = filterLayer(req.phenotypic, req.phenotypicSetIds,
                phenoMinNes, phenoMaxNp, phenoMaxFdr);
        if (mechRows.isEmpty() && phenoRows.isEmpty()) {
            throw new IllegalArgumentException("No enrichment rows after filtering. Select sets or relax thresholds.");
        }

        IntegrationOptions opts = req.options != null ? req.options.copy() : IntegrationOptions.defaults();
        checkCancelled(req);
        progress(onProgress, BuildProgressPhase.FETCHING_INTERACTOME,
                "Fetching interactome (" + opts.interactomeSource.wire() + ")…");

        Set<String> seeds = seedGenes(mechRows, phenoRows);
        Map<String, Object> sourceStats = new LinkedHashMap<>();
        DiGraph interactome = resolveInteractome(opts, seeds, mechRows, phenoRows, sourceStats);
        checkCancelled(req);

        Map<String, Double> ranks = EnrichmentMath.mergeGeneRankMaps(
                req.mechanistic != null ? req.mechanistic.ranks : null,
                req.phenotypic != null ? req.phenotypic.ranks : null);

        IntegrationResult result = MechanismNetwork.buildMechanismGraph(
                mechRows, phenoRows, opts, interactome, sourceStats, ranks, p -> {
                    checkCancelled(req);
                    if (onProgress != null) {
                        onProgress.accept(p);
                    }
                });
        checkCancelled(req);

        if (req.replaceSessionId != null) {
            SESSIONS.remove(req.replaceSessionId);
        }
        String sessionId = UUID.randomUUID().toString();
        IntegrateSession session = new IntegrateSession();
        session.sessionId = sessionId;
        session.mechRows = mechRows;
        session.phenoRows = phenoRows;
        session.ranks = ranks;
        session.interactome = interactome;
        session.options = opts;
        session.providerKey = providerKey(opts);
        session.fetchedMinScore = fetchMinScore(opts);
        SESSIONS.put(sessionId, session);
        result.sessionId = sessionId;
        result.stats.put("session_id", sessionId);
        result.stats.put("provider_key", session.providerKey);
        result.stats.put("fetched_min_score", session.fetchedMinScore);
        return result;
    }

    private static void checkCancelled(IntegrateRequest req) {
        checkCancelled(req != null ? req.cancelled : null);
    }

    private static void checkCancelled(AtomicBoolean cancelled) {
        if (cancelled != null && cancelled.get()) {
            throw new IllegalStateException("CoreMap operation cancelled.");
        }
    }

    public static String interactomeProviderKey(IntegrationOptions opts) {
        return providerKey(opts != null ? opts : IntegrationOptions.defaults());
    }

    public static Double sessionFetchedMinScore(String sessionId) {
        IntegrateSession session = SESSIONS.get(sessionId);
        return session == null ? null : session.fetchedMinScore;
    }

    /** Drop a cached integrate session (e.g. when the UI clears layers). */
    public static void dropSession(String sessionId) {
        if (sessionId != null) {
            SESSIONS.remove(sessionId);
        }
    }

    public static IntegrationResult rescore(String sessionId, IntegrationOptions newOptions,
            Consumer<BuildProgress> onProgress) throws Exception {
        return rescore(sessionId, newOptions, onProgress, null);
    }

    public static IntegrationResult rescore(String sessionId, IntegrationOptions newOptions,
            Consumer<BuildProgress> onProgress, AtomicBoolean cancelled) throws Exception {
        checkCancelled(cancelled);
        IntegrateSession session = SESSIONS.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Unknown CoreMap session: " + sessionId);
        }
        IntegrationOptions opts = newOptions != null ? newOptions.copy() : session.options.copy();
        if (!providerKey(opts).equals(session.providerKey)) {
            throw new IllegalStateException("Cannot change interactome provider on rescore; re-integrate instead.");
        }
        if ((opts.interactomeSource == InteractomeSource.STRING
                || opts.interactomeSource == InteractomeSource.FUSED)
                && opts.minInteractionScore < session.fetchedMinScore) {
            throw new IllegalStateException("Cannot lower STRING score below fetch threshold ("
                    + session.fetchedMinScore + ") without re-fetching.");
        }
        progress(onProgress, BuildProgressPhase.RANKING_CASCADES, "Rescoring with cached interactome…");
        IntegrationResult result = MechanismNetwork.buildMechanismGraph(
                session.mechRows, session.phenoRows, opts, session.interactome,
                Map.of("rescored", true), session.ranks, p -> {
                    checkCancelled(cancelled);
                    if (onProgress != null) {
                        onProgress.accept(p);
                    }
                });
        checkCancelled(cancelled);
        result.sessionId = sessionId;
        session.options = opts;
        return result;
    }

    private static List<EnrichmentRow> filterLayer(EnrichmentParseResult parsed, Set<String> setIds,
            double minNes, double maxNp, double maxFdr) {
        if (parsed == null || parsed.rows == null) {
            return new ArrayList<>();
        }
        if (setIds != null) {
            return EnrichmentMath.filterRowsBySetIds(parsed.rows, setIds);
        }
        return EnrichmentMath.filterRowsByThresholds(parsed.rows, minNes, maxNp, maxFdr);
    }

    private static Set<String> seedGenes(List<EnrichmentRow> mech, List<EnrichmentRow> pheno) {
        Set<String> seeds = new HashSet<>();
        for (EnrichmentRow r : mech) {
            if (r.leadingEdge && r.geneSymbol != null) {
                seeds.add(r.geneSymbol.toUpperCase(Locale.ROOT));
            }
        }
        for (EnrichmentRow r : pheno) {
            if (r.leadingEdge && r.geneSymbol != null) {
                seeds.add(r.geneSymbol.toUpperCase(Locale.ROOT));
            }
        }
        return seeds;
    }

    static DiGraph resolveInteractome(IntegrationOptions opts, Set<String> seeds,
            List<EnrichmentRow> mechRows, List<EnrichmentRow> phenoRows,
            Map<String, Object> sourceStats) throws Exception {
        List<EnrichmentRow> allRows = new ArrayList<>();
        allRows.addAll(mechRows);
        allRows.addAll(phenoRows);

        DiGraph graph;
        List<String> soft = Collections.synchronizedList(new ArrayList<>());
        switch (opts.interactomeSource) {
            case NONE: {
                List<InteractomeEdge> edges = Comembership.leadingEdgeComembershipEdges(allRows);
                sourceStats.put("interactome_edges", edges.size());
                graph = InteractomeGraph.graphFromEdges(edges);
                break;
            }
            case SIGNOR: {
                List<String> query = cappedQueryGenes(seeds, mechRows, phenoRows);
                List<InteractomeEdge> edges = SignorClient.fetch(query, opts, soft);
                if (edges.isEmpty()) {
                    throw new IllegalStateException(
                            "SIGNOR returned no edges for " + query.size()
                                    + " query genes. Check network access and UniProt mapping.");
                }
                sourceStats.put("interactome_edges", edges.size());
                sourceStats.put("query_genes", query.size());
                graph = InteractomeGraph.graphFromEdges(edges);
                break;
            }
            case STRING: {
                List<String> query = cappedQueryGenes(seeds, mechRows, phenoRows);
                List<InteractomeEdge> edges = StringClient.fetch(query, opts);
                if (edges.isEmpty()) {
                    throw new IllegalStateException(
                            "STRING returned no edges for " + query.size()
                                    + " query genes. Check network access.");
                }
                edges = InteractomeGraph.expandUndirectedBothWays(edges);
                sourceStats.put("interactome_edges", edges.size() / 2);
                sourceStats.put("query_genes", query.size());
                graph = InteractomeGraph.graphFromEdges(edges);
                break;
            }
            case FUSED: {
                List<String> query = cappedQueryGenes(seeds, mechRows, phenoRows);
                // SIGNOR and STRING are independent network fetches — overlap wall time.
                CompletableFuture<List<InteractomeEdge>> signorFut = CompletableFuture.supplyAsync(() -> {
                    try {
                        return SignorClient.fetch(query, opts, soft);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
                CompletableFuture<List<InteractomeEdge>> stringFut = CompletableFuture.supplyAsync(() -> {
                    try {
                        return StringClient.fetch(query, opts);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
                List<InteractomeEdge> signor;
                List<InteractomeEdge> string;
                try {
                    signor = signorFut.join();
                    string = InteractomeGraph.expandUndirectedBothWays(stringFut.join());
                } catch (RuntimeException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof Exception e) {
                        throw e;
                    }
                    throw ex;
                }
                List<InteractomeEdge> fused = InteractomeGraph.fuseEdges(signor, string);
                if (fused.isEmpty()) {
                    throw new IllegalStateException(
                            "Fused SIGNOR+STRING returned no edges for " + query.size() + " query genes.");
                }
                sourceStats.put("interactome_edges", fused.size());
                sourceStats.put("query_genes", query.size());
                sourceStats.put("signor_edges", signor.size());
                sourceStats.put("string_edges", string.size() / 2);
                graph = InteractomeGraph.graphFromEdges(fused);
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown interactome source: " + opts.interactomeSource);
        }
        if (!soft.isEmpty()) {
            softWarnings(sourceStats).addAll(soft);
        }

        boolean includeComembership = opts.interactomeSource == InteractomeSource.NONE;
        sourceStats.put("source_edges", 0);
        sourceStats.put("resolved_sets", 0);
        sourceStats.put("source_enrichment_status", "skipped");
        sourceStats.put("include_comembership", includeComembership);

        if (opts.enableSourceEnrichment) {
            try {
                SourceEnrichment.Result enrichment = SourceEnrichment.enrich(
                        mechRows, phenoRows, seeds, opts.msigdbPath, includeComembership,
                        opts.signorOrganism);
                sourceStats.putAll(enrichment.stats);
                SourceEnrichment.mergeOverlayEdges(graph, enrichment.edges);
                Set<String> platforms = new java.util.LinkedHashSet<>();
                for (ResolvedSource r : enrichment.resolved) {
                    if (r.platform != null) {
                        platforms.add(r.platform.name().toLowerCase(Locale.ROOT));
                    }
                }
                sourceStats.put("resolved_platforms", new ArrayList<>(platforms));
                sourceStats.put("source_enrichment_status", "ready");
            } catch (Exception ex) {
                sourceStats.put("source_enrichment_error", String.valueOf(ex));
                sourceStats.put("source_enrichment_status", "error");
                @SuppressWarnings("unchecked")
                List<String> warnings = (List<String>) sourceStats.computeIfAbsent("warnings",
                        k -> new ArrayList<String>());
                warnings.add("Source enrichment failed: " + ex.getMessage());
            }
        }

        return graph;
    }

    private static List<String> cappedQueryGenes(Set<String> seeds, List<EnrichmentRow> mech,
            List<EnrichmentRow> pheno) {
        MechanismGraphShared.GeneLayerAgg m = MechanismGraphShared.aggregateGeneSets(mech);
        MechanismGraphShared.GeneLayerAgg p = MechanismGraphShared.aggregateGeneSets(pheno);
        List<SharedDriverSummary> shared = MechanismGraphShared.findSharedDrivers(m, p, 0.45);
        List<String> ordered = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (SharedDriverSummary s : shared) {
            if (seen.add(s.gene)) {
                ordered.add(s.gene);
            }
        }
        List<String> rest = new ArrayList<>(seeds);
        rest.sort(Comparator.comparingDouble((String g) ->
                -Math.max(m.confidence.getOrDefault(g, 0.0), p.confidence.getOrDefault(g, 0.0))));
        for (String g : rest) {
            if (seen.add(g)) {
                ordered.add(g);
            }
        }
        if (ordered.size() > CoreMapConstants.STRING_QUERY_CAP) {
            return new ArrayList<>(ordered.subList(0, CoreMapConstants.STRING_QUERY_CAP));
        }
        return ordered;
    }

    @SuppressWarnings("unchecked")
    private static List<String> softWarnings(Map<String, Object> sourceStats) {
        return (List<String>) sourceStats.computeIfAbsent("warnings", k -> new ArrayList<String>());
    }

    /**
     * Identity of fetch/enrichment settings that determine the cached interactome.
     * Scoring-only knobs (path length, penalties, top-K, nulls) are intentionally excluded.
     */
    private static String providerKey(IntegrationOptions opts) {
        String pathways = opts.signorPathways == null || opts.signorPathways.isEmpty()
                ? ""
                : opts.signorPathways.stream().map(String::trim).filter(s -> !s.isEmpty())
                        .sorted().collect(Collectors.joining(","));
        return opts.interactomeSource.wire()
                + "|" + opts.stringMode.wire()
                + "|" + opts.signorLevel
                + "|" + nullToEmpty(opts.signorQueryType)
                + "|" + nullToEmpty(opts.signorOrganism)
                + "|" + opts.signorProteinOnly
                + "|" + opts.signorDirectOnly
                + "|" + pathways
                + "|" + opts.includeExtraNeighbors
                + "|" + opts.neighborLimit
                + "|" + opts.enableSourceEnrichment
                + "|" + nullToEmpty(opts.msigdbPath);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static double fetchMinScore(IntegrationOptions opts) {
        if (opts.interactomeSource == InteractomeSource.STRING
                || opts.interactomeSource == InteractomeSource.FUSED) {
            return opts.minInteractionScore;
        }
        return CoreMapConstants.FETCH_MIN_SCORE_DEFAULT;
    }

    private static void progress(Consumer<BuildProgress> onProgress, BuildProgressPhase phase, String detail) {
        if (onProgress != null) {
            onProgress.accept(new BuildProgress(phase, detail));
        }
    }

    static final class IntegrateSession {
        String sessionId;
        List<EnrichmentRow> mechRows;
        List<EnrichmentRow> phenoRows;
        Map<String, Double> ranks;
        DiGraph interactome;
        IntegrationOptions options;
        String providerKey;
        double fetchedMinScore;
    }
}
