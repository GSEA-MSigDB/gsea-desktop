/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.coremap;

import javafx.scene.control.Label;

/** Shared insight / selection chrome styles (theme via gsea-fx.css / dark). */
public final class CoreMapInsightStyles {

    private CoreMapInsightStyles() {
    }

    public static Label chip(String text, boolean associative) {
        Label l = new Label(text);
        l.getStyleClass().add(associative ? "coremap-chip-assoc" : "coremap-chip");
        return l;
    }

    public static Label meta(String text) {
        Label l = new Label(text);
        l.getStyleClass().addAll("gsea-muted", "coremap-meta");
        l.setWrapText(true);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    public static Label help(String text) {
        Label l = new Label(text);
        l.getStyleClass().addAll("gsea-muted", "coremap-help");
        l.setWrapText(true);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    public static String layerLabel(String wire) {
        if ("mechanistic".equals(wire)) {
            return "Mechanistic";
        }
        if ("phenotypic".equals(wire)) {
            return "Phenotypic";
        }
        return wire != null ? wire : "";
    }

    public static String providerLabel(String wire) {
        if (wire == null || wire.isBlank()) {
            return "";
        }
        return switch (wire.toLowerCase()) {
            case "signor" -> "SIGNOR";
            case "string" -> "STRING";
            case "fused" -> "Fused";
            case "reactome" -> "Reactome";
            case "kegg" -> "KEGG";
            case "go" -> "GO";
            case "hpo" -> "HPO";
            case "geneset" -> "Gene-set";
            case "demo", "local" -> "Demo";
            default -> wire;
        };
    }

    public static String evidenceLabel(String wire) {
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

    public static String polarityLabel(Double pol) {
        if (pol == null || pol == 0) {
            return "unsigned";
        }
        return pol > 0 ? "up" : "down";
    }
}
