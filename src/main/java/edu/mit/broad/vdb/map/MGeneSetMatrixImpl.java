/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.map;

import edu.mit.broad.genome.objects.DefaultGeneSetMatrix;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.GeneSetMatrix;

/**
 * @author Aravind Subramanian
 */
class MGeneSetMatrixImpl implements MGeneSetMatrix {

    private MGeneSet[] mappedGeneSets; // always stored

    private String fOrigGmName;

    /**
     * Class constructor
     *
     * @param sourceGeneSetMatrix
     * @param maintainEtiology
     */

    MGeneSetMatrixImpl(final GeneSetMatrix sourceGeneSetMatrix,
                       final boolean maintainEtiology,
                       final String sourceChipName,
                       final String targetChipName,
                       final MappingDbType dbType,
                       final Mapper mapper) throws Exception {

        if (sourceGeneSetMatrix == null) {
            throw new IllegalArgumentException("Param sourceGeneSetMatrix cannot be null");
        }

        if (sourceChipName == null) {
            throw new IllegalArgumentException("Param sourceChipName cannot be null");
        }

        if (targetChipName == null) {
            throw new IllegalArgumentException("Param targetChipName cannot be null");
        }

        this.mappedGeneSets = new MGeneSet[sourceGeneSetMatrix.getNumGeneSets()];
        for (int i = 0; i < sourceGeneSetMatrix.getNumGeneSets(); i++) {
            this.mappedGeneSets[i] = new MGeneSetImpl(sourceGeneSetMatrix.getGeneSet(i),
                    maintainEtiology, sourceChipName, targetChipName, dbType, mapper);
        }

        this.fOrigGmName = sourceGeneSetMatrix.getName();

    }

    public MGeneSet[] getMappedGeneSets() {
        return mappedGeneSets;
    }

    public MGeneSet getMappedGeneSet(final int m) {
        return mappedGeneSets[m];
    }

    public GeneSetMatrix getMappedGeneSetMatrix(final String prefix) {
        GeneSet[] gsets = new GeneSet[mappedGeneSets.length];
        for (int i = 0; i < gsets.length; i++) {
            gsets[i] = mappedGeneSets[i].getMappedGeneSet(true);
        }

        String name;
        if (prefix == null || prefix.length() == 0) {
            name = fOrigGmName;
        } else {
            name = prefix + "_" + fOrigGmName;
        }

        return new DefaultGeneSetMatrix(name, gsets);
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

} // End interface MGeneSetMatrixImpl
