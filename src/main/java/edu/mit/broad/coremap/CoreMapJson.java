/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import edu.mit.broad.coremap.CoreMapTypes.Bridge;
import edu.mit.broad.coremap.CoreMapTypes.BridgeEvidenceKind;
import edu.mit.broad.coremap.CoreMapTypes.CascadeHop;
import edu.mit.broad.coremap.CoreMapTypes.CascadeSupport;
import edu.mit.broad.coremap.CoreMapTypes.EdgeElement;
import edu.mit.broad.coremap.CoreMapTypes.GeneEvidenceClass;
import edu.mit.broad.coremap.CoreMapTypes.GraphEdgeData;
import edu.mit.broad.coremap.CoreMapTypes.GraphElements;
import edu.mit.broad.coremap.CoreMapTypes.GraphNodeData;
import edu.mit.broad.coremap.CoreMapTypes.IntegrationResult;
import edu.mit.broad.coremap.CoreMapTypes.InteractomeSource;
import edu.mit.broad.coremap.CoreMapTypes.Layer;
import edu.mit.broad.coremap.CoreMapTypes.LayerHubSummary;
import edu.mit.broad.coremap.CoreMapTypes.NodeElement;
import edu.mit.broad.coremap.CoreMapTypes.NodeRole;
import edu.mit.broad.coremap.CoreMapTypes.SetEnrichmentMetric;
import edu.mit.broad.coremap.CoreMapTypes.SharedDriverSummary;
import edu.mit.broad.coremap.CoreMapTypes.StringMode;

/**
 * Serialize CoreMap results to JSON for the Cytoscape.js WebView and file export.
 */
@SuppressWarnings("unchecked")
public final class CoreMapJson {

    private CoreMapJson() {
    }

    /** Cytoscape elements payload ({@code nodes}/{@code edges}) for the WebView. */
    public static String elementsJson(List<NodeElement> nodes, List<EdgeElement> edges) {
        JSONObject root = new JSONObject();
        root.put("nodes", nodesArr(nodes, true));
        root.put("edges", edgesArr(edges));
        return root.toJSONString();
    }

    /**
     * Slim WebView inject payload — paint + ids only. Omits membership lists / member_genes /
     * evidence blobs that bloat LiveConnect and are unused by Cytoscape styles.
     */
    public static String webViewElementsJson(List<NodeElement> nodes, List<EdgeElement> edges) {
        JSONObject root = new JSONObject();
        root.put("nodes", webViewNodesArr(nodes));
        root.put("edges", webViewEdgesArr(edges));
        return root.toJSONString();
    }

    /**
     * Full integration result for Export JSON: bridges, drivers, hubs, set metrics,
     * graph elements, and stats.
     */
    public static String integrationResultJson(IntegrationResult result) {
        if (result == null) {
            return "{}";
        }
        JSONObject root = new JSONObject();
        put(root, "session_id", result.sessionId);
        root.put("shared_drivers", sharedDrivers(result.sharedDrivers));
        root.put("layer_hubs", layerHubs(result.layerHubs));
        root.put("bridges", bridges(result.bridges));
        root.put("set_enrichments", setEnrichments(result.setEnrichments));
        JSONObject els = new JSONObject();
        if (result.elements != null) {
            els.put("nodes", nodesArr(result.elements.nodes, false));
            els.put("edges", edgesArr(result.elements.edges));
        } else {
            els.put("nodes", new JSONArray());
            els.put("edges", new JSONArray());
        }
        root.put("elements", els);
        root.put("stats", statsObject(result.stats));
        return root.toJSONString();
    }

    public static String jobJson(CoreMapJob job) {
        if (job == null) {
            return "{}";
        }
        JSONObject root = new JSONObject();
        root.put("format_version", job.formatVersion);
        put(root, "saved_at", job.savedAt);
        put(root, "mechanistic_path", job.mechanisticPath);
        put(root, "phenotypic_path", job.phenotypicPath);
        put(root, "mechanistic_name", job.mechanisticName);
        put(root, "phenotypic_name", job.phenotypicName);
        root.put("min_nes", job.minNes);
        root.put("max_np", job.maxNp);
        root.put("max_fdr", job.maxFdr);
        root.put("separate_layer_thresholds", job.separateLayerThresholds);
        put(root, "pheno_min_nes", job.phenoMinNes);
        put(root, "pheno_max_np", job.phenoMaxNp);
        put(root, "pheno_max_fdr", job.phenoMaxFdr);
        put(root, "mechanistic_set_ids", job.mechanisticSetIds);
        put(root, "phenotypic_set_ids", job.phenotypicSetIds);
        root.put("options", optionsObject(job.options));
        put(root, "view_mode", job.viewMode);
        put(root, "gene_visibility", job.geneVisibility);
        put(root, "provider_key", job.providerKey);
        put(root, "fetched_min_score", job.fetchedMinScore);
        put(root, "result_file", job.resultFile != null ? job.resultFile : CoreMapJob.RESULT_FILE);
        return root.toJSONString();
    }

