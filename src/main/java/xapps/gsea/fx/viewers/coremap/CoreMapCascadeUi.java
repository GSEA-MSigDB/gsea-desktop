/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.coremap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import edu.mit.broad.coremap.CoreMapTypes.Bridge;
import edu.mit.broad.coremap.CoreMapTypes.CascadeHop;
import edu.mit.broad.coremap.CoreMapTypes.CascadeSupport;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/** Cascade path formatting / edge-pair helpers (CoreMap {@code cascadeHop.ts} / {@code CascadeDiagram}). */
public final class CoreMapCascadeUi {

    private CoreMapCascadeUi() {
    }

    public static String hopLabel(CascadeHop hop) {
        if (hop == null) {
            return "·";
        }
        List<String> parts = new ArrayList<>();
        if (hop.providers != null && !hop.providers.isEmpty()) {
            parts.add(String.join("+", hop.providers));
        } else if (hop.provider != null && !hop.provider.isBlank()) {
            parts.add(hop.provider);
        }
        if (hop.mechanism != null && !hop.mechanism.isBlank()) {
            parts.add(hop.mechanism);
        }
        if (hop.sign != null && hop.sign == 1) {
            parts.add("+");
        } else if (hop.sign != null && hop.sign == -1) {
            parts.add("−");
        } else if (hop.effect != null && !hop.effect.isBlank()) {
            String effect = hop.effect.replaceAll("(?i)-regulates\\*?$", "").replaceAll("\\*$", "");
            parts.add(effect.isBlank() ? hop.effect : effect);
        }
        return parts.isEmpty() ? "·" : String.join("/", parts);
    }

    public static List<CascadeHop> hopsFor(List<String> nodes, List<CascadeHop> hops) {
        if (hops != null && !hops.isEmpty()) {
            return hops;
        }
        List<CascadeHop> derived = new ArrayList<>();
        if (nodes == null) {
            return derived;
        }
        for (int i = 0; i < nodes.size() - 1; i++) {
            CascadeHop hop = new CascadeHop();
            hop.source = nodes.get(i);
            hop.target = nodes.get(i + 1);
            hop.sign = 0;
            derived.add(hop);
        }
        return derived;
    }

    /** Compact annotated cascade: {@code A —[phos/+]→ B —[ubi/−]→ C}. */
    public static String formatPathPreview(List<String> nodes, List<CascadeHop> hops) {
        if (nodes == null || nodes.isEmpty()) {
            return "";
        }
        List<CascadeHop> resolved = hopsFor(nodes, hops);
        if (resolved.isEmpty()) {
            return String.join(" → ", nodes);
        }
        StringBuilder sb = new StringBuilder(nodes.get(0));
        for (int i = 0; i < resolved.size(); i++) {
            CascadeHop hop = resolved.get(i);
            String next = i + 1 < nodes.size() ? nodes.get(i + 1)
                    : (hop.target != null ? hop.target : "?");
            sb.append(" —[").append(hopLabel(hop)).append("]→ ").append(next);
        }
        return sb.toString();
    }

    public static String formatPathPreview(Bridge bridge) {
        if (bridge == null) {
            return "";
        }
        return formatPathPreview(bridge.nodes, bridge.hops);
    }

    public static List<CascadeSupport> supportsFor(Bridge bridge) {
        List<CascadeSupport> supports = new ArrayList<>();
        if (bridge == null) {
            return supports;
        }
        if (bridge.supportingPaths != null && !bridge.supportingPaths.isEmpty()) {
            supports.addAll(bridge.supportingPaths);
        } else {
            CascadeSupport best = new CascadeSupport();
            best.nodes = bridge.nodes != null ? bridge.nodes : List.of();
            best.hops = bridge.hops;
            best.score = bridge.bridgeScore;
            best.length = bridge.length;
            best.directionAlignment = bridge.directionAlignment;
            best.sharedDrivers = bridge.sharedDrivers;
            supports.add(best);
        }
        return supports;
    }

    /** Directed + reverse pairs for exact cascade edge highlighting. */
    public static Set<String> collectCascadeEdgePairs(Bridge bridge) {
        Set<String> pairs = new HashSet<>();
        if (bridge == null) {
            return pairs;
        }
        for (CascadeSupport support : supportsFor(bridge)) {
            for (CascadeHop hop : hopsFor(support.nodes, support.hops)) {
                if (hop.source == null || hop.target == null) {
                    continue;
                }
                pairs.add(hop.source + "\t" + hop.target);
                pairs.add(hop.target + "\t" + hop.source);
            }
        }
        return pairs;
    }

