/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Domain types for the CoreMap engine (ported from CoreMap {@code types.ts}). */
public final class CoreMapTypes {

    private CoreMapTypes() {
    }

    public enum NodeRole {
        SHARED_DRIVER("shared_driver"),
        MECHANISTIC("mechanistic"),
        PHENOTYPIC("phenotypic"),
        INTERACTOR("interactor");

        private final String wire;

        NodeRole(String wire) {
            this.wire = wire;
        }

        public String wire() {
            return wire;
        }

        public static NodeRole fromWire(String s) {
            if (s == null) {
                return INTERACTOR;
            }
            for (NodeRole r : values()) {
                if (r.wire.equals(s)) {
                    return r;
                }
            }
            return INTERACTOR;
        }
    }

    public enum InteractomeSource {
        NONE("none"),
        SIGNOR("signor"),
        STRING("string"),
        FUSED("fused");

        private final String wire;

        InteractomeSource(String wire) {
            this.wire = wire;
        }

        public String wire() {
            return wire;
        }

        public static InteractomeSource fromWire(String s) {
            if (s == null || s.isBlank()) {
                return SIGNOR;
            }
            for (InteractomeSource v : values()) {
                if (v.wire.equalsIgnoreCase(s.trim())) {
                    return v;
                }
            }
            return SIGNOR;
        }
    }

    public enum StringMode {
        FUNCTIONAL("functional"),
        REGULATORY("regulatory"),
        INTEGRATED("integrated"),
        PHYSICAL("physical");

        private final String wire;

        StringMode(String wire) {
            this.wire = wire;
        }

        public String wire() {
            return wire;
        }

        public static StringMode fromWire(String s) {
            if (s == null) {
                return INTEGRATED;
            }
            for (StringMode v : values()) {
                if (v.wire.equalsIgnoreCase(s.trim())) {
                    return v;
                }
            }
            return INTEGRATED;
        }
    }

    public enum Layer {
        MECHANISTIC("mechanistic"),
        PHENOTYPIC("phenotypic");

        private final String wire;

        Layer(String wire) {
            this.wire = wire;
        }

        public String wire() {
            return wire;
        }

        public static Layer fromWire(String s) {
            if (s == null) {
                return MECHANISTIC;
            }
            for (Layer v : values()) {
                if (v.wire.equalsIgnoreCase(s)) {
                    return v;
                }
            }
            return MECHANISTIC;
        }
    }

    public enum GeneEvidenceClass {
        LEADING_EDGE("leading_edge"),
        RANKED_EXTENSION("ranked_extension"),
        UNRANKED_EXTENSION("unranked_extension");

        private final String wire;

        GeneEvidenceClass(String wire) {
            this.wire = wire;
        }

        public String wire() {
            return wire;
        }

        public static GeneEvidenceClass fromWire(String s) {
            if (s == null) {
                return null;
            }
            for (GeneEvidenceClass v : values()) {
                if (v.wire.equalsIgnoreCase(s)) {
                    return v;
                }
            }
            return null;
        }
    }

    public enum BridgeEvidenceKind {
        DIRECTED("directed"),
        ASSOCIATIVE("associative");

        private final String wire;

        BridgeEvidenceKind(String wire) {
            this.wire = wire;
        }

        public String wire() {
            return wire;
        }

        public static BridgeEvidenceKind fromWire(String s) {
            if (s == null) {
                return null;
            }
            for (BridgeEvidenceKind v : values()) {
                if (v.wire.equalsIgnoreCase(s)) {
                    return v;
                }
            }
            return null;
        }
    }

    public enum BuildProgressPhase {
        PARSING("parsing"),
        FETCHING_INTERACTOME("fetching_interactome"),
        RANKING_CASCADES("ranking_cascades"),
        FINALIZING("finalizing"),
        READY("ready");

        private final String wire;

        BuildProgressPhase(String wire) {
            this.wire = wire;
        }

        public String wire() {
            return wire;
        }
    }

    public static final class BuildProgress {
        public final BuildProgressPhase phase;
        public final String detail;

        public BuildProgress(BuildProgressPhase phase, String detail) {
            this.phase = phase;
            this.detail = detail != null ? detail : "";
        }
    }

    public static final class EnrichmentRow {
        public String geneSymbol;
        public String setId;
        public String setName;
        public Double nes;
        public Double enrichmentScore;
        public Double rnkScore;
        public Double pValue;
        public Double fdr;
        public boolean leadingEdge = true;

