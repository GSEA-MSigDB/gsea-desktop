/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap.providers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import edu.mit.broad.coremap.CoreMapConstants;
import edu.mit.broad.coremap.CoreMapTypes.InteractomeEdge;

/** Soft evidence-quality factors for SIGNOR/STRING (ported from CoreMap evidenceQuality.ts). */
public final class EvidenceQuality {

    public static final double Q_MIN = 0.45;
    public static final double Q_MAX = 1.35;
    public static final double FUSED_STRING_ONLY_CAP = 0.72;
    public static final double FUSED_STRING_BOOST_MAX = 0.12;

    private static final Map<String, Double> CHANNEL_WEIGHTS = Map.of(
            "escore", 1.0,
            "dscore", 0.95,
            "ascore", 0.7,
            "nscore", 0.55,
            "fscore", 0.5,
            "pscore", 0.5,
            "tscore", 0.35);

    private EvidenceQuality() {
    }

    public static final class EffectResolved {
        public final int sign;
        public final String effectClass;

        public EffectResolved(int sign, String effectClass) {
            this.sign = sign;
            this.effectClass = effectClass;
        }
    }

    public static EffectResolved resolveSignorEffect(String effect) {
        if (effect == null || effect.isBlank()) {
            return new EffectResolved(0, "unknown");
        }
        String text = effect.trim().toLowerCase(Locale.ROOT);
        if (text.startsWith("up-regulates") || text.contains("up-regulates")) {
            return new EffectResolved(1, "up");
        }
        if (text.startsWith("down-regulates") || text.contains("down-regulates")) {
            return new EffectResolved(-1, "down");
        }
        if (text.contains("form complex") || text.contains("form-complex")
                || text.equals("complex") || text.contains("binding")) {
            return new EffectResolved(0, "form_complex");
        }
        if (text.contains("unknown") || text.equals("?") || text.equals("n/a")) {
            return new EffectResolved(0, "unknown");
        }
        return new EffectResolved(0, "other");
    }

    public static final class StringEffect {
        public final String effect;
        public final int sign;
        public final boolean directed;

        public StringEffect(String effect, int sign, boolean directed) {
            this.effect = effect;
            this.sign = sign;
            this.directed = directed;
        }
    }

    public static int parseStringRegulationSign(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        String text = raw.trim().toLowerCase(Locale.ROOT);
        if (text.matches("^(1|\\+|pos|positive|activation|up|stimulat).*")
                || text.contains("positive") || text.contains("activation")) {
            return 1;
        }
        if (text.matches("^(-1|-|neg|negative|inhibition|repression|down).*")
                || text.contains("negative") || text.contains("inhibition")
                || text.contains("repression")) {
            return -1;
        }
        return 0;
    }

    public static StringEffect resolveStringEffect(String networkType, String regulationRaw) {
        if ("functional".equals(networkType)) {
            return new StringEffect("associates", 0, false);
        }
        if ("physical".equals(networkType)) {
            return new StringEffect("binds", 0, false);
        }
        int sign = parseStringRegulationSign(regulationRaw);
        if (sign > 0) {
            return new StringEffect("up-regulates", 1, true);
        }
        if (sign < 0) {
            return new StringEffect("down-regulates", -1, true);
        }
        return new StringEffect("regulates", 0, true);
    }

    public static InteractomeEdge enrichSignorEdge(InteractomeEdge edge, boolean proteinOnly,
            String typeA, String typeB) {
        EffectResolved resolved = resolveSignorEffect(edge.effect);
        Map<String, Double> factors = signorQualityFactors(edge, proteinOnly, typeA, typeB, resolved);
        double quality = product(factors);
        double score = CoreMapConstants.clamp01(edge.score * quality);
        InteractomeEdge out = copy(edge);
        out.score = score;
        if (out.channels == null) {
            out.channels = new LinkedHashMap<>();
        } else {
            out.channels = new LinkedHashMap<>(out.channels);
        }
        out.channels.put("sign", (double) resolved.sign);
        out.channels.put("quality", quality);
        for (Map.Entry<String, Double> e : factors.entrySet()) {
            out.channels.put("q_" + e.getKey(), e.getValue());
        }
        if (out.qualityFactors == null) {
            out.qualityFactors = new LinkedHashMap<>();
        }
        out.qualityFactors.putAll(factors);
        return out;
    }

