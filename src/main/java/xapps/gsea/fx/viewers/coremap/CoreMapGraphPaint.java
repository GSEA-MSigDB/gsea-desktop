/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.coremap;

import edu.mit.broad.coremap.CoreMapConstants;
import edu.mit.broad.coremap.CoreMapTypes.EdgeElement;
import edu.mit.broad.coremap.CoreMapTypes.GraphEdgeData;
import edu.mit.broad.coremap.CoreMapTypes.GraphNodeData;
import edu.mit.broad.coremap.CoreMapTypes.NodeElement;

/**
 * Stamps Cytoscape paint fields on view elements. Element selection stays in
 * {@link CoreMapGraphViews}.
 */
final class CoreMapGraphPaint {
    private CoreMapGraphPaint() {}

    static void apply(CoreMapGraphViews.ViewElements out) {
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
        if (CoreMapGraphViews.isComembership(e)) {
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
}
