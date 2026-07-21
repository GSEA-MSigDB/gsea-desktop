/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap;

/** Adaptive cascade search budget. */
public final class SearchBudget {

    public final double avgDegree;
    public final int endpointGenes;
    public final int pathsPerPair;
    public final int endpointPairs;
    public final int setsPerLayer;
    public final int beam;
    public final int maxExpansions;
    public final int searchBallCeiling;
    /** Null endpoint subsample per set (pairs ≤ this²). */
    public final int nullMaxEndpoints;

    public SearchBudget(double avgDegree, int endpointGenes, int pathsPerPair, int endpointPairs,
            int setsPerLayer, int beam, int maxExpansions, int searchBallCeiling, int nullMaxEndpoints) {
        this.avgDegree = avgDegree;
        this.endpointGenes = endpointGenes;
        this.pathsPerPair = pathsPerPair;
        this.endpointPairs = endpointPairs;
        this.setsPerLayer = setsPerLayer;
        this.beam = beam;
        this.maxExpansions = maxExpansions;
        this.searchBallCeiling = searchBallCeiling;
        this.nullMaxEndpoints = nullMaxEndpoints;
    }

    public static double graphAvgDegree(DiGraph graph) {
        int n = graph.numberOfNodes();
        return n > 0 ? (double) graph.numberOfEdges() / n : 0.0;
    }

    public static double pairWorkBudget(double avgDeg) {
        if (avgDeg <= 6) {
            return 2400;
        }
        if (avgDeg >= 24) {
            return 600;
        }
        return 2400 - ((avgDeg - 6) / 18.0) * 1800;
    }

    public static SearchBudget compute(double avgDegree, int mechSetCount, int phenoSetCount,
            int seedCount, int maxPathLength, double avgOutDegree) {
        double avgDeg = Math.max(0, avgDegree);
        int pathsPerPair;
        int endpointGenes;
        if (avgDeg >= 24) {
            pathsPerPair = 2;
            endpointGenes = 8;
        } else if (avgDeg >= 12) {
            pathsPerPair = 4;
            endpointGenes = 12;
        } else {
            pathsPerPair = 8;
            endpointGenes = 16;
        }
        int setsPerLayer = (int) CoreMapConstants.clamp(
                Math.round(15.0 / Math.max(1.0, avgDeg / 8.0)), 8, 15);
        double W = pairWorkBudget(avgDeg);
        int setsM = Math.max(1, Math.min(mechSetCount, setsPerLayer));
        int setsP = Math.max(1, Math.min(phenoSetCount, setsPerLayer));
        int endpointPairs = (int) CoreMapConstants.clamp(Math.round(W / (setsM * setsP)), 16, 96);
        int beam = (int) CoreMapConstants.clamp(Math.round(48.0 * 12.0 / Math.max(avgDeg, 6)), 16, 48);
        int maxExpansions = (int) CoreMapConstants.clamp(
                Math.round(5000.0 * 12.0 / Math.max(avgDeg, 6)), 1500, 5000);
        double branch = Math.max(1, avgOutDegree > 0 ? avgOutDegree : avgDeg);
        int ceiling = (int) CoreMapConstants.clamp(
                Math.round(seedCount * Math.pow(Math.min(branch, 8), Math.min(maxPathLength, 4))),
                400, 4000);
        int nullMaxEndpoints = Math.min(8, Math.max(1, (int) Math.ceil(endpointGenes / 2.0)));
        return new SearchBudget(avgDeg, endpointGenes, pathsPerPair, endpointPairs,
                setsPerLayer, beam, maxExpansions, ceiling, nullMaxEndpoints);
    }

    public static java.util.Map<String, Number> statsMap(SearchBudget budget) {
        java.util.Map<String, Number> m = new java.util.LinkedHashMap<>();
        m.put("avg_degree", CoreMapConstants.round4(budget.avgDegree));
        m.put("cascade_endpoint_pairs", budget.endpointPairs);
        m.put("cascade_sets_per_layer", budget.setsPerLayer);
        m.put("cascade_endpoint_genes", budget.endpointGenes);
        m.put("cascade_paths_per_pair", budget.pathsPerPair);
        m.put("cascade_beam", budget.beam);
        m.put("cascade_max_expansions", budget.maxExpansions);
        m.put("search_ball_ceiling", budget.searchBallCeiling);
        m.put("null_max_endpoints", budget.nullMaxEndpoints);
        return m;
    }
}
