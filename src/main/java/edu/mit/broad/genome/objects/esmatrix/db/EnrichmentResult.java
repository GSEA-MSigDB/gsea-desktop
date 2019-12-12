/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects.esmatrix.db;

import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.GeneSetSignal;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.objects.strucs.FdrStruc;
import edu.mit.broad.vdb.chip.Chip;

/**
 * Class representing a single result
 */
public class EnrichmentResult {

    private EnrichmentScore fScore;

    private Vector fRndESS;

    private RankedList fRankedList;

    private Template fTemplate_opt;

    private GeneSet fGeneSet;

    private Chip fChip;

    private FdrStruc fFdr;

    // TODO: track possible NaNs creeping in via the es object. 
    public EnrichmentResult(final RankedList rl, final Template t_opt, final GeneSet gset, final Chip chip,
    		final EnrichmentScore es, final Vector rndEss, final FdrStruc fdr) {
        if (es == null) {
            throw new IllegalArgumentException("Param esStruc cannot be null");
        }
        if (gset == null) {
            throw new IllegalArgumentException("Param gset cannot be null");
        }

        this.fTemplate_opt = t_opt;
        this.fRankedList = rl;
        this.fGeneSet = gset;
        this.fChip = chip;
        this.fScore = es;
        this.fRndESS = rndEss;
        this.fGeneSet = gset;
        this.fFdr = fdr;
    }

    private GeneSetSignal fSignal;

    public GeneSetSignal getSignal() {
        if (fSignal == null) {
            this.fSignal = new GeneSetSignalImpl(this);
        }

        return fSignal;
    }

    public Vector getRndESS() {
        return fRndESS;
    }

    public FdrStruc getFDR() {
        return fFdr;
    }

    public EnrichmentScore getScore() {
        return fScore;
    }

    // DATA RELATED APIs

    public RankedList getRankedList() {
        return fRankedList;
    }

    public Template getTemplate() {
        return fTemplate_opt;
    }

    public GeneSet getGeneSet() {
        return fGeneSet;
    }

    public String getGeneSetName() {
        return fGeneSet.getName(true);
    }

    public Chip getChip() {
        return fChip;
    }

    public int getNumPerms() {
        return fRndESS.getSize();
    }

} // End lass EnrichmentResult
