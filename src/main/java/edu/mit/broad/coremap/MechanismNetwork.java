/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.coremap.CoreMapTypes.Bridge;
import edu.mit.broad.coremap.CoreMapTypes.BuildProgress;
import edu.mit.broad.coremap.CoreMapTypes.BuildProgressPhase;
import edu.mit.broad.coremap.CoreMapTypes.EnrichmentRow;
import edu.mit.broad.coremap.CoreMapTypes.GeneEvidenceClass;
import edu.mit.broad.coremap.CoreMapTypes.IntegrationResult;
import edu.mit.broad.coremap.CoreMapTypes.InteractomeSource;
import edu.mit.broad.coremap.CoreMapTypes.Layer;
import edu.mit.broad.coremap.CoreMapTypes.LayerHubSummary;
import edu.mit.broad.coremap.CoreMapTypes.SetEnrichmentMetric;
import edu.mit.broad.coremap.CoreMapTypes.SharedDriverSummary;
import edu.mit.broad.coremap.CoreMapTypes.StringMode;
import edu.mit.broad.coremap.MechanismGraphShared.BallMode;
import edu.mit.broad.coremap.MechanismGraphShared.GeneLayerAgg;
import edu.mit.broad.coremap.providers.MsigdbResolver;
import edu.mit.broad.coremap.providers.MsigdbResolver.ResolvedSource;
import edu.mit.broad.coremap.providers.SourceEnrichment;
import edu.mit.broad.coremap.providers.UniProtMapper;

/** Build the unified mechanism graph from enrichment layers + interactome. */
public final class MechanismNetwork {

    private static final Logger klog = LoggerFactory.getLogger(MechanismNetwork.class);

    private MechanismNetwork() {
    }

