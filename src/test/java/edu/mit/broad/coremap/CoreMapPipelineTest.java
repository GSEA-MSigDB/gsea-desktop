/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import edu.mit.broad.coremap.CoreMapPipeline.IntegrateRequest;
import edu.mit.broad.coremap.CoreMapTypes.EnrichmentParseResult;
import edu.mit.broad.coremap.CoreMapTypes.EnrichmentRow;
import edu.mit.broad.coremap.CoreMapTypes.IntegrationResult;
import edu.mit.broad.coremap.CoreMapTypes.InteractomeEdge;
import edu.mit.broad.coremap.CoreMapTypes.InteractomeSource;
import edu.mit.broad.coremap.providers.MsigdbResolver;
import edu.mit.broad.coremap.providers.SignorClient;
import edu.mit.broad.coremap.providers.StringClient;
import edu.mit.broad.coremap.providers.UniProtMapper;

public class CoreMapPipelineTest {

    @Test
    public void geneHitStatsPreferBestAbsNesAndMinFdr() {
        EnrichmentRow a = row("TP53", "SET_A", 2.0);
        a.nes = 2.1;
        a.fdr = 0.05;
        a.pValue = 0.01;
        EnrichmentRow b = row("TP53", "SET_B", 1.5);
        b.nes = -2.8;
        b.fdr = 0.2;
        b.pValue = 0.02;
        MechanismGraphShared.GeneHitStats hits = MechanismGraphShared.aggregateGeneHitStats(List.of(a, b));
        assertTrue(hits.nes.get("TP53") == -2.8);
        assertTrue(hits.fdr.get("TP53") == 0.05);
        assertTrue(hits.pValue.get("TP53") == 0.01);
    }

    @Test
    public void noneInteractomeUsesComembership() throws Exception {
        EnrichmentParseResult mech = syntheticLayer("SET1", "TP53", "MDM2", "BAX");
        IntegrateRequest req = new IntegrateRequest();
        req.mechanistic = mech;
        req.phenotypic = new EnrichmentParseResult(List.of(), Map.of());
        req.options = IntegrationOptions.defaults();
        req.options.interactomeSource = InteractomeSource.NONE;
        req.options.enableSourceEnrichment = false;
        req.minNes = 0;
        req.maxNp = 1;
        req.maxFdr = 1;
        IntegrationResult result = CoreMapPipeline.integrate(req, null);
        assertNotNull(result);
        assertTrue(result.elements.nodes.size() >= 2);
        assertNotNull(result.sessionId);

        String resultJson = CoreMapJson.integrationResultJson(result);
        IntegrationResult parsed = CoreMapJson.parseIntegrationResult(resultJson);
        assertNotNull(parsed);
        assertTrue(parsed.elements.nodes.size() == result.elements.nodes.size());
        assertTrue(parsed.bridges.size() == result.bridges.size());
        if (!result.bridges.isEmpty() && result.bridges.get(0).supportingPaths != null
                && !result.bridges.get(0).supportingPaths.isEmpty()) {
            assertTrue(parsed.bridges.get(0).supportingPaths.size()
                    == result.bridges.get(0).supportingPaths.size());
            assertTrue(parsed.bridges.get(0).supportingPaths.get(0).nodes.size()
                    == result.bridges.get(0).supportingPaths.get(0).nodes.size());
        }
        if (!result.elements.nodes.isEmpty()) {
            var src = result.elements.nodes.get(0).data;
            var dst = parsed.elements.nodes.stream()
                    .filter(n -> n.data != null && src.id.equals(n.data.id))
                    .findFirst()
                    .orElseThrow()
                    .data;
            assertTrue(dst.mechanisticSets.size() == src.mechanisticSets.size());
            assertTrue(dst.roles.size() == src.roles.size());
        }

        CoreMapJob job = new CoreMapJob();
        job.mechanisticPath = "/tmp/mech";
        job.options = req.options.copy();
        job.viewMode = "connectome";
        job.providerKey = "none";
        job.result = result;
        String jobJson = CoreMapJson.jobJson(job);
        CoreMapJob loaded = CoreMapJson.parseJob(jobJson);
        assertTrue(loaded.formatVersion == CoreMapJob.FORMAT_VERSION);
        assertTrue("/tmp/mech".equals(loaded.mechanisticPath));
        assertTrue(loaded.options.interactomeSource == InteractomeSource.NONE);

        IntegrationOptions rescoreOpts = req.options.copy();
        rescoreOpts.topKBridges = 5;
        IntegrationResult rescored = CoreMapPipeline.rescore(result.sessionId, rescoreOpts, null);
        assertNotNull(rescored);
        assertTrue(rescored.sessionId.equals(result.sessionId));
    }

    @Test
    public void msigdbHeuristicsResolveAccessions() {
        MsigdbResolver index = new MsigdbResolver();
        assertNotNull(index.resolve("R-HSA-109581", "APOPTOSIS"));
        assertTrue(index.resolve("R-HSA-109581", null).platform == MsigdbResolver.Platform.REACTOME);
        assertTrue(index.resolve("hsa04110", "CELL_CYCLE").platform == MsigdbResolver.Platform.KEGG);
        assertTrue(index.resolve("GO:0006915", null).platform == MsigdbResolver.Platform.GO);
    }