    public static Set<String> collectCascadeEdgePairs(List<Bridge> bridges) {
        Set<String> pairs = new HashSet<>();
        if (bridges == null) {
            return pairs;
        }
        for (Bridge bridge : bridges) {
            pairs.addAll(collectCascadeEdgePairs(bridge));
        }
        return pairs;
    }

    public static TextFlow cascadeChainFlow(List<String> nodes, List<CascadeHop> hops) {
        TextFlow flow = new TextFlow();
        if (nodes == null || nodes.isEmpty()) {
            return flow;
        }
        List<CascadeHop> resolved = hopsFor(nodes, hops);
        for (int i = 0; i < nodes.size(); i++) {
            Text gene = new Text(nodes.get(i));
            gene.getStyleClass().add("coremap-cascade-gene");
            flow.getChildren().add(gene);
            if (i < resolved.size()) {
                flow.getChildren().add(hopText(resolved.get(i)));
            }
        }
        return flow;
    }

    /**
     * Branched / stacked cascade diagram (CoreMap {@code CascadeBranchDiagram}).
     * Compact mode always stacks flat chains; full mode folds a shared prefix when possible.
     */
    public static Node cascadeBranchDiagram(Bridge bridge, boolean compact) {
        List<CascadeSupport> supports = supportsFor(bridge);
        if (supports.isEmpty()) {
            return new Label("");
        }
        Set<String> starts = new HashSet<>();
        for (CascadeSupport s : supports) {
            if (s.nodes != null && !s.nodes.isEmpty() && s.nodes.get(0) != null) {
                starts.add(s.nodes.get(0));
            }
        }
        BranchNode tree = (!compact && starts.size() == 1 && supports.size() > 1)
                ? buildBranchTree(supports) : null;
        if (tree != null && !tree.children.isEmpty()) {
            VBox box = new VBox(2, renderBranch(tree));
            box.getStyleClass().add("cascade-diagram");
            return box;
        }
        VBox rows = new VBox(4);
        for (CascadeSupport support : supports) {
            VBox row = new VBox(2, cascadeChainFlow(support.nodes, support.hops));
            if (!compact && supports.size() > 1) {
                Label meta = new Label(String.format(Locale.ROOT, "%.3f · %s",
                        support.score, alignmentLabel(support.directionAlignment)));
                meta.getStyleClass().addAll("gsea-muted", "coremap-meta");
                meta.setStyle("-fx-font-size: 10px;");
                row.getChildren().add(meta);
            }
            rows.getChildren().add(row);
        }
        return rows;
    }

    private static BranchNode buildBranchTree(List<CascadeSupport> supports) {
        BranchNode root = new BranchNode("");
        String rootGene = null;
        for (CascadeSupport support : supports) {
            List<String> nodes = support.nodes;
            if (nodes == null || nodes.isEmpty()) {
                continue;
            }
            if (rootGene == null) {
                rootGene = nodes.get(0);
            }
            if (!nodes.get(0).equals(rootGene)) {
                continue;
            }
            List<CascadeHop> hops = hopsFor(nodes, support.hops);
            BranchNode cursor = root;
            cursor.gene = nodes.get(0);
            for (CascadeHop hop : hops) {
                String key = hop.source + "|" + hop.target + "|"
                        + (hop.effect != null ? hop.effect : "") + "|"
                        + (hop.mechanism != null ? hop.mechanism : "") + "|"
                        + (hop.sign != null ? hop.sign : 0);
                BranchEdge child = cursor.children.get(key);
                if (child == null) {
                    child = new BranchEdge(hop, new BranchNode(hop.target != null ? hop.target : "?"));
                    cursor.children.put(key, child);
                }
                cursor = child.next;
            }
        }
        return root.gene.isEmpty() ? null : root;
    }

