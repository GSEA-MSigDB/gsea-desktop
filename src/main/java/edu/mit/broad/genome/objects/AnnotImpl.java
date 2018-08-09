/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import edu.mit.broad.vdb.chip.Chip;
import edu.mit.broad.vdb.sampledb.SampleAnnot;

/**
 * @author Aravind Subramanian
 */
public class AnnotImpl implements Annot {

    private FeatureAnnot fFeatureAnnot;

    private SampleAnnot fSampleAnnot;

    /**
     * Class constructor
     * <p/>
     * Both parameters are nullable
     *
     * @param featureAnnot
     * @param sampleAnnot
     */
    public AnnotImpl(final FeatureAnnot featureAnnot, final SampleAnnot sampleAnnot) {

        if (featureAnnot == null) {
            throw new IllegalArgumentException("Param featureAnnot cannot be null");
        }

        if (sampleAnnot == null) {
            throw new IllegalArgumentException("Param sampleAnnot cannot be null");
            //klog.warn("Param sampleAnnot_optAnnot was null for fa: " + featureAnnot.getName());
            //TraceUtils.showTrace();
        }

        this.fFeatureAnnot = featureAnnot;
        this.fSampleAnnot = sampleAnnot;
    }

    public FeatureAnnot getFeatureAnnot() {
        return fFeatureAnnot;
    }

    // @note heres some magix
    // if sample annot is null
    public SampleAnnot getSampleAnnot_global() {
        return fSampleAnnot;
    }

    public SampleAnnot getSampleAnnot_synched(final String[] colNames) {
        if (colNames == null) {
            throw new IllegalArgumentException("Param colNames cannot be null");
        }

        if (fSampleAnnot == null) {
            throw new IllegalStateException("No available sample annot: " + fSampleAnnot);
        }

        return fSampleAnnot.cloneDeep(colNames);
    }

    public Chip getChip() {
        return fFeatureAnnot.getChip();
    }

    public void setChip(final Chip chip, final ColorMap.Rows cmr) {
        fFeatureAnnot.setChip(chip, cmr);
    }

} // End class AnnotImpl
