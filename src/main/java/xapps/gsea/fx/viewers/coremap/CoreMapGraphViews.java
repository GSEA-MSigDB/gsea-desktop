/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.coremap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.mit.broad.coremap.CoreMapConstants;
import edu.mit.broad.coremap.CoreMapTypes.Bridge;
import edu.mit.broad.coremap.CoreMapTypes.EdgeElement;
import edu.mit.broad.coremap.CoreMapTypes.GraphEdgeData;
import edu.mit.broad.coremap.CoreMapTypes.GraphNodeData;
import edu.mit.broad.coremap.CoreMapTypes.IntegrationResult;
import edu.mit.broad.coremap.CoreMapTypes.InteractomeSource;
import edu.mit.broad.coremap.CoreMapTypes.LayerHubSummary;
import edu.mit.broad.coremap.CoreMapTypes.NodeElement;
import edu.mit.broad.coremap.CoreMapTypes.NodeRole;
import edu.mit.broad.coremap.CoreMapTypes.SetEnrichmentMetric;
import edu.mit.broad.coremap.CoreMapTypes.SharedDriverSummary;

/**
 * Build Cytoscape view elements (connectome / genes / sets) and stamp paint fields.
 * Mirrors CoreMap {@code graphViews.ts}: engine elements are gene–gene only; set nodes,
 * membership, and set_link edges are view constructs.
 */
public final class CoreMapGraphViews {

    public enum ViewMode {
        CONNECTOME, GENES, SETS
    }

    /** Gene filter for Connectome / Gene Networks (matches CoreMap {@code GeneVisibility}). */
    public enum GeneVisibility {
        /** Genes on any ranked bridge cascade (default). */
        CASCADE,
        /** Genes that participate in at least one mechanism edge. */
        MECHANISM,
        /** Every gene in the serialized result. */
        ALL
    }

    private CoreMapGraphViews() {
    }

    public static final class ViewElements {
        public final List<NodeElement> nodes = new ArrayList<>();
        public final List<EdgeElement> edges = new ArrayList<>();
    }

    public static ViewElements buildViewElements(IntegrationResult result, ViewMode mode,
            InteractomeSource source) {
        return buildViewElements(result, mode, source, GeneVisibility.CASCADE);
    }

    public static ViewElements buildViewElements(IntegrationResult result, ViewMode mode,
            InteractomeSource source, GeneVisibility geneVisibility) {
        ViewElements out = new ViewElements();
        if (result == null || result.elements == null) {
            return out;
        }
        GeneVisibility visibility = geneVisibility != null ? geneVisibility : GeneVisibility.CASCADE;
        boolean includeComembership = source == InteractomeSource.NONE
                || (result.stats != null && "none".equals(String.valueOf(result.stats.get("interactome_source"))));

        Map<String, GraphNodeData> geneData = new HashMap<>();
        for (NodeElement n : result.elements.nodes) {
            if (n.data != null && n.data.id != null) {
                geneData.put(n.data.id, n.data);
            }
        }

        if (mode == ViewMode.SETS) {
            SetIndex index = collectSetIndex(result);
            addSetNodes(out, result, geneData.keySet(), index, result.bridges != null && !result.bridges.isEmpty());
            addSetLinkEdges(out, result);
            paintAll(out);
            return out;
        }

        Set<String> visibleGenes = visibleGeneIds(result, visibility);
        if (mode == ViewMode.CONNECTOME) {
            SetIndex index = collectSetIndex(result);
            addSetNodes(out, result, visibleGenes, index, false);
            Set<String> usedMech = usedSetIds(out, "mechanistic");
            Set<String> usedPheno = usedSetIds(out, "phenotypic");
            addMembershipEdges(out, result, visibleGenes, usedMech, usedPheno);
        }

        for (String gid : visibleGenes) {
            GraphNodeData src = geneData.get(gid);
            if (src == null) {
                continue;
            }
            GraphNodeData copy = copyNode(src);
            copy.kind = "gene";
            out.nodes.add(new NodeElement(copy));
        }

        for (EdgeElement e : result.elements.edges) {
            if (e.data == null) {
                continue;
            }
            if (isComembership(e.data) && !includeComembership) {
                continue;
            }
            if (!visibleGenes.contains(e.data.source) || !visibleGenes.contains(e.data.target)) {
                continue;
            }
            GraphEdgeData copy = copyEdge(e.data);
            if (copy.edgeKind == null) {
                copy.edgeKind = "mechanism";
            }
            out.edges.add(new EdgeElement(copy));
        }

        // Connectome is bipartite membership + mechanism only (no set_link — that is Sets mode).
        paintAll(out);
        return out;
    }

