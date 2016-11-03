/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.chip;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.vdb.meg.Gene;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Thin wrapper around a Hugo (meant for the gene symbol chip)
 *
 * @author Aravind Subramanian
 */
public class SimpleProbe3 implements Probe, Gene {

    private String fSymbol;

    private String fTitle;

    private Set fAliases;

    private static final Set EMPTY_SET = Collections.unmodifiableSet(new HashSet());


    /**
     * Class constructor
     *
     * @param symbol
     * @param title
     * @param seqAccessions
     * @param aliases
     */
    public SimpleProbe3(final String symbol,
                        String title,
                        final Set aliases) {

        if (symbol == null) {
            throw new IllegalArgumentException("Parameter symbol cannot be null");
        }


        this.fTitle = NamingConventions.titleize(title);
        this.fSymbol = symbol.toUpperCase(); // @note

        this.fAliases = aliases;

        /* @note dont do -- see below
        if (aliases != null) {
            this.fAliases = Collections.unmodifiableSet(aliases);
        }
        */

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

    public Set getAliases() {
        if (fAliases == null) {
            return EMPTY_SET;
        } else {
            return Collections.unmodifiableSet(fAliases);
        }
    }

    public void removeAnyAliasesThatMatch(final Set these) {
        if (fAliases != null && fAliases.isEmpty() == false) {
            //int before = fAliases.size();
            this.fAliases.removeAll(these);
            this.fAliases = Collections.unmodifiableSet(fAliases);
            //System.out.println("before: " + before + " after: " + fAliases.size());
        }
    }

    public String[] getAliasesArray() {
        Set set = getAliases();
        return (String[]) set.toArray(new String[set.size()]);
    }

    public String toString() {
        return getSymbol();
    }

    public int hashCode() {
        return getSymbol().hashCode();
    }

    public String getName() {
        return getSymbol();
    }

    public boolean equals(Object obj) {
        if (obj instanceof Probe) {
            String id = ((Probe) obj).getName();
            return id.equals(getName());
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

    // -------------------------------------------------------------------------------------------- //


} // End class SimpleProbe3