    public static IntegrationResult buildMechanismGraph(
            List<EnrichmentRow> mechRows,
            List<EnrichmentRow> phenoRows,
            IntegrationOptions options,
            DiGraph interactome,
            Map<String, Object> sourceStats,
            Map<String, Double> companionRanks,
            Consumer<BuildProgress> onProgress) throws Exception {
        IntegrationOptions opts = options != null ? options.copy() : IntegrationOptions.defaults();
        progress(onProgress, BuildProgressPhase.RANKING_CASCADES, "Integrating enrichment layers…");

        GeneLayerAgg mech = MechanismGraphShared.aggregateGeneSets(mechRows);
        GeneLayerAgg pheno = MechanismGraphShared.aggregateGeneSets(phenoRows);
        List<SharedDriverSummary> sharedDrivers = MechanismGraphShared.findSharedDrivers(
                mech, pheno, opts.sharedDriverDirectionWeight);

        Map<String, Double> geneConf = new HashMap<>();
        Map<String, GeneEvidenceClass> evidenceClass = new HashMap<>();
        Map<String, Double> geneRnk = new HashMap<>();
        Map<String, Double> genePolarity = new HashMap<>();
        Map<String, Double> mechPolarity = new HashMap<>(mech.polarity);
        Map<String, Double> phenoPolarity = new HashMap<>(pheno.polarity);

        for (Map.Entry<String, Double> e : mech.confidence.entrySet()) {
            geneConf.put(e.getKey(), e.getValue());
            evidenceClass.put(e.getKey(), GeneEvidenceClass.LEADING_EDGE);
        }
        for (Map.Entry<String, Double> e : pheno.confidence.entrySet()) {
            geneConf.merge(e.getKey(), e.getValue(), Math::max);
            evidenceClass.putIfAbsent(e.getKey(), GeneEvidenceClass.LEADING_EDGE);
            if (!evidenceClass.containsKey(e.getKey())
                    || evidenceClass.get(e.getKey()) != GeneEvidenceClass.LEADING_EDGE) {
                evidenceClass.put(e.getKey(), GeneEvidenceClass.LEADING_EDGE);
            }
        }
        geneRnk.putAll(mech.signedRnk);
        for (Map.Entry<String, Double> e : pheno.signedRnk.entrySet()) {
            Double cur = geneRnk.get(e.getKey());
            if (cur == null || Math.abs(e.getValue()) >= Math.abs(cur)) {
                geneRnk.put(e.getKey(), e.getValue());
            }
        }
        Map<String, Double> ranks = EnrichmentMath.mergeGeneRankMaps(companionRanks, geneRnk);

        Set<String> allGenes = new HashSet<>();
        allGenes.addAll(mech.setsByGene.keySet());
        allGenes.addAll(pheno.setsByGene.keySet());
        for (String g : allGenes) {
            genePolarity.put(g, MechanismGraphShared.mechanismPolarity(g, mech, pheno));
        }

        Set<String> seedGenes = new HashSet<>(allGenes);
        SearchBudget budget = SearchBudget.compute(
                SearchBudget.graphAvgDegree(interactome),
                mech.setScores.size(),
                pheno.setScores.size(),
                seedGenes.size(),
                opts.maxPathLength,
                SearchBudget.graphAvgDegree(interactome));

        BallMode ballMode = ballMode(opts.interactomeSource, opts.stringMode);
        Set<String> searchNodes = MechanismGraphShared.nodesWithinHops(
                interactome, seedGenes, opts.maxPathLength, budget.searchBallCeiling, ballMode);

        Map<String, Double> seedInteractions = MechanismGraphShared.maxInteractionToSeedMap(interactome, seedGenes);
        for (String n : searchNodes) {
            if (geneConf.containsKey(n) && evidenceClass.get(n) == GeneEvidenceClass.LEADING_EDGE) {
                continue;
            }
            EnrichmentMath.assignExtensionEvidence(n, geneConf, genePolarity, geneRnk, evidenceClass,
                    ranks, seedInteractions.getOrDefault(n, 0.0), interactome.degree(n));
        }

        boolean excludeComembership = opts.interactomeSource != InteractomeSource.NONE;
        DiGraph searchGraph = MechanismSerialize.weightedSubgraph(interactome, searchNodes,
                geneConf, genePolarity, mechPolarity, phenoPolarity,
                opts.minInteractionScore, excludeComembership, false, null);

        List<LayerHubSummary> hubs = new ArrayList<>();
        hubs.addAll(MechanismGraphShared.computeLayerHubs(searchGraph, mech, Layer.MECHANISTIC));
        hubs.addAll(MechanismGraphShared.computeLayerHubs(searchGraph, pheno, Layer.PHENOTYPIC));

        progress(onProgress, BuildProgressPhase.RANKING_CASCADES, "Ranking bridges…");
        Set<String> sharedGenes = new HashSet<>();
        for (SharedDriverSummary s : sharedDrivers) {
            sharedGenes.add(s.gene);
        }
        klog.info("Cascade ranking start (interactome nodes={}, edges={})",
                searchGraph.numberOfNodes(), searchGraph.numberOfEdges());
        List<Bridge> bridges = Cascades.rankSetCascades(searchGraph, mech, pheno, geneConf, genePolarity,
                mechPolarity, phenoPolarity, evidenceClass, sharedGenes, opts, budget);
        klog.info("Cascade ranking done — {} bridges", bridges.size());

        if (opts.nullPermutations > 0 && !bridges.isEmpty()) {
            progress(onProgress, BuildProgressPhase.RANKING_CASCADES,
                    "Empirical nulls (" + opts.nullPermutations + " permutations)…");
            bridges = CascadeNulls.attachEmpiricalNulls(bridges, searchGraph, mech, pheno, geneConf,
                    mechPolarity, phenoPolarity, evidenceClass, opts, budget, opts.nullPermutations, 1);
        }

        Set<String> visible = new HashSet<>(seedGenes);
        for (Bridge b : bridges) {
            if (b.nodes != null) {
                visible.addAll(b.nodes);
            }
            if (b.supportingPaths != null) {
                for (CoreMapTypes.CascadeSupport support : b.supportingPaths) {
                    if (support.nodes != null) {
                        visible.addAll(support.nodes);
                    }
                }
            }
        }
        Set<String> extras = MechanismSerialize.optionalNeighborGenes(interactome, sharedDrivers, seedGenes,
                geneConf, genePolarity, geneRnk, evidenceClass, ranks,
                opts.includeExtraNeighbors, opts.neighborLimit, opts.minInteractionScore);
        visible.addAll(extras);

        DiGraph unified = MechanismSerialize.weightedSubgraph(interactome, visible,
                geneConf, genePolarity, mechPolarity, phenoPolarity,
                opts.minInteractionScore, excludeComembership, false, null);
        // Overlay search-graph edge attributes onto unified where present
        for (DiGraph.EdgeTriple t : searchGraph.edgesWithData()) {
            if (unified.hasEdge(t.u, t.v)) {
                CoreMapTypes.EdgeData u = unified.getEdge(t.u, t.v);
                u.weight = t.data.weight;
                u.polarityCompat = t.data.polarityCompat;
            }
        }

        Map<String, String> uniprotByGene = Map.of();
        if (opts.interactomeSource == InteractomeSource.SIGNOR
                || opts.interactomeSource == InteractomeSource.FUSED) {
            try {
                uniprotByGene = UniProtMapper.mapSymbolsToUniprot(new ArrayList<>(unified.nodes()),
                        CoreMapOrganism.taxonId(opts.signorOrganism));
            } catch (Exception ignored) {
                uniprotByGene = Map.of();
            }
        }

        Map<String, ResolvedSource> sourcesBySet =
                resolveSourcesBySet(mechRows, phenoRows, opts.msigdbPath);
        MechanismGraphShared.GeneHitStats geneHits = MechanismGraphShared.mergeGeneHitStats(
                MechanismGraphShared.aggregateGeneHitStats(mechRows),
                MechanismGraphShared.aggregateGeneHitStats(phenoRows));

        progress(onProgress, BuildProgressPhase.FINALIZING, "Building graph…");
        IntegrationResult result = new IntegrationResult();
        result.sharedDrivers = sharedDrivers;
        result.layerHubs = hubs;
        result.bridges = bridges;
        result.elements = MechanismSerialize.serializeElements(unified, mech, pheno, sharedDrivers,
                geneConf, genePolarity, geneRnk, evidenceClass, uniprotByGene, geneHits);
        result.setEnrichments = buildSetEnrichments(mechRows, phenoRows, mech, pheno, sourcesBySet);
        result.stats = new LinkedHashMap<>();
        int mechGenes = mech.setsByGene.size();
        int phenoGenes = pheno.setsByGene.size();
        boolean hasMechSets = !mech.genesBySet.isEmpty();
        boolean hasPhenoSets = !pheno.genesBySet.isEmpty();
        // CoreMap network.ts keys (+ short aliases used by older desktop code).
        result.stats.put("mechanistic_gene_count", mechGenes);
        result.stats.put("phenotypic_gene_count", phenoGenes);
        result.stats.put("mechanistic_genes", mechGenes);
        result.stats.put("phenotypic_genes", phenoGenes);
        result.stats.put("shared_driver_count", sharedDrivers.size());
        result.stats.put("shared_drivers", sharedDrivers.size());
        result.stats.put("layer_hub_count", hubs.size());
        result.stats.put("cascade_count", bridges.size());
        result.stats.put("bridges", bridges.size());
        result.stats.put("node_count", result.elements.nodes.size());
        result.stats.put("edge_count", result.elements.edges.size());
        result.stats.put("nodes", result.elements.nodes.size());
        result.stats.put("edges", result.elements.edges.size());
        result.stats.put("interactome_source", opts.interactomeSource.wire());
        result.stats.put("search_nodes", searchNodes.size());
        if (bridges.isEmpty() && sharedDrivers.isEmpty() && searchNodes.isEmpty()) {
            result.stats.put("message", "No enrichment genes or cascades found in the interactome.");
        } else if (bridges.isEmpty() && hasMechSets != hasPhenoSets) {
            result.stats.put("message",
                    "One layer loaded. Hubs are ranked within that layer; load the other GSEA result for bridges and shared drivers.");
        } else if (bridges.isEmpty()) {
            result.stats.put("message",
                    "No bridges from mechanistic to phenotypic sets; enrichment genes may still appear.");
        } else if (sharedDrivers.isEmpty()) {
            result.stats.put("message", "Bridges found, but no shared drivers across both layers.");
        }
        result.stats.putAll(SearchBudget.statsMap(budget));
        if (sourceStats != null) {
            result.stats.putAll(sourceStats);
        }
        progress(onProgress, BuildProgressPhase.READY, "Done");
        return result;
    }