    private static boolean isComembership(GraphEdgeData data) {
        if ("comembership".equals(data.support)) {
            return true;
        }
        if (data.provider != null && (data.provider.equals("go") || data.provider.equals("hpo")
                || data.provider.equals("geneset"))) {
            return true;
        }
        return data.channels != null && data.channels.getOrDefault("comembership", 0.0) > 0;
    }

    static Set<String> visibleGeneIds(IntegrationResult result, GeneVisibility mode) {
        Set<String> all = new HashSet<>();
        if (result.elements != null && result.elements.nodes != null) {
            for (NodeElement n : result.elements.nodes) {
                if (n.data != null && n.data.id != null) {
                    all.add(n.data.id);
                }
            }
        }
        if (mode == GeneVisibility.ALL || all.isEmpty()) {
            return all;
        }
        if (mode == GeneVisibility.CASCADE) {
            Set<String> cascade = genesOnCascades(result);
            if (!cascade.isEmpty()) {
                Set<String> out = new HashSet<>();
                for (String id : all) {
                    if (cascade.contains(id)) {
                        out.add(id);
                    }
                }
                if (!out.isEmpty()) {
                    return out;
                }
            }
        }
        Set<String> linked = genesWithMechanismLinks(result);
        if (linked.isEmpty()) {
            return all;
        }
        Set<String> out = new HashSet<>();
        for (String id : all) {
            if (linked.contains(id)) {
                out.add(id);
            }
        }
        return out.isEmpty() ? all : out;
    }

    private static Set<String> genesOnCascades(IntegrationResult result) {
        Set<String> cascade = new HashSet<>();
        if (result.bridges != null) {
            for (Bridge b : result.bridges) {
                if (b.nodes != null) {
                    cascade.addAll(b.nodes);
                }
                if (b.supportingPaths != null) {
                    for (var support : b.supportingPaths) {
                        if (support.nodes != null) {
                            cascade.addAll(support.nodes);
                        }
                    }
                }
                if (b.sharedDrivers != null) {
                    cascade.addAll(b.sharedDrivers);
                }
            }
        }
        if (result.sharedDrivers != null) {
            for (SharedDriverSummary s : result.sharedDrivers) {
                if (s.gene != null) {
                    cascade.add(s.gene);
                }
            }
        }
        // Ranked layer hubs (≤ TOP_LAYER_HUBS_MAX per layer). Including them here keeps hub
        // clicks on the cheap highlight path instead of forcing a Genes=all rebuild/layout in
        // WebView. maybeWidenGeneVisibility already switches partial maps to ALL.
        if (result.layerHubs != null) {
            for (LayerHubSummary h : result.layerHubs) {
                if (h != null && h.gene != null && !h.gene.isBlank()) {
                    cascade.add(h.gene);
                }
            }
        }
        return cascade;
    }

    private static Set<String> genesWithMechanismLinks(IntegrationResult result) {
        Set<String> linked = new HashSet<>();
        if (result.elements == null || result.elements.edges == null) {
            return linked;
        }
        for (EdgeElement e : result.elements.edges) {
            if (e.data != null) {
                linked.add(e.data.source);
                linked.add(e.data.target);
            }
        }
        return linked;
    }

    private static final class SetIndexEntry {
        String name;
        final Set<String> genes = new HashSet<>();
        double confidence;
        Double nes;
        double absNes;
        Double pValue;
        Double fdr;
    }

    private static final class SetIndex {
        final Map<String, SetIndexEntry> mechanistic = new HashMap<>();
        final Map<String, SetIndexEntry> phenotypic = new HashMap<>();
    }

