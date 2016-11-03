/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api;

import edu.mit.broad.genome.JarResources;

import javax.swing.*;

/**
 * Class that defines the categories of Tools.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ToolCategory {

    /**
     * As a convention, DONT add the thing "Tools" to name
     */

    public static final ToolCategory DATASET =
            new ToolCategory("Dataset Pre-processing", "Dataset processing - creation, extraction & manipulation related tools",
                    JarResources.getIcon("Res16.gif"));
    public static final ToolCategory CLS =
            new ToolCategory("Template", "Cls file info, creation and manipulation related tools",
                    JarResources.getIcon("Cls.gif"));

    public static final ToolCategory CLUSTER =
            new ToolCategory("Cluster", "Cls file info, creation and manipulation related tools",
                    JarResources.getIcon("Cls.gif"));


    public static final ToolCategory DATA_MUNGING = new ToolCategory("Data Munging",
            "Collection of tools to format data",
            JarResources.getIcon("DataFormatTools16.gif"));

    // -------------------- GSEA RELATED TOOLS -------------------------

    public static final ToolCategory GSEA =
            new ToolCategory("Gene Set Enrichment Analysis", "Gene Set Enrichment Analysis Tools - the Gsea statistic and related procedures",
                    JarResources.getIcon("Gsea.gif"));

    public static final ToolCategory GENESET_TOOLS = new ToolCategory("Gene Set Tools",
            "Collection of gene set tools - data analysis, creation etc",
            JarResources.getIcon("Grp.gif"));

    public static final ToolCategory MARKER_SELECTION =
            new ToolCategory("Marker Selection", "Select markers by phenotype or gene profile and compute significance levels",
                    JarResources.getIcon("Marker16.png"));

    public static final ToolCategory MISC = new ToolCategory("Misc Tools",
            "Collection of miscellaneous tools",
            JarResources.getIcon("MiscTools.gif"));

    public static final ToolCategory FILE_MUNGING = new ToolCategory("File Munging",
            "Tools that manipilate file &dir names, extensions etc",
            JarResources.getIcon("FileTools.png"));


    public static final ToolCategory MSIGDB =
            new ToolCategory("MSigDb", "Molecular signature database",
                    JarResources.getIcon("Compare16.png"));

    public static final ToolCategory MAPPING_TOOLS = new ToolCategory("Mapping: Chip <=> Chip",
            "Chip to Chip mappings (within and between species)",
            JarResources.getIcon("Map.gif"));


    private final String fName;
    private final String fDesc;
    private final Icon fIcon;
    private boolean fExposePublically;

    /**
     * Class constructor
     */
    private ToolCategory(String name, String desc, Icon icon) {
        this(name, desc, icon, true);
    }

    private ToolCategory(String name, String desc, Icon icon, boolean exposePublically) {
        this.fName = name;
        this.fDesc = desc;
        this.fIcon = icon;
        this.fExposePublically = exposePublically;
    }

    public String getName() {
        return fName;
    }

    public String getDesc() {
        return fDesc;
    }

    public Icon getIcon() {
        return fIcon;
    }

}    // End ToolCategory
