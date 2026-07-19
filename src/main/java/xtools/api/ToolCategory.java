/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xtools.api;

/**
 * Categories of Tools (Swing ToolCategory names/descriptions/icon resources).
 */
public class ToolCategory {

    public static final ToolCategory DATASET =
            new ToolCategory("Dataset Pre-processing",
                    "Dataset processing - creation, extraction & manipulation related tools",
                    "Res16.gif");
    public static final ToolCategory CLS =
            new ToolCategory("Template",
                    "Cls file info, creation and manipulation related tools",
                    "Cls.gif");
    public static final ToolCategory CLUSTER =
            new ToolCategory("Cluster",
                    "Cls file info, creation and manipulation related tools",
                    "Cls.gif");
    public static final ToolCategory DATA_MUNGING = new ToolCategory("Data Munging",
            "Collection of tools to format data",
            "DataFormatTools16.gif");
    public static final ToolCategory GSEA =
            new ToolCategory("Gene Set Enrichment Analysis",
                    "Gene Set Enrichment Analysis Tools - the Gsea statistic and related procedures",
                    "Gsea.gif");
    public static final ToolCategory GENESET_TOOLS = new ToolCategory("Gene Set Tools",
            "Collection of gene set tools - data analysis, creation etc",
            "Grp.gif");
    public static final ToolCategory MARKER_SELECTION =
            new ToolCategory("Marker Selection",
                    "Select markers by phenotype or gene profile and compute significance levels",
                    "Marker16.png");
    public static final ToolCategory MISC = new ToolCategory("Misc Tools",
            "Collection of miscellaneous tools",
            "MiscTools.gif");
    public static final ToolCategory FILE_MUNGING = new ToolCategory("File Munging",
            "Tools that manipilate file &dir names, extensions etc",
            "FileTools.png");
    public static final ToolCategory MSIGDB =
            new ToolCategory("MSigDb", "Molecular signature database", "Compare16.png");
    public static final ToolCategory MAPPING_TOOLS = new ToolCategory("Mapping: Chip <=> Chip",
            "Chip to Chip mappings (within and between species)",
            "Map.gif");

    private final String fName;
    private final String fDesc;
    private final String fIconResource;

    private ToolCategory(String name, String desc) {
        this(name, desc, null);
    }

    private ToolCategory(String name, String desc, String iconResource) {
        this.fName = name;
        this.fDesc = desc;
        this.fIconResource = iconResource;
    }

    public String getName() {
        return fName;
    }

    public String getDesc() {
        return fDesc;
    }

    /** Swing {@code ToolCategory#getIcon} resource name for FX rendering. */
    public String getIconResource() {
        return fIconResource;
    }
}
