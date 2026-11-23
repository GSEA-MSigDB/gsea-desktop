/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.mit.broad.genome.objects.MSigDBCatalogFile;
import edu.mit.broad.genome.objects.MSigDBSpecies;
import edu.mit.broad.genome.objects.MSigDBVersion;

class ChooserHelperTest {
    private static MSigDBCatalogFile file(String name, MSigDBSpecies species) {
        return new MSigDBCatalogFile(name, "desc", "http://example.invalid/" + name,
                new MSigDBVersion(species, "2026.1"));
    }

    @Test
    void withHallmarkFirst_movesHumanHallmarkToFront() {
        List<MSigDBCatalogFile> files = Arrays.asList(
                file("c1.all.v2026.1.Hs.symbols.gmt", MSigDBSpecies.Human),
                file("c2.all.v2026.1.Hs.symbols.gmt", MSigDBSpecies.Human),
                file("h.all.v2026.1.Hs.symbols.gmt", MSigDBSpecies.Human));

        List<MSigDBCatalogFile> sorted = ChooserHelper.withHallmarkFirst(files);

        assertEquals("h.all.v2026.1.Hs.symbols.gmt", sorted.get(0).getName());
        assertEquals("c1.all.v2026.1.Hs.symbols.gmt", sorted.get(1).getName());
        assertEquals("c2.all.v2026.1.Hs.symbols.gmt", sorted.get(2).getName());
    }

    @Test
    void withHallmarkFirst_movesMouseHallmarkToFront() {
        List<MSigDBCatalogFile> files = Arrays.asList(
                file("c2.all.v2026.1.Mm.symbols.gmt", MSigDBSpecies.Mouse),
                file("mh.all.v2026.1.Mm.symbols.gmt", MSigDBSpecies.Mouse));

        List<MSigDBCatalogFile> sorted = ChooserHelper.withHallmarkFirst(files);

        assertEquals("mh.all.v2026.1.Mm.symbols.gmt", sorted.get(0).getName());
        assertEquals("c2.all.v2026.1.Mm.symbols.gmt", sorted.get(1).getName());
    }

    @Test
    void withHallmarkFirst_leavesChipFileNamesInOriginalOrder() {
        List<MSigDBCatalogFile> files = Arrays.asList(
                file("Human_Gene_Symbol_with_Remapping_MSigDB.v2026.1.Hs.chip", MSigDBSpecies.Human),
                file("Human_ENSEMBL_Gene_ID_MSigDB.v2026.1.Hs.chip", MSigDBSpecies.Human));

        List<MSigDBCatalogFile> sorted = ChooserHelper.withHallmarkFirst(files);

        assertEquals(files, sorted);
    }
}