    public static CoreMapJob parseJob(String json) throws Exception {
        Object parsed = new JSONParser().parse(json);
        if (!(parsed instanceof JSONObject root)) {
            throw new IllegalArgumentException("CoreMap job JSON root must be an object");
        }
        CoreMapJob job = new CoreMapJob();
        job.formatVersion = asInt(root.get("format_version"), CoreMapJob.FORMAT_VERSION);
        job.savedAt = asString(root.get("saved_at"));
        job.mechanisticPath = asString(root.get("mechanistic_path"));
        job.phenotypicPath = asString(root.get("phenotypic_path"));
        job.mechanisticName = asString(root.get("mechanistic_name"));
        job.phenotypicName = asString(root.get("phenotypic_name"));
        job.minNes = asDouble(root.get("min_nes"), CoreMapConstants.DEFAULT_MIN_NES);
        job.maxNp = asDouble(root.get("max_np"), CoreMapConstants.DEFAULT_MAX_NP);
        job.maxFdr = asDouble(root.get("max_fdr"), CoreMapConstants.DEFAULT_MAX_FDR);
        job.separateLayerThresholds = asBoolean(root.get("separate_layer_thresholds"), false);
        job.phenoMinNes = asDoubleObj(root.get("pheno_min_nes"));
        job.phenoMaxNp = asDoubleObj(root.get("pheno_max_np"));
        job.phenoMaxFdr = asDoubleObj(root.get("pheno_max_fdr"));
        job.mechanisticSetIds = asStringList(root.get("mechanistic_set_ids"));
        job.phenotypicSetIds = asStringList(root.get("phenotypic_set_ids"));
        job.options = parseOptions(root.get("options"));
        job.viewMode = asString(root.get("view_mode"));
        if (job.viewMode == null || job.viewMode.isBlank()) {
            job.viewMode = "connectome";
        }
        job.geneVisibility = asString(root.get("gene_visibility"));
        if (job.geneVisibility == null || job.geneVisibility.isBlank()) {
            job.geneVisibility = "cascade";
        }
        job.providerKey = asString(root.get("provider_key"));
        job.fetchedMinScore = asDoubleObj(root.get("fetched_min_score"));
        job.resultFile = asString(root.get("result_file"));
        if (job.resultFile == null || job.resultFile.isBlank()) {
            job.resultFile = CoreMapJob.RESULT_FILE;
        }
        Object embedded = root.get("result");
        if (embedded instanceof JSONObject emb) {
            job.result = parseIntegrationResultObject(emb);
        }
        return job;
    }

    public static IntegrationResult parseIntegrationResult(String json) throws Exception {
        Object parsed = new JSONParser().parse(json);
        if (!(parsed instanceof JSONObject root)) {
            throw new IllegalArgumentException("CoreMap result JSON root must be an object");
        }
        return parseIntegrationResultObject(root);
    }

    private static IntegrationResult parseIntegrationResultObject(JSONObject root) {
        IntegrationResult result = new IntegrationResult();
        result.sessionId = asString(root.get("session_id"));
        result.sharedDrivers = parseSharedDrivers(root.get("shared_drivers"));
        result.layerHubs = parseLayerHubs(root.get("layer_hubs"));
        result.bridges = parseBridges(root.get("bridges"));
        result.setEnrichments = parseSetEnrichments(root.get("set_enrichments"));
        result.stats = parseStats(root.get("stats"));
        Object els = root.get("elements");
        if (els instanceof JSONObject eo) {
            GraphElements ge = new GraphElements();
            ge.nodes = parseNodes(eo.get("nodes"));
            ge.edges = parseEdges(eo.get("edges"));
            result.elements = ge;
        }
        return result;
    }

    private static JSONObject optionsObject(IntegrationOptions opts) {
        JSONObject o = new JSONObject();
        if (opts == null) {
            opts = IntegrationOptions.defaults();
        }
        put(o, "interactome_source", opts.interactomeSource != null ? opts.interactomeSource.wire() : null);
        put(o, "string_mode", opts.stringMode != null ? opts.stringMode.wire() : null);
        o.put("min_interaction_score", opts.minInteractionScore);
        o.put("max_path_length", opts.maxPathLength);
        o.put("path_length_penalty", opts.pathLengthPenalty);
        o.put("top_k_bridges", opts.topKBridges);
        o.put("include_extra_neighbors", opts.includeExtraNeighbors);
        o.put("neighbor_limit", opts.neighborLimit);
        o.put("signor_level", opts.signorLevel);
        put(o, "signor_query_type", opts.signorQueryType);
        put(o, "signor_organism", opts.signorOrganism);
        o.put("signor_protein_only", opts.signorProteinOnly);
        o.put("signor_direct_only", opts.signorDirectOnly);
        put(o, "signor_pathways", opts.signorPathways);
        o.put("direction_weight", opts.directionWeight);
        o.put("shared_driver_direction_weight", opts.sharedDriverDirectionWeight);
        o.put("enable_source_enrichment", opts.enableSourceEnrichment);
        put(o, "msigdb_path", opts.msigdbPath);
        o.put("null_permutations", opts.nullPermutations);
        return o;
    }

