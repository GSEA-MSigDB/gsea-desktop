/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.gsea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.MSigDBVersion;
import edu.mit.broad.genome.objects.Versioned;
import edu.mit.broad.vdb.chip.Chip;

public class ToolHelper {
    public static boolean checkNonMixedVersions(Chip chip, GeneSet[] geneSets)
            throws Exception {
        // This should never happen; will already be checked earlier
        if (geneSets == null || geneSets.length == 0) { throw new IllegalArgumentException("One or more gene sets must be specified"); }

        MSigDBVersion first = geneSets[0].getMSigDBVersion();
        boolean allUnknown = first.isUnknownVersion();
        if (geneSets.length > 1) {
            for (Versioned item : geneSets) {
                MSigDBVersion currVer = item.getMSigDBVersion();
                allUnknown |= currVer.isUnknownVersion();
                if (!allUnknown && !first.equals(currVer)) { return false; }
            }
        }
        if (chip == null) { return true; }

        MSigDBVersion chipVersion = chip.getMSigDBVersion();
        if (chipVersion.isUnknownVersion()) { return allUnknown; }
        else { return first.equals(chipVersion); }
    }
    
    public static boolean checkNonMixedSpecies(Chip chip, GeneSet[] geneSets)
            throws Exception {
        // This should never happen; will already be checked earlier
        if (geneSets == null || geneSets.length == 0) { throw new IllegalArgumentException("One or more gene sets must be specified"); }

        // Remove all the Unknown Version sets as they will not be part of the Species comparison.
        List<GeneSet> geneSetsList = new ArrayList<GeneSet>(Arrays.asList(geneSets));
        geneSetsList.removeIf(new Predicate<Versioned>() {
            public boolean test(Versioned item) { return item.getMSigDBVersion().isUnknownVersion(); }
        });
        if (geneSetsList.isEmpty()) {  return chip == null || chip.getMSigDBVersion().isUnknownVersion(); }
        
        MSigDBVersion first = geneSetsList.remove(0).getMSigDBVersion();
        for (Versioned item : geneSetsList) {
            if (first.getMsigDBSpecies() != item.getMSigDBVersion().getMsigDBSpecies()) { return false; }
        }
        if (chip == null) { return true; }

        MSigDBVersion chipVersion = chip.getMSigDBVersion();
        return !chipVersion.isUnknownVersion() && first.getMsigDBSpecies() == chipVersion.getMsigDBSpecies();
    }
}
