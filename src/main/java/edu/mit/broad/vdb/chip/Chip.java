/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.chip;

import edu.mit.broad.genome.objects.*;
import edu.mit.broad.vdb.meg.Gene;

import java.util.Set;

/**
 * Capture a Chip object while enabling lazy loading of chip data
 * <p/>
 * Probe -> unique sequence feature used to measure a gene
 */
public interface Chip extends PersistentObject {

    public static final String COMBO_DUMMY_SOURCE = "combo_dummy_source";
    public static NullSymbolMode OMIT_NULLS = new NullSymbolModes.OmitNullSymbolMode();
    public static NullSymbolMode REPLACE_WITH_PROBEID = new NullSymbolModes.ReplaceWithProbeIdMode();

    public Chip cloneShallow(final String newName);

    public int getNumProbes() throws Exception;

    // HUGO/GENE SYMBOL BASED -------------------------------------
    // will error out if no such probe
    public Gene getHugo(final String probeName) throws Exception;

    public String getSymbol(final String probeName, final NullSymbolMode nmode);

    public String getTitle(final String probeName, final NullSymbolMode nmode);

    public String getProbeName(final int i) throws Exception;

    public Set getProbeNames() throws Exception;

    public GeneSet getProbeNamesAsGeneSet() throws Exception;

    public boolean isProbe(final String probeName) throws Exception;

    public Probe getProbe(final int i) throws Exception;

    public Probe getProbe(final String probeName) throws Exception;

    public Probe[] getProbes() throws Exception;

    public Set getProbeNames(final String symbol) throws Exception;

    public String[] getProbeNamesArr() throws Exception;

} // End Chip