    private static IntegrationOptions parseOptions(Object raw) {
        IntegrationOptions o = IntegrationOptions.defaults();
        if (!(raw instanceof JSONObject root)) {
            return o;
        }
        o.interactomeSource = InteractomeSource.fromWire(asString(root.get("interactome_source")));
        o.stringMode = StringMode.fromWire(asString(root.get("string_mode")));
        o.minInteractionScore = asDouble(root.get("min_interaction_score"), o.minInteractionScore);
        o.maxPathLength = asInt(root.get("max_path_length"), o.maxPathLength);
        o.pathLengthPenalty = asDouble(root.get("path_length_penalty"), o.pathLengthPenalty);
        o.topKBridges = asInt(root.get("top_k_bridges"), o.topKBridges);
        o.includeExtraNeighbors = asBoolean(root.get("include_extra_neighbors"), o.includeExtraNeighbors);
        o.neighborLimit = asInt(root.get("neighbor_limit"), o.neighborLimit);
        o.signorLevel = asInt(root.get("signor_level"), o.signorLevel);
        String qt = asString(root.get("signor_query_type"));
        if (qt != null) {
            o.signorQueryType = qt;
        }
        String org = asString(root.get("signor_organism"));
        if (org != null) {
            o.signorOrganism = org;
        }
        o.signorProteinOnly = asBoolean(root.get("signor_protein_only"), o.signorProteinOnly);
        o.signorDirectOnly = asBoolean(root.get("signor_direct_only"), o.signorDirectOnly);
        o.signorPathways = asStringList(root.get("signor_pathways"));
        o.directionWeight = asDouble(root.get("direction_weight"), o.directionWeight);
        o.sharedDriverDirectionWeight = asDouble(root.get("shared_driver_direction_weight"),
                o.sharedDriverDirectionWeight);
        o.enableSourceEnrichment = asBoolean(root.get("enable_source_enrichment"), o.enableSourceEnrichment);
        o.msigdbPath = asString(root.get("msigdb_path"));
        o.nullPermutations = asInt(root.get("null_permutations"), o.nullPermutations);
        return o;
    }

    private static List<SharedDriverSummary> parseSharedDrivers(Object raw) {
        List<SharedDriverSummary> out = new ArrayList<>();
        if (!(raw instanceof JSONArray arr)) {
            return out;
        }
        for (Object item : arr) {
            if (!(item instanceof JSONObject o)) {
                continue;
            }
            SharedDriverSummary s = new SharedDriverSummary();
            s.gene = asString(o.get("gene"));
            s.sharedDriverScore = asDouble(o.get("shared_driver_score"), 0);
            s.mechanisticConfidence = asDouble(o.get("mechanistic_confidence"), 0);
            s.phenotypicConfidence = asDouble(o.get("phenotypic_confidence"), 0);
            s.directionConcordance = asDouble(o.get("direction_concordance"), 0);
            s.mechanisticSets = asStringList(o.get("mechanistic_sets"));
            s.phenotypicSets = asStringList(o.get("phenotypic_sets"));
            out.add(s);
        }
        return out;
    }

    private static List<LayerHubSummary> parseLayerHubs(Object raw) {
        List<LayerHubSummary> out = new ArrayList<>();
        if (!(raw instanceof JSONArray arr)) {
            return out;
        }
        for (Object item : arr) {
            if (!(item instanceof JSONObject o)) {
                continue;
            }
            LayerHubSummary h = new LayerHubSummary();
            h.gene = asString(o.get("gene"));
            h.layer = Layer.fromWire(asString(o.get("layer")));
            h.hubScore = asDouble(o.get("hub_score"), 0);
            h.degree = asInt(o.get("degree"), 0);
            h.weightedDegree = asDouble(o.get("weighted_degree"), 0);
            h.confidence = asDouble(o.get("confidence"), 0);
            h.sets = asStringList(o.get("sets"));
            out.add(h);
        }
        return out;
    }