        public EnrichmentRow() {
        }

        public EnrichmentRow(String geneSymbol, String setId) {
            this.geneSymbol = geneSymbol;
            this.setId = setId;
            this.setName = setId;
        }
    }

    public static final class EnrichmentParseResult {
        public final List<EnrichmentRow> rows;
        /** Gene → signed companion .rnk score. */
        public final Map<String, Double> ranks;

        public EnrichmentParseResult(List<EnrichmentRow> rows, Map<String, Double> ranks) {
            this.rows = rows != null ? rows : new ArrayList<>();
            this.ranks = ranks != null ? ranks : new LinkedHashMap<>();
        }
    }

    public static final class GeneSetSummary {
        public String setId;
        public String setName;
        public Double nes;
        public Double pValue;
        public Double fdr;
        public int leadingEdgeCount;
        public List<String> leadingEdgeGenes = new ArrayList<>();
        public boolean passedFilter;
    }

    public static final class SharedDriverSummary {
        public String gene;
        public List<String> mechanisticSets = new ArrayList<>();
        public List<String> phenotypicSets = new ArrayList<>();
        public double mechanisticConfidence;
        public double phenotypicConfidence;
        public double sharedDriverScore;
        public double mechanisticPolarity;
        public double phenotypicPolarity;
        public double directionConcordance;
    }

    public static final class LayerHubSummary {
        public String gene;
        public Layer layer;
        public List<String> sets = new ArrayList<>();
        public double confidence;
        public double polarity;
        public int degree;
        public double weightedDegree;
        public double hubScore;
    }

    public static final class CascadeHopAlt {
        public String effect;
        public String mechanism;
        public Integer sign;
        public Double weight;
        public String provider;
    }

    public static final class CascadeHop {
        public String source;
        public String target;
        public String effect;
        public String mechanism;
        public Integer sign;
        public Double weight;
        public String pathwayId;
        public String provider;
        public List<String> providers;
        public List<CascadeHopAlt> parallels;
    }

    public static final class CascadeSupport {
        public List<String> nodes = new ArrayList<>();
        public List<CascadeHop> hops = new ArrayList<>();
        public double score;
        public int length;
        public double directionAlignment;
        public List<String> sharedDrivers = new ArrayList<>();
    }

    public static final class Bridge {
        public List<String> nodes = new ArrayList<>();
        public List<CascadeHop> hops = new ArrayList<>();
        public double bridgeScore;
        public int length;
        public double directionAlignment;
        public String mechanisticSet;
        public String phenotypicSet;
        public String mechanisticSetName;
        public String phenotypicSetName;
        public List<String> sharedDrivers = new ArrayList<>();
        public Integer pathCount;
        public List<CascadeSupport> supportingPaths = new ArrayList<>();
        public BridgeEvidenceKind bridgeEvidenceKind;
        public List<String> relatedMechanisticSets = new ArrayList<>();
        public List<String> relatedPhenotypicSets = new ArrayList<>();
        public Double empiricalP;
        public String nullStatistic;
    }

    public static final class GraphNodeData {
        public String id;
        public String label;
        public NodeRole role = NodeRole.INTERACTOR;
        public List<String> roles = new ArrayList<>();
        public List<String> mechanisticSets = new ArrayList<>();
        public List<String> phenotypicSets = new ArrayList<>();
        public List<String> mechanisticSetNames = new ArrayList<>();
        public List<String> phenotypicSetNames = new ArrayList<>();
        public double confidence;
        public Double rnkScore;
        public GeneEvidenceClass evidenceClass;
        public Double mechanisticPolarity;
        public Double phenotypicPolarity;
        public Double enrichmentPolarity;
        public String uniprot;
        public Double mechanisticConfidence;
        public Double phenotypicConfidence;
        public Double sharedDriverScore;
        public Double directionConcordance;
        public Double pValue;
        public Double fdr;
        public Double nes;
        public Double enrichmentScore;
        // View constructs (set nodes)
        public List<String> memberGenes;
        public Integer memberCount;
        // View paint fields (optional)
        public String kind;
        public String cmShape;
        public String cmColor;
        public String cmBorder;
        public Double cmWidth;
        public Double cmHeight;
        public String cmSplitImg;
        public String cmLeft;
        public String cmRight;
        public String edgeKind;
    }

