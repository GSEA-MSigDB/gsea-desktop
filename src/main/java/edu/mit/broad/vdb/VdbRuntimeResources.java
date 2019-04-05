/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.vdb;

import java.io.File;
import java.util.WeakHashMap;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.vdb.chip.Chip;

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

    private static String FTP_ANNOTATION_BASE = "ftp://gseaftp.broadinstitute.org/pub/gsea/annotations/";

    private static String fChipLocBase = FTP_ANNOTATION_BASE; // default us the Broad FTP site

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

        chipNameOrPath = chipNameOrPath.replace('-', '_');

        String path = fChipLocBase + chipNameOrPath;
        if (!path.endsWith(".chip")) {
            path += ".chip";
        }

        return path;
    }
}