/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.mit.broad.coremap.CoreMapTypes.EnrichmentRow;
import edu.mit.broad.coremap.CoreMapTypes.InteractomeEdge;

/** Leading-edge set co-membership support edges (used when interactome source is {@code none}). */
public final class Comembership {

    private Comembership() {
    }

    public static List<InteractomeEdge> leadingEdgeComembershipEdges(List<EnrichmentRow> rows) {
        Map<String, TreeSet<String>> bySet = new LinkedHashMap<>();
        for (EnrichmentRow row : rows) {
            if (row == null || !row.leadingEdge || row.setId == null || row.geneSymbol == null) {
                continue;
            }
            bySet.computeIfAbsent(row.setId, k -> new TreeSet<>())
                    .add(row.geneSymbol.toUpperCase(Locale.ROOT));
        }
        List<InteractomeEdge> edges = new ArrayList<>();
        for (Map.Entry<String, TreeSet<String>> e : bySet.entrySet()) {
            List<String> genes = new ArrayList<>(e.getValue());
            if (genes.size() < 2) {
                continue;
            }
            int neighbors = (int) CoreMapConstants.clamp(Math.round(Math.log(genes.size()) / Math.log(2)), 2, 6);
            for (int i = 0; i < genes.size(); i++) {
                for (int j = 1; j <= neighbors && i + j < genes.size(); j++) {
                    String a = genes.get(i);
                    String b = genes.get(i + j);
                    edges.add(makeEdge(a, b));
                    edges.add(makeEdge(b, a));
                }
            }
        }
        return edges;
    }

    private static InteractomeEdge makeEdge(String a, String b) {
        InteractomeEdge e = new InteractomeEdge(a, b, 0.4);
        e.directed = false;
        e.effect = "associates";
        e.mechanism = "set co-membership";
        e.provider = "geneset";
        e.support = "comembership";
        e.channels = new HashMap<>();
        e.channels.put("sign", 0.0);
        e.channels.put("comembership", 1.0);
        return e;
    }

    public static boolean isComembershipEdge(CoreMapTypes.EdgeData d) {
        if (d == null) {
            return false;
        }
        if ("comembership".equals(d.support)) {
            return true;
        }
        if (d.channels != null && d.channels.getOrDefault("comembership", 0.0) > 0) {
            return true;
        }
        if (d.provider != null) {
            String p = d.provider.toLowerCase(Locale.ROOT);
            if (p.equals("go") || p.equals("hpo") || p.equals("geneset")) {
                return true;
            }
        }
        return false;
    }
}
