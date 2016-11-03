/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects.esmatrix.db;

import edu.mit.broad.genome.alg.Metric;
import edu.mit.broad.genome.alg.gsea.EdbAlgs;
import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.objects.strucs.FdrStruc;

import java.io.File;
import java.util.*;

/**
 * @author Aravind Subramanian
 */
public abstract class AbstractEnrichmentDb extends AbstractObject implements EnrichmentDb {

    private LabelledVectorProcessor fLvp;
    private Metric fMetric;
    private Map fMetricParams;
    private SortMode fSortMode;
    private Order fOrder;
    private int fNumPerms;

    private File fEdbDir_opt;

    private Map fGeneSetNameResultMap;

    // Subclasses must call init before doing much

    protected AbstractEnrichmentDb() {

    }

    /**
     * Class constructor
     *
     * @param name
     */
    public void init(final String name,
                     final LabelledVectorProcessor lvp,
                     final Metric metric,
                     final Map metricParams,
                     final SortMode sort,
                     final Order order,
                     final int nperm,
                     final File edb_dir_opt) {


        if (lvp == null) {
            throw new IllegalArgumentException("Param lvp cannot be null");
        }
        if (metric == null) {
            throw new IllegalArgumentException("Param metric cannot be null");
        }
        if (sort == null) {
            throw new IllegalArgumentException("Param sort cannot be null");
        }
        if (order == null) {
            throw new IllegalArgumentException("Param order cannot be null");
        }

        super.initialize(name);

        this.fLvp = lvp;
        this.fMetric = metric;
        this.fMetricParams = metricParams;
        this.fSortMode = sort;
        this.fOrder = order;
        this.fNumPerms = nperm;
        this.fEdbDir_opt = edb_dir_opt;
        initResultMaps();
    }

    public File getEdbDir() {
        return fEdbDir_opt;
    }

    protected void initResultMaps() {
        if (fGeneSetNameResultMap == null) {

            // Enforce that gene sets are unique (barf here)
            this.fGeneSetNameResultMap = EdbAlgs.hashByGeneSetName(getResults());
        }
    }

    public int getNumScores(final boolean pos) {
        int cnt = 0;
        for (int i = 0; i < getNumResults(); i++) {

            if (pos) {

                if (XMath.isPositive(getResult(i).getScore().getES())) {
                    cnt++;
                }
            } else {
                if (XMath.isNegative(getResult(i).getScore().getES())) {
                    cnt++;
                }
            }
        }

        return cnt;
    }

    public int getNumNominallySig(final float npCutoffInclusive, final boolean pos) {
        int cnt = 0;
        for (int i = 0; i < getNumResults(); i++) {
            EnrichmentResult res = getResult(i);
            if (res.getScore().getES() > 0 && pos) {
                if (res.getScore().getNP() <= npCutoffInclusive) {
                    cnt++;
                }
            }

            if (res.getScore().getES() < 0 && !pos) {
                if (res.getScore().getNP() <= npCutoffInclusive) {
                    cnt++;
                }
            }

        }

        return cnt;
    }

    public int getNumFDRSig(final float fdrCutoffInclusive, final boolean pos) {
        return getFDRSig(fdrCutoffInclusive, pos).length;
    }

    public FdrStruc[] getFDRSig(final float fdrCutoffInclusive, final boolean pos) {
        final FdrStruc[] fdrs = getFDR(pos);

        final List list = new ArrayList();
        for (int i = 0; i < fdrs.length; i++) {
            if (fdrs[i].getFdr() <= fdrCutoffInclusive) {
                list.add(fdrs[i]);
            }
        }

        return (FdrStruc[]) list.toArray(new FdrStruc[list.size()]);
    }


    // lazilly init
    private FdrStruc[] fFdrPos;
    private FdrStruc[] fFdrNeg;

