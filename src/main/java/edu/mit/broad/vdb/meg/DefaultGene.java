/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.vdb.meg;

import edu.mit.broad.genome.NamingConventions;

/**
 * User: aravind subramanian
 */
public class DefaultGene implements Gene {

    private String fSymbol;

    private String fTitle;

    // db4o constructor
    public DefaultGene() {
    }

    /**
     * Class constructor
     *
     * @param symbol
     * @param title
     */
    public DefaultGene(final String symbol,
                       String title) {

        if (symbol == null) {
            throw new IllegalArgumentException("Parameter symbol cannot be null");
        }

        this.fTitle = NamingConventions.titleize(title);
        // TODO: want to avoid forced uppercase.  Evaluate this possibility
        this.fSymbol = symbol.toUpperCase(); // @note
    }

    public String getSymbol() {
        return fSymbol;
    }

    public String getTitle() {
        return fTitle;
    }

    public String getTitle_truncated() {
        return Helper.getTitle_truncated(fTitle);
    }

    public boolean equals(Object obj) { // @note equals on just the symbol
        return obj instanceof Gene && ((Gene) obj).getSymbol().equalsIgnoreCase(getSymbol());
    }

    public String toString() {
        return getSymbol();
    }

    // @note equals on justthe symbol
    public int hashCode() {
        return getSymbol().hashCode();
    }


} // End class DefaultNugo
