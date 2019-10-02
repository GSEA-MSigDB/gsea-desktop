/*
 * Copyright (c) 2002, 2019 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 */
package edu.mit.broad.vdb.chip;

import java.util.Collections;

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
// TODO: can we replace this altogether with FeatureAnnot?
public class FeatureAnnotChip extends FeatureAnnot {

    public FeatureAnnotChip(final Chip chip) {
        // Use EMPTY_LIST for superclass rowName because these will come from
        // the chip instead.  While we could use chip.getProbeNames(), that's 
        // a potentially expensive operation and is not necessary given the
        // method overrides in this class.
        super(chip.getName(), Collections.EMPTY_LIST, null, chip);
        this.fHelper = new Helper();
    }

    @Override
    public String getQuickInfo() {
        return fChip.getQuickInfo();
    }

    @Override
    public int getNumFeatures() {
        try {
            return fChip.getNumProbes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // No native as made from a chip
    @Override
    public boolean hasNativeDescriptions() {
        return false;
    }

    @Override
    public String getNativeDesc(final String featureName) {
        return null;
    }
}