    private static Map<String, ResolvedSource> resolveSourcesBySet(
            List<EnrichmentRow> mechRows, List<EnrichmentRow> phenoRows, String msigdbPath) {
        Map<String, ResolvedSource> out = new LinkedHashMap<>();
        try {
            MsigdbResolver index = MsigdbResolver.loadOptional(msigdbPath);
            List<EnrichmentRow> all = new ArrayList<>();
            all.addAll(mechRows);
            all.addAll(phenoRows);
            for (ResolvedSource r : SourceEnrichment.resolveSetsFromRows(all, index)) {
                if (r.setId != null) {
                    out.put(r.setId, r);
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static BallMode ballMode(InteractomeSource source, StringMode stringMode) {
        if (source == InteractomeSource.SIGNOR || source == InteractomeSource.FUSED) {
            return BallMode.OUTGOING;
        }
        if (source == InteractomeSource.STRING && stringMode == StringMode.REGULATORY) {
            return BallMode.OUTGOING;
        }
        return BallMode.UNDIRECTED;
    }

    private static List<SetEnrichmentMetric> buildSetEnrichments(
            List<EnrichmentRow> mechRows, List<EnrichmentRow> phenoRows,
            GeneLayerAgg mech, GeneLayerAgg pheno,
            Map<String, ResolvedSource> sourcesBySet) {
        List<SetEnrichmentMetric> list = new ArrayList<>();
        list.addAll(setMetricsForLayer(mechRows, Layer.MECHANISTIC, mech.setNames, sourcesBySet));
        list.addAll(setMetricsForLayer(phenoRows, Layer.PHENOTYPIC, pheno.setNames, sourcesBySet));
        return list;
    }

    private static List<SetEnrichmentMetric> setMetricsForLayer(
            List<EnrichmentRow> rows, Layer layer, Map<String, String> setNames,
            Map<String, ResolvedSource> sourcesBySet) {
        class Entry {
            Double nes;
            Double pValue;
            Double fdr;
            Double enrichmentScore;
        }
        Map<String, Entry> best = new LinkedHashMap<>();
        for (EnrichmentRow row : rows) {
            if (row == null || !row.leadingEdge || row.setId == null) {
                continue;
            }
            Entry entry = best.computeIfAbsent(row.setId, k -> new Entry());
            if (row.nes != null && Double.isFinite(row.nes)
                    && (entry.nes == null || Math.abs(row.nes) > Math.abs(entry.nes))) {
                entry.nes = row.nes;
            }
            if (row.pValue != null && (entry.pValue == null || row.pValue < entry.pValue)) {
                entry.pValue = row.pValue;
            }
            if (row.fdr != null && (entry.fdr == null || row.fdr < entry.fdr)) {
                entry.fdr = row.fdr;
            }
            if (row.enrichmentScore != null && Double.isFinite(row.enrichmentScore)
                    && (entry.enrichmentScore == null
                    || Math.abs(row.enrichmentScore) > Math.abs(entry.enrichmentScore))) {
                entry.enrichmentScore = row.enrichmentScore;
            }
        }
        List<SetEnrichmentMetric> list = new ArrayList<>();
        List<Map.Entry<String, Entry>> sorted = new ArrayList<>(best.entrySet());
        sorted.sort((a, b) -> {
            double an = Math.abs(a.getValue().nes != null ? a.getValue().nes : 0);
            double bn = Math.abs(b.getValue().nes != null ? b.getValue().nes : 0);
            int cmp = Double.compare(bn, an);
            return cmp != 0 ? cmp : a.getKey().compareTo(b.getKey());
        });
        for (Map.Entry<String, Entry> e : sorted) {
            SetEnrichmentMetric m = new SetEnrichmentMetric();
            m.setId = e.getKey();
            m.layer = layer;
            m.setName = setNames.getOrDefault(e.getKey(), e.getKey());
            m.nes = e.getValue().nes;
            m.absNes = Math.abs(e.getValue().nes != null ? e.getValue().nes : 0);
            m.pValue = e.getValue().pValue;
            m.fdr = e.getValue().fdr;
            m.enrichmentScore = e.getValue().enrichmentScore;
            ResolvedSource src = sourcesBySet.get(e.getKey());
            if (src != null) {
                m.sourcePlatform = src.platform != null ? src.platform.name().toLowerCase(Locale.ROOT) : null;
                m.sourceAccession = src.accession;
                m.collection = src.collection;
                m.externalUrl = src.externalUrl;
            }
            list.add(m);
        }
        return list;
    }

    private static void progress(Consumer<BuildProgress> onProgress, BuildProgressPhase phase, String detail) {
        if (onProgress != null) {
            onProgress.accept(new BuildProgress(phase, detail));
        }
    }
}
