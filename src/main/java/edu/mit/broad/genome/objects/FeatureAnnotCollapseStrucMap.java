/*
 * Copyright (c) 2002-2020 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

import java.util.Collections;
import java.util.Map;

import edu.mit.broad.genome.alg.DatasetGenerators.CollapseStruc;
import edu.mit.broad.genome.objects.FeatureAnnot;

/**
 * Allows looking up descriptions by gene symbol based on the information gleaned from the 
 * Collapse Dataset procedure.
 */
public class FeatureAnnotCollapseStrucMap extends FeatureAnnot {

    private final Map<String, CollapseStruc> collapseStrucMap;
    
    public FeatureAnnotCollapseStrucMap(String name, final Map<String, CollapseStruc> collapseStrucMap) {
        // Use EMPTY_LIST for superclass rowName because these are not needed for this particular use-case.
        // TODO: longer term, refactor the FeatureAnnot class hierarchy for the actual usage.
        super(name, Collections.EMPTY_LIST, null, null);
        this.collapseStrucMap = collapseStrucMap;
        this.fHelper = new Helper();
    }

    @Override
    public String getQuickInfo() {
        return fChip.getQuickInfo();
    }

    @Override
    public int getNumFeatures() {
        return collapseStrucMap.size();
    }

    @Override
    public boolean hasNativeDescriptions() {
        return true;
    }

    @Override
    public String getNativeDesc(final String featureName) {
        return collapseStrucMap.get(featureName).getTitle();
    }
}