    public static InteractomeEdge enrichStringEdge(InteractomeEdge edge, String networkType) {
        ChannelQ cq = stringChannelQuality(edge.channels, networkType);
        double quality = product(Map.of("channel", cq.q));
        InteractomeEdge out = copy(edge);
        out.score = CoreMapConstants.clamp01(edge.score * quality);
        if (out.channels == null) {
            out.channels = new LinkedHashMap<>();
        } else {
            out.channels = new LinkedHashMap<>(out.channels);
        }
        out.channels.put("quality", quality);
        for (Map.Entry<String, Double> e : cq.factors.entrySet()) {
            out.channels.put("q_" + e.getKey(), CoreMapConstants.round4(e.getValue()));
        }
        return out;
    }

    public static List<InteractomeEdge> fuseEdges(List<InteractomeEdge> signorEdges,
            List<InteractomeEdge> stringEdges) {
        Map<String, InteractomeEdge> byKey = new LinkedHashMap<>();
        for (InteractomeEdge edge : signorEdges) {
            String key = edge.source + "\t" + edge.target;
            InteractomeEdge prev = byKey.get(key);
            if (prev == null || edge.score >= prev.score) {
                InteractomeEdge copy = copy(edge);
                copy.provider = "fused";
                copy.evidence = joinEvidence(edge.evidence, "SIGNOR");
                copy.providers = listOf("signor");
                byKey.put(key, copy);
            }
        }
        for (InteractomeEdge edge : stringEdges) {
            String key = edge.source + "\t" + edge.target;
            InteractomeEdge existing = byKey.get(key);
            if (existing == null) {
                InteractomeEdge copy = copy(edge);
                copy.score = Math.min(edge.score, FUSED_STRING_ONLY_CAP);
                copy.provider = "fused";
                copy.evidence = joinEvidence(edge.evidence, "STRING");
                copy.providers = listOf("string");
                byKey.put(key, copy);
                continue;
            }
            double boost = Math.min(FUSED_STRING_BOOST_MAX, 0.25 * CoreMapConstants.clamp01(edge.score));
            existing.score = CoreMapConstants.clamp01(existing.score + boost);
            if (existing.channels == null) {
                existing.channels = new LinkedHashMap<>();
            }
            if (edge.channels != null) {
                existing.channels.putAll(edge.channels);
            }
            Set<String> providers = new LinkedHashSet<>();
            if (existing.providers != null) {
                providers.addAll(existing.providers);
            } else {
                providers.add("signor");
            }
            providers.add("string");
            existing.providers = new ArrayList<>(providers);
            existing.evidence = joinEvidence(joinEvidence(existing.evidence, edge.evidence), "STRING+SIGNOR");
            if (!existing.directed && edge.directed) {
                existing.directed = true;
                if (existing.effect == null || "associates".equals(existing.effect)) {
                    existing.effect = edge.effect;
                }
                double edgeSign = channelSign(edge);
                if (channelSign(existing) == 0 && edgeSign != 0) {
                    existing.channels.put("sign", edgeSign);
                }
            }
        }
        return new ArrayList<>(byKey.values());
    }

    private static double channelSign(InteractomeEdge e) {
        if (e.channels != null && e.channels.containsKey("sign")) {
            return e.channels.get("sign");
        }
        return resolveSignorEffect(e.effect).sign;
    }

    private static Map<String, Double> signorQualityFactors(InteractomeEdge edge, boolean proteinOnly,
            String typeA, String typeB, EffectResolved resolved) {
        Map<String, Double> f = new LinkedHashMap<>();
        f.put("direct", edge.direct == null ? 1.0 : (edge.direct ? 1.0 : 0.72));
        f.put("mechanism", mechanismPrior(edge.mechanism));
        f.put("effect_class", effectClassPrior(resolved.effectClass));
        f.put("pmid", pmidFactor(edge.pmid));
        f.put("specificity", 1.0);
        f.put("context", 1.0);
        f.put("curation", 1.0);
        f.put("complex", 1.0);
        f.put("pathway", present(edge.pathwayId) ? 1.06 : 1.0);
        f.put("entity_type", entityTypeFactor(typeA, typeB, proteinOnly));
        return f;
    }

    private static double mechanismPrior(String mechanism) {
        if (mechanism == null || mechanism.isBlank()) {
            return 0.9;
        }
        String key = mechanism.trim().toLowerCase(Locale.ROOT);
        if (key.contains("phosphorylation") || key.contains("dephosphorylation")) {
            return 1.12;
        }
        if (key.contains("transcriptional")) {
            return 1.1;
        }
        if (key.contains("ubiquitin")) {
            return 1.08;
        }
        if (key.contains("binding")) {
            return 0.95;
        }
        if (key.contains("association")) {
            return 0.92;
        }
        return 1.0;
    }

