/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.vdb.map;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.objects.AbstractObject;
import edu.mit.broad.genome.objects.DefaultGeneSetMatrix;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.GeneSetMatrix;
import edu.mit.broad.genome.parsers.AuxUtils;
import edu.mit.broad.vdb.chip.Chip;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Aravind Subramanian
 */
public class Chip2ChipMapper extends AbstractObject {

    public static GeneSetMatrix createCombinedGeneSetMatrix(final String name, final MGeneSetMatrix mgm) {
    
        if (mgm == null) {
            throw new IllegalArgumentException("Param mgm cannot be null");
        }

        int numMappedSets = mgm.getNumMappedSets();
        final List<Set<String>> sets = new ArrayList<Set<String>>(numMappedSets);
        final String[] names = new String[numMappedSets];
        for (int j = 0; j < numMappedSets; j++) {
            GeneSet gset = mgm.getMappedGeneSet(j).getMappedGeneSet(true);
            HashSet<String> memberSet = new HashSet<String>();
            sets.add(memberSet);
            memberSet.addAll(gset.getMembersS());
            names[j] = gset.getName();
        }
    
        GeneSet[] gsets = new GeneSet[numMappedSets];
        for (int i = 0; i < numMappedSets; i++) {
            gsets[i] = new GeneSet(AuxUtils.getAuxNameOnlyNoHash(names[i]), sets.get(i));
        }
    
        return new DefaultGeneSetMatrix(name, gsets);
    }

    private Chip fTargetChip;

    public Chip2ChipMapper(final Chip targetChip) {

        if (targetChip == null) {
            throw new IllegalArgumentException("Param targetChip cannot be null");
        }

        System.out.println("##### target: " + targetChip.getName());

        this.fTargetChip = targetChip;
        super.initialize(fTargetChip.getName());
    }

    public String getQuickInfo() {
        return null;
    }

    public Chip getTargetChip() {
        return fTargetChip;
    }

    public Set<String> map(final String sourceMember) throws Exception {

        Set<String> targetProbeNames = new HashSet<String>();
        if (StringUtils.isNotBlank(sourceMember)) {
            Set<String> probeNames = fTargetChip.getProbeNames(sourceMember);
            if (probeNames != null) {
                targetProbeNames.addAll(probeNames);
            }
        }

        return targetProbeNames;
    }

    public MGeneSetMatrix map(final GeneSetMatrix sourceGm, final boolean maintainEtiology) throws Exception {
        return new MGeneSetMatrix(sourceGm, maintainEtiology, fTargetChip, this);
    }
}