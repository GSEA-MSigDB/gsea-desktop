/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

import edu.mit.broad.coremap.CoreMapTypes.Bridge;
import edu.mit.broad.coremap.CoreMapTypes.BridgeEvidenceKind;
import edu.mit.broad.coremap.CoreMapTypes.CascadeHop;
import edu.mit.broad.coremap.CoreMapTypes.CascadeHopAlt;
import edu.mit.broad.coremap.CoreMapTypes.CascadeSupport;
import edu.mit.broad.coremap.CoreMapTypes.EdgeData;
import edu.mit.broad.coremap.CoreMapTypes.GeneEvidenceClass;
import edu.mit.broad.coremap.CoreMapTypes.InteractomeSource;
import edu.mit.broad.coremap.CoreMapTypes.ParallelEdgeSnapshot;
import edu.mit.broad.coremap.CoreMapTypes.StringMode;
import edu.mit.broad.coremap.MechanismGraphShared.GeneLayerAgg;
import edu.mit.broad.coremap.SetRedundancy.SetCluster;

/** PathLinker-style cascade search and bridge ranking. */
public final class Cascades {

    private Cascades() {
    }

    public static BridgeEvidenceKind bridgeEvidenceKind(InteractomeSource source, StringMode stringMode) {
        if (source == InteractomeSource.NONE) {
            return BridgeEvidenceKind.ASSOCIATIVE;
        }
        if (source == InteractomeSource.FUSED || source == InteractomeSource.SIGNOR) {
            return BridgeEvidenceKind.DIRECTED;
        }
        if (source == InteractomeSource.STRING) {
            return stringMode == StringMode.REGULATORY
                    ? BridgeEvidenceKind.DIRECTED
                    : BridgeEvidenceKind.ASSOCIATIVE;
        }
        return BridgeEvidenceKind.DIRECTED;
    }

    public static double effectiveDirectionWeight(IntegrationOptions options, BridgeEvidenceKind kind,
            boolean hasSignedDirected) {
        if (hasSignedDirected) {
            return options.directionWeight;
        }
        if (kind == BridgeEvidenceKind.ASSOCIATIVE) {
            return options.directionWeight * CoreMapConstants.ASSOCIATIVE_DIRECTION_SCALE;
        }
        return options.directionWeight;
    }

    public static double hopReliability(double w, BridgeEvidenceKind kind, boolean class3Pair) {
        double r = CoreMapConstants.clamp(w, CoreMapConstants.RELIABILITY_EPS, 1.0);
        if (kind == BridgeEvidenceKind.ASSOCIATIVE) {
            r = Math.max(CoreMapConstants.ASSOCIATIVE_RELIABILITY_FLOOR, r);
        }
        if (class3Pair) {
            r *= CoreMapConstants.CLASS3_CLASS3_RELIABILITY;
        }
        return CoreMapConstants.clamp(r, CoreMapConstants.RELIABILITY_EPS, 1.0);
    }

    public static double pathwayCoherence(List<String> pathwayIds) {
        if (pathwayIds == null || pathwayIds.size() < 2) {
            return 0;
        }
        int pairs = 0;
        int match = 0;
        for (int i = 1; i < pathwayIds.size(); i++) {
            String a = pathwayIds.get(i - 1);
            String b = pathwayIds.get(i);
            if (a != null && !a.isBlank() && b != null && !b.isBlank()) {
                pairs++;
                if (a.equals(b)) {
                    match++;
                }
            }
        }
        return pairs > 0 ? (double) match / pairs : 0;
    }

    public static double pathScoreFromReliabilities(List<Double> reliabilities, double alignment,
            double directionWeight, double pathLengthPenalty, double midPathResidual,
            double pathwayCoherence) {
        if (reliabilities.isEmpty()) {
            return -pathLengthPenalty * 0 + directionWeight * alignment;
        }
        double sumLog = 0;
        for (double r : reliabilities) {
            sumLog += Math.log(Math.max(r, CoreMapConstants.RELIABILITY_EPS));
        }
        double geoMean = Math.exp(sumLog / reliabilities.size());
        int hops = reliabilities.size();
        return geoMean
                + directionWeight * alignment
                - pathLengthPenalty * hops
                - CoreMapConstants.MID_PATH_RESIDUAL_WEIGHT * midPathResidual
                + CoreMapConstants.PATHWAY_COHERENCE_WEIGHT * pathwayCoherence;
    }

