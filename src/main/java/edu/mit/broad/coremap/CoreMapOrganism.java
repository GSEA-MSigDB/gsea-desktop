/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared Human (HSA / 9606) and Mouse (MMU / 10090) organism helpers for interactome
 * and source-enrichment providers.
 */
public final class CoreMapOrganism {

    public static final String HUMAN = "9606";
    public static final String MOUSE = "10090";
    public static final String RAT = "10116";

    private static final Pattern REACTOME_ID = Pattern.compile(
            "R-(HSA|MMU|RNO)-(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern KEGG_ID = Pattern.compile(
            "(hsa|mmu|rno)(\\d+)", Pattern.CASE_INSENSITIVE);

    private CoreMapOrganism() {
    }

    public static String normalize(String organism) {
        if (organism == null || organism.isBlank()) {
            return HUMAN;
        }
        String raw = organism.trim();
        if (raw.equals(HUMAN) || raw.equals(MOUSE) || raw.equals(RAT)) {
            return raw;
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("10090") || lower.contains("mouse") || lower.contains("mus")
                || lower.contains("mmu")) {
            return MOUSE;
        }
        if (lower.contains("10116") || lower.contains("rat") || lower.contains("rattus")
                || lower.contains("rno")) {
            return RAT;
        }
        if (lower.contains("9606") || lower.contains("human") || lower.contains("homo")
                || lower.contains("hsa")) {
            return HUMAN;
        }
        try {
            int id = Integer.parseInt(raw);
            if (id == 10090) {
                return MOUSE;
            }
            if (id == 10116) {
                return RAT;
            }
            if (id == 9606) {
                return HUMAN;
            }
        } catch (NumberFormatException ignored) {
        }
        return HUMAN;
    }

    public static int taxonId(String organism) {
        return Integer.parseInt(normalize(organism));
    }

    public static boolean isHuman(String organism) {
        return HUMAN.equals(normalize(organism));
    }

    public static boolean isMouse(String organism) {
        return MOUSE.equals(normalize(organism));
    }

    /** UI label, e.g. {@code Human (HSA / 9606)}. */
    public static String displayLabel(String organism) {
        return switch (normalize(organism)) {
            case MOUSE -> "Mouse (MMU / 10090)";
            case RAT -> "Rat (RNO / 10116)";
            default -> "Human (HSA / 9606)";
        };
    }

    /** Parse taxon from a UI label or raw id. */
    public static String fromDisplayLabel(String labelOrId) {
        return normalize(labelOrId);
    }

    public static String reactomePrefix(String organism) {
        return switch (normalize(organism)) {
            case MOUSE -> "R-MMU";
            case RAT -> "R-RNO";
            default -> "R-HSA";
        };
    }

    public static String keggCode(String organism) {
        return switch (normalize(organism)) {
            case MOUSE -> "mmu";
            case RAT -> "rno";
            default -> "hsa";
        };
    }

    /** True if accession is a Reactome ID for any supported species. */
    public static boolean isReactomeAccession(String accession) {
        return accession != null && REACTOME_ID.matcher(accession.trim()).find();
    }

    /** True if accession is a KEGG pathway id for any supported species. */
    public static boolean isKeggAccession(String accession) {
        return accession != null && KEGG_ID.matcher(accession.trim()).find();
    }

    /**
     * Remap a Reactome accession to the selected organism (shared numeric pathway id).
     * {@code R-HSA-69278} → {@code R-MMU-69278} when organism is mouse.
     */
    public static String reactomeIdForOrganism(String accession, String organism) {
        if (accession == null || accession.isBlank()) {
            return accession;
        }
        Matcher m = REACTOME_ID.matcher(accession.trim());
        if (!m.find()) {
            return accession.trim().toUpperCase(Locale.ROOT);
        }
        return reactomePrefix(organism) + "-" + m.group(2);
    }

    /**
     * Remap a KEGG pathway id to the selected organism.
     * {@code hsa04910} → {@code mmu04910} when organism is mouse.
     */
    public static String keggIdForOrganism(String accession, String organism) {
        if (accession == null || accession.isBlank()) {
            return null;
        }
        Matcher m = KEGG_ID.matcher(accession.trim());
        if (!m.find()) {
            return null;
        }
        return keggCode(organism) + m.group(2);
    }

    /** HPO is human-phenotype oriented; skip for non-human organisms. */
    public static boolean supportsHpo(String organism) {
        return isHuman(organism);
    }
}
