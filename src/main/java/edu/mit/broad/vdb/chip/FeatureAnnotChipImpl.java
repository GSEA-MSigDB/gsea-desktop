/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.chip;

import edu.mit.broad.genome.objects.AbstractObject;
import edu.mit.broad.genome.objects.ColorMap;
import edu.mit.broad.genome.objects.FeatureAnnot;

/**
 * Integrates chip and dataset annotation
 * <p/>
 * Chip is used to lookup the annotation.
 * Ann is the thing that determines how many features and their names
 * <p/>
 *
 * @author Aravind Subramanian
 */
public class FeatureAnnotChipImpl extends AbstractObject implements FeatureAnnot {

    private Chip fChip;

    private ColorMap.Rows fColorMap_opt;

    private Helper fHelper;

    /**
     * Class constructor
     *
     * @param chip
     * @param colorMap_opt
     */
    public FeatureAnnotChipImpl(final Chip chip,
                                final ColorMap.Rows colorMap_opt) {

        if (chip == null) {
            throw new IllegalArgumentException("Param chip cannot be null");
        }

        super.initialize(chip.getName());

        this.fChip = chip;
        this.fColorMap_opt = colorMap_opt;
        this.fHelper = new Helper();
    }

    public Chip getChip() {
        return fChip;
    }

    public void setChip(final Chip chip, final ColorMap.Rows cmr) {
        if (chip != null) {
            Helper.checkChip(fChip, chip);
            this.fChip = chip;
            this.fColorMap_opt = cmr;
        }
    }

    public String getQuickInfo() {
        return fChip.getQuickInfo();
    }

    public ColorMap.Rows getColorMap() {
        return fColorMap_opt;
    }

    public int getNumFeatures() {
        try {
            return fChip.getNumProbes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // No native as made from a chip
    public boolean hasNativeDescriptions() {
        return false;
    }

    public String getNativeDesc(final String featureName) {
        return null;
    }

    public String getGeneSymbol(final String featureName) {
        return fHelper.getGeneSymbol(featureName, fChip);
    }

    public String getGeneTitle(final String featureName) {
        return fHelper.getGeneTitle(featureName, fChip);
    }

} // End class FeatureAnnotationChipImpl