    public static final class ScoredPath {
        public final List<String> nodes;
        public final List<CascadeHop> hops;
        public final double score;
        public final double directionAlignment;

        public ScoredPath(List<String> nodes, List<CascadeHop> hops, double score, double directionAlignment) {
            this.nodes = nodes;
            this.hops = hops;
            this.score = score;
            this.directionAlignment = directionAlignment;
        }
    }

    public static List<ScoredPath> findTopScoredPaths(DiGraph graph, String source, String target,
            IntegrationOptions options, BridgeEvidenceKind evidenceKind,
            Map<String, Double> genePolarity,
            Map<String, Double> mechPolarity,
            Map<String, Double> phenoPolarity,
            Map<String, GeneEvidenceClass> evidenceClass,
            SearchBudget budget) {
        if (source.equals(target) || !graph.nodes().contains(source) || !graph.nodes().contains(target)) {
            return List.of();
        }
        int startPol = pathStartPolarity(source, mechPolarity, phenoPolarity, genePolarity);
        int endPol = pathEndPolarity(target, mechPolarity, phenoPolarity, genePolarity);
        if (startPol == Integer.MIN_VALUE || endPol == Integer.MIN_VALUE) {
            // conflict refused
            return List.of();
        }

        PriorityQueue<Partial> beam = new PriorityQueue<>(Comparator.comparingDouble(p -> p.cost));
        beam.add(new Partial(List.of(source), 0, 0));
        List<ScoredPath> found = new ArrayList<>();
        int expansions = 0;
        int depthBudget = options.maxPathLength;

        while (!beam.isEmpty() && expansions < budget.maxExpansions) {
            List<Partial> current = new ArrayList<>();
            while (!beam.isEmpty() && current.size() < budget.beam) {
                current.add(beam.poll());
            }
            beam.clear();
            List<Partial> next = new ArrayList<>();
            for (Partial p : current) {
                String u = p.nodes.get(p.nodes.size() - 1);
                if (u.equals(target) && p.nodes.size() > 1) {
                    ScoredPath ann = annotatePath(graph, p.nodes, options, evidenceKind,
                            startPol, endPol, genePolarity, evidenceClass);
                    if (ann != null) {
                        found.add(ann);
                    }
                    continue;
                }
                if (p.nodes.size() - 1 >= depthBudget) {
                    continue;
                }
                expansions++;
                Set<String> onPath = new HashSet<>(p.nodes);
                for (String v : graph.successors(u)) {
                    if (onPath.contains(v)) {
                        continue;
                    }
                    EdgeData e = graph.getEdge(u, v);
                    if (e == null) {
                        continue;
                    }
                    boolean c3 = isClass3(evidenceClass, u) && isClass3(evidenceClass, v);
                    double r = hopReliability(e.weight > 0 ? e.weight : e.interaction, evidenceKind, c3);
                    double sumLog = p.sumLog + (-Math.log(r));
                    int hops = p.nodes.size(); // after adding = hops edges
                    double cost = sumLog + options.pathLengthPenalty * hops;
                    List<String> nn = new ArrayList<>(p.nodes);
                    nn.add(v);
                    next.add(new Partial(nn, sumLog, cost));
                }
            }
            next.sort(Comparator.comparingDouble(p -> p.cost));
            for (int i = 0; i < Math.min(budget.beam, next.size()); i++) {
                beam.add(next.get(i));
            }
            if (found.size() >= budget.pathsPerPair * 3) {
                break;
            }
        }
        found.sort(Comparator.comparingDouble((ScoredPath s) -> -s.score));
        if (found.size() > budget.pathsPerPair) {
            return new ArrayList<>(found.subList(0, budget.pathsPerPair));
        }
        return found;
    }

    private static boolean isClass3(Map<String, GeneEvidenceClass> evidenceClass, String g) {
        return evidenceClass.get(g) == GeneEvidenceClass.UNRANKED_EXTENSION;
    }

    /** Returns Integer.MIN_VALUE if polarity conflict refuses the endpoint. */
    private static int pathStartPolarity(String gene, Map<String, Double> mech, Map<String, Double> pheno,
            Map<String, Double> genePol) {
        if (EnrichmentMath.layerPolarityConflict(mech.get(gene), pheno.get(gene))) {
            return Integer.MIN_VALUE;
        }
        Double m = mech.get(gene);
        if (m != null && m != 0) {
            return CoreMapConstants.sign(m);
        }
        Double p = pheno.get(gene);
        if (p != null && p != 0) {
            return CoreMapConstants.sign(p);
        }
        Double g = genePol.get(gene);
        return g == null ? 0 : CoreMapConstants.sign(g);
    }

