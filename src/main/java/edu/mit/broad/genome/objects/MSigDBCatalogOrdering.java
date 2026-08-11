/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordering helpers for MSigDB catalog file lists shown in choosers.
 */
public final class MSigDBCatalogOrdering {
    private MSigDBCatalogOrdering() {
    }

    /**
     * Moves the Hallmark collection ({@code h.all} for Human, {@code mh.all} for Mouse) to the
     * front of the list; all other files keep catalog order. CHIP file names never match, so CHIP
     * choosers are unaffected.
     */
    public static List<MSigDBCatalogFile> withHallmarkFirst(List<MSigDBCatalogFile> files) {
        List<MSigDBCatalogFile> hallmarkFirst = new ArrayList<>(files.size());
        List<MSigDBCatalogFile> rest = new ArrayList<>(files.size());
        for (MSigDBCatalogFile file : files) {
            (isHallmark(file) ? hallmarkFirst : rest).add(file);
        }
        hallmarkFirst.addAll(rest);
        return hallmarkFirst;
    }

    private static boolean isHallmark(MSigDBCatalogFile file) {
        String name = file.getName().toLowerCase();
        switch (file.getMSigDBVersion().getMsigDBSpecies()) {
            case Human:
                return name.startsWith("h.all.");
            case Mouse:
                return name.startsWith("mh.all.");
            default:
                return false;
        }
    }
}
