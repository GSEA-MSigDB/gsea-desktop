/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import edu.mit.broad.coremap.CoreMapTypes.EnrichmentRow;
import edu.mit.broad.coremap.CoreMapTypes.GeneEvidenceClass;
import edu.mit.broad.coremap.CoreMapTypes.GeneSetSummary;

/** Thresholds, ω scoring, and gene-set summarization. */
public final class EnrichmentMath {

    private EnrichmentMath() {
    }

    public static boolean passesThresholds(Double nes, Double pValue, Double fdr,
            double minNes, double maxNp, double maxFdr) {
        if (nes != null && Math.abs(nes) < minNes) {
            return false;
        }
        if (pValue != null && pValue > maxNp) {
            return false;
        }
        if (fdr != null && fdr > maxFdr) {
            return false;
        }
        return true;
    }

    public static boolean rowPassesThresholds(EnrichmentRow row, double minNes, double maxNp, double maxFdr) {
        return passesThresholds(row.nes, row.pValue, row.fdr, minNes, maxNp, maxFdr);
    }

    public static double geneOmegaFromRnk(double rnk) {
        return Math.min(Math.abs(rnk) / CoreMapConstants.RNK_SCALE, 1.0);
    }

    public static double leadingEdgeOmega(EnrichmentRow row) {
        if (row.rnkScore != null && Double.isFinite(row.rnkScore)) {
            return Math.max(CoreMapConstants.LE_FLOOR, geneOmegaFromRnk(row.rnkScore));
        }
        if (row.fdr != null && row.fdr > 0) {
            return Math.max(CoreMapConstants.LE_FLOOR, CoreMapConstants.clamp01(1.0 - row.fdr));
        }
        if (row.pValue != null && row.pValue > 0) {
            return Math.max(CoreMapConstants.LE_FLOOR, CoreMapConstants.clamp01(1.0 - row.pValue));
        }
        return CoreMapConstants.LE_FLOOR;
    }

    public static double rankedExtensionOmega(double rnk) {
        return Math.min(CoreMapConstants.EXT_CAP, CoreMapConstants.EXT_SCALE * geneOmegaFromRnk(rnk));
    }

    public static double unrankedExtensionOmega(double interaction) {
        return Math.min(CoreMapConstants.CLASS3_CAP,
                Math.max(0, interaction) * CoreMapConstants.CLASS3_INTERACTION_FACTOR);
    }

    public static double hubPenalizedOmega(double omega, int degree, double mu) {
        return omega / (1.0 + Math.max(0, mu) * Math.max(0, degree));
    }

    public static double setStrength(EnrichmentRow row) {
        if (row.nes != null && Double.isFinite(row.nes)) {
            return Math.abs(row.nes);
        }
        if (row.fdr != null && Double.isFinite(row.fdr)) {
            return CoreMapConstants.clamp01(1.0 - row.fdr);
        }
        if (row.pValue != null && Double.isFinite(row.pValue)) {
            return CoreMapConstants.clamp01(1.0 - row.pValue);
        }
        return 0.5;
    }

    public static int polarityFromRow(EnrichmentRow row) {
        if (row.rnkScore != null && row.rnkScore != 0) {
            return CoreMapConstants.sign(row.rnkScore);
        }
        if (row.nes != null && row.nes != 0) {
            return CoreMapConstants.sign(row.nes);
        }
        return 0;
    }

    public static double evidenceStrength(EnrichmentRow row) {
        if (row.rnkScore != null && Double.isFinite(row.rnkScore)) {
            return Math.abs(row.rnkScore);
        }
        if (row.fdr != null && Double.isFinite(row.fdr)) {
            return 1.0 - row.fdr;
        }
        if (row.pValue != null && Double.isFinite(row.pValue)) {
            return 1.0 - row.pValue;
        }
        return CoreMapConstants.LE_FLOOR;
    }

    public static double directionConcordance(double mechPol, double phenoPol) {
        if (mechPol == 0 || phenoPol == 0) {
            return 0.5;
        }
        return CoreMapConstants.sign(mechPol) == CoreMapConstants.sign(phenoPol) ? 1.0 : 0.0;
    }

    public static boolean layerPolarityConflict(Double mechPol, Double phenoPol) {
        if (mechPol == null || phenoPol == null) {
            return false;
        }
        if (mechPol == 0 || phenoPol == 0) {
            return false;
        }
        return CoreMapConstants.sign(mechPol) != CoreMapConstants.sign(phenoPol);
    }

    public static double polarityCompat(double srcPol, double tgtPol, int edgeSign,
            boolean sourceConflict, boolean targetConflict) {
        if (sourceConflict || targetConflict) {
            return CoreMapConstants.POLARITY_COMPAT_CONFLICT;
        }
        if (edgeSign == 0 || srcPol == 0 || tgtPol == 0) {
            return 1.0;
        }
        double predicted = srcPol * edgeSign;
        return CoreMapConstants.sign(predicted) == CoreMapConstants.sign(tgtPol)
                ? CoreMapConstants.POLARITY_COMPAT_MATCH
                : CoreMapConstants.POLARITY_COMPAT_MISMATCH;
    }

