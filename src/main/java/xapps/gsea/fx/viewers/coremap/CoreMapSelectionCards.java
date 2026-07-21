/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.coremap;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import edu.mit.broad.coremap.CoreMapTypes.Bridge;
import edu.mit.broad.coremap.CoreMapTypes.GraphEdgeData;
import edu.mit.broad.coremap.CoreMapTypes.GraphNodeData;
import edu.mit.broad.coremap.CoreMapTypes.IntegrationResult;
import edu.mit.broad.coremap.CoreMapTypes.LayerHubSummary;
import edu.mit.broad.coremap.CoreMapTypes.SetEnrichmentMetric;
import edu.mit.broad.coremap.CoreMapTypes.SharedDriverSummary;
import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Selection overlay cards (CoreMap {@code SelectionHoverbox} / card components). */
public final class CoreMapSelectionCards {

    private CoreMapSelectionCards() {
    }

    public static VBox bridgeCard(Bridge bridge, Consumer<String> onGeneClick) {
        VBox card = cardRoot();
        card.getChildren().add(heading(CoreMapCascadeUi.mechLabel(bridge) + " → " + CoreMapCascadeUi.phenoLabel(bridge)));

        int pathCount = bridge.pathCount != null ? bridge.pathCount : 1;
        StringBuilder meta = new StringBuilder(String.format(Locale.ROOT, "Bridge score: %.3f", bridge.bridgeScore));
        if (pathCount > 1) {
            meta.append(" · ").append(pathCount).append(" cascades");
        }
        meta.append(" · ").append(CoreMapCascadeUi.alignmentLabel(bridge.directionAlignment));
        String emp = CoreMapCascadeUi.empiricalPLabel(bridge);
        if (emp != null) {
            meta.append(" · ").append(emp);
        }
        card.getChildren().add(body(meta.toString()));

        String kind = CoreMapCascadeUi.evidenceKindLabel(bridge);
        if (kind != null) {
            card.getChildren().add(new HBox(CoreMapInsightStyles.chip(kind, kind.startsWith("association"))));
        }
        String related = CoreMapCascadeUi.relatedSetsText(bridge);
        if (related != null) {
            card.getChildren().add(muted("Related sets: " + related));
        }
        card.getChildren().add(section("Cascades"));
        card.getChildren().add(CoreMapCascadeUi.cascadeBranchDiagram(bridge, false));

        if (bridge.sharedDrivers != null && !bridge.sharedDrivers.isEmpty()) {
            card.getChildren().add(section("Shared drivers on cascades"));
            FlowPane genes = new FlowPane(6, 4);
            for (String gene : bridge.sharedDrivers) {
                if (gene == null || gene.isBlank()) {
                    continue;
                }
                Hyperlink link = new Hyperlink(gene);
                link.setStyle("-fx-font-size: 11px; -fx-padding: 0;");
                if (onGeneClick != null) {
                    link.setOnAction(e -> onGeneClick.accept(gene));
                }
                genes.getChildren().add(link);
            }
            card.getChildren().add(genes);
        }
        return card;
    }

    public static VBox bridgesCard(List<Bridge> bridges) {
        VBox card = cardRoot();
        card.getChildren().add(heading(bridges.size() + " bridges selected"));
        card.getChildren().add(muted(
                "Highlights cascade genes, cascade edges, and set membership. "
                        + "Ctrl/Cmd+click a bridge to add or remove."));
        for (Bridge path : bridges) {
            VBox row = new VBox(2,
                    body(CoreMapCascadeUi.mechLabel(path) + " → " + CoreMapCascadeUi.phenoLabel(path)),
                    CoreMapCascadeUi.cascadeChainFlow(path.nodes, path.hops),
                    muted(String.format(Locale.ROOT, "score %.3f%s", path.bridgeScore,
                            (path.pathCount != null && path.pathCount > 1)
                                    ? " · " + path.pathCount + " cascades" : "")));
            row.setPadding(new Insets(4, 0, 4, 0));
            card.getChildren().add(row);
        }
        return card;
    }

