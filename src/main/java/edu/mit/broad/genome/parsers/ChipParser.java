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

        String sp_tmp = sourcepath.toUpperCase();

        if ((sp_tmp.indexOf(Constants.UNIGENE) != -1)
                || (sp_tmp.indexOf(Constants.GENE_SYMBOL) != -1)) { // @note hack maybe fix later
            return _parse_from_unigene_or_gene_symbol(sourcepath, is);
        } else if (sp_tmp.indexOf(Constants.SEQ_ACCESSION) != -1) {
            return _parse_from_seq_accession(sourcepath, is);
            //} //else if (sourcepath.startsWith("Stanford.chip")) {
            //return _parse_chip_meg_style(sourcepath, is);
        } else if (sourcepath.endsWith(Constants.CHIP)) {
            return _parse_from_dot_chip(sourcepath, is);
        } else if (sourcepath.endsWith(Constants.CSV)) {
            return _parse_from_csv(sourcepath, is);
        } else {
            throw new IllegalArgumentException("Unknown chip file type for parsing: " + sourcepath);
        }

    }

    /**
     * Parse from netaffx csv files
     *
     * @throws java.lang.Exception
     */
    private List _parse_from_csv(String sourcepath, InputStream is) throws Exception {

        startImport(sourcepath);

        BufferedReader bin = new BufferedReader(new InputStreamReader(is));

        String currLine = nextLine(bin);

        final List colHeaders = ParseUtils.string2stringsList_csv(currLine);
        final int ps_index = indexOf(PROBE_SET_ID, colHeaders, true);
        final int symbol_index = indexOf(GENE_SYMBOL, colHeaders, true);
        final int title_index = indexOf(GENE_TITLE, colHeaders, true);

        // save all rows so that we can determine how many rows exist
        Set probes = new THashSet();
        currLine = nextLine(bin);

        while (currLine != null) {
            String[] fields = ParseUtils.string2strings_csv(currLine);

            /*
            if (fields.length != 3) { // dont as this allows us to directly parse netaffx csv files also
                throw new ParserException("Bad chip file format. Expected 3 fields but found: " + fields.length + " line\n>" + currLine + "<\n");
            }
            */

            String symbol = fields[symbol_index];
            String title = fields[title_index];

            symbol = NamingConventions.symbolize(symbol);

            if (title.equals("---")) {
                title = null;
            }

            Probe probe = new SimpleProbe(fields[ps_index], symbol, title);
            probes.add(probe);

            /* this check uggh makes it very slow
            if (probes.contains(probe) == false) {
                probes.add(probe); // @note ignore duplicates -- believe thatthey are the same
            }
            */
            currLine = nextLine(bin);
        }

        final Probe[] keeps = (Probe[]) probes.toArray(new Probe[probes.size()]);
        final Chip chip = new FileInMemoryChip(FilenameUtils.getName(sourcepath), sourcepath, keeps);

        bin.close();

        doneImport();

        return unmodlist(chip);
    }

    private boolean isSymbolProbes(String sourcepath) {
        return sourcepath.indexOf("Gene_Symbol") != -1;
    }

    private List _parse_from_unigene_or_gene_symbol(String sourcepath, InputStream is) throws Exception {

        startImport(sourcepath);

        boolean isSymbolProbes = isSymbolProbes(sourcepath);

        BufferedReader bin = new BufferedReader(new InputStreamReader(is));

        String currLine = nextLine(bin);

        List colHeaders = ParseUtils.string2stringsList(currLine, "\t");
        int ps_index = indexOf(PROBE_SET_ID, colHeaders, true);
        int symbol_index = indexOf(GENE_SYMBOL, colHeaders, true);
        int title_index = indexOf(GENE_TITLE, colHeaders, true);
        int alias_index = indexOf(ALIASES, colHeaders, false); // @note optional UNUSED

        // save all rows so that we can determine how many rows exist
        List probesList = new ArrayList();
        currLine = nextLine(bin);
        Set symbols = new HashSet();

        while (currLine != null) {
            final String[] fields = ParseUtils.string2stringsV2(currLine);
            if (fields.length != 3 && fields.length != 4) {
                throw new ParserException("Bad format expecting 3 or 4 fields but found: " + fields.length + "\nOn line >" + currLine + "<");
            }

            String probeName = fields[ps_index];

            // make the probe a symbol too
            if (isSymbolProbes) {
                probeName = NamingConventions.symbolize(probeName);
            }

            if (probeName != null && !symbols.contains(probeName)) {
                String symbol = fields[symbol_index];
                String title = fields[title_index];
                symbol = NamingConventions.symbolize(symbol);

                if (title != null && title.equals("---")) {
                    title = null;
                }

                Set aliases = null;
                if (alias_index != -1 && alias_index < fields.length) {
                    aliases = ParseUtils.string2stringsSet(fields[alias_index], Constants.INTRA_FIELD_DELIM_S, false);
                }

                //System.out.println("alias_index: " + alias_index + " " + aliases);

                //Probe probe = new SimpleProbe(probeName, symbol, title);
                Probe probe = new SimpleProbe3(symbol, title, aliases);
                probesList.add(probe);
                symbols.add(symbol);
            }

            currLine = nextLine(bin);
        }

        // clean up by removing all aliases that are actually valid gene symbols
        final SimpleProbe3[] probes = (SimpleProbe3[]) probesList.toArray(new SimpleProbe3[probesList.size()]);
        for (int i = 0; i < probes.length; i++) {
            probes[i].removeAnyAliasesThatMatch(symbols);
        }
        final Chip chip = new FileInMemoryChip(FilenameUtils.getName(sourcepath), sourcepath, probes);

        bin.close();

        log.info("Parsed from unigene / gene symbol: " + probes.length);
        doneImport();

        return unmodlist(chip);
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


    // ditto as symbol or unigene except no titles in the file to save memory
    // so fill them in from the gene symbol chip as needed
    // also add the Gene_Symbol probes always
    // Then add aliases the same way
    private List _parse_from_seq_accession(final String sourcepath, final InputStream is) throws Exception {

        startImport(sourcepath);

        final BufferedReader bin = new BufferedReader(new InputStreamReader(is));

        String currLine = nextLine(bin);

        List colHeaders = ParseUtils.string2stringsList(currLine, "\t");
        int ps_index = indexOf(PROBE_SET_ID, colHeaders, true);
        int symbol_index = indexOf(GENE_SYMBOL, colHeaders, true);
        final Chip chip_gene_symbol = VdbRuntimeResources.getChip_Gene_Symbol();

        // save all rows so that we can determine how many rows exist
        List probes = new ArrayList();
        Set probeNamesAdded = new HashSet();

        // First add all gene symbols ditto

        for (int i = 0; i < chip_gene_symbol.getNumProbes(); i++) {
            probes.add(chip_gene_symbol.getProbe(i));
            probeNamesAdded.add(chip_gene_symbol.getProbe(i).getName());
        }

        log.debug("# of seq probes (from symbol): " + probes.size());

        currLine = nextLine(bin);
        while (currLine != null) {
            String[] fields = ParseUtils.string2strings(currLine, "\t", true);
            /*
            if (fields.length != 2 && fields.length != 3) {
                throw new ParserException("Bad format expecting 2 or 3 fields but found: " + fields.length + "\nOn line >" + currLine + "<");
            }
            */

            final String symbol = NamingConventions.symbolize(fields[symbol_index]);
            final String probeName = fields[ps_index];
            if (probeNamesAdded.contains(probeName) == false) { // @note add only if its not already in
                probeNamesAdded.add(probeName);
                final Probe probe = new SimpleProbe2(probeName, symbol, chip_gene_symbol); // @note always null for title
                probes.add(probe);
            }

            currLine = nextLine(bin);
        }

        log.debug("# of seq probes: " + probes.size());

        // Then add aliases the same way
        // NOTE: this AliasDb gets populated but IS NEVER USED.  It should be possible to remove this reference
        // and all related code in the future.
        final AliasDb aliasdb = VdbRuntimeResources.getAliasDb();
        final Probe[] alias_probes = aliasdb.getAliasesAsProbes();
        for (int i = 0; i < alias_probes.length; i++) { // @note add an alias only if it is not already in
            if (probeNamesAdded.contains(alias_probes[i].getName()) == false) {
                probes.add(alias_probes[i]);
            }
        }

        log.debug("FINAL # of seq probes: " + probes.size());

        final Probe[] keeps = (Probe[]) probes.toArray(new Probe[probes.size()]);
        final Chip chip = new FileInMemoryChip(FilenameUtils.getName(sourcepath), sourcepath, keeps);

        bin.close();

        log.info("Parser from Seq_Accession: " + keeps.length);
        doneImport();

        return unmodlist(chip);
    }

}    // End of class ChipParser
