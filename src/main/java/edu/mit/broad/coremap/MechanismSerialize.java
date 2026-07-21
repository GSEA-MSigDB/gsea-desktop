/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.mit.broad.coremap.CoreMapTypes.EdgeData;
import edu.mit.broad.coremap.CoreMapTypes.EdgeElement;
import edu.mit.broad.coremap.CoreMapTypes.GeneEvidenceClass;
import edu.mit.broad.coremap.CoreMapTypes.GraphEdgeData;
import edu.mit.broad.coremap.CoreMapTypes.GraphElements;
import edu.mit.broad.coremap.CoreMapTypes.GraphNodeData;
import edu.mit.broad.coremap.CoreMapTypes.NodeElement;
import edu.mit.broad.coremap.CoreMapTypes.NodeRole;
import edu.mit.broad.coremap.CoreMapTypes.ParallelEdgeSnapshot;
import edu.mit.broad.coremap.CoreMapTypes.SharedDriverSummary;
import edu.mit.broad.coremap.MechanismGraphShared.GeneLayerAgg;

/** Weighted subgraph + Cytoscape-shaped serialization. */
public final class MechanismSerialize {

    private MechanismSerialize() {
    }

    public static double edgeWeight(double interaction, double srcConf, double tgtConf, double compat) {
        return interaction * ((srcConf + tgtConf) / 2.0) * compat;
    }

    public static DiGraph weightedSubgraph(DiGraph interactome, Set<String> keepNodes,
            Map<String, Double> geneConf,
            Map<String, Double> genePolarity,
            Map<String, Double> mechPolarity,
            Map<String, Double> phenoPolarity,
            double minInteractionScore,
            boolean excludeComembership,
            boolean requireAnchor,
            Set<String> seedsAndAnchors) {
        DiGraph out = new DiGraph();
        for (String n : keepNodes) {
            out.addNode(n);
        }
        for (DiGraph.EdgeTriple t : interactome.edgesWithData()) {
            if (!keepNodes.contains(t.u) || !keepNodes.contains(t.v)) {
                continue;
            }
            if (excludeComembership && Comembership.isComembershipEdge(t.data)) {
                continue;
            }
            double bestRaw = MechanismGraphShared.neighborScore(t.data);
            if (bestRaw < minInteractionScore) {
                continue;
            }
            if (requireAnchor && seedsAndAnchors != null
                    && !seedsAndAnchors.contains(t.u) && !seedsAndAnchors.contains(t.v)) {
                continue;
            }
            EdgeData chosen = pickBestVariant(t.data, minInteractionScore);
            if (chosen == null) {
                continue;
            }
            double srcConf = geneConf.getOrDefault(t.u, CoreMapConstants.CLASS3_CAP);
            double tgtConf = geneConf.getOrDefault(t.v, CoreMapConstants.CLASS3_CAP);
            double srcPol = genePolarity.getOrDefault(t.u, 0.0);
            double tgtPol = genePolarity.getOrDefault(t.v, 0.0);
            boolean srcConflict = EnrichmentMath.layerPolarityConflict(
                    mechPolarity.get(t.u), phenoPolarity.get(t.u));
            boolean tgtConflict = EnrichmentMath.layerPolarityConflict(
                    mechPolarity.get(t.v), phenoPolarity.get(t.v));
            double compat = EnrichmentMath.polarityCompat(srcPol, tgtPol, chosen.sign, srcConflict, tgtConflict);
            EdgeData scored = chosen.copy();
            scored.polarityCompat = compat;
            scored.weight = edgeWeight(scored.interaction, srcConf, tgtConf, compat);
            out.addEdge(t.u, t.v, scored);
        }
        return out;
    }

