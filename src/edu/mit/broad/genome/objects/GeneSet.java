/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

/**
 * @author Aravind Subramanian
 *         <p/>
 *         Simply defines a set of things(by name) that belong to the same FSet
 *         A list of names - genes/probes etc. Only 1 class in a given FSet (unlike Templates)
 *         A vertical template in some ways
 *         <p/>
 *         (synonyms: Tags, Tag group, GeneSet, BioSet)
 *         For visualization purposes, a FSet can also be associated with an icon and color.
 *         <p/>
 *         There should be no duplicate members
 */
public interface GeneSet extends ISet {

    public String getName(final boolean stripAux);

    public GeneSet cloneShallow(final String newName);

    public GeneSet cloneDeep(final Dataset qualify);

    public GeneSet cloneDeep(final RankedList qualify);

    public int getNumMembers(final RankedList rl);

}    // End Interface GeneSet
  