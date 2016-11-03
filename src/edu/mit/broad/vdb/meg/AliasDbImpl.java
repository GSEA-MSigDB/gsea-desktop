/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.meg;

import edu.mit.broad.genome.*;
import edu.mit.broad.genome.objects.AbstractObject;
import edu.mit.broad.genome.objects.StringDataframe;
import edu.mit.broad.genome.parsers.ParseUtils;
import edu.mit.broad.genome.parsers.StringDataframeParser;
import edu.mit.broad.vdb.VdbRuntimeResources;
import edu.mit.broad.vdb.chip.Chip;
import edu.mit.broad.vdb.chip.Probe;
import edu.mit.broad.vdb.chip.SimpleProbe2;

import java.io.File;
import java.util.*;

/**
 * @author Aravind Subramanian
 */
public class AliasDbImpl extends AbstractObject implements AliasDb {

    // Either file or chip must be specified
    private File fFile_opt;

    private Chip fChip_opt;

    /**
     * Class constructor
     * <p/>
     * Either file or chip must be specified
     *
     * @param file
     */
    public AliasDbImpl(final File file_opt, final Chip chip_opt) {
        super.initialize(_name(file_opt, chip_opt));
        this.fFile_opt = file_opt;
        this.fChip_opt = chip_opt;
    }

    public String getQuickInfo() {
        return null;
    }

    public Probe[] getAliasesAsProbes() throws Exception {
        _initAliasSymbolMap();
        final Probe[] probes = new Probe[fAliasSymbolMap.size()];
        int cnt = 0;
        for (Iterator iterator = fAliasSymbolMap.keySet().iterator(); iterator.hasNext();) {
            String alias = iterator.next().toString();
            String symbol = fAliasSymbolMap.get(alias).toString();
            probes[cnt++] = new SimpleProbe2(alias, symbol, VdbRuntimeResources.getChip_Gene_Symbol());
        }

        return probes;
    }

    private static String _name(final File file_opt, final Chip chip_opt) {
        if (chip_opt != null && file_opt != null) {
            throw new IllegalArgumentException("Both file: " + file_opt + " and chip cannot be specified: " + chip_opt);
        }
        if (file_opt != null) {
            return file_opt.getName();
        } else if (chip_opt != null) {
            return chip_opt.getName();
        } else {
            throw new IllegalArgumentException("Both file and chip cannot be null");
        }
    }

    private Map fAliasSymbolMap;

    private void _initAliasSymbolMap() throws Exception {
        if (fAliasSymbolMap != null) {
            return;
        }

        final Map map = new HashMap();
        final Set duplicates = new HashSet();

        if (fFile_opt != null) {
            final StringDataframe sdf = new StringDataframeParser().parseSdf(fFile_opt);
            // only 2 columns
            // NAME ALIASES
            for (int r = 0; r < sdf.getNumRow(); r++) {
                final String symbol = NamingConventions.symbolize(sdf.getRowName(r));
                final String[] accessions = ParseUtils.string2strings(sdf.getElement(r, 0), Constants.INTRA_FIELD_DELIM_S, false);
                final Set accessions_suc = new HashSet();
                for (int s = 0; s < accessions.length; s++) {
                    accessions_suc.add(accessions[s].toUpperCase()); // @note aliases always stored in UC
                }

                final String[] accessions_uc = (String[]) accessions_suc.toArray(new String[accessions_suc.size()]);
                for (int s = 0; s < accessions_uc.length; s++) {
                    if (map.containsKey(accessions_uc[s])) {
                        duplicates.add(accessions_uc[s]);
                    } else { // ignore duplicates -- dont add at all
                        map.put(accessions_uc[s], symbol);
                    }
                }
            }

        } else {

            final Probe[] probes = fChip_opt.getProbes();
            for (int i = 0; i < probes.length; i++) {
                Gene gene = probes[i].getGene();
                if (gene != null && gene != Gene.NULL_GENE && gene.getAliases() != null && !gene.getAliases().isEmpty()) {
                    final String symbol = gene.getSymbol();
                    final String[] aliases = gene.getAliasesArray();
                    for (int s = 0; s < aliases.length; s++) {
                        String alias = aliases[s].toUpperCase(); // @note aliases always stored in UC
                        if (map.containsKey(alias)) {
                            duplicates.add(alias);
                        } else { // ignore duplicates -- dont add at all
                            map.put(alias, symbol);
                        }
                    }
                }
            }
        }

        if (!duplicates.isEmpty()) {
            log.warn("There are duplicate entry for accessions: " + duplicates.size() + " ignoring them ..." + " total #: " + map.size());
            //throw new IllegalStateException("Duplicate entry for accessions: " + duplicates.size() + "\n" + duplicates);
        }

        log.info("# of aliases: " + map.size());

        this.fAliasSymbolMap = map;
    }

} // End class AliasDbImpl
