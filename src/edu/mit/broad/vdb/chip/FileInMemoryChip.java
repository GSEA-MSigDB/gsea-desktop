/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.chip;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.vdb.meg.Gene;
import gnu.trove.THashMap;

import java.io.File;
import java.util.*;

/**
 * Chip data read from a flat file or url and kept in memory
 *
 * @author Aravind Subramanian
 */
public class FileInMemoryChip extends AbstractChip {

    /**
     * @note IMP lazilly loaded, at times
     */
    private Probe[] fProbes;

    /**
     * key -> Probe name (String), Value -> Probe object (UPpercased)
     * Lazilly inited
     */
    private Map fProbeNameProbeMap;

    private boolean fDeepDataInited = false;

    private Chip[] fMadeFromChips_lazy;

    /**
     * key -> symbol, value -> Set of probe names
     * Lazilly inited
     */
    private THashMap fSymbolProbeNameSetMap;

    /**
     * Class constructor
     * Shallow construction
     *
     * @param chipName
     * @param sourcePath
     */
    public FileInMemoryChip(final String chipName, final String sourcePath) {
        // @note dont do common init routine yet -> we're in skeleton mode
        super(chipName, sourcePath);
    }


    /**
     * @param chipName
     * @param sourcePath
     */
    public FileInMemoryChip(final String chipName, final String sourcePath, final Probe[] probes) {
        initHere(chipName, sourcePath, probes);
        // dont allow normal data init'ing as thats already done
        this.fDeepDataInited = true;
    }

    /**
     * Class Constructor
     */

    protected FileInMemoryChip(final Chip[] chips) throws Exception {
        super(_createComboName(chips), COMBO_DUMMY_SOURCE);

        if (chips == null) {
            throw new IllegalArgumentException("Param chips cannot be null");
        }

        for (int i = 0; i < chips.length; i++) {
            if (chips[i] == null) {
                throw new IllegalArgumentException("Param chips cannot be null at index: " + i);
            }
        }

        this.fMadeFromChips_lazy = chips;
    }

    // all data that gets here must be duplicated or filled etc
    // no duplication is done here
    // knows NOTHING about if the data was read in or not
    private void initHere(final String chipName, final String sourcePath, final Probe[] probes) {
        //TraceUtils.showTrace();

        if (!isInited()) {
            super.initialize(chipName); // double init barfs
        }

        if (NamingConventions.isURL(sourcePath) == false) {
            boolean missing = false;

            if (sourcePath == null) {
                missing = true;
            } else if (! new File(sourcePath).exists()) {
                missing = true;
            } else if (sourcePath.equals(COMBO_DUMMY_SOURCE)) {
                missing = true;
            }

            if (missing) {
                log.warn("Missing chip file: >" + sourcePath + "<");
            }
        }

        if (probes == null) {
            throw new IllegalArgumentException("Param probes cannot be null");
        }

        if (chipName == null) {
            throw new IllegalArgumentException("Param chipName cannot be null");
        }

        Set names = new HashSet();
        Set duplicates = new HashSet();
        this.fProbes = new Probe[probes.length];
        for (int i = 0; i < probes.length; i++) {
            this.fProbes[i] = probes[i];
            if (names.contains(probes[i].getName())) {
                duplicates.add(probes[i].getName());
            }
            names.add(probes[i].getName());
        }

        if (!duplicates.isEmpty()) {
            log.debug("There were duplicate probes: " + duplicates.size() + "\n" + duplicates + "\n" + getName());
        } else {
            log.debug("There were no duplicates: " + names.size() + " " + getName());
        }

        names.clear();
        duplicates.clear();

        // all set now, so init
        this.fSourcePath = sourcePath;

        //log.debug("Initing: " + chipName + " sourcePath: " + sourcePath + " probes: " + fProbes.length);
    }

    private void readDeepData() throws Exception {

        if (fDeepDataInited) {
            return;
        }

        // Lazy reading for combio chips
        if (fMadeFromChips_lazy != null) {
            Set allProbes = new HashSet();
            //Map probeNameProbeMap = new HashMap();
            for (int c = 0; c < fMadeFromChips_lazy.length; c++) {
                final Probe[] probes = fMadeFromChips_lazy[c].getProbes();
                for (int p = 0; p < probes.length; p++) {
                    Probe probe = probes[p];
                    allProbes.add(probe); // @note change -- duplicates get clobberred
                }
            }

            initHere(_createComboName(fMadeFromChips_lazy), COMBO_DUMMY_SOURCE, (Probe[]) allProbes.toArray(new Probe[allProbes.size()]));

            // dont allow normal data init'ing as thats already done
            this.fDeepDataInited = true;
            return;
        }

        // OK path based lazy reading

        try {

            FileInMemoryChip chip = (FileInMemoryChip) ParserFactory.readChip(fSourcePath);
            initHere(chip.getName(), fSourcePath, chip.fProbes); // this is the magix

        } catch (Throwable t) {
            t.printStackTrace();
            throw new Exception(t);
        } finally {
            this.fDeepDataInited = true; // Needed otherwise endless loop
        }
    }

