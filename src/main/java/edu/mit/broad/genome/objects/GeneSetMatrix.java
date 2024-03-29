/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

import java.util.List;
import java.util.Set;

/**
 * Essentially a collection of (probably related) GeneSets
 * In additin has colors and icons and names
 *
 * Lightweigth container for a bunch of genesets -- geneset data is not duplicated
 *
 * @author Aravind Subramanian, David Eby
 */
public interface GeneSetMatrix extends PersistentObject, Versioned {
    public MSigDBVersion getMSigDBVersion();

    public void setMSigDBVersion(MSigDBVersion msigDBVersion);

    public boolean containsSet(final String gsetName);

    public int getNumGeneSets();

    /**
     * return a ref to the real GeneSet -> @todo consider cloning?
     *
     * @param i
     * @return
     */
    public GeneSet getGeneSet(final int i);


    public GeneSet getGeneSet(final String gsetName);

    /**
     * All genesets
     * directly -- no cloning
     *
     * @return
     */
    public GeneSet[] getGeneSets();

    public List<GeneSet> getGeneSetsL();

    /**
     * The number of members in the biggest GeneSet.
     *
     * @return
     */
    public int getMaxGeneSetSize();

    public String[] getAllMemberNames();

    /**
     * non-redundant list of names of features across
     * genesets in this GeneSetMatrix
     *
     * @return
     */
    public String[] getAllMemberNamesOnlyOnce();

    public Set getAllMemberNamesOnlyOnceS();

    public String getGeneSetName(final int i);
}
