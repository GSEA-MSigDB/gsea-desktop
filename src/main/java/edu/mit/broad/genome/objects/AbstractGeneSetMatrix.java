/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.Errors;
import edu.mit.broad.genome.parsers.AuxUtils;
import gnu.trove.THashSet;

import java.util.*;

/**
 * @author Aravind Subramanian
 */
public abstract class AbstractGeneSetMatrix extends AbstractObject implements GeneSetMatrix {

    /**
     * Each member is a GeneSet
     */
    private List fGeneSets;

    private Set fGeneSetNames_nonaux = null;

    /**
     * Subclasses must call initMatrix
     */
    protected AbstractGeneSetMatrix() {
    }

    // common init routine
    // subclasses MUST call
    // gset names MUST all be UNIQUE - this IS checked here
    // @BNOTE ADDED JAN 27 2006 geset names are case INsensitive
    protected void initMatrix(final String name, final GeneSet[] gsets) {
        super.initialize(name);

        if (gsets == null) {
            throw new IllegalArgumentException("Param gsets cannot be null");
        }

        Set names = new HashSet();
        Errors errors = new Errors();

        this.fGeneSets = new ArrayList(gsets.length);
        for (int i = 0; i < gsets.length; i++) {
            if (gsets[i] == null) {
                throw new IllegalArgumentException("Null GeneSet not allowed at index: " + i + " total len: " + gsets.length);
            }

            if (names.contains(gsets[i].getName())) {
                errors.add("GeneSets should have unique names. The lookup is case INsensitive. Found duplicate name: " + gsets[i].getName());
            } else {
                names.add(gsets[i].getName());
            }

            fGeneSets.add(gsets[i]);

        }

        errors.barfIfNotEmptyRuntime();
    }

    public boolean containsSet(final String gsetName) {
        if (fGeneSetNames_nonaux == null) {
            // init it
            this.fGeneSetNames_nonaux = new THashSet();
            for (int i = 0; i < getNumGeneSets(); i++) {
                //log.debug("Adding: " + getGeneSet(i).getName());
                fGeneSetNames_nonaux.add(AuxUtils.getAuxNameOnlyNoHash(getGeneSet(i).getName()));
            }
        }

        if (fGeneSetNames_nonaux.contains(gsetName)) {
            return true;
        }

        return fGeneSetNames_nonaux.contains(AuxUtils.getAuxNameOnlyNoHash(gsetName));
    }

    public String getQuickInfo() {
        StringBuffer buf = new StringBuffer().append(getNumGeneSets()).append(" gene sets");
        return buf.toString();
    }

    public int getNumGeneSets() {
        return fGeneSets.size();
    }

    /**
     * return a ref to the real geneset
     *
     * @param i
     * @return
     */
    public GeneSet getGeneSet(int i) {
        return (GeneSet) fGeneSets.get(i);
    }

    private Map fNameGeneSetMap; // lazily filled

    // case IN sensitive query
    public GeneSet getGeneSet(final String gsetnameF) {

        // @todo fix me hack to overcome the errors cause by using a File sep char in gset names
        // @see edbparser
        String gsetname = gsetnameF.replace('|', '/');
        gsetname = AuxUtils.getAuxNameOnlyNoHash(gsetnameF); // @note
        gsetname = gsetname.toUpperCase();

        if (fNameGeneSetMap == null) {
            fNameGeneSetMap = new HashMap(getNumGeneSets());
            for (int i = 0; i < getNumGeneSets(); i++) {
                GeneSet gset = getGeneSet(i);
                fNameGeneSetMap.put(gset.getName().toUpperCase(), gset); // already guranteed unique names
                fNameGeneSetMap.put(AuxUtils.getAuxNameOnlyNoHash(gset.getName()).toUpperCase(), gset);
            }
        }

        Object obj = fNameGeneSetMap.get(gsetname);

        String tryharder = null;
        if (obj == null) {
            // help out
            tryharder = gsetname;
            if (gsetname.indexOf("#") == -1) {
                tryharder = getName() + "#" + gsetname;
            }
            obj = fNameGeneSetMap.get(tryharder);
        }

        // sometimes / gets replaced into \
        if (obj == null) {
            // help out
            tryharder = gsetname;
            if (gsetname.indexOf("#") == -1) {
                tryharder = getName() + "#" + gsetname;
            }

            tryharder = tryharder.replace('\\', '/');
            obj = fNameGeneSetMap.get(tryharder);

            if (obj == null) {
                tryharder = gsetname;
                if (gsetname.indexOf("#") == -1) {
                    tryharder = getName() + "#" + gsetname;
                }

                tryharder = tryharder.replace('/', '\\');
                obj = fNameGeneSetMap.get(tryharder);
            }

            if (obj == null) {
                tryharder = gsetname.replace('|', '/');
                if (gsetname.indexOf("#") == -1) {
                    tryharder = getName() + "#" + gsetname;
                }

                tryharder = tryharder.replace('/', '\\');
                obj = fNameGeneSetMap.get(tryharder);
            }
        }


        if (obj == null) {
            StringBuffer buf = new StringBuffer("In GeneSetMatrix: " + getName() + " no GeneSet found with name: " + gsetname);
            buf.append("\nAvailable GeneSets are: \n");
            for (Iterator it = fNameGeneSetMap.keySet().iterator(); it.hasNext();) {
                Object key = it.next();
                buf.append(key.toString()).append('\n');
            }

            buf.append("Also tried looking for gset: ").append(tryharder);

            throw new IllegalArgumentException(buf.toString());
            //log.warn("temp disabled - could get geneset: " + gsetname);
            //return new FSet(gsetname, new String[]{});
        } else {
            return (GeneSet) obj;
        }
    }

    /**
     * All gsets
     * directly -- no cloning
     *
     * @return
     */
    public GeneSet[] getGeneSets() {
        return (GeneSet[]) fGeneSets.toArray(new GeneSet[fGeneSets.size()]);
    }

    public List getGeneSetsL() {
        return Collections.unmodifiableList(fGeneSets);
    }

    /**
     * The number of members in the biggest GeneSet.
     *
     * @return
     */
    public int getMaxGeneSetSize() {
        return GeneSetMatrixHelper.getMaxMemberCount(fGeneSets);
    }

    /**
     * non-redundant list of names of features across
     * gsets in this v
     *
     * @return
     */
    public String[] getAllMemberNamesOnlyOnce() {
        return GeneSetMatrixHelper.getAllMemberNames(fGeneSets);
    }

    // does the real stuff
    public Set getAllMemberNamesOnlyOnceS() {
        return GeneSetMatrixHelper.getAllMemberNamesS(fGeneSets);
    }

    public String[] getAllMemberNames() {
        return GeneSetMatrixHelper.getAllMemberNameOccurrences(fGeneSets);
    }

    public String getGeneSetName(int g) {
        return getGeneSet(g).getName();
    }

} // End class AbstractGeneSetMatrix
