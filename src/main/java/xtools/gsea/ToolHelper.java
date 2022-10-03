/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.gsea;

import java.awt.HeadlessException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;

import edu.mit.broad.genome.NotImplementedException;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.MSigDBVersion;
import edu.mit.broad.genome.objects.Versioned;
import edu.mit.broad.genome.reports.api.ToolReport;
import edu.mit.broad.vdb.chip.Chip;
import edu.mit.broad.xbench.core.api.Application;
import xtools.api.CanceledException;

public class ToolHelper {
    public static boolean checkNonMixedVersions(Chip chip, GeneSet[] geneSets)
            throws Exception {
        // This should never happen; will already be checked earlier
        if (geneSets == null || geneSets.length == 0) { throw new IllegalArgumentException("One or more gene sets must be specified"); }

        MSigDBVersion first = geneSets[0].getMSigDBVersion();

        // If *all* versions found are unknown then we give no warning.
        // We also give a warning if any of the recognized versions don't match.
        boolean allUnknown = first.isUnknownVersion();
        boolean allKnown = !first.isUnknownVersion();
        if (geneSets.length > 1) {
            for (int i = 1; i < geneSets.length; i++) {
                MSigDBVersion currVer = geneSets[i].getMSigDBVersion();
                if (!currVer.isUnknownVersion()) {
                    if (!first.equals(currVer)) { return false; }
                    allUnknown = false;
                    allKnown &= true;
                } else {
                    allUnknown &= true;
                    allKnown = false;
                }
            }
        }
        if (chip != null) {
            MSigDBVersion chipVersion = chip.getMSigDBVersion();
            if (!chipVersion.isUnknownVersion()) {
                if (!first.equals(chipVersion)) { return false; }
                allUnknown = false;
                allKnown &= true;
            } else {
                allUnknown &= true;
                allKnown = false;
            }
        }

        // The check passes at this point if either all are unknown or all are known
        return allUnknown || allKnown;
    }
    
    public static boolean checkNonMixedSpecies(Chip chip, GeneSet[] geneSets) throws Exception {
        // This should never happen; will already be checked earlier
        if (geneSets == null || geneSets.length == 0) { throw new IllegalArgumentException("One or more gene sets must be specified"); }

        // Force the Chip to fully load if it is not already
        if (chip != null) { chip.getNumProbes(); }
        
        // Remove all the Unknown Version sets as they will not be part of the Species comparison.
        List<GeneSet> geneSetsList = new ArrayList<GeneSet>(Arrays.asList(geneSets));
        geneSetsList.removeIf(new Predicate<Versioned>() {
            public boolean test(Versioned item) { return item.getMSigDBVersion().isUnknownVersion(); }
        });
        // If all the geneSets had an Unknown version, then we return as valid since they have no
        // bearing on validity.
        if (geneSetsList.isEmpty()) { return true; }
        
        MSigDBVersion first = geneSetsList.remove(0).getMSigDBVersion();
        for (Versioned item : geneSetsList) {
            if (first.getMsigDBSpecies() != item.getMSigDBVersion().getMsigDBSpecies()) { return false; }
        }

        // At this point, all the Gene Sets Species are known and match, so we just check against the Chip
        // or return as valid if there is no Chip or one with an unknown version.
        if (chip == null) { return true; }
        MSigDBVersion chipVersion = chip.getMSigDBVersion();
        return chipVersion.isUnknownVersion() || first.getMsigDBSpecies() == chipVersion.getMsigDBSpecies();
    }

    public static void validateMixedVersionAndSpecies(final GeneSet[] origGeneSets, Chip chip, ToolReport toolReport, Logger log)
            throws Exception {
        if (!checkNonMixedSpecies(chip, origGeneSets)) {
            String msg = (chip != null) ?
                    "Selected CHIP is not compatible with the selected gene set(s) species" :
                    "GSEA doesn't support simultaneous selection of human and mouse MSigDB collections";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        } else if (!checkNonMixedVersions(chip, origGeneSets)) {
            if (chip == null) {
                // No GUI warning here, since a GUI user will have already been prompted by the Gene Set chooser
                String msgShort = "Mixed MSigDB versions detected";
                String msgFull = "Selecting collections from multiple MSigDB versions may result in omitted genes and is not recommended.";
                log.warn(msgShort);
                log.warn(msgFull);
                toolReport.addWarning(msgShort + ". " + msgFull);
            } else {
                String msgShort = "Mixed MSigDB versions detected";
                String msgPt1 = "The selected CHIP does not match the version of the MSigDB";
                String msgPt2 = "collection selected. Some gene identifiers may not be mapped.";
                
                try {
                    boolean confirm = Application.getWindowManager().showConfirm(msgShort, String.join("\n", msgPt1, msgPt2));
                    if (!confirm) { throw new CanceledException(); }
                } catch (NotImplementedException | HeadlessException ex) {
                    // Swallow this; we're not running in the GUI, so we just add warnings to the log.
                }
                log.warn(msgShort);
                String msgFull = msgPt1 + " " + msgPt2;
                log.warn(msgFull);
                toolReport.addWarning(msgShort + ". " + msgFull);
            }
        }
    }
}