    private static SetIndex collectSetIndex(IntegrationResult result) {
        SetIndex index = new SetIndex();
        if (result.setEnrichments != null) {
            for (SetEnrichmentMetric m : result.setEnrichments) {
                if (m == null || m.setId == null || m.layer == null) {
                    continue;
                }
                Map<String, SetIndexEntry> target = m.layer.wire().equals("mechanistic")
                        ? index.mechanistic : index.phenotypic;
                SetIndexEntry e = new SetIndexEntry();
                e.name = m.setName != null && !m.setName.isBlank() ? m.setName : m.setId;
                e.nes = m.nes;
                e.absNes = m.absNes > 0 ? m.absNes : (m.nes != null ? Math.abs(m.nes) : 0);
                e.confidence = Math.min(1.0, e.absNes / 3.0);
                e.pValue = m.pValue;
                e.fdr = m.fdr;
                target.put(m.setId, e);
            }
        }
        if (result.elements != null && result.elements.nodes != null) {
            for (NodeElement n : result.elements.nodes) {
                if (n.data == null) {
                    continue;
                }
                double conf = n.data.confidence > 0 ? n.data.confidence : 0.25;
                addGeneToSets(index.mechanistic, n.data.mechanisticSets, n.data.mechanisticSetNames,
                        n.data.id, conf);
                addGeneToSets(index.phenotypic, n.data.phenotypicSets, n.data.phenotypicSetNames,
                        n.data.id, conf);
            }
        }
        if (result.bridges != null) {
            for (Bridge b : result.bridges) {
                if (b.mechanisticSet != null) {
                    SetIndexEntry e = index.mechanistic.computeIfAbsent(b.mechanisticSet, k -> emptyEntry(
                            b.mechanisticSetName != null ? b.mechanisticSetName : b.mechanisticSet));
                    if (b.mechanisticSetName != null) {
                        e.name = b.mechanisticSetName;
                    }
                    if (b.nodes != null && !b.nodes.isEmpty()) {
                        e.genes.add(b.nodes.get(0));
                    }
                }
                if (b.phenotypicSet != null) {
                    SetIndexEntry e = index.phenotypic.computeIfAbsent(b.phenotypicSet, k -> emptyEntry(
                            b.phenotypicSetName != null ? b.phenotypicSetName : b.phenotypicSet));
                    if (b.phenotypicSetName != null) {
                        e.name = b.phenotypicSetName;
                    }
                    if (b.nodes != null && !b.nodes.isEmpty()) {
                        e.genes.add(b.nodes.get(b.nodes.size() - 1));
                    }
                }
            }
        }
        return index;
    }

    private static SetIndexEntry emptyEntry(String name) {
        SetIndexEntry e = new SetIndexEntry();
        e.name = name;
        e.confidence = 0.25;
        return e;
    }

    private static void addGeneToSets(Map<String, SetIndexEntry> map, List<String> setIds,
            List<String> setNames, String gene, double conf) {
        if (setIds == null) {
            return;
        }
        for (int i = 0; i < setIds.size(); i++) {
            String setId = setIds.get(i);
            if (setId == null) {
                continue;
            }
            String name = setNames != null && i < setNames.size() && setNames.get(i) != null
                    ? setNames.get(i) : setId;
            SetIndexEntry e = map.computeIfAbsent(setId, k -> emptyEntry(name));
            e.genes.add(gene);
            e.confidence = Math.max(e.confidence, conf);
            if (name != null && !name.isBlank()) {
                e.name = name;
            }
        }
    }

    private static void addSetNodes(ViewElements out, IntegrationResult result, Set<String> geneIds,
            SetIndex index, boolean onlyLinkedSets) {
        Set<String> linkedMech = new HashSet<>();
        Set<String> linkedPheno = new HashSet<>();
        if (result.bridges != null) {
            for (Bridge b : result.bridges) {
                if (b.mechanisticSet != null) {
                    linkedMech.add(b.mechanisticSet);
                }
                if (b.phenotypicSet != null) {
                    linkedPheno.add(b.phenotypicSet);
                }
            }
        }
        pushSets(out, index.mechanistic, "mechanistic", geneIds, linkedMech, onlyLinkedSets);
        pushSets(out, index.phenotypic, "phenotypic", geneIds, linkedPheno, onlyLinkedSets);
    }