    private static Node renderBranch(BranchNode node) {
        List<BranchEdge> entries = new ArrayList<>(node.children.values());
        if (entries.isEmpty()) {
            Text gene = new Text(node.gene);
            gene.getStyleClass().add("coremap-cascade-gene");
            return new TextFlow(gene);
        }
        if (entries.size() == 1) {
            BranchEdge e = entries.get(0);
            TextFlow flow = new TextFlow();
            Text gene = new Text(node.gene);
            gene.getStyleClass().add("coremap-cascade-gene");
            flow.getChildren().add(gene);
            flow.getChildren().add(hopText(e.hop));
            Node rest = renderBranch(e.next);
            if (rest instanceof TextFlow tf) {
                flow.getChildren().addAll(tf.getChildren());
                return flow;
            }
            VBox box = new VBox(2, flow, rest);
            return box;
        }
        TextFlow head = new TextFlow();
        Text gene = new Text(node.gene);
        gene.getStyleClass().add("coremap-cascade-gene");
        head.getChildren().add(gene);
        VBox branches = new VBox(4);
        branches.setStyle("-fx-padding: 0 0 0 12;");
        for (BranchEdge e : entries) {
            TextFlow row = new TextFlow();
            row.getChildren().add(hopText(e.hop));
            Node rest = renderBranch(e.next);
            if (rest instanceof TextFlow tf) {
                row.getChildren().addAll(tf.getChildren());
                branches.getChildren().add(row);
            } else {
                branches.getChildren().add(new VBox(2, row, rest));
            }
        }
        return new VBox(2, head, branches);
    }

    private static Text hopText(CascadeHop hop) {
        int sign = hop.sign != null ? hop.sign : 0;
        Text mid = new Text(" —[" + hopLabel(hop) + "]→ ");
        mid.getStyleClass().add(sign > 0
                ? "coremap-cascade-hop-up"
                : sign < 0 ? "coremap-cascade-hop-down" : "coremap-cascade-hop");
        return mid;
    }

    public static String alignmentLabel(double alignment) {
        if (alignment > 0) {
            return "supports";
        }
        if (alignment < 0) {
            return "opposes";
        }
        return "unsigned";
    }

    public static String evidenceKindLabel(Bridge bridge) {
        if (bridge == null || bridge.bridgeEvidenceKind == null) {
            return null;
        }
        return switch (bridge.bridgeEvidenceKind) {
            case ASSOCIATIVE -> "associative";
            case DIRECTED -> "directed";
            default -> bridge.bridgeEvidenceKind.wire();
        };
    }

    public static String empiricalPLabel(Bridge bridge) {
        if (bridge == null || bridge.empiricalP == null) {
            return null;
        }
        String tag = "path_jaccard".equals(bridge.nullStatistic) ? "p(path)" : "p";
        return String.format(Locale.ROOT, "%s≈%.2f", tag, bridge.empiricalP);
    }

    public static String relatedSetsText(Bridge bridge) {
        if (bridge == null) {
            return null;
        }
        List<String> mech = bridge.relatedMechanisticSets != null ? bridge.relatedMechanisticSets : List.of();
        List<String> pheno = bridge.relatedPhenotypicSets != null ? bridge.relatedPhenotypicSets : List.of();
        if (mech.isEmpty() && pheno.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        if (!mech.isEmpty()) {
            parts.add("Mech: " + String.join(", ", mech));
        }
        if (!pheno.isEmpty()) {
            parts.add("Pheno: " + String.join(", ", pheno));
        }
        return String.join(" · ", parts);
    }

    public static String mechLabel(Bridge b) {
        if (b == null) {
            return "";
        }
        return b.mechanisticSetName != null && !b.mechanisticSetName.isBlank()
                ? b.mechanisticSetName : (b.mechanisticSet != null ? b.mechanisticSet : "?");
    }

    public static String phenoLabel(Bridge b) {
        if (b == null) {
            return "";
        }
        return b.phenotypicSetName != null && !b.phenotypicSetName.isBlank()
                ? b.phenotypicSetName : (b.phenotypicSet != null ? b.phenotypicSet : "?");
    }

    public static String bridgeKey(Bridge b) {
        if (b == null) {
            return "";
        }
        return (b.mechanisticSet != null ? b.mechanisticSet : "") + "\t"
                + (b.phenotypicSet != null ? b.phenotypicSet : "") + "\t"
                + b.bridgeScore + "\t" + b.length;
    }

    private static final class BranchNode {
        String gene;
        final Map<String, BranchEdge> children = new LinkedHashMap<>();

        BranchNode(String gene) {
            this.gene = gene != null ? gene : "";
        }
    }

    private static final class BranchEdge {
        final CascadeHop hop;
        final BranchNode next;

        BranchEdge(CascadeHop hop, BranchNode next) {
            this.hop = hop;
            this.next = next;
        }
    }
}
