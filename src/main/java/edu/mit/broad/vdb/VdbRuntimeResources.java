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

        //System.out.println(">>>>" + chipName + "<");

        // Ok, its from a file path or ftp location
        //String chipName = _nameOrPath2Name(chipNameOrPath);

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

        if (chipNameOrPath.startsWith("ftp")) {

            // common error is ftp.broad... while it should be ftp://ftp.broad...
            if (chipNameOrPath.startsWith("ftp.")) {
                chipNameOrPath = "ftp://" + chipNameOrPath;
            }

            return chipNameOrPath;
        }

        if (chipNameOrPath.startsWith("gseaftp")) {

            // common error is gseaftp.broad... while it should be ftp://gseaftp.broad...
            if (chipNameOrPath.startsWith("gseaftp.")) {
                chipNameOrPath = "ftp://" + chipNameOrPath;
            }

            return chipNameOrPath;
        }

        if (NamingConventions.isURL(chipNameOrPath)) {
            // Already a full URL (e.g. selected from the CHIP catalog chooser) -- use it as-is,
            // same as the ftp/gseaftp cases above. Only a bare chip name (handled below) needs a
            // base URL prepended; doing that to an already-complete URL would double it up.
            return chipNameOrPath;
        }

        chipNameOrPath = chipNameOrPath.replace('-', '_');

        // No scheme or path given -- resolve against the HTTPS annotations mirror instead of the
        // old FTP site. Chip file names conventionally encode their species (e.g.
        // "Human_..."/"Mouse_..."), so use that to pick the right one; default to Human otherwise,
        // matching the same species-guessing convention already used for gene set files in
        // GmtParser.
        String base = StringUtils.containsIgnoreCase(chipNameOrPath, "mouse")
                ? GseaWebResources.getMouseArrayAnnotationsURL() : GseaWebResources.getHumanArrayAnnotationsURL();
        String path = base + chipNameOrPath;
        if (!path.endsWith(".chip")) {
            path += ".chip";
        }

        return path;
    }
}