    private static EdgeData pickBestVariant(EdgeData primary, double minScore) {
        List<EdgeData> cands = new ArrayList<>();
        cands.add(primary);
        if (primary.parallels != null) {
            for (ParallelEdgeSnapshot p : primary.parallels) {
                EdgeData e = new EdgeData();
                e.weight = p.weight;
                e.interaction = p.interaction;
                e.directed = p.directed;
                e.effect = p.effect;
                e.mechanism = p.mechanism;
                e.sign = p.sign;
                e.provider = p.provider;
                e.evidence = p.evidence;
                e.parallels = new ArrayList<>();
                cands.add(e);
            }
        }
        EdgeData best = null;
        for (EdgeData c : cands) {
            if (c.interaction < minScore) {
                continue;
            }
            if (best == null || c.interaction > best.interaction) {
                best = c;
            }
        }
        if (best == null) {
            return null;
        }
        // Attach other variants as parallels on the winner
        EdgeData out = best.copy();
        out.parallels = new ArrayList<>();
        for (EdgeData c : cands) {
            if (c == best) {
                continue;
            }
            if (c.interaction >= minScore) {
                DiGraph.rememberParallel(out, c.toSnapshot());
            }
        }
        // Also keep original parallels that weren't promoted
        if (primary.parallels != null) {
            for (ParallelEdgeSnapshot p : primary.parallels) {
                DiGraph.rememberParallel(out, p);
            }
        }
        return out;
    }

    public static GraphElements serializeElements(DiGraph subgraph,
            GeneLayerAgg mech, GeneLayerAgg pheno,
            List<SharedDriverSummary> sharedDrivers,
            Map<String, Double> geneConf,
            Map<String, Double> genePolarity,
            Map<String, Double> geneRnk,
            Map<String, GeneEvidenceClass> evidenceClass,
            Map<String, String> uniprotByGene,
            MechanismGraphShared.GeneHitStats geneHits) {
        Set<String> shared = new HashSet<>();
        Map<String, SharedDriverSummary> sharedMap = new java.util.HashMap<>();
        for (SharedDriverSummary s : sharedDrivers) {
            shared.add(s.gene);
            sharedMap.put(s.gene, s);
        }
        if (geneHits == null) {
            geneHits = MechanismGraphShared.mergeGeneHitStats();
        }
        GraphElements els = new GraphElements();
        List<String> nodes = new ArrayList<>(new TreeSet<>(subgraph.nodes()));
        for (String gene : nodes) {
            GraphNodeData n = new GraphNodeData();
            n.id = gene;
            n.label = gene;
            boolean isMech = mech.setsByGene.containsKey(gene);
            boolean isPheno = pheno.setsByGene.containsKey(gene);
            if (shared.contains(gene)) {
                n.roles.add(NodeRole.SHARED_DRIVER.wire());
                n.role = NodeRole.SHARED_DRIVER;
            }
            if (isMech) {
                n.roles.add(NodeRole.MECHANISTIC.wire());
                if (n.role != NodeRole.SHARED_DRIVER) {
                    n.role = NodeRole.MECHANISTIC;
                }
            }
            if (isPheno) {
                n.roles.add(NodeRole.PHENOTYPIC.wire());
                if (n.role != NodeRole.SHARED_DRIVER && n.role != NodeRole.MECHANISTIC) {
                    n.role = NodeRole.PHENOTYPIC;
                }
            }
            if (n.roles.isEmpty()) {
                n.roles.add(NodeRole.INTERACTOR.wire());
                n.role = NodeRole.INTERACTOR;
            }
            n.mechanisticSets = new ArrayList<>(mech.setsByGene.getOrDefault(gene, Set.of()));
            n.phenotypicSets = new ArrayList<>(pheno.setsByGene.getOrDefault(gene, Set.of()));
            n.mechanisticSetNames = new ArrayList<>(mech.setNamesByGene.getOrDefault(gene, Set.of()));
            n.phenotypicSetNames = new ArrayList<>(pheno.setNamesByGene.getOrDefault(gene, Set.of()));
            n.confidence = geneConf.getOrDefault(gene, CoreMapConstants.CLASS3_CAP);
            n.rnkScore = geneRnk.get(gene);
            n.evidenceClass = evidenceClass.getOrDefault(gene,
                    (isMech || isPheno) ? GeneEvidenceClass.LEADING_EDGE : GeneEvidenceClass.UNRANKED_EXTENSION);
            n.mechanisticPolarity = mech.polarity.get(gene);
            n.phenotypicPolarity = pheno.polarity.get(gene);
            n.enrichmentPolarity = genePolarity.get(gene);
            n.mechanisticConfidence = mech.confidence.get(gene);
            n.phenotypicConfidence = pheno.confidence.get(gene);
            if (uniprotByGene != null) {
                n.uniprot = uniprotByGene.get(gene);
            }
            SharedDriverSummary sd = sharedMap.get(gene);
            if (sd != null) {
                n.sharedDriverScore = sd.sharedDriverScore;
                n.directionConcordance = sd.directionConcordance;
            }
            if (geneHits.pValue.containsKey(gene)) {
                n.pValue = geneHits.pValue.get(gene);
            }
            if (geneHits.fdr.containsKey(gene)) {
                n.fdr = geneHits.fdr.get(gene);
            }
            if (geneHits.nes.containsKey(gene)) {
                n.nes = geneHits.nes.get(gene);
            }
            if (geneHits.enrichmentScore.containsKey(gene)) {
                n.enrichmentScore = geneHits.enrichmentScore.get(gene);
            }
            els.nodes.add(new NodeElement(n));
        }

        Set<String> undirectedSeen = new HashSet<>();
        List<DiGraph.EdgeTriple> triples = subgraph.edgesWithData();
        triples.sort(Comparator.comparing((DiGraph.EdgeTriple t) -> t.u).thenComparing(t -> t.v));
        for (DiGraph.EdgeTriple t : triples) {
            GraphEdgeData e = new GraphEdgeData();
            e.source = t.u;
            e.target = t.v;
            e.weight = CoreMapConstants.round4(t.data.weight);
            e.interaction = CoreMapConstants.round4(t.data.interaction);
            e.directed = t.data.directed;
            e.effect = t.data.effect;
            e.mechanism = t.data.mechanism;
            e.evidence = t.data.evidence;
            e.provider = t.data.provider;
            e.providers = t.data.providers != null ? new ArrayList<>(t.data.providers) : null;
            e.support = t.data.support;
            e.sign = t.data.sign;
            e.polarityCompat = t.data.polarityCompat;
            e.channels = t.data.channels;
            e.parallels = t.data.parallels;
            e.pathwayId = t.data.pathwayId;
            e.pmid = t.data.pmid;
            e.direct = t.data.direct;
            e.edgeKind = "mechanism";
            if (t.data.directed) {
                e.id = t.u + "->" + t.v;
            } else {
                String a = t.u.compareTo(t.v) <= 0 ? t.u : t.v;
                String b = t.u.compareTo(t.v) <= 0 ? t.v : t.u;
                String uk = a + "--" + b;
                if (!undirectedSeen.add(uk)) {
                    continue;
                }
                e.id = uk;
                e.source = a;
                e.target = b;
            }
            els.edges.add(new EdgeElement(e));
        }
        return els;
    }

