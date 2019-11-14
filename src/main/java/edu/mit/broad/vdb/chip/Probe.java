/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.vdb.chip;

import edu.mit.broad.vdb.meg.Gene;

/**
 * @author Aravind Subramanian
 * @author eby
 */
public class Probe implements Gene {

    private String fProbeName;
    private String fSymbol;
    private String fTitle;
    
    /**
     * Class constructor
     *
     * @param probeId
     * @param symbol
     * @param title
     */
    public Probe(final String probeId, final String symbol, final String title) {
        if (probeId == null || symbol == null || title == null) {
            throw new IllegalArgumentException("Parameters cannot be null; use empty string instead.");
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

        if (obj instanceof Gene) {
            String id = ((Gene) obj).getSymbol();
            return id.equals(getSymbol());
        }

        return false;
    }

    public Gene getGene() {
        return this;
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
}