    public static VBox geneCard(GraphNodeData n, SharedDriverSummary sharedDriver) {
        VBox card = cardRoot();
        card.getChildren().add(heading(n.label != null && !n.label.isBlank() ? n.label : n.id));
        if (n.uniprot != null && !n.uniprot.isBlank()) {
            Hyperlink uni = new Hyperlink(n.uniprot);
            final String accession = n.uniprot;
            uni.setOnAction(e -> openUri("https://www.uniprot.org/uniprotkb/" + accession));
            HBox row = new HBox(6, bold("UniProt:"), uni);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            card.getChildren().add(row);
        }
        if (n.roles != null && !n.roles.isEmpty()) {
            card.getChildren().add(body("Roles: " + String.join(", ", n.roles)));
        } else if (n.role != null) {
            card.getChildren().add(body("Role: " + n.role.wire()));
        }
        String conf = String.format(Locale.ROOT, "Confidence: %.3f", n.confidence);
        if ((n.mechanisticConfidence != null && n.mechanisticConfidence > 0)
                || (n.phenotypicConfidence != null && n.phenotypicConfidence > 0)) {
            conf += String.format(Locale.ROOT, " (mech %.3f, pheno %.3f)",
                    n.mechanisticConfidence != null ? n.mechanisticConfidence : 0.0,
                    n.phenotypicConfidence != null ? n.phenotypicConfidence : 0.0);
        }
        card.getChildren().add(body(conf));
        if (n.evidenceClass != null) {
            card.getChildren().add(body("Evidence: " + evidenceLabel(n.evidenceClass.wire())));
        }
        if (n.rnkScore != null && n.rnkScore != 0) {
            card.getChildren().add(body(String.format(Locale.ROOT, "rnk: %.3f", n.rnkScore)));
        }
        if (n.enrichmentScore != null) {
            card.getChildren().add(body(String.format(Locale.ROOT, "Best |ES|: %.3f", n.enrichmentScore)));
        }
        if (n.nes != null) {
            card.getChildren().add(body(String.format(Locale.ROOT, "Set NES: %.3f", n.nes)));
        }
        if (n.pValue != null) {
            card.getChildren().add(body(String.format(Locale.ROOT, "Best p: %.2e", n.pValue)));
        }
        if (n.fdr != null) {
            card.getChildren().add(body(String.format(Locale.ROOT, "Best FDR: %.2e", n.fdr)));
        }
        card.getChildren().add(body(String.format(Locale.ROOT, "Polarity: mechanistic %s, phenotypic %s",
                polarityLabel(n.mechanisticPolarity), polarityLabel(n.phenotypicPolarity))));

        double sdScore = sharedDriver != null ? sharedDriver.sharedDriverScore
                : (n.sharedDriverScore != null ? n.sharedDriverScore : 0);
        double concord = sharedDriver != null ? sharedDriver.directionConcordance
                : (n.directionConcordance != null ? n.directionConcordance : 0.5);
        if (sharedDriver != null || sdScore > 0) {
            card.getChildren().add(body(String.format(Locale.ROOT, "Shared-driver score: %.3f", sdScore)));
            card.getChildren().add(body(String.format(Locale.ROOT, "Concordance: %.2f", concord)));
        }
        if (n.mechanisticSetNames != null && !n.mechanisticSetNames.isEmpty()) {
            card.getChildren().add(body("Mechanistic sets: " + String.join("; ", n.mechanisticSetNames)));
        } else if (n.mechanisticSets != null && !n.mechanisticSets.isEmpty()) {
            card.getChildren().add(body("Mechanistic sets: " + String.join(", ", n.mechanisticSets)));
        }
        if (n.phenotypicSetNames != null && !n.phenotypicSetNames.isEmpty()) {
            card.getChildren().add(body("Phenotypic sets: " + String.join("; ", n.phenotypicSetNames)));
        } else if (n.phenotypicSets != null && !n.phenotypicSets.isEmpty()) {
            card.getChildren().add(body("Phenotypic sets: " + String.join(", ", n.phenotypicSets)));
        }
        return card;
    }

    public static VBox driverCard(SharedDriverSummary d) {
        VBox card = cardRoot();
        card.getChildren().add(heading(d.gene));
        card.getChildren().add(body(String.format(Locale.ROOT, "Shared-driver score: %.3f", d.sharedDriverScore)));
        card.getChildren().add(body(String.format(Locale.ROOT, "Concordance: %.2f", d.directionConcordance)));
        if (d.mechanisticSets != null && !d.mechanisticSets.isEmpty()) {
            card.getChildren().add(body("Mechanistic sets: " + String.join(", ", d.mechanisticSets)));
        }
        if (d.phenotypicSets != null && !d.phenotypicSets.isEmpty()) {
            card.getChildren().add(body("Phenotypic sets: " + String.join(", ", d.phenotypicSets)));
        }
        return card;
    }

