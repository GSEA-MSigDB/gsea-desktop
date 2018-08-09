/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb;

import edu.mit.broad.genome.*;
import edu.mit.broad.genome.parsers.*;
import edu.mit.broad.vdb.chip.*;
import edu.mit.broad.vdb.meg.*;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;

/**
 * Vdb related resources that are available at runtime
 * This class is very-gsea desktop application related. You likely do not want to use it if you are packaging
 * gsea in some other mode.
 */
public class VdbRuntimeResources {

    static Set affy_prefixes_hack = new HashSet();

    static {
        affy_prefixes_hack.add("HG");
        affy_prefixes_hack.add("HC");
        affy_prefixes_hack.add("HU");
        affy_prefixes_hack.add("Hu");
        affy_prefixes_hack.add("MG");
        affy_prefixes_hack.add("MOE");
        affy_prefixes_hack.add("Mu");
        affy_prefixes_hack.add("Mouse");
    }

    private static AliasDb kAliasDb;

    public static AliasDb getAliasDb() throws Exception {

        if (kAliasDb == null) {
            kAliasDb = new AliasDbImpl(null, getChip_Gene_Symbol());
        }

        return kAliasDb;
    }

    /**
     * @maint if a chip is added that you dont have annotations data for, add to the list here
     */
    private static WeakHashMap kChipNameChipFileObject = new WeakHashMap();

    public static Chip getChip_Gene_Symbol() {
        return getChip(Constants.GENE_SYMBOL);
    }

    public static boolean isChipGeneSymbol(String chipName) {
        chipName = _nameOrPath2Name(chipName).toUpperCase();
        return chipName.startsWith(Constants.GENE_SYMBOL);
    }

    public static boolean isPathGeneSymbolChip(String filePath) {
        filePath = _nameOrPath2Name(filePath);
        return StringUtils.equalsIgnoreCase(Constants.GENE_SYMBOL, 
                FilenameUtils.getBaseName(filePath)) &&
                StringUtils.equalsIgnoreCase(Constants.CHIP, 
                        FilenameUtils.getExtension(filePath));
    }
    
    public static boolean isChipSeqAccession(String chipName) {
        chipName = _nameOrPath2Name(chipName).toUpperCase();
        return chipName.startsWith(Constants.SEQ_ACCESSION);
    }

    public static boolean isChipAffy_hacky(String name) {
        name = _nameOrPath2Name(name);
        name = name.toUpperCase();
        for (Iterator iterator = affy_prefixes_hack.iterator(); iterator.hasNext();) {
            String key = iterator.next().toString().toUpperCase();
            if (name.startsWith(key)) {
                return true;
            }
        }

        return false;
    }

    private static Chip kAffy_combo_chip;

    public static Chip getChip_Affy() {
        try {
            if (kAffy_combo_chip == null) {
                kAffy_combo_chip = ChipHelper.createComboChip(getChips_Affy());
                kAffy_combo_chip.cloneShallow(Constants.AFFYMETRIX);
            }

            return kAffy_combo_chip;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static Chip[] getChips_Affy() {

        try {
            final String[] affy_names = ParseUtils.slurpIntoArray(JarResources.toURL("AffyChipNames.txt"), true);
            return getChips(affy_names);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static Chip[] getChips(final String[] namesOrPaths) throws Exception {
        final Chip[] chips = new Chip[namesOrPaths.length];
        for (int i = 0; i < namesOrPaths.length; i++) {
            chips[i] = getChip(namesOrPaths[i]);
        }

        return chips;
    }

    public static Chip getChip(String chipNameOrPath) {

        if (chipNameOrPath == null) {
            throw new IllegalArgumentException("Param chipNameOrPath cannot be null");
        }

        chipNameOrPath = chipNameOrPath.trim();

        // check if a file path provided
        final File tmpf = new File(chipNameOrPath);

        if (tmpf.exists() && tmpf.isFile() && tmpf.getName().endsWith(".chip")) {
            return new FileInMemoryChip(tmpf.getName(), tmpf.getPath());
        }

        if (chipNameOrPath.toUpperCase().startsWith("AFFY")) {
            return getChip_Affy();
        }

        //System.out.println(">>>>" + chipName + "<");

        // Ok, its from a file path or ftp location
        //String chipName = _nameOrPath2Name(chipNameOrPath);

        Object obj = kChipNameChipFileObject.get(chipNameOrPath);
        if (obj == null) {
            String chipFile_source = getChipFile_source(chipNameOrPath);
            obj = new FileInMemoryChip(chipNameOrPath, chipFile_source);
            kChipNameChipFileObject.put(chipNameOrPath, obj);
        }
        return (Chip) obj;
    }

    private static String _nameOrPath2Name(final String nop) {
        // TODO: Clean up dependencies on hard-coded FTP server paths
        // This code appears highly questionable.
        
        if (nop.startsWith("ftp.")) {
            return nop.substring("gseaftp.broadinstitute.org://pub/gsea/annotations/".length()); // @note no ftp: prefix
        } else if (nop.startsWith("ftp:")) {
            return nop.substring(FTP_ANNOTATION_BASE.length()); // @note no ftp: prefix
        }

        if (nop.startsWith("gseaftp.")) {
            return nop.substring("gseaftp.broadinstitute.org://pub/gsea/annotations/".length()); // @note no ftp: prefix
        } else if (nop.startsWith("ftp:")) {
            return nop.substring(FTP_ANNOTATION_BASE.length()); // @note no ftp: prefix
        }

        return new File(nop).getName();
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

        if (chipNameOrPath.equalsIgnoreCase("GeneSymbol")) {
            chipNameOrPath = Constants.GENE_SYMBOL;
        }

        if (chipNameOrPath.equalsIgnoreCase("SeqAccession")) {
            chipNameOrPath = Constants.SEQ_ACCESSION;
        }

        String path = fChipLocBase + chipNameOrPath;
        if (!path.endsWith(".chip")) {
            path += ".chip";
        }

        return path;
    }

} // End VdbRuntimeResources
