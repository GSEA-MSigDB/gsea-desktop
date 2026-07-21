/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap.providers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.coremap.CoreMapOrganism;
import edu.mit.broad.coremap.CoreMapTypes.EnrichmentRow;
import edu.mit.broad.coremap.CoreMapTypes.InteractomeEdge;
import edu.mit.broad.coremap.DiGraph;
import edu.mit.broad.coremap.InteractomeGraph;
import edu.mit.broad.coremap.providers.MsigdbResolver.Platform;
import edu.mit.broad.coremap.providers.MsigdbResolver.ResolvedSource;

/** Resolve enrichment sets and pull Reactome/KEGG/GO/HPO overlay edges. */
public final class SourceEnrichment {

    private static final Logger klog = LoggerFactory.getLogger(SourceEnrichment.class);

    private SourceEnrichment() {
    }

    public static final class Result {
        public final List<ResolvedSource> resolved;
        public final List<InteractomeEdge> edges;
        public final Map<String, Object> stats;

        public Result(List<ResolvedSource> resolved, List<InteractomeEdge> edges, Map<String, Object> stats) {
            this.resolved = resolved;
            this.edges = edges;
            this.stats = stats;
        }
    }

    public static Result enrich(List<EnrichmentRow> mechRows, List<EnrichmentRow> phenoRows,
            Set<String> seedGenes, String msigdbPath, boolean includeComembership) {
        return enrich(mechRows, phenoRows, seedGenes, msigdbPath, includeComembership,
                CoreMapOrganism.HUMAN);
    }

    public static Result enrich(List<EnrichmentRow> mechRows, List<EnrichmentRow> phenoRows,
            Set<String> seedGenes, String msigdbPath, boolean includeComembership, String organism) {
        String taxon = CoreMapOrganism.normalize(organism);
        MsigdbResolver index = MsigdbResolver.loadOptional(msigdbPath);
        List<EnrichmentRow> all = new ArrayList<>();
        all.addAll(mechRows);
        all.addAll(phenoRows);
        List<ResolvedSource> resolved = resolveSetsFromRows(all, index);

        Set<String> reactomeIds = new LinkedHashSet<>();
        Set<String> keggIds = new LinkedHashSet<>();
        Set<String> hpoIds = new LinkedHashSet<>();
        Set<String> goIds = new LinkedHashSet<>();
        for (ResolvedSource item : resolved) {
            if (item.accession == null || item.accession.isBlank()) {
                continue;
            }
            if (item.platform == Platform.REACTOME && CoreMapOrganism.isReactomeAccession(item.accession)) {
                reactomeIds.add(CoreMapOrganism.reactomeIdForOrganism(item.accession, taxon));
            } else if (item.platform == Platform.KEGG && CoreMapOrganism.isKeggAccession(item.accession)) {
                String kegg = CoreMapOrganism.keggIdForOrganism(item.accession, taxon);
                if (kegg != null) {
                    keggIds.add(kegg);
                }
            } else if (includeComembership && item.platform == Platform.HPO
                    && CoreMapOrganism.supportsHpo(taxon)) {
                hpoIds.add(item.accession);
            } else if (includeComembership && item.platform == Platform.GO) {
                goIds.add(item.accession);
            }
        }

        // Providers are independent — fetch in parallel so wall time ≈ max, not sum.
        List<CompletableFuture<List<InteractomeEdge>>> fetches = new ArrayList<>();
        if (!reactomeIds.isEmpty()) {
            List<String> ids = new ArrayList<>(reactomeIds);
            fetches.add(CompletableFuture.supplyAsync(
                    () -> ReactomeClient.fetchMechanismEdges(ids, seedGenes)));
        }
        if (!keggIds.isEmpty()) {
            List<String> ids = new ArrayList<>(keggIds);
            fetches.add(CompletableFuture.supplyAsync(
                    () -> KeggClient.fetchMechanismEdges(ids, seedGenes)));
        }
        if (!hpoIds.isEmpty()) {
            List<String> ids = new ArrayList<>(hpoIds);
            fetches.add(CompletableFuture.supplyAsync(
                    () -> OntologyClient.fetchHpoEdges(ids, seedGenes)));
        }
        if (!goIds.isEmpty()) {
            List<String> ids = new ArrayList<>(goIds);
            final int goTaxon = CoreMapOrganism.taxonId(taxon);
            fetches.add(CompletableFuture.supplyAsync(
                    () -> OntologyClient.fetchGoEdges(ids, seedGenes, goTaxon)));
        }
        List<InteractomeEdge> edges = new ArrayList<>();
        for (CompletableFuture<List<InteractomeEdge>> fut : fetches) {
            List<InteractomeEdge> part = fut.join();
            if (part != null && !part.isEmpty()) {
                edges.addAll(part);
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("resolved_sets", resolved.size());
        stats.put("reactome_sets", reactomeIds.size());
        stats.put("kegg_sets", keggIds.size());
        stats.put("hpo_sets", hpoIds.size());
        stats.put("go_sets", goIds.size());
        stats.put("source_edges", edges.size());
        stats.put("include_comembership", includeComembership);
        stats.put("organism", taxon);
        stats.put("reactome_prefix", CoreMapOrganism.reactomePrefix(taxon));
        stats.put("kegg_code", CoreMapOrganism.keggCode(taxon));
        stats.put("msigdb_path", index.getPath());
        klog.info("Source enrichment ({}): {} resolved sets → {} overlay edges (reactome={}, kegg={})",
                CoreMapOrganism.displayLabel(taxon), resolved.size(), edges.size(),
                reactomeIds.size(), keggIds.size());
        return new Result(resolved, edges, stats);
    }

    public static List<ResolvedSource> resolveSetsFromRows(List<EnrichmentRow> rows, MsigdbResolver index) {
        Set<String> seen = new HashSet<>();
        List<ResolvedSource> resolved = new ArrayList<>();
        for (EnrichmentRow row : rows) {
            if (row == null || row.setId == null || !seen.add(row.setId)) {
                continue;
            }
            ResolvedSource hit = index.resolve(row.setId, row.setName);
            if (hit != null) {
                resolved.add(hit);
            }
        }
        return resolved;
    }

    /** Merge overlay edges into an existing interactome graph (in place). */
    public static void mergeOverlayEdges(DiGraph graph, List<InteractomeEdge> edges) {
        if (graph == null || edges == null || edges.isEmpty()) {
            return;
        }
        DiGraph overlay = InteractomeGraph.graphFromEdges(edges);
        for (DiGraph.EdgeTriple t : overlay.edgesWithData()) {
            graph.addEdge(t.u, t.v, t.data);
        }
    }
}