    public static final class ParallelEdgeSnapshot {
        public double weight;
        public double interaction;
        public boolean directed;
        public String effect;
        public String mechanism;
        public int sign;
        public String provider;
        public String evidence;
    }

    public static final class GraphEdgeData {
        public String id;
        public String source;
        public String target;
        public double weight;
        public double interaction;
        public boolean directed = true;
        public String effect;
        public String mechanism;
        public String evidence;
        public String provider;
        public String support;
        public Integer sign;
        public Double polarityCompat;
        public Map<String, Double> channels;
        public List<ParallelEdgeSnapshot> parallels;
        public String edgeKind;
        public String pathPreview;
        public String pathwayId;
        public String pathwayName;
        public String pmid;
        public Boolean direct;
        public Map<String, Double> qualityFactors;
        public List<String> providers;
        // View paint
        public Double cmWidth;
        public Double cmOpacity;
        public String cmColor;
        public String cmArrow;
        public String cmSourceArrow;
        public String cmLineStyle;
    }

    public static final class SetEnrichmentMetric {
        public String setId;
        public Layer layer;
        public String setName;
        public Double nes;
        public double absNes;
        public Double pValue;
        public Double fdr;
        public Double enrichmentScore;
        public String sourcePlatform;
        public String sourceAccession;
        public String collection;
        public String externalUrl;
    }

    public static final class GraphElements {
        public List<NodeElement> nodes = new ArrayList<>();
        public List<EdgeElement> edges = new ArrayList<>();
    }

    public static final class NodeElement {
        public GraphNodeData data;

        public NodeElement() {
        }

        public NodeElement(GraphNodeData data) {
            this.data = data;
        }
    }

    public static final class EdgeElement {
        public GraphEdgeData data;

        public EdgeElement() {
        }

        public EdgeElement(GraphEdgeData data) {
            this.data = data;
        }
    }

    public static final class IntegrationResult {
        public List<SharedDriverSummary> sharedDrivers = new ArrayList<>();
        public List<LayerHubSummary> layerHubs = new ArrayList<>();
        public GraphElements elements = new GraphElements();
        public Map<String, Object> stats = new LinkedHashMap<>();
        public List<Bridge> bridges = new ArrayList<>();
        public List<SetEnrichmentMetric> setEnrichments = new ArrayList<>();
        public String sessionId;
    }

    public static final class InteractomeEdge {
        public String source;
        public String target;
        public double score;
        public boolean directed = true;
        public String effect;
        public String mechanism;
        public String evidence;
        public String provider;
        public String support;
        public Map<String, Double> channels;
        public String pathwayId;
        public String pmid;
        public Boolean direct;
        public List<String> providers;
        public Map<String, Double> qualityFactors;

        public InteractomeEdge() {
        }

        public InteractomeEdge(String source, String target, double score) {
            this.source = source;
            this.target = target;
            this.score = score;
        }
    }

    public static final class EdgeData {
        public double weight;
        public double interaction;
        public boolean directed = true;
        public String effect;
        public String mechanism;
        public String evidence;
        public String provider;
        public String support;
        public Map<String, Double> channels = new LinkedHashMap<>();
        public int sign;
        public List<ParallelEdgeSnapshot> parallels = new ArrayList<>();
        public Double polarityCompat;
        public String pathwayId;
        public String pmid;
        public Boolean direct;
        public List<String> providers;

        public EdgeData copy() {
            EdgeData e = new EdgeData();
            e.weight = weight;
            e.interaction = interaction;
            e.directed = directed;
            e.effect = effect;
            e.mechanism = mechanism;
            e.evidence = evidence;
            e.provider = provider;
            e.support = support;
            e.channels = channels != null ? new LinkedHashMap<>(channels) : new LinkedHashMap<>();
            e.sign = sign;
            e.parallels = parallels != null ? new ArrayList<>(parallels) : new ArrayList<>();
            e.polarityCompat = polarityCompat;
            e.pathwayId = pathwayId;
            e.pmid = pmid;
            e.direct = direct;
            e.providers = providers != null ? new ArrayList<>(providers) : null;
            return e;
        }

        public ParallelEdgeSnapshot toSnapshot() {
            ParallelEdgeSnapshot s = new ParallelEdgeSnapshot();
            s.weight = weight;
            s.interaction = interaction;
            s.directed = directed;
            s.effect = effect;
            s.mechanism = mechanism;
            s.sign = sign;
            s.provider = provider;
            s.evidence = evidence;
            return s;
        }
    }
}
