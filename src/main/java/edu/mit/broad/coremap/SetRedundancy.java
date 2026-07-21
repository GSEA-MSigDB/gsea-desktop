/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Jaccard set redundancy clustering. */
public final class SetRedundancy {

    private SetRedundancy() {
    }

    public static double setJaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 1.0;
        }
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        int inter = 0;
        for (String g : a) {
            if (b.contains(g)) {
                inter++;
            }
        }
        int union = a.size() + b.size() - inter;
        return union > 0 ? (double) inter / union : 0.0;
    }

    public static double jaccardDownweight(Set<String> mechGenes, Set<String> phenoGenes) {
        return 1.0 - setJaccard(mechGenes, phenoGenes);
    }

    public static final class SetCluster {
        public final String representative;
        public final List<String> members = new ArrayList<>();

        public SetCluster(String representative) {
            this.representative = representative;
            this.members.add(representative);
        }
    }

    public static List<SetCluster> clusterSetsByJaccard(List<String> setIds,
            Map<String, Set<String>> genesBySet, double threshold) {
        List<SetCluster> clusters = new ArrayList<>();
        for (String id : setIds) {
            Set<String> genes = genesBySet.getOrDefault(id, new HashSet<>());
            boolean attached = false;
            for (SetCluster cluster : clusters) {
                Set<String> repGenes = genesBySet.getOrDefault(cluster.representative, new HashSet<>());
                if (setJaccard(genes, repGenes) >= threshold) {
                    cluster.members.add(id);
                    attached = true;
                    break;
                }
            }
            if (!attached) {
                clusters.add(new SetCluster(id));
            }
        }
        return clusters;
    }
}