    private static int pathEndPolarity(String gene, Map<String, Double> mech, Map<String, Double> pheno,
            Map<String, Double> genePol) {
        if (EnrichmentMath.layerPolarityConflict(mech.get(gene), pheno.get(gene))) {
            return Integer.MIN_VALUE;
        }
        Double p = pheno.get(gene);
        if (p != null && p != 0) {
            return CoreMapConstants.sign(p);
        }
        Double m = mech.get(gene);
        if (m != null && m != 0) {
            return CoreMapConstants.sign(m);
        }
        Double g = genePol.get(gene);
        return g == null ? 0 : CoreMapConstants.sign(g);
    }

    private static ScoredPath annotatePath(DiGraph graph, List<String> nodes, IntegrationOptions options,
            BridgeEvidenceKind evidenceKind, int startPol, int endPol,
            Map<String, Double> genePolarity,
            Map<String, GeneEvidenceClass> evidenceClass) {
        List<List<HopVar>> variantsPerHop = new ArrayList<>();
        for (int i = 0; i < nodes.size() - 1; i++) {
            String u = nodes.get(i);
            String v = nodes.get(i + 1);
            EdgeData e = graph.getEdge(u, v);
            if (e == null) {
                return null;
            }
            List<HopVar> vars = new ArrayList<>();
            vars.add(new HopVar(e.effect, e.mechanism, e.sign, e.weight > 0 ? e.weight : e.interaction,
                    e.provider, e.pathwayId, e.directed));
            if (e.parallels != null) {
                for (ParallelEdgeSnapshot p : e.parallels) {
                    vars.add(new HopVar(p.effect, p.mechanism, p.sign,
                            p.weight > 0 ? p.weight : p.interaction, p.provider, null, p.directed));
                }
            }
            // Keep heaviest if too many
            vars.sort(Comparator.comparingDouble((HopVar h) -> -h.weight));
            if (vars.size() > 4) {
                vars = new ArrayList<>(vars.subList(0, 4));
            }
            variantsPerHop.add(vars);
        }
        List<List<HopVar>> combos = cartesianLimited(variantsPerHop, CoreMapConstants.MAX_HOP_COMBINATIONS);
        ScoredPath best = null;
        for (List<HopVar> combo : combos) {
            List<Double> reliabilities = new ArrayList<>();
            List<Integer> edgeSigns = new ArrayList<>();
            List<String> pathwayIds = new ArrayList<>();
            List<CascadeHop> hops = new ArrayList<>();
            boolean hasSignedDirected = false;
            for (int i = 0; i < combo.size(); i++) {
                HopVar h = combo.get(i);
                String u = nodes.get(i);
                String v = nodes.get(i + 1);
                boolean c3 = isClass3(evidenceClass, u) && isClass3(evidenceClass, v);
                reliabilities.add(hopReliability(h.weight, evidenceKind, c3));
                int sign = h.directed ? h.sign : 0;
                edgeSigns.add(sign);
                if (h.directed && h.sign != 0) {
                    hasSignedDirected = true;
                }
                pathwayIds.add(h.pathwayId);
                CascadeHop ch = new CascadeHop();
                ch.source = u;
                ch.target = v;
                ch.effect = h.effect;
                ch.mechanism = h.mechanism;
                ch.sign = h.sign;
                ch.weight = CoreMapConstants.round4(h.weight);
                ch.provider = h.provider;
                ch.pathwayId = h.pathwayId;
                hops.add(ch);
            }
            Integer expected = EnrichmentMath.propagateExpectedPolarity(startPol, edgeSigns);
            double alignment = 0;
            if (expected != null && endPol != 0) {
                alignment = CoreMapConstants.sign(expected) == CoreMapConstants.sign(endPol) ? 1 : -1;
            }
            double residual = 0;
            if (expected != null && endPol != 0 && CoreMapConstants.sign(expected) != CoreMapConstants.sign(endPol)) {
                residual = 1;
            }
            double dirW = effectiveDirectionWeight(options, evidenceKind, hasSignedDirected);
            double score = pathScoreFromReliabilities(reliabilities, alignment, dirW,
                    options.pathLengthPenalty, residual, pathwayCoherence(pathwayIds));
            if (best == null || score > best.score) {
                best = new ScoredPath(new ArrayList<>(nodes), hops, score, alignment);
            }
        }
        return best;
    }

