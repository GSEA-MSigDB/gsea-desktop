/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.coremap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import edu.mit.broad.coremap.CoreMapTypes.Bridge;
import edu.mit.broad.coremap.CoreMapTypes.GraphEdgeData;
import edu.mit.broad.coremap.CoreMapTypes.GraphNodeData;
import edu.mit.broad.coremap.CoreMapTypes.IntegrationResult;
import edu.mit.broad.coremap.CoreMapTypes.LayerHubSummary;
import edu.mit.broad.coremap.CoreMapTypes.NodeElement;
import xapps.gsea.fx.viewers.coremap.CoreMapGraphViews.GeneVisibility;

public class CoreMapGraphViewsPaintTest {

    @Test
    public void evidenceLineStyleMatchesCoreMapRules() {
        GraphEdgeData membership = new GraphEdgeData();
        membership.edgeKind = "membership";
        assertEquals("dashed", CoreMapGraphPaint.evidenceLineStyle(membership));

        GraphEdgeData stringOnly = new GraphEdgeData();
        stringOnly.provider = "string";
        assertEquals("dotted", CoreMapGraphPaint.evidenceLineStyle(stringOnly));

        GraphEdgeData fused = new GraphEdgeData();
        fused.providers = List.of("signor", "string");
        fused.direct = true;
        assertEquals("solid", CoreMapGraphPaint.evidenceLineStyle(fused));

        GraphEdgeData indirect = new GraphEdgeData();
        indirect.provider = "signor";
        indirect.direct = false;
        assertEquals("dashed", CoreMapGraphPaint.evidenceLineStyle(indirect));

        GraphEdgeData fusedStringOnly = new GraphEdgeData();
        fusedStringOnly.provider = "fused";
        fusedStringOnly.providers = List.of("string");
        assertEquals("dotted", CoreMapGraphPaint.evidenceLineStyle(fusedStringOnly));

        GraphEdgeData fusedBoth = new GraphEdgeData();
        fusedBoth.provider = "fused";
        fusedBoth.providers = List.of("signor", "string");
        fusedBoth.direct = true;
        assertEquals("solid", CoreMapGraphPaint.evidenceLineStyle(fusedBoth));
    }

    @Test
    public void mixHexBlendsTowardTarget() {
        assertEquals("#d97706", CoreMapGraphPaint.mixHex("#F5D08A", "#D97706", 1.0).toLowerCase());
        assertEquals("#f5d08a", CoreMapGraphPaint.mixHex("#F5D08A", "#D97706", 0.0).toLowerCase());
    }

    @Test
    public void edgeArrowsMatchCoreMapSignorSemantics() {
        assertEquals("activation", CoreMapGraphPaint.resolveEdgeArrowRole(edge(
                true, 1, "up-regulates activity")));
        assertEquals("triangle", CoreMapGraphPaint.edgeTargetArrowShape(edge(
                true, 1, "up-regulates activity")));

        assertEquals("inhibition", CoreMapGraphPaint.resolveEdgeArrowRole(edge(
                true, -1, "down-regulates activity")));
        assertEquals("tee", CoreMapGraphPaint.edgeTargetArrowShape(edge(
                true, -1, "down-regulates activity")));

        assertEquals("complex", CoreMapGraphPaint.resolveEdgeArrowRole(edge(
                true, 0, "form complex")));
        assertEquals("square", CoreMapGraphPaint.edgeTargetArrowShape(edge(
                true, 0, "form complex")));

        assertEquals("unknown", CoreMapGraphPaint.resolveEdgeArrowRole(edge(
                true, 0, "unknown")));
        assertEquals("circle", CoreMapGraphPaint.edgeTargetArrowShape(edge(
                true, 0, "unknown")));
    }

    @Test
    public void edgeArrowsMatchCoreMapStringModes() {
        assertEquals("none", CoreMapGraphPaint.resolveEdgeArrowRole(edge(
                false, 0, "associates")));
        assertEquals("none", CoreMapGraphPaint.edgeTargetArrowShape(edge(
                false, 0, "associates")));

        GraphEdgeData binds = edge(false, 0, "binds");
        assertEquals("complex", CoreMapGraphPaint.resolveEdgeArrowRole(binds));
        assertEquals("square", CoreMapGraphPaint.edgeTargetArrowShape(binds));
        assertEquals("square", CoreMapGraphPaint.edgeSourceArrowShape(binds));

        assertEquals("activation", CoreMapGraphPaint.resolveEdgeArrowRole(edge(
                true, 1, "up-regulates")));
        assertEquals("inhibition", CoreMapGraphPaint.resolveEdgeArrowRole(edge(
                true, -1, "down-regulates")));
        assertEquals("unknown", CoreMapGraphPaint.resolveEdgeArrowRole(edge(
                true, 0, "regulates")));
        assertEquals("circle", CoreMapGraphPaint.edgeTargetArrowShape(edge(
                true, 0, "regulates")));
        assertEquals("none", CoreMapGraphPaint.edgeSourceArrowShape(edge(
                true, 0, "regulates")));
    }

    @Test
    public void edgeSignFallsBackToChannelsLikeCoreMap() {
        GraphEdgeData e = edge(true, 0, "down-regulates");
        e.channels = Map.of("sign", -1.0);
        assertEquals(-1, CoreMapGraphPaint.edgeSign(e));
        assertEquals("inhibition", CoreMapGraphPaint.resolveEdgeArrowRole(e));
        assertEquals("tee", CoreMapGraphPaint.edgeTargetArrowShape(e));
    }

    @Test
    public void membershipEdgesHideArrows() {
        GraphEdgeData e = edge(true, 1, "up-regulates");
        e.edgeKind = "membership";
        assertEquals("none", CoreMapGraphPaint.resolveEdgeArrowRole(e));
        assertEquals("none", CoreMapGraphPaint.edgeTargetArrowShape(e));
    }

    @Test
    public void cascadeVisibilityIncludesLayerHubs() {
        IntegrationResult result = new IntegrationResult();
        result.elements.nodes.add(node("A"));
        result.elements.nodes.add(node("HUB1"));
        result.elements.nodes.add(node("OTHER"));

        Bridge bridge = new Bridge();
        bridge.nodes = List.of("A");
        result.bridges.add(bridge);

        LayerHubSummary hub = new LayerHubSummary();
        hub.gene = "HUB1";
        result.layerHubs.add(hub);

        Set<String> visible = CoreMapGraphViews.visibleGeneIds(result, GeneVisibility.CASCADE);
        assertEquals(Set.of("A", "HUB1"), visible);
    }

    private static NodeElement node(String id) {
        GraphNodeData d = new GraphNodeData();
        d.id = id;
        return new NodeElement(d);
    }

    private static GraphEdgeData edge(boolean directed, int sign, String effect) {
        GraphEdgeData e = new GraphEdgeData();
        e.directed = directed;
        e.sign = sign;
        e.effect = effect;
        e.edgeKind = "mechanism";
        return e;
    }
}
