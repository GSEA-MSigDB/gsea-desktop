/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleSupplier;

import edu.mit.broad.coremap.CoreMapTypes.Bridge;
import edu.mit.broad.coremap.CoreMapTypes.BridgeEvidenceKind;
import edu.mit.broad.coremap.CoreMapTypes.GeneEvidenceClass;
import edu.mit.broad.coremap.Cascades.ScoredPath;
import edu.mit.broad.coremap.MechanismGraphShared.GeneLayerAgg;

/** Degree-aware label-shuffle empirical significance for Bridges. */
public final class CascadeNulls {

    private CascadeNulls() {
    }

    public static List<Bridge> attachEmpiricalNulls(
            List<Bridge> bridges,
            DiGraph searchGraph,
            GeneLayerAgg mech,
            GeneLayerAgg pheno,
            Map<String, Double> geneConf,
            Map<String, Double> mechPolarity,
            Map<String, Double> phenoPolarity,
            Map<String, GeneEvidenceClass> evidenceClass,
            IntegrationOptions options,
            SearchBudget budget,
            int permutations,
            int seed) {
        int N = Math.max(0, permutations);
        if (N <= 0 || bridges == null || bridges.isEmpty()) {
            return bridges;
        }
        if (budget == null) {
            throw new IllegalArgumentException("attachEmpiricalNulls requires budget when permutations > 0");
        }
        List<String> present = new ArrayList<>(searchGraph.nodes());
        Map<Integer, List<String>> buckets = buildDegreeBuckets(searchGraph, present);
        int endpointCap = budget.nullMaxEndpoints;

        List<Bridge> out = new ArrayList<>(bridges.size());
        for (int bridgeIndex = 0; bridgeIndex < bridges.size(); bridgeIndex++) {
            Bridge bridge = bridges.get(bridgeIndex);
            String mechId = bridge.mechanisticSet;
            String phenoId = bridge.phenotypicSet;
            if (mechId == null || phenoId == null) {
                out.add(bridge);
                continue;
            }
            List<String> mechMembers = subsampleEndpoints(
                    filterInGraph(mech.genesBySet.getOrDefault(mechId, Set.of()), searchGraph),
                    geneConf, endpointCap);
            List<String> phenoMembers = subsampleEndpoints(
                    filterInGraph(pheno.genesBySet.getOrDefault(phenoId, Set.of()), searchGraph),
                    geneConf, endpointCap);
            if (mechMembers.isEmpty() || phenoMembers.isEmpty()) {
                out.add(bridge);
                continue;
            }
            Set<String> mechGenes = mech.genesBySet.getOrDefault(mechId, Set.of());
            Set<String> phenoGenes = pheno.genesBySet.getOrDefault(phenoId, Set.of());
            double overlapFactor = SetRedundancy.jaccardDownweight(mechGenes, phenoGenes);
            double observed = bestPairScore(searchGraph, mechMembers, phenoMembers, options,
                    mechPolarity, phenoPolarity, evidenceClass, budget) * overlapFactor;

            int ge = 0;
            for (int i = 0; i < N; i++) {
                DoubleSupplier rng = mulberry32(unsignedSeed(seed, bridgeIndex, i));
                List<String> nullSources = shuffleInBucket(mechMembers, buckets, searchGraph, rng);
                List<String> nullTargets = shuffleInBucket(phenoMembers, buckets, searchGraph, rng);
                double nullScore = bestPairScore(searchGraph, nullSources, nullTargets, options,
                        mechPolarity, phenoPolarity, evidenceClass, budget) * overlapFactor;
                if (nullScore >= observed) {
                    ge++;
                }
            }
            bridge.empiricalP = CoreMapConstants.round4((1.0 + ge) / (1.0 + N));
            bridge.nullStatistic = "path_jaccard";
            out.add(bridge);
        }
        return out;
    }

