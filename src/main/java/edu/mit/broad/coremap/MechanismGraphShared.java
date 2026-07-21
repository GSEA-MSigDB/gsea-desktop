/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import edu.mit.broad.coremap.CoreMapTypes.EnrichmentRow;
import edu.mit.broad.coremap.CoreMapTypes.Layer;
import edu.mit.broad.coremap.CoreMapTypes.LayerHubSummary;
import edu.mit.broad.coremap.CoreMapTypes.SharedDriverSummary;

/** Shared-driver / hub / BFS helpers. */
public final class MechanismGraphShared {

    private MechanismGraphShared() {
    }

    public static final class GeneLayerAgg {
        public final Map<String, Set<String>> setsByGene = new HashMap<>();
        public final Map<String, Double> confidence = new HashMap<>();
        public final Map<String, Set<String>> setNamesByGene = new HashMap<>();
        public final Map<String, Double> polarity = new HashMap<>();
        public final Map<String, Double> signedRnk = new HashMap<>();
        public final Map<String, Set<String>> genesBySet = new HashMap<>();
        public final Map<String, Double> setScores = new HashMap<>();
        public final Map<String, String> setNames = new HashMap<>();
    }

    /** Best enrichment stats aggregated across leading-edge set hits for a gene. */
    public static final class GeneHitStats {
        public final Map<String, Double> pValue = new HashMap<>();
        public final Map<String, Double> fdr = new HashMap<>();
        public final Map<String, Double> nes = new HashMap<>();
        public final Map<String, Double> enrichmentScore = new HashMap<>();
    }

    public static GeneLayerAgg aggregateGeneSets(List<EnrichmentRow> rows) {
        GeneLayerAgg agg = new GeneLayerAgg();
        Map<String, Double> bestEvidence = new HashMap<>();
        for (EnrichmentRow row : rows) {
            if (row == null || !row.leadingEdge || row.geneSymbol == null || row.setId == null) {
                continue;
            }
            String gene = row.geneSymbol.toUpperCase(Locale.ROOT);
            String setId = row.setId;
            agg.setsByGene.computeIfAbsent(gene, k -> new HashSet<>()).add(setId);
            agg.setNamesByGene.computeIfAbsent(gene, k -> new HashSet<>())
                    .add(row.setName != null ? row.setName : setId);
            agg.genesBySet.computeIfAbsent(setId, k -> new HashSet<>()).add(gene);
            agg.setNames.putIfAbsent(setId, row.setName != null ? row.setName : setId);

            double conf = EnrichmentMath.leadingEdgeOmega(row);
            agg.confidence.merge(gene, conf, Math::max);

            double strength = EnrichmentMath.evidenceStrength(row);
            Double prev = bestEvidence.get(gene);
            if (prev == null || strength >= prev) {
                bestEvidence.put(gene, strength);
                agg.polarity.put(gene, (double) EnrichmentMath.polarityFromRow(row));
            }
            if (row.rnkScore != null && Double.isFinite(row.rnkScore)) {
                Double cur = agg.signedRnk.get(gene);
                if (cur == null || Math.abs(row.rnkScore) >= Math.abs(cur)) {
                    agg.signedRnk.put(gene, row.rnkScore);
                }
            }
            double ss = EnrichmentMath.setStrength(row);
            agg.setScores.merge(setId, ss, Math::max);
        }
        return agg;
    }

    public static GeneHitStats aggregateGeneHitStats(List<EnrichmentRow> rows) {
        GeneHitStats stats = new GeneHitStats();
        Map<String, Double> absNes = new HashMap<>();
        Map<String, Double> absEs = new HashMap<>();
        for (EnrichmentRow row : rows) {
            if (row == null || !row.leadingEdge || row.geneSymbol == null) {
                continue;
            }
            String gene = row.geneSymbol.toUpperCase(Locale.ROOT);
            if (row.pValue != null && Double.isFinite(row.pValue)) {
                Double cur = stats.pValue.get(gene);
                if (cur == null || row.pValue < cur) {
                    stats.pValue.put(gene, row.pValue);
                }
            }
            if (row.fdr != null && Double.isFinite(row.fdr)) {
                Double cur = stats.fdr.get(gene);
                if (cur == null || row.fdr < cur) {
                    stats.fdr.put(gene, row.fdr);
                }
            }
            if (row.nes != null && Double.isFinite(row.nes)) {
                double abs = Math.abs(row.nes);
                if (abs >= absNes.getOrDefault(gene, 0.0)) {
                    absNes.put(gene, abs);
                    stats.nes.put(gene, row.nes);
                }
            }
            if (row.enrichmentScore != null && Double.isFinite(row.enrichmentScore)) {
                double abs = Math.abs(row.enrichmentScore);
                if (abs >= absEs.getOrDefault(gene, 0.0)) {
                    absEs.put(gene, abs);
                    stats.enrichmentScore.put(gene, row.enrichmentScore);
                }
            }
        }
        return stats;
    }

