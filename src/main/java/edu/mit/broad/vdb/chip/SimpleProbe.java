/*******************************************************************************
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.chip;

import java.util.Collections;
import java.util.Set;

import edu.mit.broad.vdb.meg.DefaultGene;
import edu.mit.broad.vdb.meg.Gene;

/**
 * @author Aravind Subramanian
 */
public class SimpleProbe implements Probe, Gene {

    private String fProbeName;
    private String fSymbol;
    private String fTitle;

    private static String[] EMPTY_ALIASES = new String[] {};
    
    /**
     * Class constructor
     *
     * @param probeId
     * @param symbol
     * @param title
     */
    public SimpleProbe(final String probeId, final String symbol, final String title) {
        if (probeId == null) {
            throw new IllegalArgumentException("Param probeId cannot be null");
        }

        this.fProbeName = probeId;
        this.fSymbol = symbol;
        this.fTitle = title;
    }

    public String toString() {
        return fProbeName;
    }

    public int hashCode() {
        return fProbeName.hashCode();
    }

    public String getName() {
        return fProbeName;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Probe) {
            String id = ((Probe) obj).getName();
            return id.equals(fProbeName);
        }

        return false;
    }

    // yeah similarly can have multiple Hugos - but that is even more of a huh huh so completely preclude that
    public Gene getGene() {
        if (fSymbol == null) {
            return Gene.NULL_GENE;
        } else {
            return new DefaultGene(fSymbol, fTitle, null, null, null);
        }
    }

    @Override
    public String getSymbol() {
        return fSymbol;
    }

    @Override
    public String getTitle() {
        return fTitle;
    }

    @Override
    public String getTitle_truncated() {
        return Helper.getTitle_truncated(fTitle);
    }

    @Override
    public Set getAliases() {
        return Collections.EMPTY_SET;
    }

    @Override
    public String[] getAliasesArray() {
        return EMPTY_ALIASES;
    }
} // End class SimpleProbe
