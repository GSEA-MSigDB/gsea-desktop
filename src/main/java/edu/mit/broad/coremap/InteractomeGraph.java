/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import edu.mit.broad.coremap.CoreMapTypes.EdgeData;
import edu.mit.broad.coremap.CoreMapTypes.InteractomeEdge;
import edu.mit.broad.coremap.CoreMapTypes.ParallelEdgeSnapshot;
import edu.mit.broad.coremap.providers.EvidenceQuality;

/** Build DiGraph from live interactome edges. */
public final class InteractomeGraph {

    private InteractomeGraph() {
    }

    /** Mirror undirected edges both ways for STRING-style graphs. */
    public static List<InteractomeEdge> expandUndirectedBothWays(List<InteractomeEdge> edges) {
        List<InteractomeEdge> out = new ArrayList<>();
        for (InteractomeEdge e : edges) {
            out.add(e);
            if (!e.directed) {
                InteractomeEdge rev = copyEdge(e);
                rev.source = e.target;
                rev.target = e.source;
                out.add(rev);
            }
        }
        return out;
    }

    public static List<InteractomeEdge> fuseEdges(List<InteractomeEdge> signor, List<InteractomeEdge> string) {
        return EvidenceQuality.fuseEdges(signor, string);
    }

    private static InteractomeEdge copyEdge(InteractomeEdge e) {
        InteractomeEdge c = new InteractomeEdge();
        c.source = e.source;
        c.target = e.target;
        c.score = e.score;
        c.directed = e.directed;
        c.effect = e.effect;
        c.mechanism = e.mechanism;
        c.evidence = e.evidence;
        c.provider = e.provider;
        c.support = e.support;
        c.channels = e.channels;
        c.pathwayId = e.pathwayId;
        c.pmid = e.pmid;
        c.direct = e.direct;
        c.providers = e.providers;
        c.qualityFactors = e.qualityFactors;
        return c;
    }

    public static DiGraph graphFromEdges(List<InteractomeEdge> edges) {
        DiGraph g = new DiGraph();
        for (InteractomeEdge ie : edges) {
            if (ie.source == null || ie.target == null) {
                continue;
            }
            double score = CoreMapConstants.clamp(ie.score, 0, 1);
            EdgeData d = new EdgeData();
            d.weight = score;
            d.interaction = score;
            d.directed = ie.directed;
            d.effect = ie.effect;
            d.mechanism = ie.mechanism;
            d.evidence = ie.evidence;
            d.provider = ie.provider;
            d.support = ie.support;
            d.pathwayId = ie.pathwayId;
            d.pmid = ie.pmid;
            d.direct = ie.direct;
            d.sign = signFromEdge(ie);
            if (ie.channels != null) {
                d.channels = new LinkedHashMap<>(ie.channels);
            }
            if (ie.providers != null) {
                d.providers = new ArrayList<>(ie.providers);
            }
            String u = ie.source.toUpperCase(Locale.ROOT);
            String v = ie.target.toUpperCase(Locale.ROOT);
            if (g.hasEdge(u, v)) {
                EdgeData existing = g.getEdge(u, v);
                maybePromoteOrParallel(existing, d);
            } else {
                g.addEdge(u, v, d);
            }
        }
        return g;
    }

    private static int signFromEdge(InteractomeEdge ie) {
        if (ie.channels != null && ie.channels.containsKey("sign")) {
            return CoreMapConstants.sign(ie.channels.get("sign"));
        }
        return EvidenceQuality.resolveSignorEffect(ie.effect).sign;
    }

    private static void maybePromoteOrParallel(EdgeData existing, EdgeData incoming) {
        boolean preferIncoming = (!existing.directed && incoming.directed)
                || (existing.directed == incoming.directed && incoming.interaction > existing.interaction);
        if (preferIncoming) {
            ParallelEdgeSnapshot old = existing.toSnapshot();
            existing.weight = incoming.weight;
            existing.interaction = incoming.interaction;
            existing.directed = incoming.directed;
            existing.effect = incoming.effect;
            existing.mechanism = incoming.mechanism;
            existing.evidence = incoming.evidence;
            existing.provider = incoming.provider;
            existing.support = incoming.support;
            existing.sign = incoming.sign;
            existing.pathwayId = incoming.pathwayId;
            existing.pmid = incoming.pmid;
            existing.direct = incoming.direct;
            existing.channels = incoming.channels;
            existing.providers = incoming.providers;
            DiGraph.rememberParallel(existing, old);
        } else {
            DiGraph.rememberParallel(existing, incoming.toSnapshot());
        }
    }
}