    public static GeneHitStats mergeGeneHitStats(GeneHitStats... layers) {
        GeneHitStats merged = new GeneHitStats();
        for (GeneHitStats layer : layers) {
            if (layer == null) {
                continue;
            }
            for (Map.Entry<String, Double> e : layer.pValue.entrySet()) {
                Double cur = merged.pValue.get(e.getKey());
                if (cur == null || e.getValue() < cur) {
                    merged.pValue.put(e.getKey(), e.getValue());
                }
            }
            for (Map.Entry<String, Double> e : layer.fdr.entrySet()) {
                Double cur = merged.fdr.get(e.getKey());
                if (cur == null || e.getValue() < cur) {
                    merged.fdr.put(e.getKey(), e.getValue());
                }
            }
            for (Map.Entry<String, Double> e : layer.nes.entrySet()) {
                if (Math.abs(e.getValue()) >= Math.abs(merged.nes.getOrDefault(e.getKey(), 0.0))) {
                    merged.nes.put(e.getKey(), e.getValue());
                }
            }
            for (Map.Entry<String, Double> e : layer.enrichmentScore.entrySet()) {
                if (Math.abs(e.getValue()) >= Math.abs(merged.enrichmentScore.getOrDefault(e.getKey(), 0.0))) {
                    merged.enrichmentScore.put(e.getKey(), e.getValue());
                }
            }
        }
        return merged;
    }

    public static List<SharedDriverSummary> findSharedDrivers(GeneLayerAgg mech, GeneLayerAgg pheno,
            double sharedDriverDirectionWeight) {
        double alpha = CoreMapConstants.clamp01(sharedDriverDirectionWeight);
        List<SharedDriverSummary> out = new ArrayList<>();
        for (String gene : mech.setsByGene.keySet()) {
            if (!pheno.setsByGene.containsKey(gene)) {
                continue;
            }
            SharedDriverSummary s = new SharedDriverSummary();
            s.gene = gene;
            s.mechanisticSets = new ArrayList<>(mech.setsByGene.get(gene));
            s.phenotypicSets = new ArrayList<>(pheno.setsByGene.get(gene));
            s.mechanisticConfidence = mech.confidence.getOrDefault(gene, CoreMapConstants.LE_FLOOR);
            s.phenotypicConfidence = pheno.confidence.getOrDefault(gene, CoreMapConstants.LE_FLOOR);
            s.mechanisticPolarity = mech.polarity.getOrDefault(gene, 0.0);
            s.phenotypicPolarity = pheno.polarity.getOrDefault(gene, 0.0);
            s.directionConcordance = EnrichmentMath.directionConcordance(s.mechanisticPolarity, s.phenotypicPolarity);
            s.sharedDriverScore = (s.mechanisticConfidence * s.phenotypicConfidence)
                    * (1.0 - alpha + alpha * s.directionConcordance);
            out.add(s);
        }
        out.sort(Comparator.comparingDouble((SharedDriverSummary s) -> -s.sharedDriverScore));
        return out;
    }

    public static double mechanismPolarity(String gene, GeneLayerAgg mech, GeneLayerAgg pheno) {
        Double m = mech.polarity.get(gene);
        Double p = pheno.polarity.get(gene);
        if (m != null && m != 0 && p != null && p != 0) {
            return CoreMapConstants.sign(m) == CoreMapConstants.sign(p) ? m : 0.0;
        }
        if (m != null && m != 0) {
            return m;
        }
        if (p != null && p != 0) {
            return p;
        }
        return 0.0;
    }