    private static void pushSets(ViewElements out, Map<String, SetIndexEntry> entries, String layer,
            Set<String> geneIds, Set<String> linked, boolean onlyLinkedSets) {
        for (Map.Entry<String, SetIndexEntry> en : entries.entrySet()) {
            String setId = en.getKey();
            SetIndexEntry entry = en.getValue();
            if (onlyLinkedSets && !linked.contains(setId)) {
                continue;
            }
            List<String> members = new ArrayList<>();
            for (String g : entry.genes) {
                if (geneIds.contains(g)) {
                    members.add(g);
                }
            }
            if (members.isEmpty() && !linked.contains(setId)) {
                continue;
            }
            members.sort(String::compareToIgnoreCase);
            GraphNodeData n = new GraphNodeData();
            n.id = "set:" + layer + ":" + setId;
            n.label = shortSetLabel(setId, entry.name);
            n.kind = "set";
            n.role = "mechanistic".equals(layer) ? NodeRole.MECHANISTIC : NodeRole.PHENOTYPIC;
            n.roles = new ArrayList<>(List.of(layer + "_set"));
            n.nes = entry.nes;
            n.confidence = entry.confidence;
            n.pValue = entry.pValue;
            n.fdr = entry.fdr;
            n.memberGenes = members;
            n.memberCount = members.size();
            if (entry.nes != null) {
                n.enrichmentPolarity = (double) CoreMapConstants.sign(entry.nes);
            }
            out.nodes.add(new NodeElement(n));
        }
    }

    private static Set<String> usedSetIds(ViewElements out, String layer) {
        Set<String> ids = new HashSet<>();
        String prefix = "set:" + layer + ":";
        for (NodeElement n : out.nodes) {
            if (n.data != null && n.data.id != null && n.data.id.startsWith(prefix)) {
                ids.add(n.data.id.substring(prefix.length()));
            }
        }
        return ids;
    }

    private static void addMembershipEdges(ViewElements out, IntegrationResult result,
            Set<String> visibleGenes, Set<String> usedMech, Set<String> usedPheno) {
        Set<String> seen = new HashSet<>();
        if (result.elements != null && result.elements.nodes != null) {
            for (NodeElement n : result.elements.nodes) {
                if (n.data == null || !visibleGenes.contains(n.data.id)) {
                    continue;
                }
                if (n.data.mechanisticSets != null) {
                    for (String s : n.data.mechanisticSets) {
                        if (!usedMech.contains(s)) {
                            continue;
                        }
                        pushMembership(out, seen, "mem:set:mechanistic:" + s + ":" + n.data.id,
                                "set:mechanistic:" + s, n.data.id);
                    }
                }
                if (n.data.phenotypicSets != null) {
                    for (String s : n.data.phenotypicSets) {
                        if (!usedPheno.contains(s)) {
                            continue;
                        }
                        pushMembership(out, seen, "mem:" + n.data.id + ":set:phenotypic:" + s,
                                n.data.id, "set:phenotypic:" + s);
                    }
                }
            }
        }
        if (result.bridges != null) {
            for (Bridge b : result.bridges) {
                if (b.nodes == null || b.nodes.isEmpty()) {
                    continue;
                }
                String start = b.nodes.get(0);
                String end = b.nodes.get(b.nodes.size() - 1);
                if (b.mechanisticSet != null && visibleGenes.contains(start) && usedMech.contains(b.mechanisticSet)) {
                    pushMembership(out, seen, "mem:set:mechanistic:" + b.mechanisticSet + ":" + start,
                            "set:mechanistic:" + b.mechanisticSet, start);
                }
                if (b.phenotypicSet != null && visibleGenes.contains(end) && usedPheno.contains(b.phenotypicSet)) {
                    pushMembership(out, seen, "mem:" + end + ":set:phenotypic:" + b.phenotypicSet,
                            end, "set:phenotypic:" + b.phenotypicSet);
                }
            }
        }
    }

