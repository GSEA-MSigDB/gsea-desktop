/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.alg.ComparatorFactory;
import edu.mit.broad.genome.parsers.AuxUtils;
import edu.mit.broad.vdb.chip.Probe;
import edu.mit.broad.vdb.meg.Gene;

import java.util.*;

/**
 * Simply defines a set of things(by name) that belong to the same FSet
 * A list of names - genes/probes etc. Only 1 class in a given FSet (unlike Templates)
 * A vertical template in some ways
 * <p/>
 * (synonyms: Tags, Tag group, GeneSet, BioSet)
 * For visualization purposes, a FSet can also be associated with an icon and color.
 * <p/>
 * There should be no duplicate members
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class FSet extends AbstractObject implements GeneSet {

    /**
     * Each member is a String (not using a Set as we want to be able to do an
     * indexOf
     * As/Also i.e order sometimes matters
     */
    protected List fMembers;

    /**
     * Class Constructor.
     */
    protected FSet() {
    }

    /**
     * Class Constructor.
     * No member can be null
     * No duplicates allowed
     */
    public FSet(final String name, final String nameEnglish, final String[] members) {
        init(name, nameEnglish, members, true);
    }

    /**
     * Class Constructor.
     * No member can be null
     * No duplicates allowed
     * Objects in specified List are to Stringed
     * Data is NOT shared
     */
    public FSet(final String name, final String nameEnglish, final List members, final boolean checkForDuplicates) {
        init(name, nameEnglish, members, checkForDuplicates);
    }

    public FSet(final String name, final String[] members) {
        init(name, null, members, true);
    }

    /**
     * Data is NOT shared
     *
     * @param name
     * @param members
     */
    public FSet(final String name, final Set members) {
        init(name, null, members, false);
    }

    /**
     * @param gset
     * @param sds
     * @return
     * @maint does not use shared init methods
     * (nned to do custom stuff)
     * <p/>
     * FSet ordered by specified ScoredDataset and a whole NEW FSet is returned
     * Though fsets have no sense of order per se, its sometimes useful to put them
     * in order for printing out / bpog generation.
     */
    public FSet(final GeneSet gset, final ScoredDataset sds) {

        // make safe copy
        List members = new ArrayList(gset.getMembers().size());
        for (int i = 0; i < gset.getMembers().size(); i++) {
            members.add(gset.getMember(i));
        }

        // sort it
        Collections.sort(members, new ComparatorFactory.ScoredDatasetScoreComparator(sds));

        this.init(gset.getName() + ".orderedby." + sds.getName(), gset.getNameEnglish(), members, false);
    }

    // @maint IMP see duplicated init method below
    protected void init(final String name, final String nameEnglish, final Collection members, final boolean checkForDuplicates) {
        super.initialize(name, nameEnglish);

        //TraceUtils.showTrace("members: " + members.size());
        if (members == null) {
            throw new NullPointerException("Param members cant be null");
        }

        this.fMembers = new ArrayList(); // make safe copy

        int cnt = 0;
        Iterator it = members.iterator();
        while (it.hasNext()) {
            Object member = it.next();
            cnt++;

            if (member == null) {
                throw new NullPointerException("Member is null at: " + cnt);
            }

            String mn;

            if (member instanceof Gene) {
                Gene gene = ((Gene) member);
                mn = gene.getSymbol();
            } else if (member instanceof Probe) {
                mn = ((Probe) member).getName();
            } else {
                mn = member.toString();
            }

            if (checkForDuplicates) {
                // IMP to add, as in some cases it might be legit
                // for example when creating a combined dataset from bpog (when markers are shared)
                if (fMembers.contains(mn)) {
                    //TraceUtils.showTrace();
                    log.warn("Duplicate GeneSet member: " + mn);// dont barf, just warn (possible imp for randomizations)
                } else {
                    fMembers.add(mn);
                }
            } else { // blindly believe and add
                fMembers.add(mn);
            }
        }

    }

    // @maint IMP see duplicated init method above
    private void init(final String name, final String nameEnglish, final String[] members, final boolean checkForDuplicates) {
        super.initialize(name, nameEnglish);

        //TraceUtils.showTrace("membersTT: " + members.length);

        if (members == null) {
            throw new NullPointerException("Members param cant be null");
        }

        this.fMembers = new ArrayList(); // make safe copy

        for (int i = 0; i < members.length; i++) {
            if (members[i] == null) {
                throw new NullPointerException("Member is null at: " + i);
            }

            if (checkForDuplicates) {
                if (fMembers.contains(members[i])) {
                    log.warn("Duplicate GeneSet member: " + members[i]);// dont barf, just warn (possible imp for randomizations)
                    //TraceUtils.showTrace();
                } else {
                    fMembers.add(members[i]);
                }
            } else { // blindly believe
                fMembers.add(members[i]);
            }
        }

    }

    public GeneSet cloneDeep(final Dataset qualify) {

        List all = new ArrayList(fMembers);

        List remove = new ArrayList();
        for (int i = 0; i < getNumMembers(); i++) {
            String rn = getMember(i);
            if (qualify.getRowIndex(rn) == -1) {
                remove.add(rn);
            }
        }

        for (int i = 0; i < remove.size(); i++) {
            all.remove(remove.get(i));
        }

        return new FSet(getName(), getNameEnglish(), all, false);
    }

    public GeneSet cloneDeep(final RankedList qualify) {

        List all = new ArrayList(fMembers);

        List remove = new ArrayList();
        for (int i = 0; i < getNumMembers(); i++) {
            String rn = getMember(i);
            if (qualify.getRank(rn) == -1) {
                remove.add(rn);
            }
        }

        for (int i = 0; i < remove.size(); i++) {
            all.remove(remove.get(i));
        }

        return new FSet(getName(), getNameEnglish(), all, false);
    }

    // @todo this is prob not needed -> instead a constructor is better i think
    public GeneSet cloneShallow(final String newName) {
        if (newName == null) {
            throw new IllegalArgumentException("Param newName cannot be null");
        }

        FSet fset = new FSet();
        fset.initialize(newName, getNameEnglish()); // this sets the new name

        // the vars are all shallow
        fset.fMembers = this.fMembers; // @note not duplicated
        return fset;
    }

    public String getQuickInfo() {
        StringBuffer buf = new StringBuffer().append(getNumMembers()).append(" members");
        return buf.toString();
    }

    public String getName(boolean stripAux) {
        if (stripAux) {
            return AuxUtils.getAuxNameOnlyNoHash(getName());
        } else {
            return getName();
        }
    }

    /**
     * @param pos
     * @return Name of member at position pos in the group
     */
    public String getMember(int pos) {
        return (String) fMembers.get(pos);
    }

    /**
     * Checks if specified name belongs to this FSet.
     *
     * @param name
     * @return
     */
    public boolean isMember(final String name) {
        //log.debug(">" + name + "<" + getNumMembers());
        return fMembers.contains(name);
    }

    /**
     * @return Number of members of this FSet
     */
    public int getNumMembers() {
        return fMembers.size();
    }

    /**
     * @return Unmodifiable list of members of this FSet
     */
    public List getMembers() {
        return Collections.unmodifiableList(fMembers);
    }

    public Set getMembersS() {
        return Collections.unmodifiableSet(new HashSet(fMembers));
    }

    /**
     * @return
     */
    public String[] getMembersArray() {
        // safe copy
        return (String[]) fMembers.toArray(new String[fMembers.size()]);
    }

    public int getNumMembers(final RankedList rl) {
        return getNumMembers(this, rl);
    }

    static int getNumMembers(final ISet iset, final RankedList rl) {
        int ntrue = 0;
    
        for (int i = 0; i < rl.getSize(); i++) {
            if (iset.isMember(rl.getRankName(i))) {
                ntrue++;
            }
        }
    
        return ntrue;
    }

}    // End FSet