    private static List<Bridge> parseBridges(Object raw) {
        List<Bridge> out = new ArrayList<>();
        if (!(raw instanceof JSONArray arr)) {
            return out;
        }
        for (Object item : arr) {
            if (!(item instanceof JSONObject o)) {
                continue;
            }
            Bridge b = new Bridge();
            b.bridgeScore = asDouble(o.get("bridge_score"), 0);
            b.length = asInt(o.get("length"), 0);
            b.nodes = asStringList(o.get("nodes"));
            b.mechanisticSet = asString(o.get("mechanistic_set"));
            b.phenotypicSet = asString(o.get("phenotypic_set"));
            b.mechanisticSetName = asString(o.get("mechanistic_set_name"));
            b.phenotypicSetName = asString(o.get("phenotypic_set_name"));
            b.directionAlignment = asDouble(o.get("direction_alignment"), 0);
            b.pathCount = asIntObj(o.get("path_count"));
            b.sharedDrivers = asStringList(o.get("shared_drivers"));
            b.empiricalP = asDoubleObj(o.get("empirical_p"));
            b.nullStatistic = asString(o.get("null_statistic"));
            b.bridgeEvidenceKind = BridgeEvidenceKind.fromWire(asString(o.get("bridge_evidence_kind")));
            b.relatedMechanisticSets = asStringList(o.get("related_mechanistic_sets"));
            b.relatedPhenotypicSets = asStringList(o.get("related_phenotypic_sets"));
            b.hops = parseHops(o.get("hops"));
            b.supportingPaths = parseSupportingPaths(o.get("supporting_paths"));
            out.add(b);
        }
        return out;
    }

    private static List<CascadeHop> parseHops(Object raw) {
        List<CascadeHop> list = new ArrayList<>();
        if (!(raw instanceof JSONArray ha)) {
            return list;
        }
        for (Object hopItem : ha) {
            if (!(hopItem instanceof JSONObject ho)) {
                continue;
            }
            CascadeHop h = new CascadeHop();
            h.source = asString(ho.get("source"));
            h.target = asString(ho.get("target"));
            h.effect = asString(ho.get("effect"));
            h.mechanism = asString(ho.get("mechanism"));
            h.sign = asIntObj(ho.get("sign"));
            h.weight = asDoubleObj(ho.get("weight"));
            h.pathwayId = asString(ho.get("pathway_id"));
            h.provider = asString(ho.get("provider"));
            h.providers = asStringList(ho.get("providers"));
            list.add(h);
        }
        return list;
    }

    private static List<CascadeSupport> parseSupportingPaths(Object raw) {
        List<CascadeSupport> out = new ArrayList<>();
        if (!(raw instanceof JSONArray arr)) {
            return out;
        }
        for (Object item : arr) {
            if (!(item instanceof JSONObject o)) {
                continue;
            }
            CascadeSupport s = new CascadeSupport();
            s.nodes = asStringList(o.get("nodes"));
            s.hops = parseHops(o.get("hops"));
            s.score = asDouble(o.get("score"), 0);
            s.length = asInt(o.get("length"), 0);
            s.directionAlignment = asDouble(o.get("direction_alignment"), 0);
            s.sharedDrivers = asStringList(o.get("shared_drivers"));
            out.add(s);
        }
        return out;
    }

    private static List<SetEnrichmentMetric> parseSetEnrichments(Object raw) {
        List<SetEnrichmentMetric> out = new ArrayList<>();
        if (!(raw instanceof JSONArray arr)) {
            return out;
        }
        for (Object item : arr) {
            if (!(item instanceof JSONObject o)) {
                continue;
            }
            SetEnrichmentMetric m = new SetEnrichmentMetric();
            m.setId = asString(o.get("set_id"));
            m.layer = Layer.fromWire(asString(o.get("layer")));
            m.setName = asString(o.get("set_name"));
            m.nes = asDoubleObj(o.get("nes"));
            m.absNes = asDouble(o.get("abs_nes"), 0);
            m.pValue = asDoubleObj(o.get("p_value"));
            m.fdr = asDoubleObj(o.get("fdr"));
            m.enrichmentScore = asDoubleObj(o.get("enrichment_score"));
            m.sourcePlatform = asString(o.get("source_platform"));
            m.sourceAccession = asString(o.get("source_accession"));
            m.collection = asString(o.get("collection"));
            m.externalUrl = asString(o.get("external_url"));
            out.add(m);
        }
        return out;
    }

