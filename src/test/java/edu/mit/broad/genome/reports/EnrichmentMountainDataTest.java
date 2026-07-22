/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.Test;

import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.DefaultRankedList;
import edu.mit.broad.genome.objects.RankedList;

class EnrichmentMountainDataTest {

    @Test
    void peakAndLeadingEdgeAgreeBetweenSparseHits() {
        RankedList rl = sampleRl();
        int[] hits = {1, 3, 5};
        // Peak deviation at hit index 1 (rank 3), positive ES
        Vector esAtHits = new Vector(new float[]{0.2f, 0.8f, 0.1f});
        EnrichmentMountainData data = EnrichmentMountainData.build(esAtHits, null, hits, rl);

        assertEquals(3, data.peakRank);
        assertEquals(0.8f, data.peakEs, 1e-5);
        assertTrue(data.leadingEdge[0]);
        assertTrue(data.leadingEdge[1]);
        assertFalse(data.leadingEdge[2]);
        // Sparse curve: endpoint + hits + endpoint
        assertEquals(5, data.esY.length);
        assertEquals(0.0, data.esY[0], 1e-9);
        assertEquals(0.0, data.esY[data.esY.length - 1], 1e-9);
    }

    @Test
    void prefersDenseEsCurveWhenProvided() {
        RankedList rl = sampleRl();
        int[] hits = {2};
        Vector esAtHits = new Vector(new float[]{0.5f});
        Vector full = new Vector(new float[]{0f, 0.1f, 0.5f, 0.2f, 0f, 0f, 0f, 0f, 0f, 0f});
        EnrichmentMountainData data = EnrichmentMountainData.build(esAtHits, full, hits, rl);
        assertEquals(10, data.esY.length);
        assertEquals(0.5, data.esY[2], 1e-5);
    }

    @Test
    void payloadJsonUsesSharedPeakBoundsSafely() throws Exception {
        RankedList rl = sampleRl();
        int[] hits = {1, 4};
        Vector esAtHits = new Vector(new float[]{0.3f, -0.9f});
        Vector full = new Vector(new float[]{0f, 0.3f, 0.2f, 0.1f, -0.9f, -0.5f, -0.2f, -0.1f, 0f, 0f});
        String json = ModernEnrichmentPlotJson.buildPayloadJson(
                "SET", rl, hits, esAtHits, full,
                "A", "B", 0.3f, -1.2f, 0.01f, 0.05f, 0.1f, null);
        JSONObject root = (JSONObject) new JSONParser().parse(json);
        assertEquals(ModernEnrichmentPlotJson.PAYLOAD_VERSION, ((Number) root.get("version")).intValue());
        assertEquals(4, ((Number) root.get("peakRank")).intValue());
        JSONArray hitArr = (JSONArray) root.get("hits");
        assertEquals(2, hitArr.size());
        JSONObject h0 = (JSONObject) hitArr.get(0);
        JSONObject h1 = (JSONObject) hitArr.get(1);
        // Negative peak → leading edge from peak toward end
        assertFalse((Boolean) h0.get("leadingEdge"));
        assertTrue((Boolean) h1.get("leadingEdge"));
        assertTrue(root.get("colorBar") instanceof JSONArray);
        assertTrue(ModernEnrichmentPlotJson.isCurrentPayload(json, rl.getSize()));
    }

    private static RankedList sampleRl() {
        java.util.List<String> names = new java.util.ArrayList<>();
        float[] scores = new float[10];
        for (int i = 0; i < 10; i++) {
            names.add("G" + i);
            scores[i] = 5f - i;
        }
        return new DefaultRankedList("rl", names, new Vector(scores));
    }
}
