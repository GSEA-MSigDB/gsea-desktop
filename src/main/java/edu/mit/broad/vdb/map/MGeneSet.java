/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.vdb.map;

import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.parsers.AuxUtils;
import edu.mit.broad.vdb.chip.Chip;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MGeneSet {

    private GeneSet mappedGeneSet; // always stored
    private MappingEtiology met;

    public MGeneSet(final GeneSet sourceGeneSet,
                    final boolean maintainEtiology,
                    final Chip targetChip,
                    final Chip2ChipMapper mapper) throws Exception {

        if (sourceGeneSet == null) {
            throw new IllegalArgumentException("Param sourceGeneSet cannot be null");
        }
        if (targetChip == null) {
            throw new IllegalArgumentException("Param targetChip cannot be null");
        }
        if (mapper == null) {
            throw new IllegalArgumentException("Param mapper cannot be null");
        }

        Set<String> targets = new HashSet<String>();
        if (maintainEtiology) {
            this.met = new MappingEtiology(AuxUtils.getAuxNameOnlyNoHash(sourceGeneSet.getName()), targetChip.getName());
        }

        for (int i = 0; i < sourceGeneSet.getNumMembers(); i++) {
            String sourceMember = sourceGeneSet.getMember(i);
            Set<String> target = mapper.map(sourceMember);

            if (target != null) {
                targets.addAll(target);
            }

            if (maintainEtiology) {
                try {
                    this.met.add(sourceMember, target);
                } catch (Throwable t) {
                    // TODO: confirm if this clause is necessary
                    t.printStackTrace();
                }
            }
        }

        this.mappedGeneSet = new GeneSet(sourceGeneSet.getName(), Collections.unmodifiableSet(targets));

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
}