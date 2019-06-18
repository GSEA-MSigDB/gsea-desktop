/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.vdb.chip;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.objects.AbstractObject;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.vdb.meg.Gene;

/**
 * Capture a Chip object while enabling lazy loading of chip data
 * <p/>
 * Probe -> unique sequence feature used to measure a gene
 */
public class Chip extends AbstractObject {

    /**
     * @note IMP lazily loaded, at times
     */
    private Probe[] fProbes;

    // TODO: want to avoid forced uppercase.  Evaluate this possibility
    /**
     * key -> Probe name (String), Value -> Probe object (UPpercased)
     * Lazily inited
     */
    private Map<String, Probe> fProbeNameProbeMap;
    private boolean fDeepDataInited = false;
    
    /**
     * key -> symbol, value -> Set of probe names
     * Lazy inited
     */
    private Map<String, Set<String>> fSymbolProbeNameSetMap;
    private String fSourcePath;

    public Chip(final String chipName, final String sourcePath) {

        // @note dont do common init routine yet -> we're in skeletonmode
        super.initialize(chipName);

        if (sourcePath == null) {
            throw new IllegalArgumentException("Parameter sourcePath cannot be null");
        }

        this.fSourcePath = sourcePath;
    }

    public Chip(final String chipName, final String sourcePath, final Probe[] probes) {
        initHere(chipName, sourcePath, probes);
        // dont allow normal data init'ing as thats already done
        this.fDeepDataInited = true;
    }

    public boolean equals(Object obj) {
        return (obj instanceof Chip) && (((Chip) obj).getName().equalsIgnoreCase(getName()));
    }

    public int hashCode() {
        return getName().hashCode();
    }

    public String toString() {
        return getName();
    }

    public GeneSet getProbeNamesAsGeneSet() throws Exception {
        return new GeneSet(getName(), getProbeNames());
    }

    public Gene getHugo(final String probeName) throws Exception {
        return getProbe(probeName).getGene();
    }

    public String getSymbol(final String probeName, final NullSymbolMode nmode) {
        // @TODO: doesn't look like this throws any reasonable exceptions.  
        // Maybe NPEs but we should detect those instead of try/catch
        try {
            if (isProbe(probeName)) {
                return nmode.getSymbol(probeName, getHugo(probeName));
            } else {
                return nmode.getSymbol(probeName, null);
            }
        } catch (Throwable t) {
            log.error(t);
            return "";
            // return Constants.NA;
        }
    }

    public String getTitle(final String probeName, final NullSymbolMode nmode) {
        // @TODO: doesn't look like this throws any reasonable exceptions.  
        // Maybe NPEs but we should detect those instead of try/catch
        try {
            if (isProbe(probeName)) {
                return nmode.getTitle(probeName, getHugo(probeName));
            } else {
                return nmode.getTitle(probeName, null);
            }
        } catch (Throwable t) {
            log.error(t);
            return "";
            // return Constants.NA;
        }

    }

    private void initHere(final String chipName, final String sourcePath, final Probe[] probes) {
        //TraceUtils.showTrace();
    
        if (!isInited()) {
            super.initialize(chipName); // double init barfs
        }
    
        if (!NamingConventions.isURL(sourcePath) && (sourcePath == null || ! new File(sourcePath).exists())) {
            log.warn("Missing chip file: >" + sourcePath + "<");
        }
    
        if (probes == null) {
            throw new IllegalArgumentException("Param probes cannot be null");
        }
    
        if (chipName == null) {
            throw new IllegalArgumentException("Param chipName cannot be null");
        }
    
        Set<String> names = new HashSet<String>();
        Set<String> duplicates = new HashSet<String>();
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
        
        // Path based lazy reading
        try {
            Chip chip = ParserFactory.readChip(fSourcePath);
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

    private void initProbeProbeMap() throws Exception {
    
        if (this.fProbeNameProbeMap == null) {
            readDeepData();
            this.fProbeNameProbeMap = new HashMap<String, Probe>();
            for (int i = 0; i < fProbes.length; i++) {
                // TODO: want to avoid forced uppercase.  Evaluate this possibility
                fProbeNameProbeMap.put(fProbes[i].getName().toUpperCase(), fProbes[i]); // @note
            }
        }
    }

    public Set<String> getProbeNames() throws Exception {
        initProbeProbeMap();
        final Set<String> set = new HashSet<String>();
        for (int i = 0; i < fProbes.length; i++) {
            set.add(getProbe(i).getName());
        }
        return set;
    }

    public boolean isProbe(String probeName) throws Exception {
        if (probeName == null) {
            return false;
        }
    
        initProbeProbeMap();
    
        // TODO: want to avoid forced uppercase.  Evaluate this possibility
        probeName = probeName.toUpperCase(); // @note
        return fProbeNameProbeMap.containsKey(probeName);
    }

    public Probe getProbe(final String probeName_orig) throws Exception {
        if (probeName_orig == null) {
            throw new IllegalArgumentException("Param probeName cannot be null");
        }
    
        initProbeProbeMap();
    
        // TODO: want to avoid forced uppercase.  Evaluate this possibility
        final String probeName = probeName_orig.toUpperCase();
        final Object obj = fProbeNameProbeMap.get(probeName);
    
        if (obj == null) {
            throw new IllegalArgumentException("No Probe called: " + probeName_orig + " on this chip (chip name is >" + getName() + "<)");
        } else {
            return (Probe) obj;
        }
    }

    public Set<String> getProbeNames(final String geneSymbol) throws Exception {
        initSymbolMap();
    
        Set<String> curr = fSymbolProbeNameSetMap.get(geneSymbol);
    
        if (curr == null) {
            return Collections.emptySet();
        } else {
            return curr;
        }
    }

    private void initSymbolMap() throws Exception {
        if (fSymbolProbeNameSetMap == null) {
            readDeepData();
            this.fSymbolProbeNameSetMap = new HashMap<String, Set<String>>();
            for (int i = 0; i < fProbes.length; i++) {
                Gene gene = fProbes[i].getGene();
                if (gene != null) {
                    String symbol = gene.getSymbol();
                    Set<String> curr = fSymbolProbeNameSetMap.get(symbol);
                    if (curr == null) {
                        curr = new HashSet<String>();
                    }
                    curr.add(fProbes[i].getName());
                    this.fSymbolProbeNameSetMap.put(symbol, curr);
                }
            }
        }
    
    }
}