    private void initFDR() {

        if (fFdrPos != null) {
            return;
        }

        final List pos = new ArrayList();
        final List neg = new ArrayList();

        for (int i = 0; i < getNumResults(); i++) {
            final FdrStruc fdr = getResult(i).getFDR();
            if (fdr != null) {
                if (XMath.isPositive(fdr.getRealScore())) {
                    pos.add(fdr);
                } else {
                    neg.add(fdr);
                }
            }
        }

        this.fFdrPos = (FdrStruc[]) pos.toArray(new FdrStruc[pos.size()]);
        this.fFdrNeg = (FdrStruc[]) neg.toArray(new FdrStruc[neg.size()]);
    }

    public FdrStruc[] getFDR(boolean pos) {
        initFDR();
        if (pos && fFdrPos == null) {
            throw new IllegalStateException("Fdrs not yet calculated: " + pos);
        }

        if (!pos && fFdrNeg == null) {
            throw new IllegalStateException("Fdrs not yet calculated: " + pos);
        }


        if (pos) {
            return fFdrPos;
        } else {
            return fFdrNeg;
        }
    }

    public EnrichmentResult getResultForGeneSet(String gsetName_no_aux) {
        Object obj = fGeneSetNameResultMap.get(gsetName_no_aux);
        if (obj == null) {
            throw new IllegalArgumentException("No such gset result: " + gsetName_no_aux);
        } else {
            return (EnrichmentResult) obj;
        }
    }

    public String getQuickInfo() {
        return null;
    }

    public GeneSet[] getGeneSets() {
        return EdbAlgs.getGeneSets(getResults());
    }

    public List getGeneSetNames() {
        return EdbAlgs.getGeneSetNames(getResults());
    }

    public String[] getGeneSetNamesArray() {
        List list = getGeneSetNames();
        return (String[]) list.toArray(new String[list.size()]);
    }
    
    public EnrichmentResult[] getResults(final Comparator comp) {
        List list = getResultsList(comp);
        return (EnrichmentResult[]) list.toArray(new EnrichmentResult[list.size()]);
    }

    public EnrichmentResult getResult(final int i) {
        return getResults()[i];
    }

    public List getResultsList(final Comparator comp) {
        List list = getResultsList();
        Collections.sort(list, comp);
        return list;
    }


    public EnrichmentResult[] getResults(final Comparator comp, final boolean pos) {
        final EnrichmentResult[] all = getResults(comp);
        final List sub = new ArrayList();
        for (int i = 0; i < all.length; i++) {
            float es = all[i].getScore().getES();
            if (pos && XMath.isPositive(es)) {
                sub.add(all[i]);
            }

            if (!pos && XMath.isNegative(es)) {
                sub.add(all[i]);
            }
        }

        return (EnrichmentResult[]) sub.toArray(new EnrichmentResult[sub.size()]);
    }

    public int getNumResults() {
        return getResults().length;
    }

    public LabelledVectorProcessor getRankedListProcessor() {
        return fLvp;
    }

    public Metric getMetric() {
        return fMetric;
    }

    public Map getMetricParams() {
        return fMetricParams;
    }

    public SortMode getSortMode() {
        return fSortMode;
    }

    public Order getOrder() {
        return fOrder;
    }

    public int getNumPerm() {
        return fNumPerms;
    }

// @todo using gse names for the lss is not always ok

    public Vector getESS() {
        final Vector ess = new Vector(getNumResults());
        for (int i = 0; i < getNumResults(); i++) {
            ess.setElement(i, getResult(i).getScore().getES());
        }

        return ess;
    }

    public LabelledVector getESS_lv() {
        return new LabelledVector(getName() + "_ess", getGeneSetNamesArray(), getESS());
    }

    public Vector getNESS() {
        final Vector ness = new Vector(getNumResults());
        for (int i = 0; i < getNumResults(); i++) {
            ness.setElement(i, getResult(i).getScore().getNES());
        }

        return ness;
    }

    public Vector getNPs() {
        final Vector ess = new Vector(getNumResults());
        for (int i = 0; i < getNumResults(); i++) {
            ess.setElement(i, getResult(i).getScore().getNP());
        }

        return ess;
    }

    public Vector getFDRs() {
        final Vector fdsr = new Vector(getNumResults());
        for (int i = 0; i < getNumResults(); i++) {
            fdsr.setElement(i, getResult(i).getScore().getFDR());
        }

        return fdsr;
    }

}