    private static void pushMembership(ViewElements out, Set<String> seen, String id,
            String source, String target) {
        if (!seen.add(id)) {
            return;
        }
        GraphEdgeData e = new GraphEdgeData();
        e.id = id;
        e.source = source;
        e.target = target;
        e.weight = 0.35;
        e.interaction = 0.35;
        e.directed = false;
        e.sign = 0;
        e.edgeKind = "membership";
        e.evidence = "leading-edge membership";
        out.edges.add(new EdgeElement(e));
    }

    private static void addSetLinkEdges(ViewElements out, IntegrationResult result) {
        if (result.bridges == null) {
            return;
        }
        Map<String, Bridge> best = new HashMap<>();
        for (Bridge b : result.bridges) {
            if (b.mechanisticSet == null || b.phenotypicSet == null) {
                continue;
            }
            String key = b.mechanisticSet + "->" + b.phenotypicSet;
            Bridge cur = best.get(key);
            if (cur == null || b.bridgeScore > cur.bridgeScore) {
                best.put(key, b);
            }
        }
        for (Bridge b : best.values()) {
            GraphEdgeData e = new GraphEdgeData();
            e.id = "setlink:" + b.mechanisticSet + "->" + b.phenotypicSet;
            e.source = "set:mechanistic:" + b.mechanisticSet;
            e.target = "set:phenotypic:" + b.phenotypicSet;
            e.weight = CoreMapConstants.clamp(b.bridgeScore, 0.15, 2.5);
            e.interaction = e.weight;
            e.directed = true;
            e.sign = CoreMapConstants.sign(b.directionAlignment);
            e.edgeKind = "set_link";
            e.pathPreview = CoreMapCascadeUi.formatPathPreview(b);
            out.edges.add(new EdgeElement(e));
        }
    }

    private static void paintAll(ViewElements out) {
        for (NodeElement n : out.nodes) {
            paintNode(n.data);
        }
        for (EdgeElement e : out.edges) {
            paintEdge(e.data);
        }
    }

    private static void paintNode(GraphNodeData n) {
        String role = n.role != null ? n.role.wire() : "interactor";
        if (n.roles != null) {
            for (String r : n.roles) {
                if (r != null && r.endsWith("_set")) {
                    role = r;
                    break;
                }
            }
            if (n.roles.contains("shared_driver")) {
                role = "shared_driver";
            }
        }
        n.cmShape = shapeFor(role);
        double pol = polarityFor(n, role);
        double depth = sizeMagnitude(n);
        String[] palette = paletteFor(role);
        if ("shared_driver".equals(role)) {
            String left = tone(paletteFor("mechanistic"),
                    n.mechanisticPolarity != null ? n.mechanisticPolarity : pol);
            String right = tone(paletteFor("phenotypic"),
                    n.phenotypicPolarity != null ? n.phenotypicPolarity : pol);
            n.cmLeft = left;
            n.cmRight = right;
            n.cmColor = left;
            n.cmBorder = "#9A3412";
            n.cmSplitImg = sharedDriverSplitImage(left, right);
        } else {
            n.cmColor = tone(palette, pol);
            n.cmBorder = palette[3];
            n.cmLeft = n.cmColor;
            n.cmRight = n.cmColor;
            n.cmSplitImg = null;
        }
        if ("set".equals(n.kind)) {
            n.cmWidth = 36 + depth * 22;
            n.cmHeight = 28 + depth * 10;
        } else {
            double size = 26 + depth * 20;
            n.cmWidth = size;
            n.cmHeight = size;
        }
    }

