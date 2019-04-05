/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.vdb.map;

import edu.mit.broad.genome.objects.GeneSetMatrix;
import edu.mit.broad.vdb.chip.Chip;

/**
 * @author Aravind Subramanian
 */
public class MGeneSetMatrix {

    private MGeneSet[] mappedGeneSets; // always stored

    public MGeneSetMatrix(final GeneSetMatrix sourceGeneSetMatrix,
                          final boolean maintainEtiology,
                          final Chip targetChip,
                          final Chip2ChipMapper mapper) throws Exception {

        if (sourceGeneSetMatrix == null) {
            throw new IllegalArgumentException("Param sourceGeneSetMatrix cannot be null");
        }
        if (targetChip == null) {
            throw new IllegalArgumentException("Param targetChip cannot be null");
        }

        this.mappedGeneSets = new MGeneSet[sourceGeneSetMatrix.getNumGeneSets()];
        for (int i = 0; i < sourceGeneSetMatrix.getNumGeneSets(); i++) {
            this.mappedGeneSets[i] = new MGeneSet(sourceGeneSetMatrix.getGeneSet(i),
                    maintainEtiology, targetChip, mapper);
        }
    }

    public MGeneSet getMappedGeneSet(final int m) {
        return mappedGeneSets[m];
    }

    public int getNumMappedSets() {
        return mappedGeneSets.length;
    }

    public MappingEtiology[] getEtiologies() {
        final MappingEtiology[] mets = new MappingEtiology[getNumMappedSets()];
        for (int i = 0; i < mets.length; i++) {
            mets[i] = getMappedGeneSet(i).getEtiology();
        }
    
        return mets;
    }
} 