    public static Set<String> optionalNeighborGenes(DiGraph interactome, List<SharedDriverSummary> shared,
            Set<String> seeds, Map<String, Double> geneConf, Map<String, Double> genePolarity,
            Map<String, Double> geneRnk, Map<String, GeneEvidenceClass> evidenceClass,
            Map<String, Double> ranks, boolean includeExtra, int neighborLimit, double minInteraction) {
        Set<String> out = new HashSet<>();
        if (!includeExtra || neighborLimit <= 0) {
            return out;
        }
        for (SharedDriverSummary sd : shared) {
            List<Nbr> cands = new ArrayList<>();
            interactome.forEachNeighbor(sd.gene, (nbr, data) -> {
                if (seeds.contains(nbr)) {
                    return;
                }
                double score = MechanismGraphShared.neighborScore(data);
                if (score < minInteraction) {
                    return;
                }
                boolean ranked = ranks != null && ranks.containsKey(nbr);
                cands.add(new Nbr(nbr, score, ranked));
            });
            cands.sort(Comparator.comparing((Nbr n) -> !n.ranked).thenComparingDouble(n -> -n.score));
            int added = 0;
            for (Nbr n : cands) {
                if (added >= neighborLimit) {
                    break;
                }
                if (out.add(n.gene)) {
                    EnrichmentMath.assignExtensionEvidence(n.gene, geneConf, genePolarity, geneRnk,
                            evidenceClass, ranks, n.score, interactome.degree(n.gene));
                    added++;
                }
            }
        }
        return out;
    }

    private static final class Nbr {
        final String gene;
        final double score;
        final boolean ranked;

        Nbr(String gene, double score, boolean ranked) {
            this.gene = gene;
            this.score = score;
            this.ranked = ranked;
        }
    }
}
