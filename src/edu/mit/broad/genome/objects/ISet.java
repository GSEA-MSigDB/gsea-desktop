/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import java.util.List;
import java.util.Set;

/**
 * @author Aravind Subramanian
 *         <p/>
 *         Simply defines a set of things(by name) that belong to the same Set
 *         A list of names - genes/probes etc. Only 1 class in a given ISet (unlike Templates)
 *         A vertical template in some ways
 *         <p/>
 *         (synonyms: Tags, Tag group, GeneSet, BioSet)
 *         For visualization purposes, a ISet can also be associated with an icon and color.
 *         <p/>
 *         There should be no duplicate members
 */
public interface ISet extends PersistentObject {

    /**
     * @param pos
     * @return Name of member at position pos in the group
     */
    public String getMember(int pos);

    /**
     * Checks if specified name belongs to this ISet.
     *
     * @param name
     * @return
     */
    public boolean isMember(String name);

    /**
     * @return Number of members of this ISet
     */
    public int getNumMembers();

    /**
     * @return Unmodifiable list of members of this ISet
     */
    public List getMembers();

    public Set getMembersS();

    /**
     * @return
     */
    public String[] getMembersArray();

}    // End Interface ISet
