/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects.esmatrix.db;

import edu.mit.broad.genome.math.Vector;

/**
 * @author Aravind Subramanian
 */
public class EnrichmentScoreImpl implements EnrichmentScore {

    private int fNumHits;
    private float fES;
    private int fRankAtES;
    private float fRankScoreAtES;

    private float fNES;
    private float fNP;
    private float fFWER;
    private float fFDR;

    // deep vars -- not always available
    private Vector fESProfile;
    private Vector fESProfile_point_by_point;
    private int[] fHitIndices;

    /**
     * Class constructor
     *
     * @param es
     * @param rankAtEs
     * @param corrAtEs
     * @param nes
     * @param np
     * @param fdr
     * @param fwer
     * @param hitIndices_opt
     * @param runningRaw_opt
     */
    public EnrichmentScoreImpl(final float es,
                               final int rankAtEs,
                               final float corrAtEs,
                               final float nes,
                               final float np,
                               final float fdr,
                               final float fwer,
                               final int numHits,
                               final int[] hitIndices_opt,
                               final Vector runningRaw_opt,
                               final Vector runningRaw_ppoint_by_point_opt) {
        init(es, rankAtEs, corrAtEs, nes, np, fdr, fwer, numHits,
                hitIndices_opt, runningRaw_opt, runningRaw_ppoint_by_point_opt);
    }

    /**
     * Class constructor
     *
     * @param es
     * @param nes
     * @param np
     * @param fdr
     * @param fwer
     */
    public EnrichmentScoreImpl(EnrichmentScore es,
                               final float nes,
                               final float np,
                               final float fdr,
                               final float fwer) {
        init(es.getES(), es.getRankAtES(), es.getRankScoreAtES(),
                nes, np, fdr, fwer, es.getNumHits(), es.getHitIndices(), es.getESProfile(), es.getESProfile_point_by_point_opt());
    }

    protected void init(
            final float es,
            final int rankAtEs,
            final float corrAtEs,
            final float nes,
            final float np,
            final float fdr,
            final float fwer,
            final int numHits,
            final int[] hitIndices_opt,
            final Vector runningRaw_opt,
            final Vector runningRaw_ppoint_by_point_opt) {

        this.fES = es;
        this.fRankAtES = rankAtEs;
        this.fRankScoreAtES = corrAtEs;
        this.fNES = nes;
        this.fNP = np;
        this.fFDR = fdr;
        this.fFWER = fwer;

        this.fNumHits = numHits;

        // optional
        this.fHitIndices = hitIndices_opt;
        this.fESProfile = runningRaw_opt;
        this.fESProfile_point_by_point = runningRaw_ppoint_by_point_opt;
    }

    public float getES() {
        return fES;
    }

    public int getRankAtES() {
        return fRankAtES;
    }

    public float getRankScoreAtES() {
        return fRankScoreAtES;
    }

    public float getNES() {
        return fNES;
    }

    public float getNP() {
        return fNP;
    }

    public float getFWER() {
        return fFWER;
    }

    public float getFDR() {
        return fFDR;
    }

    public Vector getESProfile() {
        //ensureDeep();
        if (fESProfile != null) {
            fESProfile.setImmutable();
        }
        return fESProfile;
    }

    public Vector getESProfile_point_by_point_opt() {
        return fESProfile_point_by_point;
    }

    public int getNumHits() {
        return fNumHits;
    }

    public int[] getHitIndices() {
        //ensureDeep();
        return fHitIndices;
    }

} // End class KSScoreStruc