    public enum BallMode {
        OUTGOING, UNDIRECTED
    }

    public static Set<String> nodesWithinHops(DiGraph graph, Set<String> seeds, int maxHops,
            int ceiling, BallMode mode) {
        Set<String> visited = new LinkedHashSetPreserveOrder();
        Queue<HopNode> q = new ArrayDeque<>();
        for (String s : seeds) {
            visited.add(s);
            q.add(new HopNode(s, 0));
        }
        while (!q.isEmpty() && visited.size() < ceiling) {
            HopNode cur = q.poll();
            if (cur.hops >= maxHops) {
                continue;
            }
            Iterable<String> next = mode == BallMode.OUTGOING
                    ? graph.successors(cur.node)
                    : graph.neighbors(cur.node);
            for (String n : next) {
                if (visited.add(n)) {
                    if (visited.size() >= ceiling) {
                        break;
                    }
                    q.add(new HopNode(n, cur.hops + 1));
                }
            }
        }
        return visited;
    }

    /** Simple LinkedHashSet factory alias. */
    private static final class LinkedHashSetPreserveOrder extends java.util.LinkedHashSet<String> {
    }

    private static final class HopNode {
        final String node;
        final int hops;

        HopNode(String node, int hops) {
            this.node = node;
            this.hops = hops;
        }
    }

    public static Map<String, Double> maxInteractionToSeedMap(DiGraph graph, Set<String> seeds) {
        Map<String, Double> out = new HashMap<>();
        for (String seed : seeds) {
            graph.forEachNeighbor(seed, (nbr, data) -> {
                double score = neighborScore(data);
                out.merge(nbr, score, Math::max);
            });
        }
        return out;
    }

    public static double neighborScore(CoreMapTypes.EdgeData data) {
        if (data == null) {
            return 0;
        }
        double best = data.interaction;
        if (data.parallels != null) {
            for (CoreMapTypes.ParallelEdgeSnapshot p : data.parallels) {
                best = Math.max(best, p.interaction);
            }
        }
        return best;
    }

    public static List<LayerHubSummary> computeLayerHubs(DiGraph graph, GeneLayerAgg layerAgg, Layer layer) {
        Set<String> layerGenes = layerAgg.setsByGene.keySet();
        List<LayerHubSummary> hubs = new ArrayList<>();
        for (String gene : layerGenes) {
            if (!graph.nodes().contains(gene)) {
                continue;
            }
            double weighted = 0;
            int deg = 0;
            for (String nbr : graph.neighbors(gene)) {
                if (!layerGenes.contains(nbr)) {
                    continue;
                }
                CoreMapTypes.EdgeData ab = graph.getEdge(gene, nbr);
                CoreMapTypes.EdgeData ba = graph.getEdge(nbr, gene);
                double inter = 0;
                if (ab != null) {
                    inter = Math.max(inter, neighborScore(ab));
                }
                if (ba != null) {
                    inter = Math.max(inter, neighborScore(ba));
                }
                if (inter > 0) {
                    weighted += inter;
                    deg++;
                }
            }
            LayerHubSummary h = new LayerHubSummary();
            h.gene = gene;
            h.layer = layer;
            h.sets = new ArrayList<>(layerAgg.setsByGene.getOrDefault(gene, Set.of()));
            h.confidence = layerAgg.confidence.getOrDefault(gene, CoreMapConstants.LE_FLOOR);
            h.polarity = layerAgg.polarity.getOrDefault(gene, 0.0);
            h.degree = deg;
            h.weightedDegree = weighted;
            h.hubScore = h.confidence * (Math.log(1.0 + weighted) / Math.log(2));
            hubs.add(h);
        }
        hubs.sort(Comparator.comparingDouble((LayerHubSummary h) -> -h.hubScore));
        int topK = (int) CoreMapConstants.clamp(Math.round(Math.sqrt(layerGenes.size()) * 2),
                CoreMapConstants.TOP_LAYER_HUBS_MIN, CoreMapConstants.TOP_LAYER_HUBS_MAX);
        if (hubs.size() > topK) {
            return new ArrayList<>(hubs.subList(0, topK));
        }
        return hubs;
    }
}