    /** Hard left|right split as SVG data URI (Cytoscape clips to diamond). */
    static String sharedDriverSplitImage(String left, String right) {
        String svg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE svg>"
                + "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\">"
                + "<rect x=\"0\" y=\"0\" width=\"50\" height=\"100\" fill=\"" + left + "\"/>"
                + "<rect x=\"50\" y=\"0\" width=\"50\" height=\"100\" fill=\"" + right + "\"/>"
                + "</svg>";
        return "data:image/svg+xml;utf8," + java.net.URLEncoder.encode(svg, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private static double polarityFor(GraphNodeData n, String role) {
        if (role.startsWith("mechanistic") && n.mechanisticPolarity != null) {
            return n.mechanisticPolarity;
        }
        if (role.startsWith("phenotypic") && n.phenotypicPolarity != null) {
            return n.phenotypicPolarity;
        }
        if (n.enrichmentPolarity != null) {
            return n.enrichmentPolarity;
        }
        if (n.nes != null) {
            return CoreMapConstants.sign(n.nes);
        }
        if (n.rnkScore != null) {
            return CoreMapConstants.sign(n.rnkScore);
        }
        return 0;
    }

    private static void paintEdge(GraphEdgeData e) {
        String kind = e.edgeKind != null ? e.edgeKind : "mechanism";
        if ("membership".equals(kind)) {
            e.cmWidth = 1.1;
            e.cmOpacity = 0.4;
            e.cmColor = "#C4CAD3";
            e.cmArrow = "none";
            e.cmSourceArrow = "none";
            e.cmLineStyle = "dashed";
            return;
        }
        if ("set_link".equals(kind)) {
            e.cmWidth = 1.6 + 1.4 * Math.min(e.weight, 2);
            e.cmOpacity = 0.8;
            e.cmColor = "#16A34A";
            e.cmArrow = "triangle";
            e.cmSourceArrow = "none";
            e.cmLineStyle = "solid";
            return;
        }
        if (isComembership(e)) {
            e.cmWidth = 0.9 + 2.5 * CoreMapConstants.clamp01(e.interaction);
            e.cmOpacity = 0.45;
            e.cmColor = "#94A3B8";
            e.cmArrow = "none";
            e.cmSourceArrow = "none";
            e.cmLineStyle = "dashed";
            return;
        }
        double compat = e.polarityCompat != null ? e.polarityCompat : 1.0;
        e.cmWidth = 0.9 + 4.2 * CoreMapConstants.clamp01(e.interaction);
        e.cmOpacity = 0.35 + 0.55 * CoreMapConstants.clamp01(compat);
        e.cmLineStyle = evidenceLineStyle(e);
        int sign = edgeSign(e);
        // Soften color by polarity_compat like CoreMap edgeSignalColor.
        if (sign > 0) {
            e.cmColor = mixHex("#F5D08A", "#D97706", CoreMapConstants.clamp01(compat));
        } else if (sign < 0) {
            e.cmColor = mixHex("#9AD4E0", "#0E7490", CoreMapConstants.clamp01(compat));
        } else {
            e.cmColor = mixHex("#D0D5DC", "#8A93A0", CoreMapConstants.clamp01(compat));
        }
        // CoreMap edgeTargetArrowShape / edgeSourceArrowShape.
        e.cmArrow = edgeTargetArrowShape(e);
        e.cmSourceArrow = edgeSourceArrowShape(e);
    }

    /** CoreMap {@code edgeSignFromData}: top-level sign, else {@code channels.sign}. */
    static int edgeSign(GraphEdgeData e) {
        if (e == null) {
            return 0;
        }
        if (e.sign != null && e.sign != 0) {
            return e.sign;
        }
        if (e.channels != null && e.channels.get("sign") != null) {
            return CoreMapConstants.sign(e.channels.get("sign"));
        }
        return e.sign != null ? e.sign : 0;
    }

    /**
     * CoreMap {@code resolveEdgeArrowRole} → Cytoscape arrow shape
     * (activation→triangle, inhibition→tee, complex→square, unknown→circle, none).
     */
    static String edgeTargetArrowShape(GraphEdgeData e) {
        if (e == null) {
            return "triangle";
        }
        String kind = e.edgeKind != null ? e.edgeKind : "mechanism";
        if ("set_link".equals(kind)) {
            return "triangle";
        }
        return arrowShapeForRole(resolveEdgeArrowRole(e));
    }

    /** CoreMap {@code edgeSourceArrowShape}: square both ends for undirected complex/binding. */
    static String edgeSourceArrowShape(GraphEdgeData e) {
        if (e == null) {
            return "none";
        }
        String role = resolveEdgeArrowRole(e);
        if (!e.directed && "complex".equals(role)) {
            return "square";
        }
        return "none";
    }

    /** CoreMap {@code resolveEdgeArrowRole}. */
    static String resolveEdgeArrowRole(GraphEdgeData e) {
        String kind = e.edgeKind != null ? e.edgeKind : "mechanism";
        if ("membership".equals(kind)) {
            return "none";
        }
        int sign = edgeSign(e);
        if (sign > 0) {
            return "activation";
        }
        if (sign < 0) {
            return "inhibition";
        }
        String effect = e.effect != null ? e.effect.trim().toLowerCase(java.util.Locale.ROOT) : "";
        if (effectLooksComplex(effect)) {
            return "complex";
        }
        if (!e.directed) {
            return "none";
        }
        if (effect.contains("unknown") || "?".equals(effect) || "regulates".equals(effect)
                || "n/a".equals(effect)) {
            return "unknown";
        }
        // Directed causal edge without resolved polarity (e.g. STRING regulatory).
        return "unknown";
    }

    private static String arrowShapeForRole(String role) {
        return switch (role) {
            case "activation" -> "triangle";
            case "inhibition" -> "tee";
            case "complex" -> "square";
            case "unknown" -> "circle";
            default -> "none";
        };
    }

    /** Linear RGB mix (CoreMap {@code mixHex}). */
    static String mixHex(String a, String b, double t) {
        double clamp = Math.min(1, Math.max(0, t));
        int[] ar = parseHex(a);
        int[] br = parseHex(b);
        return String.format("#%02x%02x%02x",
                (int) Math.round(ar[0] + (br[0] - ar[0]) * clamp),
                (int) Math.round(ar[1] + (br[1] - ar[1]) * clamp),
                (int) Math.round(ar[2] + (br[2] - ar[2]) * clamp));
    }

    private static int[] parseHex(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        return new int[] {
                Integer.parseInt(h.substring(0, 2), 16),
                Integer.parseInt(h.substring(2, 4), 16),
                Integer.parseInt(h.substring(4, 6), 16)
        };
    }

    /**
     * Provenance line style (CoreMap {@code evidenceLineStyle}): STRING-only dotted;
     * SIGNOR indirect dashed; membership/comembership dashed; else solid.
     */
    static String evidenceLineStyle(GraphEdgeData e) {
        if (e == null) {
            return "solid";
        }
        String kind = e.edgeKind != null ? e.edgeKind : "mechanism";
        if ("membership".equals(kind)) {
            return "dashed";
        }
        java.util.Set<String> providers = resolveProviders(e);
        if (providers.contains("geneset") || providers.contains("go") || providers.contains("hpo")
                || "comembership".equals(e.support)) {
            return "dashed";
        }
        // Prefer concrete providers; "fused" alone is not STRING-only.
        if (providers.contains("string") && !providers.contains("signor")) {
            return "dotted";
        }
        if (Boolean.FALSE.equals(e.direct)) {
            return "dashed";
        }
        return "solid";
    }

    private static java.util.Set<String> resolveProviders(GraphEdgeData e) {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
        if (e.providers != null) {
            for (String p : e.providers) {
                if (p != null && !p.isBlank()) {
                    out.add(p.trim().toLowerCase(java.util.Locale.ROOT));
                }
            }
        }
        // Match CoreMap: ignore composite "fused" tag when providers[] is present.
        if (e.provider != null && !e.provider.isBlank()) {
            String p = e.provider.trim().toLowerCase(java.util.Locale.ROOT);
            if (!"fused".equals(p) || out.isEmpty()) {
                if (!"fused".equals(p)) {
                    out.add(p);
                }
            }
        }
        return out;
    }

    /** CoreMap {@code effectLooksComplex}. */
    private static boolean effectLooksComplex(String effect) {
        return effect.contains("form complex") || effect.contains("form-complex")
                || "complex".equals(effect) || "binds".equals(effect) || effect.contains("binding");
    }

    private static double sizeMagnitude(GraphNodeData n) {
        if ("set".equals(n.kind) && n.nes != null) {
            return Math.min(1.0, Math.abs(n.nes) / 3.0);
        }
        if (n.rnkScore != null && n.rnkScore != 0) {
            return Math.min(1.0, Math.abs(n.rnkScore) / 2.0);
        }
        return CoreMapConstants.clamp01(n.confidence);
    }

    private static String shapeFor(String role) {
        return switch (role) {
            case "shared_driver" -> "diamond";
            case "mechanistic_set" -> "round-rectangle";
            case "phenotypic_set" -> "barrel";
            case "mechanistic" -> "triangle";
            case "phenotypic" -> "hexagon";
            default -> "ellipse";
        };
    }

    /** Returns [up, down, neutral, border]. */
    private static String[] paletteFor(String role) {
        if (role.startsWith("mechanistic")) {
            return new String[] { "#D97706", "#0E7490", "#6B7280", "#B45309" };
        }
        if (role.startsWith("phenotypic")) {
            return new String[] { "#E11D48", "#4338CA", "#78716C", "#BE123C" };
        }
        if ("shared_driver".equals(role)) {
            return new String[] { "#D97706", "#E11D48", "#6B7280", "#9A3412" };
        }
        return new String[] { "#78716C", "#57534E", "#6E7785", "#57534E" };
    }

    private static String tone(String[] palette, double polarity) {
        return polarity > 0 ? palette[0] : polarity < 0 ? palette[1] : palette[2];
    }

    private static String shortSetLabel(String setId, String setName) {
        String raw = (setName != null && !setName.isBlank() && !setName.equals(setId) ? setName : setId).trim();
        if (raw.length() <= 28) {
            return raw;
        }
        return raw.substring(0, 26) + "…";
    }

    private static GraphNodeData copyNode(GraphNodeData s) {
        GraphNodeData n = new GraphNodeData();
        n.id = s.id;
        n.label = s.label;
        n.role = s.role;
        n.roles = s.roles != null ? new ArrayList<>(s.roles) : new ArrayList<>();
        n.mechanisticSets = s.mechanisticSets != null ? new ArrayList<>(s.mechanisticSets) : new ArrayList<>();
        n.phenotypicSets = s.phenotypicSets != null ? new ArrayList<>(s.phenotypicSets) : new ArrayList<>();
        n.mechanisticSetNames = s.mechanisticSetNames != null ? new ArrayList<>(s.mechanisticSetNames) : new ArrayList<>();
        n.phenotypicSetNames = s.phenotypicSetNames != null ? new ArrayList<>(s.phenotypicSetNames) : new ArrayList<>();
        n.confidence = s.confidence;
        n.rnkScore = s.rnkScore;
        n.evidenceClass = s.evidenceClass;
        n.mechanisticPolarity = s.mechanisticPolarity;
        n.phenotypicPolarity = s.phenotypicPolarity;
        n.enrichmentPolarity = s.enrichmentPolarity;
        n.mechanisticConfidence = s.mechanisticConfidence;
        n.phenotypicConfidence = s.phenotypicConfidence;
        n.sharedDriverScore = s.sharedDriverScore;
        n.directionConcordance = s.directionConcordance;
        n.uniprot = s.uniprot;
        n.pValue = s.pValue;
        n.fdr = s.fdr;
        n.nes = s.nes;
        n.enrichmentScore = s.enrichmentScore;
        return n;
    }

    private static GraphEdgeData copyEdge(GraphEdgeData s) {
        GraphEdgeData e = new GraphEdgeData();
        e.id = s.id;
        e.source = s.source;
        e.target = s.target;
        e.weight = s.weight;
        e.interaction = s.interaction;
        e.directed = s.directed;
        e.effect = s.effect;
        e.mechanism = s.mechanism;
        e.evidence = s.evidence;
        e.provider = s.provider;
        e.providers = s.providers != null ? new ArrayList<>(s.providers) : null;
        e.support = s.support;
        e.sign = s.sign;
        e.polarityCompat = s.polarityCompat;
        e.edgeKind = s.edgeKind;
        e.direct = s.direct;
        e.pmid = s.pmid;
        e.pathwayId = s.pathwayId;
        e.pathwayName = s.pathwayName;
        e.channels = s.channels != null ? new HashMap<>(s.channels) : null;
        e.parallels = s.parallels;
        e.pathPreview = s.pathPreview;
        return e;
    }
}