    public static VBox hubCard(LayerHubSummary h) {
        VBox card = cardRoot();
        card.getChildren().add(heading(h.gene));
        card.getChildren().add(body(String.format(Locale.ROOT, "Layer: %s · hub %.3f · degree %d",
                CoreMapInsightStyles.layerLabel(h.layer != null ? h.layer.wire() : ""), h.hubScore, h.degree)));
        if (h.sets != null && !h.sets.isEmpty()) {
            card.getChildren().add(body("Sets: " + String.join(", ", h.sets)));
        }
        return card;
    }

    public static VBox genesCard(List<String> geneIds, IntegrationResult result, Consumer<String> onGeneClick) {
        VBox card = cardRoot();
        card.getChildren().add(heading(geneIds.size() + " genes selected"));
        card.getChildren().add(muted(
                "Highlights each gene’s neighborhood. Ctrl/Cmd+click a gene to add or remove."));
        for (String gene : geneIds) {
            if (gene == null || gene.isBlank()) {
                continue;
            }
            Hyperlink link = new Hyperlink(gene);
            link.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 0;");
            if (onGeneClick != null) {
                link.setOnAction(e -> onGeneClick.accept(gene));
            }
            VBox row = new VBox(2, link, muted(geneMetaLine(gene, result)));
            row.setPadding(new Insets(4, 0, 4, 0));
            card.getChildren().add(row);
        }
        return card;
    }

