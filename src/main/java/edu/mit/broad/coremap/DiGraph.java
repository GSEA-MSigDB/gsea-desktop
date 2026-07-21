/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import edu.mit.broad.coremap.CoreMapTypes.EdgeData;
import edu.mit.broad.coremap.CoreMapTypes.ParallelEdgeSnapshot;

/** Directed in-memory interaction graph. */
public final class DiGraph {

    private final Set<String> nodeSet = new LinkedHashSet<>();
    private final Map<String, EdgeData> edgeMap = new LinkedHashMap<>();
    private final Map<String, Set<String>> outAdj = new HashMap<>();
    private final Map<String, Set<String>> inAdj = new HashMap<>();

    private static String key(String u, String v) {
        return u + "\t" + v;
    }

    public void addNode(String n) {
        if (n == null || n.isBlank()) {
            return;
        }
        nodeSet.add(n);
        outAdj.computeIfAbsent(n, k -> new LinkedHashSet<>());
        inAdj.computeIfAbsent(n, k -> new LinkedHashSet<>());
    }

    public void addEdge(String u, String v, EdgeData data) {
        addNode(u);
        addNode(v);
        EdgeData d = data != null ? data : new EdgeData();
        String k = key(u, v);
        EdgeData existing = edgeMap.get(k);
        if (existing != null) {
            mergeInto(existing, d);
            return;
        }
        edgeMap.put(k, d);
        outAdj.get(u).add(v);
        inAdj.get(v).add(u);
    }

    private static void mergeInto(EdgeData existing, EdgeData incoming) {
        if (incoming.effect != null) {
            existing.effect = incoming.effect;
        }
        if (incoming.mechanism != null) {
            existing.mechanism = incoming.mechanism;
        }
        if (incoming.evidence != null) {
            existing.evidence = incoming.evidence;
        }
        if (incoming.provider != null) {
            existing.provider = incoming.provider;
        }
        if (incoming.support != null) {
            existing.support = incoming.support;
        }
        if (incoming.pathwayId != null) {
            existing.pathwayId = incoming.pathwayId;
        }
        if (incoming.pmid != null) {
            existing.pmid = incoming.pmid;
        }
        if (incoming.direct != null) {
            existing.direct = incoming.direct;
        }
        if (incoming.channels != null && !incoming.channels.isEmpty()) {
            if (existing.channels == null) {
                existing.channels = new LinkedHashMap<>();
            }
            existing.channels.putAll(incoming.channels);
        }
        if (incoming.interaction > existing.interaction) {
            existing.interaction = incoming.interaction;
            existing.weight = incoming.weight;
            existing.sign = incoming.sign;
            existing.directed = incoming.directed;
        }
        if (incoming.parallels != null) {
            for (ParallelEdgeSnapshot p : incoming.parallels) {
                rememberParallel(existing, p);
            }
        }
    }

    public static String interactionKey(String effect, String mechanism, int sign, boolean directed) {
        return Objects.toString(effect, "") + "|" + Objects.toString(mechanism, "") + "|" + sign + "|" + directed;
    }

    public static void rememberParallel(EdgeData edge, ParallelEdgeSnapshot candidate) {
        if (edge.parallels == null) {
            edge.parallels = new ArrayList<>();
        }
        String candKey = interactionKey(candidate.effect, candidate.mechanism, candidate.sign, candidate.directed);
        String primaryKey = interactionKey(edge.effect, edge.mechanism, edge.sign, edge.directed);
        if (candKey.equals(primaryKey)) {
            edge.weight = Math.max(edge.weight, candidate.weight);
            edge.interaction = Math.max(edge.interaction, candidate.interaction);
            return;
        }
        for (ParallelEdgeSnapshot p : edge.parallels) {
            String pk = interactionKey(p.effect, p.mechanism, p.sign, p.directed);
            if (pk.equals(candKey)) {
                p.weight = Math.max(p.weight, candidate.weight);
                p.interaction = Math.max(p.interaction, candidate.interaction);
                return;
            }
        }
        edge.parallels.add(candidate);
    }

    public boolean hasEdge(String u, String v) {
        return edgeMap.containsKey(key(u, v));
    }

    public EdgeData getEdge(String u, String v) {
        return edgeMap.get(key(u, v));
    }

    public Set<String> successors(String u) {
        Set<String> s = outAdj.get(u);
        // Copy — callers may iterate from multiple threads; live HashSet views are not safe.
        return s == null || s.isEmpty() ? Set.of() : Set.copyOf(s);
    }

    public Set<String> predecessors(String v) {
        Set<String> s = inAdj.get(v);
        return s == null || s.isEmpty() ? Set.of() : Set.copyOf(s);
    }

    public Set<String> neighbors(String n) {
        Set<String> out = new LinkedHashSet<>();
        out.addAll(successors(n));
        out.addAll(predecessors(n));
        return out;
    }

    public int degree(String n) {
        return neighbors(n).size();
    }

    public void forEachSuccessor(String u, BiConsumer<String, EdgeData> fn) {
        for (String v : successors(u)) {
            fn.accept(v, getEdge(u, v));
        }
    }

    public void forEachNeighbor(String n, BiConsumer<String, EdgeData> fn) {
        Set<String> seen = new HashSet<>();
        for (String v : successors(n)) {
            seen.add(v);
            fn.accept(v, getEdge(n, v));
        }
        for (String u : predecessors(n)) {
            if (seen.add(u)) {
                fn.accept(u, getEdge(u, n));
            }
        }
    }

    public int numberOfNodes() {
        return nodeSet.size();
    }

    public int numberOfEdges() {
        return edgeMap.size();
    }

    public Set<String> nodes() {
        return Collections.unmodifiableSet(nodeSet);
    }

    public List<EdgeTriple> edgesWithData() {
        List<EdgeTriple> list = new ArrayList<>();
        for (Map.Entry<String, EdgeData> e : edgeMap.entrySet()) {
            String[] uv = e.getKey().split("\t", 2);
            list.add(new EdgeTriple(uv[0], uv[1], e.getValue()));
        }
        return list;
    }

    public static final class EdgeTriple {
        public final String u;
        public final String v;
        public final EdgeData data;

        public EdgeTriple(String u, String v, EdgeData data) {
            this.u = u;
            this.v = v;
            this.data = data;
        }
    }
}