    private static List<List<HopVar>> cartesianLimited(List<List<HopVar>> perHop, int limit) {
        List<List<HopVar>> out = new ArrayList<>();
        out.add(new ArrayList<>());
        for (List<HopVar> hopVars : perHop) {
            List<List<HopVar>> next = new ArrayList<>();
            for (List<HopVar> prefix : out) {
                for (HopVar v : hopVars) {
                    List<HopVar> copy = new ArrayList<>(prefix);
                    copy.add(v);
                    next.add(copy);
                    if (next.size() >= limit) {
                        return next;
                    }
                }
            }
            out = next;
            if (out.isEmpty()) {
                break;
            }
        }
        return out;
    }

    public static List<Bridge> rankSetCascades(DiGraph searchGraph,
            GeneLayerAgg mech, GeneLayerAgg pheno,
            Map<String, Double> geneConf,
            Map<String, Double> genePolarity,
            Map<String, Double> mechPolarityMap,
            Map<String, Double> phenoPolarityMap,
            Map<String, GeneEvidenceClass> evidenceClass,
            Set<String> sharedDriverGenes,
            IntegrationOptions options,
            SearchBudget budget) {
        BridgeEvidenceKind kind = bridgeEvidenceKind(options.interactomeSource, options.stringMode);
        List<String> mechSets = topSets(mech, budget.setsPerLayer);
        List<String> phenoSets = topSets(pheno, budget.setsPerLayer);
        List<SetCluster> mechClusters = SetRedundancy.clusterSetsByJaccard(mechSets, mech.genesBySet,
                CoreMapConstants.JACCARD_CLUSTER_THRESHOLD);
        List<SetCluster> phenoClusters = SetRedundancy.clusterSetsByJaccard(phenoSets, pheno.genesBySet,
                CoreMapConstants.JACCARD_CLUSTER_THRESHOLD);

        List<Bridge> bridges = new ArrayList<>();
        for (SetCluster mc : mechClusters) {
            for (SetCluster pc : phenoClusters) {
                Set<String> mechGenes = mech.genesBySet.getOrDefault(mc.representative, Set.of());
                Set<String> phenoGenes = pheno.genesBySet.getOrDefault(pc.representative, Set.of());
                double overlapFactor = SetRedundancy.jaccardDownweight(mechGenes, phenoGenes);
                List<String> sources = endpoints(mechGenes, geneConf, searchGraph, budget.endpointGenes);
                List<String> targets = endpoints(phenoGenes, geneConf, searchGraph, budget.endpointGenes);
                List<ScoredPath> pairPaths = new ArrayList<>();
                int pairs = 0;
                for (String s : sources) {
                    for (String t : targets) {
                        if (s.equals(t)) {
                            continue;
                        }
                        if (pairs++ >= budget.endpointPairs) {
                            break;
                        }
                        for (ScoredPath p : findTopScoredPaths(searchGraph, s, t, options, kind,
                                genePolarity, mechPolarityMap, phenoPolarityMap, evidenceClass, budget)) {
                            pairPaths.add(new ScoredPath(p.nodes, p.hops, p.score * overlapFactor,
                                    p.directionAlignment));
                        }
                    }
                    if (pairs >= budget.endpointPairs) {
                        break;
                    }
                }
                // Dedupe by node key
                Map<String, ScoredPath> bestByKey = new HashMap<>();
                for (ScoredPath p : pairPaths) {
                    String key = String.join(">", p.nodes);
                    ScoredPath cur = bestByKey.get(key);
                    if (cur == null || p.score > cur.score) {
                        bestByKey.put(key, p);
                    }
                }
                List<ScoredPath> unique = new ArrayList<>(bestByKey.values());
                unique.sort(Comparator.comparingDouble((ScoredPath p) -> -p.score));
                if (unique.size() > CoreMapConstants.MAX_PATHS_PER_SET_PAIR) {
                    unique = new ArrayList<>(unique.subList(0, CoreMapConstants.MAX_PATHS_PER_SET_PAIR));
                }
                if (unique.isEmpty()) {
                    continue;
                }
                bridges.add(mergeSetPairPaths(unique, mc, pc, mech, pheno, sharedDriverGenes, kind));
            }
        }
        bridges.sort(Comparator.comparingDouble((Bridge b) -> -b.bridgeScore));
        if (bridges.size() > options.topKBridges) {
            return new ArrayList<>(bridges.subList(0, options.topKBridges));
        }
        return bridges;
    }

