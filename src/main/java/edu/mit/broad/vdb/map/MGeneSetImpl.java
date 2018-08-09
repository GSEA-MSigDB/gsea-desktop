/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.map;

import edu.mit.broad.genome.objects.FSet;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.parsers.AuxUtils;
import edu.mit.broad.vdb.VdbRuntimeResources;
import edu.mit.broad.vdb.chip.Chip;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Internal class
 */
class MGeneSetImpl implements MGeneSet {

    private GeneSet mappedGeneSet; // always stored
    private MutableMappingEtiology met;

    /**
     * Class constructor
     *
     * @param sourceGeneSet
     * @param maintainEtiology
     */
    protected MGeneSetImpl(final GeneSet sourceGeneSet,
                           final boolean maintainEtiology,
                           final String sourceChipName,
                           final String targetChipName,
                           final MappingDbType dbType,
                           final Mapper mapper) throws Exception {

        if (sourceGeneSet == null) {
            throw new IllegalArgumentException("Param sourceGeneSet cannot be null");
        }

        if (sourceChipName == null) {
            throw new IllegalArgumentException("Param sourceChipName cannot be null");
        }

        if (targetChipName == null) {
            throw new IllegalArgumentException("Param targetChipName cannot be null");
        }

        if (dbType == null) {
            throw new IllegalArgumentException("Param dbType cannot be null");
        }
        if (mapper == null) {
            throw new IllegalArgumentException("Param mapper cannot be null");
        }

        Set targets = new HashSet();
        if (maintainEtiology) {
            this.met = new MutableMappingEtiology(AuxUtils.getAuxNameOnlyNoHash(sourceGeneSet.getName()), sourceChipName, targetChipName, dbType);
        }

        Chip sourceChip = null;

        for (int i = 0; i < sourceGeneSet.getNumMembers(); i++) {
            String sourceProbeName = sourceGeneSet.getMember(i);
            Object target = mapper.map(sourceProbeName);

            if (target == null) {

            } else if (target instanceof String) {
                targets.add(target);
            } else if (target instanceof Set) {
                targets.addAll((Set) target);
            } else {
                throw new IllegalStateException("Unnown mapped object: " + target + " " + target.getClass());
            }

            if (maintainEtiology) {
                try {
                    if (sourceChip == null) {
                        sourceChip = VdbRuntimeResources.getChip(sourceChipName);
                    }
                    this.met.add(sourceProbeName, target, sourceChip.isProbe(sourceProbeName));
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        //this.mappedGeneSet = new FSet(sourceGeneSet.getName() + "_mapped" + sourceChipName, Collections.unmodifiableSet(targets));
        this.mappedGeneSet = new FSet(sourceGeneSet.getName(), Collections.unmodifiableSet(targets));

        if (maintainEtiology) {
            this.met.setImmutable();
        }
    }

    public GeneSet getMappedGeneSet(final boolean simpleName) {
        return mappedGeneSet;
    }

    public MappingEtiology getEtiology() {
        return met;
    }

} // End class MGeneSetImpl
