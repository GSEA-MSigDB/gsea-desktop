/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.vdb;

import java.io.File;
import java.util.WeakHashMap;

import org.apache.commons.lang3.StringUtils;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.vdb.chip.Chip;
import xapps.gsea.GseaWebResources;

/**
 * Vdb related resources that are available at runtime
 * This class is very-gsea desktop application related. You likely do not want to use it if you are packaging
 * gsea in some other mode.
 */
public class VdbRuntimeResources {

    /**
     * @maint if a chip is added that you dont have annotations data for, add to the list here
     */
    private static WeakHashMap<String, Chip> kChipNameChipFileObject = new WeakHashMap<String, Chip>();

    public static Chip getChip(String chipNameOrPath) {

        if (chipNameOrPath == null) {
            throw new IllegalArgumentException("Param chipNameOrPath cannot be null");
        }

        chipNameOrPath = chipNameOrPath.trim();

        // check if a file path provided
        final File tmpf = new File(chipNameOrPath);

        if (tmpf.exists() && tmpf.isFile() && tmpf.getName().endsWith(".chip")) {
            return new Chip(tmpf.getName(), tmpf.getPath());
        }

        Chip chip = kChipNameChipFileObject.get(chipNameOrPath);
        if (chip == null) {
            String chipFile_source = getChipFile_source(chipNameOrPath);
            chip = new Chip(chipNameOrPath, chipFile_source);
            kChipNameChipFileObject.put(chipNameOrPath, chip);
        }
        return chip;
    }

    public static String getChipFile_source(String chipNameOrPath) {
        if (chipNameOrPath == null) {
            throw new IllegalArgumentException("Param chipNameOrPath cannot be null");
        }

        if (NamingConventions.isURL(chipNameOrPath)) {
            // Already a full URL (e.g. selected from the CHIP catalog chooser) -- use it as-is,
            // same as the ftp/gseaftp cases above. Only a bare chip name (handled below) needs a
            // base URL prepended; doing that to an already-complete URL would double it up.
            return chipNameOrPath;
        }
        if (!chipNameOrPath.endsWith(".chip")) {
            chipNameOrPath += ".chip";
        }

        return chipNameOrPath;
    }
}