    public static Integer propagateExpectedPolarity(int start, List<Integer> edgeSigns) {
        if (start == 0) {
            return null;
        }
        int cur = start;
        boolean any = false;
        for (Integer s : edgeSigns) {
            if (s != null && s != 0) {
                cur *= s;
                any = true;
            }
        }
        return any ? cur : null;
    }

    /**
     * Assign Class 2/3 evidence. Never overwrites an existing Class-1 entry in geneConf.
     * @return true if assigned
     */
    public static boolean assignExtensionEvidence(String gene,
            Map<String, Double> geneConf,
            Map<String, Double> genePolarity,
            Map<String, Double> geneRnk,
            Map<String, GeneEvidenceClass> evidenceClass,
            Map<String, Double> ranks,
            double interaction,
            int degree) {
        if (geneConf.containsKey(gene)) {
            return false;
        }
        Double rnk = ranks != null ? ranks.get(gene) : null;
        if (rnk != null && Double.isFinite(rnk)) {
            geneConf.put(gene, rankedExtensionOmega(rnk));
            genePolarity.put(gene, (double) CoreMapConstants.sign(rnk));
            geneRnk.put(gene, rnk);
            evidenceClass.put(gene, GeneEvidenceClass.RANKED_EXTENSION);
            return true;
        }
        double omega = hubPenalizedOmega(unrankedExtensionOmega(interaction), degree, CoreMapConstants.CLASS3_HUB_MU);
        geneConf.put(gene, omega);
        genePolarity.put(gene, 0.0);
        evidenceClass.put(gene, GeneEvidenceClass.UNRANKED_EXTENSION);
        return true;
    }

    public static List<GeneSetSummary> summarizeGenesets(List<EnrichmentRow> rows,
            double minNes, double maxNp, double maxFdr) {
        Map<String, Agg> bySet = new LinkedHashMap<>();
        for (EnrichmentRow row : rows) {
            if (row == null || !row.leadingEdge || row.setId == null || row.setId.isBlank()) {
                continue;
            }
            Agg a = bySet.computeIfAbsent(row.setId, k -> new Agg());
            a.setId = row.setId;
            a.setName = row.setName != null ? row.setName : row.setId;
            if (row.nes != null && Double.isFinite(row.nes)) {
                if (a.nes == null || Math.abs(row.nes) > Math.abs(a.nes)) {
                    a.nes = row.nes;
                }
            }
            if (row.pValue != null && Double.isFinite(row.pValue)) {
                a.pValue = a.pValue == null ? row.pValue : Math.min(a.pValue, row.pValue);
            }
            if (row.fdr != null && Double.isFinite(row.fdr)) {
                a.fdr = a.fdr == null ? row.fdr : Math.min(a.fdr, row.fdr);
            }
            if (row.geneSymbol != null) {
                a.genes.add(row.geneSymbol.toUpperCase(Locale.ROOT));
            }
        }
        List<GeneSetSummary> out = new ArrayList<>();
        for (Agg a : bySet.values()) {
            GeneSetSummary s = new GeneSetSummary();
            s.setId = a.setId;
            s.setName = a.setName;
            s.nes = a.nes;
            s.pValue = a.pValue;
            s.fdr = a.fdr;
            s.leadingEdgeGenes = new ArrayList<>(a.genes);
            s.leadingEdgeCount = s.leadingEdgeGenes.size();
            s.passedFilter = passesThresholds(a.nes, a.pValue, a.fdr, minNes, maxNp, maxFdr);
            out.add(s);
        }
        out.sort(Comparator
                .comparing((GeneSetSummary g) -> !g.passedFilter)
                .thenComparing((GeneSetSummary g) -> g.nes == null ? 0.0 : -Math.abs(g.nes))
                .thenComparing(g -> g.setId));
        return out;
    }

    public static List<EnrichmentRow> filterRowsBySetIds(List<EnrichmentRow> rows, Set<String> setIds) {
        if (setIds == null) {
            return new ArrayList<>(rows);
        }
        List<EnrichmentRow> out = new ArrayList<>();
        for (EnrichmentRow row : rows) {
            if (row.setId != null && setIds.contains(row.setId)) {
                out.add(row);
            }
        }
        return out;
    }

    public static List<EnrichmentRow> filterRowsByThresholds(List<EnrichmentRow> rows,
            double minNes, double maxNp, double maxFdr) {
        List<EnrichmentRow> out = new ArrayList<>();
        for (EnrichmentRow row : rows) {
            if (rowPassesThresholds(row, minNes, maxNp, maxFdr)) {
                out.add(row);
            }
        }
        return out;
    }

    public static Map<String, Double> mergeGeneRankMaps(Map<String, Double>... maps) {
        Map<String, Double> out = new HashMap<>();
        for (Map<String, Double> m : maps) {
            if (m == null) {
                continue;
            }
            for (Map.Entry<String, Double> e : m.entrySet()) {
                Double cur = out.get(e.getKey());
                if (cur == null || Math.abs(e.getValue()) >= Math.abs(cur)) {
                    out.put(e.getKey(), e.getValue());
                }
            }
        }
        return out;
    }

    private static final class Agg {
        String setId;
        String setName;
        Double nes;
        Double pValue;
        Double fdr;
        Set<String> genes = new HashSet<>();
    }
}