    private static Bridge mergeSetPairPaths(List<ScoredPath> paths, SetCluster mc, SetCluster pc,
            GeneLayerAgg mech, GeneLayerAgg pheno, Set<String> sharedDrivers, BridgeEvidenceKind kind) {
        ScoredPath best = paths.get(0);
        double sumOther = 0;
        for (int i = 1; i < paths.size(); i++) {
            sumOther += Math.max(0, paths.get(i).score);
        }
        double meanOther = paths.size() > 1 ? sumOther / (paths.size() - 1) : 0;
        double aggScore = best.score
                + CoreMapConstants.CASCADE_MULTIPLICITY_WEIGHT * (Math.log(paths.size()) / Math.log(2))
                * Math.max(meanOther, 0);
        double alignNum = 0;
        double alignDen = 0;
        Set<String> shared = new HashSet<>();
        List<CascadeSupport> supports = new ArrayList<>();
        for (ScoredPath p : paths) {
            alignNum += p.directionAlignment * Math.max(p.score, 0);
            alignDen += Math.max(p.score, 0);
            for (String n : p.nodes) {
                if (sharedDrivers.contains(n)) {
                    shared.add(n);
                }
            }
            CascadeSupport cs = new CascadeSupport();
            cs.nodes = p.nodes;
            cs.hops = p.hops;
            cs.score = p.score;
            cs.length = Math.max(0, p.nodes.size() - 1);
            cs.directionAlignment = p.directionAlignment;
            cs.sharedDrivers = new ArrayList<>(shared);
            supports.add(cs);
        }
        Bridge b = new Bridge();
        b.nodes = best.nodes;
        b.hops = best.hops;
        b.bridgeScore = CoreMapConstants.round4(aggScore);
        b.length = Math.max(0, best.nodes.size() - 1);
        b.directionAlignment = alignDen > 0 ? alignNum / alignDen : 0;
        b.mechanisticSet = mc.representative;
        b.phenotypicSet = pc.representative;
        b.mechanisticSetName = mech.setNames.getOrDefault(mc.representative, mc.representative);
        b.phenotypicSetName = pheno.setNames.getOrDefault(pc.representative, pc.representative);
        b.sharedDrivers = new ArrayList<>(shared);
        b.pathCount = paths.size();
        b.supportingPaths = supports;
        b.bridgeEvidenceKind = kind;
        b.relatedMechanisticSets = new ArrayList<>(mc.members);
        b.relatedPhenotypicSets = new ArrayList<>(pc.members);
        return b;
    }

    private static List<String> topSets(GeneLayerAgg agg, int limit) {
        List<Map.Entry<String, Double>> entries = new ArrayList<>(agg.setScores.entrySet());
        entries.sort(Comparator.comparingDouble((Map.Entry<String, Double> e) -> -e.getValue()));
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Double> e : entries) {
            out.add(e.getKey());
            if (out.size() >= limit) {
                break;
            }
        }
        return out;
    }

    private static List<String> endpoints(Set<String> genes, Map<String, Double> geneConf,
            DiGraph graph, int limit) {
        List<String> list = new ArrayList<>();
        for (String g : genes) {
            if (graph.nodes().contains(g)) {
                list.add(g);
            }
        }
        list.sort(Comparator.comparingDouble((String g) -> -geneConf.getOrDefault(g, 0.0)));
        if (list.size() > limit) {
            return new ArrayList<>(list.subList(0, limit));
        }
        return list;
    }

    private static final class Partial {
        final List<String> nodes;
        final double sumLog;
        final double cost;

        Partial(List<String> nodes, double sumLog, double cost) {
            this.nodes = nodes;
            this.sumLog = sumLog;
            this.cost = cost;
        }
    }

    private static final class HopVar {
        final String effect;
        final String mechanism;
        final int sign;
        final double weight;
        final String provider;
        final String pathwayId;
        final boolean directed;

        HopVar(String effect, String mechanism, int sign, double weight, String provider,
                String pathwayId, boolean directed) {
            this.effect = effect;
            this.mechanism = mechanism;
            this.sign = sign;
            this.weight = weight;
            this.provider = provider;
            this.pathwayId = pathwayId;
            this.directed = directed;
        }
    }
}
