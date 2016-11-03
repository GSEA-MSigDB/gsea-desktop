/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.chip;

import edu.mit.broad.vdb.meg.DefaultGene;
import edu.mit.broad.vdb.meg.Gene;

/**
 * Prsewrves some meory by not requiring a title
 *
 * @author Aravind Subramanian
 */
public class SimpleProbe2 implements Probe {

    private String fProbeName;
    private String fSymbol;

    private Chip fGeneSymbol;

    /**
     * Class constructor
     *
     * @param probeId
     * @param symbol
     * @param title
     */
    public SimpleProbe2(final String probeId, final String symbol, final Chip chip_gene_symbol) {
        if (probeId == null) {
            throw new IllegalArgumentException("Param probeId cannot be null");
        }

        if (chip_gene_symbol == null) {
            throw new IllegalArgumentException("Param chip_gene_symbol cannot be null");
        }

        this.fProbeName = probeId;
        this.fSymbol = symbol;
        this.fGeneSymbol = chip_gene_symbol;
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
            try {
                return fGeneSymbol.getHugo(fSymbol);
            } catch (Throwable t) {
                return new DefaultGene(fSymbol, null, null, null, null);
            }
        }
    }

} // End class SimpleProbe2
