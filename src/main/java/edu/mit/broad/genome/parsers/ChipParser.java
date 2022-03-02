/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.vdb.chip.*;
import edu.mit.broad.vdb.meg.Gene;

import java.io.*;
import java.util.*;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Parses a chip file
 */
public class ChipParser extends AbstractParser {

    // These are for the .chip parsing

    // Thes are for the TAF parsing
    // @maint see below if changing
    private static final String PROBE_SET_ID = "Probe Set ID";
    private static final String GENE_TITLE = "Gene Title";
    private static final String GENE_SYMBOL = "Gene Symbol";
    
    public ChipParser() { super(Chip.class); }

    /**
     * Export a chip
     * Only works for export to .chip format
     */
    public void export(final PersistentObject pob, final File file) throws Exception {
        final Chip chip = (Chip) pob;
        String[] colNames = new String[]{PROBE_SET_ID, GENE_SYMBOL, GENE_TITLE};

        final PrintWriter pw = startExport(chip, file);
        try {
            for (int i = 0; i < colNames.length; i++) {
                pw.print(colNames[i]);
                if (i != colNames.length) {
                    pw.print('\t');
                }
            }

            pw.println();

            for (int r = 0; r < chip.getNumProbes(); r++) {
                Probe probe = chip.getProbe(r);
                pw.print(probe.getName());
                pw.print('\t');

                Gene gene = probe.getGene();
                String symbol = null;
                String title = null;
                if (gene != null) {
                    symbol = gene.getSymbol();
                    title = gene.getTitle();
                }

                if (symbol == null) { symbol = Constants.NULL; }
                if (title == null) { title = Constants.NULL; }

                pw.print(symbol);
                pw.print('\t');
                pw.println(title);
            }
        } finally {
            doneExport();  // Handles pw.close()
        }
    }

    public List parse(String sourcepath, InputStream is) throws Exception {
        startImport(sourcepath);

        BufferedReader bin = new BufferedReader(new InputStreamReader(is));
        try {
            String currLine = nextLine(bin);
    
            List colHeaders = ParseUtils.string2stringsList(currLine, "\t");
            int ps_index = indexOf(PROBE_SET_ID, colHeaders, true);
            int symbol_index = indexOf(GENE_SYMBOL, colHeaders, true);
            int title_index = indexOf(GENE_TITLE, colHeaders, false);
    
            // save all rows so that we can determine how many rows exist
            List<Probe> probesList = new ArrayList<Probe>();
            currLine = nextLine(bin);
            Set<String> names = new HashSet<String>();
            Set<String> duplicates = new HashSet<String>();

            while (currLine != null) {
                final String[] fields = ParseUtils.string2strings(currLine, "\t");
                String probeName = (fields.length <= ps_index) ? null : StringUtils.trimToNull(fields[ps_index]);
    
                // Skip empty or duplicate probeNames
                if (probeName != null && !names.contains(probeName)) {
                    String symbol = (fields.length <= symbol_index) ? "" : StringUtils.trimToEmpty(fields[symbol_index]);
                    if ("---".equals(symbol)) symbol = "";
                    String title = (title_index < 0 || fields.length <= title_index) ? "" : StringUtils.trimToEmpty(fields[title_index]);
                    Probe probe = new Probe(probeName, symbol, title);
                    probesList.add(probe);
                    names.add(probeName);
                } else if (probeName != null && log.isDebugEnabled()) {
                    // Track the duplicates if we are debugging
                    duplicates.add(probeName);
                }
    
                currLine = nextLine(bin);
            }
    
            final Probe[] probes = probesList.toArray(new Probe[probesList.size()]);
            String chipName = FilenameUtils.getName(sourcepath);
            final Chip chip = new Chip(chipName, sourcepath, probes);
            log.info("Parsed from dotchip : {}", probes.length);
            if (!duplicates.isEmpty()) { log.debug("There were duplicate probes: {}\n{}\n{}", duplicates.size(), duplicates, chipName); }

            return unmodlist(chip);
        }
        finally {
            doneImport();
            bin.close();
        }
    }
}