    public String getQuickInfo() {
        if (fDeepDataInited) {
            return fProbes.length + " probes";
        } else {
            return null;
        }
    }

    public int getNumProbes() throws Exception {
        readDeepData();
        return fProbes.length;
    }

    public Probe getProbe(final int i) throws Exception {
        readDeepData();
        return fProbes[i];
    }

    public String getProbeName(final int i) throws Exception {
        readDeepData();
        return fProbes[i].getName();
    }

    public Probe[] getProbes() throws Exception {
        readDeepData();
        Probe[] probes = new Probe[fProbes.length];
        for (int p = 0; p < fProbes.length; p++) {
            probes[p] = fProbes[p];
        }

        return probes;
    }

    // ------------------------------------------------------------------------------- //
    // ----------------------------- PROBE PROBE MAP INIT STUFF ---------------------- //
    // ------------------------------------------------------------------------------- //

    protected void initProbeProbeMap() throws Exception {

        if (this.fProbeNameProbeMap == null) {
            readDeepData();
            this.fProbeNameProbeMap = new HashMap();
            for (int i = 0; i < fProbes.length; i++) {
                fProbeNameProbeMap.put(fProbes[i].getName().toUpperCase(), fProbes[i]); // @note
            }
        }
    }

    public Set getProbeNames() throws Exception {
        initProbeProbeMap();
        final Set set = new HashSet();
        for (int i = 0; i < getNumProbes(); i++) {
            set.add(getProbe(i).getName());
        }
        return set;
    }

    public boolean isProbe(String probeName) throws Exception {
        if (probeName == null) {
            return false;
        }

        initProbeProbeMap();

        probeName = probeName.toUpperCase(); // @note
        return fProbeNameProbeMap.containsKey(probeName);
    }

    public Probe getProbe(final String probeName_orig) throws Exception {
        if (probeName_orig == null) {
            throw new IllegalArgumentException("Param probeName cannot be null");
        }

        initProbeProbeMap();

        final String probeName = probeName_orig.toUpperCase();
        final Object obj = fProbeNameProbeMap.get(probeName);

        if (obj == null) {
            throw new IllegalArgumentException("No Probe called: " + probeName_orig + " on this chip (chip name is >" + getName() + "<)");
        } else {
            return (Probe) obj;
        }
    }

    // ------------------------------------------------------------------------------- //
    // ----------------------------- SYMBOL PROBE MAP INIT STUFF -------------------- //
    // ------------------------------------------------------------------------------- //

    public Set getProbeNames(final String geneSymbol) throws Exception {
        initSymbolMap();
    
        Object curr = fSymbolProbeNameSetMap.get(geneSymbol);
    
        //System.out.println("mapping: " + geneSymbol + " got: " + curr + " fSymbolProbeNameSetHashMap: " + fSymbolProbeNameSetHashMap.size());
        //Printf.outl(fSymbolProbeNameSetHashMap);
    
        if (curr == null) {
            return Collections.EMPTY_SET;
        } else {
            return (Set) curr;
        }
    }


    // ------------------------------------------------------------------------------- //
    // ----------------------------- SYMBOL PROBE MAP INIT STUFF -------------------- //
    // ------------------------------------------------------------------------------- //
    
    private void initSymbolMap() throws Exception {
        if (fSymbolProbeNameSetMap == null) {
            readDeepData();
            //log.debug("initing SYMBOL MAP: " + fProbes);
            this.fSymbolProbeNameSetMap = new THashMap();
            for (int i = 0; i < fProbes.length; i++) {
                Gene gene = fProbes[i].getGene();
                if (gene != null) {
                    String symbol = gene.getSymbol();
                    Object curr = fSymbolProbeNameSetMap.get(symbol);
                    if (curr == null) {
                        curr = new HashSet();
                    }
                    ((Set) curr).add(fProbes[i].getName());
                    this.fSymbolProbeNameSetMap.put(symbol, curr);
                }
            }
        }
    
    }


    private static String _createComboName(final Chip[] chips) {
        StringBuffer chipName = new StringBuffer();
        for (int c = 0; c < chips.length; c++) {
            chipName.append(chips[c].getName());
            if (c != chips.length - 1) {
                chipName.append('_');
            }
        }

        return chipName.toString();
    }


} // End class FileInMemoryChip