    private static int unsignedSeed(int seed, int bridgeIndex, int i) {
        return (int) ((seed + bridgeIndex * 997L + i * 7919L) & 0xffffffffL);
    }

    private static double bestPairScore(DiGraph graph, List<String> sources, List<String> targets,
            IntegrationOptions options,
            Map<String, Double> mechPolarity,
            Map<String, Double> phenoPolarity,
            Map<String, GeneEvidenceClass> evidenceClass,
            SearchBudget budget) {
        BridgeEvidenceKind kind = Cascades.bridgeEvidenceKind(options.interactomeSource, options.stringMode);
        Map<String, Double> genePolarity = new HashMap<>();
        genePolarity.putAll(mechPolarity);
        genePolarity.putAll(phenoPolarity);
        double best = Double.NEGATIVE_INFINITY;
        for (String source : sources) {
            for (String target : targets) {
                if (source.equals(target)) {
                    continue;
                }
                List<ScoredPath> found = Cascades.findTopScoredPaths(graph, source, target, options, kind,
                        genePolarity, mechPolarity, phenoPolarity, evidenceClass, budget);
                if (!found.isEmpty() && found.get(0).score > best) {
                    best = found.get(0).score;
                }
            }
        }
        return Double.isFinite(best) ? best : Double.NEGATIVE_INFINITY;
    }

    private static List<String> filterInGraph(Set<String> genes, DiGraph graph) {
        List<String> out = new ArrayList<>();
        for (String g : genes) {
            if (graph.nodes().contains(g)) {
                out.add(g);
            }
        }
        return out;
    }

    private static List<String> subsampleEndpoints(List<String> members, Map<String, Double> geneConf, int limit) {
        if (members.size() <= limit) {
            return members;
        }
        List<String> sorted = new ArrayList<>(members);
        sorted.sort(Comparator.comparingDouble((String g) -> -geneConf.getOrDefault(g, 0.0)));
        return new ArrayList<>(sorted.subList(0, limit));
    }

    private static int degreeBucket(int degree) {
        if (degree <= 0) {
            return 0;
        }
        return (int) Math.floor(Math.log(degree + 1) / Math.log(2));
    }

    private static Map<Integer, List<String>> buildDegreeBuckets(DiGraph graph, List<String> universe) {
        Map<Integer, List<String>> buckets = new HashMap<>();
        for (String gene : universe) {
            int b = degreeBucket(graph.degree(gene));
            buckets.computeIfAbsent(b, k -> new ArrayList<>()).add(gene);
        }
        return buckets;
    }

    private static List<String> shuffleInBucket(List<String> members, Map<Integer, List<String>> buckets,
            DiGraph graph, DoubleSupplier rng) {
        List<String> out = new ArrayList<>();
        Set<String> used = new HashSet<>();
        for (String gene : members) {
            List<String> bucket = buckets.getOrDefault(degreeBucket(graph.degree(gene)), List.of());
            List<String> candidates = new ArrayList<>();
            for (String g : bucket) {
                if (!used.contains(g)) {
                    candidates.add(g);
                }
            }
            List<String> pool = !candidates.isEmpty() ? candidates
                    : (!bucket.isEmpty() ? bucket : List.of(gene));
            String pick = pool.get((int) Math.floor(rng.getAsDouble() * pool.size()));
            out.add(pick);
            used.add(pick);
        }
        return out;
    }

    /** JS Math.imul / mulberry32 compatible PRNG. */
    private static DoubleSupplier mulberry32(int seed) {
        int[] t = { seed };
        return () -> {
            t[0] += 0x6d2b79f5;
            int r = imul(t[0] ^ (t[0] >>> 15), 1 | t[0]);
            r ^= r + imul(r ^ (r >>> 7), 61 | r);
            return ((r ^ (r >>> 14)) & 0xffffffffL) / 4294967296.0;
        };
    }

    private static int imul(int a, int b) {
        return (int) (((long) a * (long) b) & 0xffffffffL);
    }
}
