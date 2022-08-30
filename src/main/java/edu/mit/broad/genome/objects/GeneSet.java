/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
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
 * For visualization purposes, a GeneSet can also be associated with an icon and color.
 * <p/>
 * There should be no duplicate members
 *
 * @author Aravind Subramanian, David Eby
 */
public class GeneSet extends AbstractObject implements PersistentObject, Versioned {
    /**
     * Each member is a String (not using a Set as we want to be able to do an
     * indexOf
     * As/Also i.e order sometimes matters
     */
    private List<String> fMembers;

    // For fast membership tests
    private Set<String> fMembersSet;
    
    private MSigDBVersion msigDBVersion = null;

    private GeneSet() { }

    /**
     * Class Constructor.
     * No member can be null
     * No duplicates allowed
     */
    public GeneSet(final String name, final String nameEnglish, final String[] members) {
        init(name, nameEnglish, members, true);
    }

    /**
     * Class Constructor.
     * No member can be null
     * No duplicates allowed
     * Objects in specified List are to Stringed
     * Data is NOT shared
     */
    // @TODO Confirm whether the collection can be typed as <String>
    public GeneSet(final String name, final String nameEnglish, final List members, final boolean checkForDuplicates) {
        init(name, nameEnglish, members, checkForDuplicates);
    }

    // As above, but with an explicit Version provided.
    // TODO: deeper refactoring around this field (and more).
    public GeneSet(final String name, final String nameEnglish, final List members, final boolean checkForDuplicates, MSigDBVersion msigDBVersion) {
        setMSigDBVersion(msigDBVersion);
        init(name, nameEnglish, members, checkForDuplicates);
    }

    public GeneSet(final String name, final String[] members) {
        init(name, null, members, true);
    }

    /**
     * Data is NOT shared
     *
     * @param name
     * @param members
     */
    // TODO: Confirm whether the collection can be typed as <String>
    public GeneSet(final String name, final Set members) {
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
    public GeneSet(final GeneSet gset, final ScoredDataset sds) {

        // make safe copy
        List<String> members = new ArrayList<String>(gset.getMembers());

        // sort it
        Collections.sort(members, new ComparatorFactory.ScoredDatasetScoreComparator(sds));

        this.init(gset.getName() + ".orderedby." + sds.getName(), gset.getNameEnglish(), members, false);
    }

    // @maint IMP see duplicated init method below
    // TODO: Confirm whether the collection can be typed as <String>
    private void init(final String name, final String nameEnglish, final Collection members, final boolean checkForDuplicates) {
        super.initialize(name, nameEnglish);

        if (members == null) {
            throw new NullPointerException("Param members cant be null");
        }

        this.fMembers = new ArrayList<String>(members.size()); // make safe copy
        this.fMembersSet = new HashSet<String>();

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

            // Possibly speed up by using a LinkedHashSet instead of an ArrayList, or similar
            if (checkForDuplicates) {
                // IMP to add, as in some cases it might be legit
                // for example when creating a combined dataset from bpog (when markers are shared)
                if (fMembersSet.contains(mn)) {
                    log.warn("Duplicate GeneSet member: {}", mn);// dont barf, just warn (possible imp for randomizations)
                } else {
                    fMembers.add(mn);
                    fMembersSet.add(mn);
                }
            } else { // blindly believe and add
                fMembers.add(mn);
                fMembersSet.add(mn);
            }
        }

        if (msigDBVersion == null) { setMSigDBVersion(MSigDBVersion.createUnknownTrackingVersion(name)); }
    }

    // @maint IMP see duplicated init method above
    // Prob should just call the other init with Arrays.asList(members) or vice versa
    private void init(final String name, final String nameEnglish, final String[] members, final boolean checkForDuplicates) {
        super.initialize(name, nameEnglish);

        if (members == null) { throw new NullPointerException("Members param cant be null"); }

        this.fMembers = new ArrayList<String>(members.length); // make safe copy
        this.fMembersSet = new HashSet<String>();

        for (int i = 0; i < members.length; i++) {
            if (members[i] == null) {
                throw new NullPointerException("Member is null at: " + i);
            }

            if (checkForDuplicates) {
                if (fMembersSet.contains(members[i])) {
                    log.warn("Duplicate GeneSet member: {}", members[i]);// dont barf, just warn (possible imp for randomizations)
                } else {
                    fMembers.add(members[i]);
                    fMembersSet.add(members[i]);
                }
            } else { // blindly believe
                fMembers.add(members[i]);
                fMembersSet.add(members[i]);
            }
        }
        
        if (msigDBVersion == null) { setMSigDBVersion(MSigDBVersion.createUnknownTrackingVersion(name)); }
    }

    public GeneSet cloneDeep(final Dataset qualify) {

        List<String> all = new ArrayList<String>(fMembers);

        for (int i = 0; i < getNumMembers(); i++) {
            String rn = getMember(i);
            if (qualify.getRowIndex(rn) == -1) {
                all.remove(rn);
            }
        }

        return new GeneSet(getName(), getNameEnglish(), all, false);
    }

    public GeneSet cloneDeep(final RankedList qualify) {

        List<String> all = new ArrayList<String>(fMembers);

        for (int i = 0; i < getNumMembers(); i++) {
            String rn = getMember(i);
            if (qualify.getRank(rn) == -1) {
                all.remove(rn);
            }
        }

        return new GeneSet(getName(), getNameEnglish(), all, false);
    }

    // @todo this is prob not needed -> instead a constructor is better i think
    public GeneSet cloneShallow(final String newName) {
        if (newName == null) {
            throw new IllegalArgumentException("Param newName cannot be null");
        }

        GeneSet fset = new GeneSet();
        fset.initialize(newName, getNameEnglish()); // this sets the new name

        // the vars are all shallow
        fset.fMembers = this.fMembers; // @note not duplicated
        fset.fMembersSet = this.fMembersSet;
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
        return fMembers.get(pos);
    }

    /**
     * Checks if specified name belongs to this FSet.
     *
     * @param name
     * @return
     */
    public boolean isMember(final String name) {
        return fMembersSet.contains(name);
    }

    /**
     * @return Number of members of this FSet
     */
    public int getNumMembers() {
        return fMembers.size();
    }

    /**
     * @return Unmodifiable list of members of this GeneSet
     */
    public List<String> getMembers() {
        return Collections.unmodifiableList(fMembers);
    }

    public Set<String> getMembersS() {
        return Collections.unmodifiableSet(new HashSet<String>(fMembers));
    }

    public String[] getMembersArray() {
        // safe copy
        return fMembers.toArray(new String[fMembers.size()]);
    }

    public int getNumMembers(final RankedList rl) {
        int ntrue = 0;
    
        for (int i = 0; i < rl.getSize(); i++) {
            if (isMember(rl.getRankName(i))) {
                ntrue++;
            }
        }
    
        return ntrue;
    }

    public MSigDBVersion getMSigDBVersion() {
        return msigDBVersion;
    }

    public void setMSigDBVersion(MSigDBVersion msigDBVersion) {
        this.msigDBVersion = msigDBVersion;
    }
}