    private static double effectClassPrior(String effectClass) {
        return switch (effectClass) {
            case "up", "down" -> 1.05;
            case "form_complex" -> 0.98;
            case "unknown" -> 0.92;
            default -> 1.0;
        };
    }

    private static double pmidFactor(String pmid) {
        if (!present(pmid)) {
            return 0.94;
        }
        String[] ids = pmid.split("[;,\\s]+");
        int n = 0;
        for (String id : ids) {
            if (!id.isBlank()) {
                n++;
            }
        }
        if (n >= 3) {
            return 1.1;
        }
        if (n == 2) {
            return 1.06;
        }
        return 1.03;
    }

    private static double entityTypeFactor(String typeA, String typeB, boolean proteinOnly) {
        if (proteinOnly) {
            return 1.0;
        }
        String a = typeA != null ? typeA.toLowerCase(Locale.ROOT) : "";
        String b = typeB != null ? typeB.toLowerCase(Locale.ROOT) : "";
        boolean aOk = a.isEmpty() || a.equals("protein") || a.equals("proteinfamily");
        boolean bOk = b.isEmpty() || b.equals("protein") || b.equals("proteinfamily");
        return aOk && bOk ? 1.0 : 0.82;
    }

    private static final class ChannelQ {
        final double q;
        final Map<String, Double> factors;

        ChannelQ(double q, Map<String, Double> factors) {
            this.q = q;
            this.factors = factors;
        }
    }

    private static ChannelQ stringChannelQuality(Map<String, Double> channels, String networkType) {
        Map<String, Double> ch = channels != null ? channels : Map.of();
        Map<String, Double> factors = new LinkedHashMap<>();
        double weightSum = 0;
        double scoreSum = 0;
        for (Map.Entry<String, Double> e : CHANNEL_WEIGHTS.entrySet()) {
            double raw = ch.getOrDefault(e.getKey(), 0.0);
            if (!Double.isFinite(raw) || raw <= 0) {
                continue;
            }
            weightSum += e.getValue();
            scoreSum += e.getValue() * CoreMapConstants.clamp01(raw);
            factors.put("ch_" + e.getKey(), CoreMapConstants.clamp01(raw));
        }
        double channelMean = weightSum > 0 ? scoreSum / weightSum : 0;
        boolean experimental = ch.getOrDefault("escore", 0.0) > 0 || ch.getOrDefault("dscore", 0.0) > 0;
        boolean textOnly = ch.getOrDefault("tscore", 0.0) > 0
                && ch.getOrDefault("escore", 0.0) <= 0
                && ch.getOrDefault("dscore", 0.0) <= 0
                && ch.getOrDefault("ascore", 0.0) <= 0;
        double mix = 0.55 + 0.45 * channelMean;
        if (experimental) {
            mix *= 1.08;
        }
        if (textOnly) {
            mix *= 0.78;
        }
        if ("physical".equals(networkType)) {
            mix *= 1.05;
        }
        if ("regulatory".equals(networkType)) {
            mix *= 1.03;
        }
        mix = CoreMapConstants.clamp(mix, Q_MIN, Q_MAX);
        factors.put("channel_mix", mix);
        return new ChannelQ(mix, factors);
    }

    private static double product(Map<String, Double> factors) {
        double q = 1;
        for (double v : factors.values()) {
            if (!Double.isFinite(v) || v <= 0) {
                continue;
            }
            q *= v;
        }
        return CoreMapConstants.clamp(q, Q_MIN, Q_MAX);
    }

    private static boolean present(String v) {
        return v != null && !v.isBlank();
    }

    private static String joinEvidence(String a, String b) {
        if (!present(a)) {
            return b;
        }
        if (!present(b)) {
            return a;
        }
        return a + ";" + b;
    }

    private static List<String> listOf(String s) {
        List<String> l = new ArrayList<>();
        l.add(s);
        return l;
    }

    private static InteractomeEdge copy(InteractomeEdge e) {
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
        c.channels = e.channels != null ? new HashMap<>(e.channels) : null;
        c.pathwayId = e.pathwayId;
        c.pmid = e.pmid;
        c.direct = e.direct;
        c.providers = e.providers != null ? new ArrayList<>(e.providers) : null;
        c.qualityFactors = e.qualityFactors != null ? new HashMap<>(e.qualityFactors) : null;
        return c;
    }
}