    public static VBox edgeCard(GraphEdgeData edge, Bridge setLinkBridge, Consumer<String> onGeneClick) {
        if (edge == null) {
            return plain("Edge", "");
        }
        String kind = edge.edgeKind != null ? edge.edgeKind : "mechanism";
        if ("set_link".equals(kind)) {
            return setLinkEdgeCard(edge, setLinkBridge, onGeneClick);
        }
        VBox card = cardRoot();
        card.getChildren().add(heading(
                (edge.source != null ? edge.source : "?") + " → " + (edge.target != null ? edge.target : "?")));
        card.getChildren().addAll(evidenceOriginBlock(edge));
        if ("membership".equals(kind)) {
            card.getChildren().add(body("Membership: gene ↔ enrichment set"));
        }
        if (edge.effect != null && !edge.effect.isBlank()) {
            card.getChildren().add(body("Effect: " + edge.effect));
        }
        if (edge.mechanism != null && !edge.mechanism.isBlank()) {
            card.getChildren().add(body("Mechanism: " + edge.mechanism));
        }
        if (!"membership".equals(kind)) {
            String direct = edge.direct == null ? "—" : (edge.direct ? "yes" : "no");
            card.getChildren().add(body("Direct: " + direct));
        }
        StringBuilder score = new StringBuilder(String.format(Locale.ROOT, "Score: %.3f", edge.weight));
        if (edge.channels != null && edge.channels.get("quality") != null) {
            score.append(String.format(Locale.ROOT, " · Q=%.2f", edge.channels.get("quality")));
        }
        card.getChildren().add(body(score.toString()));
        if (edge.qualityFactors != null && !edge.qualityFactors.isEmpty()) {
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, Double> e : edge.qualityFactors.entrySet()) {
                parts.add(String.format(Locale.ROOT, "%s=%.2f", e.getKey(), e.getValue()));
            }
            card.getChildren().add(muted("Quality factors: " + String.join(" · ", parts)));
        }
        if (edge.evidence != null && !edge.evidence.isBlank()) {
            card.getChildren().add(muted("Evidence tags: " + edge.evidence));
        }
        return card;
    }

    private static VBox setLinkEdgeCard(GraphEdgeData edge, Bridge bridge, Consumer<String> onGeneClick) {
        VBox card = cardRoot();
        String left = bridge != null ? CoreMapCascadeUi.mechLabel(bridge)
                : (edge.source != null ? edge.source : "?");
        String right = bridge != null ? CoreMapCascadeUi.phenoLabel(bridge)
                : (edge.target != null ? edge.target : "?");
        card.getChildren().add(heading(left + " → " + right));
        StringBuilder meta = new StringBuilder(String.format(Locale.ROOT, "Link score: %.3f", edge.weight));
        if (bridge != null) {
            int pathCount = bridge.pathCount != null ? bridge.pathCount : 1;
            if (pathCount > 1) {
                meta.append(" · ").append(pathCount).append(" cascades");
            }
            meta.append(" · ").append(CoreMapCascadeUi.alignmentLabel(bridge.directionAlignment));
        }
        card.getChildren().add(body(meta.toString()));
        if (bridge != null) {
            card.getChildren().add(section("Cascades"));
            card.getChildren().add(CoreMapCascadeUi.cascadeBranchDiagram(bridge, false));
            if (bridge.sharedDrivers != null && !bridge.sharedDrivers.isEmpty()) {
                card.getChildren().add(section("Shared drivers"));
                FlowPane genes = new FlowPane(6, 4);
                for (String gene : bridge.sharedDrivers) {
                    if (gene == null || gene.isBlank()) {
                        continue;
                    }
                    Hyperlink link = new Hyperlink(gene);
                    link.setStyle("-fx-font-size: 11px; -fx-padding: 0;");
                    if (onGeneClick != null) {
                        link.setOnAction(e -> onGeneClick.accept(gene));
                    }
                    genes.getChildren().add(link);
                }
                card.getChildren().add(genes);
            }
        } else if (edge.pathPreview != null && !edge.pathPreview.isBlank()) {
            card.getChildren().add(body("Cascade: " + edge.pathPreview));
        }
        return card;
    }

    private static List<javafx.scene.Node> evidenceOriginBlock(GraphEdgeData edge) {
        List<javafx.scene.Node> nodes = new ArrayList<>();
        if ("membership".equals(edge.edgeKind)) {
            nodes.add(body("From: gene ↔ enrichment set membership"));
            return nodes;
        }
        List<String> providers = new ArrayList<>();
        if (edge.providers != null) {
            for (String p : edge.providers) {
                String label = CoreMapInsightStyles.providerLabel(p);
                if (!label.isBlank() && !providers.contains(label)) {
                    providers.add(label);
                }
            }
        }
        if (providers.isEmpty() && edge.provider != null) {
            String label = CoreMapInsightStyles.providerLabel(edge.provider);
            if (!label.isBlank()) {
                providers.add(label);
            }
        }
        nodes.add(body("From: " + (providers.isEmpty() ? "interactome edge" : String.join(" + ", providers))));
        if (!providers.isEmpty()) {
            HBox chips = new HBox(6);
            for (String p : providers) {
                chips.getChildren().add(CoreMapInsightStyles.chip(p, true));
            }
            String style = edge.cmLineStyle != null ? edge.cmLineStyle : "solid";
            chips.getChildren().add(CoreMapInsightStyles.meta(style + " edge"));
            nodes.add(chips);
        }
        if (edge.channels != null && !edge.channels.isEmpty()
                && (providers.stream().anyMatch(p -> p.equalsIgnoreCase("STRING") || p.equalsIgnoreCase("Fused")))) {
            List<String> ch = new ArrayList<>();
            for (Map.Entry<String, Double> e : edge.channels.entrySet()) {
                if (e.getKey() != null && e.getKey().toLowerCase(Locale.ROOT).contains("score")) {
                    ch.add(String.format(Locale.ROOT, "%s=%.2f",
                            e.getKey().replace("score", ""), e.getValue()));
                }
            }
            if (!ch.isEmpty()) {
                nodes.add(muted("STRING channels: " + String.join(" · ", ch)));
            }
        }
        if (edge.pmid != null && !edge.pmid.isBlank()) {
            String first = edge.pmid.split("[;,\\s]+")[0];
            Hyperlink link = new Hyperlink("PMID " + edge.pmid);
            link.setOnAction(e -> openUri("https://pubmed.ncbi.nlm.nih.gov/" + first + "/"));
            HBox row = new HBox(6, bold("Literature:"), link);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            nodes.add(row);
        }
        if (edge.pathwayId != null && !edge.pathwayId.isBlank()) {
            String text = edge.pathwayName != null && !edge.pathwayName.isBlank()
                    ? edge.pathwayName + " (" + edge.pathwayId + ")"
                    : edge.pathwayId;
            nodes.add(body("Pathway annotation: " + text));
        }
        return nodes;
    }

    private static String geneMetaLine(String gene, IntegrationResult result) {
        if (result == null) {
            return "selected gene";
        }
        if (result.sharedDrivers != null) {
            for (SharedDriverSummary d : result.sharedDrivers) {
                if (d != null && gene.equals(d.gene)) {
                    return String.format(Locale.ROOT, "shared driver %.3f", d.sharedDriverScore);
                }
            }
        }
        if (result.layerHubs != null) {
            for (LayerHubSummary h : result.layerHubs) {
                if (h != null && gene.equals(h.gene)) {
                    return String.format(Locale.ROOT, "%s hub %.3f",
                            CoreMapInsightStyles.layerLabel(h.layer != null ? h.layer.wire() : ""),
                            h.hubScore);
                }
            }
        }
        return "selected gene";
    }

    public static VBox setCard(SetEnrichmentMetric m) {
        return setCard(m, null, null);
    }

    public static VBox setCard(SetEnrichmentMetric m, List<String> memberGenes, Consumer<String> onGeneClick) {
        VBox card = cardRoot();
        String title = m.setName != null && !m.setName.isBlank() ? m.setName : m.setId;
        card.getChildren().add(heading(title));
        if (m.layer != null) {
            card.getChildren().add(body("Layer: " + m.layer.wire() + " set"));
        }
        card.getChildren().add(body("Set ID: " + m.setId));
        if (m.sourcePlatform != null || m.sourceAccession != null) {
            String src = (m.sourcePlatform != null ? m.sourcePlatform : "source")
                    + (m.sourceAccession != null ? " · " + m.sourceAccession : "");
            if (m.externalUrl != null && !m.externalUrl.isBlank()) {
                Hyperlink link = new Hyperlink(src);
                link.setOnAction(e -> openUri(m.externalUrl));
                HBox row = new HBox(6, bold("Source:"), link);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                card.getChildren().add(row);
            } else {
                card.getChildren().add(body("Source: " + src));
            }
        }
        if (m.collection != null && !m.collection.isBlank()) {
            card.getChildren().add(body("Collection: " + m.collection));
        }
        if (memberGenes != null) {
            card.getChildren().add(body("Members in map: " + memberGenes.size()));
        }
        if (m.enrichmentScore != null) {
            card.getChildren().add(body(String.format(Locale.ROOT, "ES: %.3f", m.enrichmentScore)));
        }
        if (m.nes != null) {
            card.getChildren().add(body(String.format(Locale.ROOT, "NES: %.3f", m.nes)));
        }
        if (m.pValue != null) {
            card.getChildren().add(body("NOM p-Val: " + m.pValue));
        }
        if (m.fdr != null) {
            card.getChildren().add(body("FDR: " + m.fdr));
        }
        if (memberGenes != null && !memberGenes.isEmpty()) {
            card.getChildren().add(section("Genes"));
            FlowPane genes = new FlowPane(6, 4);
            for (String gene : memberGenes) {
                if (gene == null || gene.isBlank()) {
                    continue;
                }
                Hyperlink link = new Hyperlink(gene);
                link.setStyle("-fx-font-size: 11px; -fx-padding: 0;");
                if (onGeneClick != null) {
                    final String g = gene;
                    link.setOnAction(e -> onGeneClick.accept(g));
                }
                genes.getChildren().add(link);
            }
            card.getChildren().add(genes);
        }
        return card;
    }

    public static VBox plain(String title, String bodyText) {
        VBox card = cardRoot();
        if (title != null && !title.isBlank()) {
            card.getChildren().add(heading(title));
        }
        Label body = body(bodyText != null ? bodyText : "");
        body.setWrapText(true);
        card.getChildren().add(body);
        return card;
    }

    private static String evidenceLabel(String wire) {
        if ("leading_edge".equals(wire)) {
            return "leading edge (Class 1)";
        }
        if ("ranked_extension".equals(wire)) {
            return "ranked extension (Class 2)";
        }
        if ("unranked_extension".equals(wire)) {
            return "unranked extension (Class 3)";
        }
        return wire != null ? wire : "";
    }

    private static String polarityLabel(Double pol) {
        if (pol == null || pol == 0) {
            return "unsigned";
        }
        return pol > 0 ? "up" : "down";
    }

    private static VBox cardRoot() {
        VBox card = new VBox(6);
        card.setPadding(new Insets(2, 0, 2, 0));
        return card;
    }

    private static Label heading(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setMaxWidth(Double.MAX_VALUE);
        l.getStyleClass().add("coremap-card-heading");
        return l;
    }

    private static Label section(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("coremap-card-section");
        return l;
    }

    private static Label body(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setMaxWidth(Double.MAX_VALUE);
        l.getStyleClass().add("coremap-card-body");
        return l;
    }

    private static Label muted(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setMaxWidth(Double.MAX_VALUE);
        l.getStyleClass().addAll("gsea-muted", "coremap-meta");
        return l;
    }

    private static Label bold(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("coremap-card-section");
        return l;
    }

    private static void openUri(String uri) {
        Thread t = new Thread(() -> {
            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create(uri));
                }
            } catch (Exception ignored) {
                // Browser open is best-effort from the selection card.
            }
        }, "coremap-open-uri");
        t.setDaemon(true);
        t.start();
    }
}
