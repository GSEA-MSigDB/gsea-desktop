/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.vdb.VdbRuntimeResources;
import edu.mit.broad.vdb.chip.*;
import edu.mit.broad.vdb.meg.AliasDb;
import edu.mit.broad.vdb.meg.Gene;
import gnu.trove.THashSet;

import java.io.*;
import java.util.*;

import org.apache.commons.io.FilenameUtils;

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
    
    // NOTE: the Aliases column is optional and *does* get parsed and loaded.  However, that loaded
    // data is never used.  This could possibly be removed in the future.
    private static final String ALIASES = "Aliases";
    /**
     * Class Constructor.
     */
    public ChipParser() {
        super(Chip.class);
    }

    public void export(final PersistentObject pob, final File file) throws Exception {
        export((Chip) pob, file, true);
    }

    /**
     * Export a chip
     * Only works for export to .chip format
     *
     * @see "Above for format"
     */
    public void export(final Chip chip, final File file, final boolean withTitles) throws Exception {

        String[] colNames;

        if (withTitles) {
            colNames = new String[]{PROBE_SET_ID, GENE_SYMBOL, GENE_TITLE};
        } else {
            colNames = new String[]{PROBE_SET_ID, GENE_SYMBOL};
        }

        final PrintWriter pw = new PrintWriter(new FileOutputStream(file));

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

            if (symbol == null) {
                symbol = Constants.NULL;
            }

            if (title == null) {
                title = Constants.NULL;
            }

            pw.print(symbol);

            if (withTitles) {
                pw.print('\t');
                pw.print(title);
            }

            pw.println();
        }

        pw.close();

        doneExport();

    }    // End export

    public List parse(String sourcepath, InputStream is) throws Exception {

        if (sourcepath.endsWith(Constants.CHIP)) {
            return _parse_from_dot_chip(sourcepath, is);
        } else {
            throw new IllegalArgumentException("Unknown chip file type for parsing: " + sourcepath);
        }
    }

    private List _parse_from_dot_chip(String sourcepath, InputStream is) throws Exception {

        startImport(sourcepath);

        BufferedReader bin = new BufferedReader(new InputStreamReader(is));

        String currLine = nextLine(bin);

        List colHeaders = ParseUtils.string2stringsList(currLine, "\t");
        int ps_index = indexOf(PROBE_SET_ID, colHeaders, true);
        int symbol_index = indexOf(GENE_SYMBOL, colHeaders, true);
        int title_index = indexOf(GENE_TITLE, colHeaders, true);

        // save all rows so that we can determine how many rows exist
        List probesList = new ArrayList();
        currLine = nextLine(bin);
        Set names = new HashSet();

        while (currLine != null) {
            final String[] fields = ParseUtils.string2strings(currLine, "\t", true);

            /* we are looking up indexs so can be extra OR fewer
            if (fields.length != 3) {
                throw new ParserException("Bad format expecting 3 fields but found: " + fields.length + "\nOn line >" + currLine + "<");
            }
            */

            String probeName = fields[ps_index];

            if (probeName != null && !names.contains(probeName)) {
                String symbol = fields[symbol_index];
                symbol = NamingConventions.symbolize(symbol);

                String title = null;
                try {
                    title = fields[title_index];
                    if (title != null && title.equals("---")) {
                        title = null;
                    }
                } catch (Throwable t) {

                }

                Probe probe = new SimpleProbe(probeName, symbol, title);
                probesList.add(probe);
            }

            if (probeName != null) {
                names.add(probeName);
            }

            currLine = nextLine(bin);
        }

        final Probe[] probes = (Probe[]) probesList.toArray(new Probe[probesList.size()]);
        final Chip chip = new FileInMemoryChip(FilenameUtils.getName(sourcepath), sourcepath, probes);

        bin.close();

        log.info("Parsed from dotchip : " + probes.length);
        doneImport();

        return unmodlist(chip);
    }

}    // End of class ChipParser