    private static List<NodeElement> parseNodes(Object raw) {
        List<NodeElement> out = new ArrayList<>();
        if (!(raw instanceof JSONArray arr)) {
            return out;
        }
        for (Object item : arr) {
            if (!(item instanceof JSONObject wrap)) {
                continue;
            }
            Object dataObj = wrap.get("data");
            if (!(dataObj instanceof JSONObject d)) {
                continue;
            }
            GraphNodeData n = new GraphNodeData();
            n.id = asString(d.get("id"));
            n.label = asString(d.get("label"));
            n.role = NodeRole.fromWire(asString(d.get("role")));
            n.roles = asStringList(d.get("roles"));
            n.mechanisticSets = asStringList(d.get("mechanistic_sets"));
            n.phenotypicSets = asStringList(d.get("phenotypic_sets"));
            n.mechanisticSetNames = asStringList(d.get("mechanistic_set_names"));
            n.phenotypicSetNames = asStringList(d.get("phenotypic_set_names"));
            n.confidence = asDouble(d.get("confidence"), 0);
            n.rnkScore = asDoubleObj(d.get("rnk_score"));
            n.evidenceClass = GeneEvidenceClass.fromWire(asString(d.get("evidence_class")));
            n.mechanisticPolarity = asDoubleObj(d.get("mechanistic_polarity"));
            n.phenotypicPolarity = asDoubleObj(d.get("phenotypic_polarity"));
            n.enrichmentPolarity = asDoubleObj(d.get("enrichment_polarity"));
            n.mechanisticConfidence = asDoubleObj(d.get("mechanistic_confidence"));
            n.phenotypicConfidence = asDoubleObj(d.get("phenotypic_confidence"));
            n.sharedDriverScore = asDoubleObj(d.get("shared_driver_score"));
            n.directionConcordance = asDoubleObj(d.get("direction_concordance"));
            n.uniprot = asString(d.get("uniprot"));
            n.pValue = asDoubleObj(d.get("p_value"));
            n.fdr = asDoubleObj(d.get("fdr"));
            n.nes = asDoubleObj(d.get("nes"));
            n.enrichmentScore = asDoubleObj(d.get("enrichment_score"));
            n.memberGenes = asStringList(d.get("member_genes"));
            n.memberCount = asIntObj(d.get("member_count"));
            n.kind = asString(d.get("kind"));
            n.cmShape = asString(d.get("cm_shape"));
            n.cmColor = asString(d.get("cm_color"));
            n.cmBorder = asString(d.get("cm_border"));
            n.cmWidth = asDoubleObj(d.get("cm_width"));
            n.cmHeight = asDoubleObj(d.get("cm_height"));
            n.cmSplitImg = asString(d.get("cm_split_img"));
            n.cmLeft = asString(d.get("cm_left"));
            n.cmRight = asString(d.get("cm_right"));
            out.add(new NodeElement(n));
        }
        return out;
    }

    private static List<EdgeElement> parseEdges(Object raw) {
        List<EdgeElement> out = new ArrayList<>();
        if (!(raw instanceof JSONArray arr)) {
            return out;
        }
        for (Object item : arr) {
            if (!(item instanceof JSONObject wrap)) {
                continue;
            }
            Object dataObj = wrap.get("data");
            if (!(dataObj instanceof JSONObject d)) {
                continue;
            }
            out.add(new EdgeElement(edgeFromDataObject(d)));
        }
        return out;
    }