    @Test
    public void attachEmpiricalNullsNoOpWhenZero() {
        List<edu.mit.broad.coremap.CoreMapTypes.Bridge> bridges = new ArrayList<>();
        edu.mit.broad.coremap.CoreMapTypes.Bridge b = new edu.mit.broad.coremap.CoreMapTypes.Bridge();
        b.mechanisticSet = "A";
        b.phenotypicSet = "B";
        bridges.add(b);
        List<edu.mit.broad.coremap.CoreMapTypes.Bridge> out = CascadeNulls.attachEmpiricalNulls(
                bridges, new DiGraph(), MechanismGraphShared.aggregateGeneSets(List.of()),
                MechanismGraphShared.aggregateGeneSets(List.of()), Map.of(), Map.of(), Map.of(), Map.of(),
                IntegrationOptions.defaults(), SearchBudget.compute(4, 1, 1, 3, 4, 4), 0, 1);
        assertTrue(out.get(0).empiricalP == null);
    }

    @Test
    public void enrichmentThresholds() {
        assertTrue(EnrichmentMath.passesThresholds(2.0, 0.01, 0.1, 1.6, 0.05, 0.25));
        assertFalse(EnrichmentMath.passesThresholds(1.0, 0.01, 0.1, 1.6, 0.05, 0.25));
        assertTrue(EnrichmentMath.leadingEdgeOmega(row("G", "S", 2.0)) >= CoreMapConstants.LE_FLOOR);
    }

    @Test
    public void emptySetIdsYieldNoRowsWhileNullUsesThresholds() throws Exception {
        EnrichmentParseResult mech = syntheticLayer("SET1", "TP53", "MDM2");
        IntegrateRequest emptyIds = new IntegrateRequest();
        emptyIds.mechanistic = mech;
        emptyIds.phenotypic = new EnrichmentParseResult(List.of(), Map.of());
        emptyIds.options = IntegrationOptions.defaults();
        emptyIds.options.interactomeSource = InteractomeSource.NONE;
        emptyIds.options.enableSourceEnrichment = false;
        emptyIds.minNes = 0;
        emptyIds.maxNp = 1;
        emptyIds.maxFdr = 1;
        emptyIds.mechanisticSetIds = Set.of();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> CoreMapPipeline.integrate(emptyIds, null));
        assertTrue(ex.getMessage().toLowerCase().contains("no enrichment"));

        IntegrateRequest byThreshold = new IntegrateRequest();
        byThreshold.mechanistic = mech;
        byThreshold.phenotypic = new EnrichmentParseResult(List.of(), Map.of());
        byThreshold.options = emptyIds.options.copy();
        byThreshold.minNes = 0;
        byThreshold.maxNp = 1;
        byThreshold.maxFdr = 1;
        byThreshold.mechanisticSetIds = null;
        IntegrationResult ok = CoreMapPipeline.integrate(byThreshold, null);
        assertNotNull(ok);
        assertTrue(ok.elements.nodes.size() >= 1);

        IntegrateRequest byIds = new IntegrateRequest();
        byIds.mechanistic = mech;
        byIds.phenotypic = new EnrichmentParseResult(List.of(), Map.of());
        byIds.options = emptyIds.options.copy();
        byIds.minNes = 99;
        byIds.maxNp = 0;
        byIds.maxFdr = 0;
        byIds.mechanisticSetIds = Set.of("SET1");
        IntegrationResult selected = CoreMapPipeline.integrate(byIds, null);
        assertNotNull(selected);
        assertTrue(selected.elements.nodes.size() >= 1);
    }

    @Test
    public void uniprotMapperResolvesCacheHits() throws Exception {
        Map<String, String> map = UniProtMapper.mapSymbolsToUniprot(List.of("TP53", "MDM2"));
        assertTrue(map.containsKey("TP53") || map.containsKey("MDM2"));
    }

    /** Live SIGNOR smoke — enable with COREMAP_LIVE_TESTS=1. */
    @Test
    @EnabledIfEnvironmentVariable(named = "COREMAP_LIVE_TESTS", matches = "1")
    public void liveSignorFetch() throws Exception {
        IntegrationOptions opts = IntegrationOptions.defaults();
        opts.interactomeSource = InteractomeSource.SIGNOR;
        opts.signorLevel = 1;
        opts.signorDirectOnly = false;
        List<InteractomeEdge> edges = SignorClient.fetch(List.of("TP53", "MDM2", "CDKN1A"), opts);
        assertFalse(edges.isEmpty(), "SIGNOR should return edges among TP53/MDM2/CDKN1A");
    }

    /** Live STRING smoke — enable with COREMAP_LIVE_TESTS=1. */
    @Test
    @EnabledIfEnvironmentVariable(named = "COREMAP_LIVE_TESTS", matches = "1")
    public void liveStringFetch() throws Exception {
        IntegrationOptions opts = IntegrationOptions.defaults();
        opts.interactomeSource = InteractomeSource.STRING;
        opts.minInteractionScore = 0.4;
        List<InteractomeEdge> edges = StringClient.fetch(List.of("TP53", "MDM2", "BAX"), opts);
        assertFalse(edges.isEmpty(), "STRING should return edges among TP53/MDM2/BAX");
    }

    private static EnrichmentParseResult syntheticLayer(String setId, String... genes) {
        List<EnrichmentRow> rows = new ArrayList<>();
        java.util.Map<String, Double> ranks = new java.util.HashMap<>();
        double r = 2.0;
        for (String g : genes) {
            EnrichmentRow row = new EnrichmentRow(g, setId);
            row.nes = 2.5;
            row.pValue = 0.01;
            row.fdr = 0.05;
            row.rnkScore = r;
            row.leadingEdge = true;
            rows.add(row);
            ranks.put(g, r);
            r -= 0.3;
        }
        return new EnrichmentParseResult(rows, ranks);
    }

    private static EnrichmentRow row(String g, String s, double rnk) {
        EnrichmentRow row = new EnrichmentRow(g, s);
        row.rnkScore = rnk;
        row.leadingEdge = true;
        return row;
    }
}
