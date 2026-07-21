/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CoreMapOrganismTest {

    @Test
    public void normalizesHumanAndMouse() {
        assertEquals(CoreMapOrganism.HUMAN, CoreMapOrganism.normalize("9606"));
        assertEquals(CoreMapOrganism.HUMAN, CoreMapOrganism.normalize("Human (HSA / 9606)"));
        assertEquals(CoreMapOrganism.MOUSE, CoreMapOrganism.normalize("10090"));
        assertEquals(CoreMapOrganism.MOUSE, CoreMapOrganism.normalize("Mouse (MMU / 10090)"));
        assertEquals(CoreMapOrganism.MOUSE, CoreMapOrganism.normalize("mus musculus"));
    }

    @Test
    public void remapsReactomeAndKeggIds() {
        assertEquals("R-MMU-69278", CoreMapOrganism.reactomeIdForOrganism("R-HSA-69278", "10090"));
        assertEquals("R-HSA-69278", CoreMapOrganism.reactomeIdForOrganism("R-MMU-69278", "9606"));
        assertEquals("mmu04910", CoreMapOrganism.keggIdForOrganism("hsa04910", "10090"));
        assertEquals("hsa04910", CoreMapOrganism.keggIdForOrganism("mmu04910", "9606"));
    }

    @Test
    public void accessionHelpers() {
        assertTrue(CoreMapOrganism.isReactomeAccession("R-MMU-109581"));
        assertTrue(CoreMapOrganism.isKeggAccession("mmu04110"));
        assertTrue(CoreMapOrganism.supportsHpo("9606"));
        assertFalse(CoreMapOrganism.supportsHpo("10090"));
    }
}