    /** Parse a Cytoscape edge {@code data} object (or JSON string) into {@link GraphEdgeData}. */
    public static GraphEdgeData parseEdgeDataJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            Object parsed = new JSONParser().parse(json);
            if (!(parsed instanceof JSONObject d)) {
                return null;
            }
            return edgeFromDataObject(d);
        } catch (Exception ex) {
            return null;
        }
    }

    private static GraphEdgeData edgeFromDataObject(JSONObject d) {
        GraphEdgeData e = new GraphEdgeData();
        e.id = asString(d.get("id"));
        e.source = asString(d.get("source"));
        e.target = asString(d.get("target"));
        e.weight = asDouble(d.get("weight"), 0);
        e.interaction = asDouble(d.get("interaction"), 0);
        e.directed = asBoolean(d.get("directed"), true);
        e.effect = asString(d.get("effect"));
        e.mechanism = asString(d.get("mechanism"));
        e.provider = asString(d.get("provider"));
        e.support = asString(d.get("support"));
        e.sign = asIntObj(d.get("sign"));
        e.polarityCompat = asDoubleObj(d.get("polarity_compat"));
        e.edgeKind = asString(d.get("edge_kind"));
        e.pathPreview = asString(d.get("path_preview"));
        e.pathwayId = asString(d.get("pathway_id"));
        e.pathwayName = asString(d.get("pathway_name"));
        e.pmid = asString(d.get("pmid"));
        e.evidence = asString(d.get("evidence"));
        if (d.get("direct") != null) {
            e.direct = asBoolean(d.get("direct"), true);
        }
        e.providers = asStringList(d.get("providers"));
        e.channels = asDoubleMap(d.get("channels"));
        e.qualityFactors = asDoubleMap(d.get("quality_factors"));
        e.cmWidth = asDoubleObj(d.get("cm_width"));
        e.cmOpacity = asDoubleObj(d.get("cm_opacity"));
        e.cmColor = asString(d.get("cm_color"));
        e.cmArrow = asString(d.get("cm_arrow"));
        e.cmSourceArrow = asString(d.get("cm_source_arrow"));
        e.cmLineStyle = asString(d.get("cm_line_style"));
        return e;
    }

    private static Map<String, Object> parseStats(Object raw) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (!(raw instanceof JSONObject o)) {
            return out;
        }
        for (Object key : o.keySet()) {
            out.put(String.valueOf(key), o.get(key));
        }
        return out;
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static List<String> asStringList(Object v) {
        List<String> out = new ArrayList<>();
        if (!(v instanceof JSONArray arr)) {
            return out;
        }
        for (Object item : arr) {
            if (item != null) {
                out.add(String.valueOf(item));
            }
        }
        return out;
    }

    private static Map<String, Double> asDoubleMap(Object v) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (!(v instanceof JSONObject o)) {
            return out;
        }
        for (Object key : o.keySet()) {
            Double d = asDoubleObj(o.get(key));
            if (key != null && d != null) {
                out.put(String.valueOf(key), d);
            }
        }
        return out;
    }

    private static int asInt(Object v, int def) {
        if (v instanceof Number n) {
            return n.intValue();
        }
        if (v != null) {
            try {
                return Integer.parseInt(String.valueOf(v));
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    private static Integer asIntObj(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double asDouble(Object v, double def) {
        Double d = asDoubleObj(v);
        return d != null ? d : def;
    }

    private static Double asDoubleObj(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean asBoolean(Object v, boolean def) {
        if (v instanceof Boolean b) {
            return b;
        }
        if (v != null) {
            return Boolean.parseBoolean(String.valueOf(v));
        }
        return def;
    }

    private static JSONArray nodesArr(List<NodeElement> nodes, boolean withClasses) {
        JSONArray arr = new JSONArray();
        if (nodes == null) {
            return arr;
        }
        for (NodeElement n : nodes) {
            if (n == null || n.data == null) {
                continue;
            }
            JSONObject wrap = new JSONObject();
            wrap.put("data", nodeData(n.data));
            if (withClasses && n.data.roles != null && !n.data.roles.isEmpty()) {
                wrap.put("classes", String.join(" ", n.data.roles.stream()
                        .map(r -> "role-" + r.replace('_', '-')).toList()));
            }
            arr.add(wrap);
        }
        return arr;
    }

    private static JSONArray edgesArr(List<EdgeElement> edges) {
        JSONArray arr = new JSONArray();
        if (edges == null) {
            return arr;
        }
        for (EdgeElement e : edges) {
            if (e == null || e.data == null) {
                continue;
            }
            JSONObject wrap = new JSONObject();
            wrap.put("data", edgeData(e.data));
            arr.add(wrap);
        }
        return arr;
    }

    private static JSONArray webViewNodesArr(List<NodeElement> nodes) {
        JSONArray arr = new JSONArray();
        if (nodes == null) {
            return arr;
        }
        for (NodeElement n : nodes) {
            if (n == null || n.data == null || n.data.id == null) {
                continue;
            }
            GraphNodeData d = n.data;
            JSONObject data = new JSONObject();
            put(data, "id", d.id);
            put(data, "label", d.label);
            put(data, "kind", d.kind);
            put(data, "cm_shape", d.cmShape);
            put(data, "cm_color", d.cmColor);
            put(data, "cm_border", d.cmBorder);
            put(data, "cm_width", d.cmWidth);
            put(data, "cm_height", d.cmHeight);
            put(data, "cm_split_img", d.cmSplitImg);
            JSONObject wrap = new JSONObject();
            wrap.put("data", data);
            arr.add(wrap);
        }
        return arr;
    }

    private static JSONArray webViewEdgesArr(List<EdgeElement> edges) {
        JSONArray arr = new JSONArray();
        if (edges == null) {
            return arr;
        }
        for (EdgeElement e : edges) {
            if (e == null || e.data == null || e.data.source == null || e.data.target == null) {
                continue;
            }
            GraphEdgeData d = e.data;
            JSONObject data = new JSONObject();
            put(data, "id", d.id);
            put(data, "source", d.source);
            put(data, "target", d.target);
            put(data, "edge_kind", d.edgeKind);
            put(data, "directed", d.directed);
            put(data, "cm_width", d.cmWidth);
            put(data, "cm_opacity", d.cmOpacity);
            put(data, "cm_color", d.cmColor);
            put(data, "cm_arrow", d.cmArrow);
            put(data, "cm_source_arrow", d.cmSourceArrow);
            put(data, "cm_line_style", d.cmLineStyle);
            JSONObject wrap = new JSONObject();
            wrap.put("data", data);
            arr.add(wrap);
        }
        return arr;
    }

    private static JSONObject nodeData(GraphNodeData n) {
        JSONObject o = new JSONObject();
        put(o, "id", n.id);
        put(o, "label", n.label);
        put(o, "role", n.role != null ? n.role.wire() : null);
        put(o, "roles", n.roles);
        put(o, "mechanistic_sets", n.mechanisticSets);
        put(o, "phenotypic_sets", n.phenotypicSets);
        put(o, "mechanistic_set_names", n.mechanisticSetNames);
        put(o, "phenotypic_set_names", n.phenotypicSetNames);
        put(o, "confidence", n.confidence);
        put(o, "rnk_score", n.rnkScore);
        put(o, "evidence_class", n.evidenceClass != null ? n.evidenceClass.wire() : null);
        put(o, "mechanistic_polarity", n.mechanisticPolarity);
        put(o, "phenotypic_polarity", n.phenotypicPolarity);
        put(o, "enrichment_polarity", n.enrichmentPolarity);
        put(o, "mechanistic_confidence", n.mechanisticConfidence);
        put(o, "phenotypic_confidence", n.phenotypicConfidence);
        put(o, "shared_driver_score", n.sharedDriverScore);
        put(o, "direction_concordance", n.directionConcordance);
        put(o, "uniprot", n.uniprot);
        put(o, "p_value", n.pValue);
        put(o, "fdr", n.fdr);
        put(o, "nes", n.nes);
        put(o, "enrichment_score", n.enrichmentScore);
        put(o, "member_genes", n.memberGenes);
        put(o, "member_count", n.memberCount);
        put(o, "kind", n.kind);
        put(o, "cm_shape", n.cmShape);
        put(o, "cm_color", n.cmColor);
        put(o, "cm_border", n.cmBorder);
        put(o, "cm_width", n.cmWidth);
        put(o, "cm_height", n.cmHeight);
        put(o, "cm_split_img", n.cmSplitImg);
        put(o, "cm_left", n.cmLeft);
        put(o, "cm_right", n.cmRight);
        return o;
    }

    private static JSONObject edgeData(GraphEdgeData e) {
        JSONObject o = new JSONObject();
        put(o, "id", e.id);
        put(o, "source", e.source);
        put(o, "target", e.target);
        put(o, "weight", e.weight);
        put(o, "interaction", e.interaction);
        put(o, "directed", e.directed);
        put(o, "effect", e.effect);
        put(o, "mechanism", e.mechanism);
        put(o, "evidence", e.evidence);
        put(o, "provider", e.provider);
        put(o, "support", e.support);
        put(o, "sign", e.sign);
        put(o, "polarity_compat", e.polarityCompat);
        put(o, "edge_kind", e.edgeKind);
        put(o, "path_preview", e.pathPreview);
        put(o, "pathway_id", e.pathwayId);
        put(o, "pathway_name", e.pathwayName);
        put(o, "pmid", e.pmid);
        put(o, "direct", e.direct);
        put(o, "providers", e.providers);
        put(o, "channels", e.channels);
        put(o, "quality_factors", e.qualityFactors);
        put(o, "cm_width", e.cmWidth);
        put(o, "cm_opacity", e.cmOpacity);
        put(o, "cm_color", e.cmColor);
        put(o, "cm_arrow", e.cmArrow);
        put(o, "cm_source_arrow", e.cmSourceArrow);
        put(o, "cm_line_style", e.cmLineStyle);
        return o;
    }

    private static JSONArray sharedDrivers(List<SharedDriverSummary> list) {
        JSONArray arr = new JSONArray();
        if (list == null) {
            return arr;
        }
        for (SharedDriverSummary s : list) {
            JSONObject o = new JSONObject();
            put(o, "gene", s.gene);
            put(o, "shared_driver_score", s.sharedDriverScore);
            put(o, "mechanistic_confidence", s.mechanisticConfidence);
            put(o, "phenotypic_confidence", s.phenotypicConfidence);
            put(o, "direction_concordance", s.directionConcordance);
            put(o, "mechanistic_sets", s.mechanisticSets);
            put(o, "phenotypic_sets", s.phenotypicSets);
            arr.add(o);
        }
        return arr;
    }

    private static JSONArray layerHubs(List<LayerHubSummary> list) {
        JSONArray arr = new JSONArray();
        if (list == null) {
            return arr;
        }
        for (LayerHubSummary h : list) {
            JSONObject o = new JSONObject();
            put(o, "gene", h.gene);
            put(o, "layer", h.layer != null ? h.layer.wire() : null);
            put(o, "hub_score", h.hubScore);
            put(o, "degree", h.degree);
            put(o, "weighted_degree", h.weightedDegree);
            put(o, "confidence", h.confidence);
            put(o, "sets", h.sets);
            arr.add(o);
        }
        return arr;
    }

    private static JSONArray bridges(List<Bridge> list) {
        JSONArray arr = new JSONArray();
        if (list == null) {
            return arr;
        }
        for (Bridge b : list) {
            JSONObject o = new JSONObject();
            put(o, "bridge_score", b.bridgeScore);
            put(o, "length", b.length);
            put(o, "nodes", b.nodes);
            put(o, "mechanistic_set", b.mechanisticSet);
            put(o, "phenotypic_set", b.phenotypicSet);
            put(o, "mechanistic_set_name", b.mechanisticSetName);
            put(o, "phenotypic_set_name", b.phenotypicSetName);
            put(o, "direction_alignment", b.directionAlignment);
            put(o, "path_count", b.pathCount);
            put(o, "shared_drivers", b.sharedDrivers);
            put(o, "empirical_p", b.empiricalP);
            put(o, "null_statistic", b.nullStatistic);
            if (b.bridgeEvidenceKind != null) {
                put(o, "bridge_evidence_kind", b.bridgeEvidenceKind.wire());
            }
            put(o, "related_mechanistic_sets", b.relatedMechanisticSets);
            put(o, "related_phenotypic_sets", b.relatedPhenotypicSets);
            if (b.hops != null) {
                o.put("hops", hopsArr(b.hops));
            }
            if (b.supportingPaths != null && !b.supportingPaths.isEmpty()) {
                o.put("supporting_paths", supportingPathsArr(b.supportingPaths));
            }
            arr.add(o);
        }
        return arr;
    }

    private static JSONArray hopsArr(List<CascadeHop> hops) {
        JSONArray out = new JSONArray();
        if (hops == null) {
            return out;
        }
        for (CascadeHop h : hops) {
            JSONObject ho = new JSONObject();
            put(ho, "source", h.source);
            put(ho, "target", h.target);
            put(ho, "effect", h.effect);
            put(ho, "mechanism", h.mechanism);
            put(ho, "sign", h.sign);
            put(ho, "weight", h.weight);
            put(ho, "pathway_id", h.pathwayId);
            put(ho, "provider", h.provider);
            put(ho, "providers", h.providers);
            out.add(ho);
        }
        return out;
    }

    private static JSONArray supportingPathsArr(List<CascadeSupport> supports) {
        JSONArray out = new JSONArray();
        if (supports == null) {
            return out;
        }
        for (CascadeSupport s : supports) {
            JSONObject o = new JSONObject();
            put(o, "nodes", s.nodes);
            put(o, "score", s.score);
            put(o, "length", s.length);
            put(o, "direction_alignment", s.directionAlignment);
            put(o, "shared_drivers", s.sharedDrivers);
            if (s.hops != null) {
                o.put("hops", hopsArr(s.hops));
            }
            out.add(o);
        }
        return out;
    }

    private static JSONArray setEnrichments(List<SetEnrichmentMetric> list) {
        JSONArray arr = new JSONArray();
        if (list == null) {
            return arr;
        }
        for (SetEnrichmentMetric m : list) {
            JSONObject o = new JSONObject();
            put(o, "set_id", m.setId);
            put(o, "layer", m.layer != null ? m.layer.wire() : null);
            put(o, "set_name", m.setName);
            put(o, "nes", m.nes);
            put(o, "abs_nes", m.absNes);
            put(o, "p_value", m.pValue);
            put(o, "fdr", m.fdr);
            put(o, "enrichment_score", m.enrichmentScore);
            put(o, "source_platform", m.sourcePlatform);
            put(o, "source_accession", m.sourceAccession);
            put(o, "collection", m.collection);
            put(o, "external_url", m.externalUrl);
            arr.add(o);
        }
        return arr;
    }

    private static JSONObject statsObject(Map<String, Object> stats) {
        JSONObject o = new JSONObject();
        if (stats == null) {
            return o;
        }
        for (Map.Entry<String, Object> e : stats.entrySet()) {
            Object v = toJsonValue(e.getValue());
            if (v != null) {
                o.put(e.getKey(), v);
            }
        }
        return o;
    }

    private static Object toJsonValue(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof String || val instanceof Number || val instanceof Boolean) {
            return val;
        }
        if (val instanceof List<?> list) {
            JSONArray arr = new JSONArray();
            for (Object item : list) {
                Object converted = toJsonValue(item);
                if (converted != null) {
                    arr.add(converted);
                }
            }
            return arr;
        }
        if (val instanceof Map<?, ?> map) {
            JSONObject o = new JSONObject();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() == null) {
                    continue;
                }
                Object converted = toJsonValue(e.getValue());
                if (converted != null) {
                    o.put(String.valueOf(e.getKey()), converted);
                }
            }
            return o;
        }
        return String.valueOf(val);
    }

    private static void put(JSONObject o, String key, Object val) {
        Object converted = toJsonValue(val);
        if (converted != null) {
            o.put(key, converted);
        }
    }
}
