/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.coremap;

import java.util.Locale;

import edu.mit.broad.coremap.CoreMapTypes.Bridge;
import edu.mit.broad.coremap.CoreMapTypes.LayerHubSummary;
import edu.mit.broad.coremap.CoreMapTypes.SharedDriverSummary;

/**
 * Shared text assembly for CoreMap selection overlay and insight sidebar cards.
 * Layout stays in the caller's UI; this only formats titles / meta / set lines.
 */
public final class CoreMapInsightSummaries {

    private CoreMapInsightSummaries() {
    }

    public static String bridgeTitle(Bridge bridge) {
        return CoreMapCascadeUi.mechLabel(bridge) + " → " + CoreMapCascadeUi.phenoLabel(bridge);
    }

    /**
     * @param scorePrefix when true, prefix with {@code "Bridge score: "}; sidebar omits it
     */
    public static String bridgeMeta(Bridge bridge, boolean scorePrefix) {
        int pathCount = bridge.pathCount != null ? bridge.pathCount : 1;
        StringBuilder meta = new StringBuilder();
        if (scorePrefix) {
            meta.append(String.format(Locale.ROOT, "Bridge score: %.3f", bridge.bridgeScore));
        } else {
            meta.append(String.format(Locale.ROOT, "%.3f", bridge.bridgeScore));
        }
        if (pathCount > 1) {
            meta.append(" · ").append(pathCount).append(" cascades");
        }
        meta.append(" · ").append(CoreMapCascadeUi.alignmentLabel(bridge.directionAlignment));
        String emp = CoreMapCascadeUi.empiricalPLabel(bridge);
        if (emp != null) {
            meta.append(" · ").append(emp);
        }
        return meta.toString();
    }

    public static String relatedSetsLine(Bridge bridge, boolean fullLabel) {
        String related = CoreMapCascadeUi.relatedSetsText(bridge);
        if (related == null) {
            return null;
        }
        return (fullLabel ? "Related sets: " : "Related: ") + related;
    }

    public static String driverScoreLine(SharedDriverSummary d) {
        return String.format(Locale.ROOT, "score %.3f · concordance %.2f",
                d.sharedDriverScore, d.directionConcordance);
    }

    public static String driverScoreFull(SharedDriverSummary d) {
        return String.format(Locale.ROOT, "Shared-driver score: %.3f", d.sharedDriverScore);
    }

    public static String driverConcordanceFull(SharedDriverSummary d) {
        return String.format(Locale.ROOT, "Concordance: %.2f", d.directionConcordance);
    }

    public static String driverMechSets(SharedDriverSummary d, boolean compact) {
        if (d.mechanisticSets == null || d.mechanisticSets.isEmpty()) {
            return null;
        }
        String joined = String.join(", ", d.mechanisticSets);
        return compact ? "Mech: " + joined : "Mechanistic sets: " + joined;
    }

    public static String driverPhenoSets(SharedDriverSummary d, boolean compact) {
        if (d.phenotypicSets == null || d.phenotypicSets.isEmpty()) {
            return null;
        }
        String joined = String.join(", ", d.phenotypicSets);
        return compact ? "Pheno: " + joined : "Phenotypic sets: " + joined;
    }

    public static String hubMeta(LayerHubSummary h, boolean includeLayerPrefix) {
        String layer = h.layer != null ? CoreMapInsightStyles.layerLabel(h.layer.wire()) : "";
        if (includeLayerPrefix) {
            return String.format(Locale.ROOT, "Layer: %s · hub %.3f · degree %d",
                    layer, h.hubScore, h.degree);
        }
        return String.format(Locale.ROOT, "%s · hub %.3f · degree %d", layer, h.hubScore, h.degree);
    }

    public static String hubSets(LayerHubSummary h, boolean includeSetsPrefix) {
        if (h.sets == null || h.sets.isEmpty()) {
            return null;
        }
        String joined = String.join(", ", h.sets);
        return includeSetsPrefix ? "Sets: " + joined : joined;
    }
}
