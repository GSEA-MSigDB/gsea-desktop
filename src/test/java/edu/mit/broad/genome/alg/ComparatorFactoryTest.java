/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.alg;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.mit.broad.genome.objects.MSigDBRelease;
import edu.mit.broad.genome.objects.MSigDBSpecies;

class ComparatorFactoryTest {
    private static MSigDBRelease release(MSigDBSpecies species, String versionId) {
        return new MSigDBRelease(species, "MSigDB " + versionId, versionId, "desc", "2023-01-01",
                "http://example.invalid/genesets.json", "http://example.invalid/chips.json");
    }

    @Test
    void msigDBReleaseByVersionComparator_sortsNewestFirstAndTracksHighestVersion() {
        List<MSigDBRelease> releases = new ArrayList<MSigDBRelease>();
        releases.add(release(MSigDBSpecies.Human, "2022.1.Hs"));
        releases.add(release(MSigDBSpecies.Human, "2023.2.Hs"));
        releases.add(release(MSigDBSpecies.Human, "2023.1.Hs"));

        ComparatorFactory.MSigDBReleaseByVersionComparator comparator =
                new ComparatorFactory.MSigDBReleaseByVersionComparator();
        Collections.sort(releases, comparator);

        assertEquals("2023.2.Hs", releases.get(0).getMSigDBVersion().getVersionString());
        assertEquals("2023.1.Hs", releases.get(1).getMSigDBVersion().getVersionString());
        assertEquals("2022.1.Hs", releases.get(2).getMSigDBVersion().getVersionString());
        assertEquals("2023.2.Hs", comparator.getHighestVersionId());
    }

    @Test
    void msigDBReleaseByVersionComparator_sameVersionFallsBackToReleaseNameOrder() {
        List<MSigDBRelease> releases = new ArrayList<MSigDBRelease>();
        releases.add(release(MSigDBSpecies.Mouse, "2023.2.Mm"));
        // Same versionId, different releaseName -- exercises the stable tie-break.
        MSigDBRelease duplicateVersion = new MSigDBRelease(MSigDBSpecies.Mouse, "AAA duplicate", "2023.2.Mm",
                "desc", "2023-01-01", "http://example.invalid/g2", "http://example.invalid/c2");
        releases.add(duplicateVersion);

        ComparatorFactory.MSigDBReleaseByVersionComparator comparator =
                new ComparatorFactory.MSigDBReleaseByVersionComparator();
        Collections.sort(releases, comparator);

        assertEquals("AAA duplicate", releases.get(0).getReleaseName());
        assertEquals("MSigDB 2023.2.Mm", releases.get(1).getReleaseName());
    }
}
