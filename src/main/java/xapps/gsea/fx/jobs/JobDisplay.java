/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.jobs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.function.Function;

import edu.mit.broad.genome.parsers.AuxUtils;
import xtools.api.param.Param;

/**
 * Formats Jobs-list title and hover text from a tool name + param snapshot.
 * Hover lines are driven by whichever primary inputs are present for that run
 * (GSEA, preranked, ssGSEA, CollapseDataset, Chip2Chip, Leading Edge, …).
 */
public final class JobDisplay {

    /** Ordered primary inputs; only keys present in the snapshot are shown. */
    private static final List<HoverField> HOVER_FIELDS = List.of(
            field(Param.RES, "Expression dataset", JobDisplay::shortPaths),
            field(Param.RNK, "Ranked list", JobDisplay::shortPaths),
            field(Param.CLS, null, JobDisplay::phenotypeLine),
            field(Param.GMX, "Gene set database", JobDisplay::shortPaths),
            field(Param.CHIP, "Chip platform", JobDisplay::shortPaths),
            field("chip_target", "Target chip", JobDisplay::shortPaths),
            field(Param.DIR, "GSEA result folder", JobDisplay::shortPaths),
            field("gsets", "Gene sets", JobDisplay::geneSetsLine),
            field("mode", "Collapsing mode", JobDisplay::identity));

    private JobDisplay() {
    }

    /** List title: {@code Tool — analysisName}, or just the tool name. */
    public static String listTitle(String toolName, Properties params) {
        String tool = blankToNull(toolName) != null ? toolName.trim() : "Tool";
        String analysis = prop(params, Param.RPT);
        if (analysis != null) {
            return tool + " — " + analysis;
        }
        return tool;
    }

    /**
     * Hover text for primary run inputs present in the snapshot.
     * Missing keys are skipped so each job type shows only relevant details.
     */
    public static String hoverText(Properties params) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        List<String> lines = new ArrayList<>(HOVER_FIELDS.size());
        for (HoverField field : HOVER_FIELDS) {
            String raw = prop(params, field.key);
            if (raw == null) {
                continue;
            }
            String formatted = field.formatter.apply(raw);
            if (formatted == null) {
                continue;
            }
            if (field.label != null) {
                lines.add(field.label + ": " + formatted);
            } else {
                lines.add(formatted); // formatter already includes the label
            }
        }
        return lines.isEmpty() ? null : String.join("\n", lines);
    }

    private static HoverField field(String key, String label, Function<String, String> formatter) {
        return new HoverField(key, label, formatter);
    }

    private static String phenotypeLine(String cls) {
        if (AuxUtils.isAux(cls)) {
            return "Phenotype comparison: " + AuxUtils.getAuxNameOnlyNoHash(cls);
        }
        String path = shortPath(cls);
        return path != null ? "Phenotype labels: " + path : null;
    }

    private static String geneSetsLine(String value) {
        String raw = blankToNull(value);
        if (raw == null) {
            return null;
        }
        int count = 1;
        for (int i = 0; i < raw.length(); i++) {
            if (raw.charAt(i) == ',') {
                count++;
            }
        }
        if (count > 4 || raw.length() > 80) {
            return count + " selected";
        }
        return raw;
    }

    private static String identity(String value) {
        return blankToNull(value);
    }

    private static String shortPaths(String value) {
        if (value == null) {
            return null;
        }
        if (!value.contains(",")) {
            return shortPath(value);
        }
        StringTokenizer tok = new StringTokenizer(value, ",");
        List<String> parts = new ArrayList<>();
        while (tok.hasMoreTokens()) {
            String part = shortPath(tok.nextToken().trim());
            if (part != null) {
                parts.add(part);
            }
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private static String shortPath(String path) {
        String raw = blankToNull(path);
        if (raw == null) {
            return null;
        }
        String base = AuxUtils.isAux(raw) ? AuxUtils.getBaseNameOnly(raw) : raw;
        try {
            File f = new File(base);
            String name = f.getName();
            return blankToNull(name) != null ? name : base;
        } catch (Exception e) {
            return base;
        }
    }

    private static String prop(Properties params, String key) {
        if (params == null || key == null) {
            return null;
        }
        return blankToNull(params.getProperty(key));
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static final class HoverField {
        private final String key;
        private final String label;
        private final Function<String, String> formatter;

        private HoverField(String key, String label, Function<String, String> formatter) {
            this.key = key;
            this.label = label;
            this.formatter = formatter;
        }
    }
}
